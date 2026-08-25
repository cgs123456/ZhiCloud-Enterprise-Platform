package cn.zhicloud.module.mes.enums.pro;

import cn.zhicloud.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * MES 排产计划状态枚举
 *
 * 状态转换图：
 * <pre>
 * DRAFT ──确认──▶ CONFIRMED ──执行──▶ EXECUTING ──完工──▶ COMPLETED
 *    │                  │                   │
 *    └──────────取消──────────────────────────────▶ CANCELLED
 * </pre>
 *
 * @author 智云
 */
@Getter
@AllArgsConstructor
public enum MesProApsPlanStatusEnum implements ArrayValuable<Integer> {

    /**
     * 草稿
     */
    DRAFT(0, "草稿"),
    /**
     * 已确认
     */
    CONFIRMED(1, "已确认"),
    /**
     * 执行中
     */
    EXECUTING(2, "执行中"),
    /**
     * 已完成
     */
    COMPLETED(3, "已完成"),
    /**
     * 已取消
     */
    CANCELLED(4, "已取消");

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(MesProApsPlanStatusEnum::getStatus).toArray(Integer[]::new);

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
