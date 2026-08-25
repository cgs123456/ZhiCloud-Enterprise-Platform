package cn.zhicloud.module.hr.controller.admin.leave.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - HR 假期余额 Response VO")
@Data
public class HrLeaveBalanceRespVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "员工 ID", example = "2048")
    private Long employeeId;

    @Schema(description = "假期类型 ID", example = "1")
    private Long leaveTypeId;

    @Schema(description = "年份", example = "2024")
    private Integer year;

    @Schema(description = "年度总额度", example = "10")
    private BigDecimal totalDays;

    @Schema(description = "已用天数", example = "3")
    private BigDecimal usedDays;

    @Schema(description = "剩余天数", example = "7")
    private BigDecimal remainingDays;

}