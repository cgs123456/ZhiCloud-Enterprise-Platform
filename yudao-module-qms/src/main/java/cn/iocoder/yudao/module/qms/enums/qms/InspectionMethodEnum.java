package cn.iocoder.yudao.module.qms.enums.qms;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * QMS 检验方法枚举
 *
 * @author 芋道源码
 */
@RequiredArgsConstructor
@Getter
public enum InspectionMethodEnum implements ArrayValuable<Integer> {

    APPEARANCE(10, "外观"),
    DIMENSION(20, "尺寸"),
    FUNCTION(30, "功能"),
    CHEMICAL(40, "理化"),
    ;

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(InspectionMethodEnum::getMethod).toArray(Integer[]::new);

    /**
     * 方法
     */
    private final Integer method;
    /**
     * 名称
     */
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
