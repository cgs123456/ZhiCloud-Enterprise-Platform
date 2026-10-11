package cn.zhicloud.module.hr.controller.admin.hrm.portal.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 员工自助端 - 未读工资条概况 VO
 *
 * 对齐前端 SalarySlipUnreadSummaryVO 契约。
 *
 * @author zhicloud
 */
@Schema(description = "员工自助端 - 未读工资条概况 VO")
@Data
public class HrmPortalSalarySlipUnreadSummaryVO {

    @Schema(description = "未读工资条数量")
    private Long unreadCount;

    @Schema(description = "最新未读工资条年份")
    private Integer latestYear;

    @Schema(description = "最新未读工资条月份")
    private Integer latestMonth;

}
