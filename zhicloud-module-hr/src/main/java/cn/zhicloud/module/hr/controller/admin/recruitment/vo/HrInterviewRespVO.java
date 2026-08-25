package cn.zhicloud.module.hr.controller.admin.recruitment.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - HR 面试 Response VO")
@Data
public class HrInterviewRespVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "简历 ID", example = "2048")
    private Long resumeId;

    @Schema(description = "面试轮次", example = "1")
    private Integer interviewRound;

    @Schema(description = "面试官 ID", example = "1024")
    private Long interviewerId;

    @Schema(description = "面试时间", example = "2024-01-01T10:00:00")
    private LocalDateTime interviewTime;

    @Schema(description = "面试类型", example = "1")
    private Integer interviewType;

    @Schema(description = "结果", example = "1")
    private Integer result;

    @Schema(description = "面试评价", example = "表现优秀")
    private String comment;

    @Schema(description = "状态", example = "0")
    private Integer status;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}