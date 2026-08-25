package cn.zhicloud.module.tms.dal.dataobject.gps;

import cn.zhicloud.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * TMS GPS 定位记录 DO
 *
 * @author zhicloud
 */
@TableName("tms_gps_position")
@KeySequence("tms_gps_position_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TmsGpsPositionDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 车辆编号
     */
    private Long vehicleId;
    /**
     * 运单编号
     */
    private Long shipmentId;
    /**
     * 经度
     */
    private BigDecimal longitude;
    /**
     * 纬度
     */
    private BigDecimal latitude;
    /**
     * 速度（km/h）
     */
    private BigDecimal speed;
    /**
     * 方向（0-360度，0=正北）
     */
    private BigDecimal direction;
    /**
     * 上报时间
     */
    private LocalDateTime reportTime;
    /**
     * 位置描述
     */
    private String locationDesc;

}
