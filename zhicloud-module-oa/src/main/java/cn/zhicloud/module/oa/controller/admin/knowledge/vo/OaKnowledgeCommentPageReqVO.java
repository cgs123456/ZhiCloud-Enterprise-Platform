package cn.zhicloud.module.oa.controller.admin.knowledge.vo;

import cn.zhicloud.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - OA 知识库评论分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class OaKnowledgeCommentPageReqVO extends PageParam {

    @Schema(description = "文章 ID", example = "100")
    private Long articleId;

    @Schema(description = "父评论 ID（0 为根评论）", example = "0")
    private Long parentId;

    @Schema(description = "状态", example = "0")
    private Integer status;

    @Schema(description = "评论人 ID", example = "2048")
    private Long commentatorUserId;

}
