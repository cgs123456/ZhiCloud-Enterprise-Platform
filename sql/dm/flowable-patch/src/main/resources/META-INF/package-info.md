# Flowable DM 数据库适配补丁

本补丁用于适配 Flowable 工作流引擎在达梦数据库（DM DBMS）上的运行。

## 修改内容

1. **AbstractEngineConfiguration.java**: 添加达梦数据库类型映射
   - 将 "DM DBMS" 映射到 Oracle 兼容模式（`DATABASE_TYPE_ORACLE`）
   - 达梦数据库兼容 Oracle SQL 语法，使用 Oracle 类型处理即可

2. **DmDatabase.java**: Liquibase 达梦数据库支持
   - 继承 PostgresDatabase，适配达梦的 JDBC 元数据行为

3. **BooleanType.java**: 达梦布尔类型支持
   - 映射到 VARCHAR(1) + CHECK 约束，兼容达梦数据库

## 使用说明

在 `application-dm.yml` 中配置 Flowable 引擎时，无需额外设置 `databaseType`，
补丁会自动识别达梦数据库并使用 Oracle 兼容模式。