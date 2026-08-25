package cn.zhicloud.module.airag.controller.admin.chat.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Schema(description = "管理后台 - AI RAG 发送问题 Response VO")
@Data
@Accessors(chain = true)
public class AiragRagChatSendRespVO {

    @Schema(description = "回答内容", required = true, example = "您可以通过 application.yaml 中的 zhicloud.tenant.* 进行配置...")
    private String content;

    @Schema(description = "命中的知识库分块")
    private List<RetrievedChunk> retrievedChunks;

    @Schema(description = "命中的知识库分块")
    @Data
    @Accessors(chain = true)
    public static class RetrievedChunk {

        @Schema(description = "分块内容", example = "多租户配置在 application.yaml...")
        private String content;

        @Schema(description = "相似度得分", example = "0.85")
        private Double score;

        @Schema(description = "来源文档名称", example = "Java 开发手册.pdf")
        private String documentName;

    }

}
