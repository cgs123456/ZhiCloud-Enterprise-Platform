package cn.zhicloud.module.qms.controller.admin.inspectionorder.vo;

import cn.zhicloud.framework.excel.core.annotations.DictFormat;
import cn.zhicloud.framework.excel.core.convert.DictConvert;
import cn.zhicloud.module.qms.enums.DictTypeConstants;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - QMS 检验单 Response VO")
@Data
@ExcelIgnoreUnannotated
public class InspectionOrderRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("编号")
    private Long id;

    @Schema(description = "检验单号", requiredMode = Schema.RequiredMode.REQUIRED, example = "IQC20240101001")
    @ExcelProperty("检验单号")
    private String orderNo;

    @Schema(description = "检验类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @ExcelProperty(value = "检验类型", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.INSPECTION_TYPE)
    private Integer type;

    @Schema(description = "供应商 ID", example = "1024")
    @ExcelProperty("供应商 ID")
    private Long supplierId;

    @Schema(description = "批次号", example = "BATCH001")
    @ExcelProperty("批次号")
    private String batchNo;

    @Schema(description = "工单 ID", example = "2048")
    @ExcelProperty("工单 ID")
    private Long workOrderId;

    @Schema(description = "产品 ID", example = "3072")
    @ExcelProperty("产品 ID")
    private Long productId;

    @Schema(description = "检验员", example = "芋头")
    @ExcelProperty("检验员")
    private String inspector;

    @Schema(description = "检验时间")
    @ExcelProperty("检验时间")
    private LocalDateTime inspectTime;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @ExcelProperty(value = "状态", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.INSPECTION_ORDER_STATUS)
    private Integer status;

    @Schema(description = "备注", example = "随便")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}
