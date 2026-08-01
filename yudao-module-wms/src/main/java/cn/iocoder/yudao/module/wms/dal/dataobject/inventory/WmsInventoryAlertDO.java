package cn.iocoder.yudao.module.wms.dal.dataobject.inventory;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import cn.iocoder.yudao.module.wms.enums.inventory.WmsInventoryAlertTypeEnum;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * WMS 库存预警记录 DO
 *
 * @author 芋道源码
 */
@TableName("wms_inventory_alert")
@KeySequence("wms_inventory_alert_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WmsInventoryAlertDO extends BaseDO {

    /**
     * 主键编号
     */
    @TableId
    private Long id;
    /**
     * 预警类型
     *
     * 枚举 {@link WmsInventoryAlertTypeEnum}
     */
    private String alertType;
    /**
     * 仓库编号
     */
    private Long warehouseId;
    /**
     * 商品 SKU 编号
     */
    private Long productId;
    /**
     * 批次号（保质期预警用）
     */
    private String batchNo;
    /**
     * 当前库存
     */
    private BigDecimal currentQuantity;
    /**
     * 阈值
     */
    private BigDecimal thresholdValue;
    /**
     * 预警时间
     */
    private LocalDateTime alertTime;
    /**
     * 状态（0 未处理 1 已确认 2 已处理）
     */
    private Integer status;
    /**
     * 备注
     */
    private String remark;

}