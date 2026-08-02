package cn.iocoder.yudao.module.datalake.mcp;

import cn.iocoder.yudao.framework.common.exception.enums.GlobalErrorCodeConstants;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.datalake.service.DataArchivalService;
import cn.iocoder.yudao.module.datalake.service.IcebergCatalogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 数据湖仓 MCP 工具暴露
 *
 * <p>用 {@link Tool} 注解暴露数据湖查询能力，供 AI Agent 调用。
 *
 * <h3>暴露的工具</h3>
 * <ul>
 *   <li>{@code datalake_list_tables}：列出数据湖表</li>
 *   <li>{@code datalake_query_table}：执行只读查询（白名单校验：只允许 SELECT）</li>
 *   <li>{@code datalake_get_archive_status}：查询归档状态</li>
 * </ul>
 *
 * <h3>SQL 注入防护</h3>
 * <p>{@code datalake_query_table} 工具执行严格的 SQL 白名单校验：
 * <ol>
 *   <li>仅允许 SELECT / WITH 开头的语句</li>
 *   <li>拒绝分号（防止多语句注入）</li>
 *   <li>拒绝所有 DDL/DML 关键字（INSERT/UPDATE/DELETE/DROP/ALTER/CREATE/TRUNCATE 等）</li>
 *   <li>拒绝注释符号（双连字符和斜杠星号）</li>
 * </ol>
 *
 * <p>设计说明：本类不继承 {@code TenantAwareMcpTool}，因为数据湖中的冷数据已脱离多租户上下文，
 * 归档后的数据按命名空间（namespace）组织，不按租户隔离。
 *
 * @author yudao
 */
@Component
@ConditionalOnProperty(prefix = "yudao.datalake", name = "enabled", havingValue = "true")
@ConditionalOnBean(IcebergCatalogService.class)
@RequiredArgsConstructor
@Slf4j
public class DataLakeMcpTool {

    /**
     * 允许的 SQL 语句前缀（只读查询）
     */
    private static final Set<String> ALLOWED_PREFIXES = Set.of("SELECT", "WITH");

    /**
     * 禁止的 SQL 关键字（DDL / DML / 危险操作）
     */
    private static final Set<String> FORBIDDEN_KEYWORDS = Set.of(
            "INSERT", "UPDATE", "DELETE", "DROP", "ALTER", "CREATE", "TRUNCATE",
            "MERGE", "GRANT", "REVOKE", "CALL", "EXECUTE", "RENAME",
            "VACUUM", "OPTIMIZE", "RESET", "SET", "ANALYZE"
    );

    /**
     * 危险字符模式：分号、注释符号
     */
    private static final Pattern DANGEROUS_CHARS = Pattern.compile(";|--|/\\*|\\*/|@@");

    private final IcebergCatalogService icebergCatalogService;
    private final DataArchivalService dataArchivalService;

    /**
     * SQL 白名单校验
     *
     * <p>校验规则：
     * <ol>
     *   <li>非空检查</li>
     *   <li>危险字符检查（分号、注释符号）</li>
     *   <li>语句前缀检查（仅允许 SELECT / WITH）</li>
     *   <li>禁止关键字检查（DDL / DML 关键字）</li>
     * </ol>
     *
     * @param sql 待校验的 SQL 语句
     * @return 校验通过返回 true，否则返回 false
     */
    private boolean validateSelectSql(String sql) {
        if (sql == null || sql.isBlank()) {
            log.warn("[validateSelectSql][SQL 为空，拒绝执行]");
            return false;
        }
        String trimmed = sql.trim();
        // 1. 危险字符检查
        if (DANGEROUS_CHARS.matcher(trimmed).find()) {
            log.warn("[validateSelectSql][SQL 包含危险字符（分号/注释），拒绝执行: {}]",
                    truncateForLog(trimmed));
            return false;
        }
        // 2. 提取首个单词（大写）
        String upperSql = trimmed.toUpperCase();
        String firstWord = upperSql.split("\\s+")[0];
        // 3. 前缀白名单检查
        if (!ALLOWED_PREFIXES.contains(firstWord)) {
            log.warn("[validateSelectSql][SQL 不以 SELECT/WITH 开头，拒绝执行: {}]",
                    truncateForLog(trimmed));
            return false;
        }
        // 4. 禁止关键字检查（在 SQL 中搜索 DDL/DML 关键字）
        Set<String> foundKeywords = new HashSet<>();
        for (String keyword : FORBIDDEN_KEYWORDS) {
            // 用单词边界匹配，避免误报（如列名包含 update）
            Pattern keywordPattern = Pattern.compile("\\b" + keyword + "\\b", Pattern.CASE_INSENSITIVE);
            if (keywordPattern.matcher(trimmed).find()) {
                foundKeywords.add(keyword);
            }
        }
        if (!foundKeywords.isEmpty()) {
            log.warn("[validateSelectSql][SQL 包含禁止关键字 {}，拒绝执行: {}]",
                    foundKeywords, truncateForLog(trimmed));
            return false;
        }
        return true;
    }

    /**
     * 截断 SQL 用于日志输出（避免日志过长）
     */
    private String truncateForLog(String sql) {
        return sql.length() > 200 ? sql.substring(0, 200) + "..." : sql;
    }

    // ==================== MCP 工具方法 ====================

    @Tool(name = "datalake_list_tables",
            description = "列出数据湖中指定命名空间下的所有表（List all tables in a data lake namespace）。" +
                    "返回表名列表。命名空间类似于数据库的 schema，如 ods/dwd/dws/ads。")
    @DataLakeMcpToolRequiresPermission("datalake:query")
    public List<String> listDataLakeTables(
            @ToolParam(description = "命名空间名称 / Namespace name, e.g. ods, dwd, dws, ads") String namespace) {
        log.info("[listDataLakeTables][查询命名空间 {} 下的表]", namespace);
        try {
            List<String> tables = icebergCatalogService.listTables(namespace);
            return tables != null ? tables : new ArrayList<>();
        } catch (Exception e) {
            log.error("[listDataLakeTables][查询失败: namespace={}]", namespace, e);
            return new ArrayList<>();
        }
    }

    @Tool(name = "datalake_query_table",
            description = "对数据湖执行只读 SELECT 查询（Execute a read-only SELECT query on the data lake）。" +
                    "安全约束：仅允许 SELECT / WITH 开头的语句，拒绝所有 DDL（CREATE/ALTER/DROP）和 DML（INSERT/UPDATE/DELETE）。" +
                    "返回查询结果列表，每行是列名到列值的 Map。建议加 LIMIT 子句控制返回行数。")
    @DataLakeMcpToolRequiresPermission("datalake:query")
    public List<Map<String, Object>> queryDataLakeTable(
            @ToolParam(description = "只读 SELECT 查询语句 / Read-only SELECT SQL, e.g. SELECT * FROM ods.mes_pro_work_order LIMIT 10") String sql) {
        log.info("[queryDataLakeTable][接收到 SQL 查询请求: {}]", truncateForLog(sql));
        // SQL 白名单校验
        if (!validateSelectSql(sql)) {
            log.warn("[queryDataLakeTable][SQL 校验失败，拒绝执行]");
            throw new IllegalArgumentException("SQL 校验失败：仅允许只读 SELECT 查询，禁止 DDL/DML 操作");
        }
        try {
            List<Map<String, Object>> result = icebergCatalogService.queryTable(sql);
            return result != null ? result : new ArrayList<>();
        } catch (Exception e) {
            log.error("[queryDataLakeTable][查询失败: sql={}]", truncateForLog(sql), e);
            throw new ServiceException(GlobalErrorCodeConstants.INTERNAL_SERVER_ERROR, "数据湖查询失败: " + e.getMessage(), e);
        }
    }

    @Tool(name = "datalake_get_archive_status",
            description = "查询指定业务表的归档状态（Get the archival status of a business table）。" +
                    "返回归档状态信息，包含状态、最后归档时间、已归档行数等。当前为占位实现，返回 PENDING 状态。")
    @DataLakeMcpToolRequiresPermission("datalake:query")
    public Map<String, Object> getArchiveStatus(
            @ToolParam(description = "业务表名 / Business table name, e.g. mes_pro_work_order, wms_inventory_log") String tableName) {
        log.info("[getArchiveStatus][查询表 {} 的归档状态]", tableName);
        try {
            Map<String, Object> status = dataArchivalService.getArchiveStatus(tableName);
            return status != null ? status : Map.of("tableName", tableName, "status", "UNKNOWN");
        } catch (Exception e) {
            log.error("[getArchiveStatus][查询失败: table={}]", tableName, e);
            return Map.of("tableName", tableName, "status", "ERROR", "message", e.getMessage());
        }
    }

}
