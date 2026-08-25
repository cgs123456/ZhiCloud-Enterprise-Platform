package cn.zhicloud.module.aimultiagent.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Agent 结果模型
 *
 * Worker 执行完任务后返回的结果，供 Supervisor 汇总使用。
 *
 * @author zhicloud
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentResult {

    /**
     * 任务 ID（与 {@link AgentTask#getTaskId()} 对应）
     */
    private String taskId;

    /**
     * 是否执行成功
     */
    private boolean success;

    /**
     * Worker 输出（LLM 生成的文本）
     */
    private String output;

    /**
     * 本次任务消耗的 Token 数（用于预算熔断检查）
     */
    private Integer tokensUsed;

    /**
     * 错误信息（执行失败时填写）
     */
    private String errorMsg;

    /**
     * 执行耗时（毫秒）
     */
    private Long durationMs;

}
