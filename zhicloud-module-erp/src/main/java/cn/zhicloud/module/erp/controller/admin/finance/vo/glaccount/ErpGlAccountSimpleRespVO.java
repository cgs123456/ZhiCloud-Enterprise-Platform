package cn.zhicloud.module.erp.controller.admin.finance.vo.glaccount;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * ERP 会计科目精简 Response VO（P0-7）
 *
 * <p>用于凭证分录选择科目时下拉列表展示，仅返回关键字段。
 *
 * @author 智云
 */
@Schema(description = "管理后台 - ERP 会计科目精简 Response VO")
@Data
public class ErpGlAccountSimpleRespVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "科目编码", example = "1001")
    private String code;

    @Schema(description = "科目名称", example = "库存现金")
    private String name;

    @Schema(description = "科目类型", example = "10")
    private Integer type;

    @Schema(description = "余额方向", example = "10")
    private Integer balanceDirection;

    @Schema(description = "是否末级科目", example = "true")
    private Boolean isLeaf;

}
