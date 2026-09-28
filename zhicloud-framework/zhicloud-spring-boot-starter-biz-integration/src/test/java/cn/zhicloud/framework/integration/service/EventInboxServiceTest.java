package cn.zhicloud.framework.integration.service;

import cn.zhicloud.framework.common.exception.ServiceException;
import cn.zhicloud.framework.datasource.config.ZhiCloudDataSourceAutoConfiguration;
import cn.zhicloud.framework.integration.api.ConsumeResult;
import cn.zhicloud.framework.integration.config.IntegrationAutoConfiguration;
import cn.zhicloud.framework.integration.config.IntegrationProperties;
import cn.zhicloud.framework.integration.dal.dataobject.EventInboxDO;
import cn.zhicloud.framework.integration.dal.mysql.EventInboxMapper;
import cn.zhicloud.framework.integration.enums.ErrorCodeConstants;
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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.util.concurrent.atomic.AtomicInteger;

import static cn.zhicloud.framework.integration.enums.EventStatusConstants.INBOX_DEAD;
import static cn.zhicloud.framework.integration.enums.EventStatusConstants.INBOX_FAILED;
import static cn.zhicloud.framework.integration.enums.EventStatusConstants.INBOX_PROCESSED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * 事件收件箱测试（V100）：幂等去重、失败重试、死信与人工重放
 *
 * @author 智云集成治理
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = EventInboxServiceTest.TestApplication.class)
@ActiveProfiles("unit-test")
@Sql(scripts = "/sql/clean.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
public class EventInboxServiceTest {

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
    private EventInboxService eventInboxService;

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

    @Resource
    private EventInboxMapper eventInboxMapper;

    @Test
    public void testConsumeOnceThenDuplicate() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        assertEquals(ConsumeResult.PROCESSED, consume("c1", "e1", calls));
        assertEquals(ConsumeResult.DUPLICATE, consume("c1", "e1", calls),
                "同一消费者重复收到同一事件时必须跳过");
        assertEquals(1, calls.get(), "去重后业务处理器只应执行一次");

        EventInboxDO row = eventInboxService.selectByConsumerAndEvent("c1", "e1", 0L);
        assertEquals(INBOX_PROCESSED, row.getStatus());
        assertNotNull(row.getProcessedAt());
        assertEquals(0, row.getRetryCount());
    }

    @Test
    public void testProcessedRowCannotBeClaimedAgain() throws Exception {
        // 服务层 PROCESSED 短路是第一道防线；claim() 的 status 前置条件是第二道防线。
        // 这里单独验证第二道防线：即使上层判断被改写，已完成的记录也永远抢不到执行权，
        // 否则「恰好一次」会退化成「至少一次」，重复扣减 / 重复过账将不可控。
        AtomicInteger calls = new AtomicInteger();
        assertEquals(ConsumeResult.PROCESSED, consume("c-dup", "e-dup", calls));
        EventInboxDO processed = eventInboxService.selectByConsumerAndEvent("c-dup", "e-dup", 0L);
        assertEquals(INBOX_PROCESSED, processed.getStatus());

        assertEquals(0, eventInboxMapper.claim(processed.getId(), 0L), "已完成的记录不允许被再次抢占");
        assertEquals(INBOX_PROCESSED, eventInboxService.selectByConsumerAndEvent("c-dup", "e-dup", 0L).getStatus());
        assertEquals(1, calls.get());
    }

    @Test
    public void testDifferentConsumersAreIndependent() throws Exception {
        AtomicInteger a = new AtomicInteger();
        AtomicInteger b = new AtomicInteger();
        assertEquals(ConsumeResult.PROCESSED, eventInboxService.consume("c-a", "e2", "t", 0L, () -> a.incrementAndGet()));
        assertEquals(ConsumeResult.PROCESSED, eventInboxService.consume("c-b", "e2", "t", 0L, () -> b.incrementAndGet()));
        assertEquals(1, a.get());
        assertEquals(1, b.get(), "不同消费者各有一份去重记录，互不干扰");
    }

    @Test
    public void testFailureCanBeRetried() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        // 首次失败：异常必须原样抛回调用方，由调用方决定重试策略
        assertThrows(IllegalStateException.class, () -> eventInboxService.consume("c3", "e3", "t", 0L, () -> {
            calls.incrementAndGet();
            throw new IllegalStateException("boom");
        }));
        EventInboxDO failed = eventInboxService.selectByConsumerAndEvent("c3", "e3", 0L);
        assertEquals(INBOX_FAILED, failed.getStatus());
        assertEquals(1, failed.getRetryCount());
        assertNotNull(failed.getError());

        // 重投成功：FAILED 状态允许再次执行
        assertEquals(ConsumeResult.PROCESSED, eventInboxService.consume("c3", "e3", "t", 0L, () -> calls.incrementAndGet()));
        assertEquals(2, calls.get());
        assertEquals(INBOX_PROCESSED, eventInboxService.selectByConsumerAndEvent("c3", "e3", 0L).getStatus());
    }

    @Test
    public void testFailureReachesDeadThenRejects() throws Exception {
        for (int i = 1; i <= properties.getMaxRetry(); i++) {
            assertThrows(IllegalStateException.class, () -> eventInboxService.consume("c4", "e4", "t", 0L, () -> {
                throw new IllegalStateException("boom");
            }));
        }
        assertEquals(INBOX_DEAD, eventInboxService.selectByConsumerAndEvent("c4", "e4", 0L).getStatus());
        // 死信必须显式拒绝而不是静默跳过：否则中继会把事件当消费成功，故障彻底消失在监控之外
        assertInboxDead("c4", "e4");
    }

    @Test
    public void testReplayDeadInbox() throws Exception {
        for (int i = 1; i <= properties.getMaxRetry(); i++) {
            try {
                eventInboxService.consume("c5", "e5", "t", 0L, () -> {
                    throw new IllegalStateException("boom");
                });
                fail("应抛出异常");
            } catch (IllegalStateException expected) {
                // 预期内
            }
        }
        assertTrue(eventInboxService.replay("c5", "e5", 0L));
        EventInboxDO row = eventInboxService.selectByConsumerAndEvent("c5", "e5", 0L);
        assertEquals(INBOX_FAILED, row.getStatus());
        assertEquals(0, row.getRetryCount());

        assertEquals(ConsumeResult.PROCESSED, eventInboxService.consume("c5", "e5", "t", 0L, () -> {
        }));
        assertEquals(INBOX_PROCESSED, eventInboxService.selectByConsumerAndEvent("c5", "e5", 0L).getStatus());
        assertFalse(eventInboxService.replay("c5", "e5", 0L), "非死信记录不应被重放");
    }

    @Test
    public void testConsumeRejectsBlankArgs() {
        try {
            eventInboxService.consume("", "e6", "t", 0L, () -> {
            });
            fail("消费者名称为空时应拒绝");
        } catch (ServiceException ex) {
            assertEquals(ErrorCodeConstants.EVENT_OUTBOX_ARG_INVALID.getCode(), ex.getCode());
        } catch (Exception ex) {
            fail("期望 ServiceException，实际：" + ex);
        }
    }

    @Test
    public void testMonitoringCountsAndOldestPendingSeconds() throws Exception {
        assertEquals(0L, eventInboxService.countFailed());
        assertEquals(0L, eventInboxService.countDead());
        assertEquals(0L, eventInboxService.oldestPendingSeconds());

        assertThrows(IllegalStateException.class, () -> eventInboxService.consume(
                "c-monitor-failed", "e-monitor-failed", "t", 0L, () -> {
                    throw new IllegalStateException("monitor-boom");
                }));
        assertEquals(1L, eventInboxService.countFailed());
        assertEquals(0L, eventInboxService.countDead());

        EventInboxDO failed = eventInboxService.selectByConsumerAndEvent("c-monitor-failed", "e-monitor-failed", 0L);
        failed.setReceivedAt(java.time.LocalDateTime.now().minusSeconds(120));
        eventInboxMapper.updateById(failed);
        long oldest = eventInboxService.oldestPendingSeconds();
        assertTrue(oldest >= 119L && oldest <= 125L, "最老待消费时长应接近 120 秒，实际：" + oldest);

        AtomicInteger processedCalls = new AtomicInteger();
        assertEquals(ConsumeResult.PROCESSED, consume("c-monitor-processed", "e-monitor-processed", processedCalls));
        assertEquals(1L, eventInboxService.countFailed(), "PROCESSED 不应计入失败存量");
        assertEquals(1, processedCalls.get());

        for (int i = 1; i <= properties.getMaxRetry(); i++) {
            assertThrows(IllegalStateException.class, () -> eventInboxService.consume(
                    "c-monitor-dead", "e-monitor-dead", "t", 0L, () -> {
                        throw new IllegalStateException("monitor-dead");
                    }));
        }
        assertEquals(1L, eventInboxService.countDead());
        assertEquals(1L, eventInboxService.countFailed(), "DEAD 必须单独统计，不能混入 FAILED 存量");

        EventInboxDO dead = eventInboxService.selectByConsumerAndEvent("c-monitor-dead", "e-monitor-dead", 0L);
        dead.setReceivedAt(java.time.LocalDateTime.now().minusSeconds(600));
        eventInboxMapper.updateById(dead);
        oldest = eventInboxService.oldestPendingSeconds();
        assertTrue(oldest >= 119L && oldest <= 125L,
                "DEAD 已停止自动消费，不应成为最老待消费项，实际：" + oldest);
    }
    private ConsumeResult consume(String consumer, String eventId, AtomicInteger counter) throws Exception {
        return eventInboxService.consume(consumer, eventId, "erp.sale-out.approved.v1", 0L, () -> counter.incrementAndGet());
    }

    private void assertInboxDead(String consumer, String eventId) {
        try {
            eventInboxService.consume(consumer, eventId, "t", 0L, null);
            fail("死信事件应当拒绝继续消费");
        } catch (ServiceException ex) {
            assertEquals(ErrorCodeConstants.EVENT_INBOX_DEAD.getCode(), ex.getCode());
        } catch (Exception ex) {
            fail("期望 ServiceException，实际：" + ex);
        }
    }

}
