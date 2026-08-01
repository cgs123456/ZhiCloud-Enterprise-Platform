package cn.iocoder.yudao.module.oa.controller.admin.reimburse.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - OA 报销单 Response VO")
@Data
@ExcelIgnoreUnannotated
public class OaReimburseRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("编号")
    private Long id;

    @Schema(description = "报销单号", requiredMode = Schema.RequiredMode.REQUIRED, example = "BX20240101001")
    @ExcelProperty("报销单号")
    private String no;

    @Schema(description = "报销主题", requiredMode = Schema.RequiredMode.REQUIRED, example = "北京出差报销")
    @ExcelProperty("报销主题")
    private String reimburseName;

    @Schema(description = "申请人 ID", example = "2048")
    @ExcelProperty("申请人 ID")
    private Long applicantUserId;

    @Schema(description = "部门 ID", example = "100")
    @ExcelProperty("部门 ID")
    private Long deptId;

    @Schema(description = "报销类型（10 差旅 20 招待 30 办公 40 交通 50 其他）", example = "10")
    @ExcelProperty("报销类型")
    private Integer reimburseType;

    @Schema(description = "报销日期", example = "2024-01-01")
    @ExcelProperty("报销日期")
    private LocalDate reimburseDate;

    @Schema(description = "报销总额", example = "1000.00")
    @ExcelProperty("报销总额")
    private BigDecimal totalAmount;

    @Schema(description = "支付状态（10 未支付 20 部分支付 30 已支付）", example = "10")
    @ExcelProperty("支付状态")
    private Integer paymentStatus;

    @Schema(description = "工作流编号", example = "12345")
    @ExcelProperty("工作流编号")
    private String processInstanceId;

    @Schema(description = "状态（10 草稿 20 审批中 30 已通过 40 已驳回 50 已撤销）", example = "10")
    @ExcelProperty("状态")
    private Integer status;

    @Schema(description = "备注", example = "随便")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

    @Schema(description = "报销明细列表")
    private List<OaReimburseItemVO> items;

}
