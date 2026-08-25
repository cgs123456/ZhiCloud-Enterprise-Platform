package cn.zhicloud.module.crm.controller.admin.servicecloud.vo;

import cn.zhicloud.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.zhicloud.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - CRM 售后工单分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class CrmWorkOrderPageReqVO extends PageParam {

    @Schema(description = "工单编号", example = "WO20230101")
    private String no;

    @Schema(description = "标题", example = "设备故障")
    private String title;

    @Schema(description = "客户编号", example = "18336")
    private Long customerId;

    @Schema(description = "工单类型", example = "20")
    private Integer workOrderType;

    @Schema(description = "优先级", example = "30")
    private Integer priority;

    @Schema(description = "状态", example = "10")
    private Integer status;

    @Schema(description = "处理人", example = "1024")
    private Long assigneeUserId;

    @Schema(description = "创建时间", example = "[2023-01-01 00:00:00, 2023-01-31 23:59:59]")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}
