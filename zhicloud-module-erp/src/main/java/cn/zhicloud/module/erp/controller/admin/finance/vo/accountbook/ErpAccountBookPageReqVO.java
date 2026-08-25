package cn.zhicloud.module.erp.controller.admin.finance.vo.accountbook;

import cn.zhicloud.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - ERP 账簿分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpAccountBookPageReqVO extends PageParam {

    @Schema(description = "账簿编码", example = "BOOK-CAS")
    private String code;

    @Schema(description = "账簿名称", example = "中国会计准则账簿")
    private String name;

    @Schema(description = "会计准则", example = "10")
    private Integer accountingStandard;

    @Schema(description = "本位币编号", example = "1")
    private Long currencyId;

    @Schema(description = "是否主账簿", example = "true")
    private Boolean isPrimary;

    @Schema(description = "状态", example = "10")
    private Integer status;

}
