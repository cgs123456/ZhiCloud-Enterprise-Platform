package cn.zhicloud.module.oa.service.approvaltemplate;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.oa.controller.admin.approvaltemplate.vo.OaApprovalTemplatePageReqVO;
import cn.zhicloud.module.oa.controller.admin.approvaltemplate.vo.OaApprovalTemplateSaveReqVO;
import cn.zhicloud.module.oa.dal.dataobject.approvaltemplate.OaApprovalTemplateDO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * OA 审批模板 Service 接口
 *
 * @author zhicloud
 */
public interface OaApprovalTemplateService {

    /**
     * 创建审批模板
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createApprovalTemplate(@Valid OaApprovalTemplateSaveReqVO createReqVO);

    /**
     * 更新审批模板
     *
     * @param updateReqVO 更新信息
     */
    void updateApprovalTemplate(@Valid OaApprovalTemplateSaveReqVO updateReqVO);

    /**
     * 删除审批模板
     *
     * @param id 编号
     */
    void deleteApprovalTemplate(Long id);

    /**
     * 获得审批模板
     *
     * @param id 编号
     * @return 审批模板
     */
    OaApprovalTemplateDO getApprovalTemplate(Long id);

    /**
     * 获得审批模板分页
     *
     * @param pageReqVO 分页查询
     * @return 审批模板分页
     */
    PageResult<OaApprovalTemplateDO> getApprovalTemplatePage(OaApprovalTemplatePageReqVO pageReqVO);

    /**
     * 获得启用的审批模板列表
     *
     * @return 审批模板列表
     */
    List<OaApprovalTemplateDO> getEnabledList();

    /**
     * 累加使用次数
     *
     * @param id 编号
     */
    void incrementUsageCount(Long id);

}
