package cn.zhicloud.module.hr.controller.admin.leave.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - HR 请假单 Response VO")
@Data
public class HrLeaveRequestRespVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "员工 ID", example = "2048")
    private Long employeeId;

    @Schema(description = "假期类型 ID", example = "1")
    private Long leaveTypeId;

    @Schema(description = "开始日期", example = "2024-01-01")
    private LocalDate startDate;

    @Schema(description = "结束日期", example = "2024-01-03")
    private LocalDate endDate;

    @Schema(description = "请假天数", example = "3")
    private BigDecimal days;

    @Schema(description = "请假原因", example = "家中有事")
    private String reason;

    @Schema(description = "状态", example = "0")
    private Integer status;

    @Schema(description = "审批人 ID", example = "1024")
    private Long approverId;

    @Schema(description = "审批时间")
    private LocalDateTime approveTime;

    @Schema(description = "审批备注", example = "同意")
    private String approveRemark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}