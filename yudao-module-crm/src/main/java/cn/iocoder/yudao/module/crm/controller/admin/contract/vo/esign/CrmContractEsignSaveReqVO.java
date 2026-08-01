package cn.iocoder.yudao.module.crm.controller.admin.contract.vo.esign;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Schema(description = "管理后台 - CRM 合同电子签发起 Request VO")
@Data
public class CrmContractEsignSaveReqVO {

    @Schema(description = "合同编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "合同编号不能为空")
    private Long contractId;

}
