package cn.iocoder.yudao.module.crm.enums.salesorder;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * CRM 销售订单付款状态枚举
 *
 * @author dhb52
 */
@RequiredArgsConstructor
@Getter
public enum CrmSaleOrderPaymentStatusEnum implements ArrayValuable<Integer> {

    UNPAID(10, "未付款"),
    PARTIAL_PAID(20, "部分付款"),
    PAID(30, "已付款");

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(CrmSaleOrderPaymentStatusEnum::getStatus).toArray(Integer[]::new);

    private final Integer status;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
