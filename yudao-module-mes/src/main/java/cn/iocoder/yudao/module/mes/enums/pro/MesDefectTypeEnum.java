package cn.iocoder.yudao.module.mes.enums.pro;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * MES 缺陷类型枚举
 *
 * @author 芋道源码
 */
@Getter
@AllArgsConstructor
public enum MesDefectTypeEnum implements ArrayValuable<Integer> {

    DIMENSION(10, "尺寸不良"),
    APPEARANCE(20, "外观不良"),
    FUNCTION(30, "功能不良"),
    PERFORMANCE(40, "性能不良"),
    OTHER(50, "其他");

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(MesDefectTypeEnum::getType).toArray(Integer[]::new);

    /**
     * 类型值
     */
    private final Integer type;
    /**
     * 类型名
     */
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
