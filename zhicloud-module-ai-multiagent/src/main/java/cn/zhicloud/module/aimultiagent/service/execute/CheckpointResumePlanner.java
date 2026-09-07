package cn.zhicloud.module.aimultiagent.service.execute;

import cn.zhicloud.module.aimultiagent.model.AgentResult;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 检查点恢复决策器（纯函数，可单测）
 *
 * <p>根据某次执行的最新检查点，判定恢复动作。与 DB/LLM 无关，便于单元测试覆盖
 * 正常流 / 中断恢复 / 已完成 / 无检查点 / 状态损坏等分支。
 *
 * @author zhicloud
 */
public final class CheckpointResumePlanner {

    /** 任务拆解完成 */
    public static final String TYPE_PLAN_COMPLETED = "PLAN_COMPLETED";
    /** 单个 Worker 完成 */
    public static final String TYPE_WORKER_DONE = "WORKER_DONE";
    /** 结果汇总完成 */
    public static final String TYPE_SUMMARIZE_DONE = "SUMMARIZE_DONE";

    private CheckpointResumePlanner() {
    }

    /**
     * 恢复决策
     */
    public enum Decision {
        /** 无可恢复内容（无检查点） */
        NOTHING_TO_RESUME,
        /** 已完成，无需恢复 */
        ALREADY_DONE,
        /** 从头开始（仅有拆解结果） */
        START_OVER,
        /** 从指定序号继续 */
        CONTINUE_FROM_INDEX,
    }

    /**
     * 恢复计划
     *
     * @param decision       决策
     * @param startIndex     CONTINUE_FROM_INDEX 时的起始任务序号（含）；其余情况为 -1
     * @param completedTasks 已完成的任务结果（不可变视图的防御拷贝来源，调用方自行决定是否复用）
     */
    public record ResumePlan(Decision decision, int startIndex, List<AgentResult> completedTasks) {
    }

    /**
     * 根据最新检查点判定恢复动作
     *
     * @param latestType      最新检查点类型，可为 null（无检查点）
     * @param latestTaskIndex 最新检查点的任务序号（WORKER_DONE 时有效，可为 null）
     * @param completedTasks  已解析出的已完成任务结果（调用方负责反序列化 state_json）
     * @param logStatusDone   执行日志是否已标记成功
     * @return 恢复计划
     */
    public static ResumePlan decide(String latestType, Integer latestTaskIndex,
                                    List<AgentResult> completedTasks, boolean logStatusDone) {
        if (logStatusDone || TYPE_SUMMARIZE_DONE.equals(latestType)) {
            return new ResumePlan(Decision.ALREADY_DONE, -1, Collections.emptyList());
        }
        if (latestType == null) {
            return new ResumePlan(Decision.NOTHING_TO_RESUME, -1, Collections.emptyList());
        }
        if (TYPE_PLAN_COMPLETED.equals(latestType)) {
            return new ResumePlan(Decision.START_OVER, 0, Collections.emptyList());
        }
        if (TYPE_WORKER_DONE.equals(latestType)) {
            int startIndex = latestTaskIndex == null ? 0 : latestTaskIndex + 1;
            List<AgentResult> done = completedTasks == null
                    ? Collections.emptyList()
                    : List.copyOf(completedTasks);
            return new ResumePlan(Decision.CONTINUE_FROM_INDEX, startIndex, done);
        }
        // 未知检查点类型：视为无可恢复内容，由调用方按无检查点处理并告警
        return new ResumePlan(Decision.NOTHING_TO_RESUME, -1, Collections.emptyList());
    }

    /**
     * 校验已完成任务结果与起始序号是否自洽（防御状态损坏）
     *
     * @param plan 恢复计划
     * @return 自洽返回 true
     */
    public static boolean isConsistent(ResumePlan plan) {
        Objects.requireNonNull(plan, "plan");
        if (plan.decision() != Decision.CONTINUE_FROM_INDEX) {
            return true;
        }
        return plan.completedTasks() != null && plan.completedTasks().size() <= plan.startIndex();
    }

}
