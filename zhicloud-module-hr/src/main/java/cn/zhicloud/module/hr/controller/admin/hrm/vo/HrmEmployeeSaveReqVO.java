package cn.zhicloud.module.hr.controller.admin.hrm.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - HRM 员工档案新增/修改 Request VO")
@Data
public class HrmEmployeeSaveReqVO {

    @Schema(description = "员工编号（修改时必填）")
    private Long id;

    @Schema(description = "员工姓名")
    @NotEmpty(message = "员工姓名不能为空")
    private String name;

    @Schema(description = "工号（在职必填，服务层校验）")
    private String jobNumber;

    @Schema(description = "后台用户编号")
    private Long userId;

    @Schema(description = "手机号")
    private String mobile;

    @Schema(description = "国家或地区")
    private String country;

    @Schema(description = "民族")
    private String nation;

    @Schema(description = "证件类型")
    private Integer idType;

    @Schema(description = "证件号码")
    private String idNumber;

    @Schema(description = "性别：1男 2女")
    private Integer sex;

    @Schema(description = "邮箱")
    private String email;

    @Schema(description = "籍贯")
    private String nativePlace;

    @Schema(description = "出生时间")
    private LocalDateTime birthday;

    @Schema(description = "户籍地址")
    private String address;

    @Schema(description = "最高学历（1-12）")
    private Integer highestEducation;

    @Schema(description = "部门编号")
    private Long deptId;

    @Schema(description = "直属上级员工编号")
    private Long leaderEmployeeId;

    @Schema(description = "入职状态：1在职 2待入职 3待离职 4离职")
    private Integer entryStatus;

    @Schema(description = "员工状态：1正式 2试用 3实习 4兼职 5劳务 6顾问 7返聘 8外包")
    private Integer status;

    @Schema(description = "聘用形式：1正式 2非正式")
    private Integer type;

    @Schema(description = "入职时间")
    private LocalDateTime entryTime;

    @Schema(description = "试用期（月）")
    private Integer probation;

    @Schema(description = "转正时间")
    private LocalDateTime regularTime;

    @Schema(description = "职位名称")
    private String postName;

    @Schema(description = "岗位职级")
    private String postLevel;

    @Schema(description = "工作城市")
    private String workCity;

    @Schema(description = "工作地点")
    private String workAddress;

    @Schema(description = "工作详细地址")
    private String workDetailAddress;

    @Schema(description = "招聘渠道编号")
    private Long channelId;

    @Schema(description = "招聘候选人编号")
    private Long candidateId;

    @Schema(description = "备注")
    private String remark;

}
