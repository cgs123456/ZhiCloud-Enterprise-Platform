package cn.iocoder.yudao.module.qms.enums.instrument;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * QMS 校准结果枚举
 *
 * @author 芋道源码
 */
@RequiredArgsConstructor
@Getter
public enum QmsCalibrationResultEnum implements ArrayValuable<Integer> {

    QUALIFIED(10, "合格"),
    UNQUALIFIED(20, "不合格"),
    ;

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(QmsCalibrationResultEnum::getResult).toArray(Integer[]::new);

    /**
     * 校准结果
     */
    private final Integer result;
    /**
     * 名称
     */
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
