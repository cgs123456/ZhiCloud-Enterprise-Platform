package cn.iocoder.yudao.module.erp.dal.dataobject.finance;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import cn.iocoder.yudao.module.erp.enums.finance.ErpCostAllocationTypeEnum;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * ERP 成本分摊 DO
 *
 * <p>记录从一个成本中心向另一个成本中心分摊成本的明细。
 * 分摊类型支持手工与规则两种。
 *
 * @author 芋道源码
 */
@TableName("erp_cost_allocation")
@KeySequence("erp_cost_allocation_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpCostAllocationDO extends TenantBaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 源成本中心编号
     *
     * 关联 {@link ErpCostCenterDO#getId()}
     */
    private Long costCenterId;
    /**
     * 分摊类型
     *
     * 枚举 {@link ErpCostAllocationTypeEnum}
     */
    private Integer allocationType;
    /**
     * 分摊金额
     */
    private BigDecimal amount;
    /**
     * 分摊日期
     */
    private LocalDate allocationDate;
    /**
     * 目标成本中心编号
     *
     * 关联 {@link ErpCostCenterDO#getId()}
     */
    private Long targetCostCenterId;
    /**
     * 备注
     */
    private String remark;

}
