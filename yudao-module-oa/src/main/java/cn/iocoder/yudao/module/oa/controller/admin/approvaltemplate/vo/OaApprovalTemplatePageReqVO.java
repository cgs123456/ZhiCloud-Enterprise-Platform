package cn.iocoder.yudao.module.oa.controller.admin.approvaltemplate.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - OA 审批模板分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class OaApprovalTemplatePageReqVO extends PageParam {

    @Schema(description = "模板编码", example = "LEAVE")
    private String code;

    @Schema(description = "模板名称", example = "请假审批")
    private String name;

    @Schema(description = "分类", example = "人事")
    private String category;

    @Schema(description = "状态（0 启用 1 停用）", example = "0")
    private Integer status;

}
