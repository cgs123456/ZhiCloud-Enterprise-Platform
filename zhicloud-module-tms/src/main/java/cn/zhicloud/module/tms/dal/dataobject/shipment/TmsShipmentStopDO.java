package cn.zhicloud.module.tms.dal.dataobject.shipment;

import cn.zhicloud.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/**
 * TMS 运单站点 DO
 *
 * @author zhicloud
 */
@TableName("tms_shipment_stop")
@KeySequence("tms_shipment_stop_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TmsShipmentStopDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 运单编号
     */
    private Long shipmentId;
    /**
     * 站点顺序
     */
    private Integer sequenceNo;
    /**
     * 站点地址
     */
    private String address;
    /**
     * 到达时间
     */
    private LocalDateTime arrivalTime;
    /**
     * 离开时间
     */
    private LocalDateTime departureTime;
    /**
     * 备注
     */
    private String remark;

}
