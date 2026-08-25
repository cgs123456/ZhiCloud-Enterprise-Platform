package cn.zhicloud.module.erp.controller.admin.finance.vo.accountbook;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - ERP 账簿 Response VO")
@Data
@ExcelIgnoreUnannotated
public class ErpAccountBookRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("编号")
    private Long id;

    @Schema(description = "账簿编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "BOOK-CAS")
    @ExcelProperty("账簿编码")
    private String code;

    @Schema(description = "账簿名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "中国会计准则账簿")
    @ExcelProperty("账簿名称")
    private String name;

    @Schema(description = "会计准则", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @ExcelProperty("会计准则")
    private Integer accountingStandard;

    @Schema(description = "会计准则名称", example = "中国会计准则")
    @ExcelProperty("会计准则名称")
    private String accountingStandardName;

    @Schema(description = "本位币编号", example = "1")
    @ExcelProperty("本位币编号")
    private Long currencyId;

    @Schema(description = "是否主账簿", example = "true")
    @ExcelProperty("是否主账簿")
    private Boolean isPrimary;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @ExcelProperty("状态")
    private Integer status;

    @Schema(description = "状态名称", example = "启用")
    @ExcelProperty("状态名称")
    private String statusName;

    @Schema(description = "排序", example = "1")
    @ExcelProperty("排序")
    private Integer sort;

    @Schema(description = "备注", example = "中国会计准则主账簿")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}
