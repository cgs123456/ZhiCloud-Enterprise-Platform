package cn.zhicloud.module.hr.controller.admin.hrm.portal.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 员工自助端 - 工资条 VO
 *
 * 对齐前端 SalarySlipVO 契约。
 *
 * @author zhicloud
 */
@Schema(description = "员工自助端 - 工资条 VO")
@Data
public class HrmPortalSalarySlipVO {

    @Schema(description = "工资条编号")
    private Long id;

    @Schema(description = "工资条批次编号")
    private Long sendRecordId;

    @Schema(description = "员工月度工资记录编号")
    private Long monthEmployeeRecordId;

    @Schema(description = "员工编号")
    private Long employeeId;

    @Schema(description = "年份")
    private Integer year;

    @Schema(description = "月份")
    private Integer month;

    @Schema(description = "阅读状态：0未读 1已读")
    private Integer readStatus;

    @Schema(description = "实发工资")
    private BigDecimal realPaySalary;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "薪资项列表")
    private List<HrmPortalSalarySlipOptionVO> options;

}
