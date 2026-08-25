package cn.zhicloud.module.mes.dal.dataobject.dv.oeerecord;

import cn.zhicloud.framework.mybatis.core.dataobject.BaseDO;
import cn.zhicloud.module.mes.dal.dataobject.dv.machinery.MesDvMachineryDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * MES OEE 记录 DO
 *
 * @author 智云
 */
@TableName("mes_dv_oee_record")
@KeySequence("mes_dv_oee_record_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MesDvOeeRecordDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 设备编号
     *
     * 关联 {@link MesDvMachineryDO#getId()}
     */
    private Long machineryId;
    /**
     * 记录日期
     */
    private LocalDateTime recordDate;
    /**
     * 计划生产时间（分钟）
     */
    private BigDecimal plannedProductionTime;
    /**
     * 实际运行时间（分钟）
     */
    private BigDecimal runTime;
    /**
     * 理论节拍（分钟/件）
     */
    private BigDecimal idealCycleTime;
    /**
     * 总产量
     */
    private BigDecimal totalProduced;
    /**
     * 合格产量
     */
    private BigDecimal goodProduced;
    /**
     * 可用率
     */
    private BigDecimal availability;
    /**
     * 表现率
     */
    private BigDecimal performance;
    /**
     * 质量率
     */
    private BigDecimal quality;
    /**
     * OEE 值
     */
    private BigDecimal oee;
    /**
     * ISO 22400-2 时间稼动率 TUR (Time Utilization Rate)
     *
     * 公式：TUR = RunTime / PlannedProductionTime
     * 注：ISO 22400 中 TUR 与 Availability 同义，但区分于 AU（可用性利用率）
     */
    private BigDecimal timeUtilizationRate;
    /**
     * ISO 22400-2 机械效率 ME (Mechanical Efficiency)
     *
     * 公式：ME = (IdealCycleTime × GoodProduced) / RunTime
     * 区别于 Performance (PEE)：PEE 使用 TotalProduced，ME 使用 GoodProduced
     */
    private BigDecimal mechanicalEfficiency;
    /**
     * 备注
     */
    private String remark;

}
