package cn.iocoder.yudao.module.ai.service.nl2sql;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * SQL 安全校验器
 *
 * <p>复用 {@code yudao-module-datalake} 的 {@code DataLakeMcpTool.validateSelectSql} 四层校验逻辑，
 * 抽取为公共组件供 NL2SQL 场景复用。
 *
 * <h3>校验规则（四层）</h3>
 * <ol>
 *   <li>非空检查</li>
 *   <li>危险字符检查（分号、注释符号 {@code ;}、{@code --}、{@code /*} 与星号斜杠、{@code @@}）</li>
 *   <li>语句前缀白名单检查（仅允许 {@code SELECT} / {@code WITH}）</li>
 *   <li>禁止关键字检查（DDL / DML 关键字，如 INSERT/UPDATE/DELETE/DROP/ALTER 等）</li>
 * </ol>
 *
 * <p>设计说明：与 DataLakeMcpTool 保持校验逻辑一致，确保 NL2SQL 生成的 SQL 与数据湖查询
 * 具有同等的安全约束。
 *
 * @author yudao
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
     * 校验 SQL 是否为安全的只读查询
     *
     * <p>校验失败时返回 false，并记录警告日志；调用方应根据返回值决定是否执行。
     *
     * @param sql 待校验的 SQL 语句
     * @return 校验通过返回 true，否则返回 false
     */
    public boolean validate(String sql) {
        if (sql == null || sql.isBlank()) {
            log.warn("[validate][SQL 为空，拒绝执行]");
            return false;
        }
        String trimmed = sql.trim();
        // 1. 危险字符检查（分号、注释符号）
        if (DANGEROUS_CHARS.matcher(trimmed).find()) {
            log.warn("[validate][SQL 包含危险字符（分号/注释），拒绝执行: {}]", truncateForLog(trimmed));
            return false;
        }
        // 2. 提取首个单词（大写）进行前缀白名单检查
        String upperSql = trimmed.toUpperCase();
        String firstWord = upperSql.split("\\s+")[0];
        if (!ALLOWED_PREFIXES.contains(firstWord)) {
            log.warn("[validate][SQL 不以 SELECT/WITH 开头，拒绝执行: {}]", truncateForLog(trimmed));
            return false;
        }
        // 3. 禁止关键字检查（在 SQL 中搜索 DDL/DML 关键字，用单词边界匹配避免误报）
        Set<String> foundKeywords = new HashSet<>();
        for (String keyword : FORBIDDEN_KEYWORDS) {
            Pattern keywordPattern = Pattern.compile("\\b" + keyword + "\\b", Pattern.CASE_INSENSITIVE);
            if (keywordPattern.matcher(trimmed).find()) {
                foundKeywords.add(keyword);
            }
        }
        if (!foundKeywords.isEmpty()) {
            log.warn("[validate][SQL 包含禁止关键字 {}，拒绝执行: {}]", foundKeywords, truncateForLog(trimmed));
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

}
