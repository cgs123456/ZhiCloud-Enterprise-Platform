package cn.zhicloud.module.erp.dal.dataobject.finance.cost;

import cn.zhicloud.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

/**
 * ERP 工单成本归集 DO
 *
 * <p>记录工单的材料、人工、制造费用、外协成本归集结果。
 *
 * @author 智云
 */
@TableName("erp_work_order_cost")
@KeySequence("erp_work_order_cost_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpWorkOrderCostDO extends TenantBaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 工单 ID
     */
    private Long workOrderId;
    /**
     * 工单编码
     */
    private String workOrderCode;
    /**
     * 产品 ID
     */
    private Long productId;
    /**
     * 成本期间（yyyymm）
     */
    private String costPeriod;
    /**
     * 材料成本
     */
    private BigDecimal materialCost;
    /**
     * 人工成本
     */
    private BigDecimal laborCost;
    /**
     * 制造费用
     */
    private BigDecimal overheadCost;
    /**
     * 外协成本
     */
    private BigDecimal outsourcingCost;
    /**
     * 总成本
     */
    private BigDecimal totalCost;
    /**
     * 工单产量
     */
    private BigDecimal quantity;
    /**
     * 单位成本
     */
    private BigDecimal unitCost;
    /**
     * 备注
     */
    private String remark;

}
