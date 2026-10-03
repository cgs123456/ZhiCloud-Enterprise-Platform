package cn.zhicloud.server.integration;

import cn.zhicloud.framework.integration.service.EventInboxService;
import cn.zhicloud.framework.integration.service.EventOutboxService;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Outbox/Inbox 运行指标接线。
 *
 * <p>指标值由 Gauge 在每次抓取时读取，避免注册时快照成固定值。这里只负责
 * 把领域服务已有的事实暴露给 Micrometer；指标注册失败不应改变业务语义。
 *
 * <p>失败指标是当前数据库状态存量，不是时间窗口失败率；后者需要累计 Counter。
 */
@Component
@ConditionalOnClass(MeterRegistry.class)
@ConditionalOnProperty(prefix = "zhicloud.integration", name = "enabled", havingValue = "true", matchIfMissing = true)
public class IntegrationMetrics {

    public static final String METRIC_OUTBOX_BACKLOG = "zhicloud.integration.outbox.backlog";
    public static final String METRIC_OUTBOX_DEAD = "zhicloud.integration.outbox.dead";
    public static final String METRIC_OUTBOX_OLDEST_PENDING_SECONDS = "zhicloud.integration.outbox.oldest-pending-seconds";
    public static final String METRIC_INBOX_FAILED = "zhicloud.integration.inbox.failed";
    public static final String METRIC_INBOX_DEAD = "zhicloud.integration.inbox.dead";
    public static final String METRIC_INBOX_OLDEST_PENDING_SECONDS = "zhicloud.integration.inbox.oldest-pending-seconds";

    /**
     * @deprecated 使用 {@link #METRIC_OUTBOX_BACKLOG}，保留旧常量避免外部代码编译中断。
     */
    @Deprecated
    public static final String METRIC_BACKLOG = METRIC_OUTBOX_BACKLOG;
    /**
     * @deprecated 使用 {@link #METRIC_OUTBOX_DEAD}，保留旧常量避免外部代码编译中断。
     */
    @Deprecated
    public static final String METRIC_DEAD = METRIC_OUTBOX_DEAD;
    /**
     * @deprecated 使用 {@link #METRIC_OUTBOX_OLDEST_PENDING_SECONDS}，保留旧常量避免外部代码编译中断。
     */
    @Deprecated
    public static final String METRIC_OLDEST_PENDING_SECONDS = METRIC_OUTBOX_OLDEST_PENDING_SECONDS;

    public IntegrationMetrics(EventOutboxService outboxService, EventInboxService inboxService, MeterRegistry registry) {
        Gauge.builder(METRIC_OUTBOX_BACKLOG, outboxService, EventOutboxService::countBacklog)
                .description("待投递事件堆积量（NEW + PUBLISHING）")
                .register(registry);
        Gauge.builder(METRIC_OUTBOX_DEAD, outboxService, EventOutboxService::countDead)
                .description("发件箱死信事件数量")
                .register(registry);
        Gauge.builder(METRIC_OUTBOX_OLDEST_PENDING_SECONDS, outboxService, EventOutboxService::oldestPendingSeconds)
                .description("最老未投递事件已等待秒数（NEW + PUBLISHING）")
                .register(registry);
        Gauge.builder(METRIC_INBOX_FAILED, inboxService, EventInboxService::countFailed)
                .description("收件箱当前待重试失败存量（FAILED，不是时间窗口失败率）")
                .register(registry);
        Gauge.builder(METRIC_INBOX_DEAD, inboxService, EventInboxService::countDead)
                .description("收件箱死信事件数量")
                .register(registry);
        Gauge.builder(METRIC_INBOX_OLDEST_PENDING_SECONDS, inboxService, EventInboxService::oldestPendingSeconds)
                .description("最老待消费事件已等待秒数（RECEIVED + FAILED）")
                .register(registry);
    }
}
