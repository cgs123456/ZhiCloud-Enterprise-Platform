package cn.iocoder.yudao.module.erp.enums.finance;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * ERP 合并范围状态枚举（P1-合并报表引擎）
 *
 * @author 芋道源码
 */
@Getter
@AllArgsConstructor
public enum ErpConsolidationScopeStatusEnum implements ArrayValuable<Integer> {

    /**
     * 启用
     */
    ENABLED(10, "启用"),
    /**
     * 禁用
     */
    DISABLED(20, "禁用");

    public static final Integer[] ARRAYS = Arrays.stream(values())
            .map(ErpConsolidationScopeStatusEnum::getStatus).toArray(Integer[]::new);

    private final Integer status;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
