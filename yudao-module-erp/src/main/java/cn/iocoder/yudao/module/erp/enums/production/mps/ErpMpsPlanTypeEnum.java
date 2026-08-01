package cn.iocoder.yudao.module.erp.enums.production.mps;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * ERP 主生产计划类型枚举
 *
 * @author 芋道源码
 */
@Getter
@AllArgsConstructor
public enum ErpMpsPlanTypeEnum implements ArrayValuable<Integer> {

    MONTH(10, "月度"),
    QUARTER(20, "季度"),
    YEAR(30, "年度");

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(ErpMpsPlanTypeEnum::getType).toArray(Integer[]::new);

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
