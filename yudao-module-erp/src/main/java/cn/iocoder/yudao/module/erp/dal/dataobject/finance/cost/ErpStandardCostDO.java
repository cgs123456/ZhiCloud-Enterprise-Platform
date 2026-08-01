package cn.iocoder.yudao.module.erp.dal.dataobject.finance.cost;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import cn.iocoder.yudao.module.erp.enums.finance.cost.ErpStandardCostStatusEnum;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * ERP 标准成本 DO
 *
 * <p>记录产品在某一成本项目下的标准成本，按生效日期区间维护。
 *
 * @author 芋道源码
 */
@TableName("erp_standard_cost")
@KeySequence("erp_standard_cost_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpStandardCostDO extends TenantBaseDO {

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
     * 产品名称
     */
    private String productName;
    /**
     * 成本项目 ID
     */
    private Long costItemId;
    /**
     * 标准成本
     */
    private BigDecimal standardCost;
    /**
     * 生效日期
     */
    private LocalDate effectiveDate;
    /**
     * 失效日期
     */
    private LocalDate expiryDate;
    /**
     * 状态
     *
     * 枚举 {@link ErpStandardCostStatusEnum}
     */
    private Integer status;
    /**
     * 备注
     */
    private String remark;

}
