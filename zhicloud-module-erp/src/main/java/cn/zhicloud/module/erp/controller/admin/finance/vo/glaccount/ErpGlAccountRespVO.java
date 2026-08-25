package cn.zhicloud.module.erp.controller.admin.finance.vo.glaccount;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * ERP 会计科目 Response VO（P0-7）
 *
 * <p>包含父子树形结构 children、类型与方向的中文名称。
 *
 * @author 智云
 */
@Schema(description = "管理后台 - ERP 会计科目 Response VO")
@Data
public class ErpGlAccountRespVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "父级编号", example = "0")
    private Long parentId;

    @Schema(description = "科目编码", example = "1001")
    private String code;

    @Schema(description = "科目名称", example = "库存现金")
    private String name;

    @Schema(description = "科目类型", example = "10")
    private Integer type;

    @Schema(description = "科目类型名称", example = "资产")
    private String typeName;

    @Schema(description = "余额方向", example = "10")
    private Integer balanceDirection;

    @Schema(description = "余额方向名称", example = "借方")
    private String balanceDirectionName;

    @Schema(description = "层级", example = "1")
    private Integer level;

    @Schema(description = "是否末级科目", example = "true")
    private Boolean isLeaf;

    @Schema(description = "期初借方余额", example = "0")
    private BigDecimal openingDebit;

    @Schema(description = "期初贷方余额", example = "0")
    private BigDecimal openingCredit;

    @Schema(description = "当前借方累计发生额", example = "0")
    private BigDecimal currentDebit;

    @Schema(description = "当前贷方累计发生额", example = "0")
    private BigDecimal currentCredit;

    @Schema(description = "期末借方余额", example = "0")
    private BigDecimal closingDebit;

    @Schema(description = "期末贷方余额", example = "0")
    private BigDecimal closingCredit;

    @Schema(description = "状态", example = "0")
    private Integer status;

    @Schema(description = "排序", example = "1")
    private Integer sort;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "子科目列表")
    private List<ErpGlAccountRespVO> children;

}
