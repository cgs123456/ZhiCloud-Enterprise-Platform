package cn.zhicloud.module.erp.enums.finance;

import cn.zhicloud.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * ERP 资金计划类型枚举（P0-3）
 *
 * @author 智云
 */
@RequiredArgsConstructor
@Getter
public enum ErpFundPlanTypeEnum implements ArrayValuable<Integer> {

    RECEIPT(10, "收款"),
    PAYMENT(20, "付款");

    public static final Integer[] ARRAYS = Arrays.stream(values())
            .map(ErpFundPlanTypeEnum::getType).toArray(Integer[]::new);

    private final Integer type;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}