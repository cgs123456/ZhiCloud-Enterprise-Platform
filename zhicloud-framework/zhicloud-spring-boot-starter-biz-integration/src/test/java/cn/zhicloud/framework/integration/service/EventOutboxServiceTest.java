package cn.zhicloud.framework.integration.service;

import cn.zhicloud.framework.datasource.config.ZhiCloudDataSourceAutoConfiguration;
import cn.zhicloud.framework.integration.api.EventAppendReq;
import cn.zhicloud.framework.integration.config.IntegrationAutoConfiguration;
import cn.zhicloud.framework.integration.config.IntegrationProperties;
import cn.zhicloud.framework.integration.dal.dataobject.EventOutboxDO;
import cn.zhicloud.framework.integration.dal.mysql.EventOutboxMapper;
import cn.zhicloud.framework.integration.enums.ErrorCodeConstants;
import cn.zhicloud.framework.mybatis.config.ZhiCloudMybatisAutoConfiguration;
import cn.zhicloud.framework.tenant.core.context.TenantContextHolder;
import cn.zhicloud.framework.test.config.SqlInitializationTestConfiguration;
import com.alibaba.druid.spring.boot3.autoconfigure.DruidDataSourceAutoConfigure;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.github.yulichang.autoconfigure.MybatisPlusJoinAutoConfiguration;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.time.Duration;
import java.time.LocalDateTime;

import static cn.zhicloud.framework.integration.enums.EventStatusConstants.OUTBOX_DEAD;
import static cn.zhicloud.framework.integration.enums.EventStatusConstants.OUTBOX_NEW;
import static cn.zhicloud.framework.integration.enums.EventStatusConstants.OUTBOX_PUBLISHING;
import static cn.zhicloud.framework.integration.enums.EventStatusConstants.OUTBOX_PUBLISHED;
import static cn.zhicloud.framework.test.core.util.AssertUtils.assertServiceException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 事件发件箱测试（V100）：幂等登记、租约互斥、指数退避、死信与重放
 *
 * <p>只导入 DB + 集成 Starter 自动配置，避免加载 tenant/redis/security 等无关自动配置导致 H2 缺表。
 *
 * @author 智云集成治理
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = EventOutboxServiceTest.TestApplication.class)
@ActiveProfiles("unit-test")
@Sql(scripts = "/sql/clean.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
public class EventOutboxServiceTest {

    @Import({
            ZhiCloudDataSourceAutoConfiguration.class,
            DataSourceAutoConfiguration.class,
            DataSourceTransactionManagerAutoConfiguration.class,
            DruidDataSourceAutoConfigure.class,
            SqlInitializationTestConfiguration.class,
            ZhiCloudMybatisAutoConfiguration.class,
            MybatisPlusAutoConfiguration.class,
            MybatisPlusJoinAutoConfiguration.class,
            IntegrationAutoConfiguration.class
    })
    public static class TestApplication {
    }

    @Resource
    private EventOutboxService eventOutboxService;

    @Resource
    private EventOutboxMapper eventOutboxMapper;

    @Resource
    private IntegrationProperties properties;

    @BeforeEach
    public void setUpTenantContext() {
        TenantContextHolder.setTenantId(0L);
    }

    @AfterEach
    public void clearTenantContext() {
        TenantContextHolder.clear();
    }

    // -------------------- 登记 --------------------

    @Test
    public void testAppendIsIdempotent() {
        Long id1 = append("evt-idem-1");
        Long id2 = append("evt-idem-1");
        assertEquals(id1, id2, "同一 eventId 重复登记必须返回同一行，否则消费侧失去去重能力");
        assertEquals(1L, countAll());

        EventOutboxDO row = eventOutboxMapper.selectById(id1);
        assertEquals(OUTBOX_NEW, row.getStatus());
        assertEquals(0, row.getRetryCount());
        assertEquals("v1", row.getSchemaVersion());
        assertEquals("0:1001", row.getPartitionKey(), "未指定分区键时默认 tenantId:aggregateId");
        assertNull(row.getNextRetryAt());
        assertNotNull(row.getOccurredAt());
    }

    @Test
    public void testAppendRejectsIncompleteArgs() {
        assertServiceException(() -> eventOutboxService.append(null),
                ErrorCodeConstants.EVENT_OUTBOX_ARG_INVALID);
        assertServiceException(() -> eventOutboxService.append(EventAppendReq.builder()
                        .eventId("evt-bad-1").aggregateType("erp_sale_out").aggregateId("1001")
                        .eventType("erp.sale-out.approved.v1").payload(null).build()),
                ErrorCodeConstants.EVENT_OUTBOX_ARG_INVALID);
        assertServiceException(() -> eventOutboxService.append(EventAppendReq.builder()
                        .eventId("evt-bad-2").aggregateType("erp_sale_out").aggregateId("1001")
                        .eventType(null).payload("{}").build()),
                ErrorCodeConstants.EVENT_OUTBOX_ARG_INVALID);
        assertEquals(0L, countAll(), "校验失败的事件不应落库");
    }

    @Test
    public void testAppendRejectsNonJsonPayload() {
        assertServiceException(() -> eventOutboxService.append(EventAppendReq.builder()
                        .eventId("evt-bad-3").aggregateType("erp_sale_out").aggregateId("1001")
                        .eventType("erp.sale-out.approved.v1").payload("not a json").build()),
                ErrorCodeConstants.EVENT_OUTBOX_PAYLOAD_INVALID);
        assertEquals(0L, countAll());
    }

    // -------------------- 领取租约与 owner fencing --------------------

    @Test
    public void testClaimLeaseBlocksConcurrentTakeover() {
        Long id = append("evt-lease-1");
        LocalDateTime now = LocalDateTime.now();
        String owner = assertClaimed(id, now);
        EventOutboxDO claimed = eventOutboxMapper.selectById(id);
        assertEquals(OUTBOX_PUBLISHING, claimed.getStatus());
        assertEquals(owner, claimed.getProcessingOwner());
        assertNotNull(claimed.getLeaseUntil());

        assertNull(eventOutboxService.claim(id, now), "租约内不允许二次领取");
        assertNull(eventOutboxService.claim(id, now.plusSeconds(properties.getLeaseSeconds() - 1)));
        String takeoverOwner = assertClaimed(id, now.plusSeconds(properties.getLeaseSeconds() + 1));
        assertFalse(owner.equals(takeoverOwner), "租约过期后接管必须生成新的 owner");
    }

    @Test
    public void testStaleOwnerCannotSettleAfterTakeover() {
        Long id = append("evt-stale-owner");
        LocalDateTime now = LocalDateTime.now();
        String oldOwner = assertClaimed(id, now);
        String newOwner = assertClaimed(id, now.plusSeconds(properties.getLeaseSeconds() + 1));

        assertFalse(eventOutboxService.markPublished(id, oldOwner),
                "租约过期并接管后，旧 owner 不得把事件标记为已投递");
        eventOutboxService.markRetryOrDead(id, oldOwner, new IllegalStateException("stale"));
        EventOutboxDO afterStaleSettlement = eventOutboxMapper.selectById(id);
        assertEquals(OUTBOX_PUBLISHING, afterStaleSettlement.getStatus());
        assertEquals(0, afterStaleSettlement.getRetryCount());
        assertEquals(newOwner, afterStaleSettlement.getProcessingOwner());

        assertTrue(eventOutboxService.markPublished(id, newOwner));
        EventOutboxDO published = eventOutboxMapper.selectById(id);
        assertEquals(OUTBOX_PUBLISHED, published.getStatus());
        assertNull(published.getProcessingOwner());
        assertNull(published.getLeaseUntil());
    }

    @Test
    public void testMarkPublishedOnlyFromPublishingAndCurrentOwner() {
        Long id = append("evt-pub-1");
        assertFalse(eventOutboxService.markPublished(id, null), "未被领取的事件不能直接标记完成");
        String owner = assertClaimed(id, LocalDateTime.now());
        assertFalse(eventOutboxService.markPublished(id, "stale-owner"), "非当前 owner 不能确认事件");
        assertTrue(eventOutboxService.markPublished(id, owner));
        EventOutboxDO row = eventOutboxMapper.selectById(id);
        assertEquals(OUTBOX_PUBLISHED, row.getStatus());
        assertNotNull(row.getPublishedAt());
        assertNull(row.getProcessingOwner());
        assertNull(row.getLeaseUntil());
    }

    // -------------------- 退避与死信 --------------------

    @Test
    public void testBackoffWindowBlocksRescan() {
        Long id = append("evt-backoff-1");
        LocalDateTime before = LocalDateTime.now();
        String owner = assertClaimed(id, before);
        eventOutboxService.markRetryOrDead(id, owner, new IllegalStateException("boom"));

        EventOutboxDO row = eventOutboxMapper.selectById(id);
        assertEquals(OUTBOX_NEW, row.getStatus(), "失败后回到待投递，等待退避窗口");
        assertEquals(1, row.getRetryCount());
        assertNull(row.getProcessingOwner());
        assertNull(row.getLeaseUntil());
        long backoff = Duration.between(before, row.getNextRetryAt()).getSeconds();
        assertTrue(backoff >= 9 && backoff <= 11, "首次退避应为 baseBackoffSeconds=10，实际 " + backoff);

        assertTrue(eventOutboxService.selectClaimable(LocalDateTime.now(), 10).isEmpty(),
                "退避窗口内的事件不应被扫描到");
        assertEquals(1, eventOutboxService.selectClaimable(LocalDateTime.now().plusSeconds(11), 10).size());
    }

    @Test
    public void testBackoffGrowsExponentially() {
        Long id = append("evt-backoff-2");
        LocalDateTime t1 = LocalDateTime.now();
        String owner1 = assertClaimed(id, t1);
        eventOutboxService.markRetryOrDead(id, owner1, new IllegalStateException("boom-1"));

        LocalDateTime t2 = forceReady(id);
        String owner2 = assertClaimed(id, t2);
        eventOutboxService.markRetryOrDead(id, owner2, new IllegalStateException("boom-2"));

        long backoff = Duration.between(t2, eventOutboxMapper.selectById(id).getNextRetryAt()).getSeconds();
        assertTrue(backoff >= 19 && backoff <= 21, "第二次退避应为 base * 2 = 20，实际 " + backoff);
        assertEquals(2, eventOutboxMapper.selectById(id).getRetryCount());
    }

    @Test
    public void testRetryUntilDeadThenStop() {
        Long id = append("evt-dead-1");
        // maxRetry=3：前两次失败回到 NEW，第三次失败转 DEAD
        for (int i = 1; i <= 2; i++) {
            assertEquals(OUTBOX_NEW, eventOutboxMapper.selectById(id).getStatus());
            String owner = assertClaimed(id, forceReady(id));
            eventOutboxService.markRetryOrDead(id, owner, new IllegalStateException("fail-" + i));
        }
        assertEquals(2, eventOutboxMapper.selectById(id).getRetryCount());
        assertEquals(0L, eventOutboxService.countDead());

        String owner = assertClaimed(id, forceReady(id));
        eventOutboxService.markRetryOrDead(id, owner, new IllegalStateException("fail-3"));

        EventOutboxDO dead = eventOutboxMapper.selectById(id);
        assertEquals(OUTBOX_DEAD, dead.getStatus());
        assertEquals(3, dead.getRetryCount());
        assertNull(dead.getProcessingOwner());
        assertNull(dead.getLeaseUntil());
        assertEquals(1L, eventOutboxService.countDead());
        assertTrue(eventOutboxService.selectClaimable(LocalDateTime.now(), 10).isEmpty(),
                "死信事件不应再被扫描");
        assertFalse(eventOutboxService.markPublished(id, owner), "死信事件不应被标记为投递完成");
    }

    // -------------------- 失败原因落库 --------------------

    @Test
    public void testLastErrorTruncatedToColumnWidth() {
        Long id = append("evt-err-1");
        String owner = assertClaimed(id, LocalDateTime.now());
        eventOutboxService.markRetryOrDead(id, owner, new IllegalStateException("x".repeat(5000)));
        String lastError = eventOutboxMapper.selectById(id).getLastError();
        assertEquals(2000, lastError.length(), "超长异常信息必须截断到列宽，否则整个 UPDATE 会失败");
        assertTrue(lastError.startsWith("java.lang.IllegalStateException"));
    }

    @Test
    public void testLastErrorUsesRootCause() {
        Long id = append("evt-err-2");
        String owner = assertClaimed(id, LocalDateTime.now());
        eventOutboxService.markRetryOrDead(id, owner,
                new RuntimeException("outer", new IllegalStateException("inner")));
        String lastError = eventOutboxMapper.selectById(id).getLastError();
        assertTrue(lastError.contains("java.lang.IllegalStateException: inner"),
                "应记录根因而非最外层包装，实际：" + lastError);
        assertFalse(lastError.contains("outer"));
    }

    // -------------------- 监控口径 --------------------

    @Test
    public void testBacklogAndDeadCounters() {
        Long a = append("evt-cnt-1");
        append("evt-cnt-2");
        assertEquals(2L, eventOutboxService.countBacklog());
        String owner = assertClaimed(a, LocalDateTime.now());
        assertEquals(2L, eventOutboxService.countBacklog(), "PUBLISHING 仍在堆积口径内，避免漏报");
        assertTrue(eventOutboxService.markPublished(a, owner));
        assertEquals(1L, eventOutboxService.countBacklog());
        assertEquals(0L, eventOutboxService.countDead());

        Long b = eventOutboxMapper.selectByEventIdAndTenantId("evt-cnt-2", 0L).getId();
        makeDead(b);
        assertEquals(1L, eventOutboxService.countDead());
        assertEquals(0L, eventOutboxService.countBacklog(), "死信不再计入堆积口径，避免同一事件同时触发堆积与死信两个告警");
    }

    @Test
    public void testOldestPendingSecondsUsesOldestUnfinishedOccurredAt() {
        Long published = append("evt-age-published");
        String owner = assertClaimed(published, LocalDateTime.now());
        assertTrue(eventOutboxService.markPublished(published, owner));

        Long oldestPending = append("evt-age-pending-old");
        EventOutboxDO oldestRow = eventOutboxMapper.selectById(oldestPending);
        oldestRow.setOccurredAt(LocalDateTime.now().minusSeconds(120));
        eventOutboxMapper.updateById(oldestRow);

        Long newerPending = append("evt-age-pending-new");
        EventOutboxDO newerRow = eventOutboxMapper.selectById(newerPending);
        newerRow.setOccurredAt(LocalDateTime.now().minusSeconds(10));
        eventOutboxMapper.updateById(newerRow);

        long age = eventOutboxService.oldestPendingSeconds();
        assertTrue(age >= 115 && age <= 125,
                "最老未投递时长必须取 NEW/PUBLISHING 的最小 occurred_at，实际=" + age);
    }

    @Test
    public void testOldestPendingSecondsIsZeroWhenNothingPending() {
        Long published = append("evt-age-empty");
        String owner = assertClaimed(published, LocalDateTime.now());
        assertTrue(eventOutboxService.markPublished(published, owner));

        assertEquals(0L, eventOutboxService.oldestPendingSeconds(),
                "没有待投递事件时最老年龄必须是 0，不能把 PUBLISHED 历史事件算成堆积");
    }

    // -------------------- 辅助方法 --------------------

    private Long append(String eventId) {
        return eventOutboxService.append(EventAppendReq.builder()
                .eventId(eventId)
                .tenantId(0L)
                .aggregateType("erp_sale_out")
                .aggregateId("1001")
                .eventType("erp.sale-out.approved.v1")
                .payload("{\"id\":1001}")
                .build());
    }

    private long countAll() {
        Long count = eventOutboxMapper.selectCount(new LambdaQueryWrapper<EventOutboxDO>());
        return count == null ? 0L : count;
    }

    /**
     * 把事件强制拉回「立即可领取」状态：单测不能真的等退避窗口过去，
     * 直接把 next_retry_at 拨到过去，等价于窗口已到期。
     */
    private LocalDateTime forceReady(Long id) {
        EventOutboxDO row = eventOutboxMapper.selectById(id);
        row.setStatus(OUTBOX_NEW);
        // 拨到过去 5 秒而不是「恰好当下」：H2/MySQL 的时间精度舍入可能让存储值比传入参数
        // 略大一点点，导致 next_retry_at <= now 判定抖动，留出秒级余量避免测试自身不稳定
        row.setNextRetryAt(LocalDateTime.now().minusSeconds(5));
        eventOutboxMapper.updateById(row);
        return LocalDateTime.now();
    }

    private String assertClaimed(Long id, LocalDateTime now) {
        String owner = eventOutboxService.claim(id, now);
        assertNotNull(owner, "claim 成功时必须返回本次 owner");
        return owner;
    }

    private void makeDead(Long id) {
        for (int i = 0; i < properties.getMaxRetry(); i++) {
            String owner = assertClaimed(id, forceReady(id));
            eventOutboxService.markRetryOrDead(id, owner, new IllegalStateException("fail"));
        }
        assertEquals(OUTBOX_DEAD, eventOutboxMapper.selectById(id).getStatus());
    }

}
