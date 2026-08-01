package cn.iocoder.yudao.module.crm.enums.visit;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * CRM 拜访类型枚举
 *
 * @author 芋道源码
 */
@Getter
@AllArgsConstructor
public enum CrmVisitTypeEnum implements ArrayValuable<Integer> {

    DOOR(1, "上门拜访"),
    PHONE(2, "电话拜访"),
    MEETING(3, "会议拜访");

    public static final Integer[] ARRAYS = Arrays.stream(values())
            .map(CrmVisitTypeEnum::getType).toArray(Integer[]::new);

    private final Integer type;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
