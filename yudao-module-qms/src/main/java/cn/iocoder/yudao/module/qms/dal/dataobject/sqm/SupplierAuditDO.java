package cn.iocoder.yudao.module.qms.dal.dataobject.sqm;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import cn.iocoder.yudao.module.qms.enums.qms.SupplierAuditStatusEnum;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDate;

/**
 * QMS 供应商审核 DO
 *
 * <p>供应商年度审核计划与审核报告。
 *
 * @author yudao
 */
@TableName("qms_supplier_audit")
@KeySequence("qms_supplier_audit_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierAuditDO extends TenantBaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 审核编号
     */
    private String auditNo;
    /**
     * 审核名称
     */
    private String auditName;
    /**
     * 供应商 ID
     */
    private Long supplierId;
    /**
     * 供应商名称
     */
    private String supplierName;
    /**
     * 审核类型（10 首次审核 20 年度审核 30 跟踪审核 40 专项审核）
     */
    private Integer auditType;
    /**
     * 计划日期
     */
    private LocalDate plannedDate;
    /**
     * 实际日期
     */
    private LocalDate actualDate;
    /**
     * 审核员
     */
    private String auditor;
    /**
     * 审核结论（10 合格 20 有条件合格 30 不合格）
     */
    private Integer conclusion;
    /**
     * 审核报告
     */
    private String auditReport;
    /**
     * 状态
     *
     * 枚举 {@link SupplierAuditStatusEnum}
     */
    private Integer status;
    /**
     * 备注
     */
    private String remark;

}