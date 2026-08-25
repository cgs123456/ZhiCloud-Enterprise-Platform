package cn.zhicloud.module.erp.enums.finance;

import cn.zhicloud.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * ERP 成本分摊类型枚举
 *
 * @author 智云
 */
@RequiredArgsConstructor
@Getter
public enum ErpCostAllocationTypeEnum implements ArrayValuable<Integer> {

    MANUAL(10, "手工分摊"),
    RULE_BASED(20, "规则分摊");

    public static final Integer[] ARRAYS = Arrays.stream(values())
            .map(ErpCostAllocationTypeEnum::getType).toArray(Integer[]::new);

    private final Integer type;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
