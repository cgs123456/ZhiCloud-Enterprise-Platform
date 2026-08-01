package cn.iocoder.yudao.module.oa.controller.admin.knowledge.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - OA 知识库版本分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class OaKnowledgeVersionPageReqVO extends PageParam {

    @Schema(description = "文章 ID", example = "100")
    private Long articleId;

    @Schema(description = "编辑人 ID", example = "2048")
    private Long editorUserId;

}
