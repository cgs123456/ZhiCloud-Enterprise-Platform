package cn.iocoder.yudao.module.erp.enums.finance;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * ERP 固定资产折旧方法枚举
 *
 * @author 芋道源码
 */
@Getter
@AllArgsConstructor
public enum ErpDepreciationMethodEnum implements ArrayValuable<Integer> {

    /**
     * 直线法（年限平均法）
     */
    STRAIGHT_LINE(10, "直线法"),
    /**
     * 双倍余额递减法（预留）
     */
    DOUBLE_DECLINING_BALANCE(20, "双倍余额递减法"),
    /**
     * 年数总和法（预留）
     */
    SUM_OF_YEARS_DIGITS(30, "年数总和法");

    public static final Integer[] ARRAYS = Arrays.stream(values())
            .map(ErpDepreciationMethodEnum::getMethod).toArray(Integer[]::new);

    private final Integer method;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
