package cn.zhicloud.module.mes.service.dv.tp;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.mes.controller.admin.dv.tp.vo.MesDvTpRecordPageReqVO;
import cn.zhicloud.module.mes.dal.dataobject.dv.tp.MesDvTpRecordDO;

import java.util.List;

/**
 * MES TPM 执行记录 Service 接口
 *
 * @author 智云
 */
public interface MesDvTpRecordService {

    /**
     * 获得 TPM 执行记录
     *
     * @param id 编号
     * @return 执行记录
     */
    MesDvTpRecordDO getTpRecord(Long id);

    /**
     * 获得 TPM 执行记录分页
     *
     * @param pageReqVO 分页查询
     * @return 执行记录分页
     */
    PageResult<MesDvTpRecordDO> getTpRecordPage(MesDvTpRecordPageReqVO pageReqVO);

    /**
     * 获得 TPM 执行记录列表（按计划编号）
     *
     * @param planId 计划编号
     * @return 记录列表
     */
    List<MesDvTpRecordDO> getTpRecordListByPlanId(Long planId);

    /**
     * 获得设备指定周期的执行记录
     *
     * @param equipmentId 设备编号
     * @param periodStart 周期开始（yyyy-MM-dd）
     * @param periodEnd 周期结束（yyyy-MM-dd）
     * @return 记录列表
     */
    List<MesDvTpRecordDO> getTpRecordListByEquipmentAndPeriod(Long equipmentId, String periodStart, String periodEnd);

}