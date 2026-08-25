package cn.zhicloud.module.crm.controller.admin.servicecloud.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.zhicloud.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - CRM 售后工单创建/更新 Request VO")
@Data
public class CrmWorkOrderSaveReqVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "标题", requiredMode = Schema.RequiredMode.REQUIRED, example = "设备故障维修")
    @NotEmpty(message = "标题不能为空")
    private String title;

    @Schema(description = "客户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "18336")
    @NotNull(message = "客户编号不能为空")
    private Long customerId;

    @Schema(description = "联系人编号", example = "18546")
    private Long contactId;

    @Schema(description = "产品编号", example = "20529")
    private Long productId;

    @Schema(description = "工单类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "20")
    @NotNull(message = "工单类型不能为空")
    private Integer workOrderType;

    @Schema(description = "优先级", requiredMode = Schema.RequiredMode.REQUIRED, example = "30")
    @NotNull(message = "优先级不能为空")
    private Integer priority;

    @Schema(description = "问题描述", example = "设备无法开机")
    private String description;

    @Schema(description = "处理人", example = "1024")
    private Long assigneeUserId;

    @Schema(description = "SLA 截止时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime slaDeadline;

    @Schema(description = "备注", example = "随便")
    private String remark;

}
