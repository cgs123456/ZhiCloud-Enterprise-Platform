package cn.iocoder.yudao.module.mes.dal.dataobject.dv.tp;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

/**
 * MES TPM KPI 指标 DO
 *
 * @author 芋道源码
 */
@TableName("mes_dv_tp_kpi")
@KeySequence("mes_dv_tp_kpi_seq") // 用于 Oracle、PostgreSQL、Kingbase、DB2、H2 数据库的主键自增。如果是 MySQL 等数据库，可不写。
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MesDvTpKpiDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 设备编号
     *
     * 关联 {@link cn.iocoder.yudao.module.mes.dal.dataobject.dv.machinery.MesDvMachineryDO#getId()}
     */
    private Long equipmentId;
    /**
     * 周期（yyyyMM）
     */
    private String period;
    /**
     * 平均故障间隔时间（MTBF）
     */
    private BigDecimal mtbf;
    /**
     * 平均修复时间（MTTR）
     */
    private BigDecimal mttr;
    /**
     * OEE 改善值
     */
    private BigDecimal oeeImprovement;
    /**
     * 计划停机时间
     */
    private BigDecimal plannedDowntime;
    /**
     * 非计划停机时间
     */
    private BigDecimal unplannedDowntime;
    /**
     * 备注
     */
    private String remark;

}