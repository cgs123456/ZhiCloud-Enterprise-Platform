package cn.zhicloud.module.oa.controller.admin.knowledge.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - OA 知识库分类 Response VO")
@Data
public class OaKnowledgeCategoryRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "父分类 ID（0 为根）", example = "0")
    private Long parentId;

    @Schema(description = "分类名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "技术文档")
    private String name;

    @Schema(description = "排序", example = "0")
    private Integer sort;

    @Schema(description = "状态（0 启用 1 停用）", example = "0")
    private Integer status;

    @Schema(description = "备注", example = "随便")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

}
