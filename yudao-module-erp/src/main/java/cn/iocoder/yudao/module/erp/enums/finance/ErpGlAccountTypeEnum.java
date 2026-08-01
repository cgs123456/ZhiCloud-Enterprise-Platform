package cn.iocoder.yudao.module.erp.enums.finance;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * ERP 会计科目类型枚举（P0-7）
 *
 * <p>遵循中国会计准则六大类：资产、负债、权益、收入、支出、共同。
 * 科目类型决定余额方向与财务报表归类。
 *
 * @author 芋道源码
 */
@RequiredArgsConstructor
@Getter
public enum ErpGlAccountTypeEnum implements ArrayValuable<Integer> {

    ASSET(10, "资产"),
    LIABILITY(20, "负债"),
    EQUITY(30, "所有者权益"),
    REVENUE(40, "收入"),
    EXPENSE(50, "费用"),
    COMMON(60, "共同");

    public static final Integer[] ARRAYS = Arrays.stream(values())
            .map(ErpGlAccountTypeEnum::getType).toArray(Integer[]::new);

    private final Integer type;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
