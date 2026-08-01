package cn.iocoder.yudao.module.erp.enums.finance.cost;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * ERP 成本差异类型枚举
 *
 * <p>差异类型：
 * <ul>
 *   <li>{@link #FAVORABLE} 有利差异：实际成本 &lt; 标准成本（variance_amount &lt; 0）</li>
 *   <li>{@link #UNFAVORABLE} 不利差异：实际成本 &gt; 标准成本（variance_amount &gt; 0）</li>
 * </ul>
 *
 * @author 芋道源码
 */
@Getter
@AllArgsConstructor
public enum ErpVarianceTypeEnum implements ArrayValuable<Integer> {

    /**
     * 有利差异
     */
    FAVORABLE(10, "有利差异"),
    /**
     * 不利差异
     */
    UNFAVORABLE(20, "不利差异");

    public static final Integer[] ARRAYS = Arrays.stream(values())
            .map(ErpVarianceTypeEnum::getType).toArray(Integer[]::new);

    private final Integer type;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
