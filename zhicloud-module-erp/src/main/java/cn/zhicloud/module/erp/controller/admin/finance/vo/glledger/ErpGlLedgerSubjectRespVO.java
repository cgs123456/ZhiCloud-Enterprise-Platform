package cn.zhicloud.module.erp.controller.admin.finance.vo.glledger;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 管理后台 - ERP 账表明细账科目树节点 Response VO（平铺，前端 handleTree 建树）
 *
 * <p>返回"查询期间内有发生额，或期初行非零"的科目及其全部祖先（保证树连通）。
 *
 * @author 智云
 */
@Data
@Schema(description = "管理后台 - ERP 账表明细账科目树节点 Response VO")
public class ErpGlLedgerSubjectRespVO {

    @Schema(description = "科目编号", example = "1001")
    private Long id;

    @Schema(description = "父级科目编号", example = "0")
    private Long parentId;

    @Schema(description = "科目编码", example = "1001")
    private String code;

    @Schema(description = "科目名称", example = "库存现金")
    private String name;

    @Schema(description = "科目级次", example = "1")
    private Integer level;

    @Schema(description = "余额方向：10 借方 / 20 贷方 / 30 双向", example = "10")
    private Integer balanceDirection;

    @Schema(description = "状态：0 启用 / 1 停用", example = "0")
    private Integer status;

    @Schema(description = "是否末级科目", example = "true")
    private Boolean isLeaf;

    @Schema(description = "排序", example = "1")
    private Integer sort;

}
