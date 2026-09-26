package cn.zhicloud.server.integration;

import cn.zhicloud.framework.integration.service.EventOutboxService;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Outbox 运行指标接线。
 *
 * <p>指标值由 Gauge 在每次抓取时读取，避免注册时快照成固定值。这里只负责
 * 把领域服务已有的事实暴露给 Micrometer；指标注册失败不应改变业务语义。
 */
@Component
@ConditionalOnClass(MeterRegistry.class)
@ConditionalOnProperty(prefix = "zhicloud.integration", name = "enabled", havingValue = "true", matchIfMissing = true)
public class IntegrationMetrics {

    public static final String METRIC_BACKLOG = "zhicloud.integration.outbox.backlog";
    public static final String METRIC_DEAD = "zhicloud.integration.outbox.dead";
    public static final String METRIC_OLDEST_PENDING_SECONDS = "zhicloud.integration.outbox.oldest-pending-seconds";

    public IntegrationMetrics(EventOutboxService outboxService, MeterRegistry registry) {
        Gauge.builder(METRIC_BACKLOG, outboxService, EventOutboxService::countBacklog)
                .description("待投递事件堆积量（NEW + PUBLISHING）")
                .register(registry);
        Gauge.builder(METRIC_DEAD, outboxService, EventOutboxService::countDead)
                .description("死信事件数量")
                .register(registry);
        Gauge.builder(METRIC_OLDEST_PENDING_SECONDS, outboxService, EventOutboxService::oldestPendingSeconds)
                .description("最老未投递事件已等待秒数（NEW + PUBLISHING）")
                .register(registry);
    }
}
