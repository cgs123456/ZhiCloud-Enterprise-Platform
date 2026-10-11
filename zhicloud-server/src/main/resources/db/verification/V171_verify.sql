-- V171 验证：HRM 招聘域 6 张表落库
SELECT CASE WHEN COUNT(*) = 6 THEN 'V171_RECRUIT_TABLES PASS'
            ELSE CONCAT('V171_RECRUIT_TABLES FAIL ', COUNT(*)) END AS check_result
  FROM `information_schema`.`tables`
 WHERE `table_schema` = DATABASE()
   AND `table_name` IN ('hr_recruit_post_type','hr_recruit_post','hr_recruit_channel',
                        'hr_recruit_candidate','hr_recruit_interview','hr_recruit_config');

-- 候选人 post_id / status 索引
SELECT CASE WHEN COUNT(*) = 2 THEN 'V171_CANDIDATE_IDX PASS'
            ELSE CONCAT('V171_CANDIDATE_IDX FAIL ', COUNT(*)) END AS check_result
  FROM `information_schema`.`statistics`
 WHERE `table_schema` = DATABASE() AND `table_name` = 'hr_recruit_candidate'
   AND `index_name` IN ('idx_hr_recruit_candidate_post','idx_hr_recruit_candidate_status')
   AND `seq_in_index` = 1;

-- 候选人简历附件 json 列
SELECT CASE WHEN COUNT(*) = 1 THEN 'V171_CANDIDATE_RESUME_JSON PASS'
            ELSE CONCAT('V171_CANDIDATE_RESUME_JSON FAIL ', COUNT(*)) END AS check_result
  FROM `information_schema`.`columns`
 WHERE `table_schema` = DATABASE() AND `table_name` = 'hr_recruit_candidate'
   AND `column_name` = 'resume_urls' AND `data_type` = 'json';

-- 职位面试官 json 列
SELECT CASE WHEN COUNT(*) = 1 THEN 'V171_POST_INTERVIEWER_JSON PASS'
            ELSE CONCAT('V171_POST_INTERVIEWER_JSON FAIL ', COUNT(*)) END AS check_result
  FROM `information_schema`.`columns`
 WHERE `table_schema` = DATABASE() AND `table_name` = 'hr_recruit_post'
   AND `column_name` = 'interview_employee_ids' AND `data_type` = 'json';
