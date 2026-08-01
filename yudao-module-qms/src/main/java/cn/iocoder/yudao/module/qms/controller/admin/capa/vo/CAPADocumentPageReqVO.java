package cn.iocoder.yudao.module.qms.controller.admin.capa.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - QMS CAPA 文档分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class CAPADocumentPageReqVO extends PageParam {

    @Schema(description = "CAPA 单号", example = "CAPA20240101001")
    private String capaNo;

    @Schema(description = "来源", example = "10")
    private Integer source;

    @Schema(description = "优先级", example = "20")
    private Integer priority;

    @Schema(description = "当前阶段", example = "10")
    private Integer stage;

    @Schema(description = "状态", example = "10")
    private Integer status;

    @Schema(description = "有效性验证结果", example = "20")
    private Integer verificationResult;

    @Schema(description = "责任人", example = "芋头")
    private String responsiblePerson;

    @Schema(description = "截止日期")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] dueDate;

}
