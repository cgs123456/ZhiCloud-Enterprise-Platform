package cn.zhicloud.module.qms.controller.admin.inspectionorder.vo;

import cn.zhicloud.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.zhicloud.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - QMS 检验单分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class InspectionOrderPageReqVO extends PageParam {

    @Schema(description = "检验单号", example = "IQC20240101001")
    private String orderNo;

    @Schema(description = "检验类型（10 IQC 来料检验 20 IPQC 过程检验 35 FQC 成品检验 30 OQC 出货检验）", example = "10")
    private Integer type;

    @Schema(description = "供应商 ID", example = "1024")
    private Long supplierId;

    @Schema(description = "批次号", example = "BATCH001")
    private String batchNo;

    @Schema(description = "工单 ID", example = "2048")
    private Long workOrderId;

    @Schema(description = "产品 ID", example = "3072")
    private Long productId;

    @Schema(description = "状态", example = "10")
    private Integer status;

    @Schema(description = "检验时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] inspectTime;

}