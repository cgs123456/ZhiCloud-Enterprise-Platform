package cn.zhicloud.module.oa.service.reimburse.listener;

import cn.zhicloud.module.bpm.api.event.BpmProcessInstanceStatusEvent;
import cn.zhicloud.module.bpm.api.event.BpmProcessInstanceStatusEventListener;
import cn.zhicloud.module.bpm.enums.task.BpmProcessInstanceStatusEnum;
import cn.zhicloud.module.oa.service.reimburse.OaReimburseService;
import cn.zhicloud.module.oa.service.reimburse.OaReimburseServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 报销审批结果监听器：把 BPM 的流程实例状态回写到报销单状态。
 *
 * <p>报销单状态：10 草稿 20 审批中 30 已通过 40 已驳回 50 已撤销；
 * BPM 状态：1 审批中 2 通过 3 驳回 4 取消。两套编号不能直接互相赋值。
 *
 * @author 智云
 */
@Component
@Slf4j
public class OaReimburseStatusListener extends BpmProcessInstanceStatusEventListener {

    /** 报销单 - 已通过 */
    private static final int STATUS_APPROVED = 30;
    /** 报销单 - 已驳回 */
    private static final int STATUS_REJECTED = 40;
    /** 报销单 - 已撤销 */
    private static final int STATUS_REVOKED = 50;

    @Resource
    private OaReimburseService reimburseService;

    @Override
    public String getProcessDefinitionKey() {
        return OaReimburseServiceImpl.PROCESS_KEY;
    }

    @Override
    protected void onEvent(BpmProcessInstanceStatusEvent event) {
        Integer status = convertBpmStatus(event.getStatus());
        if (status == null) {
            // RUNNING 等中间态不落报销单状态：提交时已经置为「审批中」，重复写反而会把
            // 已通过的单据倒退回审批中。未知状态只记日志，不静默改状态。
            log.info("[onEvent][报销单({}) 忽略 BPM 状态({})]", event.getBusinessKey(), event.getStatus());
            return;
        }
        reimburseService.updateReimburseStatus(Long.parseLong(event.getBusinessKey()),
                status, event.getId());
    }

    /**
     * @param bpmStatus BPM 流程实例状态
     * @return 对应的报销单状态；不需要回写时返回 null
     */
    private static Integer convertBpmStatus(Integer bpmStatus) {
        if (BpmProcessInstanceStatusEnum.APPROVE.getStatus().equals(bpmStatus)) {
            return STATUS_APPROVED;
        }
        if (BpmProcessInstanceStatusEnum.REJECT.getStatus().equals(bpmStatus)) {
            return STATUS_REJECTED;
        }
        if (BpmProcessInstanceStatusEnum.CANCEL.getStatus().equals(bpmStatus)) {
            return STATUS_REVOKED;
        }
        return null;
    }

}
