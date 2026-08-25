package cn.zhicloud.module.wms.enums.md;

import cn.zhicloud.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * WMS 库位类型枚举
 *
 * @author 智云
 */
@Getter
@AllArgsConstructor
public enum WmsLocationTypeEnum implements ArrayValuable<Integer> {

    STORAGE(10, "储位"),
    PICKING(20, "拣货位"),
    RECEIPT(30, "收货位"),
    SHIPMENT(40, "发货位");

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(WmsLocationTypeEnum::getType).toArray(Integer[]::new);

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
