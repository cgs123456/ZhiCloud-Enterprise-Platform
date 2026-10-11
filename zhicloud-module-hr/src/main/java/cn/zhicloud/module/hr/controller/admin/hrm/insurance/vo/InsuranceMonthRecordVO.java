package cn.zhicloud.module.hr.controller.admin.hrm.insurance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - HRM 月度社保表 VO")
@Data
public class InsuranceMonthRecordVO {

    @Schema(description = "月度社保表编号")
    private Long id;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "年份")
    private Integer year;

    @Schema(description = "月份")
    private Integer month;

    @Schema(description = "参保人数")
    private Integer insuredEmployeeCount;

    @Schema(description = "停止参保人数")
    private Integer stoppedEmployeeCount;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "个人社保金额")
    private BigDecimal personalInsuranceAmount;

    @Schema(description = "个人公积金金额")
    private BigDecimal personalProvidentFundAmount;

    @Schema(description = "公司社保金额")
    private BigDecimal corporateInsuranceAmount;

    @Schema(description = "公司公积金金额")
    private BigDecimal corporateProvidentFundAmount;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
