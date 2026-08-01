package cn.iocoder.yudao.module.mes.enums.dv.tp;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * MES TPM 周期类型枚举
 *
 * @author 芋道源码
 */
@Getter
@AllArgsConstructor
public enum MesDvTpCycleTypeEnum implements ArrayValuable<Integer> {

    DAY(10, "日"),
    WEEK(20, "周"),
    MONTH(30, "月"),
    QUARTER(40, "季"),
    YEAR(50, "年");

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(MesDvTpCycleTypeEnum::getCycleType).toArray(Integer[]::new);

    /**
     * 周期类型值
     */
    private final Integer cycleType;
    /**
     * 周期类型名
     */
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}