-- ============================================================
-- V144：校正 V142 自检口径（V142 正文已应用，按仓规不改正文）
-- ============================================================
-- V142 的真机自检报了 `V142_MDM_MENUS_SEEDED FAIL 权限点不齐`，但**数据是对的**：
-- 四条 system_menu 行（7900 菜单 + 7901/7902/7903 按钮）全部落库。
-- 失败是自检口径写错：它用 `COUNT(*) = 3` 统计三个权限点，而
-- `system:mdm-cross-ref:query` 同时出现在菜单行(7900)与按钮行(7901)，总数是 4——
-- 「菜单+按钮同权限」是 V134 起的既有形态，用等值计数必然误判。
--
-- 已应用的迁移正文受哈希契约钉住，不能回改 V142，故以本迁移补一条正确断言，
-- 并把口径问题记在此处（与 V130/V143 的"改口径要新增迁移"一致）。
-- ============================================================

-- 校正自检：三个权限点**各自至少存在一行**（不写等值计数）
SELECT CASE WHEN (SELECT COUNT(*) FROM `system_menu` WHERE deleted = b'0'
                   AND permission = 'system:mdm-cross-ref:query') >= 1
             AND (SELECT COUNT(*) FROM `system_menu` WHERE deleted = b'0'
                   AND permission = 'system:mdm-cross-ref:create') >= 1
             AND (SELECT COUNT(*) FROM `system_menu` WHERE deleted = b'0'
                   AND permission = 'system:mdm-cross-ref:delete') >= 1
            THEN 'V144_MDM_PERMISSIONS_PRESENT PASS'
            ELSE 'V144_MDM_PERMISSIONS_PRESENT FAIL 权限点不齐，接口会恒 403' END AS check_result;

-- 复核菜单挂在「系统管理」目录下（V142 的意图，重新确认一次）
SELECT CASE WHEN (SELECT COUNT(*) FROM `system_menu`
                   WHERE deleted = b'0' AND parent_id = 1 AND type = 2
                     AND permission = 'system:mdm-cross-ref:query') = 1
            THEN 'V144_MENU_UNDER_SYSTEM PASS'
            ELSE 'V144_MENU_UNDER_SYSTEM FAIL' END AS check_result;
