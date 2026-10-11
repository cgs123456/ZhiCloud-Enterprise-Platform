-- V161 验证：hr_employee HRM 超集列 + 回填一致 + 组织树种子
-- 1) 新列全部存在（32 个 ADD COLUMN；emp_no 仅 MODIFY 不加列。
--    注：V161 正文头注误写「34 列」——正文哈希钉住不回改，真机实测新列恰 32 个、全部到位，断言以库为准）
SELECT CASE WHEN COUNT(*) = 32 THEN 'V161_SUPSET_COLS PASS'
            ELSE CONCAT('V161_SUPSET_COLS FAIL ', COUNT(*)) END AS check_result
  FROM information_schema.columns
 WHERE table_schema = DATABASE() AND table_name = 'hr_employee'
   AND column_name IN ('sex','birthday','entry_status','employee_status','type','entry_time','leave_time',
                       'user_id','leader_employee_id','country','nation','id_type','native_place','address',
                       'highest_education','probation','regular_time','post_name','post_level','work_city',
                       'work_address','work_detail_address','channel_id','candidate_id','company_age_start_time',
                       'salary_card_number','salary_card_area_id','salary_card_area_name','salary_card_bank_name',
                       'salary_card_bank_branch_name','social_security_number','accumulation_fund_number');

-- 2) 回填一致：sex 值域只允许 1/2/NULL（老 10/20 必须已映射）
SELECT CASE WHEN COUNT(*) = 0 THEN 'V161_SEX_DOMAIN PASS'
            ELSE CONCAT('V161_SEX_DOMAIN FAIL ', COUNT(*)) END AS check_result
  FROM `hr_employee`
 WHERE `deleted` = b'0' AND `sex` IS NOT NULL AND `sex` NOT IN (1, 2);

-- 3) 回填一致：entry_status ∈ 1..4 且与老 status 轴逻辑一致（老在职10 -> 新1，老离职20 -> 新4）
SELECT CASE WHEN COUNT(*) = 0 THEN 'V161_ENTRY_STATUS_PASS'
            ELSE CONCAT('V161_ENTRY_STATUS FAIL ', COUNT(*)) END AS check_result
  FROM `hr_employee`
 WHERE `deleted` = b'0'
   AND ((`status` = 10 AND `entry_status` <> 1) OR (`status` = 20 AND `entry_status` <> 4));

-- 4) employee_status 值域 1..8
SELECT CASE WHEN COUNT(*) = 0 THEN 'V161_EMP_STATUS_DOMAIN PASS'
            ELSE CONCAT('V161_EMP_STATUS_DOMAIN FAIL ', COUNT(*)) END AS check_result
  FROM `hr_employee`
 WHERE `deleted` = b'0' AND `employee_status` NOT BETWEEN 1 AND 8;

-- 5) 组织树种子：hr_department 存活行全部在 system_dept 存在且 id 同值
SELECT CASE WHEN (SELECT COUNT(*) FROM `hr_department` WHERE `deleted` = b'0')
             = (SELECT COUNT(*) FROM `system_dept` s
                 WHERE s.`deleted` = b'0'
                   AND EXISTS (SELECT 1 FROM `hr_department` d WHERE d.`deleted` = b'0' AND d.`id` = s.`id`))
        THEN 'V161_DEPT_SYNC PASS'
        ELSE 'V161_DEPT_SYNC FAIL' END AS check_result;

-- 6) 新列查询索引存在
SELECT CASE WHEN COUNT(DISTINCT INDEX_NAME) = 3 THEN 'V161_NEW_INDEXES PASS'
            ELSE CONCAT('V161_NEW_INDEXES FAIL ', COUNT(DISTINCT INDEX_NAME)) END AS check_result
  FROM information_schema.statistics
 WHERE table_schema = DATABASE() AND table_name = 'hr_employee'
   AND index_name IN ('idx_hr_employee_leader','idx_hr_employee_user','idx_hr_employee_entry_status');
