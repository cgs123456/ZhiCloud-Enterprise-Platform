package cn.iocoder.yudao.module.qms.controller.admin.ncr.vo;

import cn.iocoder.yudao.framework.excel.core.annotations.DictFormat;
import cn.iocoder.yudao.framework.excel.core.convert.DictConvert;
import cn.iocoder.yudao.module.qms.enums.DictTypeConstants;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - QMS 不合格品报告 Response VO")
@Data
@ExcelIgnoreUnannotated
public class NcrDocumentRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("编号")
    private Long id;

    @Schema(description = "NCR 单号", requiredMode = Schema.RequiredMode.REQUIRED, example = "NCR20240101001")
    @ExcelProperty("NCR 单号")
    private String ncrNo;

    @Schema(description = "来源", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @ExcelProperty(value = "来源", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.NCR_SOURCE)
    private Integer source;

    @Schema(description = "检验单 ID", example = "2048")
    private Long inspectionOrderId;

    @Schema(description = "产品 ID", example = "1024")
    private Long productId;

    @Schema(description = "供应商 ID", example = "2048")
    private Long supplierId;

    @Schema(description = "工单 ID", example = "3072")
    private Long workOrderId;

    @Schema(description = "缺陷描述", requiredMode = Schema.RequiredMode.REQUIRED, example = "外观划痕")
    @ExcelProperty("缺陷描述")
    private String defectDescription;

    @Schema(description = "缺陷等级", example = "20")
    @ExcelProperty(value = "缺陷等级", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.NCR_DEFECT_LEVEL)
    private Integer defectLevel;

    @Schema(description = "不合格数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "100")
    @ExcelProperty("不合格数量")
    private BigDecimal quantity;

    @Schema(description = "处置方式", example = "10")
    @ExcelProperty(value = "处置方式", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.NCR_DISPOSITION)
    private Integer disposition;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @ExcelProperty(value = "状态", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.NCR_STATUS)
    private Integer status;

    @Schema(description = "备注", example = "随便")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}
