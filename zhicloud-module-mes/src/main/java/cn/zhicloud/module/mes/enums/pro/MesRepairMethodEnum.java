package cn.zhicloud.module.mes.enums.pro;

import cn.zhicloud.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * MES 返工处理方式枚举
 *
 * @author 智云
 */
@Getter
@AllArgsConstructor
public enum MesRepairMethodEnum implements ArrayValuable<Integer> {

    REPAIR(10, "返修"),
    DOWNGRADE(20, "降级"),
    SCRAP(30, "报废"),
    REWORK(40, "重新加工");

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(MesRepairMethodEnum::getMethod).toArray(Integer[]::new);

    /**
     * 方式值
     */
    private final Integer method;
    /**
     * 方式名
     */
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
