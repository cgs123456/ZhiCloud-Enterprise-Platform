package cn.zhicloud.module.crm.util;

import cn.hutool.core.lang.Assert;
import cn.zhicloud.module.bpm.enums.task.BpmTaskStatusEnum;
import cn.zhicloud.module.crm.enums.common.CrmAuditStatusEnum;

/**
 * CRM 流程工具类
 *
 * @author HUIHUI
 */
public class CrmAuditStatusUtils {

    /**
     * BPM 审批结果转换
     *
     * @param bpmResult BPM 审批结果
     */
    public static Integer convertBpmResultToAuditStatus(Integer bpmResult) {
        Integer auditStatus = BpmTaskStatusEnum.APPROVE.getStatus().equals(bpmResult) ? CrmAuditStatusEnum.APPROVE.getStatus()
                : BpmTaskStatusEnum.REJECT.getStatus().equals(bpmResult) ? CrmAuditStatusEnum.REJECT.getStatus()
                : BpmTaskStatusEnum.CANCEL.getStatus().equals(bpmResult) ? CrmAuditStatusEnum.CANCEL.getStatus() : null;
        // §12.261 D6：撤回事件（BPM CANCEL=4）必须映射为 CRM 已取消(40)；此前的 BpmTaskStatusEnum 自映射会落下非枚举值 4
        Assert.notNull(auditStatus, "BPM 审批结果({}) 转换失败", bpmResult);
        return auditStatus;
    }

}
