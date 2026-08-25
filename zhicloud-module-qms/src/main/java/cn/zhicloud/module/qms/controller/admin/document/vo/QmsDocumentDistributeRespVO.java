package cn.zhicloud.module.qms.controller.admin.document.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - QMS 文档分发记录 Response VO")
@Data
@ExcelIgnoreUnannotated
public class QmsDocumentDistributeRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("编号")
    private Long id;

    @Schema(description = "受控文档 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("受控文档 ID")
    private Long documentId;

    @Schema(description = "分发对象", requiredMode = Schema.RequiredMode.REQUIRED, example = "质量部")
    @ExcelProperty("分发对象")
    private String distributeTo;

    @Schema(description = "分发份数", requiredMode = Schema.RequiredMode.REQUIRED, example = "5")
    @ExcelProperty("分发份数")
    private Integer distributeQty;

    @Schema(description = "分发日期", requiredMode = Schema.RequiredMode.REQUIRED, example = "2024-01-01")
    @ExcelProperty("分发日期")
    private LocalDate distributeDate;

    @Schema(description = "签收人", example = "张三")
    @ExcelProperty("签收人")
    private String receivedBy;

    @Schema(description = "签收日期", example = "2024-01-02")
    @ExcelProperty("签收日期")
    private LocalDate receivedDate;

    @Schema(description = "回收份数", example = "3")
    @ExcelProperty("回收份数")
    private Integer returnedQty;

    @Schema(description = "回收日期", example = "2025-01-01")
    @ExcelProperty("回收日期")
    private LocalDate returnedDate;

    @Schema(description = "备注", example = "随便")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "排序", example = "0")
    private Integer sort;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}
