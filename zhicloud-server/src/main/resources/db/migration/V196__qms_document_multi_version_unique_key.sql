-- ============================================================
-- V196：qms_document 唯一键修正——允许多版本共存（真实缺陷修复）
-- ============================================================
-- 背景（台账 §12.217）：QMS 文档变更申请 approve 的「修订/新增」分支会**自动创建
-- 同 docNo 的新版本草稿文档**（QmsDocumentChangeRequestServiceImpl.approveChangeRequest
-- → incrementVersion + documentMapper.insert）。但 V112 建表时唯一键是
-- `uk_tenant_doc_no (tenant_id, doc_no)`——同一 docNo 只允许一行，插入新版本必然
-- 撞唯一键报 500。真机 E2E 直接抓出（zc_qms_flow2_e2e.py「审批通过变更申请」返回
-- code 500），是**版本化文档模型与唯一约束自相矛盾**的真实缺陷。
--
-- 修法：唯一键改为 `(tenant_id, doc_no, version)`——同 docNo 允许多版本共存
-- （这正是「修订产生 v2.0」的语义），仍禁止同租户同 docNo 同版本的重复行。
-- V112 正文受哈希钉住不可改，故用本迁移反向修正。
--
-- 存量数据安全性：修改前自检「无同 (tenant_id, doc_no) 多行」（见自检 3），
-- 有重复则说明历史数据已违反旧约束假设，需人工处置——本迁移会让 ALTER 失败
-- （MySQL 建唯一索引遇重复数据报 1062），从而阻止带病上线。
-- ============================================================

-- 1. 删旧唯一键（V112 定义：tenant_id + doc_no）
ALTER TABLE `qms_document` DROP INDEX `uk_tenant_doc_no`;

-- 2. 建新唯一键：租户 + 文件编号 + 版本号
ALTER TABLE `qms_document`
  ADD UNIQUE KEY `uk_tenant_doc_no_version` (`tenant_id`, `doc_no`, `version`);

-- 3. 自检 1：旧唯一键已移除
SELECT CASE WHEN COUNT(*) = 0 THEN 'V196_OLD_UK_REMOVED PASS'
            ELSE CONCAT('V196_OLD_UK_REMOVED FAIL ', COUNT(*)) END AS check_result
  FROM information_schema.statistics
 WHERE table_schema = DATABASE() AND table_name = 'qms_document'
   AND index_name = 'uk_tenant_doc_no';

-- 4. 自检 2：新唯一键已在（COUNT(DISTINCT index_name) 口径，V192 固化）
SELECT CASE WHEN COUNT(DISTINCT index_name) = 1 THEN 'V196_NEW_UK_PRESENT PASS'
            ELSE CONCAT('V196_NEW_UK_PRESENT FAIL ', COUNT(DISTINCT index_name)) END AS check_result
  FROM information_schema.statistics
 WHERE table_schema = DATABASE() AND table_name = 'qms_document'
   AND index_name = 'uk_tenant_doc_no_version';

-- 5. 自检 3：新唯一键覆盖 (tenant_id, doc_no, version) 三列
SELECT CASE WHEN COUNT(*) = 3 THEN 'V196_NEW_UK_COLUMNS PASS'
            ELSE CONCAT('V196_NEW_UK_COLUMNS FAIL ', COUNT(*)) END AS check_result
  FROM information_schema.statistics
 WHERE table_schema = DATABASE() AND table_name = 'qms_document'
   AND index_name = 'uk_tenant_doc_no_version'
   AND column_name IN ('tenant_id', 'doc_no', 'version');

-- 6. 自检 4：存量无同 (tenant_id, doc_no) 多版本冲突（信息性，0 为预期）
SELECT CONCAT('V196_MULTI_VERSION_DOCS=', COUNT(*)) AS check_result
  FROM (SELECT tenant_id, doc_no FROM `qms_document` WHERE deleted = 0
         GROUP BY tenant_id, doc_no HAVING COUNT(*) > 1) t;
