package cn.zhicloud.module.wms.dal.dataobject.md.sn;

import cn.zhicloud.framework.mybatis.core.dataobject.BaseDO;
import cn.zhicloud.module.wms.enums.DictTypeConstants;
import cn.zhicloud.module.wms.enums.md.WmsSnStatusEnum;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDateTime;

/**
 * WMS 序列号 DO
 *
 * 记录单件商品的序列号全生命周期：生成 -> 绑定入库 -> 在库 -> 出库 -> 退货，支持正反向追溯。
 *
 * @author 智云
 */
@TableName("wms_sn")
@KeySequence("wms_sn_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WmsSnDO extends BaseDO {

    /**
     * 主键编号
     */
    @TableId
    private Long id;
    /**
     * 序列号
     */
    private String sn;
    /**
     * 商品编号
     *
     * 关联 {@link cn.zhicloud.module.wms.dal.dataobject.md.item.WmsItemDO#getId()}
     */
    private Long productId;
    /**
     * 库存批次编号
     *
     * 关联 {@link cn.zhicloud.module.wms.dal.dataobject.inventory.WmsInventoryBatchDO#getId()}
     */
    private Long batchId;
    /**
     * 库存编号
     *
     * 关联 {@link cn.zhicloud.module.wms.dal.dataobject.inventory.WmsInventoryDO#getId()}
     */
    private Long inventoryId;
    /**
     * 序列号状态
     *
     * 字典 {@link DictTypeConstants#SN_STATUS}
     * 枚举 {@link WmsSnStatusEnum}
     */
    private String status;
    /**
     * 仓库编号
     */
    private Long warehouseId;
    /**
     * 库区编号
     */
    private Long zoneId;
    /**
     * 库位编号
     */
    private Long locationId;
    /**
     * 入库单编号（正向追溯：SN -> 入库单 -> 生产工单/采购单）
     */
    private Long inboundOrderId;
    /**
     * 出库单编号（反向追溯：SN -> 出库单 -> 客户）
     */
    private Long outboundOrderId;
    /**
     * 绑定时间
     */
    private LocalDateTime boundTime;
    /**
     * 出库时间
     */
    private LocalDateTime shippedTime;
    /**
     * 备注
     */
    private String remark;

}