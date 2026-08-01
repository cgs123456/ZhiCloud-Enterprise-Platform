package cn.iocoder.yudao.module.tms.dal.dataobject.shipment;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * TMS 运单 DO
 *
 * @author yudao
 */
@TableName("tms_shipment")
@KeySequence("tms_shipment_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TmsShipmentDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 运单号
     */
    private String no;
    /**
     * 承运商编号
     */
    private Long carrierId;
    /**
     * 车辆编号
     */
    private Long vehicleId;
    /**
     * 司机编号
     */
    private Long driverId;
    /**
     * 起点地址
     */
    private String originAddress;
    /**
     * 终点地址
     */
    private String destinationAddress;
    /**
     * 运单类型
     *
     * 10 采购入库 / 20 销售出库 / 30 调拨 / 40 退货
     */
    private Integer shipmentType;
    /**
     * 来源单据号
     */
    private String sourceOrderNo;
    /**
     * 合计数量
     */
    private BigDecimal totalQuantity;
    /**
     * 合计重量
     */
    private BigDecimal totalWeight;
    /**
     * 合计体积
     */
    private BigDecimal totalVolume;
    /**
     * 运费金额
     */
    private BigDecimal freightAmount;
    /**
     * 发车时间
     */
    private LocalDateTime departureTime;
    /**
     * 预计到达时间
     */
    private LocalDateTime estimatedArrivalTime;
    /**
     * 实际到达时间
     */
    private LocalDateTime actualArrivalTime;
    /**
     * 状态
     *
     * 10 待发车 / 20 运输中 / 30 已到达 / 40 已签收 / 50 已取消
     */
    private Integer status;
    /**
     * 备注
     */
    private String remark;

}
