-- ============================================================
-- V121 人工复核脚本（只读，不修改业务数据）
--
-- 门禁：
--   bpm_process_instance_idempotency.claim_token 必须存在，且为
--   varchar(64) NULL DEFAULT NULL；legacy/blank 行允许保持 NULL，
--   应用必须对 token 不匹配的行 fail-closed。
-- ============================================================

SELECT 'V121_BPM_IDEMPOTENCY_CLAIM_TOKEN_COLUMN' AS check_name,
       CASE
           WHEN EXISTS (
               SELECT 1
                 FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = 'bpm_process_instance_idempotency'
                  AND column_name = 'claim_token'
                  AND LOWER(data_type) = 'varchar'
                  AND character_maximum_length = 64
                  AND is_nullable = 'YES'
                  AND column_default IS NULL
           )
           THEN 'PASS' ELSE 'FAIL'
       END AS verdict;
