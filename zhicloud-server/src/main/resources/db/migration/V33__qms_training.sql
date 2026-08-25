-- ============================================================
-- V33: QMS 培训管理
--
-- 包含：培训计划、培训记录、岗位资格矩阵（含到期预警支持）
-- ============================================================

-- ----------------------------
-- 培训计划表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `qms_training_plan` (
    `id` BIGINT NOT NULL COMMENT '主键',
    `plan_no` VARCHAR(64) NOT NULL COMMENT '计划编号',
    `plan_name` VARCHAR(255) NOT NULL COMMENT '计划名称',
    `year` INT NOT NULL COMMENT '年度',
    `course_name` VARCHAR(255) DEFAULT NULL COMMENT '课程名称',
    `trainer` VARCHAR(128) DEFAULT NULL COMMENT '讲师',
    `plan_date` DATE DEFAULT NULL COMMENT '计划日期',
    `status` TINYINT NOT NULL DEFAULT 10 COMMENT '状态（10 草稿 20 已安排 30 进行中 40 已完成 50 已取消）',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `creator` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` BIT(1) DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` BIGINT DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (`id`),
    KEY `idx_plan_no` (`plan_no`),
    KEY `idx_year` (`year`),
    KEY `idx_status` (`status`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='QMS 培训计划表';

-- ----------------------------
-- 培训记录表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `qms_training_record` (
    `id` BIGINT NOT NULL COMMENT '主键',
    `record_no` VARCHAR(64) NOT NULL COMMENT '记录编号',
    `plan_id` BIGINT DEFAULT NULL COMMENT '培训计划 ID',
    `trainee_id` BIGINT NOT NULL COMMENT '参训人员 ID',
    `trainee_name` VARCHAR(128) NOT NULL COMMENT '参训人员姓名',
    `score` DECIMAL(6,2) DEFAULT NULL COMMENT '成绩',
    `passed` TINYINT DEFAULT NULL COMMENT '是否通过（0 否 1 是）',
    `certificate_no` VARCHAR(128) DEFAULT NULL COMMENT '证书编号',
    `certificate_expire_date` DATE DEFAULT NULL COMMENT '证书到期日',
    `status` TINYINT NOT NULL DEFAULT 10 COMMENT '状态（10 已登记 20 已完成）',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `creator` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` BIT(1) DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` BIGINT DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (`id`),
    KEY `idx_record_no` (`record_no`),
    KEY `idx_plan_id` (`plan_id`),
    KEY `idx_trainee_id` (`trainee_id`),
    KEY `idx_status` (`status`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='QMS 培训记录表';

-- ----------------------------
-- 岗位资格表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `qms_qualification` (
    `id` BIGINT NOT NULL COMMENT '主键',
    `user_id` BIGINT NOT NULL COMMENT '用户 ID',
    `user_name` VARCHAR(128) NOT NULL COMMENT '用户姓名',
    `post_id` BIGINT DEFAULT NULL COMMENT '岗位 ID',
    `post_name` VARCHAR(128) DEFAULT NULL COMMENT '岗位名称',
    `qualification_name` VARCHAR(255) NOT NULL COMMENT '资格名称',
    `qualify_date` DATE DEFAULT NULL COMMENT '取得日期',
    `expire_date` DATE DEFAULT NULL COMMENT '到期日期',
    `status` TINYINT NOT NULL DEFAULT 10 COMMENT '状态（10 有效 20 即将到期 30 已到期 40 已撤销）',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `creator` VARCHAR(64) DEFAULT '' COMMENT '创建者',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater` VARCHAR(64) DEFAULT '' COMMENT '更新者',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` BIT(1) DEFAULT b'0' COMMENT '是否删除',
    `tenant_id` BIGINT DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_post_id` (`post_id`),
    KEY `idx_status` (`status`),
    KEY `idx_expire_date` (`expire_date`),
    KEY `idx_tenant_id` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='QMS 岗位资格表';