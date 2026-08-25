package cn.zhicloud.module.aimultiagent.service.execute;

import cn.zhicloud.module.aimultiagent.dal.dataobject.MultiAgentExecutionLogDO;

import java.util.List;

/**
 * 多 Agent 编排执行 Service 接口
 *
 * @author zhicloud
 */
public interface MultiAgentExecuteService {

    /**
     * 执行多 Agent 编排
     *
     * 核心流程：
     * 1. 加载拓扑配置
     * 2. Supervisor 任务拆解
     * 3. 调用深度熔断检查
     * 4. 分发任务给 Worker 执行
     * 5. Token 预算熔断检查
     * 6. Supervisor 结果汇总
     * 7. 记录执行日志
     *
     * @param topologyId 拓扑 ID
     * @param userInput  用户输入
     * @param tenantId   租户 ID
     * @return 执行日志（包含最终答案、Token 消耗、执行状态等）
     */
    MultiAgentExecutionLogDO execute(Long topologyId, String userInput, Long tenantId);

    /**
     * 查询执行日志
     *
     * @param logId 日志 ID
     * @return 执行日志
     */
    MultiAgentExecutionLogDO getExecutionLog(Long logId);

    /**
     * 按拓扑 ID 查询执行日志列表
     *
     * @param topologyId 拓扑 ID
     * @return 执行日志列表
     */
    List<MultiAgentExecutionLogDO> getExecutionLogListByTopologyId(Long topologyId);

}
