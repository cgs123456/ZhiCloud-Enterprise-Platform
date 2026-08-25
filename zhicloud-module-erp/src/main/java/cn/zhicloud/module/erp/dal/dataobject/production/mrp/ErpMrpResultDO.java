package cn.zhicloud.module.erp.dal.dataobject.production.mrp;

import cn.zhicloud.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * ERP 物料需求计划结果 DO
 *
 * @author 智云
 */
@TableName("erp_mrp_result")
@KeySequence("erp_mrp_result_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpMrpResultDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * MRP 计划编号
     *
     * 关联 {@link ErpMrpPlanDO#getId()}
     */
    private Long planId;
    /**
     * 产品编号
     *
     * 关联 {@link cn.zhicloud.module.erp.dal.dataobject.product.ErpProductDO#getId()}
     */
    private Long productId;
    /**
     * 产品名称（冗余）
     */
    private String productName;
    /**
     * 需求类型
     *
     * 10 独立需求 / 20 相关需求
     */
    private Integer demandType;
    /**
     * 需求量
     */
    private BigDecimal demandQuantity;
    /**
     * 库存可用量
     */
    private BigDecimal stockQuantity;
    /**
     * 净需求
     */
    private BigDecimal netDemand;
    /**
     * 计划订单类型
     *
     * 10 采购 / 20 生产
     */
    private Integer plannedOrderType;
    /**
     * 计划订单量
     */
    private BigDecimal plannedOrderQuantity;
    /**
     * 计划交付日
     */
    private LocalDate plannedDeliveryDate;
    /**
     * 供应商编号
     *
     * 关联 {@link cn.zhicloud.module.erp.dal.dataobject.purchase.ErpSupplierDO#getId()}
     */
    private Long supplierId;
    /**
     * 生产车间编号
     */
    private Long workshopId;
    /**
     * 上层产品编号（BOM 父件）
     */
    private Long sourceProductId;
    /**
     * 上层需求量
     */
    private BigDecimal sourceQuantity;
    /**
     * 备注
     */
    private String remark;

}
