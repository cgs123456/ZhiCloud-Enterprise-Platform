package cn.zhicloud.module.wms.enums.order;

import cn.zhicloud.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * WMS 盘点类型枚举
 *
 * @author 智云
 */
@Getter
@AllArgsConstructor
public enum WmsCheckTypeEnum implements ArrayValuable<Integer> {

    BLIND(1, "暗盘"),
    OPEN(2, "明盘"),
    CYCLE(3, "循环盘点");

    public static final Integer[] ARRAYS = Arrays.stream(values())
            .map(WmsCheckTypeEnum::getType).toArray(Integer[]::new);

    /**
     * 类型
     */
    private final Integer type;
    /**
     * 名称
     */
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}