package cn.zhicloud.module.crm.controller.admin.contract.vo.esign;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - CRM 合同电子签 Response VO")
@Data
public class CrmContractEsignRespVO {

    @Schema(description = "电子签任务 ID", example = "ESIGN1690000000000")
    private String esignTaskId;

    @Schema(description = "合同编号", example = "1024")
    private Long contractId;

    @Schema(description = "签署状态", example = "20")
    private Integer status;

    @Schema(description = "签署状态名称", example = "已签署")
    private String statusName;

    @Schema(description = "签署时间")
    private LocalDateTime signTime;

    @Schema(description = "已签署合同 URL 列表")
    private List<String> fileUrls;

}
