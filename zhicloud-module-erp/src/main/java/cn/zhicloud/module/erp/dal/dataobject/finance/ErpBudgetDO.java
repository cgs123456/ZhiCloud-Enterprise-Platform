package cn.zhicloud.module.erp.dal.dataobject.finance;

import cn.zhicloud.framework.mybatis.core.dataobject.BaseDO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpPeriodDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

/**
 * ERP 预算主表 DO
 *
 * <p>按年度/期间/部门维度制定预算，明细按科目拆分。
 *
 * @author 智云
 */
@TableName("erp_budget")
@KeySequence("erp_budget_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpBudgetDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 预算编号
     */
    private String budgetNo;
    /**
     * 预算年度（如 2026）
     */
    private Integer budgetYear;
    /**
     * 会计期间编号（可空，为空表示年度预算；非空表示月度预算）
     *
     * 关联 {@link ErpPeriodDO#getId()}
     */
    private Long periodId;
    /**
     * 期间编码（冗余，如 202607 或 2026）
     */
    private String periodCode;
    /**
     * 部门编号
     */
    private Long departmentId;
    /**
     * 预算类型（10=运营预算 OPERATING；20=资本预算 CAPITAL；30=现金流预算 CASH_FLOW）
     */
    private Integer budgetType;
    /**
     * 预算总额（所有明细金额之和）
     */
    private BigDecimal totalAmount;
    /**
     * 状态（10=草稿 DRAFT；20=已审批 APPROVED；30=已执行 EXECUTING；40=已关闭 CLOSED）
     */
    private Integer status;
    /**
     * 备注
     */
    private String remark;

}
