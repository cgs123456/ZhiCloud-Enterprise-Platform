package cn.zhicloud.module.hr.controller.admin.hrm.salary.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - HRM 薪资配置 VO（get/update 共用）")
@Data
public class HrmSalaryConfigVO {

    @Schema(description = "配置编号")
    private Long id;

    @Schema(description = "计薪周期开始日")
    private Integer cycleStartDay;

    @Schema(description = "计薪周期结束日")
    private Integer cycleEndDay;

    @Schema(description = "社保对应月份类型")
    private Integer socialSecurityMonthType;

    @Schema(description = "工资开始年份")
    private Integer startYear;

    @Schema(description = "工资开始月份")
    private Integer startMonth;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
