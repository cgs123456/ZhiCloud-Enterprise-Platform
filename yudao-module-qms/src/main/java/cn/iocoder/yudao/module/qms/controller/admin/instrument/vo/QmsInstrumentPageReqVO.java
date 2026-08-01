package cn.iocoder.yudao.module.qms.controller.admin.instrument.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Schema(description = "管理后台 - QMS 计量器具台账分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class QmsInstrumentPageReqVO extends PageParam {

    @Schema(description = "器具编号", example = "INS20240101001")
    private String code;

    @Schema(description = "器具名称", example = "千分尺")
    private String name;

    @Schema(description = "类别", example = "10")
    private Integer category;

    @Schema(description = "状态", example = "10")
    private Integer status;

    @Schema(description = "生产厂家", example = "上海量具刃具厂")
    private String manufacturer;

    @Schema(description = "使用地点", example = "一号车间检验室")
    private String location;

    @Schema(description = "负责人", example = "张三")
    private String responsiblePerson;

    @Schema(description = "下次校准日期范围")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate[] nextCalibrationDate;

}
