package cn.zhicloud.module.hr.controller.admin.hrm.insurance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.List;

@Schema(description = "管理后台 - HRM 添加参保人员 Request VO")
@Data
public class InsuranceMonthEmployeeCreateListReqVO {

    @Schema(description = "月度社保表编号")
    @NotNull(message = "月度社保表编号不能为空")
    private Long monthRecordId;

    @Schema(description = "员工编号数组")
    @NotEmpty(message = "员工不能为空")
    private List<Long> employeeIds;

}
