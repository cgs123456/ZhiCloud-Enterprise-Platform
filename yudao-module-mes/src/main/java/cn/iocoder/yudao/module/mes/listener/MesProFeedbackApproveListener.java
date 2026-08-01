package cn.iocoder.yudao.module.mes.listener;

import cn.iocoder.yudao.module.mes.service.pro.piecework.MesProPieceworkRecordService;
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
 * @author 芋道源码
 */
@Component
@Slf4j
public class MesProFeedbackApproveListener {

    @Resource
    private MesProPieceworkRecordService pieceworkRecordService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onFeedbackApproved(MesProFeedbackApprovedEvent event) {
        if (event == null || event.getFeedbackId() == null) {
            return;
        }
        try {
            Long recordId = pieceworkRecordService.generateRecordFromFeedback(event.getFeedbackId());
            if (recordId != null) {
                log.info("[onFeedbackApproved][报工单 {} 生成计件明细 {}]", event.getFeedbackId(), recordId);
            }
        } catch (Exception e) {
            log.warn("[onFeedbackApproved][报工单 {} 生成计件明细失败]", event.getFeedbackId(), e);
        }
    }

}
