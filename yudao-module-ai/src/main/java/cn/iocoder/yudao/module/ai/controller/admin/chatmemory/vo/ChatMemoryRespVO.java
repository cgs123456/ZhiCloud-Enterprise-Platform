package cn.iocoder.yudao.module.ai.controller.admin.chatmemory.vo;

import cn.iocoder.yudao.module.ai.service.chatmemory.dto.ChatMessageDTO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - Chat 记忆 Response VO")
@Data
public class ChatMemoryRespVO {

    @Schema(description = "会话 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "conv-1024")
    private String conversationId;

    @Schema(description = "消息列表（按时间正序）")
    private List<ChatMessageDTO> messages;

    @Schema(description = "长期记忆摘要", example = "用户之前询问了库存报告...")
    private String summary;

}
