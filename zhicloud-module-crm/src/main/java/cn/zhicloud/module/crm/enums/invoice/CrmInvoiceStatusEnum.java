package cn.zhicloud.module.crm.enums.invoice;

import cn.zhicloud.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * CRM 开票状态枚举
 *
 * @author 智云
 */
@Getter
@AllArgsConstructor
public enum CrmInvoiceStatusEnum implements ArrayValuable<Integer> {

    WAIT(1, "待开票"),
    DONE(2, "已开票"),
    CANCEL(3, "已作废");

    public static final Integer[] ARRAYS = Arrays.stream(values())
            .map(CrmInvoiceStatusEnum::getStatus).toArray(Integer[]::new);

    private final Integer status;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
