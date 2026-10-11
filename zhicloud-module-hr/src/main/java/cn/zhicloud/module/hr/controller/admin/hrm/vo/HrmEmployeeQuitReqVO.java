package cn.zhicloud.module.hr.controller.admin.hrm.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - HRM 员工离职 Request VO")
@Data
public class HrmEmployeeQuitReqVO {

    @Schema(description = "员工编号")
    @NotNull(message = "员工编号不能为空")
    private Long employeeId;

    @Schema(description = "计划离职时间")
    private LocalDateTime planQuitTime;

    @Schema(description = "申请离职时间")
    private LocalDateTime applyQuitTime;

    @Schema(description = "薪资结算时间")
    private LocalDateTime salarySettlementTime;

    @Schema(description = "离职类型：1主动 2被动 3退休")
    private Integer type;

    @Schema(description = "离职原因（1-16）")
    private Integer reason;

    @Schema(description = "备注")
    private String remark;

}
