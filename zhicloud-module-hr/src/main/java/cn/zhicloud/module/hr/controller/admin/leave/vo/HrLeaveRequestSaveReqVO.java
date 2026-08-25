package cn.zhicloud.module.hr.controller.admin.leave.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "管理后台 - HR 请假单创建 Request VO")
@Data
public class HrLeaveRequestSaveReqVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "员工 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    @NotNull(message = "员工不能为空")
    private Long employeeId;

    @Schema(description = "假期类型 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "假期类型不能为空")
    private Long leaveTypeId;

    @Schema(description = "开始日期", requiredMode = Schema.RequiredMode.REQUIRED, example = "2024-01-01")
    @NotNull(message = "开始日期不能为空")
    private LocalDate startDate;

    @Schema(description = "结束日期", requiredMode = Schema.RequiredMode.REQUIRED, example = "2024-01-03")
    @NotNull(message = "结束日期不能为空")
    private LocalDate endDate;

    @Schema(description = "请假天数", requiredMode = Schema.RequiredMode.REQUIRED, example = "3")
    @NotNull(message = "请假天数不能为空")
    private BigDecimal days;

    @Schema(description = "请假原因", example = "家中有事")
    private String reason;

}