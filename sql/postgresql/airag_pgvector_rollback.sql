-- ======================================================================
-- yudao-module-ai-rag 回滚脚本（PostgreSQL + pgvector）
--
-- 对应正向脚本：sql/postgresql/airag_pgvector.sql（Task 8）
-- 对应文档：.trae/specs/upgrade-tech-stack-and-ai-native/rollback-strategy.md SubTask 13.2
--
-- 说明：
--   1. 本脚本为 airag_pgvector.sql 的 down 操作，回滚 Task 8 的 RAG 模块建表
--   2. 所有 DROP 均使用 IF EXISTS，保证幂等可重复执行
--   3. 回滚顺序：先删索引 → 再删表 → 最后按需删除 pgvector 扩展
--   4. ⚠️ 生产环境执行前必须先备份：pg_dump -h <host> -U <user> -d <db> -t airag_knowledge -t airag_document -t airag_vector_store > airag_backup.sql
--   5. pgvector 扩展默认不删除（可能被其他业务依赖），仅在确认无其他依赖时手动取消注释执行
-- ======================================================================

-- ----------------------------------------------------------------------
-- 1. 删除向量存储表（airag_vector_store）
--
-- 说明：正向脚本中该表由 Spring AI PgVectorStore 通过 initializeSchema=true 自动创建，
--      回滚时同样需要手动删除（PgVectorStore 不会自动 drop）。
-- ----------------------------------------------------------------------
DROP TABLE IF EXISTS airag_vector_store CASCADE;

-- 删除向量存储表的 HNSW 索引（CASCADE 删表时索引自动级联删除，此处显式删除仅为语义清晰）
-- DROP INDEX IF EXISTS idx_airag_vector_store_embedding;

-- ----------------------------------------------------------------------
-- 2. 删除文档表（airag_document）及其索引
-- ----------------------------------------------------------------------
DROP INDEX IF EXISTS idx_airag_document_tenant_id;
DROP INDEX IF EXISTS idx_airag_document_status;
DROP INDEX IF EXISTS idx_airag_document_knowledge_id;

DROP TABLE IF EXISTS airag_document CASCADE;

-- ----------------------------------------------------------------------
-- 3. 删除知识库表（airag_knowledge）
-- ----------------------------------------------------------------------
DROP TABLE IF EXISTS airag_knowledge CASCADE;

-- ----------------------------------------------------------------------
-- 4. 删除 pgvector 扩展（可选，默认不执行）
--
-- ⚠️ 警告：DROP EXTENSION 会级联删除所有依赖 vector 类型的表/列。
--          仅在确认本数据库无其他业务使用 pgvector 时才取消注释执行！
--          多数生产场景下应保留扩展，仅删业务表即可完成回滚。
-- ----------------------------------------------------------------------
-- DROP EXTENSION IF EXISTS vector CASCADE;

-- ----------------------------------------------------------------------
-- 5. 回滚完成校验（可选，执行后应返回空集）
-- ----------------------------------------------------------------------
-- SELECT tablename FROM pg_tables WHERE schemaname = 'public' AND tablename LIKE 'airag_%';
-- SELECT extname FROM pg_extension WHERE extname = 'vector';
