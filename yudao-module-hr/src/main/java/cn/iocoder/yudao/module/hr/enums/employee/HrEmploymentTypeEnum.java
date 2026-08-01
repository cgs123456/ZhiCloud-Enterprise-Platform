package cn.iocoder.yudao.module.hr.enums.employee;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * HR 用工类型枚举
 *
 * @author yudao
 */
@RequiredArgsConstructor
@Getter
public enum HrEmploymentTypeEnum implements ArrayValuable<Integer> {

    FULL_TIME(10, "全职"),
    PART_TIME(20, "兼职"),
    INTERN(30, "实习"),
    OUTSOURCE(40, "外包"),
    ;

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(HrEmploymentTypeEnum::getType).toArray(Integer[]::new);

    /**
     * 类型
     */
    private final Integer type;
    /**
     * 名称
     */
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}