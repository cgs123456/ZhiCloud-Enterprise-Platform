-- ============================================================
-- OA 办公自动化模块初始化 DDL
-- 覆盖：报销管理、会议室管理、公文管理
-- 幂等语法：CREATE TABLE IF NOT EXISTS
-- ============================================================

-- ----------------------------
-- 1. 报销单 oa_reimburse
-- ----------------------------
CREATE TABLE IF NOT EXISTS oa_reimburse (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  no VARCHAR(64) NOT NULL COMMENT '报销单号',
  reimburse_name VARCHAR(255) NOT NULL COMMENT '报销主题',
  applicant_user_id BIGINT NOT NULL COMMENT '申请人 ID',
  dept_id BIGINT COMMENT '部门 ID',
  reimburse_type TINYINT NOT NULL COMMENT '10 差旅 20 招待 30 办公 40 交通 50 其他',
  reimburse_date DATE COMMENT '报销日期',
  total_amount DECIMAL(20,4) COMMENT '报销总额',
  payment_status TINYINT DEFAULT 10 COMMENT '10 未支付 20 部分支付 30 已支付',
  process_instance_id VARCHAR(64) COMMENT '工作流编号',
  status TINYINT DEFAULT 10 COMMENT '10 草稿 20 审批中 30 已通过 40 已驳回 50 已撤销',
  remark VARCHAR(500),
  creator VARCHAR(64), create_time DATETIME, updater VARCHAR(64), update_time DATETIME, deleted BIT DEFAULT 0,
  tenant_id BIGINT DEFAULT 0,
  UNIQUE KEY uk_reimburse_no (no)
);
CREATE INDEX idx_reimburse_applicant ON oa_reimburse(applicant_user_id);
CREATE INDEX idx_reimburse_status ON oa_reimburse(status);

-- ----------------------------
-- 2. 报销明细 oa_reimburse_item
-- ----------------------------
CREATE TABLE IF NOT EXISTS oa_reimburse_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  reimburse_id BIGINT NOT NULL COMMENT '报销单 ID',
  subject VARCHAR(128) NOT NULL COMMENT '科目（如机票/住宿/餐饮）',
  occurrence_date DATE COMMENT '发生日期',
  amount DECIMAL(20,4) COMMENT '金额',
  invoice_count INT DEFAULT 0 COMMENT '发票张数',
  description VARCHAR(500) COMMENT '说明',
  creator VARCHAR(64), create_time DATETIME, updater VARCHAR(64), update_time DATETIME, deleted BIT DEFAULT 0,
  tenant_id BIGINT DEFAULT 0
);
-- 幂等新增索引：oa_reimburse_item.idx_reimburse_item_reimburse
SET @zc_sql := IF(EXISTS(SELECT 1 FROM information_schema.STATISTICS
                         WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'oa_reimburse_item' AND INDEX_NAME = 'idx_reimburse_item_reimburse'),
                  'DO 0',
                  'ALTER TABLE `oa_reimburse_item` ADD INDEX `idx_reimburse_item_reimburse` (reimburse_id)');
PREPARE zc_stmt FROM @zc_sql;
EXECUTE zc_stmt;
DEALLOCATE PREPARE zc_stmt;

-- ----------------------------
-- 3. 会议室 oa_meeting_room
-- ----------------------------
CREATE TABLE IF NOT EXISTS oa_meeting_room (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(128) NOT NULL COMMENT '会议室名称',
  location VARCHAR(255) COMMENT '位置',
  capacity INT COMMENT '容纳人数',
  equipment VARCHAR(500) COMMENT '设备配置（如投影/白板/视频会议，逗号分隔）',
  status TINYINT DEFAULT 10 COMMENT '10 可用 20 维修中 30 已停用',
  remark VARCHAR(500),
  creator VARCHAR(64), create_time DATETIME, updater VARCHAR(64), update_time DATETIME, deleted BIT DEFAULT 0,
  tenant_id BIGINT DEFAULT 0
);
CREATE INDEX idx_meeting_room_status ON oa_meeting_room(status);

-- ----------------------------
-- 4. 会议室预约 oa_meeting_reservation
-- ----------------------------
CREATE TABLE IF NOT EXISTS oa_meeting_reservation (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(255) NOT NULL COMMENT '会议主题',
  room_id BIGINT NOT NULL COMMENT '会议室 ID',
  organizer_user_id BIGINT NOT NULL COMMENT '组织人 ID',
  attendee_user_ids VARCHAR(1024) COMMENT '参会人（逗号分隔）',
  start_time DATETIME NOT NULL COMMENT '开始时间',
  end_time DATETIME NOT NULL COMMENT '结束时间',
  status TINYINT DEFAULT 10 COMMENT '10 待确认 20 已确认 30 已取消 40 已完成',
  reminder_enabled BIT DEFAULT 0 COMMENT '是否提醒',
  reminder_minutes INT COMMENT '提前提醒分钟',
  remark VARCHAR(500),
  creator VARCHAR(64), create_time DATETIME, updater VARCHAR(64), update_time DATETIME, deleted BIT DEFAULT 0,
  tenant_id BIGINT DEFAULT 0
);
CREATE INDEX idx_meeting_reservation_room ON oa_meeting_reservation(room_id);
CREATE INDEX idx_meeting_reservation_organizer ON oa_meeting_reservation(organizer_user_id);
CREATE INDEX idx_meeting_reservation_time ON oa_meeting_reservation(start_time, end_time);

-- ----------------------------
-- 5. 公文 oa_document
-- ----------------------------
CREATE TABLE IF NOT EXISTS oa_document (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  no VARCHAR(64) NOT NULL COMMENT '公文编号',
  title VARCHAR(255) NOT NULL COMMENT '标题',
  document_type TINYINT NOT NULL COMMENT '10 通知 20 通报 30 报告 40 请示 50 决定 60 批复',
  urgency TINYINT DEFAULT 10 COMMENT '10 普通 20 紧急 30 特急',
  confidentiality TINYINT DEFAULT 10 COMMENT '10 公开 20 内部 30 秘密',
  issuer_user_id BIGINT COMMENT '发文人 ID',
  issue_dept_id BIGINT COMMENT '发文部门 ID',
  issue_date DATE COMMENT '发文日期',
  content TEXT COMMENT '正文内容',
  process_instance_id VARCHAR(64) COMMENT '工作流编号',
  status TINYINT DEFAULT 10 COMMENT '10 草稿 20 审核中 30 已发布 40 已废止',
  remark VARCHAR(500),
  creator VARCHAR(64), create_time DATETIME, updater VARCHAR(64), update_time DATETIME, deleted BIT DEFAULT 0,
  tenant_id BIGINT DEFAULT 0,
  UNIQUE KEY uk_document_no (no)
);
CREATE INDEX idx_document_issuer ON oa_document(issuer_user_id);
CREATE INDEX idx_document_status ON oa_document(status);

-- ----------------------------
-- 6. 公文附件 oa_document_attachment
-- ----------------------------
CREATE TABLE IF NOT EXISTS oa_document_attachment (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  document_id BIGINT NOT NULL COMMENT '公文 ID',
  file_name VARCHAR(255) NOT NULL COMMENT '文件名',
  file_url VARCHAR(500) NOT NULL COMMENT '文件地址',
  file_size BIGINT COMMENT '文件大小（字节）',
  file_type VARCHAR(64) COMMENT '文件类型',
  creator VARCHAR(64), create_time DATETIME, updater VARCHAR(64), update_time DATETIME, deleted BIT DEFAULT 0,
  tenant_id BIGINT DEFAULT 0
);
CREATE INDEX idx_document_attachment_document ON oa_document_attachment(document_id);
