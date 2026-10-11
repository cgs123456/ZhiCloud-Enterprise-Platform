package cn.zhicloud.module.hr.controller.admin.hrm.salary.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - HRM 薪资组 VO（新增/修改/响应共用）")
@Data
public class HrmSalaryGroupVO {

    @Schema(description = "薪资组编号")
    private Long id;

    @Schema(description = "薪资组名称")
    @NotEmpty(message = "薪资组名称不能为空")
    private String name;

    @Schema(description = "月计薪标准")
    private Integer salaryStandard;

    @Schema(description = "转正、调薪月规则")
    private String changeRule;

    @Schema(description = "计税规则编号")
    private Long taxRuleId;

    @Schema(description = "计税规则名称（enrich）")
    private String taxRuleName;

    @Schema(description = "适用部门编号列表")
    private List<Long> deptIds;

    @Schema(description = "适用部门名称列表（enrich）")
    private List<String> deptNames;

    @Schema(description = "适用员工编号列表")
    private List<Long> employeeIds;

    @Schema(description = "适用员工名称列表（enrich）")
    private List<String> employeeNames;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
