package cn.zhicloud.module.crm.service.invoice.listener;

import cn.zhicloud.module.bpm.api.event.BpmProcessInstanceStatusEvent;
import cn.zhicloud.module.bpm.api.event.BpmProcessInstanceStatusEventListener;
import cn.zhicloud.module.crm.service.invoice.CrmInvoiceService;
import cn.zhicloud.module.crm.service.invoice.CrmInvoiceServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * 开票审批的结果的监听器实现类
 *
 * <p>修复（2026-10-13，台账 §12.209）：合同（CrmContractStatusListener）与回款
 * （CrmReceivableStatusListener）都有审批结果监听器，唯独开票域漏了——
 * 提交开票审批后 BPM 流程实例建起来了，但审批通过/驳回的结果没有任何人
 * 把它写回 crm_invoice.audit_status，导致单据永远停在「审批中(10)」。
 * 真机 E2E 抓出：任务 approve 成功后 DB 仍为 10。
 *
 * @author 智云
 */
@Component
public class CrmInvoiceStatusListener extends BpmProcessInstanceStatusEventListener {

    @Resource
    private CrmInvoiceService invoiceService;

    @Override
    public String getProcessDefinitionKey() {
        return CrmInvoiceServiceImpl.BPM_PROCESS_DEFINITION_KEY;
    }

    @Override
    protected void onEvent(BpmProcessInstanceStatusEvent event) {
        invoiceService.updateInvoiceAuditStatus(Long.parseLong(event.getBusinessKey()), event.getStatus());
    }

}
