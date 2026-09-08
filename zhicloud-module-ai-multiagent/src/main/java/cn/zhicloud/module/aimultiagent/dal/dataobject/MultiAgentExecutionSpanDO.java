package cn.zhicloud.module.aimultiagent.dal.dataobject;

import cn.zhicloud.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 多 Agent 执行链路 Span DO
 *
 * <p>一次编排执行拆解为 PLAN → WORKER（逐任务）→ SUMMARIZE 三类 Span，
 * 记录起止时间、耗时、Token、状态与输出摘要，供事后复盘与时间线可视化。
 *
 * @author zhicloud
 */
@TableName(value = "aimultiagent_execution_span", autoResultMap = true)
@KeySequence("aimultiagent_execution_span")
@Data
public class MultiAgentExecutionSpanDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 关联执行日志 aimultiagent_execution_log.id
     */
    private Long executionLogId;
    /**
     * 全链路追踪 ID（与执行日志 trace_id 一致）
     */
    private String traceId;
    /**
     * Span 类型（PLAN/WORKER/SUMMARIZE）
     */
    private String spanType;
    /**
     * WORKER Span 的 Worker 名称
     */
    private String workerName;
    /**
     * WORKER Span 的任务序号（从 0 开始）
     */
    private Integer taskIndex;
    /**
     * 任务 ID（与 AgentTask.taskId 对应）
     */
    private String taskId;
    /**
     * 状态（0运行中 1成功 2失败）
     */
    private Integer status;
    /**
     * 本 Span 消耗的 Token 数
     */
    private Integer tokens;
    /**
     * 耗时（毫秒）
     */
    private Long durationMs;
    /**
     * 开始时间
     */
    private LocalDateTime startTime;
    /**
     * 结束时间
     */
    private LocalDateTime endTime;
    /**
     * 错误信息（失败时）
     */
    private String errorMsg;
    /**
     * 输出摘要（截断，避免大文本）
     */
    private String outputExcerpt;

}
