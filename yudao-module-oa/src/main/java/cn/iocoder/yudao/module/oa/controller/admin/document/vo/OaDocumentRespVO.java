package cn.iocoder.yudao.module.oa.controller.admin.document.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - OA 公文 Response VO")
@Data
@ExcelIgnoreUnannotated
public class OaDocumentRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("编号")
    private Long id;

    @Schema(description = "公文编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "GW20240101001")
    @ExcelProperty("公文编号")
    private String no;

    @Schema(description = "标题", requiredMode = Schema.RequiredMode.REQUIRED, example = "关于季度复盘的通知")
    @ExcelProperty("标题")
    private String title;

    @Schema(description = "公文类型（10 通知 20 通报 30 报告 40 请示 50 决定 60 批复）", example = "10")
    @ExcelProperty("公文类型")
    private Integer documentType;

    @Schema(description = "紧急程度（10 普通 20 紧急 30 特急）", example = "10")
    @ExcelProperty("紧急程度")
    private Integer urgency;

    @Schema(description = "保密级别（10 公开 20 内部 30 秘密）", example = "10")
    @ExcelProperty("保密级别")
    private Integer confidentiality;

    @Schema(description = "发文人 ID", example = "2048")
    @ExcelProperty("发文人 ID")
    private Long issuerUserId;

    @Schema(description = "发文部门 ID", example = "100")
    @ExcelProperty("发文部门 ID")
    private Long issueDeptId;

    @Schema(description = "发文日期", example = "2024-01-01")
    @ExcelProperty("发文日期")
    private LocalDate issueDate;

    @Schema(description = "正文内容", example = "正文...")
    @ExcelProperty("正文内容")
    private String content;

    @Schema(description = "工作流编号", example = "12345")
    @ExcelProperty("工作流编号")
    private String processInstanceId;

    @Schema(description = "状态（10 草稿 20 审核中 30 已发布 40 已废止）", example = "10")
    @ExcelProperty("状态")
    private Integer status;

    @Schema(description = "备注", example = "随便")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "核稿人 ID", example = "2049")
    @ExcelProperty("核稿人 ID")
    private Long reviewerUserId;

    @Schema(description = "核稿人姓名", example = "张三")
    @ExcelProperty("核稿人姓名")
    private String reviewerName;

    @Schema(description = "核稿时间")
    @ExcelProperty("核稿时间")
    private LocalDateTime reviewTime;

    @Schema(description = "核稿意见", example = "同意")
    @ExcelProperty("核稿意见")
    private String reviewOpinion;

    @Schema(description = "签发人 ID", example = "2050")
    @ExcelProperty("签发人 ID")
    private Long signerUserId;

    @Schema(description = "签发人姓名", example = "李四")
    @ExcelProperty("签发人姓名")
    private String signerName;

    @Schema(description = "签发时间")
    @ExcelProperty("签发时间")
    private LocalDateTime signTime;

    @Schema(description = "签发意见", example = "准予发布")
    @ExcelProperty("签发意见")
    private String signOpinion;

    @Schema(description = "归档人 ID", example = "2051")
    @ExcelProperty("归档人 ID")
    private Long archiverUserId;

    @Schema(description = "归档人姓名", example = "王五")
    @ExcelProperty("归档人姓名")
    private String archiverName;

    @Schema(description = "归档时间")
    @ExcelProperty("归档时间")
    private LocalDateTime archiveTime;

    @Schema(description = "归档编号", example = "ARC20240101001")
    @ExcelProperty("归档编号")
    private String archiveNo;

    @Schema(description = "主送部门（逗号分隔 ID）", example = "100,101")
    @ExcelProperty("主送部门")
    private String mainSendDepts;

    @Schema(description = "抄送部门（逗号分隔 ID）", example = "102,103")
    @ExcelProperty("抄送部门")
    private String copySendDepts;

    @Schema(description = "阅读量", example = "0")
    @ExcelProperty("阅读量")
    private Integer readCount;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

    @Schema(description = "公文附件列表")
    private List<OaDocumentAttachmentVO> attachments;

}
