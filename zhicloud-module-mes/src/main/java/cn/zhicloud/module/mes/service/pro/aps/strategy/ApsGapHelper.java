package cn.zhicloud.module.mes.service.pro.aps.strategy;

import cn.zhicloud.module.mes.dal.dataobject.md.workstation.MesMdWorkstationDO;
import cn.zhicloud.module.mes.dal.dataobject.pro.aps.MesProApsPlanDO;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * APS 工位空闲空隙构建工具（§12.263 空隙建模）
 *
 * 空隙 = 排产窗口 [startDate, endDate) 扣除既有计划占用后的空闲区间：
 * 占用先裁剪至窗口（左跨/右跨计划只封锁被裁进窗口的部分），再按起点升序合并
 * （重叠与相邻均合并），余段即空隙。
 *
 * 与旧标量模型（工位下次可用时间 = 最晚结束时间）的差异：
 * 中段占用不再把早段空闲一并封锁；跨窗占用不再把整窗封锁（只封锁实际重叠部分）。
 *
 * @author 智云
 */
public final class ApsGapHelper {

    private ApsGapHelper() {
    }

    /**
     * 构建各工位在窗口内的空闲空隙
     *
     * @param workstations  可用工位列表（每个工位都会得到条目，无空隙则为空列表）
     * @param existingPlans 与窗口重叠的既有计划（占用来源；空值安全）
     * @param startDate     排产开始时间（含）
     * @param endDate       排产结束时间（不含）
     * @return 工位编号 -> 空闲空隙列表（升序、两两不相交且不相邻）
     */
    public static Map<Long, List<ApsTimeRange>> buildFreeGaps(List<MesMdWorkstationDO> workstations,
                                                              List<MesProApsPlanDO> existingPlans,
                                                              LocalDateTime startDate, LocalDateTime endDate) {
        // 1. 占用按工位分组：裁剪至窗口，空段（窗口外/零长）丢弃
        Map<Long, List<ApsTimeRange>> busyByWorkstation = new HashMap<>();
        if (existingPlans != null) {
            for (MesProApsPlanDO plan : existingPlans) {
                if (plan.getWorkstationId() == null || plan.getPlannedStartTime() == null
                        || plan.getPlannedEndTime() == null) {
                    continue;
                }
                LocalDateTime busyStart = plan.getPlannedStartTime().isBefore(startDate)
                        ? startDate : plan.getPlannedStartTime();
                LocalDateTime busyEnd = plan.getPlannedEndTime().isAfter(endDate)
                        ? endDate : plan.getPlannedEndTime();
                if (!busyEnd.isAfter(busyStart)) {
                    continue;
                }
                busyByWorkstation.computeIfAbsent(plan.getWorkstationId(), k -> new ArrayList<>())
                        .add(new ApsTimeRange(busyStart, busyEnd));
            }
        }
        // 2. 逐工位求窗口补集（游标扫描，重叠与相邻自然合并）
        Map<Long, List<ApsTimeRange>> freeGaps = new HashMap<>();
        for (MesMdWorkstationDO workstation : workstations) {
            List<ApsTimeRange> busy = busyByWorkstation.getOrDefault(workstation.getId(), new ArrayList<>());
            busy.sort(Comparator.comparing(ApsTimeRange::getStart));
            List<ApsTimeRange> gaps = new ArrayList<>();
            LocalDateTime cursor = startDate;
            for (ApsTimeRange range : busy) {
                // 游标之后的空闲段即空隙（起点不晚于游标时为空段，天然完成重叠/相邻合并）
                if (range.getStart().isAfter(cursor)) {
                    gaps.add(new ApsTimeRange(cursor, range.getStart()));
                }
                if (range.getEnd().isAfter(cursor)) {
                    cursor = range.getEnd();
                }
            }
            if (cursor.isBefore(endDate)) {
                gaps.add(new ApsTimeRange(cursor, endDate));
            }
            freeGaps.put(workstation.getId(), gaps);
        }
        return freeGaps;
    }

}
