package cn.zhicloud.module.oa.controller.admin.document.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "管理后台 - OA 公文新增/修改 Request VO")
@Data
public class OaDocumentSaveReqVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "公文编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "GW20240101001")
    @NotEmpty(message = "公文编号不能为空")
    private String no;

    @Schema(description = "标题", requiredMode = Schema.RequiredMode.REQUIRED, example = "关于季度复盘的通知")
    @NotEmpty(message = "标题不能为空")
    private String title;

    @Schema(description = "公文类型（10 通知 20 通报 30 报告 40 请示 50 决定 60 批复）", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @NotNull(message = "公文类型不能为空")
    private Integer documentType;

    @Schema(description = "紧急程度（10 普通 20 紧急 30 特急）", example = "10")
    private Integer urgency;

    @Schema(description = "保密级别（10 公开 20 内部 30 秘密）", example = "10")
    private Integer confidentiality;

    @Schema(description = "发文人 ID", example = "2048")
    private Long issuerUserId;

    @Schema(description = "发文部门 ID", example = "100")
    private Long issueDeptId;

    @Schema(description = "发文日期", example = "2024-01-01")
    private LocalDate issueDate;

    @Schema(description = "正文内容", example = "正文...")
    private String content;

    @Schema(description = "工作流编号", example = "12345")
    private String processInstanceId;

    @Schema(description = "状态（10 草稿 20 审核中 30 已发布 40 已废止）", example = "10")
    private Integer status;

    @Schema(description = "备注", example = "随便")
    private String remark;

    @Schema(description = "核稿人 ID", example = "2049")
    private Long reviewerUserId;

    @Schema(description = "签发人 ID", example = "2050")
    private Long signerUserId;

    @Schema(description = "归档人 ID", example = "2051")
    private Long archiverUserId;

    @Schema(description = "归档编号", example = "ARC20240101001")
    private String archiveNo;

    @Schema(description = "主送部门（逗号分隔 ID）", example = "100,101")
    private String mainSendDepts;

    @Schema(description = "抄送部门（逗号分隔 ID）", example = "102,103")
    private String copySendDepts;

    @Schema(description = "公文附件列表")
    @Valid
    private List<OaDocumentAttachmentVO> attachments;

}
