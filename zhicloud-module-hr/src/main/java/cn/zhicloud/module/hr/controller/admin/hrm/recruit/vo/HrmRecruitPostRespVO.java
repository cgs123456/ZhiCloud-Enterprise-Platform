package cn.zhicloud.module.hr.controller.admin.hrm.recruit.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - HRM 招聘职位 Response VO")
@Data
public class HrmRecruitPostRespVO {

    @Schema(description = "招聘职位编号")
    private Long id;

    @Schema(description = "职位名称")
    private String postName;

    @Schema(description = "用人部门编号")
    private Long deptId;

    @Schema(description = "用人部门名称")
    private String deptName;

    @Schema(description = "工作性质：1全职 2实习 3兼职")
    private Integer jobNature;

    @Schema(description = "工作城市地区编号")
    private Long areaId;

    @Schema(description = "工作城市地区名称")
    private String areaName;

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

    @Schema(description = "招聘负责人姓名")
    private String ownerEmployeeName;

    @Schema(description = "面试官员工编号数组")
    private List<Long> interviewEmployeeIds;

    @Schema(description = "面试官姓名数组")
    private List<String> interviewEmployeeNames;

    @Schema(description = "职位描述")
    private String description;

    @Schema(description = "紧急程度")
    private Integer emergencyLevel;

    @Schema(description = "职位类型编号")
    private Long postTypeId;

    @Schema(description = "职位类型名称")
    private String postTypeName;

    @Schema(description = "职位状态：0停止招聘 1招聘中")
    private Integer status;

    @Schema(description = "停止原因")
    private String stopReason;

    @Schema(description = "已入职人数")
    private Long hasEntryNum;

    @Schema(description = "招聘进度百分比")
    private Integer recruitSchedule;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
