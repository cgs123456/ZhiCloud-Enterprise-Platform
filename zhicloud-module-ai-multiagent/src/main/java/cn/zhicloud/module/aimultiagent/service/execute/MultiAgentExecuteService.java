package cn.zhicloud.module.aimultiagent.service.execute;

import cn.zhicloud.module.aimultiagent.dal.dataobject.MultiAgentCheckpointDO;
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
     * 从检查点恢复中断的编排执行
     *
     * <p>恢复语义：
     * <ul>
     *   <li>仅有 PLAN_COMPLETED 检查点 → 从第 0 个任务开始执行</li>
     *   <li>最新为 WORKER_DONE → 复用已完成结果，从下一任务继续</li>
     *   <li>已是 SUMMARIZE_DONE / 日志已成功 → 直接返回，不再执行</li>
     *   <li>无检查点 → 抛错（无可恢复内容）</li>
     * </ul>
     *
     * @param executionLogId 执行日志编号
     * @param tenantId       租户 ID
     * @return 恢复后的执行日志
     */
    MultiAgentExecutionLogDO resume(Long executionLogId, Long tenantId);

    /**
     * 查询某次执行的检查点列表（按写入顺序）
     *
     * @param executionLogId 执行日志编号
     * @return 检查点列表
     */
    List<MultiAgentCheckpointDO> getCheckpoints(Long executionLogId);

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
