package cn.zhicloud.module.qms.controller.admin.document.vo;

import cn.zhicloud.framework.excel.core.annotations.DictFormat;
import cn.zhicloud.framework.excel.core.convert.DictConvert;
import cn.zhicloud.module.qms.enums.DictTypeConstants;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - QMS 受控文档 Response VO")
@Data
@ExcelIgnoreUnannotated
public class QmsDocumentRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("编号")
    private Long id;

    @Schema(description = "文件编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "QM-PR-001")
    @ExcelProperty("文件编号")
    private String docNo;

    @Schema(description = "标题", requiredMode = Schema.RequiredMode.REQUIRED, example = "质量手册")
    @ExcelProperty("标题")
    private String title;

    @Schema(description = "文件类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @ExcelProperty(value = "文件类型", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.DOC_TYPE)
    private Integer docType;

    @Schema(description = "版本号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1.0")
    @ExcelProperty("版本号")
    private String version;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @ExcelProperty(value = "状态", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.DOC_STATUS)
    private Integer status;

    @Schema(description = "生效日期", example = "2024-01-01")
    @ExcelProperty("生效日期")
    private LocalDate effectiveDate;

    @Schema(description = "失效日期", example = "2025-12-31")
    @ExcelProperty("失效日期")
    private LocalDate expiryDate;

    @Schema(description = "审批人 ID", example = "1024")
    private Long approverId;

    @Schema(description = "审批日期")
    @ExcelProperty("审批日期")
    private LocalDateTime approveDate;

    @Schema(description = "归属部门 ID", example = "2048")
    private Long ownerDeptId;

    @Schema(description = "文件 URL", example = "https://example.com/doc.pdf")
    private String fileUrl;

    @Schema(description = "备注", example = "随便")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "排序", example = "0")
    private Integer sort;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}
