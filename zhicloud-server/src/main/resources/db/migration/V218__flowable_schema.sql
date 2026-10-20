-- ============================================================
-- V218 迁移：初始化 Flowable 8.0.0 引擎表结构（幂等）
--
-- 背景：Flowable 的建表 DDL 只随 jar 发布（org/flowable/**/db/create/flowable.mysql.create.*.sql），
--      仓库里既没有 flowable.sql，也没有任何迁移会创建 ACT_* / FLW_* 引擎表。
--      prod profile 明确禁止自动 DDL（flowable.database-schema-update=false），
--      于是"全新安装 + prod profile"会在启动时直接失败：
--      "no flowable tables in db ... Table 'ACT_GE_PROPERTY' doesn't exist"。
--      local/dev profile 之所以能起来，是靠 database-schema-update=true 现场建表，掩盖了这个缺口。
--
-- 处理：把 Flowable 8.0.0 官方 MySQL 建表脚本纳入迁移链，并全部改写成幂等形式：
--        - create table            -> create table if not exists
--        - create index            -> information_schema.statistics 判断 + PREPARE 执行
--        - alter table add constraint -> information_schema.table_constraints 判断 + PREPARE 执行
--        - insert                  -> insert ignore
--      于是：全新安装会建出引擎表；存量库（表已由 local profile 自动建好）幂等跳过，不改动既有结构。
--
-- 引擎范围：common + bpm engine + history + idm + eventregistry
--      （app 引擎在 application.yaml 里 flowable.app.enabled=false 显式关闭，不引入 ACT_APP_* 表）
--
-- 维护：Flowable 版本升级时，用新版本 jar 的同名脚本重新生成并新增迁移版本，不要改本文件。
-- ============================================================


-- ===== org/flowable/common/db/create/flowable.mysql.create.common.sql =====
CREATE TABLE IF NOT EXISTS ACT_GE_PROPERTY (
    NAME_ varchar(64),
    VALUE_ varchar(300),
    REV_ integer,
    primary key (NAME_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;
CREATE TABLE IF NOT EXISTS ACT_GE_BYTEARRAY (
    ID_ varchar(64),
    REV_ integer,
    NAME_ varchar(255),
    DEPLOYMENT_ID_ varchar(64),
    BYTES_ LONGBLOB,
    GENERATED_ TINYINT,
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;
INSERT IGNORE INTO ACT_GE_PROPERTY
values ('common.schema.version', '8.0.0.0', 1);
INSERT IGNORE INTO ACT_GE_PROPERTY
values ('next.dbid', '1', 1);
CREATE TABLE IF NOT EXISTS ACT_RU_ENTITYLINK (
    ID_ varchar(64),
    REV_ integer,
    CREATE_TIME_ datetime(3),
    LINK_TYPE_ varchar(255),
    SCOPE_ID_ varchar(255),
    SUB_SCOPE_ID_ varchar(255),
    SCOPE_TYPE_ varchar(255),
    SCOPE_DEFINITION_ID_ varchar(255),
    PARENT_ELEMENT_ID_ varchar(255),
    REF_SCOPE_ID_ varchar(255),
    REF_SCOPE_TYPE_ varchar(255),
    REF_SCOPE_DEFINITION_ID_ varchar(255),
    ROOT_SCOPE_ID_ varchar(255),
    ROOT_SCOPE_TYPE_ varchar(255),
    HIERARCHY_TYPE_ varchar(255),
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;
-- 索引 ACT_IDX_ENT_LNK_SCOPE（幂等）
SET @v218_idx_act_idx_ent_lnk_scope := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_RU_ENTITYLINK'
      AND index_name = 'ACT_IDX_ENT_LNK_SCOPE'
);
SET @v218_sql := IF(
    @v218_idx_act_idx_ent_lnk_scope = 0,
    'CREATE INDEX `ACT_IDX_ENT_LNK_SCOPE` ON `ACT_RU_ENTITYLINK` (SCOPE_ID_, SCOPE_TYPE_, LINK_TYPE_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 索引 ACT_IDX_ENT_LNK_REF_SCOPE（幂等）
SET @v218_idx_act_idx_ent_lnk_ref_scope := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_RU_ENTITYLINK'
      AND index_name = 'ACT_IDX_ENT_LNK_REF_SCOPE'
);
SET @v218_sql := IF(
    @v218_idx_act_idx_ent_lnk_ref_scope = 0,
    'CREATE INDEX `ACT_IDX_ENT_LNK_REF_SCOPE` ON `ACT_RU_ENTITYLINK` (REF_SCOPE_ID_, REF_SCOPE_TYPE_, LINK_TYPE_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 索引 ACT_IDX_ENT_LNK_ROOT_SCOPE（幂等）
SET @v218_idx_act_idx_ent_lnk_root_scope := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_RU_ENTITYLINK'
      AND index_name = 'ACT_IDX_ENT_LNK_ROOT_SCOPE'
);
SET @v218_sql := IF(
    @v218_idx_act_idx_ent_lnk_root_scope = 0,
    'CREATE INDEX `ACT_IDX_ENT_LNK_ROOT_SCOPE` ON `ACT_RU_ENTITYLINK` (ROOT_SCOPE_ID_, ROOT_SCOPE_TYPE_, LINK_TYPE_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 索引 ACT_IDX_ENT_LNK_SCOPE_DEF（幂等）
SET @v218_idx_act_idx_ent_lnk_scope_def := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_RU_ENTITYLINK'
      AND index_name = 'ACT_IDX_ENT_LNK_SCOPE_DEF'
);
SET @v218_sql := IF(
    @v218_idx_act_idx_ent_lnk_scope_def = 0,
    'CREATE INDEX `ACT_IDX_ENT_LNK_SCOPE_DEF` ON `ACT_RU_ENTITYLINK` (SCOPE_DEFINITION_ID_, SCOPE_TYPE_, LINK_TYPE_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
CREATE TABLE IF NOT EXISTS ACT_HI_ENTITYLINK (
    ID_ varchar(64),
    LINK_TYPE_ varchar(255),
    CREATE_TIME_ datetime(3),
    SCOPE_ID_ varchar(255),
    SUB_SCOPE_ID_ varchar(255),
    SCOPE_TYPE_ varchar(255),
    SCOPE_DEFINITION_ID_ varchar(255),
    PARENT_ELEMENT_ID_ varchar(255),
    REF_SCOPE_ID_ varchar(255),
    REF_SCOPE_TYPE_ varchar(255),
    REF_SCOPE_DEFINITION_ID_ varchar(255),
    ROOT_SCOPE_ID_ varchar(255),
    ROOT_SCOPE_TYPE_ varchar(255),
    HIERARCHY_TYPE_ varchar(255),
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;
-- 索引 ACT_IDX_HI_ENT_LNK_SCOPE（幂等）
SET @v218_idx_act_idx_hi_ent_lnk_scope := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_HI_ENTITYLINK'
      AND index_name = 'ACT_IDX_HI_ENT_LNK_SCOPE'
);
SET @v218_sql := IF(
    @v218_idx_act_idx_hi_ent_lnk_scope = 0,
    'CREATE INDEX `ACT_IDX_HI_ENT_LNK_SCOPE` ON `ACT_HI_ENTITYLINK` (SCOPE_ID_, SCOPE_TYPE_, LINK_TYPE_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 索引 ACT_IDX_HI_ENT_LNK_REF_SCOPE（幂等）
SET @v218_idx_act_idx_hi_ent_lnk_ref_scope := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_HI_ENTITYLINK'
      AND index_name = 'ACT_IDX_HI_ENT_LNK_REF_SCOPE'
);
SET @v218_sql := IF(
    @v218_idx_act_idx_hi_ent_lnk_ref_scope = 0,
    'CREATE INDEX `ACT_IDX_HI_ENT_LNK_REF_SCOPE` ON `ACT_HI_ENTITYLINK` (REF_SCOPE_ID_, REF_SCOPE_TYPE_, LINK_TYPE_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 索引 ACT_IDX_HI_ENT_LNK_ROOT_SCOPE（幂等）
SET @v218_idx_act_idx_hi_ent_lnk_root_scope := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_HI_ENTITYLINK'
      AND index_name = 'ACT_IDX_HI_ENT_LNK_ROOT_SCOPE'
);
SET @v218_sql := IF(
    @v218_idx_act_idx_hi_ent_lnk_root_scope = 0,
    'CREATE INDEX `ACT_IDX_HI_ENT_LNK_ROOT_SCOPE` ON `ACT_HI_ENTITYLINK` (ROOT_SCOPE_ID_, ROOT_SCOPE_TYPE_, LINK_TYPE_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 索引 ACT_IDX_HI_ENT_LNK_SCOPE_DEF（幂等）
SET @v218_idx_act_idx_hi_ent_lnk_scope_def := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_HI_ENTITYLINK'
      AND index_name = 'ACT_IDX_HI_ENT_LNK_SCOPE_DEF'
);
SET @v218_sql := IF(
    @v218_idx_act_idx_hi_ent_lnk_scope_def = 0,
    'CREATE INDEX `ACT_IDX_HI_ENT_LNK_SCOPE_DEF` ON `ACT_HI_ENTITYLINK` (SCOPE_DEFINITION_ID_, SCOPE_TYPE_, LINK_TYPE_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
CREATE TABLE IF NOT EXISTS ACT_RU_IDENTITYLINK (
    ID_ varchar(64),
    REV_ integer,
    GROUP_ID_ varchar(255),
    TYPE_ varchar(255),
    USER_ID_ varchar(255),
    TASK_ID_ varchar(64),
    PROC_INST_ID_ varchar(64),
    PROC_DEF_ID_ varchar(64),
    SCOPE_ID_ varchar(255),
    SUB_SCOPE_ID_ varchar(255),
    SCOPE_TYPE_ varchar(255),
    SCOPE_DEFINITION_ID_ varchar(255),
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;
-- 索引 ACT_IDX_IDENT_LNK_USER（幂等）
SET @v218_idx_act_idx_ident_lnk_user := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_RU_IDENTITYLINK'
      AND index_name = 'ACT_IDX_IDENT_LNK_USER'
);
SET @v218_sql := IF(
    @v218_idx_act_idx_ident_lnk_user = 0,
    'CREATE INDEX `ACT_IDX_IDENT_LNK_USER` ON `ACT_RU_IDENTITYLINK` (USER_ID_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 索引 ACT_IDX_IDENT_LNK_GROUP（幂等）
SET @v218_idx_act_idx_ident_lnk_group := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_RU_IDENTITYLINK'
      AND index_name = 'ACT_IDX_IDENT_LNK_GROUP'
);
SET @v218_sql := IF(
    @v218_idx_act_idx_ident_lnk_group = 0,
    'CREATE INDEX `ACT_IDX_IDENT_LNK_GROUP` ON `ACT_RU_IDENTITYLINK` (GROUP_ID_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 索引 ACT_IDX_IDENT_LNK_SCOPE（幂等）
SET @v218_idx_act_idx_ident_lnk_scope := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_RU_IDENTITYLINK'
      AND index_name = 'ACT_IDX_IDENT_LNK_SCOPE'
);
SET @v218_sql := IF(
    @v218_idx_act_idx_ident_lnk_scope = 0,
    'CREATE INDEX `ACT_IDX_IDENT_LNK_SCOPE` ON `ACT_RU_IDENTITYLINK` (SCOPE_ID_, SCOPE_TYPE_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 索引 ACT_IDX_IDENT_LNK_SUB_SCOPE（幂等）
SET @v218_idx_act_idx_ident_lnk_sub_scope := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_RU_IDENTITYLINK'
      AND index_name = 'ACT_IDX_IDENT_LNK_SUB_SCOPE'
);
SET @v218_sql := IF(
    @v218_idx_act_idx_ident_lnk_sub_scope = 0,
    'CREATE INDEX `ACT_IDX_IDENT_LNK_SUB_SCOPE` ON `ACT_RU_IDENTITYLINK` (SUB_SCOPE_ID_, SCOPE_TYPE_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 索引 ACT_IDX_IDENT_LNK_SCOPE_DEF（幂等）
SET @v218_idx_act_idx_ident_lnk_scope_def := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_RU_IDENTITYLINK'
      AND index_name = 'ACT_IDX_IDENT_LNK_SCOPE_DEF'
);
SET @v218_sql := IF(
    @v218_idx_act_idx_ident_lnk_scope_def = 0,
    'CREATE INDEX `ACT_IDX_IDENT_LNK_SCOPE_DEF` ON `ACT_RU_IDENTITYLINK` (SCOPE_DEFINITION_ID_, SCOPE_TYPE_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
CREATE TABLE IF NOT EXISTS ACT_HI_IDENTITYLINK (
    ID_ varchar(64),
    GROUP_ID_ varchar(255),
    TYPE_ varchar(255),
    USER_ID_ varchar(255),
    TASK_ID_ varchar(64),
    CREATE_TIME_ datetime(3),
    PROC_INST_ID_ varchar(64),
    SCOPE_ID_ varchar(255),
    SUB_SCOPE_ID_ varchar(255),
    SCOPE_TYPE_ varchar(255),
    SCOPE_DEFINITION_ID_ varchar(255),
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;
-- 索引 ACT_IDX_HI_IDENT_LNK_USER（幂等）
SET @v218_idx_act_idx_hi_ident_lnk_user := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_HI_IDENTITYLINK'
      AND index_name = 'ACT_IDX_HI_IDENT_LNK_USER'
);
SET @v218_sql := IF(
    @v218_idx_act_idx_hi_ident_lnk_user = 0,
    'CREATE INDEX `ACT_IDX_HI_IDENT_LNK_USER` ON `ACT_HI_IDENTITYLINK` (USER_ID_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 索引 ACT_IDX_HI_IDENT_LNK_SCOPE（幂等）
SET @v218_idx_act_idx_hi_ident_lnk_scope := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_HI_IDENTITYLINK'
      AND index_name = 'ACT_IDX_HI_IDENT_LNK_SCOPE'
);
SET @v218_sql := IF(
    @v218_idx_act_idx_hi_ident_lnk_scope = 0,
    'CREATE INDEX `ACT_IDX_HI_IDENT_LNK_SCOPE` ON `ACT_HI_IDENTITYLINK` (SCOPE_ID_, SCOPE_TYPE_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 索引 ACT_IDX_HI_IDENT_LNK_SUB_SCOPE（幂等）
SET @v218_idx_act_idx_hi_ident_lnk_sub_scope := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_HI_IDENTITYLINK'
      AND index_name = 'ACT_IDX_HI_IDENT_LNK_SUB_SCOPE'
);
SET @v218_sql := IF(
    @v218_idx_act_idx_hi_ident_lnk_sub_scope = 0,
    'CREATE INDEX `ACT_IDX_HI_IDENT_LNK_SUB_SCOPE` ON `ACT_HI_IDENTITYLINK` (SUB_SCOPE_ID_, SCOPE_TYPE_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 索引 ACT_IDX_HI_IDENT_LNK_SCOPE_DEF（幂等）
SET @v218_idx_act_idx_hi_ident_lnk_scope_def := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_HI_IDENTITYLINK'
      AND index_name = 'ACT_IDX_HI_IDENT_LNK_SCOPE_DEF'
);
SET @v218_sql := IF(
    @v218_idx_act_idx_hi_ident_lnk_scope_def = 0,
    'CREATE INDEX `ACT_IDX_HI_IDENT_LNK_SCOPE_DEF` ON `ACT_HI_IDENTITYLINK` (SCOPE_DEFINITION_ID_, SCOPE_TYPE_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
CREATE TABLE IF NOT EXISTS ACT_RU_JOB (
    ID_ varchar(64) NOT NULL,
    REV_ integer,
    CATEGORY_ varchar(255),
    TYPE_ varchar(255) NOT NULL,
    LOCK_EXP_TIME_ timestamp(3) NULL,
    LOCK_OWNER_ varchar(255),
    EXCLUSIVE_ boolean,
    EXECUTION_ID_ varchar(64),
    PROCESS_INSTANCE_ID_ varchar(64),
    PROC_DEF_ID_ varchar(64),
    ELEMENT_ID_ varchar(255),
    ELEMENT_NAME_ varchar(255),
    SCOPE_ID_ varchar(255),
    SUB_SCOPE_ID_ varchar(255),
    SCOPE_TYPE_ varchar(255),
    SCOPE_DEFINITION_ID_ varchar(255),
    CORRELATION_ID_ varchar(255),
    RETRIES_ integer,
    EXCEPTION_STACK_ID_ varchar(64),
    EXCEPTION_MSG_ varchar(4000),
    DUEDATE_ timestamp(3) NULL,
    REPEAT_ varchar(255),
    HANDLER_TYPE_ varchar(255),
    HANDLER_CFG_ varchar(4000),
    CUSTOM_VALUES_ID_ varchar(64),
    CREATE_TIME_ timestamp(3) NULL,
    TENANT_ID_ varchar(255) default '',
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;
CREATE TABLE IF NOT EXISTS ACT_RU_TIMER_JOB (
    ID_ varchar(64) NOT NULL,
    REV_ integer,
    CATEGORY_ varchar(255),
    TYPE_ varchar(255) NOT NULL,
    LOCK_EXP_TIME_ timestamp(3) NULL,
    LOCK_OWNER_ varchar(255),
    EXCLUSIVE_ boolean,
    EXECUTION_ID_ varchar(64),
    PROCESS_INSTANCE_ID_ varchar(64),
    PROC_DEF_ID_ varchar(64),
    ELEMENT_ID_ varchar(255),
    ELEMENT_NAME_ varchar(255),
    SCOPE_ID_ varchar(255),
    SUB_SCOPE_ID_ varchar(255),
    SCOPE_TYPE_ varchar(255),
    SCOPE_DEFINITION_ID_ varchar(255),
    CORRELATION_ID_ varchar(255),
    RETRIES_ integer,
    EXCEPTION_STACK_ID_ varchar(64),
    EXCEPTION_MSG_ varchar(4000),
    DUEDATE_ timestamp(3) NULL,
    REPEAT_ varchar(255),
    HANDLER_TYPE_ varchar(255),
    HANDLER_CFG_ varchar(4000),
    CUSTOM_VALUES_ID_ varchar(64),
    CREATE_TIME_ timestamp(3) NULL,
    TENANT_ID_ varchar(255) default '',
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;
CREATE TABLE IF NOT EXISTS ACT_RU_SUSPENDED_JOB (
    ID_ varchar(64) NOT NULL,
    REV_ integer,
    CATEGORY_ varchar(255),
    TYPE_ varchar(255) NOT NULL,
    EXCLUSIVE_ boolean,
    EXECUTION_ID_ varchar(64),
    PROCESS_INSTANCE_ID_ varchar(64),
    PROC_DEF_ID_ varchar(64),
    ELEMENT_ID_ varchar(255),
    ELEMENT_NAME_ varchar(255),
    SCOPE_ID_ varchar(255),
    SUB_SCOPE_ID_ varchar(255),
    SCOPE_TYPE_ varchar(255),
    SCOPE_DEFINITION_ID_ varchar(255),
    CORRELATION_ID_ varchar(255),
    RETRIES_ integer,
    EXCEPTION_STACK_ID_ varchar(64),
    EXCEPTION_MSG_ varchar(4000),
    DUEDATE_ timestamp(3) NULL,
    REPEAT_ varchar(255),
    HANDLER_TYPE_ varchar(255),
    HANDLER_CFG_ varchar(4000),
    CUSTOM_VALUES_ID_ varchar(64),
    CREATE_TIME_ timestamp(3) NULL,
    TENANT_ID_ varchar(255) default '',
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;
CREATE TABLE IF NOT EXISTS ACT_RU_DEADLETTER_JOB (
    ID_ varchar(64) NOT NULL,
    REV_ integer,
    CATEGORY_ varchar(255),
    TYPE_ varchar(255) NOT NULL,
    EXCLUSIVE_ boolean,
    EXECUTION_ID_ varchar(64),
    PROCESS_INSTANCE_ID_ varchar(64),
    PROC_DEF_ID_ varchar(64),
    ELEMENT_ID_ varchar(255),
    ELEMENT_NAME_ varchar(255),
    SCOPE_ID_ varchar(255),
    SUB_SCOPE_ID_ varchar(255),
    SCOPE_TYPE_ varchar(255),
    SCOPE_DEFINITION_ID_ varchar(255),
    CORRELATION_ID_ varchar(255),
    EXCEPTION_STACK_ID_ varchar(64),
    EXCEPTION_MSG_ varchar(4000),
    DUEDATE_ timestamp(3) NULL,
    REPEAT_ varchar(255),
    HANDLER_TYPE_ varchar(255),
    HANDLER_CFG_ varchar(4000),
    CUSTOM_VALUES_ID_ varchar(64),
    CREATE_TIME_ timestamp(3) NULL,
    TENANT_ID_ varchar(255) default '',
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;
CREATE TABLE IF NOT EXISTS ACT_RU_HISTORY_JOB (
    ID_ varchar(64) NOT NULL,
    REV_ integer,
    LOCK_EXP_TIME_ timestamp(3) NULL,
    LOCK_OWNER_ varchar(255),
    RETRIES_ integer,
    EXCEPTION_STACK_ID_ varchar(64),
    EXCEPTION_MSG_ varchar(4000),
    HANDLER_TYPE_ varchar(255),
    HANDLER_CFG_ varchar(4000),
    CUSTOM_VALUES_ID_ varchar(64),
    ADV_HANDLER_CFG_ID_ varchar(64),
    CREATE_TIME_ timestamp(3) NULL,
    SCOPE_TYPE_ varchar(255),
    TENANT_ID_ varchar(255) default '',
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;
CREATE TABLE IF NOT EXISTS ACT_RU_EXTERNAL_JOB (
    ID_ varchar(64) NOT NULL,
    REV_ integer,
    CATEGORY_ varchar(255),
    TYPE_ varchar(255) NOT NULL,
    LOCK_EXP_TIME_ timestamp(3) NULL,
    LOCK_OWNER_ varchar(255),
    EXCLUSIVE_ boolean,
    EXECUTION_ID_ varchar(64),
    PROCESS_INSTANCE_ID_ varchar(64),
    PROC_DEF_ID_ varchar(64),
    ELEMENT_ID_ varchar(255),
    ELEMENT_NAME_ varchar(255),
    SCOPE_ID_ varchar(255),
    SUB_SCOPE_ID_ varchar(255),
    SCOPE_TYPE_ varchar(255),
    SCOPE_DEFINITION_ID_ varchar(255),
    CORRELATION_ID_ varchar(255),
    RETRIES_ integer,
    EXCEPTION_STACK_ID_ varchar(64),
    EXCEPTION_MSG_ varchar(4000),
    DUEDATE_ timestamp(3) NULL,
    REPEAT_ varchar(255),
    HANDLER_TYPE_ varchar(255),
    HANDLER_CFG_ varchar(4000),
    CUSTOM_VALUES_ID_ varchar(64),
    CREATE_TIME_ timestamp(3) NULL,
    TENANT_ID_ varchar(255) default '',
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;
-- 索引 ACT_IDX_JOB_EXCEPTION_STACK_ID（幂等）
SET @v218_idx_act_idx_job_exception_stack_id := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_RU_JOB'
      AND index_name = 'ACT_IDX_JOB_EXCEPTION_STACK_ID'
);
SET @v218_sql := IF(
    @v218_idx_act_idx_job_exception_stack_id = 0,
    'CREATE INDEX `ACT_IDX_JOB_EXCEPTION_STACK_ID` ON `ACT_RU_JOB` (EXCEPTION_STACK_ID_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 索引 ACT_IDX_JOB_CUSTOM_VALUES_ID（幂等）
SET @v218_idx_act_idx_job_custom_values_id := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_RU_JOB'
      AND index_name = 'ACT_IDX_JOB_CUSTOM_VALUES_ID'
);
SET @v218_sql := IF(
    @v218_idx_act_idx_job_custom_values_id = 0,
    'CREATE INDEX `ACT_IDX_JOB_CUSTOM_VALUES_ID` ON `ACT_RU_JOB` (CUSTOM_VALUES_ID_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 索引 ACT_IDX_JOB_CORRELATION_ID（幂等）
SET @v218_idx_act_idx_job_correlation_id := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_RU_JOB'
      AND index_name = 'ACT_IDX_JOB_CORRELATION_ID'
);
SET @v218_sql := IF(
    @v218_idx_act_idx_job_correlation_id = 0,
    'CREATE INDEX `ACT_IDX_JOB_CORRELATION_ID` ON `ACT_RU_JOB` (CORRELATION_ID_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 索引 ACT_IDX_TIMER_JOB_EXCEPTION_STACK_ID（幂等）
SET @v218_idx_act_idx_timer_job_exception_stack_id := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_RU_TIMER_JOB'
      AND index_name = 'ACT_IDX_TIMER_JOB_EXCEPTION_STACK_ID'
);
SET @v218_sql := IF(
    @v218_idx_act_idx_timer_job_exception_stack_id = 0,
    'CREATE INDEX `ACT_IDX_TIMER_JOB_EXCEPTION_STACK_ID` ON `ACT_RU_TIMER_JOB` (EXCEPTION_STACK_ID_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 索引 ACT_IDX_TIMER_JOB_CUSTOM_VALUES_ID（幂等）
SET @v218_idx_act_idx_timer_job_custom_values_id := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_RU_TIMER_JOB'
      AND index_name = 'ACT_IDX_TIMER_JOB_CUSTOM_VALUES_ID'
);
SET @v218_sql := IF(
    @v218_idx_act_idx_timer_job_custom_values_id = 0,
    'CREATE INDEX `ACT_IDX_TIMER_JOB_CUSTOM_VALUES_ID` ON `ACT_RU_TIMER_JOB` (CUSTOM_VALUES_ID_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 索引 ACT_IDX_TIMER_JOB_CORRELATION_ID（幂等）
SET @v218_idx_act_idx_timer_job_correlation_id := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_RU_TIMER_JOB'
      AND index_name = 'ACT_IDX_TIMER_JOB_CORRELATION_ID'
);
SET @v218_sql := IF(
    @v218_idx_act_idx_timer_job_correlation_id = 0,
    'CREATE INDEX `ACT_IDX_TIMER_JOB_CORRELATION_ID` ON `ACT_RU_TIMER_JOB` (CORRELATION_ID_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 索引 ACT_IDX_TIMER_JOB_DUEDATE（幂等）
SET @v218_idx_act_idx_timer_job_duedate := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_RU_TIMER_JOB'
      AND index_name = 'ACT_IDX_TIMER_JOB_DUEDATE'
);
SET @v218_sql := IF(
    @v218_idx_act_idx_timer_job_duedate = 0,
    'CREATE INDEX `ACT_IDX_TIMER_JOB_DUEDATE` ON `ACT_RU_TIMER_JOB` (DUEDATE_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 索引 ACT_IDX_SUSPENDED_JOB_EXCEPTION_STACK_ID（幂等）
SET @v218_idx_act_idx_suspended_job_exception_stack_id := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_RU_SUSPENDED_JOB'
      AND index_name = 'ACT_IDX_SUSPENDED_JOB_EXCEPTION_STACK_ID'
);
SET @v218_sql := IF(
    @v218_idx_act_idx_suspended_job_exception_stack_id = 0,
    'CREATE INDEX `ACT_IDX_SUSPENDED_JOB_EXCEPTION_STACK_ID` ON `ACT_RU_SUSPENDED_JOB` (EXCEPTION_STACK_ID_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 索引 ACT_IDX_SUSPENDED_JOB_CUSTOM_VALUES_ID（幂等）
SET @v218_idx_act_idx_suspended_job_custom_values_id := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_RU_SUSPENDED_JOB'
      AND index_name = 'ACT_IDX_SUSPENDED_JOB_CUSTOM_VALUES_ID'
);
SET @v218_sql := IF(
    @v218_idx_act_idx_suspended_job_custom_values_id = 0,
    'CREATE INDEX `ACT_IDX_SUSPENDED_JOB_CUSTOM_VALUES_ID` ON `ACT_RU_SUSPENDED_JOB` (CUSTOM_VALUES_ID_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 索引 ACT_IDX_SUSPENDED_JOB_CORRELATION_ID（幂等）
SET @v218_idx_act_idx_suspended_job_correlation_id := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_RU_SUSPENDED_JOB'
      AND index_name = 'ACT_IDX_SUSPENDED_JOB_CORRELATION_ID'
);
SET @v218_sql := IF(
    @v218_idx_act_idx_suspended_job_correlation_id = 0,
    'CREATE INDEX `ACT_IDX_SUSPENDED_JOB_CORRELATION_ID` ON `ACT_RU_SUSPENDED_JOB` (CORRELATION_ID_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 索引 ACT_IDX_DEADLETTER_JOB_EXCEPTION_STACK_ID（幂等）
SET @v218_idx_act_idx_deadletter_job_exception_stack_id := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_RU_DEADLETTER_JOB'
      AND index_name = 'ACT_IDX_DEADLETTER_JOB_EXCEPTION_STACK_ID'
);
SET @v218_sql := IF(
    @v218_idx_act_idx_deadletter_job_exception_stack_id = 0,
    'CREATE INDEX `ACT_IDX_DEADLETTER_JOB_EXCEPTION_STACK_ID` ON `ACT_RU_DEADLETTER_JOB` (EXCEPTION_STACK_ID_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 索引 ACT_IDX_DEADLETTER_JOB_CUSTOM_VALUES_ID（幂等）
SET @v218_idx_act_idx_deadletter_job_custom_values_id := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_RU_DEADLETTER_JOB'
      AND index_name = 'ACT_IDX_DEADLETTER_JOB_CUSTOM_VALUES_ID'
);
SET @v218_sql := IF(
    @v218_idx_act_idx_deadletter_job_custom_values_id = 0,
    'CREATE INDEX `ACT_IDX_DEADLETTER_JOB_CUSTOM_VALUES_ID` ON `ACT_RU_DEADLETTER_JOB` (CUSTOM_VALUES_ID_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 索引 ACT_IDX_DEADLETTER_JOB_CORRELATION_ID（幂等）
SET @v218_idx_act_idx_deadletter_job_correlation_id := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_RU_DEADLETTER_JOB'
      AND index_name = 'ACT_IDX_DEADLETTER_JOB_CORRELATION_ID'
);
SET @v218_sql := IF(
    @v218_idx_act_idx_deadletter_job_correlation_id = 0,
    'CREATE INDEX `ACT_IDX_DEADLETTER_JOB_CORRELATION_ID` ON `ACT_RU_DEADLETTER_JOB` (CORRELATION_ID_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 索引 ACT_IDX_EXTERNAL_JOB_EXCEPTION_STACK_ID（幂等）
SET @v218_idx_act_idx_external_job_exception_stack_id := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_RU_EXTERNAL_JOB'
      AND index_name = 'ACT_IDX_EXTERNAL_JOB_EXCEPTION_STACK_ID'
);
SET @v218_sql := IF(
    @v218_idx_act_idx_external_job_exception_stack_id = 0,
    'CREATE INDEX `ACT_IDX_EXTERNAL_JOB_EXCEPTION_STACK_ID` ON `ACT_RU_EXTERNAL_JOB` (EXCEPTION_STACK_ID_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 索引 ACT_IDX_EXTERNAL_JOB_CUSTOM_VALUES_ID（幂等）
SET @v218_idx_act_idx_external_job_custom_values_id := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_RU_EXTERNAL_JOB'
      AND index_name = 'ACT_IDX_EXTERNAL_JOB_CUSTOM_VALUES_ID'
);
SET @v218_sql := IF(
    @v218_idx_act_idx_external_job_custom_values_id = 0,
    'CREATE INDEX `ACT_IDX_EXTERNAL_JOB_CUSTOM_VALUES_ID` ON `ACT_RU_EXTERNAL_JOB` (CUSTOM_VALUES_ID_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 索引 ACT_IDX_EXTERNAL_JOB_CORRELATION_ID（幂等）
SET @v218_idx_act_idx_external_job_correlation_id := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_RU_EXTERNAL_JOB'
      AND index_name = 'ACT_IDX_EXTERNAL_JOB_CORRELATION_ID'
);
SET @v218_sql := IF(
    @v218_idx_act_idx_external_job_correlation_id = 0,
    'CREATE INDEX `ACT_IDX_EXTERNAL_JOB_CORRELATION_ID` ON `ACT_RU_EXTERNAL_JOB` (CORRELATION_ID_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 外键 ACT_FK_JOB_EXCEPTION（幂等）
SET @v218_fk_act_fk_job_exception := (
    SELECT COUNT(*) FROM information_schema.table_constraints
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_RU_JOB'
      AND constraint_name = 'ACT_FK_JOB_EXCEPTION'
);
SET @v218_sql := IF(
    @v218_fk_act_fk_job_exception = 0,
    'ALTER TABLE `ACT_RU_JOB` ADD CONSTRAINT `ACT_FK_JOB_EXCEPTION` foreign key (EXCEPTION_STACK_ID_)
    references ACT_GE_BYTEARRAY (ID_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 外键 ACT_FK_JOB_CUSTOM_VALUES（幂等）
SET @v218_fk_act_fk_job_custom_values := (
    SELECT COUNT(*) FROM information_schema.table_constraints
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_RU_JOB'
      AND constraint_name = 'ACT_FK_JOB_CUSTOM_VALUES'
);
SET @v218_sql := IF(
    @v218_fk_act_fk_job_custom_values = 0,
    'ALTER TABLE `ACT_RU_JOB` ADD CONSTRAINT `ACT_FK_JOB_CUSTOM_VALUES` foreign key (CUSTOM_VALUES_ID_)
    references ACT_GE_BYTEARRAY (ID_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 外键 ACT_FK_TIMER_JOB_EXCEPTION（幂等）
SET @v218_fk_act_fk_timer_job_exception := (
    SELECT COUNT(*) FROM information_schema.table_constraints
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_RU_TIMER_JOB'
      AND constraint_name = 'ACT_FK_TIMER_JOB_EXCEPTION'
);
SET @v218_sql := IF(
    @v218_fk_act_fk_timer_job_exception = 0,
    'ALTER TABLE `ACT_RU_TIMER_JOB` ADD CONSTRAINT `ACT_FK_TIMER_JOB_EXCEPTION` foreign key (EXCEPTION_STACK_ID_)
    references ACT_GE_BYTEARRAY (ID_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 外键 ACT_FK_TIMER_JOB_CUSTOM_VALUES（幂等）
SET @v218_fk_act_fk_timer_job_custom_values := (
    SELECT COUNT(*) FROM information_schema.table_constraints
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_RU_TIMER_JOB'
      AND constraint_name = 'ACT_FK_TIMER_JOB_CUSTOM_VALUES'
);
SET @v218_sql := IF(
    @v218_fk_act_fk_timer_job_custom_values = 0,
    'ALTER TABLE `ACT_RU_TIMER_JOB` ADD CONSTRAINT `ACT_FK_TIMER_JOB_CUSTOM_VALUES` foreign key (CUSTOM_VALUES_ID_)
    references ACT_GE_BYTEARRAY (ID_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 外键 ACT_FK_SUSPENDED_JOB_EXCEPTION（幂等）
SET @v218_fk_act_fk_suspended_job_exception := (
    SELECT COUNT(*) FROM information_schema.table_constraints
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_RU_SUSPENDED_JOB'
      AND constraint_name = 'ACT_FK_SUSPENDED_JOB_EXCEPTION'
);
SET @v218_sql := IF(
    @v218_fk_act_fk_suspended_job_exception = 0,
    'ALTER TABLE `ACT_RU_SUSPENDED_JOB` ADD CONSTRAINT `ACT_FK_SUSPENDED_JOB_EXCEPTION` foreign key (EXCEPTION_STACK_ID_)
    references ACT_GE_BYTEARRAY (ID_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 外键 ACT_FK_SUSPENDED_JOB_CUSTOM_VALUES（幂等）
SET @v218_fk_act_fk_suspended_job_custom_values := (
    SELECT COUNT(*) FROM information_schema.table_constraints
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_RU_SUSPENDED_JOB'
      AND constraint_name = 'ACT_FK_SUSPENDED_JOB_CUSTOM_VALUES'
);
SET @v218_sql := IF(
    @v218_fk_act_fk_suspended_job_custom_values = 0,
    'ALTER TABLE `ACT_RU_SUSPENDED_JOB` ADD CONSTRAINT `ACT_FK_SUSPENDED_JOB_CUSTOM_VALUES` foreign key (CUSTOM_VALUES_ID_)
    references ACT_GE_BYTEARRAY (ID_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 外键 ACT_FK_DEADLETTER_JOB_EXCEPTION（幂等）
SET @v218_fk_act_fk_deadletter_job_exception := (
    SELECT COUNT(*) FROM information_schema.table_constraints
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_RU_DEADLETTER_JOB'
      AND constraint_name = 'ACT_FK_DEADLETTER_JOB_EXCEPTION'
);
SET @v218_sql := IF(
    @v218_fk_act_fk_deadletter_job_exception = 0,
    'ALTER TABLE `ACT_RU_DEADLETTER_JOB` ADD CONSTRAINT `ACT_FK_DEADLETTER_JOB_EXCEPTION` foreign key (EXCEPTION_STACK_ID_)
    references ACT_GE_BYTEARRAY (ID_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 外键 ACT_FK_DEADLETTER_JOB_CUSTOM_VALUES（幂等）
SET @v218_fk_act_fk_deadletter_job_custom_values := (
    SELECT COUNT(*) FROM information_schema.table_constraints
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_RU_DEADLETTER_JOB'
      AND constraint_name = 'ACT_FK_DEADLETTER_JOB_CUSTOM_VALUES'
);
SET @v218_sql := IF(
    @v218_fk_act_fk_deadletter_job_custom_values = 0,
    'ALTER TABLE `ACT_RU_DEADLETTER_JOB` ADD CONSTRAINT `ACT_FK_DEADLETTER_JOB_CUSTOM_VALUES` foreign key (CUSTOM_VALUES_ID_)
    references ACT_GE_BYTEARRAY (ID_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 外键 ACT_FK_EXTERNAL_JOB_EXCEPTION（幂等）
SET @v218_fk_act_fk_external_job_exception := (
    SELECT COUNT(*) FROM information_schema.table_constraints
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_RU_EXTERNAL_JOB'
      AND constraint_name = 'ACT_FK_EXTERNAL_JOB_EXCEPTION'
);
SET @v218_sql := IF(
    @v218_fk_act_fk_external_job_exception = 0,
    'ALTER TABLE `ACT_RU_EXTERNAL_JOB` ADD CONSTRAINT `ACT_FK_EXTERNAL_JOB_EXCEPTION` foreign key (EXCEPTION_STACK_ID_)
    references ACT_GE_BYTEARRAY (ID_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 外键 ACT_FK_EXTERNAL_JOB_CUSTOM_VALUES（幂等）
SET @v218_fk_act_fk_external_job_custom_values := (
    SELECT COUNT(*) FROM information_schema.table_constraints
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_RU_EXTERNAL_JOB'
      AND constraint_name = 'ACT_FK_EXTERNAL_JOB_CUSTOM_VALUES'
);
SET @v218_sql := IF(
    @v218_fk_act_fk_external_job_custom_values = 0,
    'ALTER TABLE `ACT_RU_EXTERNAL_JOB` ADD CONSTRAINT `ACT_FK_EXTERNAL_JOB_CUSTOM_VALUES` foreign key (CUSTOM_VALUES_ID_)
    references ACT_GE_BYTEARRAY (ID_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 索引 ACT_IDX_JOB_SCOPE（幂等）
SET @v218_idx_act_idx_job_scope := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_RU_JOB'
      AND index_name = 'ACT_IDX_JOB_SCOPE'
);
SET @v218_sql := IF(
    @v218_idx_act_idx_job_scope = 0,
    'CREATE INDEX `ACT_IDX_JOB_SCOPE` ON `ACT_RU_JOB` (SCOPE_ID_, SCOPE_TYPE_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 索引 ACT_IDX_JOB_SUB_SCOPE（幂等）
SET @v218_idx_act_idx_job_sub_scope := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_RU_JOB'
      AND index_name = 'ACT_IDX_JOB_SUB_SCOPE'
);
SET @v218_sql := IF(
    @v218_idx_act_idx_job_sub_scope = 0,
    'CREATE INDEX `ACT_IDX_JOB_SUB_SCOPE` ON `ACT_RU_JOB` (SUB_SCOPE_ID_, SCOPE_TYPE_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 索引 ACT_IDX_JOB_SCOPE_DEF（幂等）
SET @v218_idx_act_idx_job_scope_def := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_RU_JOB'
      AND index_name = 'ACT_IDX_JOB_SCOPE_DEF'
);
SET @v218_sql := IF(
    @v218_idx_act_idx_job_scope_def = 0,
    'CREATE INDEX `ACT_IDX_JOB_SCOPE_DEF` ON `ACT_RU_JOB` (SCOPE_DEFINITION_ID_, SCOPE_TYPE_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 索引 ACT_IDX_TJOB_SCOPE（幂等）
SET @v218_idx_act_idx_tjob_scope := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_RU_TIMER_JOB'
      AND index_name = 'ACT_IDX_TJOB_SCOPE'
);
SET @v218_sql := IF(
    @v218_idx_act_idx_tjob_scope = 0,
    'CREATE INDEX `ACT_IDX_TJOB_SCOPE` ON `ACT_RU_TIMER_JOB` (SCOPE_ID_, SCOPE_TYPE_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 索引 ACT_IDX_TJOB_SUB_SCOPE（幂等）
SET @v218_idx_act_idx_tjob_sub_scope := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_RU_TIMER_JOB'
      AND index_name = 'ACT_IDX_TJOB_SUB_SCOPE'
);
SET @v218_sql := IF(
    @v218_idx_act_idx_tjob_sub_scope = 0,
    'CREATE INDEX `ACT_IDX_TJOB_SUB_SCOPE` ON `ACT_RU_TIMER_JOB` (SUB_SCOPE_ID_, SCOPE_TYPE_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 索引 ACT_IDX_TJOB_SCOPE_DEF（幂等）
SET @v218_idx_act_idx_tjob_scope_def := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_RU_TIMER_JOB'
      AND index_name = 'ACT_IDX_TJOB_SCOPE_DEF'
);
SET @v218_sql := IF(
    @v218_idx_act_idx_tjob_scope_def = 0,
    'CREATE INDEX `ACT_IDX_TJOB_SCOPE_DEF` ON `ACT_RU_TIMER_JOB` (SCOPE_DEFINITION_ID_, SCOPE_TYPE_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 索引 ACT_IDX_SJOB_SCOPE（幂等）
SET @v218_idx_act_idx_sjob_scope := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_RU_SUSPENDED_JOB'
      AND index_name = 'ACT_IDX_SJOB_SCOPE'
);
SET @v218_sql := IF(
    @v218_idx_act_idx_sjob_scope = 0,
    'CREATE INDEX `ACT_IDX_SJOB_SCOPE` ON `ACT_RU_SUSPENDED_JOB` (SCOPE_ID_, SCOPE_TYPE_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 索引 ACT_IDX_SJOB_SUB_SCOPE（幂等）
SET @v218_idx_act_idx_sjob_sub_scope := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_RU_SUSPENDED_JOB'
      AND index_name = 'ACT_IDX_SJOB_SUB_SCOPE'
);
SET @v218_sql := IF(
    @v218_idx_act_idx_sjob_sub_scope = 0,
    'CREATE INDEX `ACT_IDX_SJOB_SUB_SCOPE` ON `ACT_RU_SUSPENDED_JOB` (SUB_SCOPE_ID_, SCOPE_TYPE_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 索引 ACT_IDX_SJOB_SCOPE_DEF（幂等）
SET @v218_idx_act_idx_sjob_scope_def := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_RU_SUSPENDED_JOB'
      AND index_name = 'ACT_IDX_SJOB_SCOPE_DEF'
);
SET @v218_sql := IF(
    @v218_idx_act_idx_sjob_scope_def = 0,
    'CREATE INDEX `ACT_IDX_SJOB_SCOPE_DEF` ON `ACT_RU_SUSPENDED_JOB` (SCOPE_DEFINITION_ID_, SCOPE_TYPE_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 索引 ACT_IDX_DJOB_SCOPE（幂等）
SET @v218_idx_act_idx_djob_scope := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_RU_DEADLETTER_JOB'
      AND index_name = 'ACT_IDX_DJOB_SCOPE'
);
SET @v218_sql := IF(
    @v218_idx_act_idx_djob_scope = 0,
    'CREATE INDEX `ACT_IDX_DJOB_SCOPE` ON `ACT_RU_DEADLETTER_JOB` (SCOPE_ID_, SCOPE_TYPE_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 索引 ACT_IDX_DJOB_SUB_SCOPE（幂等）
SET @v218_idx_act_idx_djob_sub_scope := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_RU_DEADLETTER_JOB'
      AND index_name = 'ACT_IDX_DJOB_SUB_SCOPE'
);
SET @v218_sql := IF(
    @v218_idx_act_idx_djob_sub_scope = 0,
    'CREATE INDEX `ACT_IDX_DJOB_SUB_SCOPE` ON `ACT_RU_DEADLETTER_JOB` (SUB_SCOPE_ID_, SCOPE_TYPE_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 索引 ACT_IDX_DJOB_SCOPE_DEF（幂等）
SET @v218_idx_act_idx_djob_scope_def := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_RU_DEADLETTER_JOB'
      AND index_name = 'ACT_IDX_DJOB_SCOPE_DEF'
);
SET @v218_sql := IF(
    @v218_idx_act_idx_djob_scope_def = 0,
    'CREATE INDEX `ACT_IDX_DJOB_SCOPE_DEF` ON `ACT_RU_DEADLETTER_JOB` (SCOPE_DEFINITION_ID_, SCOPE_TYPE_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 索引 ACT_IDX_EJOB_SCOPE（幂等）
SET @v218_idx_act_idx_ejob_scope := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_RU_EXTERNAL_JOB'
      AND index_name = 'ACT_IDX_EJOB_SCOPE'
);
SET @v218_sql := IF(
    @v218_idx_act_idx_ejob_scope = 0,
    'CREATE INDEX `ACT_IDX_EJOB_SCOPE` ON `ACT_RU_EXTERNAL_JOB` (SCOPE_ID_, SCOPE_TYPE_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 索引 ACT_IDX_EJOB_SUB_SCOPE（幂等）
SET @v218_idx_act_idx_ejob_sub_scope := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_RU_EXTERNAL_JOB'
      AND index_name = 'ACT_IDX_EJOB_SUB_SCOPE'
);
SET @v218_sql := IF(
    @v218_idx_act_idx_ejob_sub_scope = 0,
    'CREATE INDEX `ACT_IDX_EJOB_SUB_SCOPE` ON `ACT_RU_EXTERNAL_JOB` (SUB_SCOPE_ID_, SCOPE_TYPE_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 索引 ACT_IDX_EJOB_SCOPE_DEF（幂等）
SET @v218_idx_act_idx_ejob_scope_def := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_RU_EXTERNAL_JOB'
      AND index_name = 'ACT_IDX_EJOB_SCOPE_DEF'
);
SET @v218_sql := IF(
    @v218_idx_act_idx_ejob_scope_def = 0,
    'CREATE INDEX `ACT_IDX_EJOB_SCOPE_DEF` ON `ACT_RU_EXTERNAL_JOB` (SCOPE_DEFINITION_ID_, SCOPE_TYPE_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
CREATE TABLE IF NOT EXISTS FLW_RU_BATCH (
    ID_ varchar(64) not null,
    REV_ integer,
    TYPE_ varchar(64) not null,
    SEARCH_KEY_ varchar(255),
    SEARCH_KEY2_ varchar(255),
    CREATE_TIME_ datetime(3) not null,
    COMPLETE_TIME_ datetime(3),
    STATUS_ varchar(255),
    BATCH_DOC_ID_ varchar(64),
    TENANT_ID_ varchar(255) default '',
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;
CREATE TABLE IF NOT EXISTS FLW_RU_BATCH_PART (
    ID_ varchar(64) not null,
    REV_ integer,
    BATCH_ID_ varchar(64),
    TYPE_ varchar(64) not null,
    SCOPE_ID_ varchar(64),
    SUB_SCOPE_ID_ varchar(64),
    SCOPE_TYPE_ varchar(64),
    SEARCH_KEY_ varchar(255),
    SEARCH_KEY2_ varchar(255),
    CREATE_TIME_ datetime(3) not null,
    COMPLETE_TIME_ datetime(3),
    STATUS_ varchar(255),
    RESULT_DOC_ID_ varchar(64),
    TENANT_ID_ varchar(255) default '',
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;
-- 索引 FLW_IDX_BATCH_PART（幂等）
SET @v218_idx_flw_idx_batch_part := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'FLW_RU_BATCH_PART'
      AND index_name = 'FLW_IDX_BATCH_PART'
);
SET @v218_sql := IF(
    @v218_idx_flw_idx_batch_part = 0,
    'CREATE INDEX `FLW_IDX_BATCH_PART` ON `FLW_RU_BATCH_PART` (BATCH_ID_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 外键 FLW_FK_BATCH_PART_PARENT（幂等）
SET @v218_fk_flw_fk_batch_part_parent := (
    SELECT COUNT(*) FROM information_schema.table_constraints
    WHERE table_schema = DATABASE()
      AND table_name = 'FLW_RU_BATCH_PART'
      AND constraint_name = 'FLW_FK_BATCH_PART_PARENT'
);
SET @v218_sql := IF(
    @v218_fk_flw_fk_batch_part_parent = 0,
    'ALTER TABLE `FLW_RU_BATCH_PART` ADD CONSTRAINT `FLW_FK_BATCH_PART_PARENT` foreign key (BATCH_ID_)
    references FLW_RU_BATCH (ID_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
CREATE TABLE IF NOT EXISTS ACT_RU_TASK (
    ID_ varchar(64),
    REV_ integer,
    EXECUTION_ID_ varchar(64),
    PROC_INST_ID_ varchar(64),
    PROC_DEF_ID_ varchar(64),
    TASK_DEF_ID_ varchar(64),
    SCOPE_ID_ varchar(255),
    SUB_SCOPE_ID_ varchar(255),
    SCOPE_TYPE_ varchar(255),
    SCOPE_DEFINITION_ID_ varchar(255),
    PROPAGATED_STAGE_INST_ID_ varchar(255),
    STATE_ varchar(255),
    NAME_ varchar(255),
    PARENT_TASK_ID_ varchar(64),
    DESCRIPTION_ varchar(4000),
    TASK_DEF_KEY_ varchar(255),
    OWNER_ varchar(255),
    ASSIGNEE_ varchar(255),
    DELEGATION_ varchar(64),
    PRIORITY_ integer,
    CREATE_TIME_ timestamp(3) NULL,
    IN_PROGRESS_TIME_ datetime(3),
    IN_PROGRESS_STARTED_BY_ varchar(255),
    CLAIM_TIME_ datetime(3),
    CLAIMED_BY_ varchar(255),
    SUSPENDED_TIME_ datetime(3),
    SUSPENDED_BY_ varchar(255),
    IN_PROGRESS_DUE_DATE_ datetime(3),
    DUE_DATE_ datetime(3),
    CATEGORY_ varchar(255),
    SUSPENSION_STATE_ integer,
    TENANT_ID_ varchar(255) default '',
    FORM_KEY_ varchar(255),
    IS_COUNT_ENABLED_ TINYINT,
    VAR_COUNT_ integer,
    ID_LINK_COUNT_ integer,
    SUB_TASK_COUNT_ integer,
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;
-- 索引 ACT_IDX_TASK_CREATE（幂等）
SET @v218_idx_act_idx_task_create := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_RU_TASK'
      AND index_name = 'ACT_IDX_TASK_CREATE'
);
SET @v218_sql := IF(
    @v218_idx_act_idx_task_create = 0,
    'CREATE INDEX `ACT_IDX_TASK_CREATE` ON `ACT_RU_TASK` (CREATE_TIME_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 索引 ACT_IDX_TASK_SCOPE（幂等）
SET @v218_idx_act_idx_task_scope := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_RU_TASK'
      AND index_name = 'ACT_IDX_TASK_SCOPE'
);
SET @v218_sql := IF(
    @v218_idx_act_idx_task_scope = 0,
    'CREATE INDEX `ACT_IDX_TASK_SCOPE` ON `ACT_RU_TASK` (SCOPE_ID_, SCOPE_TYPE_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 索引 ACT_IDX_TASK_SUB_SCOPE（幂等）
SET @v218_idx_act_idx_task_sub_scope := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_RU_TASK'
      AND index_name = 'ACT_IDX_TASK_SUB_SCOPE'
);
SET @v218_sql := IF(
    @v218_idx_act_idx_task_sub_scope = 0,
    'CREATE INDEX `ACT_IDX_TASK_SUB_SCOPE` ON `ACT_RU_TASK` (SUB_SCOPE_ID_, SCOPE_TYPE_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 索引 ACT_IDX_TASK_SCOPE_DEF（幂等）
SET @v218_idx_act_idx_task_scope_def := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_RU_TASK'
      AND index_name = 'ACT_IDX_TASK_SCOPE_DEF'
);
SET @v218_sql := IF(
    @v218_idx_act_idx_task_scope_def = 0,
    'CREATE INDEX `ACT_IDX_TASK_SCOPE_DEF` ON `ACT_RU_TASK` (SCOPE_DEFINITION_ID_, SCOPE_TYPE_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
CREATE TABLE IF NOT EXISTS ACT_HI_TASKINST (
    ID_ varchar(64) not null,
    REV_ integer default 1,
    PROC_DEF_ID_ varchar(64),
    TASK_DEF_ID_ varchar(64),
    TASK_DEF_KEY_ varchar(255),
    PROC_INST_ID_ varchar(64),
    EXECUTION_ID_ varchar(64),
    SCOPE_ID_ varchar(255),
    SUB_SCOPE_ID_ varchar(255),
    SCOPE_TYPE_ varchar(255),
    SCOPE_DEFINITION_ID_ varchar(255),
    PROPAGATED_STAGE_INST_ID_ varchar(255),
    STATE_ varchar(255),
    NAME_ varchar(255),
    PARENT_TASK_ID_ varchar(64),
    DESCRIPTION_ varchar(4000),
    OWNER_ varchar(255),
    ASSIGNEE_ varchar(255),
    START_TIME_ datetime(3) not null,
    IN_PROGRESS_TIME_ datetime(3),
    IN_PROGRESS_STARTED_BY_ varchar(255),
    CLAIM_TIME_ datetime(3),
    CLAIMED_BY_ varchar(255),
    SUSPENDED_TIME_ datetime(3),
    SUSPENDED_BY_ varchar(255),
    END_TIME_ datetime(3),
    COMPLETED_BY_ varchar(255),
    DURATION_ bigint,
    DELETE_REASON_ varchar(4000),
    PRIORITY_ integer,
    IN_PROGRESS_DUE_DATE_ datetime(3),
    DUE_DATE_ datetime(3),
    FORM_KEY_ varchar(255),
    CATEGORY_ varchar(255),
    TENANT_ID_ varchar(255) default '',
    LAST_UPDATED_TIME_ datetime(3),
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;
CREATE TABLE IF NOT EXISTS ACT_HI_TSK_LOG (
    ID_ bigint auto_increment,
    TYPE_ varchar(64),
    TASK_ID_ varchar(64) not null,
    TIME_STAMP_ timestamp(3) not null,
    USER_ID_ varchar(255),
    DATA_ varchar(4000),
    EXECUTION_ID_ varchar(64),
    PROC_INST_ID_ varchar(64),
    PROC_DEF_ID_ varchar(64),
    SCOPE_ID_ varchar(255),
    SCOPE_DEFINITION_ID_ varchar(255),
    SUB_SCOPE_ID_ varchar(255),
    SCOPE_TYPE_ varchar(255),
    TENANT_ID_ varchar(255) default '',
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;
-- 索引 ACT_IDX_HI_TASK_SCOPE（幂等）
SET @v218_idx_act_idx_hi_task_scope := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_HI_TASKINST'
      AND index_name = 'ACT_IDX_HI_TASK_SCOPE'
);
SET @v218_sql := IF(
    @v218_idx_act_idx_hi_task_scope = 0,
    'CREATE INDEX `ACT_IDX_HI_TASK_SCOPE` ON `ACT_HI_TASKINST` (SCOPE_ID_, SCOPE_TYPE_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 索引 ACT_IDX_HI_TASK_SUB_SCOPE（幂等）
SET @v218_idx_act_idx_hi_task_sub_scope := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_HI_TASKINST'
      AND index_name = 'ACT_IDX_HI_TASK_SUB_SCOPE'
);
SET @v218_sql := IF(
    @v218_idx_act_idx_hi_task_sub_scope = 0,
    'CREATE INDEX `ACT_IDX_HI_TASK_SUB_SCOPE` ON `ACT_HI_TASKINST` (SUB_SCOPE_ID_, SCOPE_TYPE_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 索引 ACT_IDX_HI_TASK_SCOPE_DEF（幂等）
SET @v218_idx_act_idx_hi_task_scope_def := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_HI_TASKINST'
      AND index_name = 'ACT_IDX_HI_TASK_SCOPE_DEF'
);
SET @v218_sql := IF(
    @v218_idx_act_idx_hi_task_scope_def = 0,
    'CREATE INDEX `ACT_IDX_HI_TASK_SCOPE_DEF` ON `ACT_HI_TASKINST` (SCOPE_DEFINITION_ID_, SCOPE_TYPE_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 索引 ACT_IDX_ACT_HI_TSK_LOG_TASK（幂等）
SET @v218_idx_act_idx_act_hi_tsk_log_task := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_HI_TSK_LOG'
      AND index_name = 'ACT_IDX_ACT_HI_TSK_LOG_TASK'
);
SET @v218_sql := IF(
    @v218_idx_act_idx_act_hi_tsk_log_task = 0,
    'CREATE INDEX `ACT_IDX_ACT_HI_TSK_LOG_TASK` ON `ACT_HI_TSK_LOG` (TASK_ID_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
CREATE TABLE IF NOT EXISTS ACT_RU_VARIABLE (
    ID_ varchar(64) not null,
    REV_ integer,
    TYPE_ varchar(255) not null,
    NAME_ varchar(255) not null,
    EXECUTION_ID_ varchar(64),
    PROC_INST_ID_ varchar(64),
    TASK_ID_ varchar(64),
    SCOPE_ID_ varchar(255),
    SUB_SCOPE_ID_ varchar(255),
    SCOPE_TYPE_ varchar(255),
    BYTEARRAY_ID_ varchar(64),
    DOUBLE_ double,
    LONG_ bigint,
    TEXT_ varchar(4000),
    TEXT2_ varchar(4000),
    META_INFO_ varchar(4000),
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;
-- 索引 ACT_IDX_RU_VAR_SCOPE_ID_TYPE（幂等）
SET @v218_idx_act_idx_ru_var_scope_id_type := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_RU_VARIABLE'
      AND index_name = 'ACT_IDX_RU_VAR_SCOPE_ID_TYPE'
);
SET @v218_sql := IF(
    @v218_idx_act_idx_ru_var_scope_id_type = 0,
    'CREATE INDEX `ACT_IDX_RU_VAR_SCOPE_ID_TYPE` ON `ACT_RU_VARIABLE` (SCOPE_ID_, SCOPE_TYPE_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 索引 ACT_IDX_RU_VAR_SUB_ID_TYPE（幂等）
SET @v218_idx_act_idx_ru_var_sub_id_type := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_RU_VARIABLE'
      AND index_name = 'ACT_IDX_RU_VAR_SUB_ID_TYPE'
);
SET @v218_sql := IF(
    @v218_idx_act_idx_ru_var_sub_id_type = 0,
    'CREATE INDEX `ACT_IDX_RU_VAR_SUB_ID_TYPE` ON `ACT_RU_VARIABLE` (SUB_SCOPE_ID_, SCOPE_TYPE_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 外键 ACT_FK_VAR_BYTEARRAY（幂等）
SET @v218_fk_act_fk_var_bytearray := (
    SELECT COUNT(*) FROM information_schema.table_constraints
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_RU_VARIABLE'
      AND constraint_name = 'ACT_FK_VAR_BYTEARRAY'
);
SET @v218_sql := IF(
    @v218_fk_act_fk_var_bytearray = 0,
    'ALTER TABLE `ACT_RU_VARIABLE` ADD CONSTRAINT `ACT_FK_VAR_BYTEARRAY` foreign key (BYTEARRAY_ID_) 
    references ACT_GE_BYTEARRAY (ID_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
CREATE TABLE IF NOT EXISTS ACT_HI_VARINST (
    ID_ varchar(64) not null,
    REV_ integer default 1,
    PROC_INST_ID_ varchar(64),
    EXECUTION_ID_ varchar(64),
    TASK_ID_ varchar(64),
    NAME_ varchar(255) not null,
    VAR_TYPE_ varchar(100),
    SCOPE_ID_ varchar(255),
    SUB_SCOPE_ID_ varchar(255),
    SCOPE_TYPE_ varchar(255),
    BYTEARRAY_ID_ varchar(64),
    DOUBLE_ double,
    LONG_ bigint,
    TEXT_ varchar(4000),
    TEXT2_ varchar(4000),
    META_INFO_ varchar(4000),
    CREATE_TIME_ datetime(3),
    LAST_UPDATED_TIME_ datetime(3),
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;
-- 索引 ACT_IDX_HI_PROCVAR_NAME_TYPE（幂等）
SET @v218_idx_act_idx_hi_procvar_name_type := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_HI_VARINST'
      AND index_name = 'ACT_IDX_HI_PROCVAR_NAME_TYPE'
);
SET @v218_sql := IF(
    @v218_idx_act_idx_hi_procvar_name_type = 0,
    'CREATE INDEX `ACT_IDX_HI_PROCVAR_NAME_TYPE` ON `ACT_HI_VARINST` (NAME_, VAR_TYPE_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 索引 ACT_IDX_HI_VAR_SCOPE_ID_TYPE（幂等）
SET @v218_idx_act_idx_hi_var_scope_id_type := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_HI_VARINST'
      AND index_name = 'ACT_IDX_HI_VAR_SCOPE_ID_TYPE'
);
SET @v218_sql := IF(
    @v218_idx_act_idx_hi_var_scope_id_type = 0,
    'CREATE INDEX `ACT_IDX_HI_VAR_SCOPE_ID_TYPE` ON `ACT_HI_VARINST` (SCOPE_ID_, SCOPE_TYPE_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 索引 ACT_IDX_HI_VAR_SUB_ID_TYPE（幂等）
SET @v218_idx_act_idx_hi_var_sub_id_type := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_HI_VARINST'
      AND index_name = 'ACT_IDX_HI_VAR_SUB_ID_TYPE'
);
SET @v218_sql := IF(
    @v218_idx_act_idx_hi_var_sub_id_type = 0,
    'CREATE INDEX `ACT_IDX_HI_VAR_SUB_ID_TYPE` ON `ACT_HI_VARINST` (SUB_SCOPE_ID_, SCOPE_TYPE_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
CREATE TABLE IF NOT EXISTS ACT_RU_EVENT_SUBSCR (
    ID_ varchar(64) not null,
    REV_ integer,
    EVENT_TYPE_ varchar(255) not null,
    EVENT_NAME_ varchar(255),
    EXECUTION_ID_ varchar(64),
    PROC_INST_ID_ varchar(64),
    ACTIVITY_ID_ varchar(64),
    CONFIGURATION_ varchar(255),
    CREATED_ timestamp(3) not null DEFAULT CURRENT_TIMESTAMP(3),
    PROC_DEF_ID_ varchar(64),
    SUB_SCOPE_ID_ varchar(64),
    SCOPE_ID_ varchar(64),
    SCOPE_DEFINITION_ID_ varchar(64),
    SCOPE_DEFINITION_KEY_ varchar(255),
    SCOPE_TYPE_ varchar(64),
    LOCK_TIME_ timestamp(3) NULL,
    LOCK_OWNER_ varchar(255),
    TENANT_ID_ varchar(255) default '',
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;
-- 索引 ACT_IDX_EVENT_SUBSCR_CONFIG_（幂等）
SET @v218_idx_act_idx_event_subscr_config_ := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_RU_EVENT_SUBSCR'
      AND index_name = 'ACT_IDX_EVENT_SUBSCR_CONFIG_'
);
SET @v218_sql := IF(
    @v218_idx_act_idx_event_subscr_config_ = 0,
    'CREATE INDEX `ACT_IDX_EVENT_SUBSCR_CONFIG_` ON `ACT_RU_EVENT_SUBSCR` (CONFIGURATION_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 索引 ACT_IDX_EVENT_SUBSCR_EXEC_ID（幂等）
SET @v218_idx_act_idx_event_subscr_exec_id := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_RU_EVENT_SUBSCR'
      AND index_name = 'ACT_IDX_EVENT_SUBSCR_EXEC_ID'
);
SET @v218_sql := IF(
    @v218_idx_act_idx_event_subscr_exec_id = 0,
    'CREATE INDEX `ACT_IDX_EVENT_SUBSCR_EXEC_ID` ON `ACT_RU_EVENT_SUBSCR` (EXECUTION_ID_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 索引 ACT_IDX_EVENT_SUBSCR_PROC_ID（幂等）
SET @v218_idx_act_idx_event_subscr_proc_id := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_RU_EVENT_SUBSCR'
      AND index_name = 'ACT_IDX_EVENT_SUBSCR_PROC_ID'
);
SET @v218_sql := IF(
    @v218_idx_act_idx_event_subscr_proc_id = 0,
    'CREATE INDEX `ACT_IDX_EVENT_SUBSCR_PROC_ID` ON `ACT_RU_EVENT_SUBSCR` (PROC_INST_ID_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 索引 ACT_IDX_EVENT_SUBSCR_SCOPEREF_（幂等）
SET @v218_idx_act_idx_event_subscr_scoperef_ := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_RU_EVENT_SUBSCR'
      AND index_name = 'ACT_IDX_EVENT_SUBSCR_SCOPEREF_'
);
SET @v218_sql := IF(
    @v218_idx_act_idx_event_subscr_scoperef_ = 0,
    'CREATE INDEX `ACT_IDX_EVENT_SUBSCR_SCOPEREF_` ON `ACT_RU_EVENT_SUBSCR` (SCOPE_ID_, SCOPE_TYPE_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;

-- ===== org/flowable/db/create/flowable.mysql.create.engine.sql =====
CREATE TABLE IF NOT EXISTS ACT_RE_DEPLOYMENT (
    ID_ varchar(64),
    NAME_ varchar(255),
    CATEGORY_ varchar(255),
    KEY_ varchar(255),
    TENANT_ID_ varchar(255) default '',
    DEPLOY_TIME_ timestamp(3) NULL,
    DERIVED_FROM_ varchar(64),
    DERIVED_FROM_ROOT_ varchar(64),
    PARENT_DEPLOYMENT_ID_ varchar(255),
    ENGINE_VERSION_ varchar(255),
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;
CREATE TABLE IF NOT EXISTS ACT_RE_MODEL (
    ID_ varchar(64) not null,
    REV_ integer,
    NAME_ varchar(255),
    KEY_ varchar(255),
    CATEGORY_ varchar(255),
    CREATE_TIME_ timestamp(3) null,
    LAST_UPDATE_TIME_ timestamp(3) null,
    VERSION_ integer,
    META_INFO_ varchar(4000),
    DEPLOYMENT_ID_ varchar(64),
    EDITOR_SOURCE_VALUE_ID_ varchar(64),
    EDITOR_SOURCE_EXTRA_VALUE_ID_ varchar(64),
    TENANT_ID_ varchar(255) default '',
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;
CREATE TABLE IF NOT EXISTS ACT_RU_EXECUTION (
    ID_ varchar(64),
    REV_ integer,
    PROC_INST_ID_ varchar(64),
    BUSINESS_KEY_ varchar(255),
    PARENT_ID_ varchar(64),
    PROC_DEF_ID_ varchar(64),
    SUPER_EXEC_ varchar(64),
    ROOT_PROC_INST_ID_ varchar(64),
    ACT_ID_ varchar(255),
    IS_ACTIVE_ TINYINT,
    IS_CONCURRENT_ TINYINT,
    IS_SCOPE_ TINYINT,
    IS_EVENT_SCOPE_ TINYINT,
    IS_MI_ROOT_ TINYINT,
    SUSPENSION_STATE_ integer,
    CACHED_ENT_STATE_ integer,
    TENANT_ID_ varchar(255) default '',
    NAME_ varchar(255),
    START_ACT_ID_ varchar(255),
    START_TIME_ datetime(3),
    START_USER_ID_ varchar(255),
    LOCK_TIME_ timestamp(3) NULL,
    LOCK_OWNER_ varchar(255),
    IS_COUNT_ENABLED_ TINYINT,
    EVT_SUBSCR_COUNT_ integer, 
    TASK_COUNT_ integer, 
    JOB_COUNT_ integer, 
    TIMER_JOB_COUNT_ integer,
    SUSP_JOB_COUNT_ integer,
    DEADLETTER_JOB_COUNT_ integer,
    EXTERNAL_WORKER_JOB_COUNT_ integer,
    VAR_COUNT_ integer, 
    ID_LINK_COUNT_ integer,
    CALLBACK_ID_ varchar(255),
    CALLBACK_TYPE_ varchar(255),
    REFERENCE_ID_ varchar(255),
    REFERENCE_TYPE_ varchar(255),
    PROPAGATED_STAGE_INST_ID_ varchar(255),
    BUSINESS_STATUS_ varchar(255),
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;
CREATE TABLE IF NOT EXISTS ACT_RE_PROCDEF (
    ID_ varchar(64) not null,
    REV_ integer,
    CATEGORY_ varchar(255),
    NAME_ varchar(255),
    KEY_ varchar(255) not null,
    VERSION_ integer not null,
    DEPLOYMENT_ID_ varchar(64),
    RESOURCE_NAME_ varchar(4000),
    DGRM_RESOURCE_NAME_ varchar(4000),
    DESCRIPTION_ varchar(4000),
    HAS_START_FORM_KEY_ TINYINT,
    HAS_GRAPHICAL_NOTATION_ TINYINT,
    SUSPENSION_STATE_ integer,
    TENANT_ID_ varchar(255) default '',
    ENGINE_VERSION_ varchar(255),
    DERIVED_FROM_ varchar(64),
    DERIVED_FROM_ROOT_ varchar(64),
    DERIVED_VERSION_ integer not null default 0,
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;
CREATE TABLE IF NOT EXISTS ACT_EVT_LOG (
    LOG_NR_ bigint auto_increment,
    TYPE_ varchar(64),
    PROC_DEF_ID_ varchar(64),
    PROC_INST_ID_ varchar(64),
    EXECUTION_ID_ varchar(64),
    TASK_ID_ varchar(64),
    TIME_STAMP_ timestamp(3) not null DEFAULT CURRENT_TIMESTAMP(3),
    USER_ID_ varchar(255),
    DATA_ LONGBLOB,
    LOCK_OWNER_ varchar(255),
    LOCK_TIME_ timestamp(3) null,
    IS_PROCESSED_ tinyint default 0,
    primary key (LOG_NR_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;
CREATE TABLE IF NOT EXISTS ACT_PROCDEF_INFO (
	ID_ varchar(64) not null,
    PROC_DEF_ID_ varchar(64) not null,
    REV_ integer,
    INFO_JSON_ID_ varchar(64),
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;
CREATE TABLE IF NOT EXISTS ACT_RU_ACTINST (
    ID_ varchar(64) not null,
    REV_ integer default 1,
    PROC_DEF_ID_ varchar(64) not null,
    PROC_INST_ID_ varchar(64) not null,
    EXECUTION_ID_ varchar(64) not null,
    ACT_ID_ varchar(255) not null,
    TASK_ID_ varchar(64),
    CALL_PROC_INST_ID_ varchar(64),
    ACT_NAME_ varchar(255),
    ACT_TYPE_ varchar(255) not null,
    ASSIGNEE_ varchar(255),
    COMPLETED_BY_ varchar(255),
    START_TIME_ datetime(3) not null,
    END_TIME_ datetime(3),
    DURATION_ bigint,
    TRANSACTION_ORDER_ integer,
    DELETE_REASON_ varchar(4000),
    TENANT_ID_ varchar(255) default '',
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;
-- 索引 ACT_IDX_EXEC_BUSKEY（幂等）
SET @v218_idx_act_idx_exec_buskey := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_RU_EXECUTION'
      AND index_name = 'ACT_IDX_EXEC_BUSKEY'
);
SET @v218_sql := IF(
    @v218_idx_act_idx_exec_buskey = 0,
    'CREATE INDEX `ACT_IDX_EXEC_BUSKEY` ON `ACT_RU_EXECUTION` (BUSINESS_KEY_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 索引 ACT_IDC_EXEC_ROOT（幂等）
SET @v218_idx_act_idc_exec_root := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_RU_EXECUTION'
      AND index_name = 'ACT_IDC_EXEC_ROOT'
);
SET @v218_sql := IF(
    @v218_idx_act_idc_exec_root = 0,
    'CREATE INDEX `ACT_IDC_EXEC_ROOT` ON `ACT_RU_EXECUTION` (ROOT_PROC_INST_ID_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 索引 ACT_IDX_EXEC_REF_ID_（幂等）
SET @v218_idx_act_idx_exec_ref_id_ := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_RU_EXECUTION'
      AND index_name = 'ACT_IDX_EXEC_REF_ID_'
);
SET @v218_sql := IF(
    @v218_idx_act_idx_exec_ref_id_ = 0,
    'CREATE INDEX `ACT_IDX_EXEC_REF_ID_` ON `ACT_RU_EXECUTION` (REFERENCE_ID_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 索引 ACT_IDX_VARIABLE_TASK_ID（幂等）
SET @v218_idx_act_idx_variable_task_id := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_RU_VARIABLE'
      AND index_name = 'ACT_IDX_VARIABLE_TASK_ID'
);
SET @v218_sql := IF(
    @v218_idx_act_idx_variable_task_id = 0,
    'CREATE INDEX `ACT_IDX_VARIABLE_TASK_ID` ON `ACT_RU_VARIABLE` (TASK_ID_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 索引 ACT_IDX_ATHRZ_PROCEDEF（幂等）
SET @v218_idx_act_idx_athrz_procedef := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_RU_IDENTITYLINK'
      AND index_name = 'ACT_IDX_ATHRZ_PROCEDEF'
);
SET @v218_sql := IF(
    @v218_idx_act_idx_athrz_procedef = 0,
    'CREATE INDEX `ACT_IDX_ATHRZ_PROCEDEF` ON `ACT_RU_IDENTITYLINK` (PROC_DEF_ID_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 索引 ACT_IDX_INFO_PROCDEF（幂等）
SET @v218_idx_act_idx_info_procdef := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_PROCDEF_INFO'
      AND index_name = 'ACT_IDX_INFO_PROCDEF'
);
SET @v218_sql := IF(
    @v218_idx_act_idx_info_procdef = 0,
    'CREATE INDEX `ACT_IDX_INFO_PROCDEF` ON `ACT_PROCDEF_INFO` (PROC_DEF_ID_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 索引 ACT_IDX_BYTEAR_DEPL（幂等）
SET @v218_idx_act_idx_bytear_depl := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_GE_BYTEARRAY'
      AND index_name = 'ACT_IDX_BYTEAR_DEPL'
);
SET @v218_sql := IF(
    @v218_idx_act_idx_bytear_depl = 0,
    'CREATE INDEX `ACT_IDX_BYTEAR_DEPL` ON `ACT_GE_BYTEARRAY` (DEPLOYMENT_ID_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 索引 ACT_IDX_RU_ACTI_START（幂等）
SET @v218_idx_act_idx_ru_acti_start := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_RU_ACTINST'
      AND index_name = 'ACT_IDX_RU_ACTI_START'
);
SET @v218_sql := IF(
    @v218_idx_act_idx_ru_acti_start = 0,
    'CREATE INDEX `ACT_IDX_RU_ACTI_START` ON `ACT_RU_ACTINST` (START_TIME_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 索引 ACT_IDX_RU_ACTI_END（幂等）
SET @v218_idx_act_idx_ru_acti_end := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_RU_ACTINST'
      AND index_name = 'ACT_IDX_RU_ACTI_END'
);
SET @v218_sql := IF(
    @v218_idx_act_idx_ru_acti_end = 0,
    'CREATE INDEX `ACT_IDX_RU_ACTI_END` ON `ACT_RU_ACTINST` (END_TIME_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 索引 ACT_IDX_RU_ACTI_PROC（幂等）
SET @v218_idx_act_idx_ru_acti_proc := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_RU_ACTINST'
      AND index_name = 'ACT_IDX_RU_ACTI_PROC'
);
SET @v218_sql := IF(
    @v218_idx_act_idx_ru_acti_proc = 0,
    'CREATE INDEX `ACT_IDX_RU_ACTI_PROC` ON `ACT_RU_ACTINST` (PROC_INST_ID_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 索引 ACT_IDX_RU_ACTI_PROC_ACT（幂等）
SET @v218_idx_act_idx_ru_acti_proc_act := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_RU_ACTINST'
      AND index_name = 'ACT_IDX_RU_ACTI_PROC_ACT'
);
SET @v218_sql := IF(
    @v218_idx_act_idx_ru_acti_proc_act = 0,
    'CREATE INDEX `ACT_IDX_RU_ACTI_PROC_ACT` ON `ACT_RU_ACTINST` (PROC_INST_ID_, ACT_ID_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 索引 ACT_IDX_RU_ACTI_EXEC（幂等）
SET @v218_idx_act_idx_ru_acti_exec := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_RU_ACTINST'
      AND index_name = 'ACT_IDX_RU_ACTI_EXEC'
);
SET @v218_sql := IF(
    @v218_idx_act_idx_ru_acti_exec = 0,
    'CREATE INDEX `ACT_IDX_RU_ACTI_EXEC` ON `ACT_RU_ACTINST` (EXECUTION_ID_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 索引 ACT_IDX_RU_ACTI_EXEC_ACT（幂等）
SET @v218_idx_act_idx_ru_acti_exec_act := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_RU_ACTINST'
      AND index_name = 'ACT_IDX_RU_ACTI_EXEC_ACT'
);
SET @v218_sql := IF(
    @v218_idx_act_idx_ru_acti_exec_act = 0,
    'CREATE INDEX `ACT_IDX_RU_ACTI_EXEC_ACT` ON `ACT_RU_ACTINST` (EXECUTION_ID_, ACT_ID_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 索引 ACT_IDX_RU_ACTI_TASK（幂等）
SET @v218_idx_act_idx_ru_acti_task := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_RU_ACTINST'
      AND index_name = 'ACT_IDX_RU_ACTI_TASK'
);
SET @v218_sql := IF(
    @v218_idx_act_idx_ru_acti_task = 0,
    'CREATE INDEX `ACT_IDX_RU_ACTI_TASK` ON `ACT_RU_ACTINST` (TASK_ID_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 外键 ACT_FK_BYTEARR_DEPL（幂等）
SET @v218_fk_act_fk_bytearr_depl := (
    SELECT COUNT(*) FROM information_schema.table_constraints
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_GE_BYTEARRAY'
      AND constraint_name = 'ACT_FK_BYTEARR_DEPL'
);
SET @v218_sql := IF(
    @v218_fk_act_fk_bytearr_depl = 0,
    'ALTER TABLE `ACT_GE_BYTEARRAY` ADD CONSTRAINT `ACT_FK_BYTEARR_DEPL` foreign key (DEPLOYMENT_ID_) 
    references ACT_RE_DEPLOYMENT (ID_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 外键 ACT_UNIQ_PROCDEF（幂等）
SET @v218_fk_act_uniq_procdef := (
    SELECT COUNT(*) FROM information_schema.table_constraints
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_RE_PROCDEF'
      AND constraint_name = 'ACT_UNIQ_PROCDEF'
);
SET @v218_sql := IF(
    @v218_fk_act_uniq_procdef = 0,
    'ALTER TABLE `ACT_RE_PROCDEF` ADD CONSTRAINT `ACT_UNIQ_PROCDEF` unique (KEY_,VERSION_, DERIVED_VERSION_, TENANT_ID_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 外键 ACT_FK_EXE_PROCINST（幂等）
SET @v218_fk_act_fk_exe_procinst := (
    SELECT COUNT(*) FROM information_schema.table_constraints
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_RU_EXECUTION'
      AND constraint_name = 'ACT_FK_EXE_PROCINST'
);
SET @v218_sql := IF(
    @v218_fk_act_fk_exe_procinst = 0,
    'ALTER TABLE `ACT_RU_EXECUTION` ADD CONSTRAINT `ACT_FK_EXE_PROCINST` foreign key (PROC_INST_ID_) 
    references ACT_RU_EXECUTION (ID_) on delete cascade on update cascade',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 外键 ACT_FK_EXE_PARENT（幂等）
SET @v218_fk_act_fk_exe_parent := (
    SELECT COUNT(*) FROM information_schema.table_constraints
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_RU_EXECUTION'
      AND constraint_name = 'ACT_FK_EXE_PARENT'
);
SET @v218_sql := IF(
    @v218_fk_act_fk_exe_parent = 0,
    'ALTER TABLE `ACT_RU_EXECUTION` ADD CONSTRAINT `ACT_FK_EXE_PARENT` foreign key (PARENT_ID_) 
    references ACT_RU_EXECUTION (ID_) on delete cascade',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 外键 ACT_FK_EXE_SUPER（幂等）
SET @v218_fk_act_fk_exe_super := (
    SELECT COUNT(*) FROM information_schema.table_constraints
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_RU_EXECUTION'
      AND constraint_name = 'ACT_FK_EXE_SUPER'
);
SET @v218_sql := IF(
    @v218_fk_act_fk_exe_super = 0,
    'ALTER TABLE `ACT_RU_EXECUTION` ADD CONSTRAINT `ACT_FK_EXE_SUPER` foreign key (SUPER_EXEC_) 
    references ACT_RU_EXECUTION (ID_) on delete cascade',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 外键 ACT_FK_EXE_PROCDEF（幂等）
SET @v218_fk_act_fk_exe_procdef := (
    SELECT COUNT(*) FROM information_schema.table_constraints
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_RU_EXECUTION'
      AND constraint_name = 'ACT_FK_EXE_PROCDEF'
);
SET @v218_sql := IF(
    @v218_fk_act_fk_exe_procdef = 0,
    'ALTER TABLE `ACT_RU_EXECUTION` ADD CONSTRAINT `ACT_FK_EXE_PROCDEF` foreign key (PROC_DEF_ID_) 
    references ACT_RE_PROCDEF (ID_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 外键 ACT_FK_TSKASS_TASK（幂等）
SET @v218_fk_act_fk_tskass_task := (
    SELECT COUNT(*) FROM information_schema.table_constraints
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_RU_IDENTITYLINK'
      AND constraint_name = 'ACT_FK_TSKASS_TASK'
);
SET @v218_sql := IF(
    @v218_fk_act_fk_tskass_task = 0,
    'ALTER TABLE `ACT_RU_IDENTITYLINK` ADD CONSTRAINT `ACT_FK_TSKASS_TASK` foreign key (TASK_ID_) 
    references ACT_RU_TASK (ID_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 外键 ACT_FK_ATHRZ_PROCEDEF（幂等）
SET @v218_fk_act_fk_athrz_procedef := (
    SELECT COUNT(*) FROM information_schema.table_constraints
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_RU_IDENTITYLINK'
      AND constraint_name = 'ACT_FK_ATHRZ_PROCEDEF'
);
SET @v218_sql := IF(
    @v218_fk_act_fk_athrz_procedef = 0,
    'ALTER TABLE `ACT_RU_IDENTITYLINK` ADD CONSTRAINT `ACT_FK_ATHRZ_PROCEDEF` foreign key (PROC_DEF_ID_) 
    references ACT_RE_PROCDEF(ID_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 外键 ACT_FK_IDL_PROCINST（幂等）
SET @v218_fk_act_fk_idl_procinst := (
    SELECT COUNT(*) FROM information_schema.table_constraints
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_RU_IDENTITYLINK'
      AND constraint_name = 'ACT_FK_IDL_PROCINST'
);
SET @v218_sql := IF(
    @v218_fk_act_fk_idl_procinst = 0,
    'ALTER TABLE `ACT_RU_IDENTITYLINK` ADD CONSTRAINT `ACT_FK_IDL_PROCINST` foreign key (PROC_INST_ID_) 
    references ACT_RU_EXECUTION (ID_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 外键 ACT_FK_TASK_EXE（幂等）
SET @v218_fk_act_fk_task_exe := (
    SELECT COUNT(*) FROM information_schema.table_constraints
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_RU_TASK'
      AND constraint_name = 'ACT_FK_TASK_EXE'
);
SET @v218_sql := IF(
    @v218_fk_act_fk_task_exe = 0,
    'ALTER TABLE `ACT_RU_TASK` ADD CONSTRAINT `ACT_FK_TASK_EXE` foreign key (EXECUTION_ID_)
    references ACT_RU_EXECUTION (ID_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 外键 ACT_FK_TASK_PROCINST（幂等）
SET @v218_fk_act_fk_task_procinst := (
    SELECT COUNT(*) FROM information_schema.table_constraints
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_RU_TASK'
      AND constraint_name = 'ACT_FK_TASK_PROCINST'
);
SET @v218_sql := IF(
    @v218_fk_act_fk_task_procinst = 0,
    'ALTER TABLE `ACT_RU_TASK` ADD CONSTRAINT `ACT_FK_TASK_PROCINST` foreign key (PROC_INST_ID_)
    references ACT_RU_EXECUTION (ID_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 外键 ACT_FK_TASK_PROCDEF（幂等）
SET @v218_fk_act_fk_task_procdef := (
    SELECT COUNT(*) FROM information_schema.table_constraints
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_RU_TASK'
      AND constraint_name = 'ACT_FK_TASK_PROCDEF'
);
SET @v218_sql := IF(
    @v218_fk_act_fk_task_procdef = 0,
    'ALTER TABLE `ACT_RU_TASK` ADD CONSTRAINT `ACT_FK_TASK_PROCDEF` foreign key (PROC_DEF_ID_)
  	references ACT_RE_PROCDEF (ID_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 外键 ACT_FK_VAR_EXE（幂等）
SET @v218_fk_act_fk_var_exe := (
    SELECT COUNT(*) FROM information_schema.table_constraints
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_RU_VARIABLE'
      AND constraint_name = 'ACT_FK_VAR_EXE'
);
SET @v218_sql := IF(
    @v218_fk_act_fk_var_exe = 0,
    'ALTER TABLE `ACT_RU_VARIABLE` ADD CONSTRAINT `ACT_FK_VAR_EXE` foreign key (EXECUTION_ID_) 
    references ACT_RU_EXECUTION (ID_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 外键 ACT_FK_VAR_PROCINST（幂等）
SET @v218_fk_act_fk_var_procinst := (
    SELECT COUNT(*) FROM information_schema.table_constraints
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_RU_VARIABLE'
      AND constraint_name = 'ACT_FK_VAR_PROCINST'
);
SET @v218_sql := IF(
    @v218_fk_act_fk_var_procinst = 0,
    'ALTER TABLE `ACT_RU_VARIABLE` ADD CONSTRAINT `ACT_FK_VAR_PROCINST` foreign key (PROC_INST_ID_)
    references ACT_RU_EXECUTION(ID_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 外键 ACT_FK_JOB_EXECUTION（幂等）
SET @v218_fk_act_fk_job_execution := (
    SELECT COUNT(*) FROM information_schema.table_constraints
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_RU_JOB'
      AND constraint_name = 'ACT_FK_JOB_EXECUTION'
);
SET @v218_sql := IF(
    @v218_fk_act_fk_job_execution = 0,
    'ALTER TABLE `ACT_RU_JOB` ADD CONSTRAINT `ACT_FK_JOB_EXECUTION` foreign key (EXECUTION_ID_) 
    references ACT_RU_EXECUTION (ID_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 外键 ACT_FK_JOB_PROCESS_INSTANCE（幂等）
SET @v218_fk_act_fk_job_process_instance := (
    SELECT COUNT(*) FROM information_schema.table_constraints
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_RU_JOB'
      AND constraint_name = 'ACT_FK_JOB_PROCESS_INSTANCE'
);
SET @v218_sql := IF(
    @v218_fk_act_fk_job_process_instance = 0,
    'ALTER TABLE `ACT_RU_JOB` ADD CONSTRAINT `ACT_FK_JOB_PROCESS_INSTANCE` foreign key (PROCESS_INSTANCE_ID_) 
    references ACT_RU_EXECUTION (ID_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 外键 ACT_FK_JOB_PROC_DEF（幂等）
SET @v218_fk_act_fk_job_proc_def := (
    SELECT COUNT(*) FROM information_schema.table_constraints
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_RU_JOB'
      AND constraint_name = 'ACT_FK_JOB_PROC_DEF'
);
SET @v218_sql := IF(
    @v218_fk_act_fk_job_proc_def = 0,
    'ALTER TABLE `ACT_RU_JOB` ADD CONSTRAINT `ACT_FK_JOB_PROC_DEF` foreign key (PROC_DEF_ID_) 
    references ACT_RE_PROCDEF (ID_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 外键 ACT_FK_TIMER_JOB_EXECUTION（幂等）
SET @v218_fk_act_fk_timer_job_execution := (
    SELECT COUNT(*) FROM information_schema.table_constraints
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_RU_TIMER_JOB'
      AND constraint_name = 'ACT_FK_TIMER_JOB_EXECUTION'
);
SET @v218_sql := IF(
    @v218_fk_act_fk_timer_job_execution = 0,
    'ALTER TABLE `ACT_RU_TIMER_JOB` ADD CONSTRAINT `ACT_FK_TIMER_JOB_EXECUTION` foreign key (EXECUTION_ID_) 
    references ACT_RU_EXECUTION (ID_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 外键 ACT_FK_TIMER_JOB_PROCESS_INSTANCE（幂等）
SET @v218_fk_act_fk_timer_job_process_instance := (
    SELECT COUNT(*) FROM information_schema.table_constraints
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_RU_TIMER_JOB'
      AND constraint_name = 'ACT_FK_TIMER_JOB_PROCESS_INSTANCE'
);
SET @v218_sql := IF(
    @v218_fk_act_fk_timer_job_process_instance = 0,
    'ALTER TABLE `ACT_RU_TIMER_JOB` ADD CONSTRAINT `ACT_FK_TIMER_JOB_PROCESS_INSTANCE` foreign key (PROCESS_INSTANCE_ID_) 
    references ACT_RU_EXECUTION (ID_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 外键 ACT_FK_TIMER_JOB_PROC_DEF（幂等）
SET @v218_fk_act_fk_timer_job_proc_def := (
    SELECT COUNT(*) FROM information_schema.table_constraints
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_RU_TIMER_JOB'
      AND constraint_name = 'ACT_FK_TIMER_JOB_PROC_DEF'
);
SET @v218_sql := IF(
    @v218_fk_act_fk_timer_job_proc_def = 0,
    'ALTER TABLE `ACT_RU_TIMER_JOB` ADD CONSTRAINT `ACT_FK_TIMER_JOB_PROC_DEF` foreign key (PROC_DEF_ID_) 
    references ACT_RE_PROCDEF (ID_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 外键 ACT_FK_SUSPENDED_JOB_EXECUTION（幂等）
SET @v218_fk_act_fk_suspended_job_execution := (
    SELECT COUNT(*) FROM information_schema.table_constraints
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_RU_SUSPENDED_JOB'
      AND constraint_name = 'ACT_FK_SUSPENDED_JOB_EXECUTION'
);
SET @v218_sql := IF(
    @v218_fk_act_fk_suspended_job_execution = 0,
    'ALTER TABLE `ACT_RU_SUSPENDED_JOB` ADD CONSTRAINT `ACT_FK_SUSPENDED_JOB_EXECUTION` foreign key (EXECUTION_ID_) 
    references ACT_RU_EXECUTION (ID_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 外键 ACT_FK_SUSPENDED_JOB_PROCESS_INSTANCE（幂等）
SET @v218_fk_act_fk_suspended_job_process_instance := (
    SELECT COUNT(*) FROM information_schema.table_constraints
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_RU_SUSPENDED_JOB'
      AND constraint_name = 'ACT_FK_SUSPENDED_JOB_PROCESS_INSTANCE'
);
SET @v218_sql := IF(
    @v218_fk_act_fk_suspended_job_process_instance = 0,
    'ALTER TABLE `ACT_RU_SUSPENDED_JOB` ADD CONSTRAINT `ACT_FK_SUSPENDED_JOB_PROCESS_INSTANCE` foreign key (PROCESS_INSTANCE_ID_) 
    references ACT_RU_EXECUTION (ID_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 外键 ACT_FK_SUSPENDED_JOB_PROC_DEF（幂等）
SET @v218_fk_act_fk_suspended_job_proc_def := (
    SELECT COUNT(*) FROM information_schema.table_constraints
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_RU_SUSPENDED_JOB'
      AND constraint_name = 'ACT_FK_SUSPENDED_JOB_PROC_DEF'
);
SET @v218_sql := IF(
    @v218_fk_act_fk_suspended_job_proc_def = 0,
    'ALTER TABLE `ACT_RU_SUSPENDED_JOB` ADD CONSTRAINT `ACT_FK_SUSPENDED_JOB_PROC_DEF` foreign key (PROC_DEF_ID_) 
    references ACT_RE_PROCDEF (ID_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 外键 ACT_FK_DEADLETTER_JOB_EXECUTION（幂等）
SET @v218_fk_act_fk_deadletter_job_execution := (
    SELECT COUNT(*) FROM information_schema.table_constraints
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_RU_DEADLETTER_JOB'
      AND constraint_name = 'ACT_FK_DEADLETTER_JOB_EXECUTION'
);
SET @v218_sql := IF(
    @v218_fk_act_fk_deadletter_job_execution = 0,
    'ALTER TABLE `ACT_RU_DEADLETTER_JOB` ADD CONSTRAINT `ACT_FK_DEADLETTER_JOB_EXECUTION` foreign key (EXECUTION_ID_) 
    references ACT_RU_EXECUTION (ID_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 外键 ACT_FK_DEADLETTER_JOB_PROCESS_INSTANCE（幂等）
SET @v218_fk_act_fk_deadletter_job_process_instance := (
    SELECT COUNT(*) FROM information_schema.table_constraints
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_RU_DEADLETTER_JOB'
      AND constraint_name = 'ACT_FK_DEADLETTER_JOB_PROCESS_INSTANCE'
);
SET @v218_sql := IF(
    @v218_fk_act_fk_deadletter_job_process_instance = 0,
    'ALTER TABLE `ACT_RU_DEADLETTER_JOB` ADD CONSTRAINT `ACT_FK_DEADLETTER_JOB_PROCESS_INSTANCE` foreign key (PROCESS_INSTANCE_ID_) 
    references ACT_RU_EXECUTION (ID_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 外键 ACT_FK_DEADLETTER_JOB_PROC_DEF（幂等）
SET @v218_fk_act_fk_deadletter_job_proc_def := (
    SELECT COUNT(*) FROM information_schema.table_constraints
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_RU_DEADLETTER_JOB'
      AND constraint_name = 'ACT_FK_DEADLETTER_JOB_PROC_DEF'
);
SET @v218_sql := IF(
    @v218_fk_act_fk_deadletter_job_proc_def = 0,
    'ALTER TABLE `ACT_RU_DEADLETTER_JOB` ADD CONSTRAINT `ACT_FK_DEADLETTER_JOB_PROC_DEF` foreign key (PROC_DEF_ID_) 
    references ACT_RE_PROCDEF (ID_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 外键 ACT_FK_EVENT_EXEC（幂等）
SET @v218_fk_act_fk_event_exec := (
    SELECT COUNT(*) FROM information_schema.table_constraints
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_RU_EVENT_SUBSCR'
      AND constraint_name = 'ACT_FK_EVENT_EXEC'
);
SET @v218_sql := IF(
    @v218_fk_act_fk_event_exec = 0,
    'ALTER TABLE `ACT_RU_EVENT_SUBSCR` ADD CONSTRAINT `ACT_FK_EVENT_EXEC` foreign key (EXECUTION_ID_)
    references ACT_RU_EXECUTION(ID_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 外键 ACT_FK_MODEL_SOURCE（幂等）
SET @v218_fk_act_fk_model_source := (
    SELECT COUNT(*) FROM information_schema.table_constraints
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_RE_MODEL'
      AND constraint_name = 'ACT_FK_MODEL_SOURCE'
);
SET @v218_sql := IF(
    @v218_fk_act_fk_model_source = 0,
    'ALTER TABLE `ACT_RE_MODEL` ADD CONSTRAINT `ACT_FK_MODEL_SOURCE` foreign key (EDITOR_SOURCE_VALUE_ID_) 
    references ACT_GE_BYTEARRAY (ID_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 外键 ACT_FK_MODEL_SOURCE_EXTRA（幂等）
SET @v218_fk_act_fk_model_source_extra := (
    SELECT COUNT(*) FROM information_schema.table_constraints
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_RE_MODEL'
      AND constraint_name = 'ACT_FK_MODEL_SOURCE_EXTRA'
);
SET @v218_sql := IF(
    @v218_fk_act_fk_model_source_extra = 0,
    'ALTER TABLE `ACT_RE_MODEL` ADD CONSTRAINT `ACT_FK_MODEL_SOURCE_EXTRA` foreign key (EDITOR_SOURCE_EXTRA_VALUE_ID_) 
    references ACT_GE_BYTEARRAY (ID_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 外键 ACT_FK_MODEL_DEPLOYMENT（幂等）
SET @v218_fk_act_fk_model_deployment := (
    SELECT COUNT(*) FROM information_schema.table_constraints
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_RE_MODEL'
      AND constraint_name = 'ACT_FK_MODEL_DEPLOYMENT'
);
SET @v218_sql := IF(
    @v218_fk_act_fk_model_deployment = 0,
    'ALTER TABLE `ACT_RE_MODEL` ADD CONSTRAINT `ACT_FK_MODEL_DEPLOYMENT` foreign key (DEPLOYMENT_ID_) 
    references ACT_RE_DEPLOYMENT (ID_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 外键 ACT_FK_INFO_JSON_BA（幂等）
SET @v218_fk_act_fk_info_json_ba := (
    SELECT COUNT(*) FROM information_schema.table_constraints
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_PROCDEF_INFO'
      AND constraint_name = 'ACT_FK_INFO_JSON_BA'
);
SET @v218_sql := IF(
    @v218_fk_act_fk_info_json_ba = 0,
    'ALTER TABLE `ACT_PROCDEF_INFO` ADD CONSTRAINT `ACT_FK_INFO_JSON_BA` foreign key (INFO_JSON_ID_) 
    references ACT_GE_BYTEARRAY (ID_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 外键 ACT_FK_INFO_PROCDEF（幂等）
SET @v218_fk_act_fk_info_procdef := (
    SELECT COUNT(*) FROM information_schema.table_constraints
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_PROCDEF_INFO'
      AND constraint_name = 'ACT_FK_INFO_PROCDEF'
);
SET @v218_sql := IF(
    @v218_fk_act_fk_info_procdef = 0,
    'ALTER TABLE `ACT_PROCDEF_INFO` ADD CONSTRAINT `ACT_FK_INFO_PROCDEF` foreign key (PROC_DEF_ID_) 
    references ACT_RE_PROCDEF (ID_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 外键 ACT_UNIQ_INFO_PROCDEF（幂等）
SET @v218_fk_act_uniq_info_procdef := (
    SELECT COUNT(*) FROM information_schema.table_constraints
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_PROCDEF_INFO'
      AND constraint_name = 'ACT_UNIQ_INFO_PROCDEF'
);
SET @v218_sql := IF(
    @v218_fk_act_uniq_info_procdef = 0,
    'ALTER TABLE `ACT_PROCDEF_INFO` ADD CONSTRAINT `ACT_UNIQ_INFO_PROCDEF` unique (PROC_DEF_ID_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
INSERT IGNORE INTO ACT_GE_PROPERTY
values ('schema.version', '8.0.0.0', 1);
INSERT IGNORE INTO ACT_GE_PROPERTY
values ('schema.history', 'create(8.0.0.0)', 1);

-- ===== org/flowable/db/create/flowable.mysql.create.history.sql =====
CREATE TABLE IF NOT EXISTS ACT_HI_PROCINST (
    ID_ varchar(64) not null,
    REV_ integer default 1,
    PROC_INST_ID_ varchar(64) not null,
    BUSINESS_KEY_ varchar(255),
    PROC_DEF_ID_ varchar(64) not null,
    START_TIME_ datetime(3) not null,
    END_TIME_ datetime(3),
    DURATION_ bigint,
    START_USER_ID_ varchar(255),
    START_ACT_ID_ varchar(255),
    END_ACT_ID_ varchar(255),
    SUPER_PROCESS_INSTANCE_ID_ varchar(64),
    DELETE_REASON_ varchar(4000),
    TENANT_ID_ varchar(255) default '',
    NAME_ varchar(255),
    CALLBACK_ID_ varchar(255),
    CALLBACK_TYPE_ varchar(255),
    REFERENCE_ID_ varchar(255),
    REFERENCE_TYPE_ varchar(255),
    PROPAGATED_STAGE_INST_ID_ varchar(255),
    BUSINESS_STATUS_ varchar(255),
    END_USER_ID_ varchar(255),
    STATE_ varchar(255),
    primary key (ID_),
    unique (PROC_INST_ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;
CREATE TABLE IF NOT EXISTS ACT_HI_ACTINST (
    ID_ varchar(64) not null,
    REV_ integer default 1,
    PROC_DEF_ID_ varchar(64) not null,
    PROC_INST_ID_ varchar(64) not null,
    EXECUTION_ID_ varchar(64) not null,
    ACT_ID_ varchar(255) not null,
    TASK_ID_ varchar(64),
    CALL_PROC_INST_ID_ varchar(64),
    ACT_NAME_ varchar(255),
    ACT_TYPE_ varchar(255) not null,
    ASSIGNEE_ varchar(255),
    COMPLETED_BY_ varchar(255),
    START_TIME_ datetime(3) not null,
    END_TIME_ datetime(3),
    TRANSACTION_ORDER_ integer,
    DURATION_ bigint,
    DELETE_REASON_ varchar(4000),
    TENANT_ID_ varchar(255) default '',
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;
CREATE TABLE IF NOT EXISTS ACT_HI_DETAIL (
    ID_ varchar(64) not null,
    TYPE_ varchar(255) not null,
    PROC_INST_ID_ varchar(64),
    EXECUTION_ID_ varchar(64),
    TASK_ID_ varchar(64),
    ACT_INST_ID_ varchar(64),
    NAME_ varchar(255) not null,
    VAR_TYPE_ varchar(255),
    REV_ integer,
    TIME_ datetime(3) not null,
    BYTEARRAY_ID_ varchar(64),
    DOUBLE_ double,
    LONG_ bigint,
    TEXT_ varchar(4000),
    TEXT2_ varchar(4000),
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;
CREATE TABLE IF NOT EXISTS ACT_HI_COMMENT (
    ID_ varchar(64) not null,
    TYPE_ varchar(255),
    TIME_ datetime(3) not null,
    USER_ID_ varchar(255),
    TASK_ID_ varchar(64),
    PROC_INST_ID_ varchar(64),
    ACTION_ varchar(255),
    MESSAGE_ varchar(4000),
    FULL_MSG_ LONGBLOB,
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;
CREATE TABLE IF NOT EXISTS ACT_HI_ATTACHMENT (
    ID_ varchar(64) not null,
    REV_ integer,
    USER_ID_ varchar(255),
    NAME_ varchar(255),
    DESCRIPTION_ varchar(4000),
    TYPE_ varchar(255),
    TASK_ID_ varchar(64),
    PROC_INST_ID_ varchar(64),
    URL_ varchar(4000),
    CONTENT_ID_ varchar(64),
    TIME_ datetime(3),
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;
-- 索引 ACT_IDX_HI_PRO_INST_END（幂等）
SET @v218_idx_act_idx_hi_pro_inst_end := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_HI_PROCINST'
      AND index_name = 'ACT_IDX_HI_PRO_INST_END'
);
SET @v218_sql := IF(
    @v218_idx_act_idx_hi_pro_inst_end = 0,
    'CREATE INDEX `ACT_IDX_HI_PRO_INST_END` ON `ACT_HI_PROCINST` (END_TIME_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 索引 ACT_IDX_HI_PRO_I_BUSKEY（幂等）
SET @v218_idx_act_idx_hi_pro_i_buskey := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_HI_PROCINST'
      AND index_name = 'ACT_IDX_HI_PRO_I_BUSKEY'
);
SET @v218_sql := IF(
    @v218_idx_act_idx_hi_pro_i_buskey = 0,
    'CREATE INDEX `ACT_IDX_HI_PRO_I_BUSKEY` ON `ACT_HI_PROCINST` (BUSINESS_KEY_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 索引 ACT_IDX_HI_PRO_SUPER_PROCINST（幂等）
SET @v218_idx_act_idx_hi_pro_super_procinst := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_HI_PROCINST'
      AND index_name = 'ACT_IDX_HI_PRO_SUPER_PROCINST'
);
SET @v218_sql := IF(
    @v218_idx_act_idx_hi_pro_super_procinst = 0,
    'CREATE INDEX `ACT_IDX_HI_PRO_SUPER_PROCINST` ON `ACT_HI_PROCINST` (SUPER_PROCESS_INSTANCE_ID_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 索引 ACT_IDX_HI_ACT_INST_START（幂等）
SET @v218_idx_act_idx_hi_act_inst_start := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_HI_ACTINST'
      AND index_name = 'ACT_IDX_HI_ACT_INST_START'
);
SET @v218_sql := IF(
    @v218_idx_act_idx_hi_act_inst_start = 0,
    'CREATE INDEX `ACT_IDX_HI_ACT_INST_START` ON `ACT_HI_ACTINST` (START_TIME_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 索引 ACT_IDX_HI_ACT_INST_END（幂等）
SET @v218_idx_act_idx_hi_act_inst_end := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_HI_ACTINST'
      AND index_name = 'ACT_IDX_HI_ACT_INST_END'
);
SET @v218_sql := IF(
    @v218_idx_act_idx_hi_act_inst_end = 0,
    'CREATE INDEX `ACT_IDX_HI_ACT_INST_END` ON `ACT_HI_ACTINST` (END_TIME_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 索引 ACT_IDX_HI_DETAIL_PROC_INST（幂等）
SET @v218_idx_act_idx_hi_detail_proc_inst := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_HI_DETAIL'
      AND index_name = 'ACT_IDX_HI_DETAIL_PROC_INST'
);
SET @v218_sql := IF(
    @v218_idx_act_idx_hi_detail_proc_inst = 0,
    'CREATE INDEX `ACT_IDX_HI_DETAIL_PROC_INST` ON `ACT_HI_DETAIL` (PROC_INST_ID_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 索引 ACT_IDX_HI_DETAIL_ACT_INST（幂等）
SET @v218_idx_act_idx_hi_detail_act_inst := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_HI_DETAIL'
      AND index_name = 'ACT_IDX_HI_DETAIL_ACT_INST'
);
SET @v218_sql := IF(
    @v218_idx_act_idx_hi_detail_act_inst = 0,
    'CREATE INDEX `ACT_IDX_HI_DETAIL_ACT_INST` ON `ACT_HI_DETAIL` (ACT_INST_ID_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 索引 ACT_IDX_HI_DETAIL_TIME（幂等）
SET @v218_idx_act_idx_hi_detail_time := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_HI_DETAIL'
      AND index_name = 'ACT_IDX_HI_DETAIL_TIME'
);
SET @v218_sql := IF(
    @v218_idx_act_idx_hi_detail_time = 0,
    'CREATE INDEX `ACT_IDX_HI_DETAIL_TIME` ON `ACT_HI_DETAIL` (TIME_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 索引 ACT_IDX_HI_DETAIL_NAME（幂等）
SET @v218_idx_act_idx_hi_detail_name := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_HI_DETAIL'
      AND index_name = 'ACT_IDX_HI_DETAIL_NAME'
);
SET @v218_sql := IF(
    @v218_idx_act_idx_hi_detail_name = 0,
    'CREATE INDEX `ACT_IDX_HI_DETAIL_NAME` ON `ACT_HI_DETAIL` (NAME_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 索引 ACT_IDX_HI_DETAIL_TASK_ID（幂等）
SET @v218_idx_act_idx_hi_detail_task_id := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_HI_DETAIL'
      AND index_name = 'ACT_IDX_HI_DETAIL_TASK_ID'
);
SET @v218_sql := IF(
    @v218_idx_act_idx_hi_detail_task_id = 0,
    'CREATE INDEX `ACT_IDX_HI_DETAIL_TASK_ID` ON `ACT_HI_DETAIL` (TASK_ID_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 索引 ACT_IDX_HI_PROCVAR_PROC_INST（幂等）
SET @v218_idx_act_idx_hi_procvar_proc_inst := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_HI_VARINST'
      AND index_name = 'ACT_IDX_HI_PROCVAR_PROC_INST'
);
SET @v218_sql := IF(
    @v218_idx_act_idx_hi_procvar_proc_inst = 0,
    'CREATE INDEX `ACT_IDX_HI_PROCVAR_PROC_INST` ON `ACT_HI_VARINST` (PROC_INST_ID_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 索引 ACT_IDX_HI_PROCVAR_TASK_ID（幂等）
SET @v218_idx_act_idx_hi_procvar_task_id := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_HI_VARINST'
      AND index_name = 'ACT_IDX_HI_PROCVAR_TASK_ID'
);
SET @v218_sql := IF(
    @v218_idx_act_idx_hi_procvar_task_id = 0,
    'CREATE INDEX `ACT_IDX_HI_PROCVAR_TASK_ID` ON `ACT_HI_VARINST` (TASK_ID_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 索引 ACT_IDX_HI_PROCVAR_EXE（幂等）
SET @v218_idx_act_idx_hi_procvar_exe := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_HI_VARINST'
      AND index_name = 'ACT_IDX_HI_PROCVAR_EXE'
);
SET @v218_sql := IF(
    @v218_idx_act_idx_hi_procvar_exe = 0,
    'CREATE INDEX `ACT_IDX_HI_PROCVAR_EXE` ON `ACT_HI_VARINST` (EXECUTION_ID_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 索引 ACT_IDX_HI_ACT_INST_PROCINST（幂等）
SET @v218_idx_act_idx_hi_act_inst_procinst := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_HI_ACTINST'
      AND index_name = 'ACT_IDX_HI_ACT_INST_PROCINST'
);
SET @v218_sql := IF(
    @v218_idx_act_idx_hi_act_inst_procinst = 0,
    'CREATE INDEX `ACT_IDX_HI_ACT_INST_PROCINST` ON `ACT_HI_ACTINST` (PROC_INST_ID_, ACT_ID_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 索引 ACT_IDX_HI_ACT_INST_EXEC（幂等）
SET @v218_idx_act_idx_hi_act_inst_exec := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_HI_ACTINST'
      AND index_name = 'ACT_IDX_HI_ACT_INST_EXEC'
);
SET @v218_sql := IF(
    @v218_idx_act_idx_hi_act_inst_exec = 0,
    'CREATE INDEX `ACT_IDX_HI_ACT_INST_EXEC` ON `ACT_HI_ACTINST` (EXECUTION_ID_, ACT_ID_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 索引 ACT_IDX_HI_IDENT_LNK_TASK（幂等）
SET @v218_idx_act_idx_hi_ident_lnk_task := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_HI_IDENTITYLINK'
      AND index_name = 'ACT_IDX_HI_IDENT_LNK_TASK'
);
SET @v218_sql := IF(
    @v218_idx_act_idx_hi_ident_lnk_task = 0,
    'CREATE INDEX `ACT_IDX_HI_IDENT_LNK_TASK` ON `ACT_HI_IDENTITYLINK` (TASK_ID_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 索引 ACT_IDX_HI_IDENT_LNK_PROCINST（幂等）
SET @v218_idx_act_idx_hi_ident_lnk_procinst := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_HI_IDENTITYLINK'
      AND index_name = 'ACT_IDX_HI_IDENT_LNK_PROCINST'
);
SET @v218_sql := IF(
    @v218_idx_act_idx_hi_ident_lnk_procinst = 0,
    'CREATE INDEX `ACT_IDX_HI_IDENT_LNK_PROCINST` ON `ACT_HI_IDENTITYLINK` (PROC_INST_ID_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 索引 ACT_IDX_HI_TASK_INST_PROCINST（幂等）
SET @v218_idx_act_idx_hi_task_inst_procinst := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_HI_TASKINST'
      AND index_name = 'ACT_IDX_HI_TASK_INST_PROCINST'
);
SET @v218_sql := IF(
    @v218_idx_act_idx_hi_task_inst_procinst = 0,
    'CREATE INDEX `ACT_IDX_HI_TASK_INST_PROCINST` ON `ACT_HI_TASKINST` (PROC_INST_ID_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;

-- ===== org/flowable/idm/db/create/flowable.mysql.create.identity.sql =====
CREATE TABLE IF NOT EXISTS ACT_ID_PROPERTY (
    NAME_ varchar(64),
    VALUE_ varchar(300),
    REV_ integer,
    primary key (NAME_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;
INSERT IGNORE INTO ACT_ID_PROPERTY
values ('schema.version', '8.0.0.0', 1);
CREATE TABLE IF NOT EXISTS ACT_ID_BYTEARRAY (
    ID_ varchar(64),
    REV_ integer,
    NAME_ varchar(255),
    BYTES_ LONGBLOB,
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;
CREATE TABLE IF NOT EXISTS ACT_ID_GROUP (
    ID_ varchar(64),
    REV_ integer,
    NAME_ varchar(255),
    TYPE_ varchar(255),
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;
CREATE TABLE IF NOT EXISTS ACT_ID_MEMBERSHIP (
    USER_ID_ varchar(64),
    GROUP_ID_ varchar(64),
    primary key (USER_ID_, GROUP_ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;
CREATE TABLE IF NOT EXISTS ACT_ID_USER (
    ID_ varchar(64),
    REV_ integer,
    FIRST_ varchar(255),
    LAST_ varchar(255),
    DISPLAY_NAME_ varchar(255),
    EMAIL_ varchar(255),
    PWD_ varchar(255),
    PICTURE_ID_ varchar(64),
    TENANT_ID_ varchar(255) default '',
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;
CREATE TABLE IF NOT EXISTS ACT_ID_INFO (
    ID_ varchar(64),
    REV_ integer,
    USER_ID_ varchar(64),
    TYPE_ varchar(64),
    KEY_ varchar(255),
    VALUE_ varchar(255),
    PASSWORD_ LONGBLOB,
    PARENT_ID_ varchar(255),
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;
CREATE TABLE IF NOT EXISTS ACT_ID_TOKEN (
    ID_ varchar(64) not null,
    REV_ integer,
    TOKEN_VALUE_ varchar(255),
    TOKEN_DATE_ timestamp(3),
    IP_ADDRESS_ varchar(255),
    USER_AGENT_ varchar(255),
    USER_ID_ varchar(255),
    TOKEN_DATA_ varchar(2000),
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;
CREATE TABLE IF NOT EXISTS ACT_ID_PRIV (
    ID_ varchar(64) not null,
    NAME_ varchar(255) not null,
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;
CREATE TABLE IF NOT EXISTS ACT_ID_PRIV_MAPPING (
    ID_ varchar(64) not null,
    PRIV_ID_ varchar(64) not null,
    USER_ID_ varchar(255),
    GROUP_ID_ varchar(255),
    primary key (ID_)
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COLLATE utf8_bin;
-- 外键 ACT_FK_MEMB_GROUP（幂等）
SET @v218_fk_act_fk_memb_group := (
    SELECT COUNT(*) FROM information_schema.table_constraints
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_ID_MEMBERSHIP'
      AND constraint_name = 'ACT_FK_MEMB_GROUP'
);
SET @v218_sql := IF(
    @v218_fk_act_fk_memb_group = 0,
    'ALTER TABLE `ACT_ID_MEMBERSHIP` ADD CONSTRAINT `ACT_FK_MEMB_GROUP` foreign key (GROUP_ID_)
    references ACT_ID_GROUP (ID_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 外键 ACT_FK_MEMB_USER（幂等）
SET @v218_fk_act_fk_memb_user := (
    SELECT COUNT(*) FROM information_schema.table_constraints
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_ID_MEMBERSHIP'
      AND constraint_name = 'ACT_FK_MEMB_USER'
);
SET @v218_sql := IF(
    @v218_fk_act_fk_memb_user = 0,
    'ALTER TABLE `ACT_ID_MEMBERSHIP` ADD CONSTRAINT `ACT_FK_MEMB_USER` foreign key (USER_ID_)
    references ACT_ID_USER (ID_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 外键 ACT_FK_PRIV_MAPPING（幂等）
SET @v218_fk_act_fk_priv_mapping := (
    SELECT COUNT(*) FROM information_schema.table_constraints
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_ID_PRIV_MAPPING'
      AND constraint_name = 'ACT_FK_PRIV_MAPPING'
);
SET @v218_sql := IF(
    @v218_fk_act_fk_priv_mapping = 0,
    'ALTER TABLE `ACT_ID_PRIV_MAPPING` ADD CONSTRAINT `ACT_FK_PRIV_MAPPING` foreign key (PRIV_ID_)
    references ACT_ID_PRIV (ID_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 索引 ACT_IDX_PRIV_USER（幂等）
SET @v218_idx_act_idx_priv_user := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_ID_PRIV_MAPPING'
      AND index_name = 'ACT_IDX_PRIV_USER'
);
SET @v218_sql := IF(
    @v218_idx_act_idx_priv_user = 0,
    'CREATE INDEX `ACT_IDX_PRIV_USER` ON `ACT_ID_PRIV_MAPPING` (USER_ID_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 索引 ACT_IDX_PRIV_GROUP（幂等）
SET @v218_idx_act_idx_priv_group := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_ID_PRIV_MAPPING'
      AND index_name = 'ACT_IDX_PRIV_GROUP'
);
SET @v218_sql := IF(
    @v218_idx_act_idx_priv_group = 0,
    'CREATE INDEX `ACT_IDX_PRIV_GROUP` ON `ACT_ID_PRIV_MAPPING` (GROUP_ID_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 外键 ACT_UNIQ_PRIV_NAME（幂等）
SET @v218_fk_act_uniq_priv_name := (
    SELECT COUNT(*) FROM information_schema.table_constraints
    WHERE table_schema = DATABASE()
      AND table_name = 'ACT_ID_PRIV'
      AND constraint_name = 'ACT_UNIQ_PRIV_NAME'
);
SET @v218_sql := IF(
    @v218_fk_act_uniq_priv_name = 0,
    'ALTER TABLE `ACT_ID_PRIV` ADD CONSTRAINT `ACT_UNIQ_PRIV_NAME` unique (NAME_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;

-- ===== org/flowable/eventregistry/db/create/flowable.mysql.create.eventregistry.sql =====
CREATE TABLE IF NOT EXISTS FLW_EVENT_DEPLOYMENT (ID_ VARCHAR(255) NOT NULL, NAME_ VARCHAR(255) NULL, CATEGORY_ VARCHAR(255) NULL, DEPLOY_TIME_ datetime(3) NULL, TENANT_ID_ VARCHAR(255) NULL, PARENT_DEPLOYMENT_ID_ VARCHAR(255) NULL, CONSTRAINT PK_FLW_EVENT_DEPLOYMENT PRIMARY KEY (ID_));
CREATE TABLE IF NOT EXISTS FLW_EVENT_RESOURCE (ID_ VARCHAR(255) NOT NULL, NAME_ VARCHAR(255) NULL, DEPLOYMENT_ID_ VARCHAR(255) NULL, RESOURCE_BYTES_ LONGBLOB NULL, CONSTRAINT PK_FLW_EVENT_RESOURCE PRIMARY KEY (ID_));
CREATE TABLE IF NOT EXISTS FLW_EVENT_DEFINITION (ID_ VARCHAR(255) NOT NULL, NAME_ VARCHAR(255) NULL, VERSION_ INT NULL, KEY_ VARCHAR(255) NULL, CATEGORY_ VARCHAR(255) NULL, DEPLOYMENT_ID_ VARCHAR(255) NULL, TENANT_ID_ VARCHAR(255) NULL, RESOURCE_NAME_ VARCHAR(255) NULL, DESCRIPTION_ VARCHAR(255) NULL, CONSTRAINT PK_FLW_EVENT_DEFINITION PRIMARY KEY (ID_));
-- 外键 FLW_FK_EVENT_RSRC_DPL（幂等）
SET @v218_fk_flw_fk_event_rsrc_dpl := (
    SELECT COUNT(*) FROM information_schema.table_constraints
    WHERE table_schema = DATABASE()
      AND table_name = 'FLW_EVENT_RESOURCE'
      AND constraint_name = 'FLW_FK_EVENT_RSRC_DPL'
);
SET @v218_sql := IF(
    @v218_fk_flw_fk_event_rsrc_dpl = 0,
    'ALTER TABLE `FLW_EVENT_RESOURCE` ADD CONSTRAINT `FLW_FK_EVENT_RSRC_DPL` FOREIGN KEY (DEPLOYMENT_ID_) REFERENCES FLW_EVENT_DEPLOYMENT (ID_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 索引 FLW_IDX_EVENT_RSRC_DPL（幂等）
SET @v218_idx_flw_idx_event_rsrc_dpl := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'FLW_EVENT_RESOURCE'
      AND index_name = 'FLW_IDX_EVENT_RSRC_DPL'
);
SET @v218_sql := IF(
    @v218_idx_flw_idx_event_rsrc_dpl = 0,
    'CREATE INDEX `FLW_IDX_EVENT_RSRC_DPL` ON `FLW_EVENT_RESOURCE` (DEPLOYMENT_ID_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
-- 索引 ACT_IDX_EVENT_DEF_UNIQ（幂等）
SET @v218_idx_act_idx_event_def_uniq := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'FLW_EVENT_DEFINITION'
      AND index_name = 'ACT_IDX_EVENT_DEF_UNIQ'
);
SET @v218_sql := IF(
    @v218_idx_act_idx_event_def_uniq = 0,
    'CREATE UNIQUE INDEX `ACT_IDX_EVENT_DEF_UNIQ` ON `FLW_EVENT_DEFINITION` (KEY_, VERSION_, TENANT_ID_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
CREATE TABLE IF NOT EXISTS FLW_CHANNEL_DEFINITION (ID_ VARCHAR(255) NOT NULL, NAME_ VARCHAR(255) NULL, VERSION_ INT NULL, KEY_ VARCHAR(255) NULL, CATEGORY_ VARCHAR(255) NULL, TYPE_ VARCHAR(255) NULL, IMPLEMENTATION_ VARCHAR(255) NULL, DEPLOYMENT_ID_ VARCHAR(255) NULL, CREATE_TIME_ datetime(3) NULL, TENANT_ID_ VARCHAR(255) NULL, RESOURCE_NAME_ VARCHAR(255) NULL, DESCRIPTION_ VARCHAR(255) NULL, CONSTRAINT PK_FLW_CHANNEL_DEFINITION PRIMARY KEY (ID_));
-- 索引 ACT_IDX_CHANNEL_DEF_UNIQ（幂等）
SET @v218_idx_act_idx_channel_def_uniq := (
    SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'FLW_CHANNEL_DEFINITION'
      AND index_name = 'ACT_IDX_CHANNEL_DEF_UNIQ'
);
SET @v218_sql := IF(
    @v218_idx_act_idx_channel_def_uniq = 0,
    'CREATE UNIQUE INDEX `ACT_IDX_CHANNEL_DEF_UNIQ` ON `FLW_CHANNEL_DEFINITION` (KEY_, VERSION_, TENANT_ID_)',
    'SELECT 1'
);
PREPARE v218_stmt FROM @v218_sql;
EXECUTE v218_stmt;
DEALLOCATE PREPARE v218_stmt;
INSERT IGNORE INTO ACT_GE_PROPERTY
values ('eventregistry.schema.version', '8.0.0.0', 1);

