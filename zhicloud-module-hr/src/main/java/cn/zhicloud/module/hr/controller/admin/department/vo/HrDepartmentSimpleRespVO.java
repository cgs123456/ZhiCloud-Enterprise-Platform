package cn.zhicloud.module.hr.controller.admin.department.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "管理后台 - HR 部门精简信息 Response VO")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HrDepartmentSimpleRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "部门名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "研发部")
    private String name;

    @Schema(description = "父部门 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    private Long parentId;

}