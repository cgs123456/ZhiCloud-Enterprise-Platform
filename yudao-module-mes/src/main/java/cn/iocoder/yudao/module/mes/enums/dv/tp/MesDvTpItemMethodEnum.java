package cn.iocoder.yudao.module.mes.enums.dv.tp;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * MES TPM 计划项目方法枚举
 *
 * @author 芋道源码
 */
@Getter
@AllArgsConstructor
public enum MesDvTpItemMethodEnum implements ArrayValuable<Integer> {

    VISUAL(10, "目视"),
    AUDITORY(20, "听觉"),
    MEASURE(30, "测量"),
    OPERATION(40, "操作");

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(MesDvTpItemMethodEnum::getMethod).toArray(Integer[]::new);

    /**
     * 方法值
     */
    private final Integer method;
    /**
     * 方法名
     */
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}