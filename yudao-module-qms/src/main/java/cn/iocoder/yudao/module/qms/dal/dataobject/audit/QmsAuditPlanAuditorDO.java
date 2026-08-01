package cn.iocoder.yudao.module.qms.dal.dataobject.audit;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import cn.iocoder.yudao.module.qms.enums.audit.QmsAuditorRoleEnum;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * QMS 审核组成员 DO
 *
 * @author 芋道源码
 */
@TableName("qms_audit_plan_auditor")
@KeySequence("qms_audit_plan_auditor_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QmsAuditPlanAuditorDO extends TenantBaseDO {

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
     * 审核员 ID
     */
    private Long auditorId;
    /**
     * 角色
     *
     * 枚举 {@link QmsAuditorRoleEnum}
     */
    private Integer role;
    /**
     * 备注
     */
    private String remark;
    /**
     * 排序
     */
    private Integer sort;

}
