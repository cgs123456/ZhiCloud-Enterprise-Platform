package cn.iocoder.yudao.module.erp.dal.dataobject.production.mps;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * ERP 主生产计划明细 DO
 *
 * 按需求时界/计划时界分时段
 *
 * @author 芋道源码
 */
@TableName("erp_mps_plan_detail")
@KeySequence("erp_mps_plan_detail_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpMpsPlanDetailDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 主生产计划编号
     *
     * 关联 {@link ErpMpsPlanDO#getId()}
     */
    private Long planId;
    /**
     * 时段开始日期
     */
    private LocalDate periodStart;
    /**
     * 时段结束日期
     */
    private LocalDate periodEnd;
    /**
     * 毛需求
     */
    private BigDecimal grossRequirement;
    /**
     * 计划接收
     */
    private BigDecimal scheduledReceipt;
    /**
     * 预计可用库存
     */
    private BigDecimal projectedAvailableBalance;
    /**
     * 计划订单接收
     */
    private BigDecimal plannedOrderReceipt;
    /**
     * 计划订单下达
     */
    private BigDecimal plannedOrderRelease;
    /**
     * 备注
     */
    private String remark;
    /**
     * 排序
     */
    private Integer sort;

}