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

@Schema(description = "管理后台 - QMS 文件变更申请 Response VO")
@Data
@ExcelIgnoreUnannotated
public class QmsDocumentChangeRequestRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("编号")
    private Long id;

    @Schema(description = "受控文档 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("受控文档 ID")
    private Long documentId;

    @Schema(description = "变更类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "20")
    @ExcelProperty(value = "变更类型", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.DOC_CHANGE_TYPE)
    private Integer changeType;

    @Schema(description = "变更原因", requiredMode = Schema.RequiredMode.REQUIRED, example = "更新流程以适配新规")
    @ExcelProperty("变更原因")
    private String changeReason;

    @Schema(description = "变更内容", example = "第3.2章节流程调整")
    @ExcelProperty("变更内容")
    private String changeContent;

    @Schema(description = "申请人 ID", example = "1024")
    private Long applicantId;

    @Schema(description = "申请日期", example = "2024-01-01")
    @ExcelProperty("申请日期")
    private LocalDate applyDate;

    @Schema(description = "审批人 ID", example = "2048")
    private Long approverId;

    @Schema(description = "审批日期")
    @ExcelProperty("审批日期")
    private LocalDateTime approveDate;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @ExcelProperty(value = "状态", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.CHANGE_REQUEST_STATUS)
    private Integer status;

    @Schema(description = "备注", example = "随便")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "排序", example = "0")
    private Integer sort;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}
