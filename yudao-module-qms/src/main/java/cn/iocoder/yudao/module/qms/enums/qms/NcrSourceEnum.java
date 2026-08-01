package cn.iocoder.yudao.module.qms.enums.qms;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * QMS NCR 不合格品来源枚举
 *
 * @author 芋道源码
 */
@RequiredArgsConstructor
@Getter
public enum NcrSourceEnum implements ArrayValuable<Integer> {

    IQC(10, "IQC 来料检验"),
    IPQC(20, "IPQC 过程检验"),
    FQC(30, "FQC 成品检验"),
    OQC(40, "OQC 出货检验"),
    CUSTOMER_COMPLAINT(50, "客户投诉"),
    ;

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(NcrSourceEnum::getSource).toArray(Integer[]::new);

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
