package cn.zhicloud.module.airag.controller.admin.chat.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - AI RAG 发送问题 Request VO")
@Data
public class AiragRagChatSendReqVO {

    @Schema(description = "知识库编号", required = true, example = "1")
    @NotNull(message = "知识库编号不能为空")
    private Long knowledgeId;

    @Schema(description = "用户问题", required = true, example = "如何配置多租户？")
    @NotBlank(message = "问题不能为空")
    private String question;

}
