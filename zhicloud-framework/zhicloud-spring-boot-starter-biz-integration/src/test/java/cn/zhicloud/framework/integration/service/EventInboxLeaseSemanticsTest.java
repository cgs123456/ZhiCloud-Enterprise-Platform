package cn.zhicloud.framework.integration.service;

import cn.zhicloud.framework.integration.api.ConsumeResult;
import cn.zhicloud.framework.integration.config.IntegrationAutoConfiguration;
import cn.zhicloud.framework.integration.dal.dataobject.EventInboxDO;
import cn.zhicloud.framework.datasource.config.ZhiCloudDataSourceAutoConfiguration;
import cn.zhicloud.framework.mybatis.config.ZhiCloudMybatisAutoConfiguration;
import cn.zhicloud.framework.tenant.core.context.TenantContextHolder;
import cn.zhicloud.framework.test.config.SqlInitializationTestConfiguration;
import com.alibaba.druid.spring.boot3.autoconfigure.DruidDataSourceAutoConfigure;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.github.yulichang.autoconfigure.MybatisPlusJoinAutoConfiguration;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

import static cn.zhicloud.framework.integration.enums.EventStatusConstants.INBOX_PROCESSING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 收件箱租约与 owner fencing 回归测试。
 *
 * <p>这些用例必须在生产实现之前失败：当前实现把活跃 PROCESSING 的 CAS 失败错误地归类为
 * DUPLICATE，并允许旧 owner 在租约被接管后继续结算，都会破坏可靠投递语义。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = EventInboxServiceTest.TestApplication.class)
@ActiveProfiles("unit-test")
@Sql(scripts = "/sql/clean.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
public class EventInboxLeaseSemanticsTest {

    private static final Long TENANT_ID = 0L;

    @Resource
    private EventInboxService eventInboxService;

    @Resource
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUpTenantContext() {
        TenantContextHolder.setTenantId(TENANT_ID);
    }

    @AfterEach
    void clearTenantContext() {
        TenantContextHolder.clear();
    }

    @Test
    void testActiveProcessingLeaseReturnsInProgressWithoutExecutingHandler() {
        insertProcessing("c-active", "e-active", "owner-active", LocalDateTime.now().plusSeconds(60),
                LocalDateTime.now());

        AtomicInteger calls = new AtomicInteger();
        assertEquals(ConsumeResult.IN_PROGRESS,
                consume("c-active", "e-active", calls),
                "未过期租约必须返回 IN_PROGRESS，不能伪装成已完成或重复投递");
        assertEquals(0, calls.get(), "持有活跃租约时不得执行业务处理器");

        EventInboxDO row = eventInboxService.selectByConsumerAndEvent("c-active", "e-active", TENANT_ID);
        assertEquals(INBOX_PROCESSING, row.getStatus());
        assertEquals("owner-active", row.getProcessingOwner());
    }

    @Test
    void testExpiredProcessingLeaseCanBeTakenOver() throws Exception {
        insertProcessing("c-expired", "e-expired", "owner-old", LocalDateTime.now().minusSeconds(1),
                LocalDateTime.now());

        AtomicInteger calls = new AtomicInteger();
        assertEquals(ConsumeResult.PROCESSED, consume("c-expired", "e-expired", calls),
                "租约过期后必须允许新实例接管，否则崩溃事件永久卡死");
        assertEquals(1, calls.get(), "接管后应执行一次业务处理器");

        EventInboxDO row = eventInboxService.selectByConsumerAndEvent("c-expired", "e-expired", TENANT_ID);
        assertEquals("PROCESSED", row.getStatus());
        assertNull(row.getProcessingOwner(), "成功后必须清理 owner");
        assertNull(row.getLeaseUntil(), "成功后必须清理租约");
    }

    @Test
    void testStaleOwnerCannotSettleAfterTakeover() {
        insertProcessing("c-fence", "e-fence", "owner-old", LocalDateTime.now().minusSeconds(1),
                LocalDateTime.now());

        AtomicInteger calls = new AtomicInteger();
        assertThrows(IllegalStateException.class, () -> eventInboxService.consume(
                "c-fence", "e-fence", "t", TENANT_ID, () -> {
                    calls.incrementAndGet();
                    // 模拟租约被新 owner 接管；旧 owner 的后续结算必须失败。
                    jdbcTemplate.update("UPDATE event_inbox SET processing_owner = ? "
                                    + "WHERE tenant_id = ? AND consumer_name = ? AND event_id = ?",
                            "owner-new", TENANT_ID, "c-fence", "e-fence");
                }),
                "旧 owner 在租约被接管后不得把事件标记为 PROCESSED");
        assertEquals(1, calls.get());

        EventInboxDO row = eventInboxService.selectByConsumerAndEvent("c-fence", "e-fence", TENANT_ID);
        assertEquals(INBOX_PROCESSING, row.getStatus());
        assertEquals("owner-new", row.getProcessingOwner());
    }

    @Test
    void testOldestPendingIncludesProcessingRows() {
        insertProcessing("c-pending", "e-pending", "owner-pending", LocalDateTime.now().plusSeconds(60),
                LocalDateTime.now().minusSeconds(120));

        long oldest = eventInboxService.oldestPendingSeconds();
        assertTrue(oldest >= 119L && oldest <= 125L,
                "卡在 PROCESSING 的事件也必须计入滞留监控，实际：" + oldest);
    }

    private ConsumeResult consume(String consumer, String eventId, AtomicInteger calls) {
        try {
            return eventInboxService.consume(consumer, eventId, "t", TENANT_ID, calls::incrementAndGet);
        } catch (Exception ex) {
            throw new AssertionError(ex);
        }
    }

    private void insertProcessing(String consumer, String eventId, String owner, LocalDateTime leaseUntil,
                                  LocalDateTime receivedAt) {
        jdbcTemplate.update("INSERT INTO event_inbox "
                        + "(tenant_id, consumer_name, event_id, event_type, status, processing_owner, "
                        + "lease_until, claimed_at, retry_count, received_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                TENANT_ID, consumer, eventId, "t", INBOX_PROCESSING, owner,
                Timestamp.valueOf(leaseUntil), Timestamp.valueOf(LocalDateTime.now()), 0,
                Timestamp.valueOf(receivedAt));
    }
}
