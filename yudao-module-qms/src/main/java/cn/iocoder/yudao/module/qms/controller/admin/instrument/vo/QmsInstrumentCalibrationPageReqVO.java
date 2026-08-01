package cn.iocoder.yudao.module.qms.controller.admin.instrument.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Schema(description = "管理后台 - QMS 计量器具校准记录分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class QmsInstrumentCalibrationPageReqVO extends PageParam {

    @Schema(description = "器具 ID", example = "1024")
    private Long instrumentId;

    @Schema(description = "校准证书编号", example = "CAL20240101001")
    private String calibrationNo;

    @Schema(description = "校准结果", example = "10")
    private Integer calibrationResult;

    @Schema(description = "校准机构", example = "中国计量科学研究院")
    private String calibrationOrganization;

    @Schema(description = "校准日期范围")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate[] calibrationDate;

}
