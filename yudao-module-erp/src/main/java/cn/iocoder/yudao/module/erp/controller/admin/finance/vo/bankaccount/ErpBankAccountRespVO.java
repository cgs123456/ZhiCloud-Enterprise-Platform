package cn.iocoder.yudao.module.erp.controller.admin.finance.vo.bankaccount;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - ERP 银行账户 Response VO")
@Data
public class ErpBankAccountRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "账号", requiredMode = Schema.RequiredMode.REQUIRED, example = "6225-8812-3456")
    private String accountNo;

    @Schema(description = "账户名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "厦门芋道科技有限公司")
    private String accountName;

    @Schema(description = "开户行", example = "中国工商银行")
    private String bankName;

    @Schema(description = "开户支行", example = "厦门思明支行")
    private String bankBranch;

    @Schema(description = "账户余额", example = "100000.00")
    private BigDecimal balance;

    @Schema(description = "币种编号", example = "1")
    private Long currencyId;

    @Schema(description = "状态（0 启用 1 禁用）", example = "0")
    private Integer status;

    @Schema(description = "备注", example = "基本户")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

}