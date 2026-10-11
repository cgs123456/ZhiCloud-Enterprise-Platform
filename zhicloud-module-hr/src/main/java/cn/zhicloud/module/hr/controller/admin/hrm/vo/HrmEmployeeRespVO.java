package cn.zhicloud.module.hr.controller.admin.hrm.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - HRM 员工档案 Response VO")
@Data
public class HrmEmployeeRespVO {

    @Schema(description = "员工编号")
    private Long id;

    @Schema(description = "员工姓名")
    private String name;

    @Schema(description = "工号")
    private String jobNumber;

    @Schema(description = "后台用户编号")
    private Long userId;

    @Schema(description = "后台用户昵称")
    private String userNickname;

    @Schema(description = "手机号")
    private String mobile;

    @Schema(description = "国家或地区")
    private String country;

    @Schema(description = "民族")
    private String nation;

    @Schema(description = "证件类型：1身份证 2港澳通行证 3台湾通行证 4护照 5其他")
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

    @Schema(description = "年龄（由 birthday 计算）")
    private Integer age;

    @Schema(description = "户籍地址")
    private String address;

    @Schema(description = "最高学历（1-12）")
    private Integer highestEducation;

    @Schema(description = "部门编号")
    private Long deptId;

    @Schema(description = "部门名称")
    private String deptName;

    @Schema(description = "直属上级员工编号")
    private Long leaderEmployeeId;

    @Schema(description = "直属上级员工姓名")
    private String leaderEmployeeName;

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

    @Schema(description = "离职时间")
    private LocalDateTime leaveTime;

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

    @Schema(description = "招聘渠道名称")
    private String channelName;

    @Schema(description = "司龄开始时间")
    private LocalDateTime companyAgeStartTime;

    @Schema(description = "司龄（年，由 companyAgeStartTime 计算）")
    private Integer companyAge;

    @Schema(description = "招聘候选人编号")
    private Long candidateId;

    @Schema(description = "银行卡号")
    private String salaryCardNumber;

    @Schema(description = "开户地区编号")
    private Long salaryCardAreaId;

    @Schema(description = "开户地区名称")
    private String salaryCardAreaName;

    @Schema(description = "银行名称")
    private String salaryCardBankName;

    @Schema(description = "开户支行名称")
    private String salaryCardBankBranchName;

    @Schema(description = "个人社保账号")
    private String socialSecurityNumber;

    @Schema(description = "个人公积金账号")
    private String accumulationFundNumber;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
