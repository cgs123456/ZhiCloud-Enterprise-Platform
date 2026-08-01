package cn.iocoder.yudao.module.wms.enums.order;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * WMS 波次策略枚举
 *
 * @author 芋道源码
 */
@Getter
@AllArgsConstructor
public enum WmsWaveStrategyEnum implements ArrayValuable<Integer> {

    BY_WAREHOUSE(1, "按仓库合并"),
    BY_MERCHANT(2, "按客户合并"),
    BY_ITEM(3, "按商品合并"),
    BY_CARRIER(4, "按承运商合并");

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(WmsWaveStrategyEnum::getStrategy)
            .toArray(Integer[]::new);

    /**
     * 策略编码
     */
    private final Integer strategy;
    /**
     * 名称
     */
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
