package cn.zhicloud.module.hr.enums.recruitment;

import cn.zhicloud.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@RequiredArgsConstructor
@Getter
public enum HrResumeStatusEnum implements ArrayValuable<Integer> {

    PENDING(0, "待筛选"),
    SCREEN_PASSED(1, "已通过"),
    INTERVIEWED(2, "已面试"),
    OFFERED(3, "已录用"),
    ELIMINATED(4, "已淘汰"),
    ;

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(HrResumeStatusEnum::getStatus).toArray(Integer[]::new);

    private final Integer status;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}