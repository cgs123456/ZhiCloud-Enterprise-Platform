-- 每个单元测试结束后，清理 DB（与 create_tables.sql 的表保持一致）

DELETE FROM "oa_announcement";
DELETE FROM "oa_approval_template";
DELETE FROM "oa_document";
DELETE FROM "oa_document_attachment";
DELETE FROM "oa_knowledge_article";
DELETE FROM "oa_knowledge_category";
DELETE FROM "oa_knowledge_comment";
DELETE FROM "oa_knowledge_version";
DELETE FROM "oa_meeting_reservation";
DELETE FROM "oa_meeting_room";
DELETE FROM "oa_reimburse";
DELETE FROM "oa_reimburse_item";
DELETE FROM "oa_schedule";
