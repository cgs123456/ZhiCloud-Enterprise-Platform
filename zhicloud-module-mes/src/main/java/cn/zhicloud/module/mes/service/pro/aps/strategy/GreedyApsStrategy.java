package cn.zhicloud.module.mes.service.pro.aps.strategy;

import cn.zhicloud.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.zhicloud.module.mes.enums.pro.ApsAlgorithmEnum;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 贪婪 APS 排产策略
 *
 * 为每张工单寻找最早空闲的工位时段（有限产能排产），是默认排产算法。
 * 核心逻辑提取自历史实现，复用 {@link ApsAllocationHelper}。
 *
 * @author 智云
 */
@Component
public class GreedyApsStrategy implements ApsStrategy {

    @Override
    public ApsAlgorithmEnum getAlgorithm() {
        return ApsAlgorithmEnum.GREEDY;
    }

    @Override
    public ApsResult schedule(List<MesProWorkOrderDO> orders, ApsContext context) {
        // 工单已按「优先级 + 需求日期」排序，直接贪婪分配
        return ApsAllocationHelper.allocate(orders, context);
    }

}