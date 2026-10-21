USE zhicloud_sandbox_v140;
START TRANSACTION;

-- Demo employee 925000: one November 2026 salary slip so the portal page has data.
DELETE FROM hr_salary_slip
WHERE tenant_id = 9001 AND employee_id = 925000 AND year = 2026 AND month = 11;

DELETE FROM hr_salary_month_employee_record
WHERE tenant_id = 9001 AND employee_id = 925000 AND year = 2026 AND month = 11;

INSERT INTO hr_salary_month_employee_record
  (month_record_id, employee_id, year, month, actual_work_day, need_work_day,
   expected_pay_salary, taxable_salary, personal_tax, real_pay_salary,
   performance_coefficient, option_values, creator, create_time, tenant_id, deleted)
VALUES
  (32, 925000, 2026, 11, 21.00, 21.00, 12000.00, 12000.00, 200.00, 11800.00, 1.00,
   '[{"code":10101,"name":"基本工资","value":9000},{"code":10102,"name":"绩效工资","value":3000},{"code":230101,"name":"个人所得税","value":-200}]',
   'demo', NOW(), 9001, b'0');

SET @rid = LAST_INSERT_ID();

INSERT INTO hr_salary_slip
  (send_record_id, month_employee_record_id, employee_id, year, month, read_status,
   creator, create_time, tenant_id, deleted)
VALUES
  (1, @rid, 925000, 2026, 11, 0, 'demo', NOW(), 9001, b'0');

COMMIT;
