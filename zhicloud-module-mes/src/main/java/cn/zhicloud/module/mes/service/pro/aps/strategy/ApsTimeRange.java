package cn.zhicloud.module.mes.service.pro.aps.strategy;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * APS 时间区间（半开区间 [start, end)，不可变）
 *
 * 用于表示窗口内的工位空闲空隙（§12.263 空隙建模）：排产计划只落于验证过为空的区间内。
 *
 * @author 智云
 */
@Getter
@AllArgsConstructor
public class ApsTimeRange {

    /**
     * 区间开始（含）
     */
    private final LocalDateTime start;
    /**
     * 区间结束（不含）
     */
    private final LocalDateTime end;

    /**
     * 判定 [effectiveStart, effectiveStart + durationHours) 是否完整落入本区间
     *
     * 半开区间语义：紧贴边界（计划结束 == 区间结束）可容纳。
     *
     * @param effectiveStart 计划开始时间（不小于区间起点）
     * @param durationHours  计划时长（小时）
     * @return 能容纳返回 true
     */
    public boolean canFit(LocalDateTime effectiveStart, long durationHours) {
        return !effectiveStart.isBefore(start) && !effectiveStart.plusHours(durationHours).isAfter(end);
    }

    @Override
    public String toString() {
        return "[" + start + ", " + end + ")";
    }

}
