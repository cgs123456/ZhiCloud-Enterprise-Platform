package cn.zhicloud.server.integration;

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
 * Outbox 可观测性接线测试：既验证 Spring 扫描/装配，也验证 Gauge 每次抓取读取当前值。
 */
public class IntegrationMetricsTest {

    private final AtomicLong backlog = new AtomicLong(12L);
    private final AtomicLong dead = new AtomicLong(3L);
    private final AtomicLong oldestPendingSeconds = new AtomicLong(180L);

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(TestConfiguration.class)
            .withBean(EventOutboxService.class, this::outboxService)
            .withBean(MeterRegistry.class, SimpleMeterRegistry::new);

    @Test
    public void testSpringContextRegistersGaugesAndReadsCurrentValues() {
        contextRunner.run(context -> {
            MeterRegistry registry = context.getBean(MeterRegistry.class);

            assertEquals(12D, registry.get(IntegrationMetrics.METRIC_BACKLOG).gauge().value());
            assertEquals(3D, registry.get(IntegrationMetrics.METRIC_DEAD).gauge().value());
            assertEquals(180D, registry.get(IntegrationMetrics.METRIC_OLDEST_PENDING_SECONDS).gauge().value());

            backlog.set(20L);
            dead.set(5L);
            oldestPendingSeconds.set(240L);

            assertEquals(20D, registry.get(IntegrationMetrics.METRIC_BACKLOG).gauge().value(),
                    "Gauge 必须在每次抓取时读取当前值，不能在注册时快照");
            assertEquals(5D, registry.get(IntegrationMetrics.METRIC_DEAD).gauge().value());
            assertEquals(240D, registry.get(IntegrationMetrics.METRIC_OLDEST_PENDING_SECONDS).gauge().value());
        });
    }

    @Test
    public void testPrometheusExpositionUsesRuleMetricNames() {
        PrometheusMeterRegistry registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        new IntegrationMetrics(outboxService(), registry);

        String scrape = registry.scrape();
        assertTrue(scrape.contains("zhicloud_integration_outbox_backlog "),
                "Prometheus 规则依赖点号转下划线后的指标名，实际输出：\n" + scrape);
        assertTrue(scrape.contains("zhicloud_integration_outbox_dead "));
        assertTrue(scrape.contains("zhicloud_integration_outbox_oldest_pending_seconds "));
    }

    @Test
    public void testSpringContextDoesNotRegisterGaugesWhenIntegrationDisabled() {
        contextRunner.withPropertyValues("zhicloud.integration.enabled=false").run(context -> {
            MeterRegistry registry = context.getBean(MeterRegistry.class);

            assertNull(registry.find(IntegrationMetrics.METRIC_BACKLOG).gauge());
            assertNull(registry.find(IntegrationMetrics.METRIC_DEAD).gauge());
            assertNull(registry.find(IntegrationMetrics.METRIC_OLDEST_PENDING_SECONDS).gauge());
        });
    }

    private EventOutboxService outboxService() {
        EventOutboxService service = mock(EventOutboxService.class);
        when(service.countBacklog()).thenAnswer(invocation -> backlog.get());
        when(service.countDead()).thenAnswer(invocation -> dead.get());
        when(service.oldestPendingSeconds()).thenAnswer(invocation -> oldestPendingSeconds.get());
        return service;
    }

    @Configuration(proxyBeanMethods = false)
    @ComponentScan("cn.zhicloud.server.integration")
    static class TestConfiguration {
    }

}
