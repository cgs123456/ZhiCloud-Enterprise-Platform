package cn.iocoder.yudao.module.wms.dal.dataobject.order.asn;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import cn.iocoder.yudao.module.wms.dal.dataobject.md.merchant.WmsMerchantDO;
import cn.iocoder.yudao.module.wms.dal.dataobject.md.warehouse.WmsWarehouseDO;
import cn.iocoder.yudao.module.wms.dal.dataobject.order.dock.WmsDockDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * WMS ASN 到货通知单 DO
 *
 * <p>ASN（Advance Shipping Notice）到货通知：供应商发货前预先通知仓库的到货信息，
 * 用于仓库提前安排月台、收货资源。3PL 标配单据。
 *
 * @author 芋道源码
 */
@TableName("wms_asn_order")
@KeySequence("wms_asn_order_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WmsAsnOrderDO extends BaseDO {

    /**
     * 主键编号
     */
    @TableId
    private Long id;
    /**
     * ASN 编号
     */
    private String no;
    /**
     * 供应商编号
     *
     * 关联 {@link WmsMerchantDO#getId()}
     */
    private Long supplierId;
    /**
     * 仓库编号
     *
     * 关联 {@link WmsWarehouseDO#getId()}
     */
    private Long warehouseId;
    /**
     * 月台编号
     *
     * 关联 {@link WmsDockDO#getId()}
     */
    private Long dockId;
    /**
     * 预计到货时间
     */
    private LocalDateTime expectedArrivalTime;
    /**
     * 实际到货时间
     */
    private LocalDateTime actualArrivalTime;
    /**
     * 状态
     *
     * 枚举：10 待到货 / 20 已到货 / 30 已收货 / 40 已上架 / 50 已关闭
     */
    private Integer status;
    /**
     * 总数量
     */
    private BigDecimal totalQuantity;
    /**
     * 总金额
     */
    private BigDecimal totalAmount;
    /**
     * 运输方式
     *
     * 枚举：10 卡车 / 20 铁路 / 30 空运 / 40 海运
     */
    private Integer transportMode;
    /**
     * 承运商
     */
    private String carrierName;
    /**
     * 车牌号
     */
    private String vehicleNo;
    /**
     * 备注
     */
    private String remark;

}
