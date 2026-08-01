package cn.iocoder.yudao.module.qms.enums.audit;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * QMS 不符合项严重程度枚举
 *
 * @author 芋道源码
 */
@RequiredArgsConstructor
@Getter
public enum QmsNcSeverityEnum implements ArrayValuable<Integer> {

    SERIOUS(10, "严重"),
    GENERAL(20, "一般"),
    OBSERVATION(30, "观察"),
    ;

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(QmsNcSeverityEnum::getSeverity).toArray(Integer[]::new);

    /**
     * 严重程度
     */
    private final Integer severity;
    /**
     * 名称
     */
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
