package cn.zhicloud.module.hr.controller.admin.hrm.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - HRM 员工离职信息 Response VO")
@Data
public class HrmEmployeeQuitInfoRespVO {

    @Schema(description = "离职信息编号")
    private Long id;

    @Schema(description = "员工编号")
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

    @Schema(description = "原员工状态")
    private Integer oldEmployeeStatus;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
