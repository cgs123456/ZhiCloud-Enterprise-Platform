package cn.iocoder.yudao.module.wms.enums.md;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * WMS 序列号状态枚举
 *
 * 状态机：GENERATED -> BOUND -> IN_STOCK -> SHIPPED -> RETURNED
 *
 * @author 芋道源码
 */
@Getter
@AllArgsConstructor
public enum WmsSnStatusEnum implements ArrayValuable<String> {

    GENERATED("GENERATED", "已生成"),
    BOUND("BOUND", "已绑定"),
    IN_STOCK("IN_STOCK", "在库"),
    SHIPPED("SHIPPED", "已出库"),
    RETURNED("RETURNED", "已退货");

    public static final String[] ARRAYS = Arrays.stream(values())
            .map(WmsSnStatusEnum::getStatus).toArray(String[]::new);

    /**
     * 状态
     */
    private final String status;
    /**
     * 名称
     */
    private final String name;

    @Override
    public String[] array() {
        return ARRAYS;
    }

}