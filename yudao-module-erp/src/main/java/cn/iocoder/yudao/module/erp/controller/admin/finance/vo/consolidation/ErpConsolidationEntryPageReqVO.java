package cn.iocoder.yudao.module.erp.controller.admin.finance.vo.consolidation;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - ERP 合并报表抵消分录分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpConsolidationEntryPageReqVO extends PageParam {

    @Schema(description = "合并任务编号", example = "CONS-202607")
    private String consolidationNo;

    @Schema(description = "会计期间编号", example = "1")
    private Long periodId;

    @Schema(description = "期间编码", example = "202607")
    private String periodCode;

    @Schema(description = "抵消类型", example = "10")
    private Integer eliminationType;

    @Schema(description = "借方科目编号", example = "1")
    private Long debitAccountId;

    @Schema(description = "贷方科目编号", example = "2")
    private Long creditAccountId;

    @Schema(description = "状态（10 草稿 / 20 已审核）", example = "10")
    private Integer status;

}
