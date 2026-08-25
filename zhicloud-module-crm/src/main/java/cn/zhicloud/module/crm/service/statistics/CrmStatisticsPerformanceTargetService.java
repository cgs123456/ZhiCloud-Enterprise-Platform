package cn.zhicloud.module.crm.service.statistics;

import cn.zhicloud.module.crm.controller.admin.statistics.vo.performance.CrmStatisticsPerformanceTargetReqVO;
import cn.zhicloud.module.crm.controller.admin.statistics.vo.performance.CrmStatisticsPerformanceTargetRespVO;

import java.util.List;

/**
 * CRM 业绩目标统计 Service 接口
 *
 * @author 智云
 */
public interface CrmStatisticsPerformanceTargetService {

    /**
     * 获取业绩目标完成情况
     *
     * @param reqVO 请求参数
     * @return 业绩目标完成情况
     */
    List<CrmStatisticsPerformanceTargetRespVO> getPerformanceTargetSummary(CrmStatisticsPerformanceTargetReqVO reqVO);

}
