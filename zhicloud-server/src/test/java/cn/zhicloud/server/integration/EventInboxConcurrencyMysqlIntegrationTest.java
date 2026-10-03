package cn.zhicloud.server.integration;

import cn.zhicloud.framework.datasource.config.ZhiCloudDataSourceAutoConfiguration;
import cn.zhicloud.framework.integration.api.ConsumeResult;
import cn.zhicloud.framework.integration.config.IntegrationAutoConfiguration;
import cn.zhicloud.framework.integration.service.EventInboxService;
import cn.zhicloud.framework.mybatis.config.ZhiCloudMybatisAutoConfiguration;
import cn.zhicloud.framework.tenant.core.context.TenantContextHolder;
import com.alibaba.druid.spring.boot3.autoconfigure.DruidDataSourceAutoConfigure;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.github.yulichang.autoconfigure.MybatisPlusJoinAutoConfiguration;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static cn.zhicloud.framework.integration.enums.EventStatusConstants.INBOX_PROCESSED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 真实 MySQL 并发收件箱回归：首次并发投递只能执行一次业务处理器。
 *
 * <p>该用例专门锁定 V100 当前的 CAS 漏洞：首次插入后直接执行 handler，而
 * {@code claim} 只把状态从 RECEIVED/FAILED 写回 RECEIVED，因此另一个线程可以再次抢到并执行。
 */
@EnabledIfSystemProperty(named = "zhicloud.mysql.integration", matches = "true",
        disabledReason = "真实 MySQL 集成测试需显式开启")
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = EventInboxConcurrencyMysqlIntegrationTest.TestApplication.class,
        properties = {
                "spring.config.location=classpath:/application-integration-mysql.yaml",
                "spring.main.banner-mode=off",
                "spring.flyway.enabled=false",
                "spring.sql.init.mode=never",
                "zhicloud.integration.relay-enabled=false"
        })
public class EventInboxConcurrencyMysqlIntegrationTest {

    private static final Long TENANT_ID = 99L;

    @Import({
            ZhiCloudDataSourceAutoConfiguration.class,
            DataSourceAutoConfiguration.class,
            DataSourceTransactionManagerAutoConfiguration.class,
            JdbcTemplateAutoConfiguration.class,
            DruidDataSourceAutoConfigure.class,
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
    private JdbcTemplate jdbcTemplate;

    private String consumerName;
    private String eventId;

    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId(TENANT_ID);
        consumerName = "mysql-concurrent-" + UUID.randomUUID();
        eventId = "mysql-event-" + UUID.randomUUID();
        cleanup();
    }

    @AfterEach
    void tearDown() {
        try {
            cleanup();
        } finally {
            TenantContextHolder.clear();
        }
    }

    @Test
    void testConcurrentFirstDeliveryExecutesHandlerOnce() throws Exception {
        AtomicInteger handlerCalls = new AtomicInteger();
        CountDownLatch firstHandlerEntered = new CountDownLatch(1);
        CountDownLatch releaseFirstHandler = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<ConsumeResult> first = executor.submit(() -> eventInboxService.consume(
                    consumerName, eventId, "erp.sale-out.approved.v1", TENANT_ID, () -> {
                        handlerCalls.incrementAndGet();
                        firstHandlerEntered.countDown();
                        await(releaseFirstHandler);
                    }));
            assertTrue(firstHandlerEntered.await(10, TimeUnit.SECONDS),
                    "第一线程应已进入业务处理器，否则测试时序无效");

            Future<ConsumeResult> second = executor.submit(() -> eventInboxService.consume(
                    consumerName, eventId, "erp.sale-out.approved.v1", TENANT_ID, handlerCalls::incrementAndGet));

            ConsumeResult secondResult = second.get(10, TimeUnit.SECONDS);
            releaseFirstHandler.countDown();
            ConsumeResult firstResult = first.get(10, TimeUnit.SECONDS);

            assertEquals(ConsumeResult.PROCESSED, firstResult);
            assertEquals(ConsumeResult.IN_PROGRESS, secondResult,
                    "第二线程不得在首次投递仍处理中时再次执行业务处理器");
            assertEquals(1, handlerCalls.get(), "并发首次投递只能执行一次业务处理器");
            assertEquals(INBOX_PROCESSED, jdbcTemplate.queryForObject(
                    "SELECT status FROM event_inbox WHERE tenant_id = ? AND consumer_name = ? AND event_id = ?",
                    String.class, TENANT_ID, consumerName, eventId));
        } finally {
            releaseFirstHandler.countDown();
            executor.shutdownNow();
            executor.awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    private void cleanup() {
        jdbcTemplate.update("DELETE FROM event_inbox WHERE tenant_id = ? AND consumer_name = ? AND event_id = ?",
                TENANT_ID, consumerName, eventId);
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("等待并发测试 latch 超时");
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("并发测试线程被中断", ex);
        }
    }
}