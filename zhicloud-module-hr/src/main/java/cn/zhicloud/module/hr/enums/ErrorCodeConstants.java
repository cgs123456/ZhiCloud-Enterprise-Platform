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
    ErrorCode HR_LEAVE_BALANCE_NOT_EXISTS = new ErrorCode(1_050_007_005, "假期余额记录不存在");

    // ========== HR 社保（1-050-008-000） ==========
    ErrorCode HR_SOCIAL_INSURANCE_NOT_EXISTS = new ErrorCode(1_050_008_000, "社保基数记录不存在");
    ErrorCode HR_SOCIAL_INSURANCE_EXISTS = new ErrorCode(1_050_008_001, "该员工该年度社保基数已存在");

    // ========== HR 招聘（1-050-009-000） ==========
    ErrorCode HR_JOB_POSTING_NOT_EXISTS = new ErrorCode(1_050_009_000, "招聘职位不存在");
    ErrorCode HR_RESUME_NOT_EXISTS = new ErrorCode(1_050_009_001, "简历不存在");
    ErrorCode HR_INTERVIEW_NOT_EXISTS = new ErrorCode(1_050_009_002, "面试记录不存在");
    ErrorCode HR_RESUME_STATUS_INVALID = new ErrorCode(1_050_009_003, "简历状态非法，无法操作");

}
