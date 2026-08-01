package cn.iocoder.yudao.module.qms.enums.qms;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * QMS 检验类型枚举
 *
 * @author 芋道源码
 */
@RequiredArgsConstructor
@Getter
public enum InspectionTypeEnum implements ArrayValuable<Integer> {

    IQC(10, "来料检验"),
    IPQC(20, "过程检验"),
    FQC(35, "成品检验"),
    OQC(30, "出货检验"),
    ;

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(InspectionTypeEnum::getType).toArray(Integer[]::new);

    /**
     * 类型
     */
    private final Integer type;
    /**
     * 名称
     */
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
