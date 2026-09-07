package cn.zhicloud.module.aimultiagent.service.execute;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.zhicloud.framework.tenant.core.job.TenantJob;
import cn.zhicloud.module.aimultiagent.config.ChatClientHelper;
import cn.zhicloud.module.aimultiagent.config.MultiAgentProperties;
import cn.zhicloud.module.aimultiagent.dal.dataobject.MultiAgentCheckpointDO;
import cn.zhicloud.module.aimultiagent.dal.dataobject.MultiAgentExecutionLogDO;
import cn.zhicloud.module.aimultiagent.dal.dataobject.MultiAgentTopologyDO;
import cn.zhicloud.module.aimultiagent.dal.mysql.MultiAgentCheckpointMapper;
import cn.zhicloud.module.aimultiagent.dal.mysql.MultiAgentExecutionLogMapper;
import cn.zhicloud.module.aimultiagent.dal.mysql.MultiAgentTopologyMapper;
import cn.zhicloud.module.aimultiagent.model.AgentResult;
import cn.zhicloud.module.aimultiagent.model.AgentSseEvent;
import cn.zhicloud.module.aimultiagent.model.AgentTask;
import cn.zhicloud.module.aimultiagent.model.AgentTopology;
import cn.zhicloud.module.aimultiagent.service.agent.AbstractWorkerAgent;
import cn.zhicloud.module.aimultiagent.service.agent.SupervisorAgent;
import cn.zhicloud.module.aimultiagent.service.agent.WorkerAgentRegistry;
import cn.zhicloud.module.aimultiagent.service.metrics.MultiAgentMetrics;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.aimultiagent.enums.ErrorCodeConstants.*;
import static cn.zhicloud.module.aimultiagent.service.execute.CheckpointResumePlanner.*;

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

    /**
     * 任务执行租约有效期（毫秒）。持有租约的执行者失联超此时长后，可被超时扫描释放。
     */
    private static final long LEASE_TIMEOUT_MS = 120_000L;

    @Resource
    private MultiAgentTopologyMapper topologyMapper;
    @Resource
    private MultiAgentExecutionLogMapper executionLogMapper;
    @Resource
    private MultiAgentCheckpointMapper checkpointMapper;
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
        return executeInternal(topologyId, userInput, tenantId, null);
    }

    @Override
    public void executeWithSse(Long topologyId, String userInput, Long tenantId,
                               Consumer<AgentSseEvent> eventConsumer) {
        // 终止事件（final/error/circuit_breaker）必达其一，由调用方据此关闭 SSE 连接；
        // 未预期的抛出由调用方转为连接异常关闭。
        executeInternal(topologyId, userInput, tenantId, eventConsumer);
    }

    // @tx-ignore 本方法含分钟级 LLM 调用（plan/dispatch/summarize），包事务会长时间占用数据库连接；
    // 各写入均为追加式单行（执行日志 + 检查点），失败可独立重试，无需原子性，因此不加 @Transactional。
    private MultiAgentExecutionLogDO executeInternal(Long topologyId, String userInput, Long tenantId,
                                                     Consumer<AgentSseEvent> eventConsumer) {
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

            // 2.1 先落库拿到执行日志 ID，后续检查点与租约都关联该 ID（中断恢复依赖）
            executionLogMapper.insert(logDO);

            // 3. 解析拓扑配置为 AgentTopology
            AgentTopology topology = parseTopology(topologyDO);

            // 4. Supervisor 任务拆解
            List<AgentTask> tasks = supervisorAgent.planTasks(userInput, topology);
            logDO.setActualDepth(tasks.size());
            logDO.setSupervisorPlan(toJson(tasks));
            // 4.1 检查点：拆解完成（保存任务定义，供 resume 复用，避免重拆导致任务漂移）
            MultiAgentCheckpointDO planCheckpoint =
                    writeCheckpoint(logDO.getId(), topologyId, TYPE_PLAN_COMPLETED, null, null, toJson(tasks));
            emit(eventConsumer, AgentSseEvent.TYPE_PLAN_COMPLETED, -1, tasks.size(), null,
                    StrUtil.format("{{\"taskCount\":{}}}", tasks.size()));

            // 5. 调用深度熔断检查
            int maxDepth = topology.getMaxDepth() != null ? topology.getMaxDepth() : properties.getSupervisor().getMaxDepthDefault();
            if (tasks.size() > maxDepth) {
                String errorMsg = StrUtil.format("任务数({})超过最大调用深度({})", tasks.size(), maxDepth);
                log.warn("[execute][深度熔断，{}]", errorMsg);
                emit(eventConsumer, AgentSseEvent.TYPE_CIRCUIT_BREAKER, -1, tasks.size(), null,
                        eventMessage(errorMsg));
                return finishWithCircuitBreaker(logDO, errorMsg, null, startTime);
            }

            // 6. 分发任务给 Worker 执行（持有租约，防并发重复执行）
            String leaseToken = UUID.randomUUID().toString();
            acquireFlowLease(planCheckpoint.getId(), leaseToken);
            List<AgentResult> results;
            try {
                results = dispatchTasksWithCheckpoints(tasks, tenantId, topology, logDO.getId(), topologyId,
                        eventConsumer);
            } finally {
                releaseFlowLease(planCheckpoint.getId(), leaseToken);
            }
            logDO.setWorkerResults(toJson(results));

            // 7. Token 预算熔断检查
            int totalTokens = sumTokens(results);
            logDO.setTotalTokens(totalTokens);
            int maxTokenBudget = topology.getMaxTokenBudget() != null ? topology.getMaxTokenBudget() : properties.getSupervisor().getMaxTokenBudgetDefault();
            if (totalTokens > maxTokenBudget) {
                String errorMsg = StrUtil.format("Token 消耗({})超过预算上限({})", totalTokens, maxTokenBudget);
                log.warn("[execute][Token 熔断，{}]", errorMsg);
                emit(eventConsumer, AgentSseEvent.TYPE_CIRCUIT_BREAKER, tasks.size(), tasks.size(), null,
                        eventMessage(errorMsg));
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
                Map<String, Object> failData = new HashMap<>();
                failData.put("message", logDO.getFinalAnswer());
                failData.put("executionLogId", logDO.getId());
                emit(eventConsumer, AgentSseEvent.TYPE_ERROR, tasks.size(), tasks.size(), null, toJson(failData));
            } else {
                String finalAnswer = supervisorAgent.summarize(userInput, results);
                logDO.setFinalAnswer(finalAnswer);
                logDO.setStatus(STATUS_SUCCESS);
                logDO.setDurationMs(System.currentTimeMillis() - startTime);
                log.info("[execute][编排执行成功，topologyId={}, tasks={}, tokens={}, duration={}ms]",
                        topologyId, tasks.size(), totalTokens, logDO.getDurationMs());
                Map<String, Object> finalData = new HashMap<>();
                finalData.put("status", STATUS_SUCCESS);
                finalData.put("totalTokens", totalTokens);
                finalData.put("finalAnswer", finalAnswer);
                finalData.put("executionLogId", logDO.getId());
                emit(eventConsumer, AgentSseEvent.TYPE_FINAL, tasks.size(), tasks.size(), null, toJson(finalData));
            }
            // 8.1 检查点：汇总完成
            writeCheckpoint(logDO.getId(), topologyId, TYPE_SUMMARIZE_DONE, null, null, null);
        } catch (Exception e) {
            log.error("[execute][编排执行失败，topologyId={}]", topologyId, e);
            logDO.setStatus(STATUS_FAILED);
            logDO.setErrorMsg(StrUtil.sub(e.getMessage(), 0, 500));
            logDO.setDurationMs(System.currentTimeMillis() - startTime);
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("message", logDO.getErrorMsg());
            errorData.put("executionLogId", logDO.getId());
            emit(eventConsumer, AgentSseEvent.TYPE_ERROR, -1, -1, null, toJson(errorData));
        } finally {
            // 8.2 记录编排执行指标（成功 = status 为 SUCCESS）
            boolean ok = logDO.getStatus() != null && logDO.getStatus() == STATUS_SUCCESS;
            metrics.recordExecute(executeSample, String.valueOf(topologyId), ok);
            MDC.remove("multiAgentTraceId");
            // 9. 落库执行日志：校验前失败时尚未 insert，此处补写；其余情况 update
            //    （租户上下文由 Web 层管理，此处不清除）
            if (logDO.getId() == null) {
                executionLogMapper.insert(logDO);
            } else {
                executionLogMapper.updateById(logDO);
            }
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

    @Override
    public List<MultiAgentCheckpointDO> getCheckpoints(Long executionLogId) {
        return checkpointMapper.selectListByExecutionLogId(executionLogId);
    }

    // @tx-ignore 同 execute()：含分钟级 LLM 调用，不包事务；各写入均为追加式单行、可独立重试。
    @Override
    public MultiAgentExecutionLogDO resume(Long executionLogId, Long tenantId) {
        long startTime = System.currentTimeMillis();
        String traceId = UUID.randomUUID().toString();
        MDC.put("multiAgentTraceId", traceId);
        try {
            // 1. 加载执行日志（租户隔离由 Mapper 拦截器保证，跨租户查不到）
            MultiAgentExecutionLogDO logDO = executionLogMapper.selectById(executionLogId);
            if (logDO == null) {
                throw exception(EXECUTION_LOG_NOT_EXISTS);
            }
            if (Integer.valueOf(STATUS_SUCCESS).equals(logDO.getStatus())) {
                log.info("[resume][执行已成功，无需恢复，executionLogId={}]", executionLogId);
                return logDO;
            }
            // 2. 取最新检查点并判定恢复动作
            List<MultiAgentCheckpointDO> checkpoints = checkpointMapper.selectListByExecutionLogId(executionLogId);
            MultiAgentCheckpointDO latest = checkpoints.isEmpty() ? null : checkpoints.get(checkpoints.size() - 1);
            List<AgentResult> completed = latest != null && TYPE_WORKER_DONE.equals(latest.getCheckpointType())
                    ? parseResults(latest.getStateJson()) : Collections.emptyList();
            CheckpointResumePlanner.ResumePlan plan = CheckpointResumePlanner.decide(
                    latest == null ? null : latest.getCheckpointType(),
                    latest == null ? null : latest.getTaskIndex(),
                    completed, false);
            switch (plan.decision()) {
                case NOTHING_TO_RESUME:
                    throw exception(CHECKPOINT_RESUME_NOTHING);
                case ALREADY_DONE:
                    return logDO;
                case START_OVER:
                case CONTINUE_FROM_INDEX:
                    break;
                default:
                    throw exception(CHECKPOINT_RESUME_NOTHING);
            }
            if (!CheckpointResumePlanner.isConsistent(plan)) {
                throw exception(CHECKPOINT_STATE_CORRUPT);
            }
            // 3. 取出拆解时的任务定义（PLAN 检查点，保证与中断前一致，不重拆）
            List<AgentTask> tasks = loadPlannedTasks(checkpoints);
            // 4. 从断点继续执行
            MultiAgentTopologyDO topologyDO = topologyMapper.selectById(logDO.getTopologyId());
            if (ObjUtil.isNull(topologyDO)) {
                throw exception(EXECUTE_TOPOLOGY_NOT_EXISTS);
            }
            AgentTopology topology = parseTopology(topologyDO);
            List<AgentTask> remaining = tasks.subList(Math.min(plan.startIndex(), tasks.size()), tasks.size());
            List<AgentResult> results = new ArrayList<>(plan.completedTasks());
            String leaseToken = UUID.randomUUID().toString();
            MultiAgentCheckpointDO anchor = latest != null ? latest
                    : writeCheckpoint(logDO.getId(), logDO.getTopologyId(), TYPE_PLAN_COMPLETED, null, null, toJson(tasks));
            acquireFlowLease(anchor.getId(), leaseToken, executionLogId);
            try {
                int base = tasks.size() - remaining.size();
                for (int i = 0; i < remaining.size(); i++) {
                    AgentTask task = remaining.get(i);
                    AgentResult result = executeTask(task, tenantId,
                            allowedWorkerNames(topology));
                    results.add(result);
                    writeCheckpoint(logDO.getId(), logDO.getTopologyId(), TYPE_WORKER_DONE,
                            task.getAssignedWorker(), base + i, toJson(results));
                }
            } finally {
                releaseFlowLease(anchor.getId(), leaseToken);
            }
            // 5. 汇总并落库（与 execute() 第 7-8 步一致）
            logDO.setWorkerResults(toJson(results));
            int totalTokens = sumTokens(results);
            logDO.setTotalTokens(totalTokens);
            boolean allFailed = CollUtil.isNotEmpty(results) && results.stream().allMatch(r -> !r.isSuccess());
            if (CollUtil.isEmpty(results) || allFailed) {
                logDO.setFinalAnswer(allFailed ? "（所有 Worker 执行失败，未生成汇总）"
                        : "（无 Worker 执行结果，未生成汇总）");
                logDO.setStatus(STATUS_FAILED);
            } else {
                logDO.setFinalAnswer(supervisorAgent.summarize(logDO.getUserInput(), results));
                logDO.setStatus(STATUS_SUCCESS);
            }
            logDO.setDurationMs(System.currentTimeMillis() - startTime);
            writeCheckpoint(logDO.getId(), logDO.getTopologyId(), TYPE_SUMMARIZE_DONE, null, null, null);
            executionLogMapper.updateById(logDO);
            log.info("[resume][恢复完成，executionLogId={}, status={}]", executionLogId, logDO.getStatus());
            return logDO;
        } finally {
            MDC.remove("multiAgentTraceId");
        }
    }

    /**
     * 租约超时扫描：释放失联执行者持有的过期租约，使 resume 可以接管。
     *
     * <p>必须加 {@code @TenantJob} 逐租户执行——定时线程自身无租户上下文，
     * 直接查租户表会 NPE（见 TenantContextHolder.getRequiredTenantId）。
     */
    @Scheduled(fixedDelay = 60000)
    @TenantJob
    public void scanExpiredLeases() {
        List<MultiAgentCheckpointDO> expired = checkpointMapper.selectExpiredLeases();
        for (MultiAgentCheckpointDO cp : expired) {
            int rows = checkpointMapper.releaseLease(cp.getId(), cp.getLeaseToken());
            if (rows > 0) {
                log.warn("[scanExpiredLeases][释放过期租约，executionLogId={}, checkpointId={}]",
                        cp.getExecutionLogId(), cp.getId());
            }
        }
    }

    // ==================== 内部方法 ====================

    /**
     * 分发任务并逐个写 WORKER_DONE 检查点
     */
    private List<AgentResult> dispatchTasksWithCheckpoints(List<AgentTask> tasks, Long tenantId,
                                                           AgentTopology topology, Long executionLogId,
                                                           Long topologyId,
                                                           Consumer<AgentSseEvent> eventConsumer) {
        List<String> allowedWorkers = allowedWorkerNames(topology);
        List<AgentResult> results = new ArrayList<>();
        for (int i = 0; i < tasks.size(); i++) {
            AgentTask task = tasks.get(i);
            emit(eventConsumer, AgentSseEvent.TYPE_WORKER_STARTED, i, tasks.size(),
                    task.getAssignedWorker(), null);
            AgentResult result = executeTask(task, tenantId, allowedWorkers);
            results.add(result);
            writeCheckpoint(executionLogId, topologyId, TYPE_WORKER_DONE,
                    task.getAssignedWorker(), i, toJson(results));
            Map<String, Object> doneData = new HashMap<>();
            doneData.put("success", result.isSuccess());
            emit(eventConsumer, AgentSseEvent.TYPE_WORKER_DONE, i, tasks.size(),
                    task.getAssignedWorker(), toJson(doneData));
        }
        return results;
    }

    /**
     * 发送 SSE 事件（consumer 为空表示同步模式，直接跳过；发送失败仅记录，不中断主流程）
     */
    private void emit(Consumer<AgentSseEvent> eventConsumer, String type, int taskIndex, int totalTasks,
                      String workerName, String data) {
        if (eventConsumer == null) {
            return;
        }
        try {
            eventConsumer.accept(AgentSseEvent.of(type, taskIndex, totalTasks, workerName, data));
        } catch (Exception e) {
            log.debug("[emit][SSE 事件发送失败，type={}]", type, e);
        }
    }

    /**
     * 构造 {"message": ...} 事件数据（Jackson 转义，避免手工拼 JSON 注入问题）
     */
    private String eventMessage(String message) {
        Map<String, Object> data = new HashMap<>();
        data.put("message", message);
        return toJson(data);
    }

    /**
     * 拓扑白名单：仅允许执行拓扑中声明的 Worker，防止 LLM 越权调度其他业务域 Worker
     */
    private List<String> allowedWorkerNames(AgentTopology topology) {
        return topology.getWorkers() == null ? Collections.emptyList()
                : topology.getWorkers().stream()
                .map(AgentTopology.WorkerConfig::getName)
                .toList();
    }

    /**
     * 汇总 Token 消耗（null 按 0 计）
     */
    private int sumTokens(List<AgentResult> results) {
        return results.stream()
                .mapToInt(r -> r.getTokensUsed() != null ? r.getTokensUsed() : 0)
                .sum();
    }

    /**
     * 写检查点（降级容错：写入失败只告警，不中断主流程；resume 按已有检查点恢复）
     */
    private MultiAgentCheckpointDO writeCheckpoint(Long executionLogId, Long topologyId, String type,
                                                   String workerName, Integer taskIndex, String stateJson) {
        MultiAgentCheckpointDO cp = new MultiAgentCheckpointDO();
        cp.setExecutionLogId(executionLogId);
        cp.setTopologyId(topologyId);
        cp.setCheckpointType(type);
        cp.setWorkerName(workerName);
        cp.setTaskIndex(taskIndex);
        cp.setStateJson(stateJson);
        try {
            checkpointMapper.insert(cp);
        } catch (Exception e) {
            log.warn("[writeCheckpoint][检查点写入失败，executionLogId={}, type={}]", executionLogId, type, e);
        }
        return cp;
    }

    /**
     * 从 PLAN 检查点取出拆解时的任务定义（保证与中断前一致）
     */
    private List<AgentTask> loadPlannedTasks(List<MultiAgentCheckpointDO> checkpoints) {
        for (MultiAgentCheckpointDO cp : checkpoints) {
            if (TYPE_PLAN_COMPLETED.equals(cp.getCheckpointType()) && StrUtil.isNotBlank(cp.getStateJson())) {
                try {
                    return OBJECT_MAPPER.readValue(cp.getStateJson(),
                            OBJECT_MAPPER.getTypeFactory().constructCollectionType(List.class, AgentTask.class));
                } catch (Exception e) {
                    throw exception(CHECKPOINT_STATE_CORRUPT);
                }
            }
        }
        throw exception(CHECKPOINT_STATE_CORRUPT);
    }

    /**
     * 反序列化已完成任务结果；损坏则抛检查点损坏异常
     */
    private List<AgentResult> parseResults(String stateJson) {
        if (StrUtil.isBlank(stateJson)) {
            return Collections.emptyList();
        }
        try {
            return OBJECT_MAPPER.readValue(stateJson, new TypeReference<List<AgentResult>>() {
            });
        } catch (Exception e) {
            throw exception(CHECKPOINT_STATE_CORRUPT);
        }
    }

    /**
     * 获取流程租约（失败说明正被其他执行者处理）
     */
    private void acquireFlowLease(Long checkpointId, String leaseToken) {
        acquireFlowLease(checkpointId, leaseToken, null);
    }

    /**
     * 获取流程租约（失败说明正被其他执行者处理）
     */
    private void acquireFlowLease(Long checkpointId, String leaseToken, Long executionLogId) {
        int rows = checkpointMapper.acquireLease(checkpointId, leaseToken, LEASE_TIMEOUT_MS);
        if (rows == 0) {
            log.warn("[acquireFlowLease][租约获取失败，executionLogId={}, checkpointId={}]", executionLogId, checkpointId);
            throw exception(CHECKPOINT_LEASE_CONFLICT);
        }
    }

    /**
     * 释放流程租约（释放失败仅告警，不影响主流程结果）
     */
    private void releaseFlowLease(Long checkpointId, String leaseToken) {
        try {
            checkpointMapper.releaseLease(checkpointId, leaseToken);
        } catch (Exception e) {
            log.warn("[releaseFlowLease][租约释放失败，checkpointId={}]", checkpointId, e);
        }
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
