package cn.zhicloud.module.erp.controller.admin.finance.vo.consolidation;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ERP 合并工作底稿 Response VO（P1-合并报表引擎）
 *
 * @author 智云
 */
@Schema(description = "管理后台 - ERP 合并工作底稿 Response VO")
@Data
public class ErpConsolidationWorksheetRespVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "合并周期", example = "202607")
    private String consolidationPeriod;

    @Schema(description = "母公司编号", example = "1")
    private Long parentCompanyId;

    @Schema(description = "子公司编号", example = "2")
    private Long subsidiaryCompanyId;

    @Schema(description = "抵消类型", example = "10")
    private Integer eliminationType;

    @Schema(description = "抵消类型名称", example = "投资权益抵消")
    private String eliminationTypeName;

    @Schema(description = "抵消金额", example = "10000.00")
    private BigDecimal eliminationAmount;

    @Schema(description = "抵消描述", example = "投资权益抵消分录")
    private String description;

    @Schema(description = "状态", example = "10")
    private Integer status;

    @Schema(description = "状态名称", example = "待审核")
    private String statusName;

    @Schema(description = "排序", example = "1")
    private Integer sort;

    @Schema(description = "备注", example = "集团内部投资抵消")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
