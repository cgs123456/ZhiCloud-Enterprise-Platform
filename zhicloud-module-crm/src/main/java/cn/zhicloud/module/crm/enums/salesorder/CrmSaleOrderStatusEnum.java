package cn.zhicloud.module.crm.enums.salesorder;

import cn.zhicloud.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * CRM 销售订单状态枚举
 *
 * @author dhb52
 */
@RequiredArgsConstructor
@Getter
public enum CrmSaleOrderStatusEnum implements ArrayValuable<Integer> {

    DRAFT(10, "草稿"),
    CONFIRMED(20, "已确认"),
    SHIPPED(30, "已发货"),
    COMPLETED(40, "已完成"),
    CANCELED(50, "已取消");

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(CrmSaleOrderStatusEnum::getStatus).toArray(Integer[]::new);

    private final Integer status;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
