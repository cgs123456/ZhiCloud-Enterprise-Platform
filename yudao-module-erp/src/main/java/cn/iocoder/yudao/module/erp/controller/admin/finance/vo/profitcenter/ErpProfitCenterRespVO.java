package cn.iocoder.yudao.module.erp.controller.admin.finance.vo.profitcenter;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - ERP 利润中心 Response VO")
@Data
@ExcelIgnoreUnannotated
public class ErpProfitCenterRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("编号")
    private Long id;

    @Schema(description = "利润中心编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "PC1001")
    @ExcelProperty("利润中心编码")
    private String code;

    @Schema(description = "利润中心名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "华东事业部")
    @ExcelProperty("利润中心名称")
    private String name;

    @Schema(description = "父级编号", example = "0")
    @ExcelProperty("父级编号")
    private Long parentId;

    @Schema(description = "负责人 ID", example = "1")
    @ExcelProperty("负责人 ID")
    private Long managerId;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @ExcelProperty("状态")
    private Integer status;

    @Schema(description = "备注", example = "一级利润中心")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}
