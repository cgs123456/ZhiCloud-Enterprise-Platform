package cn.zhicloud.module.crm.service.salesorder.listener;

import cn.zhicloud.module.bpm.api.event.BpmProcessInstanceStatusEvent;
import cn.zhicloud.module.bpm.api.event.BpmProcessInstanceStatusEventListener;
import cn.zhicloud.module.crm.service.salesorder.CrmSaleOrderService;
import cn.zhicloud.module.crm.service.salesorder.CrmSaleOrderServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * 销售订单审批的结果的监听器实现类（§12.261）
 *
 * @author 智云
 */
@Component
public class CrmSaleOrderStatusListener extends BpmProcessInstanceStatusEventListener {

    @Resource
    private CrmSaleOrderService saleOrderService;

    @Override
    public String getProcessDefinitionKey() {
        return CrmSaleOrderServiceImpl.BPM_PROCESS_DEFINITION_KEY;
    }

    @Override
    public void onEvent(BpmProcessInstanceStatusEvent event) {
        saleOrderService.updateSaleOrderAuditStatus(Long.parseLong(event.getBusinessKey()), event.getStatus());
    }

}
