package cn.zhicloud.module.erp.enums.finance;

import cn.zhicloud.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * ERP 固定资产变动类型枚举
 *
 * @author 智云
 */
@Getter
@AllArgsConstructor
public enum ErpFaChangeTypeEnum implements ArrayValuable<Integer> {

    /**
     * 部门转移
     */
    DEPARTMENT_TRANSFER(10, "部门转移"),
    /**
     * 状态变动
     */
    STATUS_CHANGE(20, "状态变动"),
    /**
     * 原值调整
     */
    ORIGINAL_VALUE_ADJUST(30, "原值调整"),
    /**
     * 使用年限调整
     */
    USEFUL_LIFE_ADJUST(40, "使用年限调整"),
    /**
     * 残值调整
     */
    SALVAGE_VALUE_ADJUST(50, "残值调整"),
    /**
     * 折旧方法变更
     */
    DEPRECIATION_METHOD_CHANGE(60, "折旧方法变更");

    public static final Integer[] ARRAYS = Arrays.stream(values())
            .map(ErpFaChangeTypeEnum::getType).toArray(Integer[]::new);

    private final Integer type;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
