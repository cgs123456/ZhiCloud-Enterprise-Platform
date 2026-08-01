package cn.iocoder.yudao.module.erp.controller.admin.finance.vo.costitem;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - ERP 成本项目 Response VO")
@Data
@ExcelIgnoreUnannotated
public class ErpCostItemRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("编号")
    private Long id;

    @Schema(description = "成本项目编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "CI001")
    @ExcelProperty("成本项目编码")
    private String code;

    @Schema(description = "成本项目名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "直接材料")
    @ExcelProperty("成本项目名称")
    private String name;

    @Schema(description = "类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @ExcelProperty("类型")
    private Integer type;

    @Schema(description = "计算方法", example = "标准成本法")
    @ExcelProperty("计算方法")
    private String calculationMethod;

    @Schema(description = "是否标准成本", example = "1")
    @ExcelProperty("是否标准成本")
    private Integer isStandard;

    @Schema(description = "备注", example = "原材料成本")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "排序", example = "1")
    @ExcelProperty("排序")
    private Integer sort;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @ExcelProperty("状态")
    private Integer status;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}
