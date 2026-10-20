-- V215：补齐 mp_message 的事件字段，修复 MpMessageDO 与 V155 建表漂移
--
-- 背景：MpMessageDO 将 event / eventKey 作为普通持久化字段，MyBatis-Plus 生成的
--       SELECT/INSERT 会带上 event、event_key；V155 建表未包含这两列，事件消息
--       分页查询或落库会触发 Unknown column。
--
-- 口径：事件消息仍需要保留事件类型与事件 Key，因此补列而不是把 DO 字段标成
--       exist = false。两列均可空，历史文本消息不受影响。
ALTER TABLE `mp_message`
    ADD COLUMN `event` varchar(64) DEFAULT NULL COMMENT '事件类型' AFTER `hq_music_url`,
    ADD COLUMN `event_key` varchar(255) DEFAULT NULL COMMENT '事件 Key' AFTER `event`;
