package cn.zhicloud.module.oa.controller.admin.knowledge.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - OA 知识库文章 Response VO")
@Data
public class OaKnowledgeArticleRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "分类 ID", example = "100")
    private Long categoryId;

    @Schema(description = "标题", requiredMode = Schema.RequiredMode.REQUIRED, example = "Spring Boot 入门指南")
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

    @Schema(description = "阅读量", example = "0")
    private Integer viewCount;

    @Schema(description = "点赞数", example = "0")
    private Integer likeCount;

    @Schema(description = "评论数", example = "0")
    private Integer commentCount;

    @Schema(description = "当前版本号", example = "1")
    private Integer currentVersion;

    @Schema(description = "状态（10 草稿 20 已发布 30 已下架）", example = "10")
    private Integer status;

    @Schema(description = "是否置顶", example = "false")
    private Boolean topFlag;

    @Schema(description = "备注", example = "随便")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

}
