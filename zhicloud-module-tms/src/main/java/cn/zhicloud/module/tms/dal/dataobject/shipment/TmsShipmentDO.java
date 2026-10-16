package cn.zhicloud.module.tms.dal.dataobject.shipment;

import cn.zhicloud.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * TMS 运单 DO
 *
 * @author zhicloud
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

    /**
     * 来源 WMS 出库单编号
     *
     * 关联 WmsShipmentOrderDO#getId()；NULL 表示手工建单（不经 WMS 出库联动）
     */
    private Long shipmentOrderId;
    /**
     * 签收时间
     *
     * 此前 confirmSign 只把状态推到「已签收」而不记时间，签收时刻在库里不可追溯
     */
    private LocalDateTime signTime;
    /**
     * 乐观锁版本号
     *
     * <p>状态机按「已知版本」推进：dispatch → confirmArrival → confirmSign。
     * 加列前这些流转是 selectById → 内存判 status → updateById 全量覆盖，
     * 并发重复派车会同时通过 status=10 校验并把不同车辆/司机写进同一张运单。</p>
     *
     * <p>并发 CAS 成立的前提是**状态单调不回退**。{@code TmsShipmentSaveReqVO} 因此
     * 不暴露 status 字段（§12.231.4 起移除）。注意「VO 不放 status」是这道防线的**全部**：
     * `updateShipment` 走 `BeanUtils.toBean` + `updateById`，一旦有人把 status 加回 VO，
     * 它会被原样 SET，乐观锁只管版本号、不管值的合法性。若将来要支持「取消运单」这类
     * 回退，必须同步重新评估 dispatch 的 CAS 条件，不能只加个 VO 字段。</p>
     */
    @Version
    private Long version;

}
