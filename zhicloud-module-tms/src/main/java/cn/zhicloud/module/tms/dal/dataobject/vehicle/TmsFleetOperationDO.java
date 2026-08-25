package cn.zhicloud.module.tms.dal.dataobject.vehicle;

import cn.zhicloud.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * TMS 车队运营 DO
 *
 * <p>记录车辆运营数据：里程、油耗、维修保养、保险、年检等。
 *
 * @author 智云
 */
@TableName("tms_fleet_operation")
@KeySequence("tms_fleet_operation_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TmsFleetOperationDO extends TenantBaseDO {

    @TableId
    private Long id;
    /**
     * 车辆编号
     */
    private Long vehicleId;
    /**
     * 运营日期
     */
    private LocalDate operationDate;
    /**
     * 行驶里程（公里）
     */
    private BigDecimal mileage;
    /**
     * 油耗（升）
     */
    private BigDecimal fuelConsumption;
    /**
     * 油费（元）
     */
    private BigDecimal fuelCost;
    /**
     * 维修保养费（元）
     */
    private BigDecimal maintenanceCost;
    /**
     * 保险费（元）
     */
    private BigDecimal insuranceCost;
    /**
     * 年检费（元）
     */
    private BigDecimal inspectionCost;
    /**
     * 运营收人（元）
     */
    private BigDecimal revenue;
    /**
     * 总运营成本（元）
     */
    private BigDecimal totalCost;
    /**
     * 运营利润（收入 - 总成本）
     */
    private BigDecimal profit;
    /**
     * 备注
     */
    private String remark;

}
