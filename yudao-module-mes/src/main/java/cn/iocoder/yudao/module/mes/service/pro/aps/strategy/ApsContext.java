package cn.iocoder.yudao.module.mes.service.pro.aps.strategy;

import cn.iocoder.yudao.module.mes.dal.dataobject.md.workstation.MesMdWorkstationDO;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * APS 排产上下文
 *
 * 封装排产所需的输入数据，由 {@link cn.iocoder.yudao.module.mes.service.pro.aps.MesProApsService} 构建后传入策略。
 *
 * @author 芋道源码
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
     * 工位初始可用时间（工位编号 -> 下次可用时间，策略内部会基于副本计算，不修改此 Map）
     */
    private Map<Long, LocalDateTime> workstationAvailableTime;
    /**
     * 工单估算时长（小时）：工单编号 -> 时长
     */
    private Map<Long, Long> workOrderDurationHours;

}