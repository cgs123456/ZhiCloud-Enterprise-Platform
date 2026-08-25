package cn.zhicloud.module.qms.controller.admin.inspectionorder.vo;

import cn.zhicloud.framework.common.validation.InEnum;
import cn.zhicloud.module.qms.enums.qms.InspectionBizTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - QMS 成品检验单创建 Request VO")
@Data
public class FqcInspectionOrderCreateReqVO {

    @Schema(description = "检验单号", requiredMode = Schema.RequiredMode.REQUIRED, example = "FQC20240101001")
    @NotEmpty(message = "检验单号不能为空")
    private String orderNo;

    @Schema(description = "成品工单 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    @NotNull(message = "成品工单 ID 不能为空")
    private Long workOrderId;

    @Schema(description = "产品 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "3072")
    @NotNull(message = "产品 ID 不能为空")
    private Long productId;

    @Schema(description = "批次号", example = "BATCH001")
    private String batchNo;

    @Schema(description = "业务类型（默认 PRODUCTION_OUT）", example = "PRODUCTION_OUT")
    @InEnum(InspectionBizTypeEnum.class)
    private String bizType;

    @Schema(description = "业务单据 ID（默认取成品工单 ID）", example = "2048")
    private Long bizId;

    @Schema(description = "检验员", example = "芋头")
    private String inspector;

    @Schema(description = "检验时间")
    private LocalDateTime inspectTime;

    @Schema(description = "备注", example = "成品检验")
    private String remark;

}