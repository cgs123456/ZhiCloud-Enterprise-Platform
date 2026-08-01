package cn.iocoder.yudao.module.qms.controller.admin.inspectionorder.vo;

import cn.iocoder.yudao.framework.common.validation.InEnum;
import cn.iocoder.yudao.module.qms.enums.qms.InspectionTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - QMS 检验单新增/修改 Request VO")
@Data
public class InspectionOrderSaveReqVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "检验单号", requiredMode = Schema.RequiredMode.REQUIRED, example = "IQC20240101001")
    @NotEmpty(message = "检验单号不能为空")
    private String orderNo;

    @Schema(description = "检验类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @NotNull(message = "检验类型不能为空")
    @InEnum(InspectionTypeEnum.class)
    private Integer type;

    @Schema(description = "供应商 ID", example = "1024")
    private Long supplierId;

    @Schema(description = "批次号", example = "BATCH001")
    private String batchNo;

    @Schema(description = "工单 ID", example = "2048")
    private Long workOrderId;

    @Schema(description = "产品 ID", example = "3072")
    private Long productId;

    @Schema(description = "检验员", example = "芋头")
    private String inspector;

    @Schema(description = "检验时间")
    private LocalDateTime inspectTime;

    @Schema(description = "状态", example = "10")
    private Integer status;

    @Schema(description = "备注", example = "随便")
    private String remark;

}
