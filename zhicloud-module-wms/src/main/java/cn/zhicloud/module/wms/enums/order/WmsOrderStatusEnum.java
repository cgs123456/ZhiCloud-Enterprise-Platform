package cn.zhicloud.module.wms.enums.order;

import cn.zhicloud.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * WMS 单据状态枚举
 *
 * <p>状态机（出库单细化版本，其他单据仍走 PREPARE → FINISHED）：
 * <pre>
 *   PREPARE(0) → PICKING(10) → PICKED(20) → REVIEWED(30) → PACKED(40) → SHIPPED(50) → FINISHED(99)
 *                                                                                      ↑
 *   任意非 FINISHED 状态 → CANCELED(-1)
 * </pre>
 *
 * <p>向后兼容：PREPARE=0 保持不变；FINISHED 由历史值 4 迁移到 99；CANCELED 由历史值 5 迁移到 -1。
 * 详见 V45__wms_shipment_order_status.sql。
 *
 * @author 智云
 */
@Getter
@AllArgsConstructor
public enum WmsOrderStatusEnum implements ArrayValuable<Integer> {

    PREPARE(0, "草稿"),
    PICKING(10, "拣货中"),
    PICKED(20, "已拣货"),
    REVIEWED(30, "已复核"),
    PACKED(40, "已打包"),
    SHIPPED(50, "已发货"),
    FINISHED(99, "已完成"),
    CANCELED(-1, "已作废");

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(WmsOrderStatusEnum::getStatus)
            .toArray(Integer[]::new);

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