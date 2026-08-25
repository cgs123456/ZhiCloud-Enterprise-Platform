package cn.zhicloud.module.erp.dal.dataobject.stock;

import cn.zhicloud.framework.mybatis.core.dataobject.BaseDO;
import cn.zhicloud.module.erp.dal.dataobject.product.ErpProductDO;
import cn.zhicloud.module.erp.enums.stock.ErpStockSerialStatusEnum;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * ERP 库存序列号 DO
 *
 * <p>按序列号维度管理库存，适用于高价值/需追溯的产品。
 * 序列号可通过 batchId 关联到具体批次。
 *
 * @author 智云
 */
@TableName("erp_stock_serial")
@KeySequence("erp_stock_serial_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpStockSerialDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 序列号
     */
    private String serialNo;
    /**
     * 产品编号
     *
     * 关联 {@link ErpProductDO#getId()}
     */
    private Long productId;
    /**
     * 仓库编号
     *
     * 关联 {@link ErpWarehouseDO#getId()}
     */
    private Long warehouseId;
    /**
     * 批次编号
     *
     * 关联 {@link ErpStockBatchDO#getId()}
     */
    private Long batchId;
    /**
     * 序列号状态
     *
     * 枚举 {@link ErpStockSerialStatusEnum}
     */
    private Integer status;
    /**
     * 备注
     */
    private String remark;

}
