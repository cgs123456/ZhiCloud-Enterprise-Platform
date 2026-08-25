package cn.zhicloud.module.qms.dal.dataobject.audit;

import cn.zhicloud.framework.tenant.core.db.TenantBaseDO;
import cn.zhicloud.module.qms.enums.audit.QmsAuditConclusionEnum;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * QMS 审核报告 DO
 *
 * @author 智云
 */
@TableName("qms_audit_report")
@KeySequence("qms_audit_report_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QmsAuditReportDO extends TenantBaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 审核计划 ID
     *
     * 关联 {@link QmsAuditPlanDO#getId()}
     */
    private Long planId;
    /**
     * 报告编号
     */
    private String reportNo;
    /**
     * 审核总结
     */
    private String auditSummary;
    /**
     * 审核结论
     *
     * 枚举 {@link QmsAuditConclusionEnum}
     */
    private Integer conclusion;
    /**
     * 发现的不符合项数
     */
    private Integer issueCount;
    /**
     * 备注
     */
    private String remark;
    /**
     * 排序
     */
    private Integer sort;

}
