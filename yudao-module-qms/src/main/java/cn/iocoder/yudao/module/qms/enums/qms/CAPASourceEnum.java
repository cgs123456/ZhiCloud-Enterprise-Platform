package cn.iocoder.yudao.module.qms.enums.qms;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * QMS CAPA 来源枚举
 *
 * @author 芋道源码
 */
@RequiredArgsConstructor
@Getter
public enum CAPASourceEnum implements ArrayValuable<Integer> {

    INTERNAL(10, "内部"),
    EXTERNAL(20, "外部"),
    CUSTOMER_COMPLAINT(30, "客户投诉"),
    AUDIT(40, "审核"),
    ;

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(CAPASourceEnum::getSource).toArray(Integer[]::new);

    /**
     * 来源
     */
    private final Integer source;
    /**
     * 名称
     */
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
