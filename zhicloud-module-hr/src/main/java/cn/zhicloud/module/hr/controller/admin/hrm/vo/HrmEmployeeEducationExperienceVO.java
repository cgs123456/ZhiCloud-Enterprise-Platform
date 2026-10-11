package cn.zhicloud.module.hr.controller.admin.hrm.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - HRM 员工教育经历 VO（列表 / 新增 / 修改共用）")
@Data
public class HrmEmployeeEducationExperienceVO {

    @Schema(description = "教育经历编号")
    private Long id;

    @Schema(description = "员工编号")
    @NotNull(message = "员工编号不能为空")
    private Long employeeId;

    @Schema(description = "学历（1-12）")
    private Integer education;

    @Schema(description = "毕业院校")
    private String graduateSchool;

    @Schema(description = "专业")
    private String major;

    @Schema(description = "入学日期")
    private LocalDateTime admissionTime;

    @Schema(description = "毕业日期")
    private LocalDateTime graduationTime;

    @Schema(description = "教学方式")
    private Integer teachingMethods;

    @Schema(description = "是否第一学历")
    private Boolean firstDegree;

}
