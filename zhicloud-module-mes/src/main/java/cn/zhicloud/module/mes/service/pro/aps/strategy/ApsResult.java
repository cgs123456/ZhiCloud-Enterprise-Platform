package cn.zhicloud.module.mes.service.pro.aps.strategy;

import cn.zhicloud.module.mes.dal.dataobject.pro.aps.MesProApsPlanDO;
import lombok.Builder;
import lombok.Data;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * APS 排产结果
 *
 * @author 智云
 */
@Data
@Builder
public class ApsResult {

    /**
     * 排产计划列表（尚未持久化，由 Service 统一落库）
     */
    private List<MesProApsPlanDO> plans;
    /**
     * 因数量无效或产能不足被跳过的工单编号集合
     */
    private Set<Long> skippedWorkOrderIds;

    public Set<Long> getSkippedWorkOrderIds() {
        if (skippedWorkOrderIds == null) {
            skippedWorkOrderIds = new HashSet<>();
        }
        return skippedWorkOrderIds;
    }

}