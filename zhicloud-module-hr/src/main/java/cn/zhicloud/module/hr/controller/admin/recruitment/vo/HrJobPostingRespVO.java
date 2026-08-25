package cn.zhicloud.module.hr.controller.admin.recruitment.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - HR 招聘职位 Response VO")
@Data
public class HrJobPostingRespVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "岗位 ID", example = "4096")
    private Long positionId;

    @Schema(description = "招聘标题", example = "Java 高级工程师")
    private String title;

    @Schema(description = "招聘人数", example = "2")
    private Integer headcount;

    @Schema(description = "薪资范围", example = "15k-25k")
    private String salaryRange;

    @Schema(description = "职位描述", example = "负责后端开发")
    private String description;

    @Schema(description = "任职要求", example = "3 年以上经验")
    private String requirement;

    @Schema(description = "状态", example = "0")
    private Integer status;

    @Schema(description = "发布日期", example = "2024-01-01")
    private LocalDate publishDate;

    @Schema(description = "关闭日期", example = "2024-03-31")
    private LocalDate closeDate;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}