package cn.zhicloud.module.hr.controller.admin.hrm.insurance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - HRM 社保方案 VO")
@Data
public class InsuranceSchemeVO {

    @Schema(description = "社保方案编号")
    private Long id;

    @Schema(description = "方案名称", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "方案名称不能为空")
    private String name;

    @Schema(description = "参保地区编号")
    private Long areaId;

    @Schema(description = "参保地区（现算，AreaUtils）")
    private String areaName;

    @Schema(description = "户籍类型")
    private String householdType;

    @Schema(description = "方案类型：1比例 2固定金额")
    @NotNull(message = "方案类型不能为空")
    private Integer type;

    @Schema(description = "全部社保项目（新增/修改提交用）")
    @Valid
    @NotEmpty(message = "参保项目不能为空")
    private List<InsuranceSchemeProjectVO> projectList;

    @Schema(description = "社保项目（响应拆分）")
    private List<InsuranceSchemeProjectVO> socialSecurityProjectList;

    @Schema(description = "公积金项目（响应拆分）")
    private List<InsuranceSchemeProjectVO> providentFundProjectList;

    @Schema(description = "个人社保金额")
    private BigDecimal personalInsuranceAmount;

    @Schema(description = "公司社保金额")
    private BigDecimal corporateInsuranceAmount;

    @Schema(description = "个人公积金金额")
    private BigDecimal personalProvidentFundAmount;

    @Schema(description = "公司公积金金额")
    private BigDecimal corporateProvidentFundAmount;

    @Schema(description = "使用人数")
    private Long useCount;

    @Schema(description = "历史月记录数")
    private Long monthRecordCount;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
