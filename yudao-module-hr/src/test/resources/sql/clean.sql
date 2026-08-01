-- 每个单元测试结束后，清理 DB（与 create_tables.sql 的表保持一致）

DELETE FROM "hr_attendance";
DELETE FROM "hr_contract";
DELETE FROM "hr_department";
DELETE FROM "hr_employee";
DELETE FROM "hr_interview";
DELETE FROM "hr_job_posting";
DELETE FROM "hr_leave_balance";
DELETE FROM "hr_leave_request";
DELETE FROM "hr_leave_type";
DELETE FROM "hr_performance";
DELETE FROM "hr_position";
DELETE FROM "hr_resume";
DELETE FROM "hr_salary";
DELETE FROM "hr_social_insurance_base";
