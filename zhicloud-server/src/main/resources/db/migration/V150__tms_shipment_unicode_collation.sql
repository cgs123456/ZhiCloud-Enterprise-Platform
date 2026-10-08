-- V150：tms_shipment 表级 collation 统一。
-- 背景：tms_shipment 使用 utf8mb4_0900_ai_ci，而相关业务表多为 utf8mb4_unicode_ci，
-- 跨表字符串 JOIN 需要显式 COLLATE。此处只做定向收敛，不做全库统一。
ALTER TABLE `tms_shipment` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
