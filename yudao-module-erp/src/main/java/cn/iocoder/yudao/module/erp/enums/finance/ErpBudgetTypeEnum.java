package cn.iocoder.yudao.module.erp.enums.finance;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * ERP 预算类型枚举
 *
 * @author 芋道源码
 */
@Getter
@AllArgsConstructor
public enum ErpBudgetTypeEnum implements ArrayValuable<Integer> {

    /**
     * 运营预算
     */
    OPERATING(10, "运营预算"),
    /**
     * 资本预算
     */
    CAPITAL(20, "资本预算"),
    /**
     * 现金流预算
     */
    CASH_FLOW(30, "现金流预算");

    public static final Integer[] ARRAYS = Arrays.stream(values())
            .map(ErpBudgetTypeEnum::getType).toArray(Integer[]::new);

    private final Integer type;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
