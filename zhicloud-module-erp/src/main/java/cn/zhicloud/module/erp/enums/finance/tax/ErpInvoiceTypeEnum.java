package cn.zhicloud.module.erp.enums.finance.tax;

import cn.zhicloud.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * ERP 发票类型枚举
 *
 * @author 智云
 */
@Getter
@AllArgsConstructor
public enum ErpInvoiceTypeEnum implements ArrayValuable<Integer> {

    /**
     * 销项专票
     */
    OUTPUT_SPECIAL(10, "销项专票"),
    /**
     * 销项普票
     */
    OUTPUT_NORMAL(20, "销项普票"),
    /**
     * 进项专票
     */
    INPUT_SPECIAL(30, "进项专票"),
    /**
     * 进项普票
     */
    INPUT_NORMAL(40, "进项普票");

    public static final Integer[] ARRAYS = Arrays.stream(values())
            .map(ErpInvoiceTypeEnum::getType).toArray(Integer[]::new);

    private final Integer type;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

    /**
     * 是否为销项发票
     */
    public static boolean isOutput(Integer type) {
        return OUTPUT_SPECIAL.getType().equals(type) || OUTPUT_NORMAL.getType().equals(type);
    }

    /**
     * 是否为进项发票
     */
    public static boolean isInput(Integer type) {
        return INPUT_SPECIAL.getType().equals(type) || INPUT_NORMAL.getType().equals(type);
    }

}
