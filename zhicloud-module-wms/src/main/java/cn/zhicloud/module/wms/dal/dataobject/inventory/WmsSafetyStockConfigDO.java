package cn.zhicloud.module.wms.dal.dataobject.inventory;

import cn.zhicloud.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

/**
 * WMS 安全库存配置 DO
 *
 * @author 智云
 */
@TableName("wms_safety_stock_config")
@KeySequence("wms_safety_stock_config_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WmsSafetyStockConfigDO extends BaseDO {

    @TableId
    private Long id;
    private Long warehouseId;
    private Long productId;
    private BigDecimal safetyStock;
    private BigDecimal maxStock;
    private BigDecimal minStock;
    private String remark;

}
