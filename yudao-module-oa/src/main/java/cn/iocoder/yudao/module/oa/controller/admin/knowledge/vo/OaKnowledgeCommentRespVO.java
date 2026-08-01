package cn.iocoder.yudao.module.oa.controller.admin.knowledge.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - OA 知识库评论 Response VO")
@Data
public class OaKnowledgeCommentRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "文章 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    private Long articleId;

    @Schema(description = "父评论 ID（0 为根评论）", example = "0")
    private Long parentId;

    @Schema(description = "评论内容", requiredMode = Schema.RequiredMode.REQUIRED, example = "写得很好")
    private String content;

    @Schema(description = "评论人 ID", example = "2048")
    private Long commentatorUserId;

    @Schema(description = "评论人姓名", example = "张三")
    private String commentatorName;

    @Schema(description = "点赞数", example = "0")
    private Integer likeCount;

    @Schema(description = "状态（0 正常 1 已删除）", example = "0")
    private Integer status;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

    @Schema(description = "子评论列表")
    private List<OaKnowledgeCommentRespVO> children;

}
