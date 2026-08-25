package cn.zhicloud.module.wms.enums.inventory;

import cn.zhicloud.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * WMS 库存批次状态枚举
 *
 * @author 智云
 */
@Getter
@AllArgsConstructor
public enum WmsInventoryBatchStatusEnum implements ArrayValuable<String> {

    AVAILABLE("AVAILABLE", "正常"),
    NEAR_EXPIRY("NEAR_EXPIRY", "临期预警"),
    EXPIRED("EXPIRED", "已过期"),
    FROZEN("FROZEN", "已冻结");

    public static final String[] ARRAYS = Arrays.stream(values())
            .map(WmsInventoryBatchStatusEnum::getStatus).toArray(String[]::new);

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
