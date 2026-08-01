package cn.iocoder.yudao.module.qms.controller.admin.instrument.vo;

import cn.iocoder.yudao.framework.excel.core.annotations.DictFormat;
import cn.iocoder.yudao.framework.excel.core.convert.DictConvert;
import cn.iocoder.yudao.module.qms.enums.DictTypeConstants;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - QMS 计量器具台账 Response VO")
@Data
@ExcelIgnoreUnannotated
public class QmsInstrumentRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("编号")
    private Long id;

    @Schema(description = "器具编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "INS20240101001")
    @ExcelProperty("器具编号")
    private String code;

    @Schema(description = "器具名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "千分尺")
    @ExcelProperty("器具名称")
    private String name;

    @Schema(description = "型号规格", example = "0-25mm")
    @ExcelProperty("型号规格")
    private String model;

    @Schema(description = "生产厂家", example = "上海量具刃具厂")
    @ExcelProperty("生产厂家")
    private String manufacturer;

    @Schema(description = "出厂编号", example = "SN2024001")
    @ExcelProperty("出厂编号")
    private String serialNo;

    @Schema(description = "类别", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @ExcelProperty(value = "类别", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.INSTRUMENT_CATEGORY)
    private Integer category;

    @Schema(description = "精度等级", example = "0.001mm")
    @ExcelProperty("精度等级")
    private String accuracy;

    @Schema(description = "测量范围", example = "0-25mm")
    @ExcelProperty("测量范围")
    private String measureRange;

    @Schema(description = "计量单位", example = "mm")
    @ExcelProperty("计量单位")
    private String unit;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @ExcelProperty(value = "状态", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.INSTRUMENT_STATUS)
    private Integer status;

    @Schema(description = "使用地点", example = "一号车间检验室")
    @ExcelProperty("使用地点")
    private String location;

    @Schema(description = "负责人", example = "张三")
    @ExcelProperty("负责人")
    private String responsiblePerson;

    @Schema(description = "校准周期天数", example = "365")
    @ExcelProperty("校准周期天数")
    private Integer calibrationCycleDays;

    @Schema(description = "上次校准日期", example = "2024-01-01")
    @ExcelProperty("上次校准日期")
    private LocalDate lastCalibrationDate;

    @Schema(description = "下次校准日期", example = "2025-01-01")
    @ExcelProperty("下次校准日期")
    private LocalDate nextCalibrationDate;

    @Schema(description = "备注", example = "随便")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "排序", example = "0")
    private Integer sort;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}
