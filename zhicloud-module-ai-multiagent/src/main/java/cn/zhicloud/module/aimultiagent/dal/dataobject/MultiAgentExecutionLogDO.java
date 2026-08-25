package cn.zhicloud.module.aimultiagent.dal.dataobject;

import cn.zhicloud.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 多 Agent 执行日志 DO
 *
 * @author zhicloud
 */
@TableName(value = "aimultiagent_execution_log", autoResultMap = true)
@KeySequence("aimultiagent_execution_log")
@Data
public class MultiAgentExecutionLogDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 拓扑 ID
     */
    private Long topologyId;
    /**
     * 用户输入
     */
    private String userInput;
    /**
     * Supervisor 任务拆解 JSON
     */
    private String supervisorPlan;
    /**
     * Worker 执行结果 JSON
     */
    private String workerResults;
    /**
     * 最终汇总答案
     */
    private String finalAnswer;
    /**
     * 总 Token 消耗
     */
    private Integer totalTokens;
    /**
     * 实际调用深度
     */
    private Integer actualDepth;
    /**
     * 执行状态（0进行中 1成功 2失败 3熔断）
     */
    private Integer status;
    /**
     * 错误信息
     */
    private String errorMsg;
    /**
     * 执行耗时（毫秒）
     */
    private Long durationMs;
    /**
     * 全链路追踪 ID（跨 Worker / Supervisor 排查用，写入 MDC）
     */
    private String traceId;

}
