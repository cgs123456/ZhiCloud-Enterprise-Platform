package cn.zhicloud.module.hr.controller.admin.hrm.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - HRM 员工培训经历 VO（列表 / 新增 / 修改共用）")
@Data
public class HrmEmployeeTrainingExperienceVO {

    @Schema(description = "培训经历编号")
    private Long id;

    @Schema(description = "员工编号")
    @NotNull(message = "员工编号不能为空")
    private Long employeeId;

    @Schema(description = "培训课程")
    private String course;

    @Schema(description = "培训机构名称")
    private String organizationName;

    @Schema(description = "培训开始日期")
    private LocalDateTime startTime;

    @Schema(description = "培训结束日期")
    private LocalDateTime endTime;

    @Schema(description = "培训时长")
    private String duration;

    @Schema(description = "培训成绩")
    private String result;

    @Schema(description = "培训证书名称")
    private String certificateName;

    @Schema(description = "备注")
    private String remark;

}
