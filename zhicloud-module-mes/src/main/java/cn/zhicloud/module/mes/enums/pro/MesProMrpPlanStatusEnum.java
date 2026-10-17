package cn.zhicloud.module.mes.enums.pro;

import cn.zhicloud.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * MES MRP 计划状态枚举
 *
 * 状态转换图：
 * <pre>
 * DRAFT ──计算──▶ CALCULATED ──确认──▶ CONFIRMED ──转建议──▶ CONVERTED
 * </pre>
 *
 * @author 智云
 */
@Getter
@AllArgsConstructor
public enum MesProMrpPlanStatusEnum implements ArrayValuable<Integer> {

    /**
     * 草稿
     */
    DRAFT(0, "草稿"),
    /**
     * 已计算
     */
    CALCULATED(1, "已计算"),
    /**
     * 已确认
     */
    CONFIRMED(2, "已确认"),
    /**
     * 已转建议（终态：已落地为采购/生产工单）
     *
     * 对应 MesProMrpService#convertMrpPlan 方法
     */
    CONVERTED(3, "已转建议");

    public static final Integer[] ARRAYS = Arrays.stream(values()).map(MesProMrpPlanStatusEnum::getStatus).toArray(Integer[]::new);

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
