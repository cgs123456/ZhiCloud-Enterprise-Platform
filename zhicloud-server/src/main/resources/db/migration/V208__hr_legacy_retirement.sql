-- V208: HRM 尾置收尾（C6①/台账 §12.264）——hr_employee 旧镜像列退役 + 旧 /hr 孤儿表清退 + 退役域字典清理
-- ============================================================
-- 授权与前置（条件已全部达成）：
--   * 台账 §12.185 待办：旧列退役迁移（/hrm 链已验证，待旧 /hr 页面/接口下线后执行）；
--   * V161 头注：旧列的清理退役（DROP/RENAME）= 后继迁移，在 /hrm 员工链验证通、旧页面/接口删除后执行；
--   * §12.201 旧 /hr 退役 done（菜单软删 V185、Java 死簇清除）；§12.184–199 HRM 接线 done。
-- 范围与依据：
--   一、hr_employee 6 旧镜像列 DROP：gender/birth_date/hire_date/leave_date/status/employment_type。
--       语义数据已由 V161 回填新列（sex/birthday/entry_time/leave_time/entry_status/employee_status/type），
--       旧列为无人读写的镜像冗余；单列索引 idx_status 随 DROP COLUMN 自动删除。
--   二、旧 /hr 孤儿表 9 张 DROP：hr_attendance、hr_contract、hr_interview、hr_job_posting、
--       hr_leave_balance、hr_performance、hr_resume、hr_salary、hr_social_insurance_base。
--       Java 栈零引用（全仓 grep 含注释）、H2 测试库已移除（§12.201）、写入方服务已删除、
--       V171/V174/V176/V178 头注明示待清理；hr_contract 域已由 hr_employee_contract（V162）替代。
--   三、退役域字典 6 型清理：hr_gender/hr_employee_status/hr_employment_type/hr_attendance_status/
--       hr_salary_status/hr_performance_grade（数据行 + 类型行）；hr_position_level 保留（/hrm/position 在用）。
-- 数据损失声明（执行前实测 2026-10-18，沙箱 zhicloud_sandbox_v140 @V207；全库无生产数据，tenant 仅 1/9001）：
--   随表删除的行数——hr_contract 40、hr_attendance 800、hr_salary 120、hr_performance 80、
--   hr_leave_balance 161；hr_interview/hr_job_posting/hr_resume/hr_social_insurance_base 0。
--   hr_employee 253 行（仅删列不动行）。删除不可逆；回滚 = 备份恢复（旧列值可由新列按 V161 映射反推，
--   旧表数据无等价物）。沙箱执行记录见台账 §12.264。
-- 保留面：hr_employee、hr_position、hr_department（V161 明示存档）、hr_leave_request（V126/V130 视图钉住）、
--   hr_leave_type（请求参照 + V131/V167 钉住）及字典 hr_position_level。
-- demo 工具链（快照/生成器/README 钉值）同步按 §12.264 登记为独立专项，本迁移不做。
-- MySQL 8.4。破坏性语句已登记 scripts/production/destructive_ddl_allowlist.json（文件×表名，双向 fail-closed）。

-- 一、hr_employee 旧镜像列退役（MySQL 8.4 不支持 DROP COLUMN IF EXISTS；Flyway 保证只执行一次）
ALTER TABLE `hr_employee`
    DROP COLUMN `gender`,
    DROP COLUMN `birth_date`,
    DROP COLUMN `hire_date`,
    DROP COLUMN `leave_date`,
    DROP COLUMN `status`,
    DROP COLUMN `employment_type`;

-- 二、旧 /hr 孤儿表清退（每表一条语句，与 allowlist 逐表登记双向一致）
DROP TABLE IF EXISTS `hr_attendance`;
DROP TABLE IF EXISTS `hr_contract`;
DROP TABLE IF EXISTS `hr_interview`;
DROP TABLE IF EXISTS `hr_job_posting`;
DROP TABLE IF EXISTS `hr_leave_balance`;
DROP TABLE IF EXISTS `hr_performance`;
DROP TABLE IF EXISTS `hr_resume`;
DROP TABLE IF EXISTS `hr_salary`;
DROP TABLE IF EXISTS `hr_social_insurance_base`;

-- 三、退役域字典清理（数据行 + 类型行；UI/Java/迁移零引用，仅 V83 种子）
DELETE FROM `system_dict_data` WHERE `dict_type` IN ('hr_gender','hr_employee_status','hr_employment_type','hr_attendance_status','hr_salary_status','hr_performance_grade');
DELETE FROM `system_dict_type` WHERE `type` IN ('hr_gender','hr_employee_status','hr_employment_type','hr_attendance_status','hr_salary_status','hr_performance_grade');

-- 四、内联自检（执行时直接读数；独立复核见 V208_verify.sql）
-- 自检 1：6 旧列清零
SELECT CASE WHEN COUNT(*) = 0 THEN 'V208_LEGACY_COLUMNS_DROPPED PASS'
            ELSE CONCAT('V208_LEGACY_COLUMNS_DROPPED FAIL ', COUNT(*)) END AS check_result
  FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'hr_employee'
   AND column_name IN ('gender','birth_date','hire_date','leave_date','status','employment_type');

-- 自检 2：idx_status 随列消失
SELECT CASE WHEN COUNT(*) = 0 THEN 'V208_IDX_STATUS_DROPPED PASS'
            ELSE CONCAT('V208_IDX_STATUS_DROPPED FAIL ', COUNT(*)) END AS check_result
  FROM information_schema.statistics WHERE table_schema = DATABASE() AND table_name = 'hr_employee'
   AND index_name = 'idx_status';

-- 自检 3：9 张孤儿表清零
SELECT CASE WHEN COUNT(*) = 0 THEN 'V208_ORPHAN_TABLES_DROPPED PASS'
            ELSE CONCAT('V208_ORPHAN_TABLES_DROPPED FAIL ', COUNT(*)) END AS check_result
  FROM information_schema.tables WHERE table_schema = DATABASE()
   AND table_name IN ('hr_attendance','hr_contract','hr_interview','hr_job_posting','hr_leave_balance',
                      'hr_performance','hr_resume','hr_salary','hr_social_insurance_base');

-- 自检 4：保留 5 表齐全
SELECT CASE WHEN COUNT(*) = 5 THEN 'V208_KEPT_TABLES_INTACT PASS'
            ELSE CONCAT('V208_KEPT_TABLES_INTACT FAIL ', COUNT(*)) END AS check_result
  FROM information_schema.tables WHERE table_schema = DATABASE()
   AND table_name IN ('hr_employee','hr_position','hr_department','hr_leave_request','hr_leave_type');

-- 自检 5：退役字典清零 + hr_position_level 保留
SELECT CASE WHEN COUNT(*) = 0 THEN 'V208_RETIRED_DICTS_CLEARED PASS'
            ELSE CONCAT('V208_RETIRED_DICTS_CLEARED FAIL ', COUNT(*)) END AS check_result
  FROM `system_dict_type` WHERE deleted = b'0'
   AND `type` IN ('hr_gender','hr_employee_status','hr_employment_type','hr_attendance_status','hr_salary_status','hr_performance_grade');
SELECT CASE WHEN COUNT(*) = 1 THEN 'V208_POSITION_LEVEL_DICT_KEPT PASS'
            ELSE CONCAT('V208_POSITION_LEVEL_DICT_KEPT FAIL ', COUNT(*)) END AS check_result
  FROM `system_dict_type` WHERE `type` = 'hr_position_level' AND deleted = b'0';

-- 自检 6：hr_employee 数据面不塌（存活行 > 0 且三个非空新轴无空值）
SELECT CASE WHEN COUNT(*) > 0 THEN CONCAT('V208_HR_EMPLOYEE_INTACT PASS ', COUNT(*))
            ELSE 'V208_HR_EMPLOYEE_INTACT FAIL' END AS check_result
  FROM `hr_employee` WHERE deleted = b'0'
   AND entry_status IS NOT NULL AND employee_status IS NOT NULL AND `type` IS NOT NULL;

-- 自检 7：hr_employee 新列齐全（7 个）
SELECT CASE WHEN COUNT(*) = 7 THEN 'V208_NEW_COLUMNS_PRESENT PASS'
            ELSE CONCAT('V208_NEW_COLUMNS_PRESENT FAIL ', COUNT(*)) END AS check_result
  FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = 'hr_employee'
   AND column_name IN ('sex','birthday','entry_status','employee_status','type','entry_time','leave_time');
