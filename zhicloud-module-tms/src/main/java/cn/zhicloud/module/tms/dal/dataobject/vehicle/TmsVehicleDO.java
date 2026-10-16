package cn.zhicloud.module.tms.dal.dataobject.vehicle;

import cn.zhicloud.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.*;

import java.math.BigDecimal;

/**
 * TMS 车辆 DO
 *
 * @author zhicloud
 */
@TableName("tms_vehicle")
@KeySequence("tms_vehicle_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TmsVehicleDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 车牌号
     */
    private String plateNo;
    /**
     * 车型
     */
    private String vehicleType;
    /**
     * 承运商编号
     */
    private Long carrierId;
    /**
     * 载重
     */
    private BigDecimal loadCapacity;
    /**
     * 容积
     */
    private BigDecimal volume;
    /**
     * 司机编号
     */
    private Long driverUserId;
    /**
     * 状态
     *
     * 10 可用 / 20 运输中 / 30 维修中
     */
    private Integer status;
    /**
     * 备注
     */
    private String remark;
    /**
     * 乐观锁版本号
     *
     * <p>车辆状态由运单 dispatch/confirmArrival 改写（{@code TmsShipmentServiceImpl#updateVehicleStatus}），
     * 而改写是 selectById → 内存判状态 → updateById 的读改写。无版本列时并发派车会互相覆盖：
     * 一张车被两单同时调度，两个事务都读到 status=10，都写 status=20，先到者被后到者覆盖，
     * 之后 confirmArrival 又把状态写回 10，车辆看起来可用却已被占用。
     * 加列与 {@code MesProWorkOrderDO} 的 @Version 对齐（V63.1 同一手法）。</p>
     */
    @Version
    private Long version;

}
