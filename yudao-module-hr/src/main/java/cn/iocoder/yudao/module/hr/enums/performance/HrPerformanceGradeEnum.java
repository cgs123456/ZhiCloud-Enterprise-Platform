package cn.iocoder.yudao.module.hr.enums.performance;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * HR 绩效等级枚举
 *
 * @author yudao
 */
@RequiredArgsConstructor
@Getter
public enum HrPerformanceGradeEnum implements ArrayValuable<Integer> {

    A(10, "A"),
    B(20, "B"),
    C(30, "C"),
    D(40, "D"),
    ;

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(HrPerformanceGradeEnum::getGrade).toArray(Integer[]::new);

    /**
     * 等级
     */
    private final Integer grade;
    /**
     * 名称
     */
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}