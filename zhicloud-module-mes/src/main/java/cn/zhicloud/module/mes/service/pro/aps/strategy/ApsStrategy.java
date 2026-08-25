package cn.zhicloud.module.mes.service.pro.aps.strategy;

import cn.zhicloud.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.zhicloud.module.mes.enums.pro.ApsAlgorithmEnum;

import java.util.List;

/**
 * APS 排产策略接口
 *
 * 策略模式：将不同排产算法（贪婪 / TOC-DBR / 遗传）抽象为统一接口，
 * 由 {@link cn.zhicloud.module.mes.service.pro.aps.MesProApsService} 按配置选择具体实现。
 *
 * @author 智云
 */
public interface ApsStrategy {

    /**
     * 获取算法标识
     */
    ApsAlgorithmEnum getAlgorithm();

    /**
     * 执行排产
     *
     * @param orders   已排序的待排产工单列表（策略可按需重排）
     * @param context  排产上下文
     * @return 排产结果（计划列表尚未持久化）
     */
    ApsResult schedule(List<MesProWorkOrderDO> orders, ApsContext context);

}