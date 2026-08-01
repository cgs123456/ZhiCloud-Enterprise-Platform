package cn.iocoder.yudao.module.erp.enums.finance.cost;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * ERP 成本项目类型枚举
 *
 * @author 芋道源码
 */
@Getter
@AllArgsConstructor
public enum ErpCostItemTypeEnum implements ArrayValuable<Integer> {

    /**
     * 材料
     */
    MATERIAL(10, "材料"),
    /**
     * 人工
     */
    LABOR(20, "人工"),
    /**
     * 制造费用
     */
    OVERHEAD(30, "制造费用"),
    /**
     * 外协
     */
    OUTSOURCING(40, "外协"),
    /**
     * 其他
     */
    OTHER(50, "其他");

    public static final Integer[] ARRAYS = Arrays.stream(values())
            .map(ErpCostItemTypeEnum::getType).toArray(Integer[]::new);

    private final Integer type;
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
