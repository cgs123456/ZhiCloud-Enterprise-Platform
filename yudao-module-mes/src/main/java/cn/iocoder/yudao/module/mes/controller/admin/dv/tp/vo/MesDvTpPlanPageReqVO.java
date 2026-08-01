package cn.iocoder.yudao.module.mes.controller.admin.dv.tp.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - MES TPM 计划分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class MesDvTpPlanPageReqVO extends PageParam {

    @Schema(description = "设备编号", example = "1")
    private Long equipmentId;

    @Schema(description = "计划编号", example = "TP-001")
    private String planNo;

    @Schema(description = "计划类型", example = "10")
    private Integer planType;

    @Schema(description = "周期类型", example = "30")
    private Integer cycleType;

    @Schema(description = "状态", example = "10")
    private Integer status;

    @Schema(description = "下次执行日期")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDate[] nextExecuteDate;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDate[] createTime;

}