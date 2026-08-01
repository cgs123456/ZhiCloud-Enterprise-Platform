package cn.iocoder.yudao.module.qms.controller.admin.instrument.vo;

import cn.iocoder.yudao.framework.excel.core.annotations.DictFormat;
import cn.iocoder.yudao.framework.excel.core.convert.DictConvert;
import cn.iocoder.yudao.module.qms.enums.DictTypeConstants;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * QMS 校准到期预警 Response VO
 *
 * <p>由 {@code QmsInstrumentService.getExpiringSoonInstruments(int withinDays)} 与
 * {@code getOverdueInstruments()} 返回，用于校准到期/逾期提醒。
 *
 * @author 芋道源码
 */
@Schema(description = "管理后台 - QMS 校准到期预警 Response VO")
@Data
@ExcelIgnoreUnannotated
public class QmsInstrumentExpiringSoonRespVO {

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

    @Schema(description = "出厂编号", example = "SN2024001")
    @ExcelProperty("出厂编号")
    private String serialNo;

    @Schema(description = "类别", example = "10")
    @ExcelProperty(value = "类别", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.INSTRUMENT_CATEGORY)
    private Integer category;

    @Schema(description = "状态", example = "10")
    @ExcelProperty(value = "状态", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.INSTRUMENT_STATUS)
    private Integer status;

    @Schema(description = "使用地点", example = "一号车间检验室")
    @ExcelProperty("使用地点")
    private String location;

    @Schema(description = "负责人", example = "张三")
    @ExcelProperty("负责人")
    private String responsiblePerson;

    @Schema(description = "上次校准日期", example = "2024-01-01")
    @ExcelProperty("上次校准日期")
    private LocalDate lastCalibrationDate;

    @Schema(description = "下次校准日期", requiredMode = Schema.RequiredMode.REQUIRED, example = "2025-01-01")
    @ExcelProperty("下次校准日期")
    private LocalDate nextCalibrationDate;

    @Schema(description = "距下次校准剩余天数（负数表示已逾期）", example = "7")
    @ExcelProperty("剩余天数")
    private Long remainingDays;

    @Schema(description = "是否已逾期", example = "false")
    @ExcelProperty("是否已逾期")
    private Boolean overdue;

    /**
     * 根据下次校准日期与今天计算剩余天数与逾期标识。
     *
     * @param today 今天日期
     */
    public void computeRemaining(LocalDate today) {
        if (nextCalibrationDate == null) {
            this.remainingDays = null;
            this.overdue = false;
            return;
        }
        this.remainingDays = ChronoUnit.DAYS.between(today, nextCalibrationDate);
        this.overdue = this.remainingDays < 0;
    }

}
