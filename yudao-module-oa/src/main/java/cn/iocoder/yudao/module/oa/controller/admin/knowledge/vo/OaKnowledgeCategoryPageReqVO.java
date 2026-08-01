package cn.iocoder.yudao.module.oa.controller.admin.knowledge.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - OA 知识库分类分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class OaKnowledgeCategoryPageReqVO extends PageParam {

    @Schema(description = "父分类 ID", example = "0")
    private Long parentId;

    @Schema(description = "分类名称", example = "技术文档")
    private String name;

    @Schema(description = "状态", example = "0")
    private Integer status;

}
