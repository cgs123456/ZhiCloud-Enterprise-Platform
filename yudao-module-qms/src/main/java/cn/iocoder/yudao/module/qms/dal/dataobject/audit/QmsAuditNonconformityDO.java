package cn.iocoder.yudao.module.qms.dal.dataobject.audit;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import cn.iocoder.yudao.module.qms.enums.audit.QmsNcSeverityEnum;
import cn.iocoder.yudao.module.qms.enums.audit.QmsNcStatusEnum;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDate;

/**
 * QMS 审核不符合项 DO
 *
 * @author 芋道源码
 */
@TableName("qms_audit_nonconformity")
@KeySequence("qms_audit_nonconformity_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QmsAuditNonconformityDO extends TenantBaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 审核报告 ID
     *
     * 关联 {@link QmsAuditReportDO#getId()}
     */
    private Long reportId;
    /**
     * 不符合项编号
     */
    private String ncNo;
    /**
     * 严重程度
     *
     * 枚举 {@link QmsNcSeverityEnum}
     */
    private Integer severity;
    /**
     * 不符合描述
     */
    private String description;
    /**
     * 不符合条款（如 ISO 9001 8.2.1）
     */
    private String clause;
    /**
     * 责任部门 ID
     */
    private Long responsibleDeptId;
    /**
     * 整改截止日期
     */
    private LocalDate correctiveActionDeadline;
    /**
     * 状态
     *
     * 枚举 {@link QmsNcStatusEnum}
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
