package cn.zhicloud.module.aimultiagent.model;

/**
 * Agent 编排执行 SSE 事件
 *
 * <p>事件类型：
 * <ul>
 *   <li>{@code plan_completed}：Supervisor 拆解完成，data 为任务数 JSON</li>
 *   <li>{@code worker_started}：某个 Worker 开始执行</li>
 *   <li>{@code worker_done}：某个 Worker 执行完成，data 含是否成功</li>
 *   <li>{@code circuit_breaker}：熔断，data 含熔断原因</li>
 *   <li>{@code error}：执行失败，data 含错误信息</li>
 *   <li>{@code final}：执行结束（成功或失败状态），data 为执行结果摘要 JSON</li>
 * </ul>
 *
 * @author zhicloud
 */
public record AgentSseEvent(
        String type,
        int taskIndex,
        int totalTasks,
        String workerName,
        String data,
        long timestamp
) {

    public static final String TYPE_PLAN_COMPLETED = "plan_completed";
    public static final String TYPE_WORKER_STARTED = "worker_started";
    public static final String TYPE_WORKER_DONE = "worker_done";
    public static final String TYPE_CIRCUIT_BREAKER = "circuit_breaker";
    public static final String TYPE_ERROR = "error";
    public static final String TYPE_FINAL = "final";

    public static AgentSseEvent of(String type, int taskIndex, int totalTasks,
                                   String workerName, String data) {
        return new AgentSseEvent(type, taskIndex, totalTasks, workerName, data, System.currentTimeMillis());
    }

    /**
     * 是否为终止事件（收到后服务端关闭 SSE 连接）
     */
    public boolean isTerminal() {
        return TYPE_FINAL.equals(type) || TYPE_ERROR.equals(type) || TYPE_CIRCUIT_BREAKER.equals(type);
    }

}
