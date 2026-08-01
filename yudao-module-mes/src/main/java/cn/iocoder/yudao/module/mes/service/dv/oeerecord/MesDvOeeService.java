package cn.iocoder.yudao.module.mes.service.dv.oeerecord;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.mes.controller.admin.dv.oeerecord.vo.MesDvOeeRecordPageReqVO;
import cn.iocoder.yudao.module.mes.dal.dataobject.dv.oeerecord.MesDvOeeRecordDO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * MES OEE 设备综合效率 Service 接口
 *
 * @author 芋道源码
 */
public interface MesDvOeeService {

    /**
     * 计算 OEE
     *
     * Availability = RunTime / PlannedProductionTime
     * Performance = (TotalProduced * IdealCycleTime) / RunTime
     * Quality = GoodProduced / TotalProduced
     * OEE = Availability * Performance * Quality
     *
     * @param machineryId 设备编号
     * @param startDate 开始时间
     * @param endDate 结束时间
     * @return OEE 计算结果（聚合区间内的记录）
     */
    MesDvOeeRecordDO calculateOee(Long machineryId, LocalDateTime startDate, LocalDateTime endDate);

    /**
     * 获取 OEE 趋势
     *
     * @param machineryId 设备编号
     * @param days 天数（最近 N 天）
     * @return OEE 趋势列表
     */
    List<MesDvOeeRecordDO> getOeeTrend(Long machineryId, Integer days);

    /**
     * 获得 OEE 记录分页
     *
     * @param pageReqVO 分页查询
     * @return OEE 记录分页
     */
    PageResult<MesDvOeeRecordDO> getOeeRecordPage(MesDvOeeRecordPageReqVO pageReqVO);

}
