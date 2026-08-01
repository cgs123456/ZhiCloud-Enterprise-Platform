package cn.iocoder.yudao.module.mes.listener;

/**
 * 生产报工审批通过事件
 *
 * <p>当 {@code MesProFeedbackServiceImpl#approveFeedback} 审批通过且报工单完成时发布此事件。
 * 由 {@link MesProFeedbackApproveListener} 监听，生成计件工资明细。
 *
 * <p>使用 {@code @TransactionalEventListener(AFTER_COMMIT)} 确保报工事务提交后再消费，
 * 避免监听器读到未提交的报工数据。
 *
 * @author 芋道源码
 */
public class MesProFeedbackApprovedEvent {

    /**
     * 报工单编号
     */
    private final Long feedbackId;

    public MesProFeedbackApprovedEvent(Long feedbackId) {
        this.feedbackId = feedbackId;
    }

    public Long getFeedbackId() {
        return feedbackId;
    }

}
