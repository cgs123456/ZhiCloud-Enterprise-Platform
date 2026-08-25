package cn.zhicloud.module.wms.dal.dataobject.inventory;

import cn.zhicloud.framework.mybatis.core.dataobject.BaseDO;
import cn.zhicloud.module.wms.enums.DictTypeConstants;
import cn.zhicloud.module.wms.enums.inventory.WmsInventoryBatchStatusEnum;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * WMS 库存批次 DO
 *
 * <p>记录同一库存行下不同批次/效期的库存明细，用于批次追溯与 FIFO/FEFO 出库策略。
 *
 * @author 智云
 */
@TableName("wms_inventory_batch")
@KeySequence("wms_inventory_batch_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WmsInventoryBatchDO extends BaseDO {

    /**
     * 主键编号
     */
    @TableId
    private Long id;
    /**
     * 库存编号
     *
     * 关联 {@link WmsInventoryDO#getId()}
     */
    private Long inventoryId;
    /**
     * 批次号
     */
    private String batchNo;
    /**
     * 生产日期
     */
    private LocalDate productionDate;
    /**
     * 过期日期
     */
    private LocalDate expiryDate;
    /**
     * 保质期天数
     *
     * <p>用于自动计算过期日期（生产日期 + 保质期天数 = 过期日期）。
     */
    private Integer shelfLifeDays;
    /**
     * 供应商批次号
     *
     * <p>供应商提供的批次号，用于供应链批次追溯。
     */
    private String supplierBatchNo;
    /**
     * 批次数量
     */
    private BigDecimal quantity;
    /**
     * 锁定数量
     *
     * 已被订单/波次预占但未出库的批次数量
     */
    private BigDecimal lockedQuantity;
    /**
     * 批次状态
     *
     * 枚举 {@link WmsInventoryBatchStatusEnum}
     * 字典 {@link DictTypeConstants#INVENTORY_BATCH_STATUS}
     */
    private String status;
    /**
     * 备注
     */
    private String remark;

}
