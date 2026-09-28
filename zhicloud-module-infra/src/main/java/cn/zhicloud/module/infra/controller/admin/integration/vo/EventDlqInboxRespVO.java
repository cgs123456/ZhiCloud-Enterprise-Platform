package cn.zhicloud.module.infra.controller.admin.integration.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - Inbox 死信 Response VO")
@Data
public class EventDlqInboxRespVO {

    private Long id;
    private String consumerName;
    private String eventId;
    private String eventType;
    private String status;
    private Integer retryCount;
    private LocalDateTime receivedAt;
    private LocalDateTime processedAt;
    private String error;

}