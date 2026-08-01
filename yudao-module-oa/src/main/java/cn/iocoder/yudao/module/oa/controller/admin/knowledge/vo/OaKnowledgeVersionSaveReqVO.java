package cn.iocoder.yudao.module.oa.controller.admin.knowledge.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - OA 知识库版本新增/修改 Request VO")
@Data
public class OaKnowledgeVersionSaveReqVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "文章 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    @NotNull(message = "文章 ID 不能为空")
    private Long articleId;

    @Schema(description = "版本号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "版本号不能为空")
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

}
