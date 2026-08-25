package cn.zhicloud.module.mes.enums.pro;

import cn.zhicloud.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * MES 排产计划优先级枚举
 *
 * @author 智云
 */
@Getter
@AllArgsConstructor
public enum MesProApsPlanPriorityEnum implements ArrayValuable<Integer> {

    /**
     * 高优先级
     */
    HIGH(1, "高"),
    /**
     * 中优先级
     */
    MEDIUM(2, "中"),
    /**
     * 低优先级
     */
    LOW(3, "低");

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(MesProApsPlanPriorityEnum::getPriority).toArray(Integer[]::new);

    /**
     * 优先级值
     */
    private final Integer priority;
    /**
     * 优先级名
     */
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
