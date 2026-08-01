package cn.iocoder.yudao.module.mes.service.pro.aps;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.pro.aps.vo.MesProApsGenerateReqVO;
import cn.iocoder.yudao.module.mes.controller.admin.pro.aps.vo.MesProApsPlanPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.pro.aps.MesProApsPlanDO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * MES APS 高级排产 Service 接口
 *
 * @author 芋道源码
 */
public interface MesProApsService {

    /**
     * 有限产能排产（贪婪算法）
     *
     * 读取工单交期、BOM 工艺路线、工位可用日历，按优先级排序，贪婪分配工位时段
     *
     * @param reqVO 排产请求
     * @return 排产计划列表
     */
    List<MesProApsPlanDO> generateSchedule(MesProApsGenerateReqVO reqVO);

    /**
     * 获取工位负荷
     *
     * @param workstationId 工位编号
     * @param startDate 开始时间
     * @param endDate 结束时间
     * @return 排产计划列表
     */
    List<MesProApsPlanDO> getWorkstationLoad(Long workstationId, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * 重排产
     *
     * @param planId 排产计划编号
     * @param newStartTime 新的开始时间
     */
    void reschedule(Long planId, LocalDateTime newStartTime);

    /**
     * 获得排产计划
     *
     * @param id 编号
     * @return 排产计划
     */
    MesProApsPlanDO getApsPlan(Long id);

    /**
     * 获得排产计划分页
     *
     * @param pageReqVO 分页查询
     * @return 排产计划分页
     */
    PageResult<MesProApsPlanDO> getApsPlanPage(MesProApsPlanPageReqVO pageReqVO);

}
