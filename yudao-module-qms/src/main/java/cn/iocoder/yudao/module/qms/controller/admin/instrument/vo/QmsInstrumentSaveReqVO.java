package cn.iocoder.yudao.module.qms.controller.admin.instrument.vo;

import cn.iocoder.yudao.framework.common.validation.InEnum;
import cn.iocoder.yudao.module.qms.enums.instrument.QmsInstrumentCategoryEnum;
import cn.iocoder.yudao.module.qms.enums.instrument.QmsInstrumentStatusEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Schema(description = "管理后台 - QMS 计量器具台账新增/修改 Request VO")
@Data
public class QmsInstrumentSaveReqVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "器具编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "INS20240101001")
    @NotEmpty(message = "器具编号不能为空")
    private String code;

    @Schema(description = "器具名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "千分尺")
    @NotEmpty(message = "器具名称不能为空")
    private String name;

    @Schema(description = "型号规格", example = "0-25mm")
    private String model;

    @Schema(description = "生产厂家", example = "上海量具刃具厂")
    private String manufacturer;

    @Schema(description = "出厂编号", example = "SN2024001")
    private String serialNo;

    @Schema(description = "类别", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @NotNull(message = "类别不能为空")
    @InEnum(QmsInstrumentCategoryEnum.class)
    private Integer category;

    @Schema(description = "精度等级", example = "0.001mm")
    private String accuracy;

    @Schema(description = "测量范围", example = "0-25mm")
    private String measureRange;

    @Schema(description = "计量单位", example = "mm")
    private String unit;

    @Schema(description = "状态", example = "10")
    @InEnum(QmsInstrumentStatusEnum.class)
    private Integer status;

    @Schema(description = "使用地点", example = "一号车间检验室")
    private String location;

    @Schema(description = "负责人", example = "张三")
    private String responsiblePerson;

    @Schema(description = "校准周期天数", example = "365")
    @Positive(message = "校准周期天数必须为正数")
    private Integer calibrationCycleDays;

    @Schema(description = "上次校准日期", example = "2024-01-01")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate lastCalibrationDate;

    @Schema(description = "下次校准日期", example = "2025-01-01")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate nextCalibrationDate;

    @Schema(description = "备注", example = "随便")
    private String remark;

    @Schema(description = "排序", example = "0")
    private Integer sort;

}
