package cn.zhicloud.module.infra.controller.admin.integration.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - Outbox 死信 Response VO")
@Data
public class EventDlqOutboxRespVO {

    private Long id;
    private String eventId;
    private String aggregateType;
    private String aggregateId;
    private String eventType;
    private String schemaVersion;
    private String status;
    private Integer retryCount;
    private LocalDateTime nextRetryAt;
    private LocalDateTime occurredAt;
    private LocalDateTime publishedAt;
    private String traceId;
    private String lastError;

}