package cn.zhicloud.module.erp.dal.dataobject.stock.vmi;

import cn.zhicloud.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

/**
 * ERP VMI 补货建议明细 DO
 *
 * @author 智云
 */
@TableName("erp_vmi_replenishment_item")
@KeySequence("erp_vmi_replenishment_item_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpVmiReplenishmentItemDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 补货建议编号
     */
    private Long replenishmentId;
    /**
     * 产品编号
     */
    private Long productId;
    /**
     * 产品名称（冗余）
     */
    private String productName;
    /**
     * 建议补货数量
     */
    private BigDecimal quantity;
    /**
     * 当前库存数量
     */
    private BigDecimal currentQuantity;
    /**
     * 系统建议补货数量
     */
    private BigDecimal suggestedQuantity;
    /**
     * 备注
     */
    private String remark;

}
