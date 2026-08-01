package cn.iocoder.yudao.module.erp.enums.finance;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * ERP 合并方法枚举（P1-合并报表引擎）
 *
 * @author 芋道源码
 */
@Getter
@AllArgsConstructor
public enum ErpConsolidationMethodEnum implements ArrayValuable<Integer> {

    /**
     * 完全合并
     */
    FULL(10, "完全合并"),
    /**
     * 比例合并
     */
    PROPORTIONAL(20, "比例合并"),
    /**
     * 权益法
     */
    EQUITY(30, "权益法");

    public static final Integer[] ARRAYS = Arrays.stream(values())
            .map(ErpConsolidationMethodEnum::getMethod).toArray(Integer[]::new);

    private final Integer method;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
