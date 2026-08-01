package cn.iocoder.yudao.module.wms.enums.inventory;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * WMS 库存预警类型枚举
 *
 * @author 芋道源码
 */
@Getter
@AllArgsConstructor
public enum WmsInventoryAlertTypeEnum implements ArrayValuable<String> {

    LOW_STOCK("LOW_STOCK", "低库存"),
    HIGH_STOCK("HIGH_STOCK", "高库存"),
    NEAR_EXPIRY("NEAR_EXPIRY", "临期"),
    EXPIRED("EXPIRED", "已过期"),
    DEAD_STOCK("DEAD_STOCK", "呆滞料");

    public static final String[] ARRAYS = Arrays.stream(values())
            .map(WmsInventoryAlertTypeEnum::getType).toArray(String[]::new);

    /**
     * 类型
     */
    private final String type;
    /**
     * 名称
     */
    private final String name;

    @Override
    public String[] array() {
        return ARRAYS;
    }

}
