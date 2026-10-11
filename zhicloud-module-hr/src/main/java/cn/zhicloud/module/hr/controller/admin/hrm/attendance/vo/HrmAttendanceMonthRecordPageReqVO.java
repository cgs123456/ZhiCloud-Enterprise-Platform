package cn.zhicloud.module.hr.controller.admin.hrm.attendance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import cn.zhicloud.framework.common.pojo.PageParam;

@Schema(description = "管理后台 - HRM 月度考勤汇总分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class HrmAttendanceMonthRecordPageReqVO extends PageParam {

    @Schema(description = "年份")
    @NotNull(message = "年份不能为空")
    private Integer year;

    @Schema(description = "月份")
    @NotNull(message = "月份不能为空")
    private Integer month;

    @Schema(description = "关键字（员工姓名/工号，模糊匹配）")
    private String search;

    @Schema(description = "部门编号数组（含子部门）")
    private Long[] deptIds;

    @Schema(description = "是否全勤")
    private Boolean fullAttendance;

}
