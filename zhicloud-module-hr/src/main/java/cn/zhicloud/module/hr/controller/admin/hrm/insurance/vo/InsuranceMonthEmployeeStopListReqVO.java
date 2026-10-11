package cn.zhicloud.module.hr.controller.admin.hrm.insurance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.util.List;

@Schema(description = "管理后台 - HRM 员工停止参保 Request VO")
@Data
public class InsuranceMonthEmployeeStopListReqVO {

    @Schema(description = "员工月度社保记录编号数组")
    @NotEmpty(message = "记录编号不能为空")
    private List<Long> ids;

}
