package cn.zhicloud.module.erp.dal.dataobject.stock;

import cn.zhicloud.framework.mybatis.core.dataobject.BaseDO;
import cn.zhicloud.module.erp.dal.dataobject.product.ErpProductDO;
import cn.zhicloud.module.erp.enums.stock.ErpStockBatchStatusEnum;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * ERP 库存批次 DO
 *
 * <p>按批次维度管理库存，支持生产日期、过期日期、批次状态。
 * 同一产品 + 仓库下可有多个批次。
 *
 * @author 智云
 */
@TableName("erp_stock_batch")
@KeySequence("erp_stock_batch_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpStockBatchDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 批次号
     */
    private String batchNo;
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
     * 生产日期
     */
    private LocalDate productionDate;
    /**
     * 过期日期
     */
    private LocalDate expiryDate;
    /**
     * 批次数量
     */
    private BigDecimal quantity;
    /**
     * 批次状态
     *
     * 枚举 {@link ErpStockBatchStatusEnum}
     */
    private Integer status;
    /**
     * 备注
     */
    private String remark;

}
