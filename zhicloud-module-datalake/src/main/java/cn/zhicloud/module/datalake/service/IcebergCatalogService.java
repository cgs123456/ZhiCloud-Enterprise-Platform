package cn.zhicloud.module.datalake.service;

import java.util.List;
import java.util.Map;

/**
 * Iceberg Catalog 管理服务
 *
 * <p>通过 Trino REST API 操作 Iceberg 表。Trino 不在编译期依赖中，
 * 实现类通过 Spring {@code RestClient} 发送 HTTP 请求到 Trino 的 {@code /v1/statement} 端点。
 *
 * <p>命名空间（Namespace）对应 Iceberg 的 schema 概念，类似于数据库的 schema。
 * 表（Table）隶属于命名空间，存储 Iceberg 格式的数据文件。
 *
 * <h3>典型用法</h3>
 * <pre>
 * // 列出所有命名空间
 * List&lt;String&gt; namespaces = catalogService.listNamespaces();
 *
 * // 列出指定命名空间下的表
 * List&lt;String&gt; tables = catalogService.listTables("ods");
 *
 * // 查询表数据（只读 SELECT）
 * List&lt;Map&lt;String, Object&gt;&gt; rows = catalogService.queryTable("SELECT * FROM ods.mes_pro_work_order LIMIT 10");
 * </pre>
 *
 * @author zhicloud
 */
public interface IcebergCatalogService {

    /**
     * 列出所有命名空间
     *
     * @return 命名空间名称列表
     */
    List<String> listNamespaces();

    /**
     * 列出指定命名空间的表
     *
     * @param namespace 命名空间名称
     * @return 表名称列表
     */
    List<String> listTables(String namespace);

    /**
     * 执行查询（只读 SELECT）
     *
     * <p>安全约束：仅允许 SELECT 语句，拒绝 DDL（CREATE/ALTER/DROP）和 DML（INSERT/UPDATE/DELETE）。
     * 调用方应先进行 SQL 白名单校验。
     *
     * @param sql SELECT 查询语句
     * @return 查询结果，每行是一个列名 → 列值的 Map
     */
    List<Map<String, Object>> queryTable(String sql);

    /**
     * 创建表
     *
     * @param namespace 命名空间名称
     * @param table     表名称
     * @param schema    表结构定义（Iceberg DDL，例如 {@code (id BIGINT, name VARCHAR)}）
     */
    void createTable(String namespace, String table, String schema);

    /**
     * 获取表 schema
     *
     * @param namespace 命名空间名称
     * @param table     表名称
     * @return 表结构信息（列名 → 类型）
     */
    Map<String, String> getTableSchema(String namespace, String table);

    /**
     * 执行 DML 语句（INSERT/UPDATE/DELETE）
     *
     * <p>与 {@link #queryTable(String)} 不同，本方法用于执行写入/删除操作，不返回查询结果集，
     * 而是返回执行状态（是否成功、影响行数、错误信息）。
     *
     * <p>安全约束：调用方必须自行做 SQL 白名单校验（如表名拼接场景），
     * 避免通过 {@code ${}} 拼接引入 SQL 注入风险。
     *
     * @param sql DML 语句（INSERT/UPDATE/DELETE）
     * @return 执行结果，包含：
     *         <ul>
     *           <li>{@code success}（Boolean）：是否成功</li>
     *           <li>{@code updateCount}（Long）：影响行数（失败时为 0）</li>
     *           <li>{@code error}（String）：错误信息（成功时为 null）</li>
     *         </ul>
     */
    Map<String, Object> executeUpdate(String sql);
}
