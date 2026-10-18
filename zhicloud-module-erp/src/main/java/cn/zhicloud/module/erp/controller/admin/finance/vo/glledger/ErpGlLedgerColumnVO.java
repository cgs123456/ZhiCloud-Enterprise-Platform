package cn.zhicloud.module.erp.controller.admin.finance.vo.glledger;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 管理后台 - ERP 账表多栏账专栏列 Response VO
 *
 * <p>列 = 所选科目的直接子科目（按编码升序）；{@code balanceDirection} 用 ERP 值域
 * （10 借方 / 20 贷方），前端按此分借方专栏与贷方专栏。
 *
 * @author 智云
 */
@Data
@Schema(description = "管理后台 - ERP 账表多栏账专栏列 Response VO")
public class ErpGlLedgerColumnVO {

    @Schema(description = "子科目编号", example = "100101")
    private Long subjectId;

    @Schema(description = "子科目编码", example = "100101")
    private String subjectCode;

    @Schema(description = "子科目名称", example = "库存现金-基本户")
    private String subjectName;

    @Schema(description = "余额方向：10 借方 / 20 贷方 / 30 双向", example = "10")
    private Integer balanceDirection;

}
