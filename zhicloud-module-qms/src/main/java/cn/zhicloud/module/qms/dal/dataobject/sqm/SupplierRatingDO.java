package cn.zhicloud.module.qms.dal.dataobject.sqm;

import cn.zhicloud.framework.tenant.core.db.TenantBaseDO;
import cn.zhicloud.module.qms.enums.qms.SupplierGradeEnum;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * QMS 供应商评级 DO
 *
 * <p>基于 PPM（百万分之缺陷数）/ 交期达成率 / 质量合格率综合评级。
 *
 * @author zhicloud
 */
@TableName("qms_supplier_rating")
@KeySequence("qms_supplier_rating_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierRatingDO extends TenantBaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 评级编号
     */
    private String ratingNo;
    /**
     * 供应商 ID
     */
    private Long supplierId;
    /**
     * 供应商名称
     */
    private String supplierName;
    /**
     * 评级周期（如 2024-Q1）
     */
    private String ratingPeriod;
    /**
     * PPM 缺陷率（百万分之缺陷数）
     */
    private Integer ppm;
    /**
     * 交期达成率（百分比，如 98.50）
     */
    private BigDecimal onTimeRate;
    /**
     * 质量合格率（百分比，如 99.50）
     */
    private BigDecimal qualityRate;
    /**
     * 供应商等级
     *
     * 枚举 {@link SupplierGradeEnum}
     */
    private String grade;
    /**
     * 评级日期
     */
    private LocalDate ratingDate;
    /**
     * 备注
     */
    private String remark;

}