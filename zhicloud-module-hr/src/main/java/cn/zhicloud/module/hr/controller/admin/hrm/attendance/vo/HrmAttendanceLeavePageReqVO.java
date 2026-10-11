package cn.zhicloud.module.hr.controller.admin.hrm.attendance.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import cn.zhicloud.framework.common.pojo.PageParam;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - HRM 考勤请假分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class HrmAttendanceLeavePageReqVO extends PageParam {

    @Schema(description = "关键字（员工姓名/工号，模糊匹配）")
    private String search;

    @Schema(description = "部门编号数组（含子部门）")
    private Long[] deptIds;

    @Schema(description = "请假类型数组")
    private String[] types;

    @Schema(description = "请假时间范围")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime[] timeRange;

}
