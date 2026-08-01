package cn.iocoder.yudao.module.erp.dal.dataobject.purchase.evaluation;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

/**
 * ERP 供应商评估指标项 DO
 *
 * @author 芋道源码
 */
@TableName("erp_supplier_evaluation_item")
@KeySequence("erp_supplier_evaluation_item_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpSupplierEvaluationItemDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 评估编号
     *
     * 关联 {@link ErpSupplierEvaluationDO#getId()}
     */
    private Long evaluationId;
    /**
     * 指标名称
     */
    private String indicator;
    /**
     * 得分
     */
    private BigDecimal score;
    /**
     * 权重（百分比，例如 30 表示 30%）
     */
    private BigDecimal weight;
    /**
     * 加权得分 = score * weight / 100
     */
    private BigDecimal weightedScore;
    /**
     * 备注
     */
    private String remark;

}
