package cn.zhicloud.module.erp.dal.dataobject.finance;

import cn.zhicloud.framework.tenant.core.db.TenantBaseDO;
import cn.zhicloud.module.erp.enums.finance.ErpPeriodCloseTypeEnum;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ERP 期末处理记录 DO（P0-6）
 *
 * <p>用于记录每次期末处理的执行结果，包括：
 * <ul>
 *   <li>{@link ErpPeriodCloseTypeEnum#MONTH_CHECK} 月末检查：返回未审核单据统计</li>
 *   <li>{@link ErpPeriodCloseTypeEnum#REVALUATION} 调汇：基于外币账户汇率差异调整</li>
 *   <li>{@link ErpPeriodCloseTypeEnum#PROFIT_LOSS_TRANSFER} 损益结转：将收入/支出汇总到本年利润</li>
 * </ul>
 *
 * <p>每次执行生成一条记录，支持幂等：同一期间 + 同一类型只能成功执行一次。
 *
 * @author 智云
 */
@TableName("erp_period_close")
@KeySequence("erp_period_close_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpPeriodCloseDO extends TenantBaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 期间编号
     *
     * 关联 {@link ErpPeriodDO#getId()}
     */
    private Long periodId;
    /**
     * 期间编码（冗余，便于查询）
     */
    private String periodCode;
    /**
     * 处理类型
     *
     * 枚举 {@link ErpPeriodCloseTypeEnum}
     */
    private Integer type;
    /**
     * 执行人
     */
    private String executedBy;
    /**
     * 执行时间
     */
    private LocalDateTime executedTime;
    /**
     * 处理状态（10 成功 / 20 跳过 / 30 失败）
     */
    private Integer processStatus;
    /**
     * 关键数据摘要（JSON 字符串）
     *
     * <p>不同类型对应不同字段：
     * <ul>
     *   <li>MONTH_CHECK：未审核单据数、未付款金额、未收款金额</li>
     *   <li>REVALUATION：调整账户数、调整金额合计</li>
     *   <li>PROFIT_LOSS_TRANSFER：收入合计、支出合计、净利润</li>
     * </ul>
     */
    private String summary;
    /**
     * 调整金额（仅调汇有值，正数为收益，负数为损失）
     */
    private BigDecimal adjustmentAmount;
    /**
     * 备注
     */
    private String remark;

}
