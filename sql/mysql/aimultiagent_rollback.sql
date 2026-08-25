-- ======================================================================
-- zhicloud-module-ai-multiagent 回滚脚本（MySQL）
--
-- 对应正向脚本：sql/mysql/aimultiagent.sql（Task 9，待落地）
-- 对应文档：.trae/specs/upgrade-tech-stack-and-ai-native/rollback-strategy.md SubTask 13.2
--
-- ⚠️ 重要提示（Task 13 落地时 Task 9 尚未完成）：
--   1. 本脚本为前瞻性回滚模板，依据 Task 9 规划的 MultiAgent 模块表结构编制
--   2. 实际表名/索引名以 Task 9 落地的 aimultiagent.sql 为准，Task 9 完成后必须逐项核对
--   3. 预期表（参考 Task 9 规划：Supervisor-Worker 拓扑、Agent 定义、编排执行）：
--        - aimultiagent_agent        Agent 定义表（绑定 @Tool / 子流程）
--        - aimultiagent_topology     Supervisor-Worker 拓扑关系表
--        - aimultiagent_execution    编排执行记录表（含深度上限、Token 预算、熔断状态）
--   4. 所有 DROP 均使用 IF EXISTS，保证幂等可重复执行
--   5. ⚠️ 生产环境执行前必须先备份：
--        mysqldump -h <host> -u <user> -p <db> aimultiagent_agent aimultiagent_topology aimultiagent_execution > aimultiagent_backup.sql
-- ======================================================================

-- ----------------------------------------------------------------------
-- 1. 删除编排执行记录表（aimultiagent_execution）
--    含调用深度上限、Token 预算、熔断状态等运行时数据
-- ----------------------------------------------------------------------
DROP TABLE IF EXISTS `aimultiagent_execution`;

-- ----------------------------------------------------------------------
-- 2. 删除 Supervisor-Worker 拓扑关系表（aimultiagent_topology）
-- ----------------------------------------------------------------------
DROP TABLE IF EXISTS `aimultiagent_topology`;

-- ----------------------------------------------------------------------
-- 3. 删除 Agent 定义表（aimultiagent_agent）
--    绑定特定 @Tool 或子流程的 Agent 元数据
-- ----------------------------------------------------------------------
DROP TABLE IF EXISTS `aimultiagent_agent`;

-- ----------------------------------------------------------------------
-- 4. 回滚完成校验（可选，执行后应返回空集）
-- ----------------------------------------------------------------------
-- SELECT table_name FROM information_schema.tables
--   WHERE table_schema = DATABASE() AND table_name LIKE 'aimultiagent_%';

-- ======================================================================
-- TODO（Task 9 落地后补全）：
--   1. 核对 aimultiagent.sql 中实际建表语句，补充本脚本遗漏的表/索引
--   2. 若 aimultiagent.sql 包含初始化数据（如示例 Agent），回滚无需单独处理（随表删除）
--   3. 若引入了独立的数据源/schema，需补充对应的清理语句
-- ======================================================================
