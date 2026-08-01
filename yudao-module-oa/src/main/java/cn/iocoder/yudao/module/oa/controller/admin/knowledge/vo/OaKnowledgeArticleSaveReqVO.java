package cn.iocoder.yudao.module.oa.controller.admin.knowledge.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - OA 知识库文章新增/修改 Request VO")
@Data
public class OaKnowledgeArticleSaveReqVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "分类 ID", example = "100")
    private Long categoryId;

    @Schema(description = "标题", requiredMode = Schema.RequiredMode.REQUIRED, example = "Spring Boot 入门指南")
    @NotEmpty(message = "标题不能为空")
    private String title;

    @Schema(description = "摘要", example = "本文介绍 Spring Boot 基础")
    private String summary;

    @Schema(description = "正文（Markdown/HTML）", example = "正文内容...")
    private String content;

    @Schema(description = "标签（逗号分隔）", example = "Java,Spring")
    private String tags;

    @Schema(description = "作者 ID", example = "2048")
    private Long authorUserId;

    @Schema(description = "作者姓名", example = "张三")
    private String authorName;

    @Schema(description = "状态（10 草稿 20 已发布 30 已下架）", example = "10")
    private Integer status;

    @Schema(description = "是否置顶", example = "false")
    private Boolean topFlag;

    @Schema(description = "备注", example = "随便")
    private String remark;

    @Schema(description = "变更说明（更新时记录到版本）", example = "修正错别字")
    private String changeLog;

}
