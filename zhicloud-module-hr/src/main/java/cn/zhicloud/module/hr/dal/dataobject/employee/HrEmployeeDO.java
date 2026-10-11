package cn.zhicloud.module.hr.dal.dataobject.employee;

import cn.zhicloud.framework.tenant.core.db.TenantBaseDO;
import cn.zhicloud.module.hr.enums.employee.HrEmployeeStatusEnum;
import cn.zhicloud.module.hr.enums.employee.HrEmploymentTypeEnum;
import cn.zhicloud.module.hr.enums.employee.HrGenderEnum;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * HR 员工档案 DO
 *
 * @author zhicloud
 */
@TableName("hr_employee")
@KeySequence("hr_employee_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HrEmployeeDO extends TenantBaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 工号
     */
    private String empNo;
    /**
     * 姓名
     */
    private String name;
    /**
     * 性别
     *
     * 枚举 {@link HrGenderEnum}
     */
    private Integer gender;
    /**
     * 出生日期
     */
    private LocalDate birthDate;
    /**
     * 身份证号
     */
    private String idCard;
    /**
     * 联系电话
     */
    private String phone;
    /**
     * 邮箱
     */
    private String email;
    /**
     * 部门 ID
     */
    private Long deptId;
    /**
     * 职位 ID
     */
    private Long positionId;
    /**
     * 入职日期
     */
    private LocalDate hireDate;
    /**
     * 离职日期
     */
    private LocalDate leaveDate;
    /**
     * 状态
     *
     * 枚举 {@link HrEmployeeStatusEnum}
     */
    private Integer status;
    /**
     * 用工类型
     *
     * 枚举 {@link HrEmploymentTypeEnum}
     */
    private Integer employmentType;
    /**
     * 备注
     */
    private String remark;
    /**
     * 排序
     */
    private Integer sort;

    // ========== HRM 融合：超集列（V161）。镜像字段不建列——用 @TableField 映射单一物理列 ==========

    /**
     * 性别（新口径）：1男 2女（对齐 system_user_sex）
     */
    private Integer sex;
    /**
     * 出生时间（由 birth_date 升级）
     */
    private LocalDateTime birthday;
    /**
     * 入职状态：1在职 2待入职 3待离职 4离职
     */
    private Integer entryStatus;
    /**
     * 员工状态（HRM 细颗粒）：1正式 2试用 3实习 4兼职 5劳务 6顾问 7返聘 8外包
     */
    private Integer employeeStatus;
    /**
     * 聘用形式：1正式 2非正式
     */
    private Integer type;
    /**
     * 入职时间（由 hire_date 升级）
     */
    private LocalDateTime entryTime;
    /**
     * 离职时间（由 leave_date 升级）
     */
    private LocalDateTime leaveTime;
    /**
     * 绑定后台用户编号（system_users.id）
     */
    private Long userId;
    /**
     * 直属上级员工编号（自引用 hr_employee.id）
     */
    private Long leaderEmployeeId;
    /**
     * 国家或地区
     */
    private String country;
    /**
     * 民族
     */
    private String nation;
    /**
     * 证件类型：1身份证 2港澳通行证 3台湾通行证 4护照 5其他
     */
    private Integer idType;
    /**
     * 籍贯
     */
    private String nativePlace;
    /**
     * 户籍地址
     */
    private String address;
    /**
     * 最高学历（hrm_employee_education 字典 1-12）
     */
    private Integer highestEducation;
    /**
     * 试用期（月）
     */
    private Integer probation;
    /**
     * 转正时间
     */
    private LocalDateTime regularTime;
    /**
     * 职位名称
     */
    private String postName;
    /**
     * 岗位职级
     */
    private String postLevel;
    /**
     * 工作城市
     */
    private String workCity;
    /**
     * 工作地点
     */
    private String workAddress;
    /**
     * 工作详细地址
     */
    private String workDetailAddress;
    /**
     * 招聘渠道编号（recruit 域）
     */
    private Long channelId;
    /**
     * 招聘候选人编号（建档来源）
     */
    private Long candidateId;
    /**
     * 司龄开始时间
     */
    private LocalDateTime companyAgeStartTime;
    /**
     * 工资卡号
     */
    private String salaryCardNumber;
    /**
     * 开户地区编号
     */
    private Long salaryCardAreaId;
    /**
     * 开户地区名称
     */
    private String salaryCardAreaName;
    /**
     * 银行名称
     */
    private String salaryCardBankName;
    /**
     * 开户支行名称
     */
    private String salaryCardBankBranchName;
    /**
     * 个人社保账号
     */
    private String socialSecurityNumber;
    /**
     * 个人公积金账号
     */
    private String accumulationFundNumber;
    /**
     * 工号别名（HRM jobNumber）。
     *
     * 融合纪律（台账 §12.184）：单一物理列 = emp_no，不建镜像列。
     * 本 getter 仅让 HRM 契约的 JSON 序列化带上 jobNumber；MyBatis-Plus 按字段
     * 建映射（无 jobNumber 字段即无 job_number 列引用），写入统一走 emp_no。
     */
    public String getJobNumber() {
        return empNo;
    }

    /**
     * 手机号别名（HRM mobile），映射单一物理列 phone，同上。
     */
    public String getMobile() {
        return phone;
    }

    /**
     * 证件号码别名（HRM idNumber），映射单一物理列 id_card，同上。
     */
    public String getIdNumber() {
        return idCard;
    }

}