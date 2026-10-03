package cn.zhicloud.framework.integration.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 可靠集成数据面配置（V100）
 *
 * @author 智云集成治理
 */
@ConfigurationProperties(prefix = "zhicloud.integration")
@Data
public class IntegrationProperties {

    /**
     * 是否启用集成 Starter，默认启用
     */
    private Boolean enabled = true;

    /**
     * 最大重试次数：第 N 次失败且 {@code retryCount + 1 >= maxRetry} 时进入死信。
     * <p>即 maxRetry=3 时允许 2 次自动重试，第 3 次失败转 DEAD。
     */
    private Integer maxRetry = 5;

    /**
     * 首次重试的退避秒数
     */
    private Long baseBackoffSeconds = 5L;

    /**
     * 退避上限秒数（指数退避的封顶值，避免第 10 次重试要等几个小时）
     */
    private Long maxBackoffSeconds = 300L;

    /**
     * 领取租约秒数：事件被领取后在该窗口内不会被再次领取。
     * <p>作用是把「崩溃恢复」与「并发互斥」分开：租约内不可重复领取（互斥），
     * 租约过期后 PUBLISHING 事件重新可见（崩溃恢复）。取值必须大于单条事件的处理耗时上限，
     * 否则会出现两个节点同时处理同一事件。
     */
    private Long leaseSeconds = 60L;

    /**
     * 收件箱处理租约秒数：PROCESSING 记录在该窗口内不得被其他实例接管，
     * 过期后允许接管以恢复崩溃/失联节点留下的残留。该值必须覆盖单条消费者处理耗时上限。
     */
    private Long inboxLeaseSeconds = 60L;

    /**
     * 单次中继扫描的批大小
     */
    private Integer batchSize = 50;

    /**
     * 是否注册进程内中继定时任务。
     * <p>默认开启：没有排空机制的事件表只是「记下来」，不会真的驱动跨模块集成。
     * 关闭后必须由宿主应用用自己的调度框架调用 {@code EventRelayService#relayOnce(int)}，
     * 否则 event_outbox 会无限堆积（监控项 {@code countBacklog} 会持续上升）。
     */
    private Boolean relayEnabled = true;

    /**
     * 中继定时任务首次执行的延迟毫秒数：等待数据源、缓存等就绪后再开始扫描
     */
    private Long relayInitialDelay = 10000L;

    /**
     * 中继定时任务两轮之间的间隔毫秒数（上一轮结束后开始计时，避免叠加执行）
     */
    private Long relayFixedDelay = 5000L;

}