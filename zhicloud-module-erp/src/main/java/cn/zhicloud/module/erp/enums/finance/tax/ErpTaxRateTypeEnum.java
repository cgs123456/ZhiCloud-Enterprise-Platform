package cn.zhicloud.module.erp.enums.finance.tax;

import cn.zhicloud.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * ERP 税率类型枚举
 *
 * @author 智云
 */
@Getter
@AllArgsConstructor
public enum ErpTaxRateTypeEnum implements ArrayValuable<Integer> {

    /**
     * 增值税
     */
    VAT(10, "增值税"),
    /**
     * 消费税
     */
    CONSUMPTION_TAX(20, "消费税"),
    /**
     * 附加税
     */
    SURTAX(30, "附加税");

    public static final Integer[] ARRAYS = Arrays.stream(values())
            .map(ErpTaxRateTypeEnum::getType).toArray(Integer[]::new);

    private final Integer type;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
