package cn.zhicloud.module.erp.service.finance.cost.bom;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * BOM 子件信息（用于成本卷积计算）
 *
 * <p>描述一个产品在 BOM 中的子件：子件产品编号 + 用量（单位用量）。
 *
 * @author 智云
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErpBomComponent {

    /**
     * 子件产品编号
     */
    private Long productId;
    /**
     * 用量（生产 1 个父件所需的子件数量）
     */
    private BigDecimal quantity;

}
