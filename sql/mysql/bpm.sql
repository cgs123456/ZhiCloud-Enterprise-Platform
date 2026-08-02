-- ======================== yudao-module-bpm 生产建表脚本（自动生成，上线前需在真实 MySQL 冒烟验证） ========================

CREATE TABLE IF NOT EXISTS bpm_category (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    name VARCHAR(255) DEFAULT NULL COMMENT 'name',
    code VARCHAR(255) DEFAULT NULL COMMENT 'code',
    description TEXT DEFAULT NULL COMMENT 'description',
    `status` INT DEFAULT NULL COMMENT 'status',
    sort INT DEFAULT NULL COMMENT 'sort',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_code (tenant_id, code, deleted),
    KEY idx_tenant (tenant_id),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='bpm_category';


CREATE TABLE IF NOT EXISTS bpm_form (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    name VARCHAR(255) DEFAULT NULL COMMENT 'name',
    `status` INT DEFAULT NULL COMMENT 'status',
    conf VARCHAR(255) DEFAULT NULL COMMENT 'conf',
    fields VARCHAR(255) DEFAULT NULL COMMENT 'fields',
    remark TEXT DEFAULT NULL COMMENT 'remark',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (id),
    KEY idx_tenant (tenant_id),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='bpm_form';


CREATE TABLE IF NOT EXISTS bpm_oa_leave (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    user_id BIGINT DEFAULT NULL COMMENT 'user_id',
    type INT DEFAULT NULL COMMENT 'type',
    reason VARCHAR(255) DEFAULT NULL COMMENT 'reason',
    start_time DATETIME DEFAULT NULL COMMENT 'start_time',
    end_time DATETIME DEFAULT NULL COMMENT 'end_time',
    day BIGINT DEFAULT NULL COMMENT 'day',
    `status` INT DEFAULT NULL COMMENT 'status',
    process_instance_id VARCHAR(255) DEFAULT NULL COMMENT 'process_instance_id',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (id),
    KEY idx_tenant (tenant_id),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='bpm_oa_leave';


CREATE TABLE IF NOT EXISTS bpm_process_definition_info (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    process_definition_id TEXT DEFAULT NULL COMMENT 'process_definition_id',
    model_id TEXT DEFAULT NULL COMMENT 'model_id',
    model_type INT DEFAULT NULL COMMENT 'model_type',
    category VARCHAR(255) DEFAULT NULL COMMENT 'category',
    icon VARCHAR(255) DEFAULT NULL COMMENT 'icon',
    description TEXT DEFAULT NULL COMMENT 'description',
    form_type INT DEFAULT NULL COMMENT 'form_type',
    form_id BIGINT DEFAULT NULL COMMENT 'form_id',
    form_conf VARCHAR(255) DEFAULT NULL COMMENT 'form_conf',
    form_fields VARCHAR(255) DEFAULT NULL COMMENT 'form_fields',
    form_custom_create_path VARCHAR(255) DEFAULT NULL COMMENT 'form_custom_create_path',
    form_custom_view_path VARCHAR(255) DEFAULT NULL COMMENT 'form_custom_view_path',
    simple_model TEXT DEFAULT NULL COMMENT 'simple_model',
    visible BIT(1) DEFAULT NULL COMMENT 'visible',
    sort BIGINT DEFAULT NULL COMMENT 'sort',
    start_user_ids VARCHAR(255) DEFAULT NULL COMMENT 'start_user_ids',
    start_dept_ids VARCHAR(255) DEFAULT NULL COMMENT 'start_dept_ids',
    manager_user_ids VARCHAR(255) DEFAULT NULL COMMENT 'manager_user_ids',
    allow_cancel_running_process BIT(1) DEFAULT NULL COMMENT 'allow_cancel_running_process',
    allow_withdraw_task BIT(1) DEFAULT NULL COMMENT 'allow_withdraw_task',
    process_id_rule VARCHAR(255) DEFAULT NULL COMMENT 'process_id_rule',
    auto_approval_type INT DEFAULT NULL COMMENT 'auto_approval_type',
    title_setting VARCHAR(255) DEFAULT NULL COMMENT 'title_setting',
    summary_setting VARCHAR(255) DEFAULT NULL COMMENT 'summary_setting',
    process_before_trigger_setting VARCHAR(255) DEFAULT NULL COMMENT 'process_before_trigger_setting',
    process_after_trigger_setting VARCHAR(255) DEFAULT NULL COMMENT 'process_after_trigger_setting',
    task_before_trigger_setting VARCHAR(255) DEFAULT NULL COMMENT 'task_before_trigger_setting',
    task_after_trigger_setting VARCHAR(255) DEFAULT NULL COMMENT 'task_after_trigger_setting',
    print_template_setting VARCHAR(255) DEFAULT NULL COMMENT 'print_template_setting',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (id),
    KEY idx_tenant (tenant_id),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='bpm_process_definition_info';


CREATE TABLE IF NOT EXISTS bpm_process_expression (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    name VARCHAR(255) DEFAULT NULL COMMENT 'name',
    `status` INT DEFAULT NULL COMMENT 'status',
    expression TEXT DEFAULT NULL COMMENT 'expression',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (id),
    KEY idx_tenant (tenant_id),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='bpm_process_expression';


CREATE TABLE IF NOT EXISTS bpm_process_instance_copy (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    start_user_id BIGINT DEFAULT NULL COMMENT 'start_user_id',
    process_instance_name VARCHAR(255) DEFAULT NULL COMMENT 'process_instance_name',
    process_instance_id VARCHAR(255) DEFAULT NULL COMMENT 'process_instance_id',
    process_definition_id TEXT DEFAULT NULL COMMENT 'process_definition_id',
    category VARCHAR(255) DEFAULT NULL COMMENT 'category',
    activity_id VARCHAR(255) DEFAULT NULL COMMENT 'activity_id',
    activity_name VARCHAR(255) DEFAULT NULL COMMENT 'activity_name',
    task_id VARCHAR(255) DEFAULT NULL COMMENT 'task_id',
    user_id BIGINT DEFAULT NULL COMMENT 'user_id',
    reason VARCHAR(255) DEFAULT NULL COMMENT 'reason',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (id),
    KEY idx_tenant (tenant_id),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='bpm_process_instance_copy';


CREATE TABLE IF NOT EXISTS bpm_process_listener (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    name VARCHAR(255) DEFAULT NULL COMMENT 'name',
    `status` INT DEFAULT NULL COMMENT 'status',
    type VARCHAR(255) DEFAULT NULL COMMENT 'type',
    event VARCHAR(255) DEFAULT NULL COMMENT 'event',
    value_type VARCHAR(255) DEFAULT NULL COMMENT 'value_type',
    value VARCHAR(255) DEFAULT NULL COMMENT 'value',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (id),
    KEY idx_tenant (tenant_id),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='bpm_process_listener';


CREATE TABLE IF NOT EXISTS bpm_user_group (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    name VARCHAR(255) DEFAULT NULL COMMENT 'name',
    description TEXT DEFAULT NULL COMMENT 'description',
    `status` INT DEFAULT NULL COMMENT 'status',
    user_ids VARCHAR(255) DEFAULT NULL COMMENT 'user_ids',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (id),
    KEY idx_tenant (tenant_id),
    KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='bpm_user_group';

