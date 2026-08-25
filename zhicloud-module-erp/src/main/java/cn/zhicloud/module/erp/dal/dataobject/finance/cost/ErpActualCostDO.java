package cn.zhicloud.module.erp.dal.dataobject.finance.cost;

import cn.zhicloud.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

/**
 * ERP 实际成本 DO
 *
 * <p>记录产品在某期间某成本项目下的实际成本，用于差异分析。
 *
 * @author 智云
 */
@TableName("erp_actual_cost")
@KeySequence("erp_actual_cost_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpActualCostDO extends TenantBaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 产品 ID
     */
    private Long productId;
    /**
     * 产品编码
     */
    private String productCode;
    /**
     * 成本期间（yyyymm）
     */
    private String costPeriod;
    /**
     * 成本项目 ID
     */
    private Long costItemId;
    /**
     * 实际成本总额
     */
    private BigDecimal actualCost;
    /**
     * 实际产量/数量
     */
    private BigDecimal actualQuantity;
    /**
     * 单位成本
     */
    private BigDecimal unitCost;
    /**
     * 差异金额
     */
    private BigDecimal varianceAmount;
    /**
     * 差异率(%)
     */
    private BigDecimal varianceRate;
    /**
     * 备注
     */
    private String remark;

}
