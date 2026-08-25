package cn.zhicloud.module.erp.controller.admin.production.mrp.vo;

import cn.zhicloud.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

import static cn.zhicloud.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - ERP 物料需求计划分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpMrpPlanPageReqVO extends PageParam {

    @Schema(description = "计划编号", example = "MRP-001")
    private String no;

    @Schema(description = "计划名称", example = "2026年7月MRP")
    private String planName;

    @Schema(description = "关联 MPS 主生产计划编号", example = "1")
    private Long mpsPlanId;

    @Schema(description = "状态", example = "10")
    private Integer status;

    @Schema(description = "计划日期")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDate[] planDate;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDate[] createTime;

}
