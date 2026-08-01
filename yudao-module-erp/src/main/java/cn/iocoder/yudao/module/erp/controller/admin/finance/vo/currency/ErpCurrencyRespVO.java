package cn.iocoder.yudao.module.erp.controller.admin.finance.vo.currency;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - ERP 币种 Response VO")
@Data
@ExcelIgnoreUnannotated
public class ErpCurrencyRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("编号")
    private Long id;

    @Schema(description = "币种编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "CNY")
    @ExcelProperty("币种编码")
    private String code;

    @Schema(description = "币种名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "人民币")
    @ExcelProperty("币种名称")
    private String name;

    @Schema(description = "币种符号", example = "¥")
    @ExcelProperty("币种符号")
    private String symbol;

    @Schema(description = "是否本位币", example = "true")
    @ExcelProperty("是否本位币")
    private Boolean isBase;

    @Schema(description = "是否启用", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @ExcelProperty("是否启用")
    private Integer enabled;

    @Schema(description = "备注", example = "本位币")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}
