package cn.zhicloud.module.aimultiagent.service.workflow;

import cn.zhicloud.module.ai.service.workflow.MultiAgentExecutorGateway;
import cn.zhicloud.module.aimultiagent.dal.dataobject.MultiAgentExecutionLogDO;
import cn.zhicloud.module.aimultiagent.service.execute.MultiAgentExecuteService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * {@link MultiAgentExecutorGateway} 实现
 *
 * <p>委托给 {@link MultiAgentExecuteService#execute}，实现 zhicloud-module-ai 模块
 * 对 zhicloud-module-ai-multiagent 模块能力的反向调用（SPI 模式）。
 *
 * <p>C8 修复配套实现：当 AiWorkflowServiceImpl 的 multiAgentNode 节点触发时，
 * 通过此实现类调用多 Agent 编排引擎。
 */
@Component
@Slf4j
public class MultiAgentExecutorGatewayImpl implements MultiAgentExecutorGateway {

    @Resource
    private MultiAgentExecuteService multiAgentExecuteService;

    @Override
    public String execute(Long topologyId, String task, Long tenantId) {
        log.info("[execute][topologyId={} task={} tenantId={}]", topologyId, task, tenantId);
        MultiAgentExecutionLogDO logDO = multiAgentExecuteService.execute(topologyId, task, tenantId);
        return logDO != null ? logDO.getFinalAnswer() : null;
    }

}
