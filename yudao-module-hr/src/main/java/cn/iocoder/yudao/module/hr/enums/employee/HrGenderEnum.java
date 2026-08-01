package cn.iocoder.yudao.module.hr.enums.employee;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * HR 性别枚举
 *
 * @author yudao
 */
@RequiredArgsConstructor
@Getter
public enum HrGenderEnum implements ArrayValuable<Integer> {

    MALE(10, "男"),
    FEMALE(20, "女"),
    ;

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(HrGenderEnum::getGender).toArray(Integer[]::new);

    /**
     * 性别
     */
    private final Integer gender;
    /**
     * 名称
     */
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}