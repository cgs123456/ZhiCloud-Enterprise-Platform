package cn.zhicloud.module.hr.enums.employee;

import cn.zhicloud.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * HR 员工状态枚举
 *
 * @author zhicloud
 */
@RequiredArgsConstructor
@Getter
public enum HrEmployeeStatusEnum implements ArrayValuable<Integer> {

    ACTIVE(10, "在职"),
    RESIGNED(20, "离职"),
    SUSPENDED(30, "停薪"),
    ;

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(HrEmployeeStatusEnum::getStatus).toArray(Integer[]::new);

    /**
     * 状态
     */
    private final Integer status;
    /**
     * 名称
     */
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}