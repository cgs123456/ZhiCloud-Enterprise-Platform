package cn.iocoder.yudao.module.mes.enums.dv.tp;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * MES TPM 计划状态枚举
 *
 * @author 芋道源码
 */
@Getter
@AllArgsConstructor
public enum MesDvTpPlanStatusEnum implements ArrayValuable<Integer> {

    ENABLED(10, "启用"),
    DISABLED(20, "禁用");

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(MesDvTpPlanStatusEnum::getStatus).toArray(Integer[]::new);

    /**
     * 状态值
     */
    private final Integer status;
    /**
     * 状态名
     */
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}