package cn.iocoder.yudao.module.erp.enums.production.mps;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * ERP 主生产计划状态枚举
 *
 * @author 芋道源码
 */
@Getter
@AllArgsConstructor
public enum ErpMpsPlanStatusEnum implements ArrayValuable<Integer> {

    DRAFT(10, "草稿"),
    CONFIRMED(20, "已确认"),
    RELEASED_MRP(30, "已下发MRP"),
    CLOSED(40, "已关闭");

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(ErpMpsPlanStatusEnum::getStatus).toArray(Integer[]::new);

    /**
     * 状态值
     */
    private final Integer status;
    /**
     * 状态名
     */
    private final String name;

    @Override
    public Integer[] array() {
        return ARRAYS;
    }

}