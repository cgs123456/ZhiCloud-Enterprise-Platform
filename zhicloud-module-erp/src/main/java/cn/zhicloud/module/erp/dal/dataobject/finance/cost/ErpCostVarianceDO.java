package cn.zhicloud.module.erp.dal.dataobject.finance.cost;

import cn.zhicloud.framework.tenant.core.db.TenantBaseDO;
import cn.zhicloud.module.erp.enums.finance.cost.ErpVarianceTypeEnum;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

@TableName("erp_cost_variance")
@KeySequence("erp_cost_variance_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpCostVarianceDO extends TenantBaseDO {

    @TableId
    private Long id;
    private Long productId;
    private String costPeriod;
    private Long costItemId;
    private BigDecimal standardCost;
    private BigDecimal actualCost;
    private BigDecimal varianceAmount;
    private BigDecimal varianceRate;
    private Integer varianceType;
    private String analysisRemark;
    private String remark;

}
