package cn.zhicloud.module.tms.dal.dataobject.vehicle;

import cn.zhicloud.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
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

}
