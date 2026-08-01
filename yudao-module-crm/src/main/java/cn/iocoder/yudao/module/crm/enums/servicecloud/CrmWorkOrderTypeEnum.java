package cn.iocoder.yudao.module.crm.enums.servicecloud;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * CRM 售后工单类型枚举
 *
 * @author dhb52
 */
@RequiredArgsConstructor
@Getter
public enum CrmWorkOrderTypeEnum implements ArrayValuable<Integer> {

    INSTALL(10, "安装"),
    REPAIR(20, "维修"),
    COMPLAINT(30, "投诉"),
    CONSULT(40, "咨询"),
    RETURN_EXCHANGE(50, "退换货");

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(CrmWorkOrderTypeEnum::getType).toArray(Integer[]::new);

    private final Integer type;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
