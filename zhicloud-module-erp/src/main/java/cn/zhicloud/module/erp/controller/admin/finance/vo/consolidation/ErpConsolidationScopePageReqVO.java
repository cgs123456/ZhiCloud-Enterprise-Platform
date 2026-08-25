package cn.zhicloud.module.erp.controller.admin.finance.vo.consolidation;

import cn.zhicloud.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - ERP 合并范围分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpConsolidationScopePageReqVO extends PageParam {

    @Schema(description = "母公司编号", example = "1")
    private Long parentCompanyId;

    @Schema(description = "子公司编号", example = "2")
    private Long subsidiaryCompanyId;

    @Schema(description = "合并方法", example = "10")
    private Integer consolidationMethod;

    @Schema(description = "状态", example = "10")
    private Integer status;

}
