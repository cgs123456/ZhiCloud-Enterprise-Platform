package cn.zhicloud.module.tms.dal.dataobject.freight;

import cn.zhicloud.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * TMS 运费对账 DO
 *
 * <p>承运商运费对账单，用于核对系统计算运费与承运商账单差异。
 *
 * @author 智云
 */
@TableName("tms_freight_reconciliation")
@KeySequence("tms_freight_reconciliation_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TmsFreightReconciliationDO extends TenantBaseDO {

    @TableId
    private Long id;
    /**
     * 对账单号
     */
    private String no;
    /**
     * 承运商编号
     */
    private Long carrierId;
    /**
     * 对账周期开始日期
     */
    private LocalDate periodStart;
    /**
     * 对账周期结束日期
     */
    private LocalDate periodEnd;
    /**
     * 系统运费总额
     */
    private BigDecimal systemAmount;
    /**
     * 承运商账单金额
     */
    private BigDecimal carrierAmount;
    /**
     * 差异金额（承运商 - 系统）
     */
    private BigDecimal diffAmount;
    /**
     * 对账状态
     *
     * 0 待对账 / 10 已对账 / 20 有差异 / 30 已确认 / 40 已驳回
     */
    private Integer status;
    /**
     * 对账人
     */
    private Long reconcilerId;
    /**
     * 对账时间
     */
    private java.time.LocalDateTime reconcileTime;
    /**
     * 确认人
     */
    private Long confirmerId;
    /**
     * 确认时间
     */
    private java.time.LocalDateTime confirmTime;
    /**
     * 备注
     */
    private String remark;

}
