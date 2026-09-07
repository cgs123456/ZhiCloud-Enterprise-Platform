package cn.zhicloud.module.aimultiagent.service.execute;

import cn.zhicloud.module.aimultiagent.model.AgentResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static cn.zhicloud.module.aimultiagent.service.execute.CheckpointResumePlanner.Decision;
import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link CheckpointResumePlanner} 单元测试：覆盖正常流 / 中断恢复 / 已完成 / 无检查点 / 状态损坏判定。
 *
 * @author zhicloud
 */
class CheckpointResumePlannerTest {

    private static AgentResult done(String taskId) {
        return AgentResult.builder().taskId(taskId).success(true).output("ok").tokensUsed(10).build();
    }

    @Test
    void decide_logAlreadySuccess_returnsAlreadyDone() {
        CheckpointResumePlanner.ResumePlan plan =
                CheckpointResumePlanner.decide(null, null, null, true);
        assertEquals(Decision.ALREADY_DONE, plan.decision());
        assertEquals(-1, plan.startIndex());
        assertTrue(CheckpointResumePlanner.isConsistent(plan));
    }

    @Test
    void decide_summarizeDone_returnsAlreadyDone() {
        CheckpointResumePlanner.ResumePlan plan = CheckpointResumePlanner.decide(
                CheckpointResumePlanner.TYPE_SUMMARIZE_DONE, null, null, false);
        assertEquals(Decision.ALREADY_DONE, plan.decision());
    }

    @Test
    void decide_noCheckpoint_returnsNothingToResume() {
        CheckpointResumePlanner.ResumePlan plan =
                CheckpointResumePlanner.decide(null, null, null, false);
        assertEquals(Decision.NOTHING_TO_RESUME, plan.decision());
        assertTrue(CheckpointResumePlanner.isConsistent(plan));
    }

    @Test
    void decide_planCompleted_returnsStartOver() {
        CheckpointResumePlanner.ResumePlan plan = CheckpointResumePlanner.decide(
                CheckpointResumePlanner.TYPE_PLAN_COMPLETED, null, null, false);
        assertEquals(Decision.START_OVER, plan.decision());
        assertEquals(0, plan.startIndex());
        assertTrue(plan.completedTasks().isEmpty());
    }

    @Test
    void decide_workerDone_returnsContinueFromNextIndex() {
        List<AgentResult> done = List.of(done("t0"), done("t1"), done("t2"));
        CheckpointResumePlanner.ResumePlan plan = CheckpointResumePlanner.decide(
                CheckpointResumePlanner.TYPE_WORKER_DONE, 2, done, false);
        assertEquals(Decision.CONTINUE_FROM_INDEX, plan.decision());
        assertEquals(3, plan.startIndex());
        assertEquals(3, plan.completedTasks().size());
        assertTrue(CheckpointResumePlanner.isConsistent(plan));
    }

    @Test
    void decide_workerDoneWithoutIndex_returnsContinueFromZero() {
        CheckpointResumePlanner.ResumePlan plan = CheckpointResumePlanner.decide(
                CheckpointResumePlanner.TYPE_WORKER_DONE, null, null, false);
        assertEquals(Decision.CONTINUE_FROM_INDEX, plan.decision());
        assertEquals(0, plan.startIndex());
        assertTrue(plan.completedTasks().isEmpty());
    }

    @Test
    void decide_unknownType_returnsNothingToResume() {
        CheckpointResumePlanner.ResumePlan plan =
                CheckpointResumePlanner.decide("SOME_FUTURE_TYPE", 5, null, false);
        assertEquals(Decision.NOTHING_TO_RESUME, plan.decision());
    }

    @Test
    void isConsistent_moreResultsThanStartIndex_returnsFalse() {
        // 状态损坏：已完成 5 个结果却要从第 3 个开始，说明检查点错乱
        List<AgentResult> done = List.of(done("t0"), done("t1"), done("t2"), done("t3"), done("t4"));
        CheckpointResumePlanner.ResumePlan plan =
                new CheckpointResumePlanner.ResumePlan(Decision.CONTINUE_FROM_INDEX, 2, done);
        assertFalse(CheckpointResumePlanner.isConsistent(plan));
    }

    @Test
    void isConsistent_nonContinueDecision_returnsTrue() {
        CheckpointResumePlanner.ResumePlan plan =
                new CheckpointResumePlanner.ResumePlan(Decision.START_OVER, 0, null);
        assertTrue(CheckpointResumePlanner.isConsistent(plan));
    }

}
