package cn.zhicloud.module.erp.dal.dataobject.finance;

import cn.zhicloud.framework.mybatis.core.dataobject.BaseDO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpBudgetDO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpGlAccountDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

/**
 * ERP 预算明细 DO
 *
 * <p>按会计科目拆分预算金额，支持预算 vs 实际差异分析。
 *
 * @author 智云
 */
@TableName("erp_budget_detail")
@KeySequence("erp_budget_detail_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpBudgetDetailDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 预算主表编号
     *
     * 关联 {@link ErpBudgetDO#getId()}
     */
    private Long budgetId;
    /**
     * 会计科目编号
     *
     * 关联 {@link ErpGlAccountDO#getId()}
     */
    private Long accountId;
    /**
     * 科目编码（冗余）
     */
    private String accountCode;
    /**
     * 科目名称（冗余）
     */
    private String accountName;
    /**
     * 预算金额（借方方向）
     */
    private BigDecimal budgetAmount;
    /**
     * 实际金额（由系统汇总 GL 凭证填入，可空）
     */
    private BigDecimal actualAmount;
    /**
     * 差异金额（actualAmount - budgetAmount，可空）
     */
    private BigDecimal varianceAmount;
    /**
     * 差异率（varianceAmount / budgetAmount，可空）
     */
    private BigDecimal varianceRate;
    /**
     * 排序
     */
    private Integer sort;
    /**
     * 备注
     */
    private String remark;

}
