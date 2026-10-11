package cn.zhicloud.module.hr.controller.admin.hrm.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - HRM 员工工作经历 VO（列表 / 新增 / 修改共用）")
@Data
public class HrmEmployeeWorkExperienceVO {

    @Schema(description = "工作经历编号")
    private Long id;

    @Schema(description = "员工编号")
    @NotNull(message = "员工编号不能为空")
    private Long employeeId;

    @Schema(description = "工作单位")
    private String workUnit;

    @Schema(description = "职务")
    private String postName;

    @Schema(description = "工作开始日期")
    private LocalDateTime startTime;

    @Schema(description = "工作结束日期")
    private LocalDateTime endTime;

    @Schema(description = "离职原因")
    private String reason;

    @Schema(description = "证明人")
    private String witnessName;

    @Schema(description = "证明人手机号")
    private String witnessPhone;

    @Schema(description = "工作备注")
    private String remark;

}
