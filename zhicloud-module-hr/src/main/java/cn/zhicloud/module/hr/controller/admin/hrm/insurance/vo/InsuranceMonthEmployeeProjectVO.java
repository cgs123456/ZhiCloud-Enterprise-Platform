package cn.zhicloud.module.hr.controller.admin.hrm.insurance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.math.BigDecimal;

@Schema(description = "管理后台 - HRM 员工月度社保项目 VO")
@Data
public class InsuranceMonthEmployeeProjectVO {

    @Schema(description = "来源社保方案项目编号")
    private Long schemeProjectId;

    @Schema(description = "项目类型")
    private Integer type;

    @Schema(description = "项目名称")
    private String name;

    @Schema(description = "缴纳基数")
    private BigDecimal baseAmount;

    @Schema(description = "公司缴纳比例")
    private BigDecimal corporateRate;

    @Schema(description = "个人缴纳比例")
    private BigDecimal personalRate;

    @Schema(description = "公司缴纳金额")
    private BigDecimal corporateAmount;

    @Schema(description = "个人缴纳金额")
    private BigDecimal personalAmount;

}
