package cn.zhicloud.module.mes.enums.pro;

import cn.zhicloud.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * MES 上下工操作类型枚举
 *
 * @author 智云
 */
@Getter
@AllArgsConstructor
public enum MesProWorkRecordTypeEnum implements ArrayValuable<Integer> {

    CLOCK_IN(1, "上工"),
    CLOCK_OUT(2, "下工");

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(MesProWorkRecordTypeEnum::getType).toArray(Integer[]::new);

    /**
     * 操作类型
     */
    private final Integer type;
    /**
     * 操作名
     */
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
