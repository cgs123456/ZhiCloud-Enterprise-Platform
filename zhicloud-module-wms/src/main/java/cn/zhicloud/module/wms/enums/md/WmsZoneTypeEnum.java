package cn.zhicloud.module.wms.enums.md;

import cn.zhicloud.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * WMS 库区类型枚举
 *
 * @author 智云
 */
@Getter
@AllArgsConstructor
public enum WmsZoneTypeEnum implements ArrayValuable<Integer> {

    STORAGE(10, "存储区"),
    PICKING(20, "拣货区"),
    RETURN(30, "退货区"),
    UNQUALIFIED(40, "不合格品区");

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(WmsZoneTypeEnum::getType).toArray(Integer[]::new);

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
