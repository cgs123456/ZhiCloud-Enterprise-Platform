package cn.zhicloud.module.mes.service.energy;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.mes.controller.admin.energy.vo.MesEnergyConsumptionPageReqVO;
import cn.zhicloud.module.mes.controller.admin.energy.vo.MesEnergyConsumptionSaveReqVO;
import cn.zhicloud.module.mes.dal.dataobject.energy.MesEnergyConsumptionDO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * MES 能源消耗 Service 接口
 *
 * @author 智云
 */
public interface MesEnergyConsumptionService {

    /**
     * 创建能源消耗记录
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createEnergyConsumption(MesEnergyConsumptionSaveReqVO createReqVO);

    /**
     * 更新能源消耗记录
     *
     * @param updateReqVO 更新信息
     */
    void updateEnergyConsumption(MesEnergyConsumptionSaveReqVO updateReqVO);

    /**
     * 删除能源消耗记录
     *
     * @param id 编号
     */
    void deleteEnergyConsumption(Long id);

    /**
     * 获取能源消耗记录
     *
     * @param id 编号
     * @return 能源消耗记录
     */
    MesEnergyConsumptionDO getEnergyConsumption(Long id);

    /**
     * 获取能源消耗记录分页
     *
     * @param pageReqVO 分页查询
     * @return 能源消耗记录分页
     */
    PageResult<MesEnergyConsumptionDO> getEnergyConsumptionPage(MesEnergyConsumptionPageReqVO pageReqVO);

    /**
     * 获取车间指定日期范围的能源消耗统计
     *
     * @param workshopId 车间编号
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 按能源类型分组的消耗汇总，key=能源类型，value=消耗总量
     */
    Map<Integer, BigDecimal> getEnergySummaryByWorkshop(Long workshopId, LocalDate startDate, LocalDate endDate);

    /**
     * 获取车间指定日期范围的能源消耗明细
     *
     * @param workshopId 车间编号
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 能源消耗明细列表
     */
    List<MesEnergyConsumptionDO> getEnergyConsumptionList(Long workshopId, LocalDate startDate, LocalDate endDate);

}
