package cn.zhicloud.module.hr.controller.admin.hrm.salary.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - HRM 员工调薪记录 VO")
@Data
public class HrmSalaryEmployeeChangeRecordVO {

    @Schema(description = "调薪记录编号")
    private Long id;

    @Schema(description = "员工编号")
    private Long employeeId;

    @Schema(description = "记录类型")
    private Integer recordType;

    @Schema(description = "调薪原因")
    private Integer changeReason;

    @Schema(description = "生效时间")
    private LocalDateTime effectTime;

    @Schema(description = "调整前转正工资")
    private BigDecimal beforeTotal;

    @Schema(description = "调整后转正工资")
    private BigDecimal afterTotal;

    @Schema(description = "调整前试用期工资")
    private BigDecimal probationBeforeTotal;

    @Schema(description = "调整后试用期工资")
    private BigDecimal probationAfterTotal;

    @Schema(description = "状态：1待生效 2已取消")
    private Integer status;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "转正工资项")
    private List<SalaryOptionValueVO> salaryOptions;

    @Schema(description = "试用期工资项")
    private List<SalaryOptionValueVO> probationSalaryOptions;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
