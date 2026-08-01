package cn.iocoder.yudao.module.erp.enums.finance;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * ERP 会计准则枚举（P1-多账簿）
 *
 * <p>支持的多账簿会计准则类型，同一账簿可按不同准则并行记账。
 *
 * @author 芋道源码
 */
@Getter
@AllArgsConstructor
public enum ErpAccountingStandardEnum implements ArrayValuable<Integer> {

    /**
     * 中国会计准则
     */
    CAS(10, "中国会计准则"),
    /**
     * 国际财务报告准则
     */
    IFRS(20, "国际财务报告准则"),
    /**
     * 美国会计准则
     */
    US_GAAP(30, "美国会计准则"),
    /**
     * 其他
     */
    OTHER(40, "其他");

    public static final Integer[] ARRAYS = Arrays.stream(values())
            .map(ErpAccountingStandardEnum::getType).toArray(Integer[]::new);

    private final Integer type;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
