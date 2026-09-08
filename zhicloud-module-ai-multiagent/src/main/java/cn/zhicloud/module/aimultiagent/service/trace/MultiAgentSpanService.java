package cn.zhicloud.module.aimultiagent.service.trace;

import cn.zhicloud.module.aimultiagent.controller.admin.trace.vo.MultiAgentTraceRespVO;
import cn.zhicloud.module.aimultiagent.dal.dataobject.MultiAgentExecutionSpanDO;

import java.util.List;

/**
 * 多 Agent 执行链路追踪 Service
 *
 * <p>埋点写入均为单行追加 + 降级容错（失败只告警，不影响编排主流程）。
 * 每个写方法只碰一个 Mapper，满足 scan_tx 门禁。
 *
 * @author zhicloud
 */
public interface MultiAgentSpanService {

    /** Span 类型：任务拆解 */
    String SPAN_PLAN = "PLAN";
    /** Span 类型：Worker 执行 */
    String SPAN_WORKER = "WORKER";
    /** Span 类型：结果汇总 */
    String SPAN_SUMMARIZE = "SUMMARIZE";

    /** Span 状态：运行中 */
    int STATUS_RUNNING = 0;
    /** Span 状态：成功 */
    int STATUS_SUCCESS = 1;
    /** Span 状态：失败 */
    int STATUS_FAILED = 2;

    /**
     * 开启一个 Span（状态=运行中）
     *
     * @param executionLogId 执行日志编号（为空直接返回 null，不写库）
     * @param traceId        全链路追踪 ID
     * @param spanType       Span 类型（PLAN/WORKER/SUMMARIZE）
     * @param workerName     Worker 名称（WORKER Span 有效）
     * @param taskIndex      任务序号（WORKER Span 有效）
     * @param taskId         任务 ID
     * @return Span 编号；executionLogId 为空或写入失败时返回 null
     */
    Long startSpan(Long executionLogId, String traceId, String spanType,
                   String workerName, Integer taskIndex, String taskId);

    /**
     * 结束一个 Span
     *
     * @param spanId        Span 编号（为空直接返回）
     * @param success       是否成功
     * @param tokens        消耗 Token 数（可为空）
     * @param durationMs    耗时毫秒（可为空）
     * @param errorMsg      错误信息（失败时）
     * @param outputExcerpt 输出摘要（已截断，可为空）
     */
    void finishSpan(Long spanId, boolean success, Integer tokens, Long durationMs,
                    String errorMsg, String outputExcerpt);

    /**
     * 查询某次执行的全部 Span（按开始时间升序）
     *
     * @param executionLogId 执行日志编号
     * @return Span 列表
     */
    List<MultiAgentExecutionSpanDO> getSpans(Long executionLogId);

    /**
     * 查询某次执行的完整轨迹（Span 列表 + 汇总）
     *
     * @param executionLogId 执行日志编号
     * @return 轨迹详情
     */
    MultiAgentTraceRespVO getTrace(Long executionLogId);

}
