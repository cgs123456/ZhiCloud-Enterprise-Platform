package cn.iocoder.yudao.module.qms.dal.dataobject.audit;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import cn.iocoder.yudao.module.qms.enums.audit.QmsAuditPlanStatusEnum;
import cn.iocoder.yudao.module.qms.enums.audit.QmsAuditTypeEnum;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDate;

/**
 * QMS 审核计划 DO
 *
 * @author 芋道源码
 */
@TableName("qms_audit_plan")
@KeySequence("qms_audit_plan_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QmsAuditPlanDO extends TenantBaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 计划编号
     */
    private String planNo;
    /**
     * 审核类型
     *
     * 枚举 {@link QmsAuditTypeEnum}
     */
    private Integer auditType;
    /**
     * 审核标题
     */
    private String title;
    /**
     * 审核依据（ISO 9001 / IATF 16949 / ISO 14001 等）
     */
    private String auditStandard;
    /**
     * 审核范围
     */
    private String auditScope;
    /**
     * 审核目的
     */
    private String auditPurpose;
    /**
     * 主审 ID
     */
    private Long leadAuditorId;
    /**
     * 审核开始日期
     */
    private LocalDate auditStartDate;
    /**
     * 审核结束日期
     */
    private LocalDate auditEndDate;
    /**
     * 状态
     *
     * 枚举 {@link QmsAuditPlanStatusEnum}
     */
    private Integer status;
    /**
     * 备注
     */
    private String remark;
    /**
     * 排序
     */
    private Integer sort;

}
