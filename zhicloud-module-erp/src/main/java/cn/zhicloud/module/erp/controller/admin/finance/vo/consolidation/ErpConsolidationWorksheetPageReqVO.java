package cn.zhicloud.module.erp.controller.admin.finance.vo.consolidation;

import cn.zhicloud.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - ERP 合并工作底稿分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpConsolidationWorksheetPageReqVO extends PageParam {

    @Schema(description = "合并周期", example = "202607")
    private String consolidationPeriod;

    @Schema(description = "母公司编号", example = "1")
    private Long parentCompanyId;

    @Schema(description = "子公司编号", example = "2")
    private Long subsidiaryCompanyId;

    @Schema(description = "抵消类型", example = "10")
    private Integer eliminationType;

    @Schema(description = "状态", example = "10")
    private Integer status;

}
