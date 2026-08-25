package cn.zhicloud.module.hr.controller.admin.recruitment.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - HR 简历 Response VO")
@Data
public class HrResumeRespVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "招聘职位 ID", example = "2048")
    private Long jobPostingId;

    @Schema(description = "候选人姓名", example = "张三")
    private String candidateName;

    @Schema(description = "电话", example = "13800138000")
    private String phone;

    @Schema(description = "邮箱", example = "zhangsan@zhicloud.cn")
    private String email;

    @Schema(description = "学历", example = "本科")
    private String education;

    @Schema(description = "工作年限", example = "5")
    private Integer experienceYears;

    @Schema(description = "简历附件", example = "https://www.zhicloud.cn/resume.pdf")
    private String resumeUrl;

    @Schema(description = "状态", example = "0")
    private Integer status;

    @Schema(description = "备注", example = "随便")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}