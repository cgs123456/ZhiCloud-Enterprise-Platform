package cn.zhicloud.module.qms.controller.admin.inspectionorder.vo;

import cn.zhicloud.framework.common.validation.InEnum;
import cn.zhicloud.module.qms.enums.qms.InspectionBizTypeEnum;
import cn.zhicloud.module.qms.enums.qms.InspectionOrderStatusEnum;
import cn.zhicloud.module.qms.enums.qms.InspectionTypeEnum;
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

    /**
     * 目标为 erp_supplier.id（ERP 供应商主数据），不要用 wms_receipt_order.merchant_id（商户）回填
     */
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

    /**
     * 状态。**仅用于查询侧展示/回显，服务端不接受外部写入**：
     * 新增由 createInspectionOrder 强制置为 PENDING，修改由 updateInspectionOrder 置空。
     * 传了 @InEnum 是为了在边界处拒绝垃圾值（§12.235.6 之前连枚举约束都没有）。
     */
    @Schema(description = "状态（服务端只读，不参与写入）", example = "10")
    @InEnum(InspectionOrderStatusEnum.class)
    private Integer status;

    @Schema(description = "AQL 接收数 Ac（缺陷数 <= Ac 判合格）", example = "1")
    private Integer acceptanceQuantity;

    @Schema(description = "AQL 拒收数 Re（缺陷数 >= Re 判不合格）", example = "2")
    private Integer rejectQuantity;

    @Schema(description = "业务类型", example = "PURCHASE_IN")
    @InEnum(InspectionBizTypeEnum.class)
    private String bizType;

    @Schema(description = "业务单据 ID", example = "2048")
    private Long bizId;

    @Schema(description = "备注", example = "随便")
    private String remark;

}
