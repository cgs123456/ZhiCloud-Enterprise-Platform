package cn.zhicloud.module.hr.controller.admin.hrm.salary.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - HRM 工资条发放记录 VO")
@Data
public class HrmSalarySlipSendRecordVO {

    @Schema(description = "工资条发放记录编号")
    private Long id;

    @Schema(description = "月度工资表编号")
    private Long monthRecordId;

    @Schema(description = "工资条数量")
    private Integer employeeCount;

    @Schema(description = "发放数量")
    private Integer sendEmployeeCount;

    @Schema(description = "已查看数量")
    private Integer readCount;

    @Schema(description = "年份")
    private Integer year;

    @Schema(description = "月份")
    private Integer month;

    @Schema(description = "创建人编号")
    private String creator;

    @Schema(description = "创建人姓名")
    private String creatorName;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
