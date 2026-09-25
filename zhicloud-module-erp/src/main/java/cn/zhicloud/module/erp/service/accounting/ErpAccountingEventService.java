package cn.zhicloud.module.erp.service.accounting;

import cn.zhicloud.module.erp.service.accounting.bo.ErpAccountingEventCreateReqBO;
import cn.zhicloud.module.erp.service.accounting.bo.ErpAccountingEventReverseReqBO;

/**
 * ERP 会计事件服务。
 *
 * <p>负责将业务单据事实转换为会计事件，并按版本化过账规则生成会计凭证草稿。
 *
 * @author 智云
 */
public interface ErpAccountingEventService {

    /**
     * 创建并过账会计事件。
     *
     * @param request 会计事件请求
     * @return 生成的会计凭证编号
     */
    Long postEvent(ErpAccountingEventCreateReqBO request);

    /**
     * 根据原始事件创建独立冲销事件，并生成红字凭证草稿。
     *
     * @param request 冲销请求
     * @return 生成的冲销凭证编号
     */
    Long reverseEvent(ErpAccountingEventReverseReqBO request);

}
