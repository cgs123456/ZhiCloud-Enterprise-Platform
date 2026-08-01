package cn.iocoder.yudao.module.mes.controller.admin.pro.rework.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - MES 返工工单新增/修改 Request VO")
@Data
public class MesProReworkOrderSaveReqVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "返工工单号", requiredMode = Schema.RequiredMode.REQUIRED, example = "RW-001")
    @NotEmpty(message = "返工工单号不能为空")
    private String code;

    @Schema(description = "原工单 ID", example = "100")
    private Long originalWorkOrderId;

    @Schema(description = "原工单号", example = "WO-001")
    private String originalWorkOrderCode;

    @Schema(description = "产品 ID", example = "200")
    private Long productId;

    @Schema(description = "产品编码", example = "P-001")
    private String productCode;

    @Schema(description = "产品名称", example = "电路板 A")
    private String productName;

    @Schema(description = "返工数量", requiredMode = Schema.RequiredMode.REQUIRED, example = "10.00")
    @NotNull(message = "返工数量不能为空")
    private BigDecimal reworkQuantity;

    @Schema(description = "返工原因", example = "尺寸超差")
    private String reworkReason;

    @Schema(description = "返工类型（10 生产返工 / 20 来料不良 / 30 客户退货返工）", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @NotNull(message = "返工类型不能为空")
    private Integer reworkType;

    @Schema(description = "返工工序 ID", example = "300")
    private Long reworkProcessId;

    @Schema(description = "返工工序名称", example = "焊接")
    private String reworkProcessName;

    @Schema(description = "责任人 ID", example = "400")
    private Long responsiblePersonId;

    @Schema(description = "责任部门 ID", example = "500")
    private Long responsibleDeptId;

    @Schema(description = "计划开始时间")
    private LocalDateTime plannedStartTime;

    @Schema(description = "计划结束时间")
    private LocalDateTime plannedEndTime;

    @Schema(description = "实际开始时间")
    private LocalDateTime actualStartTime;

    @Schema(description = "实际结束时间")
    private LocalDateTime actualEndTime;

    @Schema(description = "显示顺序", example = "0")
    private Integer sort;

    @Schema(description = "备注", example = "备注")
    private String remark;

}
