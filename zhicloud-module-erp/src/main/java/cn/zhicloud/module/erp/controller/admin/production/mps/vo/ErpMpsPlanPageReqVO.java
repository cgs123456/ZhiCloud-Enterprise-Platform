package cn.zhicloud.module.erp.controller.admin.production.mps.vo;

import cn.zhicloud.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

import static cn.zhicloud.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - ERP 主生产计划分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpMpsPlanPageReqVO extends PageParam {

    @Schema(description = "计划编号", example = "MPS-001")
    private String planNo;

    @Schema(description = "产品编号", example = "1")
    private Long productId;

    @Schema(description = "计划周期", example = "202607")
    private String planPeriod;

    @Schema(description = "计划类型", example = "10")
    private Integer planType;

    @Schema(description = "来源", example = "10")
    private Integer source;

    @Schema(description = "状态", example = "10")
    private Integer status;

    @Schema(description = "需求日期")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDate[] demandDate;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDate[] createTime;

}