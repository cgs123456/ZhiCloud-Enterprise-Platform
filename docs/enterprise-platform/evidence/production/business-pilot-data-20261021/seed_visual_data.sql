USE zhicloud_sandbox_v140;
START TRANSACTION;

-- MES production trend: last 7 days, FINISHED feedback only (status=4)
DELETE FROM mes_pro_feedback WHERE tenant_id = 9001 AND code LIKE 'VIS-FB-%';
INSERT INTO mes_pro_feedback
  (code, type, feedback_time, work_order_id, feedback_quantity, qualified_quantity,
   unqualified_quantity, status, creator, create_time, tenant_id, deleted)
VALUES
  ('VIS-FB-01', 1, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 6 DAY), '10:00:00'), 5960004, 180, 172, 8, 4, 'demo', NOW(), 9001, b'0'),
  ('VIS-FB-02', 1, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 5 DAY), '11:20:00'), 5960005, 220, 210, 10, 4, 'demo', NOW(), 9001, b'0'),
  ('VIS-FB-03', 1, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 4 DAY), '09:40:00'), 5960006, 195, 188, 7, 4, 'demo', NOW(), 9001, b'0'),
  ('VIS-FB-04', 1, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 3 DAY), '14:10:00'), 5960007, 240, 228, 12, 4, 'demo', NOW(), 9001, b'0'),
  ('VIS-FB-05', 1, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 2 DAY), '10:30:00'), 5960009, 210, 202, 8, 4, 'demo', NOW(), 9001, b'0'),
  ('VIS-FB-06', 1, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 1 DAY), '15:00:00'), 5960010, 260, 248, 12, 4, 'demo', NOW(), 9001, b'0'),
  ('VIS-FB-07', 1, TIMESTAMP(CURDATE(), '09:50:00'), 5960011, 230, 221, 9, 4, 'demo', NOW(), 9001, b'0');

-- ERP sales summary: today / yesterday / this month
DELETE FROM erp_sale_out WHERE tenant_id = 9001 AND no LIKE 'VIS-SOUT-%';
INSERT INTO erp_sale_out
  (no, customer_id, out_time, status, total_count, total_amount, total_price,
   creator, create_time, tenant_id, deleted)
VALUES
  ('VIS-SOUT-001', 913000, TIMESTAMP(CURDATE(), '10:30:00'), 20, 12, 12800.00, 12800.00, 'demo', NOW(), 9001, b'0'),
  ('VIS-SOUT-002', 913001, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 1 DAY), '15:20:00'), 20, 8, 9600.00, 9600.00, 'demo', NOW(), 9001, b'0'),
  ('VIS-SOUT-003', 913002, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 3 DAY), '11:10:00'), 20, 20, 15400.00, 15400.00, 'demo', NOW(), 9001, b'0'),
  ('VIS-SOUT-004', 913003, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 8 DAY), '16:00:00'), 20, 6, 7200.00, 7200.00, 'demo', NOW(), 9001, b'0'),
  ('VIS-SOUT-005', 913004, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 15 DAY), '13:40:00'), 20, 24, 18600.00, 18600.00, 'demo', NOW(), 9001, b'0');

-- ERP purchase summary: today / yesterday / this month
DELETE FROM erp_purchase_in WHERE tenant_id = 9001 AND no LIKE 'VIS-PIN-%';
INSERT INTO erp_purchase_in
  (no, supplier_id, in_time, status, total_count, total_amount, total_price,
   creator, create_time, tenant_id, deleted)
VALUES
  ('VIS-PIN-001', 914000, TIMESTAMP(CURDATE(), '09:20:00'), 20, 16, 6800.00, 6800.00, 'demo', NOW(), 9001, b'0'),
  ('VIS-PIN-002', 914001, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 1 DAY), '14:10:00'), 20, 10, 5200.00, 5200.00, 'demo', NOW(), 9001, b'0'),
  ('VIS-PIN-003', 914002, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 3 DAY), '10:00:00'), 20, 22, 9100.00, 9100.00, 'demo', NOW(), 9001, b'0'),
  ('VIS-PIN-004', 914003, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 8 DAY), '15:30:00'), 20, 8, 4300.00, 4300.00, 'demo', NOW(), 9001, b'0'),
  ('VIS-PIN-005', 914004, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 15 DAY), '11:50:00'), 20, 30, 11200.00, 11200.00, 'demo', NOW(), 9001, b'0');

-- HRM monthly attendance: realistic current-month rows
UPDATE hr_attendance_month_record
SET attend_days = 21.00,
    actual_days = 21.00,
    late_minute = 0, late_count = 0,
    early_minute = 0, early_count = 0,
    misscard_count = 0,
    absenteeism_days = 0.00, absenteeism_minutes = 0,
    leave_days = 0.00, leave_minutes = 0,
    late_deduct_amount = 0.00, early_deduct_amount = 0.00,
    misscard_deduct_amount = 0.00, absenteeism_deduct_amount = 0.00,
    attendance_deduct_amount = 0.00,
    full_attendance = b'1'
WHERE tenant_id = 9001 AND year = 2026 AND month = 10 AND MOD(id, 3) <> 0;

UPDATE hr_attendance_month_record
SET attend_days = 21.00,
    actual_days = 20.50,
    late_minute = 15, late_count = 1,
    early_minute = 0, early_count = 0,
    misscard_count = 0,
    absenteeism_days = 0.50, absenteeism_minutes = 240,
    leave_days = 0.00, leave_minutes = 0,
    late_deduct_amount = 15.00, early_deduct_amount = 0.00,
    misscard_deduct_amount = 0.00, absenteeism_deduct_amount = 50.00,
    attendance_deduct_amount = 65.00,
    full_attendance = b'0'
WHERE tenant_id = 9001 AND year = 2026 AND month = 10 AND MOD(id, 3) = 0;

-- HRM portal: bind the demo admin user to an employee so the salary-slip endpoint is usable
UPDATE hr_employee
SET user_id = 900100
WHERE tenant_id = 9001 AND id = 925000;

-- QMS audit plan: a realistic mix of planned / executing / completed audits
DELETE FROM qms_audit_plan WHERE tenant_id = 9001 AND plan_no LIKE 'VIS-AP-%';
INSERT INTO qms_audit_plan
  (plan_no, audit_type, title, audit_standard, audit_scope, audit_purpose,
   lead_auditor_id, audit_start_date, audit_end_date, status, creator, create_time, tenant_id, deleted)
VALUES
  ('VIS-AP-001', 10, '2026年Q4质量管理体系内部审核', 'ISO 9001:2015', '生产部、质量部、仓储部', '验证质量管理体系符合性与运行有效性', 925000, DATE_ADD(CURDATE(), INTERVAL 5 DAY), DATE_ADD(CURDATE(), INTERVAL 6 DAY), 10, 'demo', NOW(), 9001, b'0'),
  ('VIS-AP-002', 20, '供应商过程审核（华东区）', 'IATF 16949:2016', '关键零部件供应商', '评估供应商过程能力与风险', 925001, DATE_SUB(CURDATE(), INTERVAL 1 DAY), CURDATE(), 20, 'demo', NOW(), 9001, b'0'),
  ('VIS-AP-003', 30, '2026年10月生产现场5S审核', '公司5S管理规范', '机加车间、装配车间', '检查现场标准化与目视化管理', 925002, DATE_SUB(CURDATE(), INTERVAL 10 DAY), DATE_SUB(CURDATE(), INTERVAL 9 DAY), 30, 'demo', NOW(), 9001, b'0'),
  ('VIS-AP-004', 40, 'IATF 16949 产品审核', 'IATF 16949:2016', 'A系列产品', '验证产品特性与客户要求一致性', 925003, DATE_ADD(CURDATE(), INTERVAL 15 DAY), DATE_ADD(CURDATE(), INTERVAL 16 DAY), 10, 'demo', NOW(), 9001, b'0'),
  ('VIS-AP-005', 50, 'ISO 14001 环境管理体系监督审核', 'ISO 14001:2015', '全厂环境管理区域', '确认环境管理体系持续符合性', 925004, CURDATE(), DATE_ADD(CURDATE(), INTERVAL 2 DAY), 20, 'demo', NOW(), 9001, b'0'),
  ('VIS-AP-006', 60, '客户投诉专项审核', '客户质量协议', '冲压线与终检工序', '追溯投诉根因并验证纠正措施', 925005, DATE_SUB(CURDATE(), INTERVAL 20 DAY), DATE_SUB(CURDATE(), INTERVAL 19 DAY), 30, 'demo', NOW(), 9001, b'0');

COMMIT;
