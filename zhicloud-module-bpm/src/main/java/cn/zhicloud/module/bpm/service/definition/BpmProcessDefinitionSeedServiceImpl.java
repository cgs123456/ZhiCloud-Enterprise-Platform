package cn.zhicloud.module.bpm.service.definition;

import cn.hutool.core.util.StrUtil;
import cn.zhicloud.framework.tenant.core.util.TenantUtils;
import cn.zhicloud.module.bpm.controller.admin.definition.vo.model.BpmModelSaveReqVO;
import cn.zhicloud.module.bpm.controller.admin.definition.vo.model.simple.BpmSimpleModelNodeVO;
import cn.zhicloud.module.bpm.enums.definition.BpmModelFormTypeEnum;
import cn.zhicloud.module.bpm.enums.definition.BpmModelTypeEnum;
import cn.zhicloud.module.bpm.enums.definition.BpmSimpleModelNodeTypeEnum;
import cn.zhicloud.module.bpm.enums.definition.BpmUserTaskApproveMethodEnum;
import cn.zhicloud.module.bpm.enums.definition.BpmUserTaskApproveTypeEnum;
import cn.zhicloud.module.bpm.framework.flowable.core.enums.BpmTaskCandidateStrategyEnum;
import cn.zhicloud.module.bpm.framework.flowable.core.util.FlowableUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.ProcessDefinition;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.bpm.enums.ErrorCodeConstants.PROCESS_DEFINITION_NOT_EXISTS;

/**
 * 流程定义播种（Seed）Service 实现
 * <p>
 * 复用 {@link BpmModelService#createModel(BpmModelSaveReqVO)} + {@link BpmModelService#deployModel(Long, String)} 链路，
 * 从而保证播种出来的流程定义和人工在管理后台配置的完全一致：带租户部署、写入 bpm_process_definition_info、
 * 挂起历史版本、以及通过部署前的模型校验（开始事件、节点名称、候选人策略配置）。
 *
 * @author 智云
 */
@Service
@Validated
@Slf4j
public class BpmProcessDefinitionSeedServiceImpl implements BpmProcessDefinitionSeedService {

    /**
     * 播种的模型以该用户作为「可管理用户」，仅用于通过部署时的模型管理权限校验
     */
    private static final Long SEED_MANAGER_USER_ID = 1L;

    /**
     * 审批节点编号、名称
     */
    private static final String APPROVE_NODE_ID = "Activity_seed_approve";
    private static final String APPROVE_NODE_NAME = "审批";
    /**
     * 结束节点编号。开始事件由 {@link cn.zhicloud.module.bpm.framework.flowable.core.util.SimpleModelUtils} 自动构建
     */
    private static final String END_NODE_ID = "Event_seed_end";

    /**
     * 业务表单（CUSTOM）的默认路由。播种出的流程由业务模块自己发起，这两个路径仅用于满足表单已配置的校验
     */
    private static final String DEFAULT_FORM_CREATE_PATH = "/bpm/process-instance/create";
    private static final String DEFAULT_FORM_VIEW_PATH = "/bpm/process-instance/detail";

    @Resource
    private RepositoryService repositoryService;

    @Resource
    private BpmModelService modelService;

    @Override
    public String seed(String processDefinitionKey, String name, Long tenantId) {
        // 「发起人自己」不需要角色、部门、用户等外部数据即可解析出审批人，最适合最小化播种
        return seed(processDefinitionKey, name, tenantId,
                BpmTaskCandidateStrategyEnum.START_USER.getStrategy(), null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class) // 让「创建模型 + 部署」原子化，避免失败后残留同 KEY 的草稿模型
    public String seed(String processDefinitionKey, String name, Long tenantId,
                       Integer candidateStrategy, String candidateParam) {
        AtomicReference<String> definitionId = new AtomicReference<>();
        // 流程定义按租户隔离，部署时的租户来自 TenantContextHolder，所以必须在指定租户的上下文中执行
        TenantUtils.execute(tenantId, () -> {
            // 1. 幂等：同租户同 KEY 已有激活定义则什么都不做。注意不能插入第二份激活定义，
            //    否则发起流程时的 singleResult() 查询会直接抛 FlowableException
            ProcessDefinition activeDefinition = getActiveProcessDefinition(processDefinitionKey);
            if (activeDefinition != null) {
                log.info("[seed][租户({}) 的流程定义({}) 已存在激活版本({})，跳过播种]",
                        tenantId, processDefinitionKey, activeDefinition.getId());
                definitionId.set(activeDefinition.getId());
                return;
            }

            // 2. 创建模型并部署，得到带租户的流程定义
            String modelId = modelService.createModel(
                    buildModelSaveReqVO(processDefinitionKey, name, candidateStrategy, candidateParam));
            modelService.deployModel(SEED_MANAGER_USER_ID, modelId);

            // 3. 回读播种结果
            ProcessDefinition definition = getActiveProcessDefinition(processDefinitionKey);
            if (definition == null) {
                throw exception(PROCESS_DEFINITION_NOT_EXISTS);
            }
            log.info("[seed][租户({}) 的流程定义({}) 播种成功，流程定义编号({})]",
                    tenantId, processDefinitionKey, definition.getId());
            definitionId.set(definition.getId());
        });
        return definitionId.get();
    }

    /**
     * 查询当前租户下指定 KEY 的激活流程定义
     * <p>
     * 有意不使用 {@link BpmProcessDefinitionService#getActiveProcessDefinition(String)}：它基于 singleResult()，
     * 一旦历史脏数据存在多份激活定义就会抛异常，而播种需要在这种场景下继续幂等跳过。
     *
     * @param processDefinitionKey 流程定义标识
     * @return 版本最新的激活流程定义，不存在时返回 null
     */
    private ProcessDefinition getActiveProcessDefinition(String processDefinitionKey) {
        return repositoryService.createProcessDefinitionQuery()
                .processDefinitionTenantId(FlowableUtils.getTenantId())
                .processDefinitionKey(processDefinitionKey).active()
                .orderByProcessDefinitionVersion().desc()
                .list().stream().findFirst().orElse(null);
    }

    private BpmModelSaveReqVO buildModelSaveReqVO(String processDefinitionKey, String name,
                                                  Integer candidateStrategy, String candidateParam) {
        BpmModelSaveReqVO reqVO = new BpmModelSaveReqVO();
        reqVO.setKey(processDefinitionKey);
        reqVO.setName(StrUtil.blankToDefault(name, processDefinitionKey));
        reqVO.setDescription("系统播种的最小流程：单个审批节点，通过后流程结束");
        reqVO.setType(BpmModelTypeEnum.SIMPLE.getType());
        // 业务表单：审批数据由业务模块自己存储（例如报销单、请假单），因此无需配置 bpm_form
        reqVO.setFormType(BpmModelFormTypeEnum.CUSTOM.getType());
        reqVO.setFormCustomCreatePath(DEFAULT_FORM_CREATE_PATH);
        reqVO.setFormCustomViewPath(DEFAULT_FORM_VIEW_PATH);
        reqVO.setVisible(true);
        reqVO.setAllowCancelRunningProcess(true);
        reqVO.setAllowWithdrawTask(false);
        reqVO.setManagerUserIds(Collections.singletonList(SEED_MANAGER_USER_ID));
        reqVO.setSimpleModel(buildSimpleModel(candidateStrategy, candidateParam));
        return reqVO;
    }

    /**
     * 构建 SIMPLE 模型：审批节点 -> 结束节点
     *
     * @return 模型的首个节点，用于 {@link cn.zhicloud.module.bpm.framework.flowable.core.util.SimpleModelUtils#buildBpmnModel}
     */
    private BpmSimpleModelNodeVO buildSimpleModel(Integer candidateStrategy, String candidateParam) {
        BpmSimpleModelNodeVO approveNode = new BpmSimpleModelNodeVO()
                .setId(APPROVE_NODE_ID).setName(APPROVE_NODE_NAME)
                .setType(BpmSimpleModelNodeTypeEnum.APPROVE_NODE.getType())
                .setApproveType(BpmUserTaskApproveTypeEnum.USER.getType())
                // 单候选人：RANDOM 审批方式不会生成多实例，任务直接分配给一个人
                .setApproveMethod(BpmUserTaskApproveMethodEnum.RANDOM.getMethod())
                .setCandidateStrategy(candidateStrategy).setCandidateParam(candidateParam);
        BpmSimpleModelNodeVO endNode = new BpmSimpleModelNodeVO()
                .setId(END_NODE_ID).setName(BpmSimpleModelNodeTypeEnum.END_NODE.getName())
                .setType(BpmSimpleModelNodeTypeEnum.END_NODE.getType());
        return approveNode.setChildNode(endNode);
    }

}
