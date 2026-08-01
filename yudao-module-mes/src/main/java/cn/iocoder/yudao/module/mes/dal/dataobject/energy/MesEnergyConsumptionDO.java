package cn.iocoder.yudao.module.mes.dal.dataobject.energy;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * MES 能源消耗记录 DO
 *
 * <p>记录车间/工位的水、电、气等能源消耗数据，支持按日/月统计与同比环比分析。
 *
 * @author 芋道源码
 */
@TableName("mes_energy_consumption")
@KeySequence("mes_energy_consumption_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MesEnergyConsumptionDO extends TenantBaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 车间编号
     */
    private Long workshopId;
    /**
     * 工位编号（可空，空表示车间级统计）
     */
    private Long workstationId;
    /**
     * 能源类型
     *
     * 10 电 / 20 水 / 30 天然气 / 40 蒸汽 / 50 压缩空气
     */
    private Integer energyType;
    /**
     * 统计日期
     */
    private LocalDate recordDate;
    /**
     * 消耗量
     */
    private BigDecimal consumption;
    /**
     * 单位
     */
    private String unit;
    /**
     * 单价
     */
    private BigDecimal unitPrice;
    /**
     * 总金额
     */
    private BigDecimal totalAmount;
    /**
     * 备注
     */
    private String remark;

}
