package cn.zhicloud.module.hr.controller.admin.hrm.insurance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.List;

@Schema(description = "管理后台 - HRM 员工月度社保修改 Request VO")
@Data
public class InsuranceMonthEmployeeUpdateReqVO {

    @Schema(description = "员工月度社保记录编号")
    @NotNull(message = "记录编号不能为空")
    private Long id;

    @Schema(description = "社保方案编号")
    private Long schemeId;

    @Schema(description = "社保项目数组")
    private List<InsuranceMonthEmployeeProjectVO> projects;

}
