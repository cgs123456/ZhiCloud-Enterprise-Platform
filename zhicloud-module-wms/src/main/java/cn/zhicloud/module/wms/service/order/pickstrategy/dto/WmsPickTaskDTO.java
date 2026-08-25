package cn.zhicloud.module.wms.service.order.pickstrategy.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * WMS 拣货任务 DTO（用于路径优化）
 *
 * <p>仅包含路径优化所需字段，附带伪坐标用于就近排序算法。
 *
 * @author 智云
 */
@Data
public class WmsPickTaskDTO {

    /**
     * 任务编号
     */
    private Long id;
    /**
     * 任务编号（业务编号）
     */
    private String taskNo;
    /**
     * 出库单编号
     */
    private Long shipmentOrderId;
    /**
     * 波次单编号
     */
    private Long waveOrderId;
    /**
     * 商品 SKU 编号
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
     * 库位编号
     */
    private Long locationId;
    /**
     * 拣货顺序
     */
    private Integer pickSequence;
    /**
     * 伪坐标 X（基于库位/库存编号生成，用于就近排序）
     */
    private Integer coordX;
    /**
     * 伪坐标 Y（基于库位/库存编号生成，用于就近排序）
     */
    private Integer coordY;

}
