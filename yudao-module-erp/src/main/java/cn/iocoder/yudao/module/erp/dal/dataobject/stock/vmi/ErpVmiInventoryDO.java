package cn.iocoder.yudao.module.erp.dal.dataobject.stock.vmi;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;

/**
 * ERP VMI 供应商管理库存 DO
 *
 * @author 芋道源码
 */
@TableName("erp_vmi_inventory")
@KeySequence("erp_vmi_inventory_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpVmiInventoryDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 供应商编号
     */
    private Long supplierId;
    /**
     * 仓库编号
     */
    private Long warehouseId;
    /**
     * 产品编号
     */
    private Long productId;
    /**
     * 产品名称（冗余）
     */
    private String productName;
    /**
     * 当前库存数量
     */
    private BigDecimal quantity;
    /**
     * 可用库存数量
     */
    private BigDecimal availableQuantity;
    /**
     * 锁定库存数量
     */
    private BigDecimal lockedQuantity;
    /**
     * 最低库存
     */
    private BigDecimal minQuantity;
    /**
     * 最高库存
     */
    private BigDecimal maxQuantity;
    /**
     * 补货点
     */
    private BigDecimal replenishmentPoint;
    /**
     * 备注
     */
    private String remark;

}