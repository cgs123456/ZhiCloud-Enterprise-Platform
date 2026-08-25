package cn.zhicloud.module.mes.enums.dv.tp;

import cn.zhicloud.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * MES TPM 计划类型枚举
 *
 * @author 智云
 */
@Getter
@AllArgsConstructor
public enum MesDvTpPlanTypeEnum implements ArrayValuable<Integer> {

    AM(10, "自主维护"),
    PM(20, "计划维护"),
    PDM(30, "预测性维护");

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(MesDvTpPlanTypeEnum::getType).toArray(Integer[]::new);

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