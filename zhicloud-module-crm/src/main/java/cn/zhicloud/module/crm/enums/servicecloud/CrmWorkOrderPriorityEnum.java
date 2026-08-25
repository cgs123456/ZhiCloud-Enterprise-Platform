package cn.zhicloud.module.crm.enums.servicecloud;

import cn.zhicloud.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * CRM 售后工单优先级枚举
 *
 * @author dhb52
 */
@RequiredArgsConstructor
@Getter
public enum CrmWorkOrderPriorityEnum implements ArrayValuable<Integer> {

    LOW(10, "低"),
    MEDIUM(20, "中"),
    HIGH(30, "高"),
    URGENT(40, "紧急");

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(CrmWorkOrderPriorityEnum::getPriority).toArray(Integer[]::new);

    private final Integer priority;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
