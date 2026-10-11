package cn.zhicloud.module.hr.controller.admin.hrm.insurance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * HRM 标准参保项目 VO（前端 getInsuranceStandardProjectList 返回）
 *
 * @author zhicloud
 */
@Schema(description = "管理后台 - HRM 标准参保项目 VO")
@Data
public class InsuranceStandardProjectVO {

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
