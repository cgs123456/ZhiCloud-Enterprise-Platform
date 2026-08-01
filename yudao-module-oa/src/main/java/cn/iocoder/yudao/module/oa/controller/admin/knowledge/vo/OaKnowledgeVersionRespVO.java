package cn.iocoder.yudao.module.oa.controller.admin.knowledge.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - OA 知识库版本 Response VO")
@Data
public class OaKnowledgeVersionRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "文章 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    private Long articleId;

    @Schema(description = "版本号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer versionNo;

    @Schema(description = "该版本标题", example = "Spring Boot 入门指南")
    private String title;

    @Schema(description = "该版本正文", example = "正文内容...")
    private String content;

    @Schema(description = "该版本摘要", example = "摘要")
    private String summary;

    @Schema(description = "变更说明", example = "修正错别字")
    private String changeLog;

    @Schema(description = "编辑人 ID", example = "2048")
    private Long editorUserId;

    @Schema(description = "编辑人姓名", example = "张三")
    private String editorName;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

}
