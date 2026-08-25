package cn.zhicloud.module.erp.controller.admin.finance.vo.bankaccount;

import cn.zhicloud.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - ERP 银行账户分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpBankAccountPageReqVO extends PageParam {

    @Schema(description = "账号", example = "6225")
    private String accountNo;

    @Schema(description = "账户名称", example = "智云")
    private String accountName;

    @Schema(description = "开户行", example = "工商银行")
    private String bankName;

    @Schema(description = "状态（0 启用 1 禁用）", example = "0")
    private Integer status;

}