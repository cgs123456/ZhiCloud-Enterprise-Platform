package cn.zhicloud.module.erp.controller.admin.finance.vo.glledger;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 管理后台 - ERP 账表多栏账 Response VO
 *
 * @author 智云
 */
@Data
@Schema(description = "管理后台 - ERP 账表多栏账 Response VO")
public class ErpGlLedgerMultiColumnRespVO {

    @Schema(description = "专栏列（所选科目的直接子科目）", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private List<ErpGlLedgerColumnVO> columns = new ArrayList<>();

    @Schema(description = "账簿行（期初行 / 明细行 / 本期合计行；明细行与合计行带 columnAmounts）",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private List<ErpGlLedgerDetailRespVO> rows = new ArrayList<>();

}
