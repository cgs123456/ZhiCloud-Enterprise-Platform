package cn.zhicloud.module.mes.service.dv.tp;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.mes.controller.admin.dv.tp.vo.MesDvTpExecuteReqVO;
import cn.zhicloud.module.mes.controller.admin.dv.tp.vo.MesDvTpPlanPageReqVO;
import cn.zhicloud.module.mes.controller.admin.dv.tp.vo.MesDvTpPlanSaveReqVO;
import cn.zhicloud.module.mes.dal.dataobject.dv.tp.MesDvTpPlanDO;
import cn.zhicloud.module.mes.dal.dataobject.dv.tp.MesDvTpPlanItemDO;
import cn.zhicloud.module.mes.dal.dataobject.dv.tp.MesDvTpRecordDO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * MES TPM 计划 Service 接口
 *
 * @author 智云
 */
public interface MesDvTpPlanService {

    /**
     * 创建 TPM 计划
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createTpPlan(@Valid MesDvTpPlanSaveReqVO createReqVO);

    /**
     * 更新 TPM 计划
     *
     * @param updateReqVO 更新信息
     */
    void updateTpPlan(@Valid MesDvTpPlanSaveReqVO updateReqVO);

    /**
     * 删除 TPM 计划
     *
     * @param ids 编号数组
     */
    void deleteTpPlan(List<Long> ids);

    /**
     * 获得 TPM 计划
     *
     * @param id 编号
     * @return TPM 计划
     */
    MesDvTpPlanDO getTpPlan(Long id);

    /**
     * 获得 TPM 计划分页
     *
     * @param pageReqVO 分页查询
     * @return TPM 计划分页
     */
    PageResult<MesDvTpPlanDO> getTpPlanPage(MesDvTpPlanPageReqVO pageReqVO);

    /**
     * 启用 TPM 计划
     *
     * @param id 编号
     */
    void enablePlan(Long id);

    /**
     * 禁用 TPM 计划
     *
     * @param id 编号
     */
    void disablePlan(Long id);

    /**
     * 执行 TPM 计划，生成执行记录
     *
     * @param reqVO 执行信息
     * @param executorId 执行人编号
     * @return 执行记录编号
     */
    Long executePlan(@Valid MesDvTpExecuteReqVO reqVO, Long executorId);

    /**
     * 查询逾期未执行的计划（next_execute_date < today）
     *
     * @return 逾期计划列表
     */
    List<MesDvTpPlanDO> getOverduePlans();

    /**
     * 校验 TPM 计划存在
     *
     * @param id 编号
     * @return TPM 计划
     */
    MesDvTpPlanDO validateTpPlan(Long id);

    /**
     * 获得 TPM 计划项目列表
     *
     * @param planId 计划编号
     * @return 项目列表
     */
    List<MesDvTpPlanItemDO> getTpPlanItemListByPlanId(Long planId);

    /**
     * 获得 TPM 执行记录列表
     *
     * @param planId 计划编号
     * @return 记录列表
     */
    List<MesDvTpRecordDO> getTpRecordListByPlanId(Long planId);

}