package cn.zhicloud.module.ai.service.workflow;

import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.module.ai.controller.admin.workflow.vo.AiWorkflowNodeTypeRespVO;
import cn.zhicloud.module.ai.controller.admin.workflow.vo.AiWorkflowPageReqVO;
import cn.zhicloud.module.ai.controller.admin.workflow.vo.AiWorkflowSaveReqVO;
import cn.zhicloud.module.ai.controller.admin.workflow.vo.AiWorkflowTemplateRespVO;
import cn.zhicloud.module.ai.controller.admin.workflow.vo.AiWorkflowTestReqVO;
import cn.zhicloud.module.ai.controller.admin.workflow.vo.AiWorkflowValidateReqVO;
import cn.zhicloud.module.ai.controller.admin.workflow.vo.AiWorkflowValidateRespVO;
import cn.zhicloud.module.ai.dal.dataobject.workflow.AiWorkflowDO;
import jakarta.validation.Valid;

import java.util.List;

/**
 * AI 工作流 Service 接口
 *
 * @author lesan
 */
public interface AiWorkflowService {

    /**
     * 创建 AI 工作流
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createWorkflow(@Valid AiWorkflowSaveReqVO createReqVO);

    /**
     * 更新 AI 工作流
     *
     * @param updateReqVO 更新信息
     */
    void updateWorkflow(@Valid AiWorkflowSaveReqVO updateReqVO);

    /**
     * 删除 AI 工作流
     *
     * @param id 编号
     */
    void deleteWorkflow(Long id);

    /**
     * 获得 AI 工作流
     *
     * @param id 编号
     * @return AI 工作流
     */
    AiWorkflowDO getWorkflow(Long id);

    /**
     * 获得 AI 工作流分页
     *
     * @param pageReqVO 分页查询
     * @return AI 工作流分页
     */
    PageResult<AiWorkflowDO> getWorkflowPage(AiWorkflowPageReqVO pageReqVO);

    /**
     * 测试 AI 工作流
     *
     * @param testReqVO 测试数据
     */
    Object testWorkflow(AiWorkflowTestReqVO testReqVO);

    /**
     * 获取所有可用节点类型列表（前端可视化编排用）
     *
     * @return 节点类型列表
     */
    List<AiWorkflowNodeTypeRespVO> getNodeTypeList();

    /**
     * 获取节点配置 Schema（前端表单渲染用）
     *
     * @param nodeType 节点类型标识
     * @return 节点类型信息（含配置 Schema）
     */
    AiWorkflowNodeTypeRespVO getNodeConfigSchema(String nodeType);

    /**
     * 校验工作流 JSON 配置合法性
     *
     * @param reqVO 校验数据
     * @return 校验结果
     */
    AiWorkflowValidateRespVO validateWorkflow(AiWorkflowValidateReqVO reqVO);

    /**
     * 获取工作流模板列表
     *
     * @return 模板列表
     */
    List<AiWorkflowTemplateRespVO> getWorkflowTemplateList();

}
