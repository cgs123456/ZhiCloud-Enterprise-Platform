package cn.zhicloud.module.wms.controller.admin.billing.vo.contract;

import cn.zhicloud.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static cn.zhicloud.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - WMS 计费合同分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class WmsBillingContractPageReqVO extends PageParam {

    @Schema(description = "合同号", example = "HT202605110001")
    private String contractNo;

    @Schema(description = "合同名称", example = "仓储合同")
    private String contractName;

    @Schema(description = "货主编号", example = "2048")
    private Long ownerId;

    @Schema(description = "合同状态", example = "10")
    private Integer status;

    @Schema(description = "生效日期")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDate[] startDate;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}
