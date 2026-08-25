package cn.zhicloud.module.crm.enums.salesorder;

import cn.zhicloud.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * CRM 销售订单发货状态枚举
 *
 * @author dhb52
 */
@RequiredArgsConstructor
@Getter
public enum CrmSaleOrderDeliveryStatusEnum implements ArrayValuable<Integer> {

    UN_SHIPPED(10, "未发货"),
    PARTIAL_SHIPPED(20, "部分发货"),
    SHIPPED(30, "已发货"),
    SIGNED(40, "已签收");

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(CrmSaleOrderDeliveryStatusEnum::getStatus).toArray(Integer[]::new);

    private final Integer status;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
