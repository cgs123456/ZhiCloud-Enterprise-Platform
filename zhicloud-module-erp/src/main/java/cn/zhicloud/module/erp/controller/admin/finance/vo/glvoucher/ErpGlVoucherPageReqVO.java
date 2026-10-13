package cn.zhicloud.module.erp.controller.admin.finance.vo.glvoucher;

import cn.zhicloud.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

import static cn.zhicloud.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - ERP 会计凭证分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpGlVoucherPageReqVO extends PageParam {

    @Schema(description = "凭证字号", example = "记-001")
    private String voucherNo;

    /**
     * FMS 账套编号（可空；按账套筛凭证流水，见 V193）
     *
     * <p>凭证只到 account_book_id，账簿再挂账套，故按账套筛凭证需先解析该账套下的
     * 账簿集合后 IN 查询；不在 erp_gl_voucher 落 account_set_id 冗余列——账簿改挂账套
     * 时冗余列会漂移（两套真相），单一来源是账簿。
     */
    @Schema(description = "FMS 账套编号（按账套筛凭证流水，见 V193）", example = "1")
    private Long accountSetId;

    /**
     * 账簿编号集合（内部字段，由 accountSetId 解析后填充；不接收前端传值，避免越权跨账套查询）
     */
    private List<Long> accountBookIds;

    @Schema(description = "凭证类型", example = "40")
    private Integer voucherType;

    @Schema(description = "状态", example = "10")
    private Integer status;

    @Schema(description = "会计期间编号", example = "1024")
    private Long periodId;

    @Schema(description = "来源会计事件 ID", example = "1024")
    private Long sourceEventId;

    @Schema(description = "来源单据类型", example = "PURCHASE_RECEIPT")
    private String sourceDocType;

    @Schema(description = "来源单据 ID", example = "2048")
    private Long sourceDocId;

    @Schema(description = "来源单据编号", example = "CGRK-202607-001")
    private String sourceDocNo;

    @Schema(description = "凭证日期起")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate voucherDateStart;

    @Schema(description = "凭证日期止")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate voucherDateEnd;

}
