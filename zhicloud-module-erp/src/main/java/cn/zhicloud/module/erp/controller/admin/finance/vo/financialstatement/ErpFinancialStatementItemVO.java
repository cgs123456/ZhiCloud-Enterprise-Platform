package cn.zhicloud.module.erp.controller.admin.finance.vo.financialstatement;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 财务报表通用行项目 VO（P0-4）
 */
@Schema(description = "管理后台 - 财务报表行项目 VO")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErpFinancialStatementItemVO {

    @Schema(description = "项目编码", example = "1001")
    private String itemCode;

    @Schema(description = "项目名称", example = "库存现金")
    private String itemName;

    @Schema(description = "金额", example = "10000.00")
    private BigDecimal amount;

}