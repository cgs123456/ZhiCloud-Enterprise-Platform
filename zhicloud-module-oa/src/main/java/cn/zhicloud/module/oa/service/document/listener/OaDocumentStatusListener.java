package cn.zhicloud.module.oa.service.document.listener;

import cn.zhicloud.framework.common.util.number.NumberUtils;
import cn.zhicloud.module.bpm.api.event.BpmProcessInstanceStatusEvent;
import cn.zhicloud.module.bpm.api.event.BpmProcessInstanceStatusEventListener;
import cn.zhicloud.module.bpm.enums.task.BpmProcessInstanceStatusEnum;
import cn.zhicloud.module.oa.service.document.OaDocumentService;
import cn.zhicloud.module.oa.service.document.OaDocumentServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 公文审批结果监听器：把 BPM 的流程实例状态回写到公文状态。
 *
 * <p>公文状态机是 10 草稿 → 20 审核中 → 25 待签发 → 30 已发布（→50 已归档 / →40 已废止），
 * 与手工核稿路径（{@code reviewPassDocument} 落 25）保持同一档：BPM 通过只走到「待签发」，
 * 签发必须由人在系统内完成（发布接口强制校验 25→30，禁止跳过签发）。
 * BPM 驳回/撤销 → 回到草稿(10)，可修改后重新提交，与手工核稿驳回（落 10）一致。
 *
 * <p>BPM 状态：1 审批中 2 通过 3 驳回 4 取消；RUNNING 这类中间态不回写
 * （提交时已置为「审核中」，倒写会把待签发/已签发的公文拖回审核中）。
 *
 * @author 智云
 */
@Component
@Slf4j
public class OaDocumentStatusListener extends BpmProcessInstanceStatusEventListener {

    /** 公文 - 待签发（BPM 通过后的落点，与手工核稿通过同档） */
    private static final int STATUS_PENDING_SIGN = 25;
    /** 公文 - 草稿（BPM 驳回/撤销后的落点，与手工核稿驳回同档） */
    private static final int STATUS_DRAFT = 10;

    @Resource
    private OaDocumentService documentService;

    @Override
    public String getProcessDefinitionKey() {
        return OaDocumentServiceImpl.PROCESS_KEY;
    }

    @Override
    protected void onEvent(BpmProcessInstanceStatusEvent event) {
        Integer status = convertBpmStatus(event.getStatus());
        if (status == null) {
            // RUNNING 等中间态不落公文状态，未知状态只记日志，不静默改状态
            log.info("[onEvent][公文({}) 忽略 BPM 状态({})]", event.getBusinessKey(), event.getStatus());
            return;
        }
        Long documentId = NumberUtils.parseLong(event.getBusinessKey());
        if (documentId == null) {
            log.error("[onEvent][businessKey({}) 不是有效的公文编号，跳过状态更新]", event.getBusinessKey());
            return;
        }
        documentService.updateDocumentStatus(documentId,
                status, event.getId());
    }

    /**
     * @param bpmStatus BPM 流程实例状态
     * @return 对应的公文状态；不需要回写时返回 null
     */
    private static Integer convertBpmStatus(Integer bpmStatus) {
        if (BpmProcessInstanceStatusEnum.APPROVE.getStatus().equals(bpmStatus)) {
            return STATUS_PENDING_SIGN;
        }
        if (BpmProcessInstanceStatusEnum.REJECT.getStatus().equals(bpmStatus)
                || BpmProcessInstanceStatusEnum.CANCEL.getStatus().equals(bpmStatus)) {
            return STATUS_DRAFT;
        }
        return null;
    }

}
