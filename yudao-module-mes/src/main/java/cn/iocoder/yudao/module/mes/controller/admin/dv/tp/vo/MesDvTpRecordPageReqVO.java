package cn.iocoder.yudao.module.mes.controller.admin.dv.tp.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - MES TPM 执行记录分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class MesDvTpRecordPageReqVO extends PageParam {

    @Schema(description = "TPM 计划编号", example = "1024")
    private Long planId;

    @Schema(description = "设备编号", example = "1")
    private Long equipmentId;

    @Schema(description = "执行人编号", example = "1")
    private Long executorId;

    @Schema(description = "结果", example = "10")
    private Integer result;

    @Schema(description = "执行日期")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDate[] executeDate;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDate[] createTime;

}