package cn.zhicloud.module.fms.controller.admin.config.vo.accountset;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * FMS 财务报表行项目 VO
 *
 * <p>与 ERP 侧 ErpFinancialStatementItemVO 同结构，但独立演进：
 * 跨模块 VO 泄漏会让 ERP 侧字段变化直接改到 FMS 的接口契约（边界纪律）。
 *
 * @author 智云
 */
@Schema(description = "用户后台 - 财务报表行项目 VO")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FmsFinancialStatementItemVO {

    @Schema(description = "项目编码", example = "1001")
    private String itemCode;

    @Schema(description = "项目名称", example = "库存现金")
    private String itemName;

    @Schema(description = "金额", example = "10000.00")
    private BigDecimal amount;

}
