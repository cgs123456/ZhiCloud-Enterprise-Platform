package cn.zhicloud.module.aimultiagent.service.execute;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.zhicloud.module.aimultiagent.config.ChatClientHelper;
import cn.zhicloud.module.aimultiagent.config.MultiAgentProperties;
import cn.zhicloud.module.aimultiagent.dal.dataobject.MultiAgentExecutionLogDO;
import cn.zhicloud.module.aimultiagent.dal.dataobject.MultiAgentTopologyDO;
import cn.zhicloud.module.aimultiagent.dal.mysql.MultiAgentExecutionLogMapper;
import cn.zhicloud.module.aimultiagent.dal.mysql.MultiAgentTopologyMapper;
import cn.zhicloud.module.aimultiagent.model.AgentResult;
import cn.zhicloud.module.aimultiagent.model.AgentTask;
import cn.zhicloud.module.aimultiagent.model.AgentTopology;
import cn.zhicloud.module.aimultiagent.service.agent.AbstractWorkerAgent;
import cn.zhicloud.module.aimultiagent.service.agent.SupervisorAgent;
import cn.zhicloud.module.aimultiagent.service.agent.WorkerAgentRegistry;
import cn.zhicloud.module.aimultiagent.service.metrics.MultiAgentMetrics;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.aimultiagent.enums.ErrorCodeConstants.*;

/**
 * 多 Agent 编排执行引擎 Service 实现类
 *
 * 核心流程：
 * 1. 加载拓扑配置，解析 Worker 配置 JSON
 * 2. 检查 LLM 可用性
 * 3. Supervisor 任务拆解（planTasks）
 * 4. 调用深度熔断检查（任务数 > maxDepth 时熔断）
 * 5. 分发任务给 Worker 执行，累计 Token 消耗
 * 6. Token 预算熔断检查（总 Token > maxTokenBudget 时熔断）
 * 7. Supervisor 结果汇总（summarize）
 * 8. 记录执行日志
 *
 * 熔断机制：当触发熔断时，记录 status=3，设置 errorMsg，返回部分结果。
 *
 * @author zhicloud
 */
@Service
@Slf4j
public class MultiAgentExecuteServiceImpl implements MultiAgentExecuteService {

    /**
     * 执行状态：进行中
     */
    private static final int STATUS_RUNNING = 0;
    /**
     * 执行状态：成功
     */
    private static final int STATUS_SUCCESS = 1;
    /**
     * 执行状态：失败
     */
    private static final int STATUS_FAILED = 2;
    /**
     * 执行状态：熔断
     */
    private static final int STATUS_CIRCUIT_BREAKER = 3;

    /**
     * 拓扑状态：启用
     */
    private static final int TOPOLOGY_STATUS_ENABLED = 0;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Resource
    private MultiAgentTopologyMapper topologyMapper;
    @Resource
    private MultiAgentExecutionLogMapper executionLogMapper;
    @Resource
    private SupervisorAgent supervisorAgent;
    @Resource
    private WorkerAgentRegistry workerAgentRegistry;
    @Resource
    private ChatClientHelper chatClientHelper;
    @Resource
    private MultiAgentMetrics metrics;
    @Resource
    private MultiAgentProperties properties;

    @Override
    public MultiAgentExecutionLogDO execute(Long topologyId, String userInput, Long tenantId) {
        long startTime = System.currentTimeMillis();

        // 全链路 traceId：写入 MDC 供所有 Worker 日志携带，并在 finally 清理
        String traceId = UUID.randomUUID().toString();
        MDC.put("multiAgentTraceId", traceId);
        var executeSample = metrics.startExecute();

        // 0. 初始化执行日志
        MultiAgentExecutionLogDO logDO = new MultiAgentExecutionLogDO()
                .setTopologyId(topologyId)
                .setUserInput(userInput)
                .setTraceId(traceId)
                .setStatus(STATUS_RUNNING)
                .setTotalTokens(0)
                .setActualDepth(0);

        try {
            // 0.1 租户上下文由 Web 层（TenantInterceptor）注入，禁止用请求体 tenantId 覆盖，
            //      避免越权（IDOR）及覆盖外层租户导致后续 DB 操作越租户 / NPE。

            // 1. 加载拓扑配置
            MultiAgentTopologyDO topologyDO = topologyMapper.selectById(topologyId);
            if (ObjUtil.isNull(topologyDO)) {
                throw exception(EXECUTE_TOPOLOGY_NOT_EXISTS);
            }
            if (ObjUtil.notEqual(topologyDO.getStatus(), TOPOLOGY_STATUS_ENABLED)) {
                throw exception(EXECUTE_TOPOLOGY_DISABLED);
            }

            // 2. 检查 LLM 可用性
            if (!chatClientHelper.isAvailable()) {
                throw exception(EXECUTE_LLM_NOT_READY);
            }

            // 3. 解析拓扑配置为 AgentTopology
            AgentTopology topology = parseTopology(topologyDO);

            // 4. Supervisor 任务拆解
            List<AgentTask> tasks = supervisorAgent.planTasks(userInput, topology);
            logDO.setActualDepth(tasks.size());
            logDO.setSupervisorPlan(toJson(tasks));

            // 5. 调用深度熔断检查
            int maxDepth = topology.getMaxDepth() != null ? topology.getMaxDepth() : properties.getSupervisor().getMaxDepthDefault();
            if (tasks.size() > maxDepth) {
                String errorMsg = StrUtil.format("任务数({})超过最大调用深度({})", tasks.size(), maxDepth);
                log.warn("[execute][深度熔断，{}]", errorMsg);
                return finishWithCircuitBreaker(logDO, errorMsg, null, startTime);
            }

            // 6. 分发任务给 Worker 执行
            List<AgentResult> results = dispatchTasks(tasks, tenantId, topology);
            logDO.setWorkerResults(toJson(results));

            // 7. Token 预算熔断检查
            int totalTokens = results.stream()
                    .mapToInt(r -> r.getTokensUsed() != null ? r.getTokensUsed() : 0)
                    .sum();
            logDO.setTotalTokens(totalTokens);
            int maxTokenBudget = topology.getMaxTokenBudget() != null ? topology.getMaxTokenBudget() : properties.getSupervisor().getMaxTokenBudgetDefault();
            if (totalTokens > maxTokenBudget) {
                String errorMsg = StrUtil.format("Token 消耗({})超过预算上限({})", totalTokens, maxTokenBudget);
                log.warn("[execute][Token 熔断，{}]", errorMsg);
                return finishWithCircuitBreaker(logDO, errorMsg, results, startTime);
            }

            // 8. Supervisor 结果汇总（全部失败 / 无结果时如实标记失败，不伪装成功）
            boolean allFailed = CollUtil.isNotEmpty(results) && results.stream().allMatch(r -> !r.isSuccess());
            if (CollUtil.isEmpty(results) || allFailed) {
                logDO.setFinalAnswer(allFailed ? "（所有 Worker 执行失败，未生成汇总）"
                        : "（无 Worker 执行结果，未生成汇总）");
                logDO.setStatus(STATUS_FAILED);
                logDO.setDurationMs(System.currentTimeMillis() - startTime);
                log.warn("[execute][编排未成功，topologyId={}, allFailed={}, tasks={}]",
                        topologyId, allFailed, tasks.size());
            } else {
                String finalAnswer = supervisorAgent.summarize(userInput, results);
                logDO.setFinalAnswer(finalAnswer);
                logDO.setStatus(STATUS_SUCCESS);
                logDO.setDurationMs(System.currentTimeMillis() - startTime);
                log.info("[execute][编排执行成功，topologyId={}, tasks={}, tokens={}, duration={}ms]",
                        topologyId, tasks.size(), totalTokens, logDO.getDurationMs());
            }
        } catch (Exception e) {
            log.error("[execute][编排执行失败，topologyId={}]", topologyId, e);
            logDO.setStatus(STATUS_FAILED);
            logDO.setErrorMsg(StrUtil.sub(e.getMessage(), 0, 500));
            logDO.setDurationMs(System.currentTimeMillis() - startTime);
        } finally {
            // 8.1 记录编排执行指标（成功 = status 为 SUCCESS）
            boolean ok = logDO.getStatus() != null && logDO.getStatus() == STATUS_SUCCESS;
            metrics.recordExecute(executeSample, String.valueOf(topologyId), ok);
            MDC.remove("multiAgentTraceId");
            // 9. 记录执行日志（租户上下文由 Web 层管理，此处不清除）
            executionLogMapper.insert(logDO);
        }
        return logDO;
    }

    @Override
    public MultiAgentExecutionLogDO getExecutionLog(Long logId) {
        return executionLogMapper.selectById(logId);
    }

    @Override
    public List<MultiAgentExecutionLogDO> getExecutionLogListByTopologyId(Long topologyId) {
        return executionLogMapper.selectListByTopologyId(topologyId);
    }

    // ==================== 内部方法 ====================

    /**
     * 分发任务给 Worker 执行
     */
    private List<AgentResult> dispatchTasks(List<AgentTask> tasks, Long tenantId, AgentTopology topology) {
        // 拓扑白名单：仅允许执行拓扑中声明的 Worker，防止 LLM 越权调度其他业务域 Worker
        List<String> allowedWorkers = topology.getWorkers() == null ? Collections.emptyList()
                : topology.getWorkers().stream()
                .map(AgentTopology.WorkerConfig::getName)
                .toList();
        List<AgentResult> results = new ArrayList<>();
        for (AgentTask task : tasks) {
            results.add(executeTask(task, tenantId, allowedWorkers));
        }
        return results;
    }

    /**
     * 执行单个任务
     */
    private AgentResult executeTask(AgentTask task, Long tenantId, List<String> allowedWorkers) {
        String workerName = task.getAssignedWorker();
        if (StrUtil.isBlank(workerName)) {
            return AgentResult.builder()
                    .taskId(task.getTaskId())
                    .success(false)
                    .errorMsg("任务未分配 Worker")
                    .build();
        }
        if (!allowedWorkers.contains(workerName)) {
            return AgentResult.builder()
                    .taskId(task.getTaskId())
                    .success(false)
                    .errorMsg(StrUtil.format("Worker({}) 不在拓扑白名单内，拒绝执行", workerName))
                    .build();
        }
        AbstractWorkerAgent worker = workerAgentRegistry.getWorker(workerName);
        if (worker == null) {
            return AgentResult.builder()
                    .taskId(task.getTaskId())
                    .success(false)
                    .errorMsg(StrUtil.format("找不到 Worker({})", workerName))
                    .build();
        }
        try {
            log.info("[executeTask][开始执行任务，taskId={}, worker={}]",
                    task.getTaskId(), workerName);
            return worker.execute(task, tenantId);
        } catch (Exception e) {
            log.error("[executeTask][任务执行异常，taskId={}, worker={}]",
                    task.getTaskId(), workerName, e);
            return AgentResult.builder()
                    .taskId(task.getTaskId())
                    .success(false)
                    .errorMsg(StrUtil.sub(e.getMessage(), 0, 500))
                    .build();
        }
    }

    /**
     * 熔断时完成执行日志
     */
    private MultiAgentExecutionLogDO finishWithCircuitBreaker(MultiAgentExecutionLogDO logDO,
                                                               String errorMsg,
                                                               List<AgentResult> partialResults,
                                                               long startTime) {
        logDO.setStatus(STATUS_CIRCUIT_BREAKER);
        logDO.setErrorMsg(errorMsg);
        logDO.setDurationMs(System.currentTimeMillis() - startTime);
        if (partialResults != null) {
            logDO.setWorkerResults(toJson(partialResults));
        }
        // 熔断时尝试生成部分汇总
        try {
            if (partialResults != null && !partialResults.isEmpty()) {
                String partialAnswer = supervisorAgent.summarize(logDO.getUserInput(), partialResults);
                logDO.setFinalAnswer(StrUtil.format("【熔断提示】{}\n\n部分结果：\n{}", errorMsg, partialAnswer));
            } else {
                logDO.setFinalAnswer(StrUtil.format("【熔断提示】{}", errorMsg));
            }
        } catch (Exception e) {
            log.warn("[finishWithCircuitBreaker][部分汇总生成失败]", e);
            logDO.setFinalAnswer(StrUtil.format("【熔断提示】{}", errorMsg));
        }
        return logDO;
    }

    /**
     * 解析拓扑配置 DO 为 AgentTopology
     */
    private AgentTopology parseTopology(MultiAgentTopologyDO topologyDO) {
        AgentTopology topology = new AgentTopology();
        topology.setSupervisorSystemPrompt(topologyDO.getSupervisorSystemPrompt());
        topology.setVersion(topologyDO.getVersion() != null ? topologyDO.getVersion() : "v1");
        topology.setMaxDepth(topologyDO.getMaxDepth());
        topology.setMaxTokenBudget(topologyDO.getMaxTokenBudget());
        // 解析 Worker 配置 JSON
        if (StrUtil.isNotBlank(topologyDO.getWorkerConfig())) {
            try {
                List<AgentTopology.WorkerConfig> workers = OBJECT_MAPPER.readValue(
                        topologyDO.getWorkerConfig(),
                        OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, AgentTopology.WorkerConfig.class));
                topology.setWorkers(workers);
            } catch (Exception e) {
                log.error("[parseTopology][Worker 配置 JSON 解析失败，workerConfig={}]",
                        topologyDO.getWorkerConfig(), e);
                throw exception(TOPOLOGY_WORKER_CONFIG_INVALID);
            }
        } else {
            topology.setWorkers(CollUtil.newArrayList());
        }
        return topology;
    }

    /**
     * 对象转 JSON 字符串
     */
    private String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(obj);
        } catch (Exception e) {
            log.warn("[toJson][JSON 序列化失败，obj={}]", obj, e);
            return null;
        }
    }

}
