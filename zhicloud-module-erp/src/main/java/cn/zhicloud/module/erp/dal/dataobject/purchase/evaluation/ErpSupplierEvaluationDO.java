package cn.zhicloud.module.erp.dal.dataobject.purchase.evaluation;

import cn.zhicloud.framework.mybatis.core.dataobject.BaseDO;
import cn.zhicloud.module.erp.dal.dataobject.purchase.ErpSupplierDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

/**
 * ERP 供应商评估 DO
 *
 * @author 智云
 */
@TableName("erp_supplier_evaluation")
@KeySequence("erp_supplier_evaluation_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpSupplierEvaluationDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 供应商编号
     *
     * 关联 {@link ErpSupplierDO#getId()}
     */
    private Long supplierId;
    /**
     * 评估周期（yyyyMM）
     */
    private String evaluationPeriod;
    /**
     * 质量评分
     */
    private BigDecimal qualityScore;
    /**
     * 交期评分
     */
    private BigDecimal deliveryScore;
    /**
     * 价格评分
     */
    private BigDecimal priceScore;
    /**
     * 服务评分
     */
    private BigDecimal serviceScore;
    /**
     * 综合评分
     */
    private BigDecimal totalScore;
    /**
     * 等级 A/B/C/D
     */
    private String grade;
    /**
     * 评估人
     */
    private String evaluator;
    /**
     * 备注
     */
    private String remark;

}
