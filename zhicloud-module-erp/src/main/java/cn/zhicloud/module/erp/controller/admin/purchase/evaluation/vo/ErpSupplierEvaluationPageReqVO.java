package cn.zhicloud.module.erp.controller.admin.purchase.evaluation.vo;

import cn.zhicloud.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.zhicloud.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - ERP 供应商评估分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpSupplierEvaluationPageReqVO extends PageParam {

    @Schema(description = "供应商编号", example = "1")
    private Long supplierId;

    @Schema(description = "评估周期 yyyyMM", example = "202607")
    private String evaluationPeriod;

    @Schema(description = "等级 A/B/C/D", example = "A")
    private String grade;

    @Schema(description = "评估人", example = "张三")
    private String evaluator;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}
