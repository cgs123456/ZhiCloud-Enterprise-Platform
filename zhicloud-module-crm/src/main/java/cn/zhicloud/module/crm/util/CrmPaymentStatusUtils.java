package cn.zhicloud.module.crm.util;

import cn.zhicloud.module.crm.enums.salesorder.CrmSaleOrderPaymentStatusEnum;

import java.math.BigDecimal;

/**
 * CRM 付款状态工具类（§12.260 D1：销售订单付款状态 = 合同收款进度投影）
 *
 * <p>唯一投影函数，两个调用点：回款审批通过后的重算（回款 Service）与订单创建时的初始化（订单 Service）。
 * 口径：received = 该合同全部「审核通过」回款的 price 之和；totalPrice = 合同总金额（合同审批通过后不可修改）。
 * <ul>
 *     <li>received &gt;= totalPrice（且 totalPrice &gt; 0）→ 已付款(30)</li>
 *     <li>received &gt; 0 → 部分付款(20)</li>
 *     <li>否则 → 未付款(10)</li>
 * </ul>
 * totalPrice 为 null/小于等于 0 视为「无收齐基准」：仅部分付款/未付款可达（不抛错——无收齐基准是
 * 合同侧口径问题，订单侧不回滚资金事实）。
 *
 * @author 智云
 */
public class CrmPaymentStatusUtils {

    /**
     * 解析订单付款状态（投影函数，口径见类注释）
     *
     * @param receivedPrice 合同已审批回款金额（null 视为 0）
     * @param totalPrice    合同总金额（null/小于等于 0 视为无收齐基准）
     * @return 付款状态，见 {@link CrmSaleOrderPaymentStatusEnum}
     */
    public static Integer resolve(BigDecimal receivedPrice, BigDecimal totalPrice) {
        BigDecimal received = receivedPrice == null ? BigDecimal.ZERO : receivedPrice;
        if (totalPrice != null && totalPrice.compareTo(BigDecimal.ZERO) > 0
                && received.compareTo(totalPrice) >= 0) {
            return CrmSaleOrderPaymentStatusEnum.PAID.getStatus();
        }
        if (received.compareTo(BigDecimal.ZERO) > 0) {
            return CrmSaleOrderPaymentStatusEnum.PARTIAL_PAID.getStatus();
        }
        return CrmSaleOrderPaymentStatusEnum.UNPAID.getStatus();
    }

}
