package cn.iocoder.yudao.module.erp.enums.finance;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * ERP 现金流业务类型枚举（P0-3）
 *
 * @author 芋道源码
 */
@RequiredArgsConstructor
@Getter
public enum ErpCashFlowBizTypeEnum implements ArrayValuable<Integer> {

    RECEIPT(10, "收款"),
    PAYMENT(20, "付款");

    public static final Integer[] ARRAYS = Arrays.stream(values())
            .map(ErpCashFlowBizTypeEnum::getType).toArray(Integer[]::new);

    private final Integer type;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}