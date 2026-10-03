-- ======================== 会员中心建表脚本 ========================
-- 作者：zhicloud
-- 说明：由 DO 类字段生成，主键自增（MySQL id-type=AUTO），基础字段对齐 BaseDO/TenantBaseDO

-- ----------------------------
-- 用户收件地址 DO (MemberAddressDO)
-- ----------------------------
CREATE TABLE IF NOT EXISTS member_address (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    PRIMARY KEY (id),
    user_id BIGINT COMMENT '用户编号',
    name VARCHAR(128) COMMENT '收件人名称',
    mobile VARCHAR(255) COMMENT '手机号',
    area_id BIGINT COMMENT '地区编号',
    detail_address VARCHAR(255) COMMENT '收件详细地址',
    default_status BIT(1) COMMENT '是否默认',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    KEY idx_tenant_id (tenant_id)
) COMMENT='用户收件地址 DO';

-- ----------------------------
-- 会员配置 DO (MemberConfigDO)
-- ----------------------------
CREATE TABLE IF NOT EXISTS member_config (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '自增主键',
    PRIMARY KEY (id),
    point_trade_deduct_enable BIT(1) COMMENT '积分抵扣开关',
    point_trade_deduct_unit_price INT COMMENT '积分抵扣，单位：分',
    point_trade_deduct_max_price INT COMMENT '积分抵扣最大值',
    point_trade_give_point INT COMMENT '1 元赠送多少分',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    KEY idx_tenant_id (tenant_id)
) COMMENT='会员配置 DO';

-- ----------------------------
-- 用户分组 DO (MemberGroupDO)
-- ----------------------------
CREATE TABLE IF NOT EXISTS member_group (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    PRIMARY KEY (id),
    name VARCHAR(128) COMMENT '名称',
    remark VARCHAR(512) COMMENT '备注',
    status INT COMMENT '状态',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    KEY idx_tenant_id (tenant_id)
) COMMENT='用户分组 DO';

-- ----------------------------
-- 会员经验记录 DO (MemberExperienceRecordDO)
-- ----------------------------
CREATE TABLE IF NOT EXISTS member_experience_record (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    PRIMARY KEY (id),
    user_id BIGINT COMMENT '用户编号',
    biz_type INT COMMENT '业务类型',
    biz_id VARCHAR(255) COMMENT '业务编号',
    title VARCHAR(128) COMMENT '标题',
    description VARCHAR(512) COMMENT '描述',
    experience INT COMMENT '经验',
    total_experience INT COMMENT '变更后的经验',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    KEY idx_tenant_id (tenant_id)
) COMMENT='会员经验记录 DO';

-- ----------------------------
-- 会员等级 DO (MemberLevelDO)
-- ----------------------------
CREATE TABLE IF NOT EXISTS member_level (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    PRIMARY KEY (id),
    name VARCHAR(128) COMMENT '等级名称',
    level INT COMMENT '等级',
    experience INT COMMENT '升级经验',
    discount_percent INT COMMENT '享受折扣',
    icon VARCHAR(512) COMMENT '等级图标',
    background_url VARCHAR(512) COMMENT '等级背景图',
    status INT COMMENT '状态',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    KEY idx_tenant_id (tenant_id)
) COMMENT='会员等级 DO';

-- ----------------------------
-- 会员等级记录 DO (MemberLevelRecordDO)
-- ----------------------------
CREATE TABLE IF NOT EXISTS member_level_record (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    PRIMARY KEY (id),
    user_id BIGINT COMMENT '用户编号',
    level_id BIGINT COMMENT '等级编号',
    level INT COMMENT '会员等级',
    discount_percent INT COMMENT '享受折扣',
    experience INT COMMENT '升级经验',
    user_experience INT COMMENT '会员此时的经验',
    remark VARCHAR(512) COMMENT '备注',
    description VARCHAR(512) COMMENT '描述',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    KEY idx_tenant_id (tenant_id)
) COMMENT='会员等级记录 DO';

-- ----------------------------
-- 用户积分记录 DO (MemberPointRecordDO)
-- ----------------------------
CREATE TABLE IF NOT EXISTS member_point_record (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '自增主键',
    PRIMARY KEY (id),
    user_id BIGINT COMMENT '用户编号',
    biz_id VARCHAR(255) COMMENT '业务编码',
    biz_type INT COMMENT '业务类型',
    title VARCHAR(128) COMMENT '积分标题',
    description VARCHAR(512) COMMENT '积分描述',
    point INT COMMENT '变动积分',
    total_point INT COMMENT '变动后的积分',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    KEY idx_tenant_id (tenant_id)
) COMMENT='用户积分记录 DO';

-- ----------------------------
-- 签到规则 DO (MemberSignInConfigDO)
-- ----------------------------
CREATE TABLE IF NOT EXISTS member_sign_in_config (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '规则自增主键',
    PRIMARY KEY (id),
    day INT COMMENT '签到第 x 天',
    point INT COMMENT '奖励积分',
    experience INT COMMENT '奖励经验',
    status INT COMMENT '状态',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    KEY idx_tenant_id (tenant_id)
) COMMENT='签到规则 DO';

-- ----------------------------
-- 签到记录 DO (MemberSignInRecordDO)
-- ----------------------------
CREATE TABLE IF NOT EXISTS member_sign_in_record (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    PRIMARY KEY (id),
    user_id BIGINT COMMENT '签到用户',
    day INT COMMENT '第几天签到',
    point INT COMMENT '签到的积分',
    experience INT COMMENT '签到的经验',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    KEY idx_tenant_id (tenant_id)
) COMMENT='签到记录 DO';

-- ----------------------------
-- 会员标签 DO (MemberTagDO)
-- ----------------------------
CREATE TABLE IF NOT EXISTS member_tag (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '编号',
    PRIMARY KEY (id),
    name VARCHAR(128) COMMENT '标签名称',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    KEY idx_tenant_id (tenant_id)
) COMMENT='会员标签 DO';

-- ----------------------------
-- 会员用户 DO (MemberUserDO)
-- ----------------------------
CREATE TABLE IF NOT EXISTS member_user (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    PRIMARY KEY (id),
    mobile VARCHAR(255) COMMENT '手机',
    email VARCHAR(255) COMMENT '邮箱',
    password VARCHAR(255) COMMENT '加密后的密码',
    status INT COMMENT '帐号状态',
    register_ip VARCHAR(255) COMMENT '注册 IP',
    register_terminal INT COMMENT '注册终端',
    login_ip VARCHAR(255) COMMENT '最后登录IP',
    login_date DATETIME COMMENT '最后登录时间',
    nickname VARCHAR(128) COMMENT '用户昵称',
    avatar VARCHAR(512) COMMENT '用户头像',
    name VARCHAR(128) COMMENT '真实名字',
    sex INT COMMENT '性别',
    birthday DATETIME COMMENT '出生日期',
    area_id INT COMMENT '所在地',
    mark VARCHAR(255) COMMENT '用户备注',
    point INT COMMENT '积分',
    tag_ids TEXT COMMENT '会员标签列表，以逗号分隔',
    level_id BIGINT COMMENT '会员级别编号',
    experience INT COMMENT '会员经验',
    group_id BIGINT COMMENT '用户分组编号',
    creator VARCHAR(64) COMMENT '创建者',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) COMMENT '更新者',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    KEY idx_tenant_id (tenant_id)
) COMMENT='会员用户 DO';
