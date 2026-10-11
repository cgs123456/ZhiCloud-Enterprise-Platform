package cn.zhicloud.module.hr.controller.admin.hrm.recruit.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - HRM 招聘职位新增/修改 Request VO")
@Data
public class HrmRecruitPostSaveReqVO {

    @Schema(description = "招聘职位编号")
    private Long id;

    @Schema(description = "职位名称")
    @NotEmpty(message = "职位名称不能为空")
    private String postName;

    @Schema(description = "用人部门编号")
    private Long deptId;

    @Schema(description = "工作性质")
    private Integer jobNature;

    @Schema(description = "工作城市地区编号")
    private Long areaId;

    @Schema(description = "招聘人数")
    private Integer recruitNum;

    @Schema(description = "招聘原因")
    private String reason;

    @Schema(description = "工作经验要求")
    private Integer workTime;

    @Schema(description = "学历要求")
    private Integer educationRequire;

    @Schema(description = "最低薪资（-1 面议）")
    private BigDecimal minSalary;

    @Schema(description = "最高薪资（-1 面议）")
    private BigDecimal maxSalary;

    @Schema(description = "薪资单位（-1 面议）")
    private Integer salaryUnit;

    @Schema(description = "最小年龄（-1 不限）")
    private Integer minAge;

    @Schema(description = "最大年龄（-1 不限）")
    private Integer maxAge;

    @Schema(description = "最迟到岗时间")
    private LocalDateTime latestEntryTime;

    @Schema(description = "招聘负责人员工编号")
    private Long ownerEmployeeId;

    @Schema(description = "面试官员工编号数组")
    private List<Long> interviewEmployeeIds;

    @Schema(description = "职位描述")
    private String description;

    @Schema(description = "紧急程度")
    private Integer emergencyLevel;

    @Schema(description = "职位类型编号")
    private Long postTypeId;

    @Schema(description = "职位状态")
    private Integer status;

    @Schema(description = "停止原因")
    private String stopReason;

}
