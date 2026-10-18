package cn.zhicloud.module.mes.service.pro.aps.strategy;

import cn.zhicloud.module.mes.dal.dataobject.md.workstation.MesMdWorkstationDO;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * APS 排产上下文
 *
 * 封装排产所需的输入数据，由 {@link cn.zhicloud.module.mes.service.pro.aps.MesProApsService} 构建后传入策略。
 *
 * 可用性模型（§12.263 空隙建模）：每工位以「窗口内空闲空隙」表示，替代旧标量「下次可用时间」，
 * 使中段/跨窗占用不再连带封锁窗口内的真实空闲段。
 *
 * @author 智云
 */
@Data
@Builder
public class ApsContext {

    /**
     * 排产开始时间
     */
    private LocalDateTime startDate;
    /**
     * 排产结束时间
     */
    private LocalDateTime endDate;
    /**
     * 可用工位列表（启用状态）
     */
    private List<MesMdWorkstationDO> workstations;
    /**
     * 工位空闲空隙（工位编号 -> 窗口 [startDate, endDate) 内空闲区间列表，升序、两两不相交且不相邻；
     * 策略内部会基于副本计算，不修改此 Map）
     */
    private Map<Long, List<ApsTimeRange>> workstationFreeGaps;
    /**
     * 工单估算时长（小时）：工单编号 -> 时长
     */
    private Map<Long, Long> workOrderDurationHours;

}