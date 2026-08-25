-- ============================================================
-- MES 制造执行系统建表脚本
-- 数据库：MySQL 8.0
-- 引擎：InnoDB / 字符集：utf8mb4 / 排序：utf8mb4_unicode_ci
-- 说明：覆盖 cal/dv/md/pro/qc/tm/wm 七大子域共 133 张表
-- ============================================================

-- ============================================================
-- 排班管理 子域（cal） - 7 张表
-- ============================================================

-- ----------------------------
-- MES 假期设置（mes_cal_holiday）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_cal_holiday (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    day DATETIME DEFAULT NULL COMMENT '日期',
    type INT DEFAULT NULL COMMENT '日期类型',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 假期设置';

-- ----------------------------
-- MES 排班计划（mes_cal_plan）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_cal_plan (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    code VARCHAR(64) DEFAULT NULL COMMENT '计划编码',
    name VARCHAR(255) DEFAULT NULL COMMENT '计划名称',
    calendar_type INT DEFAULT NULL COMMENT '班组类型',
    start_date DATETIME DEFAULT NULL COMMENT '开始日期',
    end_date DATETIME DEFAULT NULL COMMENT '结束日期',
    shift_type INT DEFAULT NULL COMMENT '轮班方式',
    shift_method INT DEFAULT NULL COMMENT '倒班方式',
    shift_count INT DEFAULT NULL COMMENT '倒班天数',
    status INT DEFAULT NULL COMMENT '状态',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 排班计划';

-- ----------------------------
-- MES 计划班次（mes_cal_plan_shift）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_cal_plan_shift (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    plan_id BIGINT DEFAULT NULL COMMENT '排班计划编号',
    sort INT DEFAULT NULL COMMENT '显示顺序',
    name VARCHAR(255) DEFAULT NULL COMMENT '班次名称',
    start_time VARCHAR(10) DEFAULT NULL COMMENT '开始时间（HH:mm 格式）',
    end_time VARCHAR(10) DEFAULT NULL COMMENT '结束时间（HH:mm 格式）',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_plan_id (plan_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 计划班次';

-- ----------------------------
-- MES 计划班组关联（mes_cal_plan_team）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_cal_plan_team (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    plan_id BIGINT DEFAULT NULL COMMENT '排班计划编号',
    team_id BIGINT DEFAULT NULL COMMENT '班组编号',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_plan_id (plan_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 计划班组关联';

-- ----------------------------
-- MES 班组（mes_cal_team）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_cal_team (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    code VARCHAR(64) DEFAULT NULL COMMENT '班组编码',
    name VARCHAR(255) DEFAULT NULL COMMENT '班组名称',
    calendar_type INT DEFAULT NULL COMMENT '班组类型',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 班组';

-- ----------------------------
-- MES 班组成员（mes_cal_team_member）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_cal_team_member (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    team_id BIGINT DEFAULT NULL COMMENT '班组编号',
    user_id BIGINT DEFAULT NULL COMMENT '用户编号',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_team_id (team_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 班组成员';

-- ----------------------------
-- MES 班组排班（mes_cal_team_shift）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_cal_team_shift (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    plan_id BIGINT DEFAULT NULL COMMENT '排班计划编号',
    team_id BIGINT DEFAULT NULL COMMENT '班组编号',
    shift_id BIGINT DEFAULT NULL COMMENT '班次编号',
    day DATETIME DEFAULT NULL COMMENT '日期',
    sort INT DEFAULT NULL COMMENT '排序',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_plan_id (plan_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 班组排班';

-- ============================================================
-- 设备管理 子域（dv） - 12 张表
-- ============================================================

-- ----------------------------
-- MES 点检保养方案（mes_dv_check_plan）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_dv_check_plan (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    code VARCHAR(64) DEFAULT NULL COMMENT '方案编码',
    name VARCHAR(255) DEFAULT NULL COMMENT '方案名称',
    type INT DEFAULT NULL COMMENT '方案类型',
    start_date DATETIME DEFAULT NULL COMMENT '开始日期',
    end_date DATETIME DEFAULT NULL COMMENT '结束日期',
    cycle_type INT DEFAULT NULL COMMENT '周期类型',
    cycle_count INT DEFAULT NULL COMMENT '周期数量',
    status INT DEFAULT NULL COMMENT '状态',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 点检保养方案';

-- ----------------------------
-- MES 点检保养方案设备（mes_dv_check_plan_machinery）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_dv_check_plan_machinery (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    plan_id BIGINT DEFAULT NULL COMMENT '方案编号',
    machinery_id BIGINT DEFAULT NULL COMMENT '设备编号',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_plan_id (plan_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 点检保养方案设备';

-- ----------------------------
-- MES 点检保养方案项目（mes_dv_check_plan_subject）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_dv_check_plan_subject (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    plan_id BIGINT DEFAULT NULL COMMENT '方案编号',
    subject_id BIGINT DEFAULT NULL COMMENT '项目编号',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_plan_id (plan_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 点检保养方案项目';

-- ----------------------------
-- MES 设备点检记录（mes_dv_check_record）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_dv_check_record (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    plan_id BIGINT DEFAULT NULL COMMENT '点检计划编号',
    machinery_id BIGINT DEFAULT NULL COMMENT '设备编号',
    check_time DATETIME DEFAULT NULL COMMENT '点检时间',
    user_id BIGINT DEFAULT NULL COMMENT '点检人编号',
    status INT DEFAULT NULL COMMENT '状态',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_plan_id (plan_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 设备点检记录';

-- ----------------------------
-- MES 设备点检记录明细（mes_dv_check_record_line）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_dv_check_record_line (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    record_id BIGINT DEFAULT NULL COMMENT '点检记录编号',
    subject_id BIGINT DEFAULT NULL COMMENT '点检项目编号',
    check_status INT DEFAULT NULL COMMENT '点检结果',
    check_result VARCHAR(500) DEFAULT NULL COMMENT '异常描述',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_record_id (record_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 设备点检记录明细';

-- ----------------------------
-- MES 设备台账（mes_dv_machinery）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_dv_machinery (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    code VARCHAR(64) DEFAULT NULL COMMENT '设备编码',
    name VARCHAR(255) DEFAULT NULL COMMENT '设备名称',
    brand VARCHAR(255) DEFAULT NULL COMMENT '品牌',
    specification VARCHAR(255) DEFAULT NULL COMMENT '规格型号',
    machinery_type_id BIGINT DEFAULT NULL COMMENT '设备类型编号',
    workshop_id BIGINT DEFAULT NULL COMMENT '所属车间编号',
    status INT DEFAULT NULL COMMENT '设备状态',
    last_mainten_time DATETIME DEFAULT NULL COMMENT '最近保养时间',
    last_check_time DATETIME DEFAULT NULL COMMENT '最近点检时间',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_code (code),
    KEY idx_machinery_type_id (machinery_type_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 设备台账';

-- ----------------------------
-- MES 设备类型（mes_dv_machinery_type）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_dv_machinery_type (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    code VARCHAR(64) DEFAULT NULL COMMENT '类型编码',
    name VARCHAR(255) DEFAULT NULL COMMENT '类型名称',
    parent_id BIGINT DEFAULT NULL COMMENT '父类型编号',
    status INT DEFAULT NULL COMMENT '状态',
    sort INT DEFAULT NULL COMMENT '显示排序',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_code (code),
    KEY idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 设备类型';

-- ----------------------------
-- MES 设备保养记录（mes_dv_mainten_record）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_dv_mainten_record (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    plan_id BIGINT DEFAULT NULL COMMENT '计划编号',
    machinery_id BIGINT DEFAULT NULL COMMENT '设备编号',
    mainten_time DATETIME DEFAULT NULL COMMENT '保养时间',
    user_id BIGINT DEFAULT NULL COMMENT '用户编号',
    status INT DEFAULT NULL COMMENT '状态',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_plan_id (plan_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 设备保养记录';

-- ----------------------------
-- MES 设备保养记录明细（mes_dv_mainten_record_line）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_dv_mainten_record_line (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    record_id BIGINT DEFAULT NULL COMMENT '保养记录编号',
    subject_id BIGINT DEFAULT NULL COMMENT '项目编号',
    status INT DEFAULT NULL COMMENT '保养结果',
    result VARCHAR(500) DEFAULT NULL COMMENT '异常描述',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_record_id (record_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 设备保养记录明细';

-- ----------------------------
-- MES 维修工单（mes_dv_repair）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_dv_repair (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    code VARCHAR(64) DEFAULT NULL COMMENT '维修工单编码',
    name VARCHAR(255) DEFAULT NULL COMMENT '维修工单名称',
    machinery_id BIGINT DEFAULT NULL COMMENT '设备编号',
    require_date DATETIME DEFAULT NULL COMMENT '报修日期',
    finish_date DATETIME DEFAULT NULL COMMENT '维修完成日期',
    confirm_date DATETIME DEFAULT NULL COMMENT '验收日期',
    result INT DEFAULT NULL COMMENT '维修结果',
    accepted_user_id BIGINT DEFAULT NULL COMMENT '维修人用户编号',
    confirm_user_id BIGINT DEFAULT NULL COMMENT '验收人用户编号',
    source_doc_type INT DEFAULT NULL COMMENT '来源单据类型',
    source_doc_id BIGINT DEFAULT NULL COMMENT '来源单据编号',
    source_doc_code VARCHAR(64) DEFAULT NULL COMMENT '来源单据编码',
    status INT DEFAULT NULL COMMENT '状态',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_code (code),
    KEY idx_machinery_id (machinery_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 维修工单';

-- ----------------------------
-- MES 维修工单行（mes_dv_repair_line）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_dv_repair_line (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    repair_id BIGINT DEFAULT NULL COMMENT '维修工单编号',
    subject_id BIGINT DEFAULT NULL COMMENT '点检保养项目编号',
    malfunction VARCHAR(500) DEFAULT NULL COMMENT '故障描述',
    malfunction_url VARCHAR(500) DEFAULT NULL COMMENT '故障图片 URL',
    description VARCHAR(500) DEFAULT NULL COMMENT '维修描述',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_repair_id (repair_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 维修工单行';

-- ----------------------------
-- MES 点检保养项目（mes_dv_subject）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_dv_subject (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    code VARCHAR(64) DEFAULT NULL COMMENT '项目编码',
    name VARCHAR(255) DEFAULT NULL COMMENT '项目名称',
    type INT DEFAULT NULL COMMENT '项目类型',
    content VARCHAR(500) DEFAULT NULL COMMENT '项目内容',
    standard VARCHAR(500) DEFAULT NULL COMMENT '标准',
    status INT DEFAULT NULL COMMENT '状态',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 点检保养项目';

-- ============================================================
-- 基础主数据 子域（md） - 17 张表
-- ============================================================

-- ----------------------------
-- MES 编码规则组成（mes_md_auto_code_part）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_md_auto_code_part (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    rule_id BIGINT DEFAULT NULL COMMENT '规则 ID',
    sort INT DEFAULT NULL COMMENT '分段序号',
    type INT DEFAULT NULL COMMENT '分段类型',
    length INT DEFAULT NULL COMMENT '分段长度',
    date_format VARCHAR(20) DEFAULT NULL COMMENT '日期格式',
    fix_character VARCHAR(64) DEFAULT NULL COMMENT '固定字符',
    serial_start_no INT DEFAULT NULL COMMENT '流水号起始值',
    serial_step INT DEFAULT NULL COMMENT '流水号步长',
    cycle_flag BIT(1) DEFAULT NULL COMMENT '流水号是否循环',
    cycle_method INT DEFAULT NULL COMMENT '循环方式',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_rule_id (rule_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 编码规则组成';

-- ----------------------------
-- MES 编码生成记录（mes_md_auto_code_record）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_md_auto_code_record (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    rule_id BIGINT DEFAULT NULL COMMENT '规则 ID',
    result VARCHAR(500) DEFAULT NULL COMMENT '生成的编码',
    serial_no BIGINT DEFAULT NULL COMMENT '生成的流水号',
    input_char VARCHAR(255) DEFAULT NULL COMMENT '传入的参数',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_rule_id (rule_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 编码生成记录';

-- ----------------------------
-- MES 编码规则（mes_md_auto_code_rule）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_md_auto_code_rule (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    code VARCHAR(64) DEFAULT NULL COMMENT '规则编码',
    name VARCHAR(255) DEFAULT NULL COMMENT '规则名称',
    description VARCHAR(500) DEFAULT NULL COMMENT '描述',
    max_length INT DEFAULT NULL COMMENT '最大长度',
    padded BIT(1) DEFAULT NULL COMMENT '是否补齐',
    padded_char VARCHAR(1) DEFAULT NULL COMMENT '补齐字符',
    padded_method INT DEFAULT NULL COMMENT '补齐方式',
    status INT DEFAULT NULL COMMENT '状态',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 编码规则';

-- ----------------------------
-- MES 客户（mes_md_client）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_md_client (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    code VARCHAR(64) DEFAULT NULL COMMENT '客户编码',
    name VARCHAR(255) DEFAULT NULL COMMENT '客户名称',
    nickname VARCHAR(64) DEFAULT NULL COMMENT '客户简称',
    english_name VARCHAR(128) DEFAULT NULL COMMENT '客户英文名称',
    description VARCHAR(500) DEFAULT NULL COMMENT '客户简介',
    logo VARCHAR(500) DEFAULT NULL COMMENT '客户LOGO地址',
    type INT DEFAULT NULL COMMENT '客户类型',
    address VARCHAR(255) DEFAULT NULL COMMENT '客户地址',
    website VARCHAR(255) DEFAULT NULL COMMENT '客户官网地址',
    email VARCHAR(128) DEFAULT NULL COMMENT '客户邮箱地址',
    telephone VARCHAR(64) DEFAULT NULL COMMENT '客户电话',
    contact1_name VARCHAR(64) DEFAULT NULL COMMENT '联系人1',
    contact1_telephone VARCHAR(64) DEFAULT NULL COMMENT '联系人1-电话',
    contact1_email VARCHAR(255) DEFAULT NULL COMMENT '联系人1-邮箱',
    contact2_name VARCHAR(64) DEFAULT NULL COMMENT '联系人2',
    contact2_telephone VARCHAR(64) DEFAULT NULL COMMENT '联系人2-电话',
    contact2_email VARCHAR(255) DEFAULT NULL COMMENT '联系人2-邮箱',
    credit_code VARCHAR(64) DEFAULT NULL COMMENT '统一社会信用代码',
    status INT DEFAULT NULL COMMENT '状态',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 客户';

-- ----------------------------
-- MES 物料产品（mes_md_item）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_md_item (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    code VARCHAR(64) DEFAULT NULL COMMENT '物料编码',
    name VARCHAR(255) DEFAULT NULL COMMENT '物料名称',
    specification VARCHAR(255) DEFAULT NULL COMMENT '规格型号',
    unit_measure_id BIGINT DEFAULT NULL COMMENT '计量单位编号',
    item_type_id BIGINT DEFAULT NULL COMMENT '物料分类编号',
    status INT DEFAULT NULL COMMENT '状态',
    safe_stock_flag BIT(1) DEFAULT NULL COMMENT '是否启用安全库存',
    min_stock DECIMAL(20,6) DEFAULT NULL COMMENT '最低库存量',
    max_stock DECIMAL(20,6) DEFAULT NULL COMMENT '最高库存量',
    high_value BIT(1) DEFAULT NULL COMMENT '是否高值物料',
    batch_flag BIT(1) DEFAULT NULL COMMENT '是否启用批次管理',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_code (code),
    KEY idx_unit_measure_id (unit_measure_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 物料产品';

-- ----------------------------
-- MES 物料批次属性配置（mes_md_item_batch_config）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_md_item_batch_config (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    item_id BIGINT DEFAULT NULL COMMENT '物料编号',
    produce_date_flag BIT(1) DEFAULT NULL COMMENT '批次属性-生产日期',
    expire_date_flag BIT(1) DEFAULT NULL COMMENT '批次属性-有效期',
    receipt_date_flag BIT(1) DEFAULT NULL COMMENT '批次属性-入库日期',
    vendor_flag BIT(1) DEFAULT NULL COMMENT '批次属性-供应商',
    client_flag BIT(1) DEFAULT NULL COMMENT '批次属性-客户',
    sales_order_code_flag BIT(1) DEFAULT NULL COMMENT '批次属性-销售订单编号',
    purchase_order_code_flag BIT(1) DEFAULT NULL COMMENT '批次属性-采购订单编号',
    work_order_flag BIT(1) DEFAULT NULL COMMENT '批次属性-生产工单',
    task_flag BIT(1) DEFAULT NULL COMMENT '批次属性-生产任务',
    workstation_flag BIT(1) DEFAULT NULL COMMENT '批次属性-工作站',
    tool_flag BIT(1) DEFAULT NULL COMMENT '批次属性-工具',
    mold_flag BIT(1) DEFAULT NULL COMMENT '批次属性-模具',
    lot_number_flag BIT(1) DEFAULT NULL COMMENT '批次属性-生产批号',
    quality_status_flag BIT(1) DEFAULT NULL COMMENT '批次属性-质量状态',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_item_id (item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 物料批次属性配置';

-- ----------------------------
-- MES 物料产品分类（mes_md_item_type）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_md_item_type (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    code VARCHAR(64) DEFAULT NULL COMMENT '分类编码',
    name VARCHAR(255) DEFAULT NULL COMMENT '分类名称',
    parent_id BIGINT DEFAULT NULL COMMENT '父分类编号',
    item_or_product VARCHAR(64) DEFAULT NULL COMMENT '物料/产品标识',
    sort INT DEFAULT NULL COMMENT '显示排序',
    status INT DEFAULT NULL COMMENT '状态',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_code (code),
    KEY idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 物料产品分类';

-- ----------------------------
-- MES 产品 BOM（mes_md_product_bom）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_md_product_bom (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    item_id BIGINT DEFAULT NULL COMMENT '物料产品编号',
    bom_item_id BIGINT DEFAULT NULL COMMENT 'BOM物料编号',
    quantity DECIMAL(20,6) DEFAULT NULL COMMENT '物料使用比例',
    status INT DEFAULT NULL COMMENT '是否启用',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_item_id (item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 产品 BOM';

-- ----------------------------
-- MES 产品SIP（mes_md_product_sip）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_md_product_sip (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    item_id BIGINT DEFAULT NULL COMMENT '物料产品编号',
    sort INT DEFAULT NULL COMMENT '排列顺序',
    process_id BIGINT DEFAULT NULL COMMENT '工序编号',
    title VARCHAR(255) DEFAULT NULL COMMENT '标题',
    description VARCHAR(500) DEFAULT NULL COMMENT '详细描述',
    url VARCHAR(500) DEFAULT NULL COMMENT '图片地址',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_item_id (item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 产品SIP';

-- ----------------------------
-- MES 产品SOP（mes_md_product_sop）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_md_product_sop (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    item_id BIGINT DEFAULT NULL COMMENT '物料产品编号',
    sort INT DEFAULT NULL COMMENT '排列顺序',
    process_id BIGINT DEFAULT NULL COMMENT '工序编号',
    title VARCHAR(255) DEFAULT NULL COMMENT '标题',
    description VARCHAR(500) DEFAULT NULL COMMENT '详细描述',
    url VARCHAR(500) DEFAULT NULL COMMENT '图片地址',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_item_id (item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 产品SOP';

-- ----------------------------
-- MES 计量单位（mes_md_unit_measure）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_md_unit_measure (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    code VARCHAR(64) DEFAULT NULL COMMENT '单位编码',
    name VARCHAR(255) DEFAULT NULL COMMENT '单位名称',
    primary_flag BIT(1) DEFAULT NULL COMMENT '是否主单位',
    primary_id BIGINT DEFAULT NULL COMMENT '主单位编号',
    change_rate DECIMAL(10,2) DEFAULT NULL COMMENT '与主单位换算比例',
    status INT DEFAULT NULL COMMENT '状态',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_code (code),
    KEY idx_primary_id (primary_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 计量单位';

-- ----------------------------
-- MES 供应商（mes_md_vendor）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_md_vendor (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    code VARCHAR(64) DEFAULT NULL COMMENT '供应商编码',
    name VARCHAR(255) DEFAULT NULL COMMENT '供应商名称',
    nickname VARCHAR(64) DEFAULT NULL COMMENT '供应商简称',
    english_name VARCHAR(128) DEFAULT NULL COMMENT '供应商英文名称',
    description VARCHAR(500) DEFAULT NULL COMMENT '供应商简介',
    logo VARCHAR(500) DEFAULT NULL COMMENT '供应商LOGO地址',
    level VARCHAR(64) DEFAULT NULL COMMENT '供应商等级',
    score INT DEFAULT NULL COMMENT '供应商评分',
    address VARCHAR(255) DEFAULT NULL COMMENT '供应商地址',
    website VARCHAR(255) DEFAULT NULL COMMENT '供应商官网地址',
    email VARCHAR(128) DEFAULT NULL COMMENT '供应商邮箱地址',
    telephone VARCHAR(64) DEFAULT NULL COMMENT '供应商电话',
    contact1_name VARCHAR(64) DEFAULT NULL COMMENT '联系人1',
    contact1_telephone VARCHAR(64) DEFAULT NULL COMMENT '联系人1-电话',
    contact1_email VARCHAR(255) DEFAULT NULL COMMENT '联系人1-邮箱',
    contact2_name VARCHAR(64) DEFAULT NULL COMMENT '联系人2',
    contact2_telephone VARCHAR(64) DEFAULT NULL COMMENT '联系人2-电话',
    contact2_email VARCHAR(255) DEFAULT NULL COMMENT '联系人2-邮箱',
    credit_code VARCHAR(64) DEFAULT NULL COMMENT '统一社会信用代码',
    status INT DEFAULT NULL COMMENT '状态',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 供应商';

-- ----------------------------
-- MES 车间（mes_md_workshop）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_md_workshop (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    code VARCHAR(64) DEFAULT NULL COMMENT '车间编码',
    name VARCHAR(255) DEFAULT NULL COMMENT '车间名称',
    area DECIMAL(20,4) DEFAULT NULL COMMENT '面积（平方米）',
    charge_user_id BIGINT DEFAULT NULL COMMENT '负责人用户编号',
    status INT DEFAULT NULL COMMENT '状态',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_code (code),
    KEY idx_charge_user_id (charge_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 车间';

-- ----------------------------
-- MES 工作站（mes_md_workstation）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_md_workstation (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    code VARCHAR(64) DEFAULT NULL COMMENT '工作站编码',
    name VARCHAR(255) DEFAULT NULL COMMENT '工作站名称',
    address VARCHAR(255) DEFAULT NULL COMMENT '工作站地点',
    workshop_id BIGINT DEFAULT NULL COMMENT '所在车间编号',
    process_id BIGINT DEFAULT NULL COMMENT '工序编号',
    warehouse_id BIGINT DEFAULT NULL COMMENT '线边库编号',
    location_id BIGINT DEFAULT NULL COMMENT '库区编号',
    area_id BIGINT DEFAULT NULL COMMENT '库位编号',
    status INT DEFAULT NULL COMMENT '状态',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_code (code),
    KEY idx_workshop_id (workshop_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 工作站';

-- ----------------------------
-- MES 设备资源（mes_md_workstation_machine）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_md_workstation_machine (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    workstation_id BIGINT DEFAULT NULL COMMENT '工作站编号',
    machinery_id BIGINT DEFAULT NULL COMMENT '设备编号',
    quantity INT DEFAULT NULL COMMENT '数量',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_workstation_id (workstation_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 设备资源';

-- ----------------------------
-- MES 工装夹具资源（mes_md_workstation_tool）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_md_workstation_tool (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    workstation_id BIGINT DEFAULT NULL COMMENT '工作站编号',
    tool_type_id BIGINT DEFAULT NULL COMMENT '工具类型编号',
    quantity INT DEFAULT NULL COMMENT '数量',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_workstation_id (workstation_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 工装夹具资源';

-- ----------------------------
-- MES 人力资源（mes_md_workstation_worker）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_md_workstation_worker (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    workstation_id BIGINT DEFAULT NULL COMMENT '工作站编号',
    post_id BIGINT DEFAULT NULL COMMENT '岗位编号',
    quantity INT DEFAULT NULL COMMENT '数量',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_workstation_id (workstation_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 人力资源';

-- ============================================================
-- 生产管理 子域（pro） - 17 张表
-- ============================================================

-- ----------------------------
-- MES 安灯呼叫配置（mes_pro_andon_config）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_pro_andon_config (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    reason VARCHAR(255) DEFAULT NULL COMMENT '呼叫原因',
    level INT DEFAULT NULL COMMENT '级别',
    handler_role_id BIGINT DEFAULT NULL COMMENT '处置人角色编号',
    handler_user_id BIGINT DEFAULT NULL COMMENT '处置人编号',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_handler_role_id (handler_role_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 安灯呼叫配置';

-- ----------------------------
-- MES 安灯呼叫记录（mes_pro_andon_record）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_pro_andon_record (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    config_id BIGINT DEFAULT NULL COMMENT '安灯配置编号',
    workstation_id BIGINT DEFAULT NULL COMMENT '工作站编号',
    user_id BIGINT DEFAULT NULL COMMENT '发起用户编号',
    work_order_id BIGINT DEFAULT NULL COMMENT '生产工单编号',
    process_id BIGINT DEFAULT NULL COMMENT '工序编号',
    reason VARCHAR(255) DEFAULT NULL COMMENT '呼叫原因（快照值，不随配置变更）',
    level INT DEFAULT NULL COMMENT '级别（快照值）',
    status INT DEFAULT NULL COMMENT '处置状态',
    handle_time DATETIME DEFAULT NULL COMMENT '处置时间',
    handler_user_id BIGINT DEFAULT NULL COMMENT '处置人编号',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_config_id (config_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 安灯呼叫记录';

-- ----------------------------
-- MES 生产流转卡（mes_pro_card）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_pro_card (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    code VARCHAR(64) DEFAULT NULL COMMENT '流转卡编码',
    work_order_id BIGINT DEFAULT NULL COMMENT '生产工单编号',
    item_id BIGINT DEFAULT NULL COMMENT '产品物料编号',
    batch_code VARCHAR(128) DEFAULT NULL COMMENT '批次号',
    transfered_quantity DECIMAL(20,6) DEFAULT NULL COMMENT '流转数量',
    status INT DEFAULT NULL COMMENT '状态',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_code (code),
    KEY idx_work_order_id (work_order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 生产流转卡';

-- ----------------------------
-- MES 流转卡工序记录（mes_pro_card_process）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_pro_card_process (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    card_id BIGINT DEFAULT NULL COMMENT '流转卡编号',
    sort INT DEFAULT NULL COMMENT '序号',
    process_id BIGINT DEFAULT NULL COMMENT '工序编号',
    input_time DATETIME DEFAULT NULL COMMENT '进入工序时间',
    output_time DATETIME DEFAULT NULL COMMENT '出工序时间',
    input_quantity DECIMAL(20,6) DEFAULT NULL COMMENT '投入数量',
    output_quantity DECIMAL(20,6) DEFAULT NULL COMMENT '产出数量',
    unqualified_quantity DECIMAL(20,6) DEFAULT NULL COMMENT '不合格品数量',
    workstation_id BIGINT DEFAULT NULL COMMENT '工位编号',
    user_id BIGINT DEFAULT NULL COMMENT '操作人编号',
    ipqc_id BIGINT DEFAULT NULL COMMENT '过程检验单编号',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_card_id (card_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 流转卡工序记录';

-- ----------------------------
-- MES 生产报工（mes_pro_feedback）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_pro_feedback (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    code VARCHAR(64) DEFAULT NULL COMMENT '报工单编号',
    type INT DEFAULT NULL COMMENT '报工类型',
    channel VARCHAR(64) DEFAULT NULL COMMENT '报工途径',
    feedback_time DATETIME DEFAULT NULL COMMENT '报工时间',
    workstation_id BIGINT DEFAULT NULL COMMENT '工作站编号',
    route_id BIGINT DEFAULT NULL COMMENT '工艺路线编号',
    process_id BIGINT DEFAULT NULL COMMENT '工序编号',
    work_order_id BIGINT DEFAULT NULL COMMENT '生产工单编号',
    task_id BIGINT DEFAULT NULL COMMENT '生产任务编号',
    item_id BIGINT DEFAULT NULL COMMENT '产品物料编号（冗余自任务）',
    expire_date DATETIME DEFAULT NULL COMMENT '过期日期',
    lot_number VARCHAR(128) DEFAULT NULL COMMENT '生产批号',
    scheduled_quantity DECIMAL(20,6) DEFAULT NULL COMMENT '排产数量',
    feedback_quantity DECIMAL(20,6) DEFAULT NULL COMMENT '本次报工数量',
    qualified_quantity DECIMAL(20,6) DEFAULT NULL COMMENT '合格品数量',
    unqualified_quantity DECIMAL(20,6) DEFAULT NULL COMMENT '不良品数量',
    uncheck_quantity DECIMAL(20,6) DEFAULT NULL COMMENT '待检测数量',
    labor_scrap_quantity DECIMAL(20,6) DEFAULT NULL COMMENT '工废数量',
    material_scrap_quantity DECIMAL(20,6) DEFAULT NULL COMMENT '料废数量',
    other_scrap_quantity DECIMAL(20,6) DEFAULT NULL COMMENT '其他废品数量',
    feedback_user_id BIGINT DEFAULT NULL COMMENT '报工用户编号',
    approve_user_id BIGINT DEFAULT NULL COMMENT '审核用户编号',
    status INT DEFAULT NULL COMMENT '状态',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_code (code),
    KEY idx_workstation_id (workstation_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 生产报工';

-- ----------------------------
-- MES 生产工序（mes_pro_process）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_pro_process (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    code VARCHAR(64) DEFAULT NULL COMMENT '工序编码',
    name VARCHAR(255) DEFAULT NULL COMMENT '工序名称',
    attention VARCHAR(500) DEFAULT NULL COMMENT '工艺要求',
    status INT DEFAULT NULL COMMENT '状态',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 生产工序';

-- ----------------------------
-- MES 生产工序内容（mes_pro_process_content）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_pro_process_content (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    process_id BIGINT DEFAULT NULL COMMENT '工序编号',
    sort INT DEFAULT NULL COMMENT '顺序编号',
    content VARCHAR(500) DEFAULT NULL COMMENT '步骤说明',
    device VARCHAR(255) DEFAULT NULL COMMENT '辅助设备',
    material VARCHAR(255) DEFAULT NULL COMMENT '辅助材料',
    doc_url VARCHAR(500) DEFAULT NULL COMMENT '材料文档 URL',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_process_id (process_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 生产工序内容';

-- ----------------------------
-- MES 工艺路线（mes_pro_route）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_pro_route (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    code VARCHAR(64) DEFAULT NULL COMMENT '工艺路线编码',
    name VARCHAR(255) DEFAULT NULL COMMENT '工艺路线名称',
    description VARCHAR(500) DEFAULT NULL COMMENT '工艺路线说明',
    status INT DEFAULT NULL COMMENT '状态',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 工艺路线';

-- ----------------------------
-- MES 工艺路线工序（mes_pro_route_process）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_pro_route_process (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    route_id BIGINT DEFAULT NULL COMMENT '工艺路线编号',
    process_id BIGINT DEFAULT NULL COMMENT '工序编号',
    sort INT DEFAULT NULL COMMENT '序号',
    next_process_id BIGINT DEFAULT NULL COMMENT '下一道工序编号',
    link_type INT DEFAULT NULL COMMENT '与下一道工序关系',
    prepare_time INT DEFAULT NULL COMMENT '准备时间（分钟）',
    wait_time INT DEFAULT NULL COMMENT '等待时间（分钟）',
    color_code VARCHAR(20) DEFAULT NULL COMMENT '甘特图显示颜色',
    key_flag BIT(1) DEFAULT NULL COMMENT '是否关键工序',
    check_flag BIT(1) DEFAULT NULL COMMENT '是否质检工序',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_route_id (route_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 工艺路线工序';

-- ----------------------------
-- MES 工艺路线产品（mes_pro_route_product）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_pro_route_product (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    route_id BIGINT DEFAULT NULL COMMENT '工艺路线编号',
    item_id BIGINT DEFAULT NULL COMMENT '产品物料编号',
    quantity INT DEFAULT NULL COMMENT '生产数量',
    production_time DECIMAL(20,4) DEFAULT NULL COMMENT '生产用时',
    time_unit_type VARCHAR(64) DEFAULT NULL COMMENT '时间单位',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_route_id (route_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 工艺路线产品';

-- ----------------------------
-- MES 工艺路线产品 BOM（mes_pro_route_product_bom）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_pro_route_product_bom (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    route_id BIGINT DEFAULT NULL COMMENT '工艺路线编号',
    process_id BIGINT DEFAULT NULL COMMENT '工序编号',
    product_id BIGINT DEFAULT NULL COMMENT '产品物料编号',
    item_id BIGINT DEFAULT NULL COMMENT 'BOM 物料编号',
    quantity DECIMAL(20,6) DEFAULT NULL COMMENT '用料比例',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_route_id (route_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 工艺路线产品 BOM';

-- ----------------------------
-- MES 生产任务（mes_pro_task）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_pro_task (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    code VARCHAR(64) DEFAULT NULL COMMENT '任务编码',
    name VARCHAR(255) DEFAULT NULL COMMENT '任务名称',
    work_order_id BIGINT DEFAULT NULL COMMENT '生产工单编号',
    workstation_id BIGINT DEFAULT NULL COMMENT '工作站编号',
    route_id BIGINT DEFAULT NULL COMMENT '工艺路线编号',
    process_id BIGINT DEFAULT NULL COMMENT '工序编号',
    item_id BIGINT DEFAULT NULL COMMENT '产品物料编号',
    quantity DECIMAL(20,6) DEFAULT NULL COMMENT '排产数量',
    produced_quantity DECIMAL(20,6) DEFAULT NULL COMMENT '已生产数量',
    qualify_quantity DECIMAL(20,6) DEFAULT NULL COMMENT '合格品数量',
    unqualify_quantity DECIMAL(20,6) DEFAULT NULL COMMENT '不良品数量',
    changed_quantity DECIMAL(20,6) DEFAULT NULL COMMENT '调整数量',
    client_id BIGINT DEFAULT NULL COMMENT '客户编号',
    start_time DATETIME DEFAULT NULL COMMENT '开始生产时间',
    duration INT DEFAULT NULL COMMENT '生产时长（工作日，1=8小时）',
    end_time DATETIME DEFAULT NULL COMMENT '结束生产时间',
    color_code VARCHAR(20) DEFAULT NULL COMMENT '甘特图显示颜色',
    finish_date DATETIME DEFAULT NULL COMMENT '完成日期',
    cancel_date DATETIME DEFAULT NULL COMMENT '取消日期',
    status INT DEFAULT NULL COMMENT '任务状态',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_code (code),
    KEY idx_work_order_id (work_order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 生产任务';

-- ----------------------------
-- MES 生产任务投料（mes_pro_task_issue）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_pro_task_issue (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    task_id BIGINT DEFAULT NULL COMMENT '生产任务编号',
    work_order_id BIGINT DEFAULT NULL COMMENT '生产工单编号',
    workstation_id BIGINT DEFAULT NULL COMMENT '工作站编号',
    source_doc_type VARCHAR(64) DEFAULT NULL COMMENT '来源单据类型',
    source_doc_id BIGINT DEFAULT NULL COMMENT '来源单据编号',
    source_line_id BIGINT DEFAULT NULL COMMENT '来源单据行编号',
    source_doc_code VARCHAR(64) DEFAULT NULL COMMENT '来源单据编码',
    batch_code VARCHAR(128) DEFAULT NULL COMMENT '投料批次',
    item_id BIGINT DEFAULT NULL COMMENT '产品物料编号',
    unit_measure_id BIGINT DEFAULT NULL COMMENT '单位编号',
    issued_quantity DECIMAL(20,6) DEFAULT NULL COMMENT '总投料数量',
    available_quantity DECIMAL(20,6) DEFAULT NULL COMMENT '当前可用数量',
    used_quantity DECIMAL(20,6) DEFAULT NULL COMMENT '当前使用数量',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_task_id (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 生产任务投料';

-- ----------------------------
-- MES 生产工单（mes_pro_work_order）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_pro_work_order (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    code VARCHAR(64) DEFAULT NULL COMMENT '工单编码',
    name VARCHAR(255) DEFAULT NULL COMMENT '工单名称',
    type INT DEFAULT NULL COMMENT '工单类型',
    order_source_type INT DEFAULT NULL COMMENT '来源类型',
    order_source_code VARCHAR(64) DEFAULT NULL COMMENT '来源单据编号',
    product_id BIGINT DEFAULT NULL COMMENT '产品编号',
    quantity DECIMAL(20,6) DEFAULT NULL COMMENT '生产数量',
    quantity_produced DECIMAL(20,6) DEFAULT NULL COMMENT '已生产数量',
    quantity_changed DECIMAL(20,6) DEFAULT NULL COMMENT '调整数量',
    quantity_scheduled DECIMAL(20,6) DEFAULT NULL COMMENT '已排产数量',
    client_id BIGINT DEFAULT NULL COMMENT '客户编号',
    vendor_id BIGINT DEFAULT NULL COMMENT '供应商编号',
    batch_code VARCHAR(128) DEFAULT NULL COMMENT '批次号',
    request_date DATETIME DEFAULT NULL COMMENT '需求日期',
    parent_id BIGINT DEFAULT NULL COMMENT '父工单编号',
    finish_date DATETIME DEFAULT NULL COMMENT '完成时间',
    cancel_date DATETIME DEFAULT NULL COMMENT '取消时间',
    status INT DEFAULT NULL COMMENT '工单状态',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_code (code),
    KEY idx_product_id (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 生产工单';

-- ----------------------------
-- MES 生产工单 BOM（mes_pro_work_order_bom）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_pro_work_order_bom (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    work_order_id BIGINT DEFAULT NULL COMMENT '生产工单编号',
    item_id BIGINT DEFAULT NULL COMMENT 'BOM 物料编号',
    quantity DECIMAL(20,6) DEFAULT NULL COMMENT '预计使用量',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_work_order_id (work_order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 生产工单 BOM';

-- ----------------------------
-- MES 用户工作站绑定关系（当前快照）（mes_pro_work_record）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_pro_work_record (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id BIGINT DEFAULT NULL COMMENT '用户编号',
    workstation_id BIGINT DEFAULT NULL COMMENT '工作站编号',
    type INT DEFAULT NULL COMMENT '当前状态',
    clock_in_time DATETIME DEFAULT NULL COMMENT '上工时间',
    clock_out_time DATETIME DEFAULT NULL COMMENT '下工时间',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 用户工作站绑定关系（当前快照）';

-- ----------------------------
-- MES 上下工记录流水（mes_pro_work_record_log）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_pro_work_record_log (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id BIGINT DEFAULT NULL COMMENT '用户编号',
    workstation_id BIGINT DEFAULT NULL COMMENT '工作站编号',
    type INT DEFAULT NULL COMMENT '操作类型',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 上下工记录流水';

-- ============================================================
-- 质量管理 子域（qc） - 16 张表
-- ============================================================

-- ----------------------------
-- MES 缺陷类型（mes_qc_defect）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_qc_defect (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    code VARCHAR(64) DEFAULT NULL COMMENT '缺陷编码',
    name VARCHAR(255) DEFAULT NULL COMMENT '缺陷描述',
    type INT DEFAULT NULL COMMENT '检测项类型',
    level INT DEFAULT NULL COMMENT '缺陷等级',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 缺陷类型';

-- ----------------------------
-- MES 质检缺陷记录（mes_qc_defect_record）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_qc_defect_record (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    qc_type INT DEFAULT NULL COMMENT '检验类型',
    qc_id BIGINT DEFAULT NULL COMMENT '检验单 ID',
    line_id BIGINT DEFAULT NULL COMMENT '检验行 ID',
    name VARCHAR(255) DEFAULT NULL COMMENT '缺陷描述',
    level INT DEFAULT NULL COMMENT '缺陷等级',
    quantity INT DEFAULT NULL COMMENT '缺陷数量',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_qc_id (qc_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 质检缺陷记录';

-- ----------------------------
-- MES 质检指标（mes_qc_indicator）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_qc_indicator (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    code VARCHAR(64) DEFAULT NULL COMMENT '检测项编码',
    name VARCHAR(255) DEFAULT NULL COMMENT '检测项名称',
    type INT DEFAULT NULL COMMENT '检测项类型',
    tool VARCHAR(64) DEFAULT NULL COMMENT '检测工具',
    result_type INT DEFAULT NULL COMMENT '结果值类型',
    result_specification VARCHAR(255) DEFAULT NULL COMMENT '结果值属性',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 质检指标';

-- ----------------------------
-- MES 检验结果记录（mes_qc_indicator_result）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_qc_indicator_result (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    code VARCHAR(64) DEFAULT NULL COMMENT '样品编号',
    qc_id BIGINT DEFAULT NULL COMMENT '关联质检单 ID（IQC/IPQC/OQC/RQC 的 id）',
    qc_type INT DEFAULT NULL COMMENT '质检类型',
    item_id BIGINT DEFAULT NULL COMMENT '产品物料 ID',
    sn VARCHAR(128) DEFAULT NULL COMMENT '物资 SN',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_code (code),
    KEY idx_qc_id (qc_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 检验结果记录';

-- ----------------------------
-- MES 检验结果明细记录（mes_qc_indicator_result_detail）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_qc_indicator_result_detail (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    result_id BIGINT DEFAULT NULL COMMENT '关联检验结果 ID',
    indicator_id BIGINT DEFAULT NULL COMMENT '检测指标 ID',
    value VARCHAR(500) DEFAULT NULL COMMENT '检测值（统一存为字符串）',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_result_id (result_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 检验结果明细记录';

-- ----------------------------
-- MES 过程检验单（IPQC, In-Process Quality Control）（mes_qc_ipqc）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_qc_ipqc (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    code VARCHAR(64) DEFAULT NULL COMMENT '检验单编号',
    name VARCHAR(255) DEFAULT NULL COMMENT '检验单名称',
    type INT DEFAULT NULL COMMENT 'IPQC 检验类型',
    template_id BIGINT DEFAULT NULL COMMENT '检验模板 ID',
    source_doc_type INT DEFAULT NULL COMMENT '来源单据类型',
    source_doc_id BIGINT DEFAULT NULL COMMENT '来源单据 ID',
    source_line_id BIGINT DEFAULT NULL COMMENT '来源单据行 ID',
    source_doc_code VARCHAR(64) DEFAULT NULL COMMENT '来源单据编号（冗余）',
    work_order_id BIGINT DEFAULT NULL COMMENT '生产工单 ID',
    task_id BIGINT DEFAULT NULL COMMENT '生产任务 ID',
    workstation_id BIGINT DEFAULT NULL COMMENT '工位 ID',
    process_id BIGINT DEFAULT NULL COMMENT '工序 ID',
    item_id BIGINT DEFAULT NULL COMMENT '产品物料 ID',
    check_quantity DECIMAL(20,6) DEFAULT NULL COMMENT '检测数量',
    qualified_quantity DECIMAL(20,6) DEFAULT NULL COMMENT '合格品数量',
    unqualified_quantity DECIMAL(20,6) DEFAULT NULL COMMENT '不合格品数量',
    labor_scrap_quantity DECIMAL(20,6) DEFAULT NULL COMMENT '工废数量',
    material_scrap_quantity DECIMAL(20,6) DEFAULT NULL COMMENT '料废数量',
    other_scrap_quantity DECIMAL(20,6) DEFAULT NULL COMMENT '其他废品数量',
    critical_rate DECIMAL(10,2) DEFAULT NULL COMMENT '致命缺陷率（%）',
    major_rate DECIMAL(10,2) DEFAULT NULL COMMENT '严重缺陷率（%）',
    minor_rate DECIMAL(10,2) DEFAULT NULL COMMENT '轻微缺陷率（%）',
    critical_quantity INT DEFAULT NULL COMMENT '致命缺陷数量',
    major_quantity INT DEFAULT NULL COMMENT '严重缺陷数量',
    minor_quantity INT DEFAULT NULL COMMENT '轻微缺陷数量',
    check_result INT DEFAULT NULL COMMENT '检测结果',
    inspect_date DATETIME DEFAULT NULL COMMENT '检测日期',
    inspector_user_id BIGINT DEFAULT NULL COMMENT '检测人员用户 ID',
    status INT DEFAULT NULL COMMENT '状态',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_code (code),
    KEY idx_template_id (template_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 过程检验单（IPQC, In-Process Quality Control）';

-- ----------------------------
-- MES 过程检验单行（mes_qc_ipqc_line）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_qc_ipqc_line (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    ipqc_id BIGINT DEFAULT NULL COMMENT '过程检验单 ID',
    indicator_id BIGINT DEFAULT NULL COMMENT '检测指标 ID',
    tool VARCHAR(64) DEFAULT NULL COMMENT '检测工具',
    check_method VARCHAR(64) DEFAULT NULL COMMENT '检测方法',
    standard_value DECIMAL(20,6) DEFAULT NULL COMMENT '标准值',
    unit_measure_id BIGINT DEFAULT NULL COMMENT '计量单位 ID',
    max_threshold DECIMAL(20,6) DEFAULT NULL COMMENT '误差上限',
    min_threshold DECIMAL(20,6) DEFAULT NULL COMMENT '误差下限',
    critical_quantity INT DEFAULT NULL COMMENT '致命缺陷数量',
    major_quantity INT DEFAULT NULL COMMENT '严重缺陷数量',
    minor_quantity INT DEFAULT NULL COMMENT '轻微缺陷数量',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_ipqc_id (ipqc_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 过程检验单行';

-- ----------------------------
-- MES 来料检验单（IQC, Incoming Quality Control）（mes_qc_iqc）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_qc_iqc (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    code VARCHAR(64) DEFAULT NULL COMMENT '检验单编号',
    name VARCHAR(255) DEFAULT NULL COMMENT '检验单名称',
    template_id BIGINT DEFAULT NULL COMMENT '检验模板 ID',
    source_doc_type INT DEFAULT NULL COMMENT '来源单据类型',
    source_doc_id BIGINT DEFAULT NULL COMMENT '来源单据 ID',
    source_line_id BIGINT DEFAULT NULL COMMENT '来源单据行 ID',
    source_doc_code VARCHAR(64) DEFAULT NULL COMMENT '来源单据编号（冗余）',
    vendor_id BIGINT DEFAULT NULL COMMENT '供应商 ID',
    vendor_batch VARCHAR(64) DEFAULT NULL COMMENT '供应商批次号',
    item_id BIGINT DEFAULT NULL COMMENT '产品物料 ID',
    received_quantity DECIMAL(20,6) DEFAULT NULL COMMENT '本次接收数量',
    check_quantity DECIMAL(20,6) DEFAULT NULL COMMENT '本次检测数量',
    qualified_quantity DECIMAL(20,6) DEFAULT NULL COMMENT '合格品数量',
    unqualified_quantity DECIMAL(20,6) DEFAULT NULL COMMENT '不合格品数量',
    critical_rate DECIMAL(10,2) DEFAULT NULL COMMENT '致命缺陷率（%）',
    major_rate DECIMAL(10,2) DEFAULT NULL COMMENT '严重缺陷率（%）',
    minor_rate DECIMAL(10,2) DEFAULT NULL COMMENT '轻微缺陷率（%）',
    critical_quantity INT DEFAULT NULL COMMENT '致命缺陷数量',
    major_quantity INT DEFAULT NULL COMMENT '严重缺陷数量',
    minor_quantity INT DEFAULT NULL COMMENT '轻微缺陷数量',
    check_result INT DEFAULT NULL COMMENT '检测结果',
    receive_date DATETIME DEFAULT NULL COMMENT '来料日期',
    inspect_date DATETIME DEFAULT NULL COMMENT '检测日期',
    inspector_user_id BIGINT DEFAULT NULL COMMENT '检测人员用户 ID',
    status INT DEFAULT NULL COMMENT '状态',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_code (code),
    KEY idx_template_id (template_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 来料检验单（IQC, Incoming Quality Control）';

-- ----------------------------
-- MES 来料检验单行（mes_qc_iqc_line）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_qc_iqc_line (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    iqc_id BIGINT DEFAULT NULL COMMENT '来料检验单 ID',
    indicator_id BIGINT DEFAULT NULL COMMENT '检测指标 ID',
    tool VARCHAR(64) DEFAULT NULL COMMENT '检测工具',
    check_method VARCHAR(64) DEFAULT NULL COMMENT '检测方法',
    standard_value DECIMAL(20,6) DEFAULT NULL COMMENT '标准值',
    unit_measure_id BIGINT DEFAULT NULL COMMENT '计量单位 ID',
    max_threshold DECIMAL(20,6) DEFAULT NULL COMMENT '误差上限',
    min_threshold DECIMAL(20,6) DEFAULT NULL COMMENT '误差下限',
    critical_quantity INT DEFAULT NULL COMMENT '致命缺陷数量',
    major_quantity INT DEFAULT NULL COMMENT '严重缺陷数量',
    minor_quantity INT DEFAULT NULL COMMENT '轻微缺陷数量',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_iqc_id (iqc_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 来料检验单行';

-- ----------------------------
-- MES 出货检验单（OQC, Outgoing Quality Control）（mes_qc_oqc）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_qc_oqc (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    code VARCHAR(64) DEFAULT NULL COMMENT '检验单编号',
    name VARCHAR(255) DEFAULT NULL COMMENT '检验单名称',
    template_id BIGINT DEFAULT NULL COMMENT '检验模板 ID',
    source_doc_type INT DEFAULT NULL COMMENT '来源单据类型',
    source_doc_id BIGINT DEFAULT NULL COMMENT '来源单据 ID',
    source_line_id BIGINT DEFAULT NULL COMMENT '来源单据行 ID',
    source_doc_code VARCHAR(64) DEFAULT NULL COMMENT '来源单据编号（冗余）',
    client_id BIGINT DEFAULT NULL COMMENT '客户 ID',
    batch_code VARCHAR(128) DEFAULT NULL COMMENT '批次号',
    item_id BIGINT DEFAULT NULL COMMENT '产品物料 ID',
    min_check_quantity INT DEFAULT NULL COMMENT '最低检测数',
    max_unqualified_quantity INT DEFAULT NULL COMMENT '最大不合格数',
    out_quantity DECIMAL(20,6) DEFAULT NULL COMMENT '本次出货数量',
    check_quantity DECIMAL(20,6) DEFAULT NULL COMMENT '本次检测数量',
    qualified_quantity DECIMAL(20,6) DEFAULT NULL COMMENT '合格品数量',
    unqualified_quantity DECIMAL(20,6) DEFAULT NULL COMMENT '不合格品数量',
    critical_rate DECIMAL(10,2) DEFAULT NULL COMMENT '致命缺陷率（%）',
    major_rate DECIMAL(10,2) DEFAULT NULL COMMENT '严重缺陷率（%）',
    minor_rate DECIMAL(10,2) DEFAULT NULL COMMENT '轻微缺陷率（%）',
    critical_quantity INT DEFAULT NULL COMMENT '致命缺陷数量',
    major_quantity INT DEFAULT NULL COMMENT '严重缺陷数量',
    minor_quantity INT DEFAULT NULL COMMENT '轻微缺陷数量',
    check_result INT DEFAULT NULL COMMENT '检测结果',
    out_date DATETIME DEFAULT NULL COMMENT '出货日期',
    inspect_date DATETIME DEFAULT NULL COMMENT '检测日期',
    inspector_user_id BIGINT DEFAULT NULL COMMENT '检测人员用户 ID',
    status INT DEFAULT NULL COMMENT '状态',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_code (code),
    KEY idx_template_id (template_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 出货检验单（OQC, Outgoing Quality Control）';

-- ----------------------------
-- MES 出货检验单行（mes_qc_oqc_line）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_qc_oqc_line (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    oqc_id BIGINT DEFAULT NULL COMMENT '出货检验单 ID',
    indicator_id BIGINT DEFAULT NULL COMMENT '检测指标 ID',
    tool VARCHAR(64) DEFAULT NULL COMMENT '检测工具',
    check_method VARCHAR(64) DEFAULT NULL COMMENT '检测方法',
    standard_value DECIMAL(20,6) DEFAULT NULL COMMENT '标准值',
    unit_measure_id BIGINT DEFAULT NULL COMMENT '计量单位 ID',
    max_threshold DECIMAL(20,6) DEFAULT NULL COMMENT '误差上限',
    min_threshold DECIMAL(20,6) DEFAULT NULL COMMENT '误差下限',
    critical_quantity INT DEFAULT NULL COMMENT '致命缺陷数量',
    major_quantity INT DEFAULT NULL COMMENT '严重缺陷数量',
    minor_quantity INT DEFAULT NULL COMMENT '轻微缺陷数量',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_oqc_id (oqc_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 出货检验单行';

-- ----------------------------
-- MES 退货检验单（RQC, Return Quality Control）（mes_qc_rqc）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_qc_rqc (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    code VARCHAR(64) DEFAULT NULL COMMENT '检验单编号',
    name VARCHAR(255) DEFAULT NULL COMMENT '检验单名称',
    template_id BIGINT DEFAULT NULL COMMENT '检验模板 ID',
    source_doc_type INT DEFAULT NULL COMMENT '来源单据类型',
    source_doc_id BIGINT DEFAULT NULL COMMENT '来源单据 ID',
    source_line_id BIGINT DEFAULT NULL COMMENT '来源单据行 ID',
    source_doc_code VARCHAR(64) DEFAULT NULL COMMENT '来源单据编码（冗余）',
    type INT DEFAULT NULL COMMENT '检验类型',
    item_id BIGINT DEFAULT NULL COMMENT '产品物料 ID',
    batch_code VARCHAR(128) DEFAULT NULL COMMENT '批次号',
    check_quantity DECIMAL(20,6) DEFAULT NULL COMMENT '检测数量',
    qualified_quantity DECIMAL(20,6) DEFAULT NULL COMMENT '合格品数量',
    unqualified_quantity DECIMAL(20,6) DEFAULT NULL COMMENT '不合格数量',
    critical_rate DECIMAL(10,2) DEFAULT NULL COMMENT '致命缺陷率（%）',
    major_rate DECIMAL(10,2) DEFAULT NULL COMMENT '严重缺陷率（%）',
    minor_rate DECIMAL(10,2) DEFAULT NULL COMMENT '轻微缺陷率（%）',
    critical_quantity INT DEFAULT NULL COMMENT '致命缺陷数量',
    major_quantity INT DEFAULT NULL COMMENT '严重缺陷数量',
    minor_quantity INT DEFAULT NULL COMMENT '轻微缺陷数量',
    check_result INT DEFAULT NULL COMMENT '检测结果',
    inspect_date DATETIME DEFAULT NULL COMMENT '检测日期',
    inspector_user_id BIGINT DEFAULT NULL COMMENT '检测人员用户 ID',
    status INT DEFAULT NULL COMMENT '状态',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_code (code),
    KEY idx_template_id (template_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 退货检验单（RQC, Return Quality Control）';

-- ----------------------------
-- MES 退货检验行（mes_qc_rqc_line）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_qc_rqc_line (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    rqc_id BIGINT DEFAULT NULL COMMENT '退货检验单 ID',
    indicator_id BIGINT DEFAULT NULL COMMENT '检测指标 ID',
    tool VARCHAR(64) DEFAULT NULL COMMENT '检测工具',
    check_method VARCHAR(64) DEFAULT NULL COMMENT '检测方法',
    standard_value DECIMAL(20,6) DEFAULT NULL COMMENT '标准值',
    unit_measure_id BIGINT DEFAULT NULL COMMENT '计量单位 ID',
    max_threshold DECIMAL(20,6) DEFAULT NULL COMMENT '误差上限',
    min_threshold DECIMAL(20,6) DEFAULT NULL COMMENT '误差下限',
    critical_quantity INT DEFAULT NULL COMMENT '致命缺陷数量',
    major_quantity INT DEFAULT NULL COMMENT '严重缺陷数量',
    minor_quantity INT DEFAULT NULL COMMENT '轻微缺陷数量',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_rqc_id (rqc_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 退货检验行';

-- ----------------------------
-- MES 质检方案（mes_qc_template）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_qc_template (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    code VARCHAR(64) DEFAULT NULL COMMENT '方案编号',
    name VARCHAR(255) DEFAULT NULL COMMENT '方案名称',
    types VARCHAR(255) DEFAULT NULL COMMENT '检测种类',
    status INT DEFAULT NULL COMMENT '状态',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 质检方案';

-- ----------------------------
-- MES 质检方案-检测指标项（mes_qc_template_indicator）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_qc_template_indicator (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    template_id BIGINT DEFAULT NULL COMMENT '质检方案编号',
    indicator_id BIGINT DEFAULT NULL COMMENT '质检指标编号',
    check_method VARCHAR(64) DEFAULT NULL COMMENT '检测方法',
    standard_value DECIMAL(20,6) DEFAULT NULL COMMENT '标准值',
    unit_measure_id BIGINT DEFAULT NULL COMMENT '计量单位编号',
    threshold_max DECIMAL(20,6) DEFAULT NULL COMMENT '误差上限',
    threshold_min DECIMAL(20,6) DEFAULT NULL COMMENT '误差下限',
    doc_url VARCHAR(500) DEFAULT NULL COMMENT '说明图 URL',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_template_id (template_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 质检方案-检测指标项';

-- ----------------------------
-- MES 质检方案-产品关联（mes_qc_template_item）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_qc_template_item (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    template_id BIGINT DEFAULT NULL COMMENT '质检方案编号',
    item_id BIGINT DEFAULT NULL COMMENT '产品物料编号',
    quantity_check INT DEFAULT NULL COMMENT '最低检测数',
    quantity_unqualified INT DEFAULT NULL COMMENT '最大不合格数（0=不启用）',
    critical_rate DECIMAL(10,2) DEFAULT NULL COMMENT '最大致命缺陷率（%，0=不允许）',
    major_rate DECIMAL(10,2) DEFAULT NULL COMMENT '最大严重缺陷率（%，0=不允许）',
    minor_rate DECIMAL(10,2) DEFAULT NULL COMMENT '最大轻微缺陷率（%）',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_template_id (template_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 质检方案-产品关联';

-- ============================================================
-- 工具管理 子域（tm） - 2 张表
-- ============================================================

-- ----------------------------
-- MES 工具台账（mes_tm_tool）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_tm_tool (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    code VARCHAR(64) DEFAULT NULL COMMENT '工具编码',
    name VARCHAR(255) DEFAULT NULL COMMENT '工具名称',
    brand VARCHAR(255) DEFAULT NULL COMMENT '品牌',
    specification VARCHAR(255) DEFAULT NULL COMMENT '型号规格',
    tool_type_id BIGINT DEFAULT NULL COMMENT '工具类型编号',
    quantity INT DEFAULT NULL COMMENT '数量',
    available_quantity INT DEFAULT NULL COMMENT '可用数量',
    mainten_type INT DEFAULT NULL COMMENT '保养维护类型',
    next_mainten_period INT DEFAULT NULL COMMENT '下次保养周期（次数）',
    next_mainten_date DATETIME DEFAULT NULL COMMENT '下次保养日期',
    status INT DEFAULT NULL COMMENT '状态',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_code (code),
    KEY idx_tool_type_id (tool_type_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 工具台账';

-- ----------------------------
-- MES 工具类型（mes_tm_tool_type）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_tm_tool_type (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    code VARCHAR(64) DEFAULT NULL COMMENT '类型编码',
    name VARCHAR(255) DEFAULT NULL COMMENT '类型名称',
    code_flag BIT(1) DEFAULT NULL COMMENT '是否编码管理',
    mainten_type INT DEFAULT NULL COMMENT '保养维护类型',
    mainten_period INT DEFAULT NULL COMMENT '保养周期',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 工具类型';

-- ============================================================
-- 仓储管理 子域（wm） - 62 张表
-- ============================================================

-- ----------------------------
-- MES 到货通知单（mes_wm_arrival_notice）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_wm_arrival_notice (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    code VARCHAR(64) DEFAULT NULL COMMENT '通知单编码',
    name VARCHAR(255) DEFAULT NULL COMMENT '通知单名称',
    purchase_order_code VARCHAR(64) DEFAULT NULL COMMENT '采购订单编号',
    vendor_id BIGINT DEFAULT NULL COMMENT '供应商编号',
    arrival_date DATETIME DEFAULT NULL COMMENT '到货日期',
    contact_name VARCHAR(64) DEFAULT NULL COMMENT '联系人',
    contact_telephone VARCHAR(64) DEFAULT NULL COMMENT '联系电话',
    status INT DEFAULT NULL COMMENT '状态',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_code (code),
    KEY idx_vendor_id (vendor_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 到货通知单';

-- ----------------------------
-- MES 到货通知单行（mes_wm_arrival_notice_line）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_wm_arrival_notice_line (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    notice_id BIGINT DEFAULT NULL COMMENT '到货通知单编号',
    item_id BIGINT DEFAULT NULL COMMENT '物料编号',
    arrival_quantity DECIMAL(20,6) DEFAULT NULL COMMENT '到货数量',
    qualified_quantity DECIMAL(20,6) DEFAULT NULL COMMENT '合格数量',
    iqc_check_flag BIT(1) DEFAULT NULL COMMENT '是否需要来料检验',
    iqc_id BIGINT DEFAULT NULL COMMENT '来料检验单编号',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_notice_id (notice_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 到货通知单行';

-- ----------------------------
-- MES 条码清单（mes_wm_barcode）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_wm_barcode (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    config_id BIGINT DEFAULT NULL COMMENT '条码配置编号',
    format INT DEFAULT NULL COMMENT '条码格式',
    biz_type INT DEFAULT NULL COMMENT '业务类型',
    content VARCHAR(500) DEFAULT NULL COMMENT '条码内容（核心字段，前端根据此内容生成条码图片）',
    biz_id BIGINT DEFAULT NULL COMMENT '业务编号',
    biz_code VARCHAR(64) DEFAULT NULL COMMENT '业务编码',
    biz_name VARCHAR(64) DEFAULT NULL COMMENT '业务名称',
    status INT DEFAULT NULL COMMENT '状态',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_config_id (config_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 条码清单';

-- ----------------------------
-- MES 条码配置（mes_wm_barcode_config）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_wm_barcode_config (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    format INT DEFAULT NULL COMMENT '条码格式',
    biz_type INT DEFAULT NULL COMMENT '业务类型',
    content_format VARCHAR(500) DEFAULT NULL COMMENT '内容格式模板（支持 {BUSINESSCODE} 占位符）',
    content_example VARCHAR(500) DEFAULT NULL COMMENT '内容样例',
    auto_generate_flag BIT(1) DEFAULT NULL COMMENT '是否自动生成',
    default_template VARCHAR(500) DEFAULT NULL COMMENT '默认打印模板',
    status INT DEFAULT NULL COMMENT '状态',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 条码配置';

-- ----------------------------
-- 批次管理（mes_wm_batch）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_wm_batch (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    code VARCHAR(64) DEFAULT NULL COMMENT '批次编码',
    item_id BIGINT DEFAULT NULL COMMENT '物料ID',
    produce_date DATETIME DEFAULT NULL COMMENT '生产日期',
    expire_date DATETIME DEFAULT NULL COMMENT '有效期',
    receipt_date DATETIME DEFAULT NULL COMMENT '入库日期',
    vendor_id BIGINT DEFAULT NULL COMMENT '供应商ID',
    client_id BIGINT DEFAULT NULL COMMENT '客户ID',
    sales_order_code VARCHAR(64) DEFAULT NULL COMMENT '销售订单编号',
    purchase_order_code VARCHAR(64) DEFAULT NULL COMMENT '采购订单编号',
    work_order_id BIGINT DEFAULT NULL COMMENT '生产工单ID',
    task_id BIGINT DEFAULT NULL COMMENT '生产任务ID',
    workstation_id BIGINT DEFAULT NULL COMMENT '工作站ID',
    tool_id BIGINT DEFAULT NULL COMMENT '工具ID',
    mold_id BIGINT DEFAULT NULL COMMENT '模具 ID',
    lot_number VARCHAR(128) DEFAULT NULL COMMENT '生产批号',
    quality_status INT DEFAULT NULL COMMENT '质量状态',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_code (code),
    KEY idx_item_id (item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='批次管理';

-- ----------------------------
-- MES 物料消耗记录（mes_wm_item_consume）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_wm_item_consume (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    work_order_id BIGINT DEFAULT NULL COMMENT '生产工单编号',
    task_id BIGINT DEFAULT NULL COMMENT '生产任务编号',
    workstation_id BIGINT DEFAULT NULL COMMENT '工作站编号',
    process_id BIGINT DEFAULT NULL COMMENT '工序编号',
    feedback_id BIGINT DEFAULT NULL COMMENT '报工记录编号',
    consume_date DATETIME DEFAULT NULL COMMENT '消耗日期',
    status INT DEFAULT NULL COMMENT '状态',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_work_order_id (work_order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 物料消耗记录';

-- ----------------------------
-- MES 物料消耗记录明细（mes_wm_item_consume_detail）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_wm_item_consume_detail (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    consume_id BIGINT DEFAULT NULL COMMENT '消耗记录编号',
    line_id BIGINT DEFAULT NULL COMMENT '消耗记录行编号',
    material_stock_id BIGINT DEFAULT NULL COMMENT '库存台账编号',
    item_id BIGINT DEFAULT NULL COMMENT '物料编号',
    quantity DECIMAL(20,6) DEFAULT NULL COMMENT '消耗数量',
    batch_id BIGINT DEFAULT NULL COMMENT '批次编号',
    batch_code VARCHAR(128) DEFAULT NULL COMMENT '批次号',
    warehouse_id BIGINT DEFAULT NULL COMMENT '仓库编号',
    location_id BIGINT DEFAULT NULL COMMENT '库区编号',
    area_id BIGINT DEFAULT NULL COMMENT '库位编号',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_consume_id (consume_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 物料消耗记录明细';

-- ----------------------------
-- MES 物料消耗记录行（mes_wm_item_consume_line）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_wm_item_consume_line (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    consume_id BIGINT DEFAULT NULL COMMENT '消耗记录编号',
    item_id BIGINT DEFAULT NULL COMMENT '物料编号',
    quantity DECIMAL(20,6) DEFAULT NULL COMMENT '消耗数量',
    batch_id BIGINT DEFAULT NULL COMMENT '批次编号',
    batch_code VARCHAR(128) DEFAULT NULL COMMENT '批次号',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_consume_id (consume_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 物料消耗记录行';

-- ----------------------------
-- MES 采购入库单（mes_wm_item_receipt）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_wm_item_receipt (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    code VARCHAR(64) DEFAULT NULL COMMENT '入库单编码',
    name VARCHAR(255) DEFAULT NULL COMMENT '入库单名称',
    iqc_id BIGINT DEFAULT NULL COMMENT '来料检验单编号',
    notice_id BIGINT DEFAULT NULL COMMENT '到货通知单编号',
    purchase_order_code VARCHAR(64) DEFAULT NULL COMMENT '采购订单号',
    vendor_id BIGINT DEFAULT NULL COMMENT '供应商编号',
    receipt_date DATETIME DEFAULT NULL COMMENT '入库日期',
    status INT DEFAULT NULL COMMENT '状态',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_code (code),
    KEY idx_iqc_id (iqc_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 采购入库单';

-- ----------------------------
-- MES 采购入库明细（mes_wm_item_receipt_detail）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_wm_item_receipt_detail (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    line_id BIGINT DEFAULT NULL COMMENT '入库单行编号',
    receipt_id BIGINT DEFAULT NULL COMMENT '入库单编号',
    item_id BIGINT DEFAULT NULL COMMENT '物料编号',
    quantity DECIMAL(20,6) DEFAULT NULL COMMENT '上架数量',
    batch_id BIGINT DEFAULT NULL COMMENT '批次编号',
    warehouse_id BIGINT DEFAULT NULL COMMENT '仓库编号',
    location_id BIGINT DEFAULT NULL COMMENT '库区编号',
    area_id BIGINT DEFAULT NULL COMMENT '库位编号',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_line_id (line_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 采购入库明细';

-- ----------------------------
-- MES 采购入库单行（mes_wm_item_receipt_line）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_wm_item_receipt_line (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    receipt_id BIGINT DEFAULT NULL COMMENT '入库单编号',
    arrival_notice_line_id BIGINT DEFAULT NULL COMMENT '到货通知单行编号',
    item_id BIGINT DEFAULT NULL COMMENT '物料编号',
    received_quantity DECIMAL(20,6) DEFAULT NULL COMMENT '入库数量',
    batch_id BIGINT DEFAULT NULL COMMENT '批次编号',
    batch_code VARCHAR(128) DEFAULT NULL COMMENT '批次编码',
    production_date DATETIME DEFAULT NULL COMMENT '生产日期',
    expire_date DATETIME DEFAULT NULL COMMENT '有效期',
    lot_number VARCHAR(128) DEFAULT NULL COMMENT '生产批号',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_receipt_id (receipt_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 采购入库单行';

-- ----------------------------
-- MES 库存台账（仓库现有量）DO（mes_wm_material_stock）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_wm_material_stock (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    item_type_id BIGINT DEFAULT NULL COMMENT '物料分类编号',
    item_id BIGINT DEFAULT NULL COMMENT '物料编号',
    batch_id BIGINT DEFAULT NULL COMMENT '批次编号',
    batch_code VARCHAR(128) DEFAULT NULL COMMENT '批次号',
    warehouse_id BIGINT DEFAULT NULL COMMENT '仓库编号',
    location_id BIGINT DEFAULT NULL COMMENT '库区编号',
    area_id BIGINT DEFAULT NULL COMMENT '库位编号',
    vendor_id BIGINT DEFAULT NULL COMMENT '供应商编号',
    quantity DECIMAL(20,6) DEFAULT NULL COMMENT '在库数量',
    receipt_time DATETIME DEFAULT NULL COMMENT '入库时间',
    frozen BIT(1) DEFAULT NULL COMMENT '是否冻结',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_item_type_id (item_type_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 库存台账（仓库现有量）DO';

-- ----------------------------
-- MES 杂项出库单（mes_wm_misc_issue）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_wm_misc_issue (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    code VARCHAR(64) DEFAULT NULL COMMENT '出库单编号',
    name VARCHAR(255) DEFAULT NULL COMMENT '出库单名称',
    type INT DEFAULT NULL COMMENT '杂项类型',
    source_doc_type VARCHAR(64) DEFAULT NULL COMMENT '来源单据类型',
    source_doc_id BIGINT DEFAULT NULL COMMENT '来源单据 ID',
    source_doc_code VARCHAR(64) DEFAULT NULL COMMENT '来源单据编号',
    issue_date DATETIME DEFAULT NULL COMMENT '出库日期',
    status INT DEFAULT NULL COMMENT '状态',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_code (code),
    KEY idx_source_doc_id (source_doc_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 杂项出库单';

-- ----------------------------
-- MES 杂项出库明细（mes_wm_misc_issue_detail）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_wm_misc_issue_detail (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    issue_id BIGINT DEFAULT NULL COMMENT '出库单ID',
    line_id BIGINT DEFAULT NULL COMMENT '行ID',
    material_stock_id BIGINT DEFAULT NULL COMMENT '库存记录ID',
    item_id BIGINT DEFAULT NULL COMMENT '物料ID',
    quantity DECIMAL(20,6) DEFAULT NULL COMMENT '出库数量',
    batch_id BIGINT DEFAULT NULL COMMENT '批次ID',
    batch_code VARCHAR(128) DEFAULT NULL COMMENT '批次号',
    warehouse_id BIGINT DEFAULT NULL COMMENT '仓库ID',
    location_id BIGINT DEFAULT NULL COMMENT '库区ID',
    area_id BIGINT DEFAULT NULL COMMENT '库位ID',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_issue_id (issue_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 杂项出库明细';

-- ----------------------------
-- MES 杂项出库单行（mes_wm_misc_issue_line）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_wm_misc_issue_line (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    issue_id BIGINT DEFAULT NULL COMMENT '出库单编号',
    source_doc_line_id BIGINT DEFAULT NULL COMMENT '来源单据行ID',
    material_stock_id BIGINT DEFAULT NULL COMMENT '库存记录ID',
    item_id BIGINT DEFAULT NULL COMMENT '物料编号',
    quantity DECIMAL(20,6) DEFAULT NULL COMMENT '出库数量',
    batch_id BIGINT DEFAULT NULL COMMENT '批次编号',
    batch_code VARCHAR(128) DEFAULT NULL COMMENT '批次号',
    warehouse_id BIGINT DEFAULT NULL COMMENT '仓库编号',
    location_id BIGINT DEFAULT NULL COMMENT '库区编号',
    area_id BIGINT DEFAULT NULL COMMENT '库位编号',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_issue_id (issue_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 杂项出库单行';

-- ----------------------------
-- MES 杂项入库单（mes_wm_misc_receipt）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_wm_misc_receipt (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    code VARCHAR(64) DEFAULT NULL COMMENT '入库单编码',
    name VARCHAR(255) DEFAULT NULL COMMENT '入库单名称',
    type INT DEFAULT NULL COMMENT '杂项类型',
    source_doc_type VARCHAR(64) DEFAULT NULL COMMENT '来源单据类型',
    source_doc_id BIGINT DEFAULT NULL COMMENT '来源单据 ID',
    source_doc_code VARCHAR(64) DEFAULT NULL COMMENT '来源单据编码',
    receipt_date DATETIME DEFAULT NULL COMMENT '入库日期',
    status INT DEFAULT NULL COMMENT '状态',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_code (code),
    KEY idx_source_doc_id (source_doc_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 杂项入库单';

-- ----------------------------
-- MES 杂项入库明细（mes_wm_misc_receipt_detail）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_wm_misc_receipt_detail (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    receipt_id BIGINT DEFAULT NULL COMMENT '入库单ID',
    line_id BIGINT DEFAULT NULL COMMENT '行ID',
    item_id BIGINT DEFAULT NULL COMMENT '物料ID',
    quantity DECIMAL(20,6) DEFAULT NULL COMMENT '入库数量',
    batch_code VARCHAR(128) DEFAULT NULL COMMENT '批次号',
    warehouse_id BIGINT DEFAULT NULL COMMENT '仓库ID',
    location_id BIGINT DEFAULT NULL COMMENT '库区ID',
    area_id BIGINT DEFAULT NULL COMMENT '库位ID',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_receipt_id (receipt_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 杂项入库明细';

-- ----------------------------
-- MES 杂项入库单行（mes_wm_misc_receipt_line）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_wm_misc_receipt_line (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    receipt_id BIGINT DEFAULT NULL COMMENT '入库单编号',
    item_id BIGINT DEFAULT NULL COMMENT '物料编号',
    quantity DECIMAL(20,6) DEFAULT NULL COMMENT '入库数量',
    batch_code VARCHAR(128) DEFAULT NULL COMMENT '批次号',
    warehouse_id BIGINT DEFAULT NULL COMMENT '仓库编号',
    location_id BIGINT DEFAULT NULL COMMENT '库区编号',
    area_id BIGINT DEFAULT NULL COMMENT '库位编号',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_receipt_id (receipt_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 杂项入库单行';

-- ----------------------------
-- MES 外协发料单（mes_wm_outsource_issue）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_wm_outsource_issue (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    code VARCHAR(64) DEFAULT NULL COMMENT '发料单编号',
    name VARCHAR(255) DEFAULT NULL COMMENT '发料单名称',
    vendor_id BIGINT DEFAULT NULL COMMENT '供应商ID',
    work_order_id BIGINT DEFAULT NULL COMMENT '生产工单ID',
    issue_date DATETIME DEFAULT NULL COMMENT '发料日期',
    status INT DEFAULT NULL COMMENT '单据状态',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_code (code),
    KEY idx_vendor_id (vendor_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 外协发料单';

-- ----------------------------
-- MES 外协发料单明细（mes_wm_outsource_issue_detail）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_wm_outsource_issue_detail (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    line_id BIGINT DEFAULT NULL COMMENT '行ID',
    issue_id BIGINT DEFAULT NULL COMMENT '发料单ID',
    material_stock_id BIGINT DEFAULT NULL COMMENT '库存ID',
    item_id BIGINT DEFAULT NULL COMMENT '物料ID',
    quantity DECIMAL(20,6) DEFAULT NULL COMMENT '数量',
    batch_id BIGINT DEFAULT NULL COMMENT '批次ID',
    warehouse_id BIGINT DEFAULT NULL COMMENT '仓库ID',
    location_id BIGINT DEFAULT NULL COMMENT '库位ID',
    area_id BIGINT DEFAULT NULL COMMENT '库区ID',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_line_id (line_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 外协发料单明细';

-- ----------------------------
-- MES 外协发料单行（mes_wm_outsource_issue_line）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_wm_outsource_issue_line (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    issue_id BIGINT DEFAULT NULL COMMENT '发料单ID',
    material_stock_id BIGINT DEFAULT NULL COMMENT '库存ID',
    item_id BIGINT DEFAULT NULL COMMENT '物料ID',
    quantity DECIMAL(20,6) DEFAULT NULL COMMENT '发料数量',
    batch_id BIGINT DEFAULT NULL COMMENT '批次ID',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_issue_id (issue_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 外协发料单行';

-- ----------------------------
-- MES 外协入库单（mes_wm_outsource_receipt）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_wm_outsource_receipt (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    code VARCHAR(64) DEFAULT NULL COMMENT '入库单编码',
    name VARCHAR(255) DEFAULT NULL COMMENT '入库单名称',
    work_order_id BIGINT DEFAULT NULL COMMENT '外协工单编号',
    vendor_id BIGINT DEFAULT NULL COMMENT '供应商编号',
    receipt_date DATETIME DEFAULT NULL COMMENT '入库日期',
    status INT DEFAULT NULL COMMENT '状态',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_code (code),
    KEY idx_work_order_id (work_order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 外协入库单';

-- ----------------------------
-- MES 外协入库明细（mes_wm_outsource_receipt_detail）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_wm_outsource_receipt_detail (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    line_id BIGINT DEFAULT NULL COMMENT '入库单行编号',
    receipt_id BIGINT DEFAULT NULL COMMENT '入库单编号',
    item_id BIGINT DEFAULT NULL COMMENT '物料编号',
    quantity DECIMAL(20,6) DEFAULT NULL COMMENT '上架数量',
    batch_id BIGINT DEFAULT NULL COMMENT '批次编号',
    warehouse_id BIGINT DEFAULT NULL COMMENT '仓库编号',
    location_id BIGINT DEFAULT NULL COMMENT '库区编号',
    area_id BIGINT DEFAULT NULL COMMENT '库位编号',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_line_id (line_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 外协入库明细';

-- ----------------------------
-- MES 外协入库单行（mes_wm_outsource_receipt_line）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_wm_outsource_receipt_line (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    receipt_id BIGINT DEFAULT NULL COMMENT '入库单编号',
    item_id BIGINT DEFAULT NULL COMMENT '物料编号',
    quantity DECIMAL(20,6) DEFAULT NULL COMMENT '入库数量',
    batch_id BIGINT DEFAULT NULL COMMENT '批次编号',
    batch_code VARCHAR(128) DEFAULT NULL COMMENT '批次号',
    production_date DATETIME DEFAULT NULL COMMENT '生产日期',
    expire_date DATETIME DEFAULT NULL COMMENT '有效期',
    lot_number VARCHAR(128) DEFAULT NULL COMMENT '生产批号',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    iqc_id BIGINT DEFAULT NULL COMMENT '来料检验单编号',
    iqc_check_flag BIT(1) DEFAULT NULL COMMENT '是否需要质检',
    quality_status INT DEFAULT NULL COMMENT '质量状态',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_receipt_id (receipt_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 外协入库单行';

-- ----------------------------
-- MES 装箱单（mes_wm_package）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_wm_package (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    code VARCHAR(64) DEFAULT NULL COMMENT '装箱单编号',
    parent_id BIGINT DEFAULT NULL COMMENT '父箱 ID',
    package_date DATETIME DEFAULT NULL COMMENT '装箱日期',
    sales_order_code VARCHAR(64) DEFAULT NULL COMMENT '销售订单编号',
    invoice_code VARCHAR(64) DEFAULT NULL COMMENT '发票编号',
    client_id BIGINT DEFAULT NULL COMMENT '客户 ID',
    length DECIMAL(20,4) DEFAULT NULL COMMENT '箱长度',
    width DECIMAL(20,4) DEFAULT NULL COMMENT '箱宽度',
    height DECIMAL(20,4) DEFAULT NULL COMMENT '箱高度',
    size_unit_id BIGINT DEFAULT NULL COMMENT '尺寸单位 ID',
    net_weight DECIMAL(20,4) DEFAULT NULL COMMENT '净重',
    gross_weight DECIMAL(20,4) DEFAULT NULL COMMENT '毛重',
    weight_unit_id BIGINT DEFAULT NULL COMMENT '重量单位 ID',
    inspector_user_id BIGINT DEFAULT NULL COMMENT '检查员用户 ID',
    status INT DEFAULT NULL COMMENT '状态',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_code (code),
    KEY idx_parent_id (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 装箱单';

-- ----------------------------
-- MES 装箱明细（mes_wm_package_line）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_wm_package_line (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    package_id BIGINT DEFAULT NULL COMMENT '装箱单 ID',
    material_stock_id BIGINT DEFAULT NULL COMMENT '库存记录 ID',
    item_id BIGINT DEFAULT NULL COMMENT '产品物料 ID',
    quantity DECIMAL(20,6) DEFAULT NULL COMMENT '装箱数量',
    work_order_id BIGINT DEFAULT NULL COMMENT '生产工单 ID',
    expire_date DATETIME DEFAULT NULL COMMENT '有效期',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_package_id (package_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 装箱明细';

-- ----------------------------
-- MES 领料出库单（mes_wm_product_issue）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_wm_product_issue (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    code VARCHAR(64) DEFAULT NULL COMMENT '领料单编号',
    name VARCHAR(255) DEFAULT NULL COMMENT '领料单名称',
    workstation_id BIGINT DEFAULT NULL COMMENT '工作站 ID',
    work_order_id BIGINT DEFAULT NULL COMMENT '生产工单 ID',
    task_id BIGINT DEFAULT NULL COMMENT '生产任务 ID',
    issue_date DATETIME DEFAULT NULL COMMENT '领料日期',
    required_time DATETIME DEFAULT NULL COMMENT '需求时间',
    status INT DEFAULT NULL COMMENT '状态',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_code (code),
    KEY idx_workstation_id (workstation_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 领料出库单';

-- ----------------------------
-- MES 领料出库明细（mes_wm_product_issue_detail）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_wm_product_issue_detail (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    issue_id BIGINT DEFAULT NULL COMMENT '领料单ID',
    line_id BIGINT DEFAULT NULL COMMENT '行ID',
    material_stock_id BIGINT DEFAULT NULL COMMENT '库存记录ID',
    item_id BIGINT DEFAULT NULL COMMENT '物料ID',
    quantity DECIMAL(20,6) DEFAULT NULL COMMENT '领料数量',
    batch_id BIGINT DEFAULT NULL COMMENT '批次ID',
    batch_code VARCHAR(128) DEFAULT NULL COMMENT '批次号',
    warehouse_id BIGINT DEFAULT NULL COMMENT '仓库ID',
    location_id BIGINT DEFAULT NULL COMMENT '库区ID',
    area_id BIGINT DEFAULT NULL COMMENT '库位ID',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_issue_id (issue_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 领料出库明细';

-- ----------------------------
-- MES 领料出库单行（mes_wm_product_issue_line）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_wm_product_issue_line (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    issue_id BIGINT DEFAULT NULL COMMENT '领料单 ID',
    item_id BIGINT DEFAULT NULL COMMENT '物料 ID',
    quantity DECIMAL(20,6) DEFAULT NULL COMMENT '领料数量',
    batch_id BIGINT DEFAULT NULL COMMENT '批次 ID',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_issue_id (issue_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 领料出库单行';

-- ----------------------------
-- MES 生产入库单（mes_wm_product_produce）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_wm_product_produce (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    work_order_id BIGINT DEFAULT NULL COMMENT '生产工单 ID',
    feedback_id BIGINT DEFAULT NULL COMMENT '报工记录 ID',
    task_id BIGINT DEFAULT NULL COMMENT '生产任务 ID',
    workstation_id BIGINT DEFAULT NULL COMMENT '工作站 ID',
    process_id BIGINT DEFAULT NULL COMMENT '工序 ID',
    produce_date DATETIME DEFAULT NULL COMMENT '生产日期',
    status INT DEFAULT NULL COMMENT '状态',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_work_order_id (work_order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 生产入库单';

-- ----------------------------
-- MES 生产入库明细（mes_wm_product_produce_detail）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_wm_product_produce_detail (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    produce_id BIGINT DEFAULT NULL COMMENT '入库单 ID',
    line_id BIGINT DEFAULT NULL COMMENT '行 ID',
    item_id BIGINT DEFAULT NULL COMMENT '物料 ID',
    quantity DECIMAL(20,6) DEFAULT NULL COMMENT '入库数量',
    batch_id BIGINT DEFAULT NULL COMMENT '批次 ID',
    batch_code VARCHAR(128) DEFAULT NULL COMMENT '批次号',
    warehouse_id BIGINT DEFAULT NULL COMMENT '仓库 ID',
    location_id BIGINT DEFAULT NULL COMMENT '库区 ID',
    area_id BIGINT DEFAULT NULL COMMENT '库位 ID',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_produce_id (produce_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 生产入库明细';

-- ----------------------------
-- MES 生产入库单行（mes_wm_product_produce_line）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_wm_product_produce_line (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    produce_id BIGINT DEFAULT NULL COMMENT '入库单 ID',
    feedback_id BIGINT DEFAULT NULL COMMENT '报工记录 ID',
    item_id BIGINT DEFAULT NULL COMMENT '物料 ID',
    quantity DECIMAL(20,6) DEFAULT NULL COMMENT '入库数量',
    batch_id BIGINT DEFAULT NULL COMMENT '批次 ID',
    batch_code VARCHAR(128) DEFAULT NULL COMMENT '批次号',
    expire_date DATETIME DEFAULT NULL COMMENT '过期日期',
    lot_number VARCHAR(128) DEFAULT NULL COMMENT '生产批号',
    quality_status INT DEFAULT NULL COMMENT '质量状态',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_produce_id (produce_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 生产入库单行';

-- ----------------------------
-- MES 产品收货（入库）单（mes_wm_product_receipt）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_wm_product_receipt (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    code VARCHAR(64) DEFAULT NULL COMMENT '收货单编码',
    name VARCHAR(255) DEFAULT NULL COMMENT '收货单名称',
    work_order_id BIGINT DEFAULT NULL COMMENT '生产工单编号',
    item_id BIGINT DEFAULT NULL COMMENT '产品物料编号',
    receipt_date DATETIME DEFAULT NULL COMMENT '收货日期',
    status INT DEFAULT NULL COMMENT '状态',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_code (code),
    KEY idx_work_order_id (work_order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 产品收货（入库）单';

-- ----------------------------
-- MES 产品收货（入库）单明细（mes_wm_product_receipt_detail）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_wm_product_receipt_detail (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    line_id BIGINT DEFAULT NULL COMMENT '收货单行编号',
    receipt_id BIGINT DEFAULT NULL COMMENT '收货单编号',
    item_id BIGINT DEFAULT NULL COMMENT '物料编号',
    quantity DECIMAL(20,6) DEFAULT NULL COMMENT '上架数量',
    batch_id BIGINT DEFAULT NULL COMMENT '批次编号',
    warehouse_id BIGINT DEFAULT NULL COMMENT '仓库编号',
    location_id BIGINT DEFAULT NULL COMMENT '库区编号',
    area_id BIGINT DEFAULT NULL COMMENT '库位编号',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_line_id (line_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 产品收货（入库）单明细';

-- ----------------------------
-- MES 产品收货（入库）单行（mes_wm_product_receipt_line）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_wm_product_receipt_line (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    receipt_id BIGINT DEFAULT NULL COMMENT '收货单编号',
    item_id BIGINT DEFAULT NULL COMMENT '物料编号',
    material_stock_id BIGINT DEFAULT NULL COMMENT '库存物资记录编号',
    quantity DECIMAL(20,6) DEFAULT NULL COMMENT '收货数量',
    batch_id BIGINT DEFAULT NULL COMMENT '批次编号',
    batch_code VARCHAR(128) DEFAULT NULL COMMENT '批次号',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_receipt_id (receipt_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 产品收货（入库）单行';

-- ----------------------------
-- MES 销售出库单（mes_wm_product_sales）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_wm_product_sales (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    code VARCHAR(64) DEFAULT NULL COMMENT '出库单号',
    name VARCHAR(255) DEFAULT NULL COMMENT '出库单名称',
    client_id BIGINT DEFAULT NULL COMMENT '客户ID',
    sales_order_code VARCHAR(64) DEFAULT NULL COMMENT '销售订单号',
    notice_id BIGINT DEFAULT NULL COMMENT '发货通知单 ID',
    sales_date DATETIME DEFAULT NULL COMMENT '出库日期',
    contact_name VARCHAR(64) DEFAULT NULL COMMENT '联系人',
    contact_telephone VARCHAR(64) DEFAULT NULL COMMENT '联系电话',
    contact_address VARCHAR(255) DEFAULT NULL COMMENT '收货地址',
    carrier VARCHAR(64) DEFAULT NULL COMMENT '承运商',
    shipping_number VARCHAR(64) DEFAULT NULL COMMENT '运输单号',
    status INT DEFAULT NULL COMMENT '状态',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_code (code),
    KEY idx_client_id (client_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 销售出库单';

-- ----------------------------
-- MES 销售出库明细（mes_wm_product_sales_detail）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_wm_product_sales_detail (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    line_id BIGINT DEFAULT NULL COMMENT '出库单行ID',
    sales_id BIGINT DEFAULT NULL COMMENT '出库单ID',
    item_id BIGINT DEFAULT NULL COMMENT '物料ID',
    quantity DECIMAL(20,6) DEFAULT NULL COMMENT '拣货数量',
    material_stock_id BIGINT DEFAULT NULL COMMENT '库存记录ID',
    batch_id BIGINT DEFAULT NULL COMMENT '批次 ID',
    batch_code VARCHAR(128) DEFAULT NULL COMMENT '批次号',
    warehouse_id BIGINT DEFAULT NULL COMMENT '仓库ID',
    location_id BIGINT DEFAULT NULL COMMENT '库区ID',
    area_id BIGINT DEFAULT NULL COMMENT '库位ID',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_line_id (line_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 销售出库明细';

-- ----------------------------
-- MES 销售出库单行（mes_wm_product_sales_line）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_wm_product_sales_line (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    sales_id BIGINT DEFAULT NULL COMMENT '出库单ID',
    notice_line_id BIGINT DEFAULT NULL COMMENT '发货通知单行ID',
    item_id BIGINT DEFAULT NULL COMMENT '物料ID',
    quantity DECIMAL(20,6) DEFAULT NULL COMMENT '出库数量',
    batch_id BIGINT DEFAULT NULL COMMENT '批次 ID',
    batch_code VARCHAR(128) DEFAULT NULL COMMENT '批次号',
    material_stock_id BIGINT DEFAULT NULL COMMENT '库存记录ID',
    oqc_check_flag BIT(1) DEFAULT NULL COMMENT '是否出厂检验',
    oqc_id BIGINT DEFAULT NULL COMMENT '出厂检验单 ID',
    quality_status INT DEFAULT NULL COMMENT '质量状态',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_sales_id (sales_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 销售出库单行';

-- ----------------------------
-- MES 生产退料单（mes_wm_return_issue）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_wm_return_issue (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    code VARCHAR(64) DEFAULT NULL COMMENT '退料单编号',
    name VARCHAR(255) DEFAULT NULL COMMENT '退料单名称',
    work_order_id BIGINT DEFAULT NULL COMMENT '生产工单 ID',
    workstation_id BIGINT DEFAULT NULL COMMENT '工作站 ID',
    type INT DEFAULT NULL COMMENT '退料类型',
    return_date DATETIME DEFAULT NULL COMMENT '退料日期',
    status INT DEFAULT NULL COMMENT '状态',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_code (code),
    KEY idx_work_order_id (work_order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 生产退料单';

-- ----------------------------
-- MES 生产退料明细（mes_wm_return_issue_detail）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_wm_return_issue_detail (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    issue_id BIGINT DEFAULT NULL COMMENT '退料单 ID',
    line_id BIGINT DEFAULT NULL COMMENT '行 ID',
    material_stock_id BIGINT DEFAULT NULL COMMENT '库存记录 ID',
    item_id BIGINT DEFAULT NULL COMMENT '物料 ID',
    quantity DECIMAL(20,6) DEFAULT NULL COMMENT '退料数量',
    batch_id BIGINT DEFAULT NULL COMMENT '批次 ID',
    batch_code VARCHAR(128) DEFAULT NULL COMMENT '批次号',
    warehouse_id BIGINT DEFAULT NULL COMMENT '仓库 ID',
    location_id BIGINT DEFAULT NULL COMMENT '库区 ID',
    area_id BIGINT DEFAULT NULL COMMENT '库位 ID',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_issue_id (issue_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 生产退料明细';

-- ----------------------------
-- MES 生产退料单行（mes_wm_return_issue_line）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_wm_return_issue_line (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    issue_id BIGINT DEFAULT NULL COMMENT '退料单 ID',
    material_stock_id BIGINT DEFAULT NULL COMMENT '库存记录 ID',
    item_id BIGINT DEFAULT NULL COMMENT '物料 ID',
    quantity DECIMAL(20,6) DEFAULT NULL COMMENT '退料数量',
    batch_id BIGINT DEFAULT NULL COMMENT '批次 ID',
    batch_code VARCHAR(128) DEFAULT NULL COMMENT '批次编码',
    rqc_id BIGINT DEFAULT NULL COMMENT '退货检验单 ID',
    rqc_check_flag BIT(1) DEFAULT NULL COMMENT '是否需要质检',
    quality_status INT DEFAULT NULL COMMENT '质量状态',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_issue_id (issue_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 生产退料单行';

-- ----------------------------
-- MES 销售退货单（mes_wm_return_sales）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_wm_return_sales (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    code VARCHAR(64) DEFAULT NULL COMMENT '退货单编号',
    name VARCHAR(255) DEFAULT NULL COMMENT '退货单名称',
    sales_order_code VARCHAR(64) DEFAULT NULL COMMENT '销售订单编号',
    client_id BIGINT DEFAULT NULL COMMENT '客户 ID',
    return_date DATETIME DEFAULT NULL COMMENT '退货日期',
    return_reason VARCHAR(500) DEFAULT NULL COMMENT '退货原因',
    status INT DEFAULT NULL COMMENT '状态',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_code (code),
    KEY idx_client_id (client_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 销售退货单';

-- ----------------------------
-- MES 销售退货明细（mes_wm_return_sales_detail）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_wm_return_sales_detail (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    return_id BIGINT DEFAULT NULL COMMENT '退货单 ID',
    line_id BIGINT DEFAULT NULL COMMENT '行 ID',
    item_id BIGINT DEFAULT NULL COMMENT '物料 ID',
    quantity DECIMAL(20,6) DEFAULT NULL COMMENT '数量',
    batch_id BIGINT DEFAULT NULL COMMENT '批次 ID',
    batch_code VARCHAR(128) DEFAULT NULL COMMENT '批次号',
    warehouse_id BIGINT DEFAULT NULL COMMENT '仓库 ID',
    location_id BIGINT DEFAULT NULL COMMENT '库区 ID',
    area_id BIGINT DEFAULT NULL COMMENT '库位 ID',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_return_id (return_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 销售退货明细';

-- ----------------------------
-- MES 销售退货单行（mes_wm_return_sales_line）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_wm_return_sales_line (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    return_id BIGINT DEFAULT NULL COMMENT '退货单 ID',
    item_id BIGINT DEFAULT NULL COMMENT '物料 ID',
    quantity DECIMAL(20,6) DEFAULT NULL COMMENT '退货数量',
    batch_id BIGINT DEFAULT NULL COMMENT '批次 ID',
    batch_code VARCHAR(128) DEFAULT NULL COMMENT '批次号',
    rqc_id BIGINT DEFAULT NULL COMMENT '退货检验单 ID',
    rqc_check_flag BIT(1) DEFAULT NULL COMMENT '是否需要质检',
    quality_status INT DEFAULT NULL COMMENT '质量状态',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_return_id (return_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 销售退货单行';

-- ----------------------------
-- MES 供应商退货单（mes_wm_return_vendor）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_wm_return_vendor (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    code VARCHAR(64) DEFAULT NULL COMMENT '退货单编号',
    name VARCHAR(255) DEFAULT NULL COMMENT '退货单名称',
    purchase_order_code VARCHAR(64) DEFAULT NULL COMMENT '采购订单编号',
    vendor_id BIGINT DEFAULT NULL COMMENT '供应商 ID',
    return_date DATETIME DEFAULT NULL COMMENT '退货日期',
    return_reason VARCHAR(500) DEFAULT NULL COMMENT '退货原因',
    transport_code VARCHAR(64) DEFAULT NULL COMMENT '物流单号',
    transport_telephone VARCHAR(64) DEFAULT NULL COMMENT '物流联系电话',
    status INT DEFAULT NULL COMMENT '状态',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_code (code),
    KEY idx_vendor_id (vendor_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 供应商退货单';

-- ----------------------------
-- MES 供应商退货明细（mes_wm_return_vendor_detail）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_wm_return_vendor_detail (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    return_id BIGINT DEFAULT NULL COMMENT '退货单 ID',
    line_id BIGINT DEFAULT NULL COMMENT '行 ID',
    material_stock_id BIGINT DEFAULT NULL COMMENT '库存记录 ID',
    item_id BIGINT DEFAULT NULL COMMENT '物料 ID',
    quantity DECIMAL(20,6) DEFAULT NULL COMMENT '退货数量',
    batch_id BIGINT DEFAULT NULL COMMENT '批次 ID',
    batch_code VARCHAR(128) DEFAULT NULL COMMENT '批次号',
    warehouse_id BIGINT DEFAULT NULL COMMENT '仓库 ID',
    location_id BIGINT DEFAULT NULL COMMENT '库区 ID',
    area_id BIGINT DEFAULT NULL COMMENT '库位 ID',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_return_id (return_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 供应商退货明细';

-- ----------------------------
-- MES 供应商退货单行（mes_wm_return_vendor_line）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_wm_return_vendor_line (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    return_id BIGINT DEFAULT NULL COMMENT '退货单 ID',
    item_id BIGINT DEFAULT NULL COMMENT '物料 ID',
    quantity DECIMAL(20,6) DEFAULT NULL COMMENT '退货数量',
    batch_id BIGINT DEFAULT NULL COMMENT '批次 ID',
    batch_code VARCHAR(128) DEFAULT NULL COMMENT '批次号',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_return_id (return_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 供应商退货单行';

-- ----------------------------
-- MES 发货通知单（mes_wm_sales_notice）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_wm_sales_notice (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    code VARCHAR(64) DEFAULT NULL COMMENT '通知单编码',
    name VARCHAR(255) DEFAULT NULL COMMENT '通知单名称',
    sales_order_code VARCHAR(64) DEFAULT NULL COMMENT '销售订单编号',
    client_id BIGINT DEFAULT NULL COMMENT '客户编号',
    sales_date DATETIME DEFAULT NULL COMMENT '发货日期',
    recipient_name VARCHAR(64) DEFAULT NULL COMMENT '收货人',
    recipient_telephone VARCHAR(64) DEFAULT NULL COMMENT '联系方式',
    recipient_address VARCHAR(255) DEFAULT NULL COMMENT '收货地址',
    status INT DEFAULT NULL COMMENT '状态',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_code (code),
    KEY idx_client_id (client_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 发货通知单';

-- ----------------------------
-- MES 发货通知单行（mes_wm_sales_notice_line）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_wm_sales_notice_line (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    notice_id BIGINT DEFAULT NULL COMMENT '发货通知单编号',
    item_id BIGINT DEFAULT NULL COMMENT '物料编号',
    batch_id BIGINT DEFAULT NULL COMMENT '批次编号',
    batch_code VARCHAR(128) DEFAULT NULL COMMENT '批次号',
    quantity DECIMAL(20,6) DEFAULT NULL COMMENT '发货数量',
    oqc_check_flag BIT(1) DEFAULT NULL COMMENT '是否检验',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_notice_id (notice_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 发货通知单行';

-- ----------------------------
-- MES SN 码（mes_wm_sn）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_wm_sn (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    uuid VARCHAR(128) DEFAULT NULL COMMENT '批次 UUID（用于标记同一批次生成的 SN 码）',
    code VARCHAR(64) DEFAULT NULL COMMENT 'SN 码（唯一）',
    item_id BIGINT DEFAULT NULL COMMENT '物料编号',
    batch_code VARCHAR(128) DEFAULT NULL COMMENT '批次号',
    work_order_id BIGINT DEFAULT NULL COMMENT '生产工单编号',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_code (code),
    KEY idx_item_id (item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES SN 码';

-- ----------------------------
-- MES 盘点方案（mes_wm_stock_taking_plan）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_wm_stock_taking_plan (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    code VARCHAR(64) DEFAULT NULL COMMENT '方案编码',
    name VARCHAR(255) DEFAULT NULL COMMENT '方案名称',
    type INT DEFAULT NULL COMMENT '盘点类型',
    start_time DATETIME DEFAULT NULL COMMENT '计划开始时间',
    end_time DATETIME DEFAULT NULL COMMENT '计划结束时间',
    blind_flag BIT(1) DEFAULT NULL COMMENT '是否盲盘',
    frozen BIT(1) DEFAULT NULL COMMENT '是否冻结库存',
    status INT DEFAULT NULL COMMENT '状态',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 盘点方案';

-- ----------------------------
-- MES 盘点方案参数（mes_wm_stock_taking_plan_param）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_wm_stock_taking_plan_param (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    plan_id BIGINT DEFAULT NULL COMMENT '盘点方案编号',
    type INT DEFAULT NULL COMMENT '参数值类型',
    value_id BIGINT DEFAULT NULL COMMENT '参数值编号，例如仓库、库区、库位、物料、批次的主键 ID',
    value_code VARCHAR(128) DEFAULT NULL COMMENT '参数值编码，例如仓库编码、库区编码、库位编码、物料编码、批次编码',
    value_name VARCHAR(128) DEFAULT NULL COMMENT '参数值名称，例如仓库名称、库区名称、库位名称、物料名称、批次名称',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_plan_id (plan_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 盘点方案参数';

-- ----------------------------
-- MES 盘点任务（mes_wm_stock_taking_task）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_wm_stock_taking_task (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    code VARCHAR(64) DEFAULT NULL COMMENT '任务编码',
    name VARCHAR(255) DEFAULT NULL COMMENT '任务名称',
    taking_date DATETIME DEFAULT NULL COMMENT '盘点日期',
    type INT DEFAULT NULL COMMENT '盘点类型',
    user_id BIGINT DEFAULT NULL COMMENT '盘点人编号',
    plan_id BIGINT DEFAULT NULL COMMENT '盘点计划编号',
    blind_flag BIT(1) DEFAULT NULL COMMENT '是否盲盘',
    frozen BIT(1) DEFAULT NULL COMMENT '是否冻结库存',
    start_time DATETIME DEFAULT NULL COMMENT '开始时间',
    end_time DATETIME DEFAULT NULL COMMENT '结束时间',
    status INT DEFAULT NULL COMMENT '任务状态',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_code (code),
    KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 盘点任务';

-- ----------------------------
-- MES 盘点任务行（mes_wm_stock_taking_task_line）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_wm_stock_taking_task_line (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    task_id BIGINT DEFAULT NULL COMMENT '盘点任务编号',
    material_stock_id BIGINT DEFAULT NULL COMMENT '库存编号',
    item_id BIGINT DEFAULT NULL COMMENT '物料编号',
    batch_id BIGINT DEFAULT NULL COMMENT '批次编号',
    batch_code VARCHAR(128) DEFAULT NULL COMMENT '批次编码',
    quantity DECIMAL(20,6) DEFAULT NULL COMMENT '在库数量',
    taking_quantity DECIMAL(20,6) DEFAULT NULL COMMENT '盘点数量',
    warehouse_id BIGINT DEFAULT NULL COMMENT '仓库编号',
    location_id BIGINT DEFAULT NULL COMMENT '库位编号',
    area_id BIGINT DEFAULT NULL COMMENT '库区编号',
    status INT DEFAULT NULL COMMENT '盘点状态',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_task_id (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 盘点任务行';

-- ----------------------------
-- MES 盘点结果（mes_wm_stock_taking_task_result）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_wm_stock_taking_task_result (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    task_id BIGINT DEFAULT NULL COMMENT '盘点任务编号',
    line_id BIGINT DEFAULT NULL COMMENT '盘点任务行编号',
    material_stock_id BIGINT DEFAULT NULL COMMENT '库存编号',
    item_id BIGINT DEFAULT NULL COMMENT '物料编号',
    batch_id BIGINT DEFAULT NULL COMMENT '批次编号',
    batch_code VARCHAR(128) DEFAULT NULL COMMENT '批次编码',
    warehouse_id BIGINT DEFAULT NULL COMMENT '仓库编号',
    location_id BIGINT DEFAULT NULL COMMENT '库位编号',
    area_id BIGINT DEFAULT NULL COMMENT '库区编号',
    quantity DECIMAL(20,6) DEFAULT NULL COMMENT '在库数量',
    taking_quantity DECIMAL(20,6) DEFAULT NULL COMMENT '盘点数量',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_task_id (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 盘点结果';

-- ----------------------------
-- MES 库存事务流水（mes_wm_transaction）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_wm_transaction (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    type INT DEFAULT NULL COMMENT '事务类型',
    biz_type INT DEFAULT NULL COMMENT '业务类型',
    biz_id BIGINT DEFAULT NULL COMMENT '来源业务主单 ID',
    biz_code VARCHAR(64) DEFAULT NULL COMMENT '来源业务单号',
    biz_line_id BIGINT DEFAULT NULL COMMENT '来源业务行 ID',
    material_stock_id BIGINT DEFAULT NULL COMMENT '库存记录 ID',
    related_transaction_id BIGINT DEFAULT NULL COMMENT '关联的事务 ID',
    item_id BIGINT DEFAULT NULL COMMENT '物料 ID',
    quantity DECIMAL(20,6) DEFAULT NULL COMMENT '本次变动数量',
    batch_id BIGINT DEFAULT NULL COMMENT '批次 ID',
    batch_code VARCHAR(128) DEFAULT NULL COMMENT '批次号',
    warehouse_id BIGINT DEFAULT NULL COMMENT '仓库 ID',
    location_id BIGINT DEFAULT NULL COMMENT '库区 ID',
    area_id BIGINT DEFAULT NULL COMMENT '库位 ID',
    transaction_time DATETIME DEFAULT NULL COMMENT '事务发生时间',
    erp_time DATETIME DEFAULT NULL COMMENT 'ERP 账期',
    receipt_time DATETIME DEFAULT NULL COMMENT '入库时间',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_biz_id (biz_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 库存事务流水';

-- ----------------------------
-- MES 转移单（mes_wm_transfer）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_wm_transfer (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    code VARCHAR(64) DEFAULT NULL COMMENT '转移单编号',
    name VARCHAR(255) DEFAULT NULL COMMENT '转移单名称',
    type INT DEFAULT NULL COMMENT '转移单类型',
    delivery_flag BIT(1) DEFAULT NULL COMMENT '是否配送',
    recipient_name VARCHAR(64) DEFAULT NULL COMMENT '收货人',
    recipient_telephone VARCHAR(64) DEFAULT NULL COMMENT '联系方式',
    destination_address VARCHAR(255) DEFAULT NULL COMMENT '目的地',
    carrier VARCHAR(64) DEFAULT NULL COMMENT '承运商',
    shipping_number VARCHAR(64) DEFAULT NULL COMMENT '运输单号',
    confirm_flag BIT(1) DEFAULT NULL COMMENT '是否已确认',
    transfer_date DATETIME DEFAULT NULL COMMENT '转移日期',
    status INT DEFAULT NULL COMMENT '状态',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 转移单';

-- ----------------------------
-- MES 调拨明细（mes_wm_transfer_detail）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_wm_transfer_detail (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    line_id BIGINT DEFAULT NULL COMMENT '转移单行编号',
    transfer_id BIGINT DEFAULT NULL COMMENT '转移单编号',
    item_id BIGINT DEFAULT NULL COMMENT '物料编号',
    quantity DECIMAL(20,6) DEFAULT NULL COMMENT '上架数量',
    batch_id BIGINT DEFAULT NULL COMMENT '批次编号',
    to_warehouse_id BIGINT DEFAULT NULL COMMENT '移入仓库编号',
    to_location_id BIGINT DEFAULT NULL COMMENT '移入库区编号',
    to_area_id BIGINT DEFAULT NULL COMMENT '移入库位编号',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_line_id (line_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 调拨明细';

-- ----------------------------
-- MES 转移单行（mes_wm_transfer_line）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_wm_transfer_line (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    transfer_id BIGINT DEFAULT NULL COMMENT '转移单编号',
    material_stock_id BIGINT DEFAULT NULL COMMENT '库存记录编号',
    item_id BIGINT DEFAULT NULL COMMENT '物料编号',
    quantity DECIMAL(20,6) DEFAULT NULL COMMENT '转移数量',
    batch_id BIGINT DEFAULT NULL COMMENT '批次编号',
    from_warehouse_id BIGINT DEFAULT NULL COMMENT '移出仓库编号',
    from_location_id BIGINT DEFAULT NULL COMMENT '移出库区编号',
    from_area_id BIGINT DEFAULT NULL COMMENT '移出库位编号',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_transfer_id (transfer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 转移单行';

-- ----------------------------
-- MES 仓库（mes_wm_warehouse）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_wm_warehouse (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    code VARCHAR(64) DEFAULT NULL COMMENT '仓库编码',
    name VARCHAR(255) DEFAULT NULL COMMENT '仓库名称',
    address VARCHAR(255) DEFAULT NULL COMMENT '仓库地址',
    area DECIMAL(20,4) DEFAULT NULL COMMENT '面积',
    charge_user_id BIGINT DEFAULT NULL COMMENT '负责人用户编号',
    frozen BIT(1) DEFAULT NULL COMMENT '是否冻结',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_code (code),
    KEY idx_charge_user_id (charge_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 仓库';

-- ----------------------------
-- MES 库位（mes_wm_warehouse_area）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_wm_warehouse_area (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    code VARCHAR(64) DEFAULT NULL COMMENT '库位编码',
    name VARCHAR(255) DEFAULT NULL COMMENT '库位名称',
    location_id BIGINT DEFAULT NULL COMMENT '库区编号',
    area DECIMAL(20,4) DEFAULT NULL COMMENT '面积',
    max_load DECIMAL(20,4) DEFAULT NULL COMMENT '最大载重',
    position_x INT DEFAULT NULL COMMENT '位置 X',
    position_y INT DEFAULT NULL COMMENT '位置 Y',
    position_z INT DEFAULT NULL COMMENT '位置 Z',
    status INT DEFAULT NULL COMMENT '状态',
    frozen BIT(1) DEFAULT NULL COMMENT '是否冻结',
    allow_item_mixing BIT(1) DEFAULT NULL COMMENT '是否允许物料混放',
    allow_batch_mixing BIT(1) DEFAULT NULL COMMENT '是否允许批次混放',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_code (code),
    KEY idx_location_id (location_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 库位';

-- ----------------------------
-- MES 库区（mes_wm_warehouse_location）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_wm_warehouse_location (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    code VARCHAR(64) DEFAULT NULL COMMENT '库区编码',
    name VARCHAR(255) DEFAULT NULL COMMENT '库区名称',
    warehouse_id BIGINT DEFAULT NULL COMMENT '仓库编号',
    area DECIMAL(20,4) DEFAULT NULL COMMENT '面积',
    frozen BIT(1) DEFAULT NULL COMMENT '是否冻结',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_code (code),
    KEY idx_warehouse_id (warehouse_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 库区';

-- ============================================================
-- APS 高级排产 / MRP 物料需求计划 / OEE 设备综合效率 子域
-- ============================================================

-- ----------------------------
-- MES 排产计划（mes_pro_aps_plan）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_pro_aps_plan (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    plan_no VARCHAR(64) DEFAULT NULL COMMENT '排产计划编号',
    work_order_id BIGINT DEFAULT NULL COMMENT '生产工单 ID',
    product_id BIGINT DEFAULT NULL COMMENT '产品 ID',
    workstation_id BIGINT DEFAULT NULL COMMENT '工位 ID',
    planned_start_time DATETIME DEFAULT NULL COMMENT '计划开始时间',
    planned_end_time DATETIME DEFAULT NULL COMMENT '计划结束时间',
    quantity DECIMAL(20,6) DEFAULT NULL COMMENT '排产数量',
    priority INT DEFAULT NULL COMMENT '优先级',
    status INT DEFAULT NULL COMMENT '状态',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_plan_no (plan_no),
    KEY idx_work_order_id (work_order_id),
    KEY idx_workstation_id (workstation_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES 排产计划';

-- ----------------------------
-- MES MRP 计划（mes_pro_mrp_plan）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_pro_mrp_plan (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    plan_no VARCHAR(64) DEFAULT NULL COMMENT 'MRP 计划编号',
    plan_date DATETIME DEFAULT NULL COMMENT '计划日期',
    status INT DEFAULT NULL COMMENT '状态',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_plan_no (plan_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES MRP 计划';

-- ----------------------------
-- MES MRP 计算结果（mes_pro_mrp_result）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_pro_mrp_result (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    plan_id BIGINT DEFAULT NULL COMMENT 'MRP 计划 ID',
    product_id BIGINT DEFAULT NULL COMMENT '物料 ID',
    requirement_qty DECIMAL(20,6) DEFAULT NULL COMMENT '需求量',
    stock_qty DECIMAL(20,6) DEFAULT NULL COMMENT '库存量',
    net_requirement DECIMAL(20,6) DEFAULT NULL COMMENT '净需求',
    planned_order_qty DECIMAL(20,6) DEFAULT NULL COMMENT '计划订单量',
    planned_order_date DATETIME DEFAULT NULL COMMENT '计划订单日期',
    supplier_id BIGINT DEFAULT NULL COMMENT '供应商 ID',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_plan_id (plan_id),
    KEY idx_product_id (product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES MRP 计算结果';

-- ----------------------------
-- MES OEE 记录（mes_dv_oee_record）
-- ----------------------------
CREATE TABLE IF NOT EXISTS mes_dv_oee_record (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    machinery_id BIGINT DEFAULT NULL COMMENT '设备 ID',
    record_date DATETIME DEFAULT NULL COMMENT '记录日期',
    planned_production_time DECIMAL(20,6) DEFAULT NULL COMMENT '计划生产时间（分钟）',
    run_time DECIMAL(20,6) DEFAULT NULL COMMENT '实际运行时间（分钟）',
    ideal_cycle_time DECIMAL(20,6) DEFAULT NULL COMMENT '理论节拍（分钟/件）',
    total_produced DECIMAL(20,6) DEFAULT NULL COMMENT '总产量',
    good_produced DECIMAL(20,6) DEFAULT NULL COMMENT '合格产量',
    availability DECIMAL(10,4) DEFAULT NULL COMMENT '可用率',
    performance DECIMAL(10,4) DEFAULT NULL COMMENT '表现率',
    quality DECIMAL(10,4) DEFAULT NULL COMMENT '质量率',
    oee DECIMAL(10,4) DEFAULT NULL COMMENT 'OEE 值',
    remark VARCHAR(500) DEFAULT NULL COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户 ID',
    PRIMARY KEY (id),
    KEY idx_tenant_id (tenant_id),
    KEY idx_machinery_id (machinery_id),
    KEY idx_record_date (record_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='MES OEE 记录';

