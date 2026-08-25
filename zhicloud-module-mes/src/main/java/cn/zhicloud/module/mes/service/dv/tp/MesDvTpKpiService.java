package cn.zhicloud.module.mes.service.dv.tp;

import cn.zhicloud.module.mes.dal.dataobject.dv.tp.MesDvTpKpiDO;

import java.util.List;

/**
 * MES TPM KPI 指标 Service 接口
 *
 * @author 智云
 */
public interface MesDvTpKpiService {

    /**
     * 计算 KPI（MTBF/MTTR/OEE 改善值）
     *
     * MTBF = 总运行时间 / 故障次数
     * MTTR = 总维修时间 / 故障次数
     * OEE 改善值 = 当期 OEE - 上期 OEE
     *
     * @param equipmentId 设备编号
     * @param period 周期（yyyyMM）
     * @return KPI 指标
     */
    MesDvTpKpiDO calculateKpi(Long equipmentId, String period);

    /**
     * 获得 TPM KPI 指标
     *
     * @param id 编号
     * @return KPI 指标
     */
    MesDvTpKpiDO getTpKpi(Long id);

    /**
     * 查询设备 KPI 历史
     *
     * @param equipmentId 设备编号
     * @param periods 周期列表
     * @return KPI 历史列表
     */
    List<MesDvTpKpiDO> getEquipmentKpiHistory(Long equipmentId, List<String> periods);

}