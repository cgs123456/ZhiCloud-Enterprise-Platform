package cn.iocoder.yudao.module.qms.controller.admin.document.vo;

import cn.iocoder.yudao.framework.common.validation.InEnum;
import cn.iocoder.yudao.module.qms.enums.document.QmsDocStatusEnum;
import cn.iocoder.yudao.module.qms.enums.document.QmsDocTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Schema(description = "管理后台 - QMS 受控文档新增/修改 Request VO")
@Data
public class QmsDocumentSaveReqVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "文件编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "QM-PR-001")
    @NotEmpty(message = "文件编号不能为空")
    private String docNo;

    @Schema(description = "标题", requiredMode = Schema.RequiredMode.REQUIRED, example = "质量手册")
    @NotEmpty(message = "标题不能为空")
    private String title;

    @Schema(description = "文件类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @NotNull(message = "文件类型不能为空")
    @InEnum(QmsDocTypeEnum.class)
    private Integer docType;

    @Schema(description = "版本号", example = "1.0")
    private String version;

    @Schema(description = "状态", example = "10")
    @InEnum(QmsDocStatusEnum.class)
    private Integer status;

    @Schema(description = "生效日期", example = "2024-01-01")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate effectiveDate;

    @Schema(description = "失效日期", example = "2025-12-31")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate expiryDate;

    @Schema(description = "审批人 ID", example = "1024")
    private Long approverId;

    @Schema(description = "归属部门 ID", example = "2048")
    private Long ownerDeptId;

    @Schema(description = "文件 URL", example = "https://example.com/doc.pdf")
    private String fileUrl;

    @Schema(description = "备注", example = "随便")
    private String remark;

    @Schema(description = "排序", example = "0")
    private Integer sort;

}
