package cn.zhicloud.module.qms.controller.admin.inspectionrecord.vo;

import cn.zhicloud.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.zhicloud.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - QMS 检验记录分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class InspectionRecordPageReqVO extends PageParam {

    @Schema(description = "检验单 ID", example = "2048")
    private Long orderId;

    @Schema(description = "检验项目 ID", example = "3072")
    private Long itemId;

    @Schema(description = "检验结果", example = "10")
    private Integer result;

    @Schema(description = "检验员", example = "芋头")
    private String inspector;

    @Schema(description = "检验时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] inspectTime;

}
