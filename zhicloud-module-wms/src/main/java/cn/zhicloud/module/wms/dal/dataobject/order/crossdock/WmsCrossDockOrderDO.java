package cn.zhicloud.module.wms.dal.dataobject.order.crossdock;

import cn.zhicloud.framework.mybatis.core.dataobject.BaseDO;
import cn.zhicloud.module.wms.dal.dataobject.md.merchant.WmsMerchantDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

/**
 * WMS 越库单 DO
 *
 * 越库（Cross-Dock）：收货后跳过上架，直接分配至出库月台发运。
 *
 * @author 智云
 */
@TableName("wms_cross_dock_order")
@KeySequence("wms_cross_dock_order_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WmsCrossDockOrderDO extends BaseDO {

    /**
     * 主键编号
     */
    @TableId
    private Long id;
    /**
     * 越库单号
     */
    private String no;
    /**
     * 源头供应商编号
     *
     * 关联 {@link WmsMerchantDO#getId()}
     */
    private Long sourceSupplierId;
    /**
     * 目标客户编号
     *
     * 关联 {@link WmsMerchantDO#getId()}
     */
    private Long targetCustomerId;
    /**
     * 关联入库单号
     */
    private String receiptOrderNo;
    /**
     * 关联出库单号
     */
    private String shipmentOrderNo;
    /**
     * 越库状态
     *
     * 10 待收货 / 20 已收货 / 30 已分配 / 40 已完成 / 50 已取消
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
     * 备注
     */
    private String remark;

}
