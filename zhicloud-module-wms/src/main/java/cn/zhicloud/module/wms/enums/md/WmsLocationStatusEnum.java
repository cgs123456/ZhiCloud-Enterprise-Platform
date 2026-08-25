package cn.zhicloud.module.wms.enums.md;

import cn.zhicloud.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * WMS 库位状态枚举
 *
 * @author 智云
 */
@Getter
@AllArgsConstructor
public enum WmsLocationStatusEnum implements ArrayValuable<Integer> {

    IDLE(10, "空闲"),
    OCCUPIED(20, "占用"),
    LOCKED(30, "锁定"),
    DISABLED(40, "禁用");

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(WmsLocationStatusEnum::getStatus).toArray(Integer[]::new);

    /**
     * 状态
     */
    private final Integer status;
    /**
     * 名称
     */
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}
