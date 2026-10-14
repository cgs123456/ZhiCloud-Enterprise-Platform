-- V197 验证：TMS/OA/CRM 缺失域菜单播种

SELECT CASE WHEN COUNT(*) = 14 THEN 'V197_MENU_COUNT PASS'
            ELSE CONCAT('V197_MENU_COUNT FAIL ', COUNT(*)) END AS check_result
  FROM `system_menu`
 WHERE `deleted` = b'0' AND `type` = 2
   AND `component` IN ('tms/freight/index', 'tms/freight-reconciliation/index',
                       'tms/fleet-operation/index', 'tms/gps/index',
                       'oa/knowledge/comment/index', 'oa/knowledge/version/index',
                       'oa/meeting-reservation/index', 'oa/schedule/index',
                       'crm/clue/channel/index', 'crm/clue/poolConfig/index',
                       'crm/saleOrder/index', 'crm/workOrder/index',
                       'crm/followup/index', 'crm/performance/config/index');

SELECT CASE WHEN COUNT(DISTINCT permission) >= 35 THEN 'V197_PERMISSIONS PASS'
            ELSE CONCAT('V197_PERMISSIONS FAIL ', COUNT(DISTINCT permission)) END AS check_result
  FROM `system_menu`
 WHERE `deleted` = b'0'
   AND `permission` IN (
     'tms:freight:query', 'tms:freight:create', 'tms:freight:update', 'tms:freight:delete',
     'tms:freight:audit', 'tms:freight:settle',
     'tms:freight-reconciliation:query', 'tms:freight-reconciliation:create',
     'tms:freight-reconciliation:update', 'tms:freight-reconciliation:delete',
     'tms:fleet-operation:query', 'tms:fleet-operation:create',
     'tms:fleet-operation:update', 'tms:fleet-operation:delete',
     'tms:gps:query', 'tms:gps:report',
     'oa:knowledge-comment:query', 'oa:knowledge-comment:create',
     'oa:knowledge-comment:update', 'oa:knowledge-comment:delete',
     'oa:schedule:query', 'oa:schedule:create', 'oa:schedule:update', 'oa:schedule:delete',
     'crm:clue-channel:query', 'crm:clue-channel:create', 'crm:clue-channel:update', 'crm:clue-channel:delete',
     'crm:clue-pool-config:query', 'crm:clue-pool-config:update',
     'crm:sale-order:query', 'crm:sale-order:create', 'crm:sale-order:update',
     'crm:sale-order:delete', 'crm:sale-order:export',
     'crm:work-order:query', 'crm:work-order:create', 'crm:work-order:update',
     'crm:work-order:delete', 'crm:work-order:export',
     'crm:follow-up-record:create', 'crm:follow-up-record:delete',
     'crm:performance-config:query', 'crm:performance-config:create',
     'crm:performance-config:update', 'crm:performance-config:delete',
     'crm:statistics-product:query', 'crm:statistics-performance-target:query');

SELECT CASE WHEN COUNT(*) = 0 THEN 'V197_NO_ORPHAN PASS'
            ELSE CONCAT('V197_NO_ORPHAN FAIL ', COUNT(*)) END AS check_result
  FROM `system_menu` c
 WHERE c.`deleted` = b'0' AND ((c.`id` BETWEEN 3160 AND 3299) OR (c.`id` BETWEEN 3300 AND 3399))
   AND NOT EXISTS (SELECT 1 FROM `system_menu` p
                    WHERE p.`id` = c.`parent_id` AND p.`deleted` = b'0');

SELECT CASE WHEN COUNT(*) = 1 THEN 'V197_MEETING_QUERY_MENU PASS'
            ELSE CONCAT('V197_MEETING_QUERY_MENU FAIL ', COUNT(*)) END AS check_result
  FROM `system_menu`
 WHERE `deleted` = b'0' AND `type` = 2
   AND `permission` = 'oa:meeting-reservation:query';
