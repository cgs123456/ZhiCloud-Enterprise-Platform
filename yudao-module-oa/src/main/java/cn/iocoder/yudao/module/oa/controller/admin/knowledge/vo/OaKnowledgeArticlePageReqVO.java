package cn.iocoder.yudao.module.oa.controller.admin.knowledge.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - OA 知识库文章分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class OaKnowledgeArticlePageReqVO extends PageParam {

    @Schema(description = "分类 ID", example = "100")
    private Long categoryId;

    @Schema(description = "标题", example = "Spring Boot")
    private String title;

    @Schema(description = "标签", example = "Java")
    private String tags;

    @Schema(description = "状态（10 草稿 20 已发布 30 已下架）", example = "20")
    private Integer status;

    @Schema(description = "作者 ID", example = "2048")
    private Long authorUserId;

    @Schema(description = "是否置顶", example = "false")
    private Boolean topFlag;

}
