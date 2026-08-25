package cn.zhicloud.module.erp.controller.admin.finance.vo.glvoucher;

import cn.zhicloud.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

import static cn.zhicloud.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - ERP 会计凭证分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpGlVoucherPageReqVO extends PageParam {

    @Schema(description = "凭证字号", example = "记-001")
    private String voucherNo;

    @Schema(description = "凭证类型", example = "40")
    private Integer voucherType;

    @Schema(description = "状态", example = "10")
    private Integer status;

    @Schema(description = "会计期间编号", example = "1024")
    private Long periodId;

    @Schema(description = "凭证日期起")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate voucherDateStart;

    @Schema(description = "凭证日期止")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate voucherDateEnd;

}
