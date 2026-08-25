package cn.zhicloud.module.erp.enums.finance.cost;

import cn.zhicloud.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * ERP 标准成本状态枚举
 *
 * <p>状态流转：草稿(DRAFT) → 已生效(EFFECTIVE) → 已失效(EXPIRED)
 *
 * @author 智云
 */
@Getter
@AllArgsConstructor
public enum ErpStandardCostStatusEnum implements ArrayValuable<Integer> {

    /**
     * 草稿
     */
    DRAFT(10, "草稿"),
    /**
     * 已生效
     */
    EFFECTIVE(20, "已生效"),
    /**
     * 已失效
     */
    EXPIRED(30, "已失效");

    public static final Integer[] ARRAYS = Arrays.stream(values())
            .map(ErpStandardCostStatusEnum::getStatus).toArray(Integer[]::new);

    private final Integer status;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
