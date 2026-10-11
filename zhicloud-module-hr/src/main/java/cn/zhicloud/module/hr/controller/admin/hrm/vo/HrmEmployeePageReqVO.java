package cn.zhicloud.module.hr.controller.admin.hrm.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import cn.zhicloud.framework.common.pojo.PageParam;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - HRM 员工档案分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class HrmEmployeePageReqVO extends PageParam {

    @Schema(description = "员工姓名，模糊匹配")
    private String name;

    @Schema(description = "手机号，模糊匹配")
    private String mobile;

    @Schema(description = "性别：1男 2女")
    private Integer sex;

    @Schema(description = "入职时间范围")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime[] entryTime;

    @Schema(description = "工号，模糊匹配")
    private String jobNumber;

    @Schema(description = "部门编号（含子部门）")
    private Long deptId;

    @Schema(description = "直属上级员工编号")
    private Long leaderEmployeeId;

    @Schema(description = "职位名称，模糊匹配")
    private String postName;

    @Schema(description = "转正时间范围")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime[] regularTime;

    @Schema(description = "工作地点，模糊匹配")
    private String workAddress;

    @Schema(description = "招聘渠道编号")
    private Long channelId;

    @Schema(description = "聘用形式：1正式 2非正式")
    private Integer type;

    @Schema(description = "入职状态：1在职 2待入职 3待离职 4离职")
    private Integer entryStatus;

    @Schema(description = "员工状态：1正式 2试用 3实习 4兼职 5劳务 6顾问 7返聘 8外包")
    private Integer status;

    @Schema(description = "状态页签：11在职 12全职 13待入职 14待离职 15已离职")
    private Integer statusCategory;

}
