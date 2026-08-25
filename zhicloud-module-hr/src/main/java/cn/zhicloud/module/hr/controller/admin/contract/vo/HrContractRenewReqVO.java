package cn.zhicloud.module.hr.controller.admin.contract.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "管理后台 - HR 合同续签 Request VO")
@Data
public class HrContractRenewReqVO {

    @Schema(description = "原合同 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "合同不能为空")
    private Long id;

    @Schema(description = "新合同开始日期", requiredMode = Schema.RequiredMode.REQUIRED, example = "2027-01-01")
    @NotNull(message = "新合同开始日期不能为空")
    private LocalDate newStartDate;

    @Schema(description = "新合同结束日期", example = "2029-12-31")
    private LocalDate newEndDate;

    @Schema(description = "新合同约定薪资", example = "12000.00")
    private BigDecimal newSalary;

    @Schema(description = "新合同附件", example = "https://www.zhicloud.cn/file2.pdf")
    private String newFileUrl;

    @Schema(description = "备注", example = "续签")
    private String remark;

}