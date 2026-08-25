package cn.zhicloud.module.erp.dal.dataobject.finance;

import cn.zhicloud.framework.tenant.core.db.TenantBaseDO;
import cn.zhicloud.module.erp.enums.finance.ErpPeriodStatusEnum;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * ERP 会计期间 DO（P0-6）
 *
 * <p>每个会计期间对应一个月，状态流转：
 * <ul>
 *   <li>{@link ErpPeriodStatusEnum#OPEN} 开放：当月业务单据可录入、审核</li>
 *   <li>{@link ErpPeriodStatusEnum#CLOSING} 结账中：月末检查已通过，正在执行调汇/结转</li>
 *   <li>{@link ErpPeriodStatusEnum#CLOSED} 已关账：本月所有业务已锁定，禁止再录入单据</li>
 * </ul>
 *
 * @author 智云
 */
@TableName("erp_period")
@KeySequence("erp_period_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpPeriodDO extends TenantBaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 期间年度（如 2026）
     */
    private Integer year;
    /**
     * 期间月份（1-12）
     */
    private Integer month;
    /**
     * 期间编码（如 202607）
     */
    private String code;
    /**
     * 期间起始日期
     */
    private LocalDate startDate;
    /**
     * 期间结束日期
     */
    private LocalDate endDate;
    /**
     * 状态
     *
     * 枚举 {@link ErpPeriodStatusEnum}
     */
    private Integer status;
    /**
     * 关账人
     */
    private String closedBy;
    /**
     * 关账时间
     */
    private LocalDateTime closedTime;
    /**
     * 备注
     */
    private String remark;

}
