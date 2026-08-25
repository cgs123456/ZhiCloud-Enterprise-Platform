package cn.zhicloud.module.oa.controller.admin.reimburse.vo;

import cn.zhicloud.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Schema(description = "管理后台 - OA 报销单分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class OaReimbursePageReqVO extends PageParam {

    @Schema(description = "报销单号", example = "BX20240101001")
    private String no;

    @Schema(description = "报销主题", example = "差旅报销")
    private String reimburseName;

    @Schema(description = "申请人 ID", example = "2048")
    private Long applicantUserId;

    @Schema(description = "报销类型（10 差旅 20 招待 30 办公 40 交通 50 其他）", example = "10")
    private Integer reimburseType;

    @Schema(description = "支付状态（10 未支付 20 部分支付 30 已支付）", example = "10")
    private Integer paymentStatus;

    @Schema(description = "状态（10 草稿 20 审批中 30 已通过 40 已驳回 50 已撤销）", example = "10")
    private Integer status;

    @Schema(description = "报销日期范围")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate[] reimburseDate;

}
