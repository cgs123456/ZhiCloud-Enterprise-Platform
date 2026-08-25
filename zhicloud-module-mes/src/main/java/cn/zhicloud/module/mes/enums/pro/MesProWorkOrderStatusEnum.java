package cn.zhicloud.module.mes.enums.pro;

import cn.zhicloud.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * MES 生产工单状态枚举
 *
 * 状态转换图：
 * <pre>
 * PREPARE ──确认──▶ CONFIRMED ──派工──▶ DISPATCHED ──开工──▶ REPORTING ──完工──▶ FINISHED ──结算──▶ CLOSED
 *    │                  │                    │                    │
 *    └──────────取消──────────────────────────────────────────────▶ CANCELED
 * </pre>
 *
 * @author 智云
 */
@Getter
@AllArgsConstructor
public enum MesProWorkOrderStatusEnum implements ArrayValuable<Integer> {

    /**
     * 草稿
     *
     * 对应 MesProWorkOrderService#createWorkOrder 方法
     */
    PREPARE(0, "草稿"),
    /**
     * 已确认
     *
     * 对应 MesProWorkOrderService#confirmWorkOrder 方法
     */
    CONFIRMED(1, "已确认"),
    /**
     * 已完成
     *
     * 对应 MesProWorkOrderService#finishWorkOrder 方法
     */
    FINISHED(2, "已完成"),
    /**
     * 已取消
     *
     * 对应 MesProWorkOrderService#cancelWorkOrder 方法
     */
    CANCELED(3, "已取消"),
    /**
     * 已派工
     *
     * 已派工给车间/班组，等待开工
     */
    DISPATCHED(4, "已派工"),
    /**
     * 报工中
     *
     * 已开始报工，部分完工
     */
    REPORTING(5, "报工中"),
    /**
     * 已关闭
     *
     * 生产完成后的最终关闭状态（区别于 FINISHED：FINISHED 表示生产完成，CLOSED 表示已结算关闭）
     */
    CLOSED(6, "已关闭");

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(MesProWorkOrderStatusEnum::getStatus).toArray(Integer[]::new);

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
