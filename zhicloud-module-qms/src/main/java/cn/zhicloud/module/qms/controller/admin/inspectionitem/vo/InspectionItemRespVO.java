package cn.zhicloud.module.qms.controller.admin.inspectionitem.vo;

import cn.zhicloud.framework.excel.core.annotations.DictFormat;
import cn.zhicloud.framework.excel.core.convert.DictConvert;
import cn.zhicloud.module.qms.enums.DictTypeConstants;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - QMS 检验项目 Response VO")
@Data
@ExcelIgnoreUnannotated
public class InspectionItemRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("编号")
    private Long id;

    @Schema(description = "检验项目编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "IQC-001")
    @ExcelProperty("检验项目编码")
    private String code;

    @Schema(description = "检验项目名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "外观检查")
    @ExcelProperty("检验项目名称")
    private String name;

    @Schema(description = "检验类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @ExcelProperty(value = "检验类型", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.INSPECTION_TYPE)
    private Integer type;

    @Schema(description = "检验方法", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @ExcelProperty(value = "检验方法", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.INSPECTION_METHOD)
    private Integer method;

    @Schema(description = "检验标准", example = "GB/T 2828.1")
    @ExcelProperty("检验标准")
    private String standard;

    @Schema(description = "目标值", example = "10.0")
    @ExcelProperty("目标值")
    private String target;

    @Schema(description = "上限", example = "10.5")
    @ExcelProperty("上限")
    private BigDecimal upperLimit;

    @Schema(description = "下限", example = "9.5")
    @ExcelProperty("下限")
    private BigDecimal lowerLimit;

    @Schema(description = "单位", example = "mm")
    @ExcelProperty("单位")
    private String unit;

    @Schema(description = "备注", example = "随便")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @ExcelProperty("状态")
    private Integer status;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}
