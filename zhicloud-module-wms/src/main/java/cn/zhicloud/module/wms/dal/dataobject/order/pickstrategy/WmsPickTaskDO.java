package cn.zhicloud.module.wms.dal.dataobject.order.pickstrategy;

import cn.zhicloud.framework.mybatis.core.dataobject.BaseDO;
import cn.zhicloud.module.wms.dal.dataobject.md.item.WmsItemSkuDO;
import cn.zhicloud.module.wms.dal.dataobject.md.location.WmsLocationDO;
import cn.zhicloud.module.wms.dal.dataobject.order.shipment.WmsShipmentOrderDO;
import cn.zhicloud.module.wms.dal.dataobject.order.wave.WmsWaveOrderDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * WMS 拣货任务 DO
 *
 * <p>由拣选策略引擎根据出库单/波次单生成，记录每个 SKU 的拣货数量、库位、顺序等信息。
 *
 * @author 智云
 */
@TableName("wms_pick_task")
@KeySequence("wms_pick_task_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WmsPickTaskDO extends BaseDO {

    /**
     * 主键编号
     */
    @TableId
    private Long id;
    /**
     * 任务编号
     */
    private String taskNo;
    /**
     * 出库单编号
     *
     * 关联 {@link WmsShipmentOrderDO#getId()}
     */
    private Long shipmentOrderId;
    /**
     * 波次单编号
     *
     * 关联 {@link WmsWaveOrderDO#getId()}
     */
    private Long waveOrderId;
    /**
     * 商品 SKU 编号
     *
     * 关联 {@link WmsItemSkuDO#getId()}
     */
    private Long skuId;
    /**
     * 商品名称
     */
    private String productName;
    /**
     * 应拣数量
     */
    private BigDecimal quantity;
    /**
     * 已拣数量
     */
    private BigDecimal pickedQuantity;
    /**
     * 库位编号
     *
     * 关联 {@link WmsLocationDO#getId()}
     */
    private Long locationId;
    /**
     * 拣货顺序
     */
    private Integer pickSequence;
    /**
     * 状态
     *
     * 枚举：10 待拣 / 20 已拣 / 30 已确认
     */
    private Integer status;
    /**
     * 拣货员用户编号
     */
    private Long pickerUserId;
    /**
     * 拣货时间
     */
    private LocalDateTime pickTime;
    /**
     * 备注
     */
    private String remark;

}
