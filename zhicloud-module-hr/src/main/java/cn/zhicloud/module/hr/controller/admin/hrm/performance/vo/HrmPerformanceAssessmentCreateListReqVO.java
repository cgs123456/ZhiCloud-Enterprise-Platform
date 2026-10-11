package cn.zhicloud.module.hr.controller.admin.hrm.performance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.List;

@Schema(description = "管理后台 - HRM 员工绩效考核批量新增 Request VO")
@Data
public class HrmPerformanceAssessmentCreateListReqVO {

    @Schema(description = "绩效计划编号")
    @NotNull(message = "计划编号不能为空")
    private Long planId;

    @Schema(description = "员工编号列表")
    @NotEmpty(message = "员工不能为空")
    private List<Long> employeeIds;

}
