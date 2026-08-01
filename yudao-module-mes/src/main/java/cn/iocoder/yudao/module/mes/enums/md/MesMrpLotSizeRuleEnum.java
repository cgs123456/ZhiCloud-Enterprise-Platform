package cn.iocoder.yudao.module.mes.enums.md;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * MRP 批量规则枚举
 *
 * <p>用于 MRP 计划订单量的批量调整：
 * <ul>
 *   <li>LFL：按需批量（Lot-For-Lot），计划订单=净需求，不调整</li>
 *   <li>FOQ：固定批量（Fixed Order Quantity），计划订单=固定批量或其倍数</li>
 *   <li>POQ：周期批量（Period Order Quantity），将若干周期净需求合并</li>
 *   <li>MULTIPLES：倍数批量，计划订单为指定倍数的向上取整</li>
 * </ul>
 *
 * @author 芋道源码
 */
@Getter
@AllArgsConstructor
public enum MesMrpLotSizeRuleEnum {

    LFL("LFL", "按需批量 Lot-For-Lot"),
    FOQ("FOQ", "固定批量 Fixed Order Quantity"),
    POQ("POQ", "周期批量 Period Order Quantity"),
    MULTIPLES("MULTIPLES", "倍数批量 Multiples");

    /**
     * 规则代码
     */
    private final String code;
    /**
     * 描述
     */
    private final String description;

}
