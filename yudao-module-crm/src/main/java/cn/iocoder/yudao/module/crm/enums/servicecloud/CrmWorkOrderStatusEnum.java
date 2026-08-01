package cn.iocoder.yudao.module.crm.enums.servicecloud;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * CRM 售后工单状态枚举
 *
 * @author dhb52
 */
@RequiredArgsConstructor
@Getter
public enum CrmWorkOrderStatusEnum implements ArrayValuable<Integer> {

    UNASSIGNED(10, "待分配"),
    ASSIGNED(20, "已分配"),
    PROCESSING(30, "处理中"),
    RESOLVED(40, "已解决"),
    CLOSED(50, "已关闭");

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(CrmWorkOrderStatusEnum::getStatus).toArray(Integer[]::new);

    private final Integer status;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
