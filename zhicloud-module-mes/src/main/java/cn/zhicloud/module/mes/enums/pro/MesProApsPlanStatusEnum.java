package cn.zhicloud.module.mes.enums.pro;

import cn.zhicloud.framework.common.core.ArrayValuable;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
     * 终态值集合（已完成 / 已取消）：generate 可用时间线与重排产冲突检测只排除这两个状态，
     * 其余状态一律按「占用工位资源」处理——含枚举域之外的未知值（历史数据/人工运维写入）。
     * 方向保守：容量规划宁可低估空档，不能高估空档造成超排（旧「被包含」查询漏计左跨计划
     * 属对偶方向缺陷，§12.240.5 登记、§12.244 收口，本锚点收口 §12.250.5-2/§12.251）。
     *
     * <p>为什么用「排除终态」而不是「白名单占位状态」：将来新增状态时默认按占位处理，
     * 无需记得改这里；白名单写法下新增状态会静默漏计。本常量是这条领域规则的唯一锚点，
     * mapper 直接引用，禁止在 SQL 构造处重新内联状态集。</p>
     */
    public static final List<Integer> TERMINAL_STATUS_VALUES =
            Collections.unmodifiableList(Arrays.asList(COMPLETED.getStatus(), CANCELLED.getStatus()));

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
