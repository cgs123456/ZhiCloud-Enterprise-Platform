package cn.zhicloud.module.hr.controller.admin.employee.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - HR 员工调动 Request VO")
@Data
public class HrEmployeeTransferReqVO {

    @Schema(description = "员工编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "员工编号不能为空")
    private Long id;

    @Schema(description = "新部门 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    @NotNull(message = "新部门不能为空")
    private Long deptId;

    @Schema(description = "新职位 ID", example = "4096")
    private Long positionId;

}