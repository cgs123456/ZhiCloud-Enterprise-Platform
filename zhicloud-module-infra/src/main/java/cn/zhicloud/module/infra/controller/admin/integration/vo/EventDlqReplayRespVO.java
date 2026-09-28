package cn.zhicloud.module.infra.controller.admin.integration.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 死信协调重放 Response VO")
@Data
public class EventDlqReplayRespVO {

    private String eventId;
    private boolean success;
    private boolean outboxReplayed;
    private int inboxReplayed;
    private String message;

}