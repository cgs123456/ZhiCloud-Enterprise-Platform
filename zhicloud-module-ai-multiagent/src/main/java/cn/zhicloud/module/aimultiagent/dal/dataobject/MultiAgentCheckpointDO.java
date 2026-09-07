package cn.zhicloud.module.aimultiagent.dal.dataobject;

import cn.zhicloud.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 多 Agent 编排检查点 DO
 *
 * <p>支撑编排中断恢复：执行流程按 PLAN_COMPLETED → WORKER_DONE（逐任务）→ SUMMARIZE_DONE
 * 写入检查点；中断后可从最后检查点继续。租约列用于并发执行保护与超时检测。
 *
 * @author zhicloud
 */
@TableName(value = "aimultiagent_checkpoint", autoResultMap = true)
@KeySequence("aimultiagent_checkpoint")
@Data
public class MultiAgentCheckpointDO extends BaseDO {

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
     * 拓扑 ID（冗余，便于按拓扑排查）
     */
    private Long topologyId;
    /**
     * 检查点类型（PLAN_COMPLETED/WORKER_DONE/SUMMARIZE_DONE）
     */
    private String checkpointType;
    /**
     * WORKER_DONE 时的 Worker 名称
     */
    private String workerName;
    /**
     * 当前任务序号（从 0 开始，WORKER_DONE 时有效）
     */
    private Integer taskIndex;
    /**
     * 序列化状态（已完成任务结果列表 JSON）
     */
    private String stateJson;
    /**
     * 租约令牌（执行中持有，完成后清空）
     */
    private String leaseToken;
    /**
     * 租约过期时间（超时未释放视为执行者失联）
     */
    private LocalDateTime leaseExpireTime;

}
