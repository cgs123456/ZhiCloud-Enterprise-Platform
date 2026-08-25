package cn.zhicloud.module.qms.service.audit;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.qms.controller.admin.audit.vo.QmsAuditPlanAuditorSaveReqVO;
import cn.zhicloud.module.qms.controller.admin.audit.vo.QmsAuditPlanPageReqVO;
import cn.zhicloud.module.qms.controller.admin.audit.vo.QmsAuditPlanSaveReqVO;
import cn.zhicloud.module.qms.dal.dataobject.audit.QmsAuditPlanAuditorDO;
import cn.zhicloud.module.qms.dal.dataobject.audit.QmsAuditPlanDO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * QMS 审核计划 Service 接口
 *
 * @author 智云
 */
public interface QmsAuditPlanService {

    /**
     * 创建审核计划
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createAuditPlan(@Valid QmsAuditPlanSaveReqVO createReqVO);

    /**
     * 更新审核计划
     *
     * @param updateReqVO 更新信息
     */
    void updateAuditPlan(@Valid QmsAuditPlanSaveReqVO updateReqVO);

    /**
     * 删除审核计划
     *
     * @param id 编号
     */
    void deleteAuditPlan(Long id);

    /**
     * 获得审核计划
     *
     * @param id 编号
     * @return 审核计划
     */
    QmsAuditPlanDO getAuditPlan(Long id);

    /**
     * 获得审核计划分页
     *
     * @param pageReqVO 分页查询
     * @return 审核计划分页
     */
    PageResult<QmsAuditPlanDO> getAuditPlanPage(QmsAuditPlanPageReqVO pageReqVO);

    /**
     * 开始执行，状态 10 已计划 -> 20 已执行
     *
     * @param id 编号
     */
    void executeAuditPlan(Long id);

    /**
     * 完成执行，状态 20 已执行 -> 30 已完成
     *
     * @param id 编号
     */
    void completeAuditPlan(Long id);

    /**
     * 取消，状态 -> 40 已取消
     *
     * @param id 编号
     */
    void cancelAuditPlan(Long id);

    /**
     * 添加审核组成员
     *
     * @param reqVO 成员信息
     * @return 编号
     */
    Long addAuditor(@Valid QmsAuditPlanAuditorSaveReqVO reqVO);

    /**
     * 更新审核组成员
     *
     * @param reqVO 成员信息
     */
    void updateAuditor(@Valid QmsAuditPlanAuditorSaveReqVO reqVO);

    /**
     * 删除审核组成员
     *
     * @param id 成员编号
     */
    void deleteAuditor(Long id);

    /**
     * 获得审核组成员列表
     *
     * @param planId 审核计划 ID
     * @return 成员列表
     */
    List<QmsAuditPlanAuditorDO> getAuditorListByPlanId(Long planId);

}
