package cn.zhicloud.framework.integration.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 死信协调重放结果（V105）
 *
 * @author 智云集成治理
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventDlqReplayResult {

    public static final String RESULT_SUCCESS = "SUCCESS";
    public static final String RESULT_FAILED = "FAILED";

    /**
     * 事件编号
     */
    private String eventId;

    /**
     * 本次重放是否成功；批量接口用该字段表达逐条结果
     */
    private boolean success;

    /**
     * 是否重新排队了 Outbox
     */
    private boolean outboxReplayed;

    /**
     * 实际恢复的 Inbox 死信数量
     */
    private int inboxReplayed;

    /**
     * 失败原因；成功时为空
     */
    private String message;

    public static EventDlqReplayResult failure(String eventId, String message) {
        return EventDlqReplayResult.builder()
                .eventId(eventId)
                .success(false)
                .message(message)
                .build();
    }

}
