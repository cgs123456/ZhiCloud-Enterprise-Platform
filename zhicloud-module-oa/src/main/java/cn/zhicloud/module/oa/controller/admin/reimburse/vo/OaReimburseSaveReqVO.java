package cn.zhicloud.module.oa.controller.admin.reimburse.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "管理后台 - OA 报销单新增/修改 Request VO")
@Data
public class OaReimburseSaveReqVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "报销单号", requiredMode = Schema.RequiredMode.REQUIRED, example = "BX20240101001")
    @NotEmpty(message = "报销单号不能为空")
    private String no;

    @Schema(description = "报销主题", requiredMode = Schema.RequiredMode.REQUIRED, example = "北京出差报销")
    @NotEmpty(message = "报销主题不能为空")
    private String reimburseName;

    @Schema(description = "申请人 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    @NotNull(message = "申请人不能为空")
    private Long applicantUserId;

    @Schema(description = "部门 ID", example = "100")
    private Long deptId;

    @Schema(description = "报销类型（10 差旅 20 招待 30 办公 40 交通 50 其他）", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @NotNull(message = "报销类型不能为空")
    private Integer reimburseType;

    @Schema(description = "报销日期", requiredMode = Schema.RequiredMode.REQUIRED, example = "2024-01-01")
    @NotNull(message = "报销日期不能为空")
    private LocalDate reimburseDate;

    @Schema(description = "报销总额", example = "1000.00")
    private BigDecimal totalAmount;

    @Schema(description = "支付状态（10 未支付 20 部分支付 30 已支付）", example = "10")
    private Integer paymentStatus;

    @Schema(description = "工作流编号", example = "12345")
    private String processInstanceId;

    @Schema(description = "状态（10 草稿 20 审批中 30 已通过 40 已驳回 50 已撤销）", example = "10")
    private Integer status;

    @Schema(description = "备注", example = "随便")
    private String remark;

    @Schema(description = "报销明细列表")
    @Valid
    private List<OaReimburseItemVO> items;

}
