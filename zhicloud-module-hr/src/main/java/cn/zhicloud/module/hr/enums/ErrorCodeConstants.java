package cn.zhicloud.module.hr.enums;

import cn.zhicloud.framework.common.exception.ErrorCode;

/**
 * HR 错误码枚举类
 * <p>
 * HR 系统，使用 1-050-000-000 段
 */
public interface ErrorCodeConstants {

    // ========== HR 员工档案（1-050-000-000） ==========
    ErrorCode HR_EMPLOYEE_NOT_EXISTS = new ErrorCode(1_050_000_000, "员工不存在");
    ErrorCode HR_EMPLOYEE_NO_DUPLICATE = new ErrorCode(1_050_000_001, "工号已存在");
    ErrorCode HR_EMPLOYEE_ALREADY_RESIGNED = new ErrorCode(1_050_000_002, "员工已离职，无法操作");

    // ========== HR 部门（1-050-001-000） ==========
    ErrorCode HR_DEPARTMENT_NOT_EXISTS = new ErrorCode(1_050_001_000, "部门不存在");
    ErrorCode HR_DEPARTMENT_CODE_DUPLICATE = new ErrorCode(1_050_001_001, "部门编码已存在");
    ErrorCode HR_DEPARTMENT_HAS_CHILDREN = new ErrorCode(1_050_001_002, "存在子部门，无法删除");
    ErrorCode HR_DEPARTMENT_HAS_EMPLOYEES = new ErrorCode(1_050_001_003, "部门下存在员工，无法删除");

    // ========== HR 职位（1-050-002-000） ==========
    ErrorCode HR_POSITION_NOT_EXISTS = new ErrorCode(1_050_002_000, "职位不存在");
    ErrorCode HR_POSITION_CODE_DUPLICATE = new ErrorCode(1_050_002_001, "职位编码已存在");

    // ========== HR 考勤（1-050-003-000） ==========
    ErrorCode HR_ATTENDANCE_NOT_EXISTS = new ErrorCode(1_050_003_000, "考勤记录不存在");

    // ========== HR 薪资（1-050-004-000） ==========
    ErrorCode HR_SALARY_NOT_EXISTS = new ErrorCode(1_050_004_000, "薪资记录不存在");
    ErrorCode HR_SALARY_STATUS_INVALID = new ErrorCode(1_050_004_001, "薪资状态非法，无法操作");
    ErrorCode HR_SALARY_ALREADY_APPROVED = new ErrorCode(1_050_004_002, "薪资已审核，无法修改");
    ErrorCode HR_SALARY_ALREADY_CALCULATED = new ErrorCode(1_050_004_003, "该员工当月薪资已计算，请勿重复计算");

    // ========== HR 绩效（1-050-005-000） ==========
    ErrorCode HR_PERFORMANCE_NOT_EXISTS = new ErrorCode(1_050_005_000, "绩效记录不存在");

    // ========== HR 合同（1-050-006-000） ==========
    ErrorCode HR_CONTRACT_NOT_EXISTS = new ErrorCode(1_050_006_000, "合同不存在");
    ErrorCode HR_CONTRACT_NO_EXISTS = new ErrorCode(1_050_006_001, "合同编号已存在");
    ErrorCode HR_CONTRACT_ALREADY_TERMINATED = new ErrorCode(1_050_006_002, "合同已终止，无法操作");

    // ========== HR 假期（1-050-007-000） ==========
    ErrorCode HR_LEAVE_TYPE_NOT_EXISTS = new ErrorCode(1_050_007_000, "假期类型不存在");
    ErrorCode HR_LEAVE_TYPE_CODE_DUPLICATE = new ErrorCode(1_050_007_001, "假期类型编码已存在");
    ErrorCode HR_LEAVE_BALANCE_NOT_ENOUGH = new ErrorCode(1_050_007_002, "假期余额不足");
    ErrorCode HR_LEAVE_REQUEST_NOT_EXISTS = new ErrorCode(1_050_007_003, "请假单不存在");
    ErrorCode HR_LEAVE_REQUEST_STATUS_INVALID = new ErrorCode(1_050_007_004, "请假单状态非法，无法操作");
    ErrorCode HR_LEAVE_REQUEST_ALREADY_SUBMITTED = new ErrorCode(1_050_007_006, "请假单已提交流程审批，不能重复提交");
    ErrorCode HR_LEAVE_BALANCE_NOT_EXISTS = new ErrorCode(1_050_007_005, "假期余额记录不存在");

    // ========== HR 社保（1-050-008-000） ==========
    ErrorCode HR_SOCIAL_INSURANCE_NOT_EXISTS = new ErrorCode(1_050_008_000, "社保基数记录不存在");
    ErrorCode HR_SOCIAL_INSURANCE_EXISTS = new ErrorCode(1_050_008_001, "该员工该年度社保基数已存在");

    // ========== HR 招聘（1-050-009-000） ==========
    ErrorCode HR_JOB_POSTING_NOT_EXISTS = new ErrorCode(1_050_009_000, "招聘职位不存在");
    ErrorCode HR_RESUME_NOT_EXISTS = new ErrorCode(1_050_009_001, "简历不存在");
    ErrorCode HR_INTERVIEW_NOT_EXISTS = new ErrorCode(1_050_009_002, "面试记录不存在");
    ErrorCode HR_RESUME_STATUS_INVALID = new ErrorCode(1_050_009_003, "简历状态非法，无法操作");

    // ========== HRM 员工档案融合（1-050-015-000） ==========
    ErrorCode HRM_EMPLOYEE_NOT_EXISTS = new ErrorCode(1_050_015_000, "员工不存在");
    ErrorCode HRM_EMPLOYEE_JOB_NUMBER_REQUIRED = new ErrorCode(1_050_015_001, "在职员工的工号不能为空");
    ErrorCode HRM_EMPLOYEE_JOB_NUMBER_DUPLICATE = new ErrorCode(1_050_015_002, "工号已存在");
    ErrorCode HRM_EMPLOYEE_NOT_PENDING_ENTRY = new ErrorCode(1_050_015_003, "员工不是待入职状态，无法确认入职");
    ErrorCode HRM_EMPLOYEE_NOT_LEFT = new ErrorCode(1_050_015_004, "员工不是离职状态，无法办理再入职");
    ErrorCode HRM_EMPLOYEE_ALREADY_LEFT = new ErrorCode(1_050_015_005, "员工已离职，无法操作");
    ErrorCode HRM_EMPLOYEE_USER_BOUND = new ErrorCode(1_050_015_006, "该后台用户已建立员工档案");
    ErrorCode HRM_EMPLOYEE_STATUS_INVALID = new ErrorCode(1_050_015_007, "员工状态非法，无法操作");
    ErrorCode HRM_EMPLOYEE_FIELD_CONFIG_NOT_EXISTS = new ErrorCode(1_050_015_008, "字段配置不存在");
    ErrorCode HRM_EMPLOYEE_QUIT_INFO_NOT_EXISTS = new ErrorCode(1_050_015_009, "离职信息不存在");
    ErrorCode HRM_EMPLOYEE_SUB_RECORD_NOT_EXISTS = new ErrorCode(1_050_015_010, "记录不存在");
    ErrorCode HRM_EMPLOYEE_NOT_IN_SERVICE = new ErrorCode(1_050_015_011, "员工不是在职状态，无法操作");

    // ========== HRM 考勤（1-050-016-000） ==========
    ErrorCode HRM_ATTENDANCE_CLOCK_NOT_EXISTS = new ErrorCode(1_050_016_000, "打卡记录不存在");
    ErrorCode HRM_ATTENDANCE_GROUP_NOT_EXISTS = new ErrorCode(1_050_016_001, "考勤组不存在");
    ErrorCode HRM_ATTENDANCE_GROUP_NAME_DUPLICATE = new ErrorCode(1_050_016_002, "考勤组名称已存在");
    ErrorCode HRM_ATTENDANCE_HOLIDAY_NOT_EXISTS = new ErrorCode(1_050_016_003, "节假日不存在");
    ErrorCode HRM_ATTENDANCE_HOLIDAY_DATE_DUPLICATE = new ErrorCode(1_050_016_004, "该日期已配置节假日");
    ErrorCode HRM_ATTENDANCE_LEAVE_NOT_EXISTS = new ErrorCode(1_050_016_005, "请假记录不存在");
    ErrorCode HRM_ATTENDANCE_DATE_INVALID = new ErrorCode(1_050_016_006, "日期格式非法，应为 yyyy-MM-dd");

    // ========== HRM 保险（1-050-017-000） ==========
    ErrorCode HRM_INSURANCE_SCHEME_NOT_EXISTS = new ErrorCode(1_050_017_000, "社保方案不存在");
    ErrorCode HRM_INSURANCE_SCHEME_NAME_DUPLICATE = new ErrorCode(1_050_017_001, "社保方案名称已存在");
    ErrorCode HRM_INSURANCE_SCHEME_USED = new ErrorCode(1_050_017_002, "方案已被员工参保引用，无法删除");
    ErrorCode HRM_INSURANCE_STANDARD_TYPE_NOT_EXISTS = new ErrorCode(1_050_017_003, "参保类型不存在");
    ErrorCode HRM_INSURANCE_STANDARD_PROJECT_NOT_EXISTS = new ErrorCode(1_050_017_004, "参保项目不存在");
    ErrorCode HRM_INSURANCE_AREA_REQUIRED = new ErrorCode(1_050_017_005, "参保地区不能为空");

    // ========== HRM 招聘（1-050-018-000） ==========
    ErrorCode HRM_RECRUIT_POST_NOT_EXISTS = new ErrorCode(1_050_018_000, "招聘职位不存在");
    ErrorCode HRM_RECRUIT_CHANNEL_NOT_EXISTS = new ErrorCode(1_050_018_001, "招聘渠道不存在");
    ErrorCode HRM_RECRUIT_CHANNEL_NAME_DUPLICATE = new ErrorCode(1_050_018_002, "招聘渠道名称已存在");
    ErrorCode HRM_RECRUIT_CHANNEL_TRANSFER_REQUIRED = new ErrorCode(1_050_018_003, "系统内置渠道删除需指定承接渠道");
    ErrorCode HRM_RECRUIT_CHANNEL_TRANSFER_INVALID = new ErrorCode(1_050_018_004, "承接渠道不能是自身");
    ErrorCode HRM_RECRUIT_CANDIDATE_NOT_EXISTS = new ErrorCode(1_050_018_005, "候选人不存在");
    ErrorCode HRM_RECRUIT_CANDIDATE_ALREADY_JOINED = new ErrorCode(1_050_018_006, "候选人已入职，无法重复转入");
    ErrorCode HRM_RECRUIT_INTERVIEW_NOT_EXISTS = new ErrorCode(1_050_018_007, "面试记录不存在");

}
