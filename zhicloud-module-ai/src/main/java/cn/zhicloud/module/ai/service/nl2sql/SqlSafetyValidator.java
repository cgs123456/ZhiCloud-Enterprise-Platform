package cn.zhicloud.module.ai.service.nl2sql;

import cn.zhicloud.framework.common.exception.ServiceException;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.util.TablesNamesFinder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.ai.enums.ErrorCodeConstants.NL2SQL_SAFETY_CHECK_REJECTED;

/**
 * SQL 安全校验器
 *
 * <p>复用 {@code zhicloud-module-datalake} 的 {@code DataLakeMcpTool.validateSelectSql} 四层校验逻辑，
 * 抽取为公共组件供 NL2SQL 场景复用。
 *
 * <h3>校验规则</h3>
 * <ol>
 *   <li>非空检查 + 危险字符检查（分号、注释符号 {@code ;}、{@code --}、{@code /*} 与星号斜杠、{@code @@}）</li>
 *   <li>去注释/分号后的预检：拒绝 {@code INTO OUTFILE / INTO DUMPFILE / LOAD_FILE} 等文件读写</li>
 *   <li>语句前缀白名单检查（仅允许 {@code SELECT} / {@code WITH}）</li>
 *   <li>禁止关键字检查（DDL / DML 关键字，如 INSERT/UPDATE/DELETE/DROP/ALTER 等）</li>
 *   <li>JSqlParser AST 解析：强制语句为 SELECT；并用 TablesNamesFinder 提取表名，
 *       拒绝 information_schema / mysql / performance_schema / pg_catalog 及一切 system_ 前缀表</li>
 * </ol>
 *
 * <p>设计说明：与 DataLakeMcpTool 保持校验逻辑一致，确保 NL2SQL 生成的 SQL 与数据湖查询
 * 具有同等的安全约束。校验失败抛出带原因的 ServiceException。
 *
 * @author zhicloud
 */
@Component
@Slf4j
public class SqlSafetyValidator {

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

    /**
     * 文件读写预检模式：INTO OUTFILE / INTO DUMPFILE / LOAD_FILE(...)（去注释后匹配，防注释混淆绕过）
     */
    private static final Pattern FILE_ACCESS_PATTERN = Pattern.compile(
            "\\bINTO\\s+(OUTFILE|DUMPFILE)\\b|\\bLOAD_FILE\\s*\\(", Pattern.CASE_INSENSITIVE);

    /**
     * 禁止访问的系统库（小写匹配，兼容 schema.table 前缀形式）
     */
    private static final Set<String> FORBIDDEN_TABLES = Set.of(
            "information_schema", "mysql", "performance_schema", "pg_catalog");

    /**
     * 一切 system_ 前缀表均视为系统表，拒绝访问
     */
    private static final String SYSTEM_TABLE_PREFIX = "system_";

    /**
     * 租户占位符：JSqlParser 无法解析 ${tenantId}，AST 解析前需替换为字面量
     */
    private static final String TENANT_PLACEHOLDER = "${tenantId}";

    /**
     * 校验 SQL 是否为安全的只读查询
     *
     * <p>校验失败时抛出带原因的 ServiceException，由调用方决定如何响应。
     *
     * @param sql 待校验的 SQL 语句
     * @throws ServiceException 任一层校验未通过时抛出，message 携带具体原因
     */
    public void validate(String sql) {
        if (sql == null || sql.isBlank()) {
            log.warn("[validate][SQL 为空，拒绝执行]");
            throw reject("SQL 为空");
        }
        String trimmed = sql.trim();
        // 1. 危险字符检查（分号、注释符号）
        if (DANGEROUS_CHARS.matcher(trimmed).find()) {
            log.warn("[validate][SQL 包含危险字符（分号/注释），拒绝执行: {}]", truncateForLog(trimmed));
            throw reject("SQL 包含危险字符（分号/注释）");
        }
        // 2. 去除注释与分号后的预检：拒绝文件读写（防 INTO OUTFILE/DUMPFILE、LOAD_FILE 写盘或读文件）
        String stripped = stripCommentsAndSemicolons(trimmed);
        Matcher fileAccessMatcher = FILE_ACCESS_PATTERN.matcher(stripped);
        if (fileAccessMatcher.find()) {
            log.warn("[validate][SQL 包含文件读写操作，拒绝执行: {}]", truncateForLog(trimmed));
            throw reject("检测到文件读写操作（INTO OUTFILE/DUMPFILE 或 LOAD_FILE）");
        }
        // 3. 提取首个单词（大写）进行前缀白名单检查
        String upperSql = trimmed.toUpperCase();
        String firstWord = upperSql.split("\\s+")[0];
        if (!ALLOWED_PREFIXES.contains(firstWord)) {
            log.warn("[validate][SQL 不以 SELECT/WITH 开头，拒绝执行: {}]", truncateForLog(trimmed));
            throw reject("仅允许 SELECT/WITH 开头的只读查询");
        }
        // 4. 禁止关键字检查（在 SQL 中搜索 DDL/DML 关键字，用单词边界匹配避免误报）
        Set<String> foundKeywords = new HashSet<>();
        for (String keyword : FORBIDDEN_KEYWORDS) {
            Pattern keywordPattern = Pattern.compile("\\b" + keyword + "\\b", Pattern.CASE_INSENSITIVE);
            if (keywordPattern.matcher(trimmed).find()) {
                foundKeywords.add(keyword);
            }
        }
        if (!foundKeywords.isEmpty()) {
            log.warn("[validate][SQL 包含禁止关键字 {}，拒绝执行: {}]", foundKeywords, truncateForLog(trimmed));
            throw reject("包含禁止关键字" + foundKeywords);
        }
        // 5. JSqlParser AST 解析：强制为 SELECT（语法层兜底，防关键字混淆/变形绕过正则）
        Statement statement;
        try {
            statement = CCJSqlParserUtil.parse(stripped.replace(TENANT_PLACEHOLDER, "0"));
        } catch (Exception e) {
            log.warn("[validate][SQL 解析失败，拒绝执行: {}, 原因={}]", truncateForLog(trimmed), e.getMessage());
            throw reject("SQL 解析失败：" + e.getMessage());
        }
        if (!(statement instanceof Select)) {
            log.warn("[validate][SQL 非只读 SELECT 语句，拒绝执行: {}]", truncateForLog(trimmed));
            throw reject("仅允许只读 SELECT 查询");
        }
        // 6. 表名白名单：提取 AST 中所有表名，拒绝系统库与 system_ 前缀表
        Set<String> tableNames = new TablesNamesFinder().getTables(statement);
        for (String tableName : tableNames) {
            String normalized = normalizeIdentifier(tableName);
            for (String segment : normalized.split("\\.")) {
                if (FORBIDDEN_TABLES.contains(segment) || segment.startsWith(SYSTEM_TABLE_PREFIX)) {
                    log.warn("[validate][SQL 访问系统表 {}，拒绝执行: {}]", tableName, truncateForLog(trimmed));
                    throw reject("禁止访问系统表：" + tableName);
                }
            }
        }
    }

    /**
     * 构造带原因的安全校验异常（错误码模板 "SQL 安全校验未通过：{}"）
     */
    private static ServiceException reject(String reason) {
        return exception(NL2SQL_SAFETY_CHECK_REJECTED, reason);
    }

    /**
     * 去除块注释 / 行注释与分号（用于混淆绕过预检），不影响原始 SQL 的其他内容
     */
    private static String stripCommentsAndSemicolons(String sql) {
        String noBlockComments = sql.replaceAll("/\\*.*?\\*/", " ");
        String noLineComments = noBlockComments.replaceAll("--[^\\n]*", " ");
        return noLineComments.replace(';', ' ');
    }

    /**
     * 标识符归一化：去除反引号 / 双引号包裹并转小写
     */
    private static String normalizeIdentifier(String identifier) {
        return identifier.replace("`", "").replace("\"", "").toLowerCase();
    }

    /**
     * 截断 SQL 用于日志输出（避免日志过长）
     */
    private String truncateForLog(String sql) {
        return sql.length() > 200 ? sql.substring(0, 200) + "..." : sql;
    }

}
