package cn.zhicloud.module.erp.enums.finance;

import cn.zhicloud.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * ERP 预算状态枚举
 *
 * @author 智云
 */
@Getter
@AllArgsConstructor
public enum ErpBudgetStatusEnum implements ArrayValuable<Integer> {

    /**
     * 草稿
     */
    DRAFT(10, "草稿"),
    /**
     * 已审批
     */
    APPROVED(20, "已审批"),
    /**
     * 执行中
     */
    EXECUTING(30, "执行中"),
    /**
     * 已关闭
     */
    CLOSED(40, "已关闭");

    public static final Integer[] ARRAYS = Arrays.stream(values())
            .map(ErpBudgetStatusEnum::getStatus).toArray(Integer[]::new);

    private final Integer status;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
