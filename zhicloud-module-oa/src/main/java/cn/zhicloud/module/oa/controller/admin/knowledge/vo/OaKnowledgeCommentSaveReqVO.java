package cn.zhicloud.module.oa.controller.admin.knowledge.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - OA 知识库评论新增/修改 Request VO")
@Data
public class OaKnowledgeCommentSaveReqVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "文章 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    @NotNull(message = "文章 ID 不能为空")
    private Long articleId;

    @Schema(description = "父评论 ID（0 为根评论）", example = "0")
    private Long parentId;

    @Schema(description = "评论内容", requiredMode = Schema.RequiredMode.REQUIRED, example = "写得很好")
    @NotEmpty(message = "评论内容不能为空")
    private String content;

    @Schema(description = "评论人 ID", example = "2048")
    private Long commentatorUserId;

    @Schema(description = "评论人姓名", example = "张三")
    private String commentatorName;

}
