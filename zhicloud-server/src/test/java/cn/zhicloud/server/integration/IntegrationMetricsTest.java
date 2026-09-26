package cn.zhicloud.server.integration;

import cn.zhicloud.framework.integration.service.EventInboxService;
import cn.zhicloud.framework.integration.service.EventOutboxService;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Outbox/Inbox 可观测性接线测试：既验证 Spring 扫描/装配，也验证 Gauge 每次抓取读取当前值。
 */
public class IntegrationMetricsTest {

    private final AtomicLong outboxBacklog = new AtomicLong(12L);
    private final AtomicLong outboxDead = new AtomicLong(3L);
    private final AtomicLong outboxOldestPendingSeconds = new AtomicLong(180L);
    private final AtomicLong inboxFailed = new AtomicLong(7L);
    private final AtomicLong inboxDead = new AtomicLong(2L);
    private final AtomicLong inboxOldestPendingSeconds = new AtomicLong(90L);

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfiguration.class)
            .withBean(EventOutboxService.class, this::outboxService)
            .withBean(EventInboxService.class, this::inboxService)
            .withBean(MeterRegistry.class, SimpleMeterRegistry::new);

    @Test
    public void testSpringContextRegistersGaugesAndReadsCurrentValues() {
        contextRunner.run(context -> {
            MeterRegistry registry = context.getBean(MeterRegistry.class);

            assertEquals(12D, registry.get(IntegrationMetrics.METRIC_OUTBOX_BACKLOG).gauge().value());
            assertEquals(3D, registry.get(IntegrationMetrics.METRIC_OUTBOX_DEAD).gauge().value());
            assertEquals(180D, registry.get(IntegrationMetrics.METRIC_OUTBOX_OLDEST_PENDING_SECONDS).gauge().value());
            assertEquals(7D, registry.get(IntegrationMetrics.METRIC_INBOX_FAILED).gauge().value());
            assertEquals(2D, registry.get(IntegrationMetrics.METRIC_INBOX_DEAD).gauge().value());
            assertEquals(90D, registry.get(IntegrationMetrics.METRIC_INBOX_OLDEST_PENDING_SECONDS).gauge().value());

            outboxBacklog.set(20L);
            outboxDead.set(5L);
            outboxOldestPendingSeconds.set(240L);
            inboxFailed.set(9L);
            inboxDead.set(4L);
            inboxOldestPendingSeconds.set(150L);

            assertEquals(20D, registry.get(IntegrationMetrics.METRIC_OUTBOX_BACKLOG).gauge().value(),
                    "Gauge 必须在每次抓取时读取当前值，不能在注册时快照");
            assertEquals(5D, registry.get(IntegrationMetrics.METRIC_OUTBOX_DEAD).gauge().value());
            assertEquals(240D, registry.get(IntegrationMetrics.METRIC_OUTBOX_OLDEST_PENDING_SECONDS).gauge().value());
            assertEquals(9D, registry.get(IntegrationMetrics.METRIC_INBOX_FAILED).gauge().value());
            assertEquals(4D, registry.get(IntegrationMetrics.METRIC_INBOX_DEAD).gauge().value());
            assertEquals(150D, registry.get(IntegrationMetrics.METRIC_INBOX_OLDEST_PENDING_SECONDS).gauge().value());
        });
    }

    @Test
    public void testPrometheusExpositionUsesRuleMetricNames() {
        PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        new IntegrationMetrics(outboxService(), inboxService(), registry);

        String scrape = registry.scrape();
        assertTrue(scrape.contains("zhicloud_integration_outbox_backlog "),
                "Prometheus 规则依赖点号转下划线后的指标名，实际输出：\n" + scrape);
        assertTrue(scrape.contains("zhicloud_integration_outbox_dead "));
        assertTrue(scrape.contains("zhicloud_integration_outbox_oldest_pending_seconds "));
        assertTrue(scrape.contains("zhicloud_integration_inbox_failed "));
        assertTrue(scrape.contains("zhicloud_integration_inbox_dead "));
        assertTrue(scrape.contains("zhicloud_integration_inbox_oldest_pending_seconds "));
    }

    @Test
    public void testSpringContextDoesNotRegisterGaugesWhenIntegrationDisabled() {
        contextRunner.withPropertyValues("zhicloud.integration.enabled=false").run(context -> {
            MeterRegistry registry = context.getBean(MeterRegistry.class);

            assertNull(registry.find(IntegrationMetrics.METRIC_OUTBOX_BACKLOG).gauge());
            assertNull(registry.find(IntegrationMetrics.METRIC_OUTBOX_DEAD).gauge());
            assertNull(registry.find(IntegrationMetrics.METRIC_OUTBOX_OLDEST_PENDING_SECONDS).gauge());
            assertNull(registry.find(IntegrationMetrics.METRIC_INBOX_FAILED).gauge());
            assertNull(registry.find(IntegrationMetrics.METRIC_INBOX_DEAD).gauge());
            assertNull(registry.find(IntegrationMetrics.METRIC_INBOX_OLDEST_PENDING_SECONDS).gauge());
        });
    }

    private EventOutboxService outboxService() {
        EventOutboxService service = mock(EventOutboxService.class);
        when(service.countBacklog()).thenAnswer(invocation -> outboxBacklog.get());
        when(service.countDead()).thenAnswer(invocation -> outboxDead.get());
        when(service.oldestPendingSeconds()).thenAnswer(invocation -> outboxOldestPendingSeconds.get());
        return service;
    }

    private EventInboxService inboxService() {
        EventInboxService service = mock(EventInboxService.class);
        when(service.countFailed()).thenAnswer(invocation -> inboxFailed.get());
        when(service.countDead()).thenAnswer(invocation -> inboxDead.get());
        when(service.oldestPendingSeconds()).thenAnswer(invocation -> inboxOldestPendingSeconds.get());
        return service;
    }

    @Configuration(proxyBeanMethods = false)
    @ComponentScan("cn.zhicloud.server.integration")
    static class TestConfiguration {
    }

}
