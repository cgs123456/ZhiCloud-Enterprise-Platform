package cn.iocoder.yudao.module.hr.controller.admin.recruitment.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Schema(description = "管理后台 - HR 招聘职位新增/修改 Request VO")
@Data
public class HrJobPostingSaveReqVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "岗位 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "4096")
    @NotNull(message = "岗位不能为空")
    private Long positionId;

    @Schema(description = "招聘标题", requiredMode = Schema.RequiredMode.REQUIRED, example = "Java 高级工程师")
    @NotEmpty(message = "招聘标题不能为空")
    private String title;

    @Schema(description = "招聘人数", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @NotNull(message = "招聘人数不能为空")
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

}