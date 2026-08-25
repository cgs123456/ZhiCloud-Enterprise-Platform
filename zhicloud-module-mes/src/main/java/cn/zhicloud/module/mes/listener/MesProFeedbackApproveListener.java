package cn.zhicloud.module.mes.listener;

import cn.zhicloud.module.mes.service.pro.piecework.MesProPieceworkRecordService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 生产报工审批通过监听器
 *
 * <p>监听 {@link MesProFeedbackApprovedEvent}，在报工审批通过且事务提交后，
 * 调用 {@code MesProPieceworkRecordService#generateRecordFromFeedback} 生成计件工资明细。
 *
 * <p>容错策略：
 * <ul>
 *   <li>使用 {@code @TransactionalEventListener(AFTER_COMMIT)} 确保报工已落库</li>
 *   <li>异常被捕获并记录日志，不影响报工审批主流程</li>
 *   <li>未匹配到计件规则时仅记录 warn 日志，不抛出异常</li>
 * </ul>
 *
 * @author 智云
 */
@Component
@Slf4j
public class MesProFeedbackApproveListener {

    /**
     * 生成计件明细的最大尝试次数（含首次）
     */
    private static final int MAX_GENERATE_RETRY = 3;

    @Resource
    private MesProPieceworkRecordService pieceworkRecordService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onFeedbackApproved(MesProFeedbackApprovedEvent event) {
        if (event == null || event.getFeedbackId() == null) {
            return;
        }
        // 重试 3 次，间隔递增 1s/2s/4s：AFTER_COMMIT 阶段失败不再有事务兜底，重试降低计件工资丢失概率
        long backoffMillis = 1000L;
        for (int attempt = 1; attempt <= MAX_GENERATE_RETRY; attempt++) {
            try {
                Long recordId = pieceworkRecordService.generateRecordFromFeedback(event.getFeedbackId());
                if (recordId != null) {
                    log.info("[onFeedbackApproved][报工单 {} 生成计件明细 {}]", event.getFeedbackId(), recordId);
                }
                return;
            } catch (Exception e) {
                if (attempt == MAX_GENERATE_RETRY) {
                    // 固定前缀 [PIECEWISE-WAGE-LOSS] 便于告警匹配与人工补偿
                    log.error("[PIECEWISE-WAGE-LOSS][报工单 {} 生成计件明细失败，已重试 {} 次]",
                            event.getFeedbackId(), MAX_GENERATE_RETRY, e);
                    return;
                }
                log.warn("[onFeedbackApproved][报工单 {} 生成计件明细失败，第 {} 次尝试，{}ms 后重试]",
                        event.getFeedbackId(), attempt, backoffMillis, e);
                try {
                    Thread.sleep(backoffMillis);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    log.error("[PIECEWISE-WAGE-LOSS][报工单 {} 生成计件明细被中断，剩余重试放弃]",
                            event.getFeedbackId(), ie);
                    return;
                }
                backoffMillis *= 2;
            }
        }
    }
}
