package cn.zhicloud.module.mes.service.dv.tp;

import cn.zhicloud.module.mes.controller.admin.dv.tp.vo.MesDvTpPlanItemSaveReqVO;
import cn.zhicloud.module.mes.dal.dataobject.dv.tp.MesDvTpPlanItemDO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * MES TPM 计划项目 Service 接口
 *
 * @author 智云
 */
public interface MesDvTpPlanItemService {

    /**
     * 添加 TPM 计划项目
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long addPlanItem(@Valid MesDvTpPlanItemSaveReqVO createReqVO);

    /**
     * 更新 TPM 计划项目
     *
     * @param updateReqVO 更新信息
     */
    void updatePlanItem(@Valid MesDvTpPlanItemSaveReqVO updateReqVO);

    /**
     * 删除 TPM 计划项目
     *
     * @param id 编号
     */
    void deletePlanItem(Long id);

    /**
     * 获得 TPM 计划项目
     *
     * @param id 编号
     * @return 计划项目
     */
    MesDvTpPlanItemDO getTpPlanItem(Long id);

    /**
     * 获得 TPM 计划项目列表
     *
     * @param planId 计划编号
     * @return 项目列表
     */
    List<MesDvTpPlanItemDO> getTpPlanItemListByPlanId(Long planId);

}