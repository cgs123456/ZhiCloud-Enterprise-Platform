-- HR 招聘流程 DDL
CREATE TABLE hr_job_posting (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  position_id BIGINT NOT NULL COMMENT '岗位 ID',
  title VARCHAR(200) NOT NULL COMMENT '招聘标题',
  headcount INT NOT NULL COMMENT '招聘人数',
  salary_range VARCHAR(100) COMMENT '薪资范围',
  description TEXT COMMENT '职位描述',
  requirement TEXT COMMENT '任职要求',
  status TINYINT DEFAULT 0 COMMENT '0 招聘中 1 已暂停 2 已结束',
  publish_date DATE,
  close_date DATE,
  creator VARCHAR(64), create_time DATETIME, updater VARCHAR(64), update_time DATETIME, deleted BIT DEFAULT 0,
  tenant_id BIGINT DEFAULT 0
);
CREATE TABLE hr_resume (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  job_posting_id BIGINT NOT NULL COMMENT '招聘职位 ID',
  candidate_name VARCHAR(64) NOT NULL,
  phone VARCHAR(32),
  email VARCHAR(128),
  education VARCHAR(32) COMMENT '学历',
  experience_years INT COMMENT '工作年限',
  resume_url VARCHAR(500) COMMENT '简历附件',
  status TINYINT DEFAULT 0 COMMENT '0 待筛选 1 已通过 2 已面试 3 已录用 4 已淘汰',
  remark VARCHAR(500),
  creator VARCHAR(64), create_time DATETIME, updater VARCHAR(64), update_time DATETIME, deleted BIT DEFAULT 0,
  tenant_id BIGINT DEFAULT 0
);
CREATE INDEX idx_resume_job_posting ON hr_resume(job_posting_id);
CREATE INDEX idx_resume_status ON hr_resume(status);
CREATE TABLE hr_interview (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  resume_id BIGINT NOT NULL,
  interview_round INT DEFAULT 1 COMMENT '面试轮次',
  interviewer_id BIGINT COMMENT '面试官 ID',
  interview_time DATETIME,
  interview_type TINYINT COMMENT '1 现场 2 电话 3 视频',
  result TINYINT COMMENT '1 通过 2 待定 3 不通过',
  comment VARCHAR(1000) COMMENT '面试评价',
  status TINYINT DEFAULT 0 COMMENT '0 待面试 1 已完成 2 已取消',
  creator VARCHAR(64), create_time DATETIME, updater VARCHAR(64), update_time DATETIME, deleted BIT DEFAULT 0,
  tenant_id BIGINT DEFAULT 0
);
CREATE INDEX idx_interview_resume ON hr_interview(resume_id);