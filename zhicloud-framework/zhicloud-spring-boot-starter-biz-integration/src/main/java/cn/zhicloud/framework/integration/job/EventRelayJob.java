package cn.zhicloud.framework.integration.job;

import cn.zhicloud.framework.integration.service.EventRelayService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * 事件中继定时任务（V100）
 *
 * <p>存在的理由：发件箱只登记不排空等于没有落地——事件会一直堆在 NEW，跨模块集成永远不发生。
 * 定时周期由配置给出，默认 5 秒一轮；多节点同时调度是安全的，因为领取动作带租约
 * （见 {@code EventOutboxMapper#claim}），同一事件在同一时刻只会被一个节点拿到执行权。
 *
 * <p>异常在这里被吞掉并记日志：单条事件的失败已由 {@link EventRelayService#relayOnce(int)}
 * 内部按事件维度捕获并转入重试/死信；能冒泡到这里的只有数据库不可用等基础设施故障，
 * 这类故障不应让调度线程终止，下一轮继续尝试即可（同时由监控盯住 error 日志与堆积量）。
 *
 * @author 智云集成治理
 */
@Slf4j
public class EventRelayJob {

    @Resource
    private EventRelayService eventRelayService;

    /**
     * 一轮中继扫描
     *
     * @return 本轮标记投递完成的事件数，异常时为 {@code -1}
     */
    @Scheduled(initialDelayString = "${zhicloud.integration.relay-initial-delay:10000}",
            fixedDelayString = "${zhicloud.integration.relay-fixed-delay:5000}")
    public int execute() {
        try {
            int published = eventRelayService.relayOnce(0);
            if (published > 0) {
                log.info("[EventRelayJob][本轮投递完成 {} 条事件]", published);
            }
            return published;
        } catch (Exception ex) {
            // 基础设施故障只记日志：调度线程必须活着，否则堆积会在无人察觉的情况下增长
            log.error("[EventRelayJob][中继扫描失败，等待下一轮重试]", ex);
            return -1;
        }
    }

}