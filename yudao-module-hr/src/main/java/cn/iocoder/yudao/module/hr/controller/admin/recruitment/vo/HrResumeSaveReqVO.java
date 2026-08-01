package cn.iocoder.yudao.module.hr.controller.admin.recruitment.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - HR 简历新增/修改 Request VO")
@Data
public class HrResumeSaveReqVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "招聘职位 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    @NotNull(message = "招聘职位不能为空")
    private Long jobPostingId;

    @Schema(description = "候选人姓名", requiredMode = Schema.RequiredMode.REQUIRED, example = "张三")
    @NotEmpty(message = "候选人姓名不能为空")
    private String candidateName;

    @Schema(description = "电话", example = "13800138000")
    private String phone;

    @Schema(description = "邮箱", example = "zhangsan@iocoder.cn")
    private String email;

    @Schema(description = "学历", example = "本科")
    private String education;

    @Schema(description = "工作年限", example = "5")
    private Integer experienceYears;

    @Schema(description = "简历附件", example = "https://www.iocoder.cn/resume.pdf")
    private String resumeUrl;

    @Schema(description = "备注", example = "随便")
    private String remark;

}