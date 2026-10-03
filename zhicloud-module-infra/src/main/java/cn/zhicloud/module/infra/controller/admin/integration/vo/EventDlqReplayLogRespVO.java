package cn.zhicloud.module.infra.controller.admin.integration.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 死信重放审计 Response VO")
@Data
public class EventDlqReplayLogRespVO {

    private Long id;
    private String eventId;
    private Long operatorId;
    private String operatorName;
    private String reason;
    private String result;
    private Boolean outboxReplayed;
    private Integer inboxReplayed;
    private String errorMessage;
    private LocalDateTime createTime;

}