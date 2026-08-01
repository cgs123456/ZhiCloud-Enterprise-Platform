-- ============================================================
-- OA 第四阶段增强 DDL
-- 覆盖：知识库（分类/文章/版本/评论）、工作台公告、审批模板库、公文完整流转扩展
-- 幂等语法：CREATE TABLE IF NOT EXISTS / ALTER TABLE ADD COLUMN IF NOT EXISTS
-- ============================================================

-- ----------------------------
-- 1. 知识库分类 oa_knowledge_category
-- ----------------------------
CREATE TABLE IF NOT EXISTS oa_knowledge_category (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  parent_id BIGINT DEFAULT 0 COMMENT '父分类 ID（0 为根）',
  name VARCHAR(128) NOT NULL COMMENT '分类名称',
  sort INT DEFAULT 0 COMMENT '排序',
  status TINYINT DEFAULT 0 COMMENT '0 启用 1 停用',
  remark VARCHAR(500),
  creator VARCHAR(64), create_time DATETIME, updater VARCHAR(64), update_time DATETIME, deleted BIT DEFAULT 0,
  tenant_id BIGINT DEFAULT 0
);
CREATE INDEX idx_knowledge_category_parent ON oa_knowledge_category(parent_id);

-- ----------------------------
-- 2. 知识库文章 oa_knowledge_article
-- ----------------------------
CREATE TABLE IF NOT EXISTS oa_knowledge_article (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  category_id BIGINT COMMENT '分类 ID',
  title VARCHAR(255) NOT NULL COMMENT '标题',
  summary VARCHAR(500) COMMENT '摘要',
  content MEDIUMTEXT COMMENT '正文（Markdown/HTML）',
  tags VARCHAR(500) COMMENT '标签（逗号分隔）',
  author_user_id BIGINT COMMENT '作者 ID',
  author_name VARCHAR(64) COMMENT '作者姓名',
  view_count INT DEFAULT 0 COMMENT '阅读量',
  like_count INT DEFAULT 0 COMMENT '点赞数',
  comment_count INT DEFAULT 0 COMMENT '评论数',
  current_version INT DEFAULT 1 COMMENT '当前版本号',
  status TINYINT DEFAULT 10 COMMENT '10 草稿 20 已发布 30 已下架',
  top_flag BIT DEFAULT 0 COMMENT '是否置顶',
  remark VARCHAR(500),
  creator VARCHAR(64), create_time DATETIME, updater VARCHAR(64), update_time DATETIME, deleted BIT DEFAULT 0,
  tenant_id BIGINT DEFAULT 0
);
CREATE INDEX idx_knowledge_article_category ON oa_knowledge_article(category_id);
CREATE INDEX idx_knowledge_article_status ON oa_knowledge_article(status);
CREATE INDEX idx_knowledge_article_author ON oa_knowledge_article(author_user_id);
CREATE FULLTEXT INDEX IF NOT EXISTS ftx_knowledge_article_content ON oa_knowledge_article(title, summary, content, tags);

-- ----------------------------
-- 3. 知识库版本 oa_knowledge_version
-- ----------------------------
CREATE TABLE IF NOT EXISTS oa_knowledge_version (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  article_id BIGINT NOT NULL COMMENT '文章 ID',
  version_no INT NOT NULL COMMENT '版本号',
  title VARCHAR(255) NOT NULL COMMENT '该版本标题',
  content MEDIUMTEXT COMMENT '该版本正文',
  summary VARCHAR(500) COMMENT '该版本摘要',
  change_log VARCHAR(500) COMMENT '变更说明',
  editor_user_id BIGINT COMMENT '编辑人 ID',
  editor_name VARCHAR(64) COMMENT '编辑人姓名',
  creator VARCHAR(64), create_time DATETIME, updater VARCHAR(64), update_time DATETIME, deleted BIT DEFAULT 0,
  tenant_id BIGINT DEFAULT 0,
  UNIQUE KEY uk_knowledge_version (article_id, version_no)
);
CREATE INDEX idx_knowledge_version_article ON oa_knowledge_version(article_id);

-- ----------------------------
-- 4. 知识库评论 oa_knowledge_comment
-- ----------------------------
CREATE TABLE IF NOT EXISTS oa_knowledge_comment (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  article_id BIGINT NOT NULL COMMENT '文章 ID',
  parent_id BIGINT DEFAULT 0 COMMENT '父评论 ID（0 为根评论）',
  content TEXT NOT NULL COMMENT '评论内容',
  commentator_user_id BIGINT COMMENT '评论人 ID',
  commentator_name VARCHAR(64) COMMENT '评论人姓名',
  like_count INT DEFAULT 0 COMMENT '点赞数',
  status TINYINT DEFAULT 0 COMMENT '0 正常 1 已删除',
  creator VARCHAR(64), create_time DATETIME, updater VARCHAR(64), update_time DATETIME, deleted BIT DEFAULT 0,
  tenant_id BIGINT DEFAULT 0
);
CREATE INDEX idx_knowledge_comment_article ON oa_knowledge_comment(article_id);
CREATE INDEX idx_knowledge_comment_parent ON oa_knowledge_comment(parent_id);

-- ----------------------------
-- 5. 工作台公告 oa_announcement
-- ----------------------------
CREATE TABLE IF NOT EXISTS oa_announcement (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(255) NOT NULL COMMENT '公告标题',
  content TEXT COMMENT '公告内容',
  category VARCHAR(64) COMMENT '公告分类（company/dept/policy/notice）',
  priority TINYINT DEFAULT 10 COMMENT '10 普通 20 重要 30 紧急',
  publisher_user_id BIGINT COMMENT '发布人 ID',
  publisher_name VARCHAR(64) COMMENT '发布人姓名',
  target_scope VARCHAR(500) COMMENT '目标范围（all/dept_id 列表，逗号分隔）',
  publish_time DATETIME COMMENT '发布时间',
  expire_time DATETIME COMMENT '失效时间',
  top_flag BIT DEFAULT 0 COMMENT '是否置顶',
  status TINYINT DEFAULT 10 COMMENT '10 草稿 20 已发布 30 已下架 40 已过期',
  view_count INT DEFAULT 0 COMMENT '阅读量',
  remark VARCHAR(500),
  creator VARCHAR(64), create_time DATETIME, updater VARCHAR(64), update_time DATETIME, deleted BIT DEFAULT 0,
  tenant_id BIGINT DEFAULT 0
);
CREATE INDEX idx_announcement_status ON oa_announcement(status);
CREATE INDEX idx_announcement_publish_time ON oa_announcement(publish_time);

-- ----------------------------
-- 6. 审批模板库 oa_approval_template
-- ----------------------------
CREATE TABLE IF NOT EXISTS oa_approval_template (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  code VARCHAR(64) NOT NULL COMMENT '模板编码',
  name VARCHAR(128) NOT NULL COMMENT '模板名称',
  category VARCHAR(64) COMMENT '分类（通用/人事/财务/采购/合同/其他）',
  process_definition_key VARCHAR(128) NOT NULL COMMENT 'BPM 流程定义 KEY',
  form_schema TEXT COMMENT '表单 JSON Schema（字段定义）',
  form_ui_schema TEXT COMMENT '表单 UI Schema（布局/校验）',
  description VARCHAR(500) COMMENT '说明',
  icon VARCHAR(64) COMMENT '图标',
  sort INT DEFAULT 0 COMMENT '排序',
  status TINYINT DEFAULT 0 COMMENT '0 启用 1 停用',
  usage_count INT DEFAULT 0 COMMENT '使用次数',
  creator VARCHAR(64), create_time DATETIME, updater VARCHAR(64), update_time DATETIME, deleted BIT DEFAULT 0,
  tenant_id BIGINT DEFAULT 0,
  UNIQUE KEY uk_approval_template_code (code)
);
CREATE INDEX idx_approval_template_category ON oa_approval_template(category);
CREATE INDEX idx_approval_template_status ON oa_approval_template(status);

-- ----------------------------
-- 7. 公文完整流转扩展（核稿/签发/归档字段）
-- ----------------------------
-- 核稿阶段
ALTER TABLE oa_document ADD COLUMN IF NOT EXISTS reviewer_user_id BIGINT COMMENT '核稿人 ID';
ALTER TABLE oa_document ADD COLUMN IF NOT EXISTS reviewer_name VARCHAR(64) COMMENT '核稿人姓名';
ALTER TABLE oa_document ADD COLUMN IF NOT EXISTS review_time DATETIME COMMENT '核稿时间';
ALTER TABLE oa_document ADD COLUMN IF NOT EXISTS review_opinion VARCHAR(500) COMMENT '核稿意见';
-- 签发阶段
ALTER TABLE oa_document ADD COLUMN IF NOT EXISTS signer_user_id BIGINT COMMENT '签发人 ID';
ALTER TABLE oa_document ADD COLUMN IF NOT EXISTS signer_name VARCHAR(64) COMMENT '签发人姓名';
ALTER TABLE oa_document ADD COLUMN IF NOT EXISTS sign_time DATETIME COMMENT '签发时间';
ALTER TABLE oa_document ADD COLUMN IF NOT EXISTS sign_opinion VARCHAR(500) COMMENT '签发意见';
-- 归档阶段
ALTER TABLE oa_document ADD COLUMN IF NOT EXISTS archiver_user_id BIGINT COMMENT '归档人 ID';
ALTER TABLE oa_document ADD COLUMN IF NOT EXISTS archiver_name VARCHAR(64) COMMENT '归档人姓名';
ALTER TABLE oa_document ADD COLUMN IF NOT EXISTS archive_time DATETIME COMMENT '归档时间';
ALTER TABLE oa_document ADD COLUMN IF NOT EXISTS archive_no VARCHAR(64) COMMENT '归档编号';
-- 主送/抄送
ALTER TABLE oa_document ADD COLUMN IF NOT EXISTS main_send_depts VARCHAR(500) COMMENT '主送部门（逗号分隔 ID）';
ALTER TABLE oa_document ADD COLUMN IF NOT EXISTS copy_send_depts VARCHAR(500) COMMENT '抄送部门（逗号分隔 ID）';
-- 抄送阅读记录
ALTER TABLE oa_document ADD COLUMN IF NOT EXISTS read_count INT DEFAULT 0 COMMENT '阅读量';

-- ----------------------------
-- 8. 新增菜单：知识库/工作台/审批模板库
-- ----------------------------
INSERT IGNORE INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time)
VALUES
(3100, '知识库', '', 1, 20, 3000, 'knowledge', 'knowledge', 'oa/knowledge/index', 0, 'admin', NOW()),
(3101, '知识分类', 'oa:knowledge-category:query', 2, 1, 3100, 'category', 'tree', 'oa/knowledge/category/index', 0, 'admin', NOW()),
(3102, '知识文章', 'oa:knowledge-article:query', 2, 2, 3100, 'article', 'article', 'oa/knowledge/article/index', 0, 'admin', NOW()),
(3110, '工作台', '', 1, 5, 3000, 'portal', 'dashboard', 'oa/portal/index', 0, 'admin', NOW()),
(3111, '公告管理', 'oa:announcement:query', 2, 1, 3110, 'announcement', 'announcement', 'oa/portal/announcement/index', 0, 'admin', NOW()),
(3120, '审批模板', 'oa:approval-template:query', 2, 30, 3000, 'approval-template', 'template', 'oa/approval-template/index', 0, 'admin', NOW()),
(3121, '审批模板查询', 'oa:approval-template:query', 3, 1, 3120, '', '', '', 0, 'admin', NOW()),
(3122, '审批模板创建', 'oa:approval-template:create', 3, 2, 3120, '', '', '', 0, 'admin', NOW()),
(3123, '审批模板更新', 'oa:approval-template:update', 3, 3, 3120, '', '', '', 0, 'admin', NOW()),
(3124, '审批模板删除', 'oa:approval-template:delete', 3, 4, 3120, '', '', '', 0, 'admin', NOW()),
(3130, '公文归档', 'oa:document:archive', 3, 5, 3002, '', '', '', 0, 'admin', NOW()),
(3131, '公文核稿', 'oa:document:review', 3, 6, 3002, '', '', '', 0, 'admin', NOW()),
(3132, '公文签发', 'oa:document:sign', 3, 7, 3002, '', '', '', 0, 'admin', NOW());

-- 知识库细分权限
INSERT IGNORE INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time)
VALUES
(3103, '分类查询', 'oa:knowledge-category:query', 3, 1, 3101, '', '', '', 0, 'admin', NOW()),
(3104, '分类创建', 'oa:knowledge-category:create', 3, 2, 3101, '', '', '', 0, 'admin', NOW()),
(3105, '分类更新', 'oa:knowledge-category:update', 3, 3, 3101, '', '', '', 0, 'admin', NOW()),
(3106, '分类删除', 'oa:knowledge-category:delete', 3, 4, 3101, '', '', '', 0, 'admin', NOW()),
(3107, '文章查询', 'oa:knowledge-article:query', 3, 1, 3102, '', '', '', 0, 'admin', NOW()),
(3108, '文章创建', 'oa:knowledge-article:create', 3, 2, 3102, '', '', '', 0, 'admin', NOW()),
(3109, '文章更新', 'oa:knowledge-article:update', 3, 3, 3102, '', '', '', 0, 'admin', NOW()),
(3150, '文章删除', 'oa:knowledge-article:delete', 3, 4, 3102, '', '', '', 0, 'admin', NOW()),
(3151, '文章发布', 'oa:knowledge-article:publish', 3, 5, 3102, '', '', '', 0, 'admin', NOW());

-- 公告细分权限
INSERT IGNORE INTO system_menu (id, name, permission, type, sort, parent_id, path, icon, component, status, creator, create_time)
VALUES
(3112, '公告创建', 'oa:announcement:create', 3, 2, 3111, '', '', '', 0, 'admin', NOW()),
(3113, '公告更新', 'oa:announcement:update', 3, 3, 3111, '', '', '', 0, 'admin', NOW()),
(3114, '公告删除', 'oa:announcement:delete', 3, 4, 3111, '', '', '', 0, 'admin', NOW()),
(3115, '公告发布', 'oa:announcement:publish', 3, 5, 3111, '', '', '', 0, 'admin', NOW());
