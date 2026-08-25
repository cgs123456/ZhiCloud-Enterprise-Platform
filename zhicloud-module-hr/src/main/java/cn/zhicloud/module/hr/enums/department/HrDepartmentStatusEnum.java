package cn.zhicloud.module.hr.enums.department;

import cn.zhicloud.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * HR 部门状态枚举
 *
 * @author zhicloud
 */
@RequiredArgsConstructor
@Getter
public enum HrDepartmentStatusEnum implements ArrayValuable<Integer> {

    ENABLE(10, "启用"),
    DISABLE(20, "禁用"),
    ;

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(HrDepartmentStatusEnum::getStatus).toArray(Integer[]::new);

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