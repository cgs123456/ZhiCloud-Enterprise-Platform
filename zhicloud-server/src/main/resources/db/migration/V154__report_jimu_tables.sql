-- V154: 积木报表(jimureport-spring-boot3-starter 2.3.4 / jimubi 2.3.2)所需的 jimu_* 表
-- 来源: JeecgBoot 官方全量脚本 jeecgboot-mysql-5.7.sql (master, 2026-10 抓取) 的 16 张 jimu_* 建表语句;
-- 校验: 与 jar 内实体/DAO SQL 逐列核对, jimu_report_export_log 补 export_from/from_id/err_msg/from_job_name 四列(jar 2.3.4 DAO 需要, 官方脚本滞后)。
-- 只带建表与字典/报表分类种子; 官方 36 个演示报表及其数据源/字段种子(~934KB, 依赖 demo 表)不带。
-- 幂等: CREATE TABLE IF NOT EXISTS。


CREATE TABLE IF NOT EXISTS `jimu_dict`  (
  `id` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `dict_name` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '字典名称',
  `dict_code` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '字典编码',
  `description` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '描述',
  `del_flag` int(1) NULL DEFAULT NULL COMMENT '删除状态',
  `create_by` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `type` int(1) UNSIGNED ZEROFILL NULL DEFAULT 0 COMMENT '字典类型0为string,1为number',
  `tenant_id` varchar(10) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '多租户标识',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_jd_dict_code`(`dict_code`) USING BTREE,
  INDEX `idx_jd_tenant_id`(`tenant_id`) USING BTREE,
  INDEX `idx_jd_create_by`(`create_by`) USING BTREE,
  INDEX `idx_jd_dict_code_tenant_id`(`dict_code`, `tenant_id`) USING BTREE,
  INDEX `idx_jd_dict_code_create_by`(`dict_code`, `create_by`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = DYNAMIC;

CREATE TABLE IF NOT EXISTS `jimu_dict_item`  (
  `id` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `dict_id` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '字典id',
  `item_text` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '字典项文本',
  `item_value` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '字典项值',
  `description` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '描述',
  `sort_order` int(10) NULL DEFAULT NULL COMMENT '排序',
  `status` int(11) NULL DEFAULT NULL COMMENT '状态（1启用 0不启用）',
  `create_by` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `create_time` datetime NULL DEFAULT NULL,
  `update_by` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL,
  `update_time` datetime NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_sdi_role_dict_id`(`dict_id`) USING BTREE,
  INDEX `idx_sdi_role_sort_order`(`sort_order`) USING BTREE,
  INDEX `idx_sdi_status`(`status`) USING BTREE,
  INDEX `idx_sdi_dict_val`(`dict_id`, `item_value`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = DYNAMIC;

CREATE TABLE IF NOT EXISTS `jimu_report`  (
  `id` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '主键',
  `code` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '编码',
  `name` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '名称',
  `note` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '说明',
  `status` varchar(10) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '状态',
  `type` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '类型',
  `json_str` longtext CHARACTER SET utf8 COLLATE utf8_general_ci NULL COMMENT 'json字符串',
  `api_url` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '请求地址',
  `thumb` text CHARACTER SET utf8 COLLATE utf8_general_ci NULL COMMENT '缩略图',
  `create_by` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '修改人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '修改时间',
  `del_flag` tinyint(1) NULL DEFAULT NULL COMMENT '删除标识0-正常,1-已删除',
  `api_method` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '请求方法0-get,1-post',
  `api_code` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '请求编码',
  `template` tinyint(1) NULL DEFAULT NULL COMMENT '是否是模板 0不是,1是',
  `view_count` bigint(15) NULL DEFAULT 0 COMMENT '浏览次数',
  `css_str` text CHARACTER SET utf8 COLLATE utf8_general_ci NULL COMMENT 'css增强',
  `js_str` text CHARACTER SET utf8 COLLATE utf8_general_ci NULL COMMENT 'js增强',
  `py_str` text CHARACTER SET utf8 COLLATE utf8_general_ci NULL COMMENT 'py增强',
  `tenant_id` varchar(10) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '多租户标识',
  `update_count` int(11) NULL DEFAULT 0 COMMENT '乐观锁版本',
  `submit_form` tinyint(1) NULL DEFAULT NULL COMMENT '是否填报报表 0不是,1是',
  `is_multi_sheet` tinyint(4) NULL DEFAULT NULL COMMENT '是否多sheet报表 1是 0否',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uniq_jmreport_code`(`code`) USING BTREE,
  INDEX `uniq_jmreport_createby`(`create_by`) USING BTREE,
  INDEX `uniq_jmreport_delflag`(`del_flag`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '在线excel设计器' ROW_FORMAT = DYNAMIC;

CREATE TABLE IF NOT EXISTS `jimu_report_category`  (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '分类名称',
  `parent_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '父级id',
  `iz_leaf` int(1) NULL DEFAULT NULL COMMENT '是否为叶子节点(0 否 1是)',
  `source_type` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '来源类型( report 积木报表 screen 大屏  drag 仪表盘)',
  `del_flag` int(1) NULL DEFAULT NULL COMMENT '删除标识(0 正常 1 已删除)',
  `create_by` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` timestamp NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '更新人',
  `update_time` timestamp NULL DEFAULT NULL COMMENT '更新时间',
  `tenant_id` varchar(11) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '租户id',
  `sort_no` int(11) NULL DEFAULT NULL COMMENT '排序',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '分类' ROW_FORMAT = DYNAMIC;

CREATE TABLE IF NOT EXISTS `jimu_report_data_source`  (
  `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '数据源名称',
  `report_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '报表_id',
  `code` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '编码',
  `remark` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '备注',
  `db_type` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '数据库类型',
  `db_driver` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '驱动类',
  `db_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '数据源地址',
  `db_username` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '用户名',
  `db_password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '密码',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建日期',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新日期',
  `connect_times` int(11) NULL DEFAULT 0 COMMENT '连接失败次数',
  `tenant_id` varchar(10) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '多租户标识',
  `type` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '类型(report:报表;drag:仪表盘)',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_jmdatasource_report_id`(`report_id`) USING BTREE,
  INDEX `idx_jmdatasource_code`(`code`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

CREATE TABLE IF NOT EXISTS `jimu_report_db`  (
  `id` varchar(36) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT 'id',
  `jimu_report_id` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '主键字段',
  `create_by` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '创建人登录名称',
  `update_by` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '更新人登录名称',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建日期',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新日期',
  `db_code` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '数据集编码',
  `db_ch_name` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '数据集名字',
  `db_type` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '数据源类型',
  `db_table_name` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '数据库表名',
  `db_dyn_sql` longtext CHARACTER SET utf8 COLLATE utf8_general_ci NULL COMMENT '动态查询SQL',
  `db_key` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '数据源KEY',
  `tb_db_key` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '填报数据源',
  `tb_db_table_name` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '填报数据表',
  `java_type` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT 'java类数据集  类型（spring:springkey,class:java类名）',
  `java_value` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT 'java类数据源  数值（bean key/java类名）',
  `api_url` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '请求地址',
  `api_method` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '请求方法0-get,1-post',
  `is_list` varchar(10) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT '0' COMMENT '是否是列表0否1是 默认0',
  `is_page` varchar(10) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '是否作为分页,0:不分页，1:分页',
  `db_source` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '数据源',
  `db_source_type` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '数据库类型 MYSQL ORACLE SQLSERVER',
  `json_data` text CHARACTER SET utf8 COLLATE utf8_general_ci NULL COMMENT 'json数据，直接解析json内容',
  `api_convert` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT 'api转换器',
  `iz_shared_source` int(1) NULL DEFAULT NULL COMMENT '是否为共享数据源(0 否 1 是)',
  `jimu_shared_source_id` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '指向共享数据集的id',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_jmreportdb_db_key`(`db_key`) USING BTREE,
  INDEX `idx_jimu_report_id`(`jimu_report_id`) USING BTREE,
  INDEX `idx_db_source_id`(`db_source`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = DYNAMIC;

CREATE TABLE IF NOT EXISTS `jimu_report_db_field`  (
  `id` varchar(36) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT 'id',
  `create_by` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '创建人登录名称',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建日期',
  `update_by` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '更新人登录名称',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新日期',
  `jimu_report_db_id` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '数据源ID',
  `field_name` varchar(80) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '字段名',
  `field_name_physics` varchar(200) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '物理字段名（文件数据集使用，存的是excel的字段标题）',
  `field_text` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '字段文本',
  `widget_type` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '控件类型',
  `widget_width` int(10) NULL DEFAULT NULL COMMENT '控件宽度',
  `order_num` int(3) NULL DEFAULT NULL COMMENT '排序',
  `search_flag` int(3) NULL DEFAULT 0 COMMENT '查询标识0否1是 默认0',
  `search_mode` int(3) NULL DEFAULT NULL COMMENT '查询模式1简单2范围',
  `dict_code` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '字典编码支持从表中取数据',
  `search_value` varchar(100) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '查询默认值',
  `search_format` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '查询时间格式化表达式',
  `ext_json` text CHARACTER SET utf8 COLLATE utf8_general_ci NULL COMMENT '参数配置',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_jrdf_jimu_report_db_id`(`jimu_report_db_id`) USING BTREE,
  INDEX `idx_dbfield_order_num`(`order_num`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = DYNAMIC;

CREATE TABLE IF NOT EXISTS `jimu_report_db_param`  (
  `id` varchar(36) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `jimu_report_head_id` varchar(36) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '动态报表ID',
  `param_name` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '参数字段',
  `param_txt` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '参数文本',
  `param_value` varchar(1000) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '参数默认值',
  `order_num` int(11) NULL DEFAULT NULL COMMENT '排序',
  `create_by` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '创建人登录名称',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建日期',
  `update_by` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '更新人登录名称',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新日期',
  `search_flag` int(1) NULL DEFAULT NULL COMMENT '查询标识0否1是 默认0',
  `widget_type` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '查询控件类型',
  `search_mode` int(1) NULL DEFAULT NULL COMMENT '查询模式1简单2范围',
  `dict_code` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '字典',
  `search_format` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '查询时间格式化表达式',
  `ext_json` text CHARACTER SET utf8 COLLATE utf8_general_ci NULL COMMENT '参数配置',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_jrdp_jimu_report_head_id`(`jimu_report_head_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci ROW_FORMAT = DYNAMIC;

CREATE TABLE IF NOT EXISTS `jimu_report_export_job`  (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '主键',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '任务名称',
  `begin_time` datetime NULL DEFAULT NULL COMMENT '开始时间',
  `end_time` datetime NULL DEFAULT NULL COMMENT '结束时间',
  `exec_interval` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '执行频率',
  `report_conf` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '导出报表配置',
  `last_run_time` datetime NULL DEFAULT NULL COMMENT '最后执行时间',
  `receiver_email` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '接收通知的邮件',
  `file_sync_path` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '文件同步路径',
  `status` int(11) NULL DEFAULT NULL COMMENT '状态(0:停止;1:启动)',
  `create_by` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(50) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '修改人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '修改时间',
  `tenant_id` varchar(10) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '多租户标识',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '积木报表导出计划表' ROW_FORMAT = DYNAMIC;

CREATE TABLE IF NOT EXISTS `jimu_report_export_log`  (
  `id` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `batch_no` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '批次编号',
  `export_channel` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '导出渠道',
  `export_type` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '导出类型',
  `report_id` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '报表id',
  `download_path` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '下载路径',
  `export_from` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '导出来源',
  `from_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '来源单据id',
  `err_msg` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '异常信息',
  `from_job_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '来源任务名',
  `status` varchar(15) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '状态',
  `create_by` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `tenant_id` varchar(10) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '多租户标识',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '积木报表自动导出记录表' ROW_FORMAT = DYNAMIC;

CREATE TABLE IF NOT EXISTS `jimu_report_ext_data`  (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键ID',
  `biz_type` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '业务类型标识，如 report_share、temp_config 等',
  `name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '名称，展示用',
  `descr` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '描述信息',
  `tags` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '标签，多个用逗号分隔',
  `data_value` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '实际存储内容',
  `metadata` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '元数据，用于存储补充信息',
  `status` tinyint(4) NULL DEFAULT 1 COMMENT '状态标识：1正常 0禁用',
  `create_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '修改人',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_biz`(`biz_type`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '通用扩展数据表' ROW_FORMAT = DYNAMIC;

CREATE TABLE IF NOT EXISTS `jimu_report_icon_lib`  (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键',
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '图片名称',
  `type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '图片类型',
  `image_url` varchar(255) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '图片地址',
  `create_by` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '更新人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `tenant_id` int(11) NULL DEFAULT NULL COMMENT '租户id',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '积木图库表' ROW_FORMAT = DYNAMIC;

CREATE TABLE IF NOT EXISTS `jimu_report_link`  (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键id',
  `report_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '积木设计器id',
  `parameter` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '参数',
  `eject_type` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '弹出方式（0 当前页面 1 新窗口）',
  `link_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '链接名称',
  `api_method` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '请求方法0-get,1-post',
  `link_type` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '链接方式(0 网络报表 1 网络连接 2 图表联动)',
  `api_url` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '外网api',
  `link_chart_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '联动图表的ID',
  `expression` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '表达式',
  `requirement` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '条件',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `uniq_link_reportid`(`report_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '超链接配置表' ROW_FORMAT = DYNAMIC;

CREATE TABLE IF NOT EXISTS `jimu_report_map`  (
  `id` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL COMMENT '主键',
  `label` varchar(125) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '地图名称',
  `name` varchar(125) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '地图编码',
  `data` longtext CHARACTER SET utf8 COLLATE utf8_general_ci NULL COMMENT '地图数据',
  `create_by` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_by` varchar(32) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '修改人',
  `update_time` datetime NULL DEFAULT NULL COMMENT '修改时间',
  `del_flag` varchar(1) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '0表示未删除,1表示删除',
  `sys_org_code` varchar(64) CHARACTER SET utf8 COLLATE utf8_general_ci NULL DEFAULT NULL COMMENT '所属部门',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uniq_jmreport_map_name`(`name`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8 COLLATE = utf8_general_ci COMMENT = '地图配置表' ROW_FORMAT = DYNAMIC;

CREATE TABLE IF NOT EXISTS `jimu_report_share`  (
  `id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键',
  `report_id` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '在线excel设计器id',
  `preview_url` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '预览地址',
  `preview_lock` varchar(4) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '密码锁',
  `last_update_time` datetime NULL DEFAULT NULL COMMENT '最后更新时间',
  `term_of_validity` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '有效期(0:永久有效，1:1天，2:7天)',
  `status` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '是否过期(0未过期，1已过期)',
  `preview_lock_status` varchar(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '是否为密码锁(0 否,1是)',
  `share_token` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '分享token',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uniq_report_id`(`report_id`) USING BTREE,
  UNIQUE INDEX `uniq_jrs_report_id`(`report_id`) USING BTREE COMMENT '报表id唯一索引',
  INDEX `idx_jrs_share_token`(`share_token`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '积木报表预览权限表' ROW_FORMAT = DYNAMIC;

CREATE TABLE IF NOT EXISTS `jimu_report_sheet`  (
  `id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键（Sheet ID）',
  `report_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '报表ID',
  `sheet_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'Sheet名称',
  `sheet_order` int(11) NOT NULL COMMENT '排序（可以为负数，负数表示在默认sheet前面）',
  `json_str` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '该sheet的完整jsonStr',
  `create_time` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建人',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '更新人',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_report_id`(`report_id`) USING BTREE,
  INDEX `idx_sheet_order`(`report_id`, `sheet_order`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '报表Sheet表' ROW_FORMAT = DYNAMIC;


-- ========= 种子数据(字典/字典项/报表分类), IF NOT EXISTS 语义用 INSERT IGNORE =========

INSERT IGNORE INTO `jimu_dict` VALUES ('0b5d19e1fce4b2e6647e6b4a17760c14', '通告类型', 'msg_category', '消息类型1:通知公告2:系统消息', 0, 'admin', '2019-04-22 18:01:35', NULL, NULL, 0, NULL);

INSERT IGNORE INTO `jimu_dict` VALUES ('1047797573274468352', '系统图库', 'gallery', '', 0, 'admin', '2025-02-07 19:00:19', NULL, NULL, 0, '1');

INSERT IGNORE INTO `jimu_dict` VALUES ('1174509082208395266', '职务职级', 'position_rank', '职务表职级字典', 0, 'admin', '2019-09-19 10:22:41', NULL, NULL, 0, NULL);

INSERT IGNORE INTO `jimu_dict` VALUES ('1174511106530525185', '机构类型', 'org_category', '机构类型 1公司，2部门 3岗位', 0, 'admin', '2019-09-19 10:30:43', NULL, NULL, 0, NULL);

INSERT IGNORE INTO `jimu_dict` VALUES ('1178295274528845826', '表单权限策略', 'form_perms_type', '', 0, 'admin', '2019-09-29 21:07:39', 'admin', '2019-09-29 21:08:26', NULL, NULL);

INSERT IGNORE INTO `jimu_dict` VALUES ('1199517671259906049', '紧急程度', 'urgent_level', '日程计划紧急程度', 0, 'admin', '2019-11-27 10:37:53', NULL, NULL, 0, NULL);

INSERT IGNORE INTO `jimu_dict` VALUES ('1199518099888414722', '日程计划类型', 'eoa_plan_type', '', 0, 'admin', '2019-11-27 10:39:36', NULL, NULL, 0, NULL);

INSERT IGNORE INTO `jimu_dict` VALUES ('1199520177767587841', '分类栏目类型', 'eoa_cms_menu_type', '', 0, 'admin', '2019-11-27 10:47:51', 'admin', '2019-11-27 10:49:35', 0, NULL);

INSERT IGNORE INTO `jimu_dict` VALUES ('1199525215290306561', '日程计划状态', 'eoa_plan_status', '', 0, 'admin', '2019-11-27 11:07:52', 'admin', '2019-11-27 11:10:11', 0, NULL);

INSERT IGNORE INTO `jimu_dict` VALUES ('1209733563293962241', '数据库类型', 'database_type', '', 0, 'admin', '2019-12-25 15:12:12', NULL, NULL, 0, NULL);

INSERT IGNORE INTO `jimu_dict` VALUES ('1232913193820581889', 'Online表单业务分类', 'ol_form_biz_type', '', 0, 'admin', '2020-02-27 14:19:46', 'admin', '2020-02-27 14:20:23', 0, NULL);

INSERT IGNORE INTO `jimu_dict` VALUES ('1250687930947620866', '定时任务状态', 'quartz_status', '', 0, 'admin', '2020-04-16 15:30:14', '', NULL, NULL, NULL);

INSERT IGNORE INTO `jimu_dict` VALUES ('1280401766745718786', '租户状态', 'tenant_status', '租户状态', 0, 'admin', '2020-07-07 15:22:25', NULL, NULL, 0, NULL);

INSERT IGNORE INTO `jimu_dict` VALUES ('1356445645198135298', '开关', 'is_open', '', 0, 'admin', '2021-02-02 11:33:38', 'admin', '2021-02-02 15:28:12', 0, NULL);

INSERT IGNORE INTO `jimu_dict` VALUES ('236e8a4baff0db8c62c00dd95632834f', '同步工作流引擎', 'activiti_sync', '同步工作流引擎', 0, 'admin', '2019-05-15 15:27:33', NULL, NULL, 0, NULL);

INSERT IGNORE INTO `jimu_dict` VALUES ('2e02df51611a4b9632828ab7e5338f00', '权限策略', 'perms_type', '权限策略', 0, 'admin', '2019-04-26 18:26:55', NULL, NULL, 0, NULL);

INSERT IGNORE INTO `jimu_dict` VALUES ('2f0320997ade5dd147c90130f7218c3e', '推送类别', 'msg_type', '', 0, 'admin', '2019-03-17 21:21:32', 'admin', '2019-03-26 19:57:45', 0, NULL);

INSERT IGNORE INTO `jimu_dict` VALUES ('3486f32803bb953e7155dab3513dc68b', '删除状态', 'del_flag', NULL, 0, 'admin', '2019-01-18 21:46:26', 'admin', '2019-03-30 11:17:11', 0, NULL);

INSERT IGNORE INTO `jimu_dict` VALUES ('3d9a351be3436fbefb1307d4cfb49bf2', '性别', 'sex', NULL, 0, NULL, '2019-01-04 14:56:32', 'admin', '2019-03-30 11:28:27', 1, NULL);

INSERT IGNORE INTO `jimu_dict` VALUES ('4274efc2292239b6f000b153f50823ff', '全局权限策略', 'global_perms_type', '全局权限策略', 0, 'admin', '2019-05-10 17:54:05', NULL, NULL, 0, NULL);

INSERT IGNORE INTO `jimu_dict` VALUES ('4c03fca6bf1f0299c381213961566349', 'Online图表展示模板', 'online_graph_display_template', 'Online图表展示模板', 0, 'admin', '2019-04-12 17:28:50', NULL, NULL, 0, NULL);

INSERT IGNORE INTO `jimu_dict` VALUES ('4c753b5293304e7a445fd2741b46529d', '字典状态', 'dict_item_status', NULL, 0, 'admin', '2020-06-18 23:18:42', 'admin', '2019-03-30 19:33:52', 1, NULL);

INSERT IGNORE INTO `jimu_dict` VALUES ('4d7fec1a7799a436d26d02325eff295e', '优先级', 'priority', '优先级', 0, 'admin', '2019-03-16 17:03:34', 'admin', '2019-04-16 17:39:23', 0, NULL);

INSERT IGNORE INTO `jimu_dict` VALUES ('4e4602b3e3686f0911384e188dc7efb4', '条件规则', 'rule_conditions', '', 0, 'admin', '2019-04-01 10:15:03', 'admin', '2019-04-01 10:30:47', 0, NULL);

INSERT IGNORE INTO `jimu_dict` VALUES ('4f69be5f507accea8d5df5f11346181a', '发送消息类型', 'msgType', NULL, 0, 'admin', '2019-04-11 14:27:09', NULL, NULL, 0, NULL);

INSERT IGNORE INTO `jimu_dict` VALUES ('68168534ff5065a152bfab275c2136f8', '有效无效状态', 'valid_status', '有效无效状态', 0, 'admin', '2020-09-26 19:21:14', 'admin', '2019-04-26 19:21:23', 0, NULL);

INSERT IGNORE INTO `jimu_dict` VALUES ('6b78e3f59faec1a4750acff08030a79b', '用户类型', 'user_type', NULL, 0, NULL, '2019-01-04 14:59:01', 'admin', '2019-03-18 23:28:18', 0, NULL);

INSERT IGNORE INTO `jimu_dict` VALUES ('72cce0989df68887546746d8f09811aa', 'Online表单类型', 'cgform_table_type', '', 0, 'admin', '2019-01-27 10:13:02', 'admin', '2019-03-30 11:37:36', 0, NULL);

INSERT IGNORE INTO `jimu_dict` VALUES ('78bda155fe380b1b3f175f1e88c284c6', '流程状态', 'bpm_status', '流程状态', 0, 'admin', '2019-05-09 16:31:52', NULL, NULL, 0, NULL);

INSERT IGNORE INTO `jimu_dict` VALUES ('83bfb33147013cc81640d5fd9eda030c', '日志类型', 'log_type', NULL, 0, 'admin', '2019-03-18 23:22:19', NULL, NULL, 1, NULL);

INSERT IGNORE INTO `jimu_dict` VALUES ('845da5006c97754728bf48b6a10f79cc', '状态', 'status', NULL, 0, 'admin', '2019-03-18 21:45:25', 'admin', '2019-03-18 21:58:25', 0, NULL);

INSERT IGNORE INTO `jimu_dict` VALUES ('880a895c98afeca9d9ac39f29e67c13e', '操作类型', 'operate_type', '操作类型', 0, 'admin', '2019-07-22 10:54:29', NULL, NULL, 0, NULL);

INSERT IGNORE INTO `jimu_dict` VALUES ('8dfe32e2d29ea9430a988b3b558bf233', '发布状态', 'send_status', '发布状态', 0, 'admin', '2019-04-16 17:40:42', NULL, NULL, 0, NULL);

INSERT IGNORE INTO `jimu_dict` VALUES ('986779503584169984', '民族', 'minzu', '', 0, 'admin', '2024-08-23 09:56:17', 'admin', '2025-01-09 10:42:06', 0, '1001');

INSERT IGNORE INTO `jimu_dict` VALUES ('986783181955223552', '学历', 'xueli_sf', '', 0, 'admin', '2024-08-23 10:10:54', NULL, NULL, NULL, NULL);

INSERT IGNORE INTO `jimu_dict` VALUES ('986784113082322944', '爱好', 'aihao', '', 0, 'admin', '2024-08-23 10:14:36', NULL, NULL, NULL, NULL);

INSERT IGNORE INTO `jimu_dict` VALUES ('a7adbcd86c37f7dbc9b66945c82ef9e6', '1是0否', 'yn', '', 0, 'admin', '2019-05-22 19:29:29', NULL, NULL, 0, NULL);

INSERT IGNORE INTO `jimu_dict` VALUES ('a9d9942bd0eccb6e89de92d130ec4c4a', '消息发送状态', 'msgSendStatus', NULL, 0, 'admin', '2019-04-12 18:18:17', NULL, NULL, 0, NULL);

INSERT IGNORE INTO `jimu_dict` VALUES ('ac2f7c0c5c5775fcea7e2387bcb22f01', '菜单类型', 'menu_type', NULL, 0, 'admin', '2020-12-18 23:24:32', 'admin', '2019-04-01 15:27:06', 1, NULL);

INSERT IGNORE INTO `jimu_dict` VALUES ('ad7c65ba97c20a6805d5dcdf13cdaf36', 'onlineT类型', 'ceshi_online', NULL, 0, 'admin', '2019-03-22 16:31:49', 'admin', '2019-03-22 16:34:16', 0, NULL);

INSERT IGNORE INTO `jimu_dict` VALUES ('bd1b8bc28e65d6feefefb6f3c79f42fd', 'Online图表数据类型', 'online_graph_data_type', 'Online图表数据类型', 0, 'admin', '2019-04-12 17:24:24', 'admin', '2019-04-12 17:24:57', 0, NULL);

INSERT IGNORE INTO `jimu_dict` VALUES ('c2b_sens_abuse_dict000001', '辱骂脏话', 'c2b_sens_abuse', 'Chat2BI 敏感词-辱骂脏话', 0, 'admin', '2026-05-25 12:00:00', NULL, NULL, 0, NULL);

INSERT IGNORE INTO `jimu_dict` VALUES ('c2b_sens_politics_dict0001', '政治敏感', 'c2b_sens_politics', 'Chat2BI 敏感词-政治敏感', 0, 'admin', '2026-05-25 12:00:00', NULL, NULL, 0, NULL);

INSERT IGNORE INTO `jimu_dict` VALUES ('c36169beb12de8a71c8683ee7c28a503', '部门状态', 'depart_status', NULL, 0, 'admin', '2019-03-18 21:59:51', NULL, NULL, 0, NULL);

INSERT IGNORE INTO `jimu_dict` VALUES ('c5a14c75172783d72cbee6ee7f5df5d1', 'Online图表类型', 'online_graph_type', 'Online图表类型', 0, 'admin', '2019-04-12 17:04:06', NULL, NULL, 0, NULL);

INSERT IGNORE INTO `jimu_dict` VALUES ('d6e1152968b02d69ff358c75b48a6ee1', '流程类型', 'bpm_process_type', NULL, 0, 'admin', '2021-02-22 19:26:54', 'admin', '2025-01-09 10:35:04', 0, '1001');

INSERT IGNORE INTO `jimu_dict` VALUES ('fc6cd58fde2e8481db10d3a1e68ce70c', '用户状态', 'user_status', NULL, 0, 'admin', '2019-03-18 21:57:25', 'admin', '2019-03-18 23:11:58', 1, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('0072d115e07c875d76c9b022e2179128', '4d7fec1a7799a436d26d02325eff295e', '低', 'L', '低', 3, 1, 'admin', '2019-04-16 17:04:59', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('05a2e732ce7b00aa52141ecc3e330b4e', '3486f32803bb953e7155dab3513dc68b', '已删除', '1', NULL, NULL, 1, 'admin', '2025-10-18 21:46:56', 'admin', '2019-03-28 22:23:20');

INSERT IGNORE INTO `jimu_dict_item` VALUES ('096c2e758d823def3855f6376bc736fb', 'bd1b8bc28e65d6feefefb6f3c79f42fd', 'SQL', 'sql', NULL, 1, 1, 'admin', '2019-04-12 17:26:26', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('0c9532916f5cd722017b46bc4d953e41', '2f0320997ade5dd147c90130f7218c3e', '指定用户', 'USER', NULL, NULL, 1, 'admin', '2019-03-17 21:22:19', 'admin', '2019-03-17 21:22:28');

INSERT IGNORE INTO `jimu_dict_item` VALUES ('0ca4beba9efc4f9dd54af0911a946d5c', '72cce0989df68887546746d8f09811aa', '附表', '3', NULL, 3, 1, 'admin', '2019-03-27 10:13:43', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('1030a2652608f5eac3b49d70458b8532', '2e02df51611a4b9632828ab7e5338f00', '禁用', '2', '禁用', 2, 1, 'admin', '2021-03-26 18:27:28', 'admin', '2019-04-26 18:39:11');

INSERT IGNORE INTO `jimu_dict_item` VALUES ('1047797624512086016', '1047797573274468352', '常规', 'common', NULL, 1, 1, 'admin', '2025-02-07 19:00:31', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('1047797669877678080', '1047797573274468352', '指向', 'point', NULL, 1, 1, 'admin', '2025-02-07 19:00:42', '15931993294', '2025-02-07 19:01:11');

INSERT IGNORE INTO `jimu_dict_item` VALUES ('1047797751893098410', '1047797573274468352', '数据指标', 'data', NULL, 1, 1, 'admin', '2025-03-12 19:01:01', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('1047797751893098411', '1047797573274468352', '数据安全', 'datasafe', NULL, 1, 1, 'admin', '2025-03-12 19:01:01', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('1057910209549750272', '1047797573274468352', '3D', '3D', NULL, 1, 1, 'admin', '2025-03-07 16:44:19', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('1174509082208395266', '1174511106530525185', '岗位', '3', '岗位', 1, 1, 'admin', '2019-09-19 10:31:16', '', NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('1174509601047994369', '1174509082208395266', '员级', '1', '', 1, 1, 'admin', '2019-09-19 10:24:45', 'admin', '2019-09-23 11:46:39');

INSERT IGNORE INTO `jimu_dict_item` VALUES ('1174509667297026049', '1174509082208395266', '助级', '2', '', 2, 1, 'admin', '2019-09-19 10:25:01', 'admin', '2019-09-23 11:46:47');

INSERT IGNORE INTO `jimu_dict_item` VALUES ('1174509713568587777', '1174509082208395266', '中级', '3', '', 3, 1, 'admin', '2019-09-19 10:25:12', 'admin', '2019-09-23 11:46:56');

INSERT IGNORE INTO `jimu_dict_item` VALUES ('1174509788361416705', '1174509082208395266', '副高级', '4', '', 4, 1, 'admin', '2019-09-19 10:25:30', 'admin', '2019-09-23 11:47:06');

INSERT IGNORE INTO `jimu_dict_item` VALUES ('1174509835803189250', '1174509082208395266', '正高级', '5', '', 5, 1, 'admin', '2019-09-19 10:25:41', 'admin', '2019-09-23 11:47:12');

INSERT IGNORE INTO `jimu_dict_item` VALUES ('1174511197735665665', '1174511106530525185', '公司', '1', '公司', 1, 1, 'admin', '2019-09-19 10:31:05', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('1174511244036587521', '1174511106530525185', '部门', '2', '部门', 1, 1, 'admin', '2019-09-19 10:31:16', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('1178295553450061826', '1178295274528845826', '可编辑(未授权禁用)', '2', '', 2, 1, 'admin', '2019-09-29 21:08:46', 'admin', '2019-09-29 21:09:18');

INSERT IGNORE INTO `jimu_dict_item` VALUES ('1178295639554928641', '1178295274528845826', '可见(未授权不可见)', '1', '', 1, 1, 'admin', '2019-09-29 21:09:06', 'admin', '2019-09-29 21:09:24');

INSERT IGNORE INTO `jimu_dict_item` VALUES ('1199517884758368257', '1199517671259906049', '一般', '1', '', 1, 1, 'admin', '2019-11-27 10:38:44', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('1199517914017832962', '1199517671259906049', '重要', '2', '', 1, 1, 'admin', '2019-11-27 10:38:51', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('1199517941339529217', '1199517671259906049', '紧急', '3', '', 1, 1, 'admin', '2019-11-27 10:38:58', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('1199518186144276482', '1199518099888414722', '日常记录', '1', '', 1, 1, 'admin', '2019-11-27 10:39:56', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('1199518214858481666', '1199518099888414722', '本周工作', '2', '', 1, 1, 'admin', '2019-11-27 10:40:03', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('1199518235943247874', '1199518099888414722', '下周计划', '3', '', 1, 1, 'admin', '2019-11-27 10:40:08', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('1199520817285701634', '1199520177767587841', '列表', '1', '', 1, 1, 'admin', '2019-11-27 10:50:24', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('1199520835035996161', '1199520177767587841', '链接', '2', '', 1, 1, 'admin', '2019-11-27 10:50:28', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('1199525468672405505', '1199525215290306561', '未开始', '0', '', 1, 1, 'admin', '2019-11-27 11:08:52', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('1199525490575060993', '1199525215290306561', '进行中', '1', '', 1, 1, 'admin', '2019-11-27 11:08:58', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('1199525506429530114', '1199525215290306561', '已完成', '2', '', 1, 1, 'admin', '2019-11-27 11:09:02', 'admin', '2019-11-27 11:10:02');

INSERT IGNORE INTO `jimu_dict_item` VALUES ('1199607547704647681', '4f69be5f507accea8d5df5f11346181a', '系统', '4', '', 1, 1, 'admin', '2019-11-27 16:35:02', 'admin', '2019-11-27 19:37:46');

INSERT IGNORE INTO `jimu_dict_item` VALUES ('1209733775114702850', '1209733563293962241', 'MySQL5.5', '1', '', 1, 1, 'admin', '2019-12-25 15:13:02', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('1209733839933476865', '1209733563293962241', 'Oracle', '2', '', 3, 1, 'admin', '2019-12-25 15:13:18', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('1209733903020003330', '1209733563293962241', 'SQLServer', '3', '', 4, 1, 'admin', '2019-12-25 15:13:33', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('1232913424813486081', '1232913193820581889', '官方示例', 'demo', '', 1, 1, 'admin', '2020-02-27 14:20:42', 'admin', '2020-02-27 14:21:37');

INSERT IGNORE INTO `jimu_dict_item` VALUES ('1232913493717512194', '1232913193820581889', '流程表单', 'bpm', '', 2, 1, 'admin', '2020-02-27 14:20:58', 'admin', '2020-02-27 14:22:20');

INSERT IGNORE INTO `jimu_dict_item` VALUES ('1232913605382467585', '1232913193820581889', '测试表单', 'temp', '', 4, 1, 'admin', '2020-02-27 14:21:25', 'admin', '2020-02-27 14:22:16');

INSERT IGNORE INTO `jimu_dict_item` VALUES ('1232914232372195330', '1232913193820581889', '导入表单', 'bdfl_include', '', 5, 1, 'admin', '2020-02-27 14:23:54', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('1234371726545010689', '4e4602b3e3686f0911384e188dc7efb4', '左模糊', 'LEFT_LIKE', '左模糊', 7, 1, 'admin', '2020-03-02 14:55:27', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('1234371809495760898', '4e4602b3e3686f0911384e188dc7efb4', '右模糊', 'RIGHT_LIKE', '右模糊', 7, 1, 'admin', '2020-03-02 14:55:47', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('1250688147579228161', '1250687930947620866', '正常', '0', '', 1, 1, 'admin', '2020-04-16 15:31:05', '', NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('1250688201064992770', '1250687930947620866', '停止', '-1', '', 1, 1, 'admin', '2020-04-16 15:31:18', '', NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('1280401815068295170', '1280401766745718786', '正常', '1', '', 1, 1, 'admin', '2020-07-07 15:22:36', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('1280401847607705602', '1280401766745718786', '冻结', '0', '', 1, 1, 'admin', '2020-07-07 15:22:44', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('1305827309355302914', 'bd1b8bc28e65d6feefefb6f3c79f42fd', 'API', 'api', '', 3, 1, 'admin', '2020-09-15 19:14:26', 'admin', '2020-09-15 19:14:41');

INSERT IGNORE INTO `jimu_dict_item` VALUES ('1334440962954936321', '1209733563293962241', 'MYSQL5.7', '4', NULL, 1, 1, 'admin', '2020-12-03 18:16:02', 'admin', '2020-12-03 18:16:02');

INSERT IGNORE INTO `jimu_dict_item` VALUES ('1356445705549975553', '1356445645198135298', '是', 'Y', '', 1, 1, 'admin', '2021-02-02 11:33:52', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('1356445754212290561', '1356445645198135298', '否', 'N', '', 1, 1, 'admin', '2021-02-02 11:34:04', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('147c48ff4b51545032a9119d13f3222a', 'd6e1152968b02d69ff358c75b48a6ee1', '测试流程', 'test', NULL, 1, 1, 'admin', '2019-03-22 19:27:05', 'admin', '2025-01-09 10:35:08');

INSERT IGNORE INTO `jimu_dict_item` VALUES ('1543fe7e5e26fb97cdafe4981bedc0c8', '4c03fca6bf1f0299c381213961566349', '单排布局', 'single', NULL, 2, 1, 'admin', '2022-07-12 17:43:39', 'admin', '2019-04-12 17:43:57');

INSERT IGNORE INTO `jimu_dict_item` VALUES ('1ce390c52453891f93514c1bd2795d44', 'ad7c65ba97c20a6805d5dcdf13cdaf36', '000', '00', NULL, 1, 1, 'admin', '2019-03-22 16:34:34', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('1db531bcff19649fa82a644c8a939dc4', '4c03fca6bf1f0299c381213961566349', '组合布局', 'combination', '', 4, 1, 'admin', '2019-05-11 16:07:08', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('222705e11ef0264d4214affff1fb4ff9', '4f69be5f507accea8d5df5f11346181a', '短信', '1', '', 1, 1, 'admin', '2023-02-28 10:50:36', 'admin', '2019-04-28 10:58:11');

INSERT IGNORE INTO `jimu_dict_item` VALUES ('23a5bb76004ed0e39414e928c4cde155', '4e4602b3e3686f0911384e188dc7efb4', '不等于', '!=', '不等于', 3, 1, 'admin', '2019-04-01 16:46:15', 'admin', '2019-04-01 17:48:40');

INSERT IGNORE INTO `jimu_dict_item` VALUES ('25847e9cb661a7c711f9998452dc09e6', '4e4602b3e3686f0911384e188dc7efb4', '小于等于', '<=', '小于等于', 6, 1, 'admin', '2019-04-01 16:44:34', 'admin', '2019-04-01 17:49:10');

INSERT IGNORE INTO `jimu_dict_item` VALUES ('2d51376643f220afdeb6d216a8ac2c01', '68168534ff5065a152bfab275c2136f8', '有效', '1', '有效', 2, 1, 'admin', '2019-04-26 19:22:01', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('308c8aadf0c37ecdde188b97ca9833f5', '8dfe32e2d29ea9430a988b3b558bf233', '已发布', '1', '已发布', 2, 1, 'admin', '2019-04-16 17:41:24', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('333e6b2196e01ef9a5f76d74e86a6e33', '8dfe32e2d29ea9430a988b3b558bf233', '未发布', '0', '未发布', 1, 1, 'admin', '2019-04-16 17:41:12', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('337ea1e401bda7233f6258c284ce4f50', 'bd1b8bc28e65d6feefefb6f3c79f42fd', 'JSON', 'json', NULL, 1, 1, 'admin', '2019-04-12 17:26:33', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('33bc9d9f753cf7dc40e70461e50fdc54', 'a9d9942bd0eccb6e89de92d130ec4c4a', '发送失败', '2', NULL, 3, 1, 'admin', '2019-04-12 18:20:02', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('3fbc03d6c994ae06d083751248037c0e', '78bda155fe380b1b3f175f1e88c284c6', '已完成', '3', '已完成', 3, 1, 'admin', '2019-05-09 16:33:25', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('41d7aaa40c9b61756ffb1f28da5ead8e', '0b5d19e1fce4b2e6647e6b4a17760c14', '通知公告', '1', NULL, 1, 1, 'admin', '2019-04-22 18:01:57', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('41fa1e9571505d643aea87aeb83d4d76', '4e4602b3e3686f0911384e188dc7efb4', '等于', '=', '等于', 4, 1, 'admin', '2019-04-01 16:45:24', 'admin', '2019-04-01 17:49:00');

INSERT IGNORE INTO `jimu_dict_item` VALUES ('43d2295b8610adce9510ff196a49c6e9', '845da5006c97754728bf48b6a10f79cc', '正常', '1', NULL, NULL, 1, 'admin', '2019-03-18 21:45:51', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('4f05fb5376f4c61502c5105f52e4dd2b', '83bfb33147013cc81640d5fd9eda030c', '操作日志', '2', NULL, NULL, 1, 'admin', '2019-03-18 23:22:49', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('50223341bfb5ba30bf6319789d8d17fe', 'd6e1152968b02d69ff358c75b48a6ee1', '业务办理', 'business', NULL, 3, 1, 'admin', '2023-04-22 19:28:05', 'admin', '2019-03-22 23:24:39');

INSERT IGNORE INTO `jimu_dict_item` VALUES ('51222413e5906cdaf160bb5c86fb827c', 'a7adbcd86c37f7dbc9b66945c82ef9e6', '是', '1', '', 1, 1, 'admin', '2019-05-22 19:29:45', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('538fca35afe004972c5f3947c039e766', '2e02df51611a4b9632828ab7e5338f00', '显示', '1', '显示', 1, 1, 'admin', '2025-03-26 18:27:13', 'admin', '2019-04-26 18:39:07');

INSERT IGNORE INTO `jimu_dict_item` VALUES ('5584c21993bde231bbde2b966f2633ac', '4e4602b3e3686f0911384e188dc7efb4', '自定义SQL表达式', 'USE_SQL_RULES', '自定义SQL表达式', 9, 1, 'admin', '2019-04-01 10:45:24', 'admin', '2019-04-01 17:49:27');

INSERT IGNORE INTO `jimu_dict_item` VALUES ('58b73b344305c99b9d8db0fc056bbc0a', '72cce0989df68887546746d8f09811aa', '主表', '2', NULL, 2, 1, 'admin', '2019-03-27 10:13:36', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('5b65a88f076b32e8e69d19bbaadb52d5', '2f0320997ade5dd147c90130f7218c3e', '全体用户', 'ALL', NULL, NULL, 1, 'admin', '2020-10-17 21:22:43', 'admin', '2019-03-28 22:17:09');

INSERT IGNORE INTO `jimu_dict_item` VALUES ('5d833f69296f691843ccdd0c91212b6b', '880a895c98afeca9d9ac39f29e67c13e', '修改', '3', '', 3, 1, 'admin', '2019-07-22 10:55:07', 'admin', '2019-07-22 10:55:41');

INSERT IGNORE INTO `jimu_dict_item` VALUES ('5d84a8634c8fdfe96275385075b105c9', '3d9a351be3436fbefb1307d4cfb49bf2', '女', '2', NULL, 2, 1, NULL, '2019-01-04 14:56:56', NULL, '2019-01-04 17:38:12');

INSERT IGNORE INTO `jimu_dict_item` VALUES ('66c952ae2c3701a993e7db58f3baf55e', '4e4602b3e3686f0911384e188dc7efb4', '大于', '>', '大于', 1, 1, 'admin', '2019-04-01 10:45:46', 'admin', '2019-04-01 17:48:29');

INSERT IGNORE INTO `jimu_dict_item` VALUES ('6937c5dde8f92e9a00d4e2ded9198694', 'ad7c65ba97c20a6805d5dcdf13cdaf36', 'easyui', '3', NULL, 1, 1, 'admin', '2019-03-22 16:32:15', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('69cacf64e244100289ddd4aa9fa3b915', 'a9d9942bd0eccb6e89de92d130ec4c4a', '未发送', '0', NULL, 1, 1, 'admin', '2019-04-12 18:19:23', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('6a7a9e1403a7943aba69e54ebeff9762', '4f69be5f507accea8d5df5f11346181a', '邮件', '2', '', 2, 1, 'admin', '2031-02-28 10:50:44', 'admin', '2019-04-28 10:59:03');

INSERT IGNORE INTO `jimu_dict_item` VALUES ('6c682d78ddf1715baf79a1d52d2aa8c2', '72cce0989df68887546746d8f09811aa', '单表', '1', NULL, 1, 1, 'admin', '2019-03-27 10:13:29', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('6d404fd2d82311fbc87722cd302a28bc', '4e4602b3e3686f0911384e188dc7efb4', '模糊', 'LIKE', '模糊', 7, 1, 'admin', '2019-04-01 16:46:02', 'admin', '2019-04-01 17:49:20');

INSERT IGNORE INTO `jimu_dict_item` VALUES ('6d4e26e78e1a09699182e08516c49fc4', '4d7fec1a7799a436d26d02325eff295e', '高', 'H', '高', 1, 1, 'admin', '2019-04-16 17:04:24', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('700e9f030654f3f90e9ba76ab0713551', '6b78e3f59faec1a4750acff08030a79b', '333', '333', NULL, NULL, 1, 'admin', '2019-02-21 19:59:47', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('7050c1522702bac3be40e3b7d2e1dfd8', 'c5a14c75172783d72cbee6ee7f5df5d1', '柱状图', 'bar', NULL, 1, 1, 'admin', '2019-04-12 17:05:17', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('71b924faa93805c5c1579f12e001c809', 'd6e1152968b02d69ff358c75b48a6ee1', 'OA办公', 'oa', NULL, 2, 1, 'admin', '2021-03-22 19:27:17', 'admin', '2019-03-22 23:24:36');

INSERT IGNORE INTO `jimu_dict_item` VALUES ('75b260d7db45a39fc7f21badeabdb0ed', 'c36169beb12de8a71c8683ee7c28a503', '不启用', '0', NULL, NULL, 1, 'admin', '2019-03-18 23:29:41', 'admin', '2019-03-18 23:29:54');

INSERT IGNORE INTO `jimu_dict_item` VALUES ('7688469db4a3eba61e6e35578dc7c2e5', 'c36169beb12de8a71c8683ee7c28a503', '启用', '1', NULL, NULL, 1, 'admin', '2019-03-18 23:29:28', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('78ea6cadac457967a4b1c4eb7aaa418c', 'fc6cd58fde2e8481db10d3a1e68ce70c', '正常', '1', NULL, NULL, 1, 'admin', '2019-03-18 23:30:28', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('7ccf7b80c70ee002eceb3116854b75cb', 'ac2f7c0c5c5775fcea7e2387bcb22f01', '按钮权限', '2', NULL, 1, 1, 'admin', '2019-03-18 23:25:40', 'jeecg', '2022-03-24 21:42:24');

INSERT IGNORE INTO `jimu_dict_item` VALUES ('81fb2bb0e838dc68b43f96cc309f8257', 'fc6cd58fde2e8481db10d3a1e68ce70c', '冻结', '2', NULL, NULL, 1, 'admin', '2019-03-18 23:30:37', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('83250269359855501ec4e9c0b7e21596', '4274efc2292239b6f000b153f50823ff', '可见/可访问(授权后可见/可访问)', '1', '', 1, 1, 'admin', '2019-05-10 17:54:51', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('84778d7e928bc843ad4756db1322301f', '4e4602b3e3686f0911384e188dc7efb4', '大于等于', '>=', '大于等于', 5, 1, 'admin', '2019-04-01 10:46:02', 'admin', '2019-04-01 17:49:05');

INSERT IGNORE INTO `jimu_dict_item` VALUES ('848d4da35ebd93782029c57b103e5b36', 'c5a14c75172783d72cbee6ee7f5df5d1', '饼图', 'pie', NULL, 3, 1, 'admin', '2019-04-12 17:05:49', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('84dfc178dd61b95a72900fcdd624c471', '78bda155fe380b1b3f175f1e88c284c6', '处理中', '2', '处理中', 2, 1, 'admin', '2019-05-09 16:33:01', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('86f19c7e0a73a0bae451021ac05b99dd', 'ac2f7c0c5c5775fcea7e2387bcb22f01', '子菜单', '1', NULL, NULL, 1, 'admin', '2019-03-18 23:25:27', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('8bccb963e1cd9e8d42482c54cc609ca2', '4f69be5f507accea8d5df5f11346181a', '微信', '3', NULL, 3, 1, 'admin', '2021-05-11 14:29:12', 'admin', '2019-04-11 14:29:31');

INSERT IGNORE INTO `jimu_dict_item` VALUES ('8c618902365ca681ebbbe1e28f11a548', '4c753b5293304e7a445fd2741b46529d', '启用', '1', '', 0, 1, 'admin', '2020-07-18 23:19:27', 'admin', '2019-05-17 14:51:18');

INSERT IGNORE INTO `jimu_dict_item` VALUES ('8cdf08045056671efd10677b8456c999', '4274efc2292239b6f000b153f50823ff', '可编辑(未授权时禁用)', '2', '', 2, 1, 'admin', '2019-05-10 17:55:38', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('8ff48e657a7c5090d4f2a59b37d1b878', '4d7fec1a7799a436d26d02325eff295e', '中', 'M', '中', 2, 1, 'admin', '2019-04-16 17:04:40', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('948923658baa330319e59b2213cda97c', '880a895c98afeca9d9ac39f29e67c13e', '添加', '2', '', 2, 1, 'admin', '2019-07-22 10:54:59', 'admin', '2019-07-22 10:55:36');

INSERT IGNORE INTO `jimu_dict_item` VALUES ('986779561591394304', '986779503584169984', '汉族', 'hanzu', NULL, 1, 1, 'admin', '2024-08-23 09:56:31', 'admin', '2024-08-23 09:56:45');

INSERT IGNORE INTO `jimu_dict_item` VALUES ('986779602800431104', '986779503584169984', '回族', 'huizu', NULL, 1, 1, 'admin', '2024-08-23 09:56:41', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('986779663689142272', '986779503584169984', '维吾尔族', 'weiwuer', NULL, 1, 1, 'admin', '2024-08-23 09:56:56', 'admin', '2025-01-09 10:42:09');

INSERT IGNORE INTO `jimu_dict_item` VALUES ('986779697352626176', '986779503584169984', '藏族', 'zangzu', NULL, 1, 1, 'admin', '2024-08-23 09:57:04', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('986783239996002304', '986783181955223552', '文盲', '0', NULL, 1, 1, 'admin', '2024-08-23 10:11:08', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('986783271616860160', '986783181955223552', '小学', '1', NULL, 1, 1, 'admin', '2024-08-23 10:11:16', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('986783303967526912', '986783181955223552', '初中', '2', NULL, 1, 1, 'admin', '2024-08-23 10:11:23', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('986783326532882432', '986783181955223552', '高中', '3', NULL, 1, 1, 'admin', '2024-08-23 10:11:29', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('986783354462752768', '986783181955223552', '专科', '4', NULL, 1, 1, 'admin', '2024-08-23 10:11:35', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('986783373504892928', '986783181955223552', '本科', '5', NULL, 1, 1, 'admin', '2024-08-23 10:11:40', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('986783415494070272', '986783181955223552', '研究生', '6', NULL, 1, 1, 'admin', '2024-08-23 10:11:50', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('986783438734708736', '986783181955223552', '博士', '7', NULL, 1, 1, 'admin', '2024-08-23 10:11:56', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('986784154379440128', '986784113082322944', '音乐', '0', NULL, 1, 1, 'admin', '2024-08-23 10:14:46', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('986784179134222336', '986784113082322944', '运动', '1', NULL, 1, 1, 'admin', '2024-08-23 10:14:52', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('986784205717721088', '986784113082322944', '舞蹈', '2', NULL, 1, 1, 'admin', '2024-08-23 10:14:58', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('986784229390372864', '986784113082322944', '棋牌', '3', NULL, 1, 1, 'admin', '2024-08-23 10:15:04', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('986784260960899072', '986784113082322944', '电竞', '4', NULL, 1, 1, 'admin', '2024-08-23 10:15:12', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('9a96c4a4e4c5c9b4e4d0cbf6eb3243cc', '4c753b5293304e7a445fd2741b46529d', '不启用', '0', NULL, 1, 1, 'admin', '2019-03-18 23:19:53', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('a1e7d1ca507cff4a480c8caba7c1339e', '880a895c98afeca9d9ac39f29e67c13e', '导出', '6', '', 6, 1, 'admin', '2019-07-22 12:06:50', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('a2be752dd4ec980afaec1efd1fb589af', '8dfe32e2d29ea9430a988b3b558bf233', '已撤销', '2', '已撤销', 3, 1, 'admin', '2019-04-16 17:41:39', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('aa0d8a8042a18715a17f0a888d360aa4', 'ac2f7c0c5c5775fcea7e2387bcb22f01', '一级菜单', '0', NULL, NULL, 1, 'admin', '2019-03-18 23:24:52', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('adcf2a1fe93bb99a84833043f475fe0b', '4e4602b3e3686f0911384e188dc7efb4', '包含', 'IN', '包含', 8, 1, 'admin', '2019-04-01 16:45:47', 'admin', '2019-04-01 17:49:24');

INSERT IGNORE INTO `jimu_dict_item` VALUES ('b029a41a851465332ee4ee69dcf0a4c2', '0b5d19e1fce4b2e6647e6b4a17760c14', '系统消息', '2', NULL, 1, 1, 'admin', '2019-02-22 18:02:08', 'admin', '2019-04-22 18:02:13');

INSERT IGNORE INTO `jimu_dict_item` VALUES ('b2a8b4bb2c8e66c2c4b1bb086337f393', '3486f32803bb953e7155dab3513dc68b', '正常', '0', NULL, NULL, 1, 'admin', '2022-10-18 21:46:48', 'admin', '2019-03-28 22:22:20');

INSERT IGNORE INTO `jimu_dict_item` VALUES ('b57f98b88363188daf38d42f25991956', '6b78e3f59faec1a4750acff08030a79b', '22', '222', NULL, NULL, 0, 'admin', '2019-02-21 19:59:43', 'admin', '2019-03-11 21:23:27');

INSERT IGNORE INTO `jimu_dict_item` VALUES ('b5f3bd5f66bb9a83fecd89228c0d93d1', '68168534ff5065a152bfab275c2136f8', '无效', '0', '无效', 1, 1, 'admin', '2019-04-26 19:21:49', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('b9fbe2a3602d4a27b45c100ac5328484', '78bda155fe380b1b3f175f1e88c284c6', '待提交', '1', '待提交', 1, 1, 'admin', '2019-05-09 16:32:35', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('ba27737829c6e0e582e334832703d75e', '236e8a4baff0db8c62c00dd95632834f', '同步', '1', '同步', 1, 1, 'admin', '2019-05-15 15:28:15', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('bcec04526b04307e24a005d6dcd27fd6', '880a895c98afeca9d9ac39f29e67c13e', '导入', '5', '', 5, 1, 'admin', '2019-07-22 12:06:41', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('c2b_sens_abuse_dict0it000', 'c2b_sens_abuse_dict000001', '傻', '傻', NULL, 1, 1, 'admin', '2026-05-25 12:00:00', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('c2b_sens_abuse_dict0it001', 'c2b_sens_abuse_dict000001', '傻逼', '傻逼', NULL, 2, 1, 'admin', '2026-05-25 12:00:00', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('c2b_sens_abuse_dict0it002', 'c2b_sens_abuse_dict000001', '傻B', '傻B', NULL, 3, 1, 'admin', '2026-05-25 12:00:00', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('c2b_sens_abuse_dict0it003', 'c2b_sens_abuse_dict000001', '沙雕', '沙雕', NULL, 4, 1, 'admin', '2026-05-25 12:00:00', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('c2b_sens_abuse_dict0it004', 'c2b_sens_abuse_dict000001', '蠢货', '蠢货', NULL, 5, 1, 'admin', '2026-05-25 12:00:00', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('c2b_sens_abuse_dict0it005', 'c2b_sens_abuse_dict000001', '蠢猪', '蠢猪', NULL, 6, 1, 'admin', '2026-05-25 12:00:00', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('c2b_sens_abuse_dict0it006', 'c2b_sens_abuse_dict000001', '蠢驴', '蠢驴', NULL, 7, 1, 'admin', '2026-05-25 12:00:00', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('c2b_sens_abuse_dict0it007', 'c2b_sens_abuse_dict000001', '废物', '废物', NULL, 8, 1, 'admin', '2026-05-25 12:00:00', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('c2b_sens_abuse_dict0it008', 'c2b_sens_abuse_dict000001', '垃圾', '垃圾', NULL, 9, 1, 'admin', '2026-05-25 12:00:00', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('c2b_sens_abuse_dict0it009', 'c2b_sens_abuse_dict000001', '笨蛋', '笨蛋', NULL, 10, 1, 'admin', '2026-05-25 12:00:00', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('c2b_sens_abuse_dict0it010', 'c2b_sens_abuse_dict000001', '白痴', '白痴', NULL, 11, 1, 'admin', '2026-05-25 12:00:00', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('c2b_sens_abuse_dict0it011', 'c2b_sens_abuse_dict000001', '智障', '智障', NULL, 12, 1, 'admin', '2026-05-25 12:00:00', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('c2b_sens_abuse_dict0it012', 'c2b_sens_abuse_dict000001', '脑残', '脑残', NULL, 13, 1, 'admin', '2026-05-25 12:00:00', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('c2b_sens_abuse_dict0it013', 'c2b_sens_abuse_dict000001', '弱智', '弱智', NULL, 14, 1, 'admin', '2026-05-25 12:00:00', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('c2b_sens_abuse_dict0it014', 'c2b_sens_abuse_dict000001', '神经病', '神经病', NULL, 15, 1, 'admin', '2026-05-25 12:00:00', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('c2b_sens_abuse_dict0it015', 'c2b_sens_abuse_dict000001', '混蛋', '混蛋', NULL, 16, 1, 'admin', '2026-05-25 12:00:00', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('c2b_sens_abuse_dict0it016', 'c2b_sens_abuse_dict000001', '王八蛋', '王八蛋', NULL, 17, 1, 'admin', '2026-05-25 12:00:00', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('c2b_sens_abuse_dict0it017', 'c2b_sens_abuse_dict000001', '畜生', '畜生', NULL, 18, 1, 'admin', '2026-05-25 12:00:00', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('c2b_sens_abuse_dict0it018', 'c2b_sens_abuse_dict000001', '贱人', '贱人', NULL, 19, 1, 'admin', '2026-05-25 12:00:00', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('c2b_sens_abuse_dict0it019', 'c2b_sens_abuse_dict000001', '贱货', '贱货', NULL, 20, 1, 'admin', '2026-05-25 12:00:00', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('c2b_sens_abuse_dict0it020', 'c2b_sens_abuse_dict000001', '婊子', '婊子', NULL, 21, 1, 'admin', '2026-05-25 12:00:00', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('c2b_sens_abuse_dict0it021', 'c2b_sens_abuse_dict000001', '狗东西', '狗东西', NULL, 22, 1, 'admin', '2026-05-25 12:00:00', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('c2b_sens_abuse_dict0it022', 'c2b_sens_abuse_dict000001', '狗屎', '狗屎', NULL, 23, 1, 'admin', '2026-05-25 12:00:00', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('c2b_sens_abuse_dict0it023', 'c2b_sens_abuse_dict000001', '去死', '去死', NULL, 24, 1, 'admin', '2026-05-25 12:00:00', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('c2b_sens_abuse_dict0it024', 'c2b_sens_abuse_dict000001', '该死', '该死', NULL, 25, 1, 'admin', '2026-05-25 12:00:00', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('c2b_sens_abuse_dict0it025', 'c2b_sens_abuse_dict000001', '滚蛋', '滚蛋', NULL, 26, 1, 'admin', '2026-05-25 12:00:00', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('c2b_sens_abuse_dict0it026', 'c2b_sens_abuse_dict000001', '二货', '二货', NULL, 27, 1, 'admin', '2026-05-25 12:00:00', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('c2b_sens_abuse_dict0it027', 'c2b_sens_abuse_dict000001', '饭桶', '饭桶', NULL, 28, 1, 'admin', '2026-05-25 12:00:00', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('c2b_sens_abuse_dict0it028', 'c2b_sens_abuse_dict000001', '草泥马', '草泥马', NULL, 29, 1, 'admin', '2026-05-25 12:00:00', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('c2b_sens_abuse_dict0it029', 'c2b_sens_abuse_dict000001', '妈的', '妈的', NULL, 30, 1, 'admin', '2026-05-25 12:00:00', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('c2b_sens_abuse_dict0it030', 'c2b_sens_abuse_dict000001', '他妈的', '他妈的', NULL, 31, 1, 'admin', '2026-05-25 12:00:00', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('c2b_sens_abuse_dict0it031', 'c2b_sens_abuse_dict000001', '卧槽', '卧槽', NULL, 32, 1, 'admin', '2026-05-25 12:00:00', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('c2b_sens_politics_diit000', 'c2b_sens_politics_dict0001', '领导', '领导', NULL, 1, 1, 'admin', '2026-05-25 12:00:00', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('c2b_sens_politics_diit001', 'c2b_sens_politics_dict0001', '书记', '书记', NULL, 2, 1, 'admin', '2026-05-25 12:00:00', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('c2b_sens_politics_diit002', 'c2b_sens_politics_dict0001', '市长', '市长', NULL, 3, 1, 'admin', '2026-05-25 12:00:00', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('c2b_sens_politics_diit003', 'c2b_sens_politics_dict0001', '省长', '省长', NULL, 4, 1, 'admin', '2026-05-25 12:00:00', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('c2b_sens_politics_diit004', 'c2b_sens_politics_dict0001', '县长', '县长', NULL, 5, 1, 'admin', '2026-05-25 12:00:00', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('c2b_sens_politics_diit005', 'c2b_sens_politics_dict0001', '部长', '部长', NULL, 6, 1, 'admin', '2026-05-25 12:00:00', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('c2b_sens_politics_diit006', 'c2b_sens_politics_dict0001', '主席', '主席', NULL, 7, 1, 'admin', '2026-05-25 12:00:00', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('c2b_sens_politics_diit007', 'c2b_sens_politics_dict0001', '总理', '总理', NULL, 8, 1, 'admin', '2026-05-25 12:00:00', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('c2b_sens_politics_diit008', 'c2b_sens_politics_dict0001', '省委', '省委', NULL, 9, 1, 'admin', '2026-05-25 12:00:00', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('c2b_sens_politics_diit009', 'c2b_sens_politics_dict0001', '市委', '市委', NULL, 10, 1, 'admin', '2026-05-25 12:00:00', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('c2b_sens_politics_diit010', 'c2b_sens_politics_dict0001', '中央', '中央', NULL, 11, 1, 'admin', '2026-05-25 12:00:00', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('c2b_sens_politics_diit011', 'c2b_sens_politics_dict0001', '国务院', '国务院', NULL, 12, 1, 'admin', '2026-05-25 12:00:00', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('c2b_sens_politics_diit012', 'c2b_sens_politics_dict0001', '政府', '政府', NULL, 13, 1, 'admin', '2026-05-25 12:00:00', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('c2b_sens_politics_diit013', 'c2b_sens_politics_dict0001', '政治', '政治', NULL, 14, 1, 'admin', '2026-05-25 12:00:00', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('c2b_sens_politics_diit014', 'c2b_sens_politics_dict0001', '政变', '政变', NULL, 15, 1, 'admin', '2026-05-25 12:00:00', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('c2b_sens_politics_diit015', 'c2b_sens_politics_dict0001', '游行', '游行', NULL, 16, 1, 'admin', '2026-05-25 12:00:00', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('c2b_sens_politics_diit016', 'c2b_sens_politics_dict0001', '示威', '示威', NULL, 17, 1, 'admin', '2026-05-25 12:00:00', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('c2b_sens_politics_diit017', 'c2b_sens_politics_dict0001', '颠覆', '颠覆', NULL, 18, 1, 'admin', '2026-05-25 12:00:00', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('c2b_sens_politics_diit018', 'c2b_sens_politics_dict0001', '反动', '反动', NULL, 19, 1, 'admin', '2026-05-25 12:00:00', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('c2b_sens_politics_diit019', 'c2b_sens_politics_dict0001', '邪教', '邪教', NULL, 20, 1, 'admin', '2026-05-25 12:00:00', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('c53da022b9912e0aed691bbec3c78473', '880a895c98afeca9d9ac39f29e67c13e', '查询', '1', '', 1, 1, 'admin', '2019-07-22 10:54:51', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('c5700a71ad08994d18ad1dacc37a71a9', 'a7adbcd86c37f7dbc9b66945c82ef9e6', '否', '0', '', 1, 1, 'admin', '2019-05-22 19:29:55', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('cbfcc5b88fc3a90975df23ffc8cbe29c', 'c5a14c75172783d72cbee6ee7f5df5d1', '曲线图', 'line', NULL, 2, 1, 'admin', '2019-05-12 17:05:30', 'admin', '2019-04-12 17:06:06');

INSERT IGNORE INTO `jimu_dict_item` VALUES ('d217592908ea3e00ff986ce97f24fb98', 'c5a14c75172783d72cbee6ee7f5df5d1', '数据列表', 'table', NULL, 4, 1, 'admin', '2019-04-12 17:05:56', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('df168368dcef46cade2aadd80100d8aa', '3d9a351be3436fbefb1307d4cfb49bf2', '男', '1', NULL, 1, 1, NULL, '2027-08-04 14:56:49', 'admin', '2019-03-23 22:44:44');

INSERT IGNORE INTO `jimu_dict_item` VALUES ('e6329e3a66a003819e2eb830b0ca2ea0', '4e4602b3e3686f0911384e188dc7efb4', '小于', '<', '小于', 2, 1, 'admin', '2019-04-01 16:44:15', 'admin', '2019-04-01 17:48:34');

INSERT IGNORE INTO `jimu_dict_item` VALUES ('e94eb7af89f1dbfa0d823580a7a6e66a', '236e8a4baff0db8c62c00dd95632834f', '不同步', '0', '不同步', 2, 1, 'admin', '2019-05-15 15:28:28', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('f0162f4cc572c9273f3e26b2b4d8c082', 'ad7c65ba97c20a6805d5dcdf13cdaf36', 'booostrap', '1', NULL, 1, 1, 'admin', '2021-08-22 16:32:04', 'admin', '2019-03-22 16:33:57');

INSERT IGNORE INTO `jimu_dict_item` VALUES ('f16c5706f3ae05c57a53850c64ce7c45', 'a9d9942bd0eccb6e89de92d130ec4c4a', '发送成功', '1', NULL, 2, 1, 'admin', '2019-04-12 18:19:43', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('f2a7920421f3335afdf6ad2b342f6b5d', '845da5006c97754728bf48b6a10f79cc', '冻结', '2', NULL, NULL, 1, 'admin', '2019-03-18 21:46:02', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('f37f90c496ec9841c4c326b065e00bb2', '83bfb33147013cc81640d5fd9eda030c', '登录日志', '1', NULL, NULL, 1, 'admin', '2019-03-18 23:22:37', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('f753aff60ff3931c0ecb4812d8b5e643', '4c03fca6bf1f0299c381213961566349', '双排布局', 'double', NULL, 3, 1, 'admin', '2019-04-12 17:43:51', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('f80a8f6838215753b05e1a5ba3346d22', '880a895c98afeca9d9ac39f29e67c13e', '删除', '4', '', 4, 1, 'admin', '2019-07-22 10:55:14', 'admin', '2019-07-22 10:55:30');

INSERT IGNORE INTO `jimu_dict_item` VALUES ('fcec03570f68a175e1964808dc3f1c91', '4c03fca6bf1f0299c381213961566349', 'Tab风格', 'tab', NULL, 1, 1, 'admin', '2019-04-12 17:43:31', NULL, NULL);

INSERT IGNORE INTO `jimu_dict_item` VALUES ('fe50b23ae5e68434def76f67cef35d2d', '78bda155fe380b1b3f175f1e88c284c6', '已作废', '4', '已作废', 4, 1, 'admin', '2021-09-09 16:33:43', 'admin', '2019-05-09 16:34:40');

INSERT IGNORE INTO `jimu_report_category` VALUES ('984272091947253760', '数据报表', '0', 1, 'report', 0, 'admin', '2024-08-16 11:52:44', NULL, NULL, '1000', 0);

INSERT IGNORE INTO `jimu_report_category` VALUES ('984302961118724096', '图形报表', '0', 1, 'report', 0, 'admin', '2024-08-16 13:55:24', NULL, NULL, '1000', 0);

INSERT IGNORE INTO `jimu_report_category` VALUES ('984302991393210368', '打印设计', '0', 1, 'report', 0, 'admin', '2024-08-16 13:55:31', NULL, NULL, '1000', 0);

INSERT IGNORE INTO `jimu_report_category` VALUES ('988299668956545024', '仪表盘设计', '0', 1, 'drag', 0, '15931993294', '2024-08-27 00:00:00', '15931993294', '2024-08-28 00:00:00', NULL, 0);

INSERT IGNORE INTO `jimu_report_category` VALUES ('988299695309357056', '门户设计', '0', 1, 'drag', 0, '15931993294', '2024-08-27 00:00:00', '15931993294', '2024-08-27 00:00:00', NULL, 0);
