package cn.zhicloud.module.crm.enums.invoice;

import cn.zhicloud.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * CRM 发票类型枚举
 *
 * @author 智云
 */
@Getter
@AllArgsConstructor
public enum CrmInvoiceTypeEnum implements ArrayValuable<Integer> {

    SPECIAL(1, "增值税专用发票"),
    NORMAL(2, "增值税普通发票");

    public static final Integer[] ARRAYS = Arrays.stream(values())
            .map(CrmInvoiceTypeEnum::getType).toArray(Integer[]::new);

    private final Integer type;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
