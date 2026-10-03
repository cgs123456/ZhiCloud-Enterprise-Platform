-- ============================================================
-- V106 人工复核脚本（只读，不修改业务数据）
--
-- 门禁：
--   1. 两个权限码必须存在且各只有一条有效记录。
--   2. 权限类型必须是按钮（type=3）、状态启用（status=0）、未删除。
-- ============================================================

WITH expected_permission AS (
    SELECT 'infra:event-dlq:query' AS permission, '死信查询' AS name, 91 AS sort
    UNION ALL
    SELECT 'infra:event-dlq:replay', '死信重放', 92
),
actual_permission AS (
    SELECT permission, COUNT(*) AS active_count,
           MAX(name) AS name, MAX(sort) AS sort,
           MAX(type) AS type, MAX(status) AS status, MAX(parent_id) AS parent_id
    FROM system_menu
    WHERE deleted = 0
      AND permission IN ('infra:event-dlq:query', 'infra:event-dlq:replay')
    GROUP BY permission
)
SELECT e.permission,
       e.name AS expected_name,
       a.name AS actual_name,
       e.sort AS expected_sort,
       a.sort AS actual_sort,
       a.type,
       a.status,
       a.parent_id,
       CASE
           WHEN a.permission IS NULL THEN 'MISSING'
           WHEN a.active_count <> 1 OR a.name <> e.name OR a.sort <> e.sort
                OR a.type <> 3 OR a.status <> 0 OR a.parent_id <> 2 THEN 'WRONG'
           ELSE 'OK'
       END AS permission_verdict
FROM expected_permission e
LEFT JOIN actual_permission a ON a.permission = e.permission
ORDER BY e.sort;

WITH expected_permission AS (
    SELECT 'infra:event-dlq:query' AS permission
    UNION ALL
    SELECT 'infra:event-dlq:replay'
),
actual_permission AS (
    SELECT permission, COUNT(*) AS active_count,
           MAX(name) AS name, MAX(sort) AS sort,
           MAX(type) AS type, MAX(status) AS status, MAX(parent_id) AS parent_id
    FROM system_menu
    WHERE deleted = 0
      AND permission IN ('infra:event-dlq:query', 'infra:event-dlq:replay')
    GROUP BY permission
)
SELECT 'V106' AS check_name,
       CASE
           WHEN COUNT(*) = 2
                AND SUM(CASE
                            WHEN a.active_count = 1 AND a.name = e.name
                                 AND a.sort = e.sort AND a.type = 3
                                 AND a.status = 0 AND a.parent_id = 2
                            THEN 1 ELSE 0 END) = 2
           THEN 'PASS' ELSE 'FAIL'
       END AS verdict,
       COUNT(*) AS expected_permissions,
       SUM(CASE
               WHEN a.active_count = 1 AND a.name = e.name
                    AND a.sort = e.sort AND a.type = 3
                    AND a.status = 0 AND a.parent_id = 2
               THEN 1 ELSE 0 END) AS correct_permissions
FROM (
    SELECT 'infra:event-dlq:query' AS permission, '死信查询' AS name, 91 AS sort
    UNION ALL
    SELECT 'infra:event-dlq:replay', '死信重放', 92
) e
LEFT JOIN actual_permission a ON a.permission = e.permission;
