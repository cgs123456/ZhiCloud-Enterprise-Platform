package cn.zhicloud.module.wms.dal.dataobject.order.wave;

import cn.zhicloud.framework.mybatis.core.dataobject.BaseDO;
import cn.zhicloud.module.wms.dal.dataobject.md.warehouse.WmsWarehouseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * WMS 波次单 DO
 *
 * <p>波次单是出库单的聚合容器，将相同仓库/相同波次策略的出库单合并为一个波次，
 * 用于批量拣货、批量复核、批量出库。
 *
 * @author 智云
 */
@TableName("wms_wave_order")
@KeySequence("wms_wave_order_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WmsWaveOrderDO extends BaseDO {

    /**
     * 主键编号
     */
    @TableId
    private Long id;
    /**
     * 波次单号
     */
    private String no;
    /**
     * 仓库编号
     *
     * 关联 {@link WmsWarehouseDO#getId()}
     */
    private Long warehouseId;
    /**
     * 波次策略：1=按仓库合并 / 2=按客户合并 / 3=按商品合并 / 4=按承运商合并
     */
    private Integer strategy;
    /**
     * 单据日期
     */
    private LocalDateTime orderTime;
    /**
     * 状态
     *
     * 枚举 {@link cn.zhicloud.module.wms.enums.order.WmsOrderStatusEnum}
     */
    private Integer status;
    /**
     * 拣货员
     */
    private String picker;
    /**
     * 备注
     */
    private String remark;
    // ========= 汇总字段 =========
    /**
     * 出库单数
     */
    private Integer shipmentCount;
    /**
     * SKU 数
     */
    private Integer skuCount;
    /**
     * 总数量
     */
    private BigDecimal totalQuantity;
    /**
     * 总金额
     */
    private BigDecimal totalPrice;

}
