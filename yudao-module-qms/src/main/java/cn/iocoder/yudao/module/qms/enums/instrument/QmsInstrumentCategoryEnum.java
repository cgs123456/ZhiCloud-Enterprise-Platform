package cn.iocoder.yudao.module.qms.enums.instrument;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * QMS 计量器具类别枚举
 *
 * @author 芋道源码
 */
@RequiredArgsConstructor
@Getter
public enum QmsInstrumentCategoryEnum implements ArrayValuable<Integer> {

    LENGTH(10, "长度类"),
    TEMPERATURE(20, "温度类"),
    MECHANICS(30, "力学类"),
    ELECTRICAL(40, "电学类"),
    OPTICAL(50, "光学类"),
    OTHER(60, "其他"),
    ;

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(QmsInstrumentCategoryEnum::getCategory).toArray(Integer[]::new);

    /**
     * 类别
     */
    private final Integer category;
    /**
     * 名称
     */
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
