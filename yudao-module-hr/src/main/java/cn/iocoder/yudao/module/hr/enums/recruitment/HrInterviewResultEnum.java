package cn.iocoder.yudao.module.hr.enums.recruitment;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@RequiredArgsConstructor
@Getter
public enum HrInterviewResultEnum implements ArrayValuable<Integer> {

    PASS(1, "通过"),
    PENDING(2, "待定"),
    FAIL(3, "不通过"),
    ;

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(HrInterviewResultEnum::getResult).toArray(Integer[]::new);

    private final Integer result;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}