package cn.iocoder.yudao.module.qms.enums.qms;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * QMS FMEA 类型枚举
 *
 * @author 芋道源码
 */
@RequiredArgsConstructor
@Getter
public enum FmeaTypeEnum implements ArrayValuable<Integer> {

    DFMEA(10, "设计 FMEA"),
    PFMEA(20, "过程 FMEA"),
    ;

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(FmeaTypeEnum::getFmeaType).toArray(Integer[]::new);

    /**
     * 类型
     */
    private final Integer fmeaType;
    /**
     * 名称
     */
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
