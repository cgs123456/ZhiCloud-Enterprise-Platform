package cn.iocoder.yudao.module.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 安全配置类
 *
 * 对应 Task 14：AI 安全审计（横切关注点）
 * 文档：.trae/specs/upgrade-tech-stack-and-ai-native/ai-security-audit.md
 *
 * 读取 {@code yudao.ai.security.*} 配置，覆盖四道防线：
 *  1. {@link TokenLimit}：Token 用量限流（按用户 / 租户 / IP 维度）
 *  2. {@link ToolAccess}：工具调用白名单 / 黑名单 / 二次确认
 *  3. {@link PromptInjection}：LLM 提示词注入防护（危险模式检测）
 *  4. {@link PiiMask}：敏感数据脱敏（PII 识别与替换）
 *
 * 配置示例见 yudao-server/src/main/resources/application.yaml 中 {@code yudao.ai.security} 注释段。
 * 所有配置项均提供默认值，未配置时不改变现有行为（默认关闭或仅告警）。
 *
 * @author 芋道源码
 */
@Configuration("yudaoAiSecurityProps") // 显式命名 yudaoAiSecurityProps，避免与 framework.security.config.SecurityConfiguration 的 bean 名 aiSecurityConfiguration 冲突
@ConfigurationProperties(prefix = "yudao.ai.security")
@Data
public class AiSecurityConfiguration {

    /**
     * Token 用量限流配置
     */
    private TokenLimit tokenLimit = new TokenLimit();

    /**
     * 工具调用白名单 / 黑名单 / 二次确认配置
     */
    private ToolAccess toolAccess = new ToolAccess();

    /**
     * LLM 提示词注入防护配置
     */
    private PromptInjection promptInjection = new PromptInjection();

    /**
     * 敏感数据脱敏配置
     */
    private PiiMask piiMask = new PiiMask();

    /**
     * Token 用量限流配置（SubTask 14.3）
     *
     * 三维度限流：按用户（每小时）、按租户（每天）、按 IP（每分钟），超限返回 429。
     */
    @Data
    public static class TokenLimit {

        /**
         * 是否启用 Token 限流
         */
        private boolean enabled = false;

        /**
         * 每用户每小时 Token 上限，默认 10000
         */
        private long perUserHour = 10000L;

        /**
         * 每租户每天 Token 上限，默认 100000
         */
        private long perTenantDay = 100000L;

        /**
         * 每 IP 每分钟 Token 上限，默认 1000
         */
        private long perIpMinute = 1000L;

        /**
         * 跳过限流的角色编码（如 superadmin），多个用逗号分隔
         */
        private String bypassRoles = "superadmin";

    }

    /**
     * 工具调用白名单 / 黑名单 / 二次确认配置（SubTask 14.1）
     *
     * 默认拒绝：未在白名单登记的 @Tool 不暴露给 LLM。
     * 写入类工具默认黑名单，敏感工具需二次确认。
     */
    @Data
    public static class ToolAccess {

        /**
         * 是否启用工具白名单校验
         */
        private boolean whitelistEnabled = true;

        /**
         * 工具白名单（允许 LLM 调用的工具名 / 方法名），未配置时不做白名单过滤
         */
        private List<String> whitelist = new ArrayList<>();

        /**
         * 工具黑名单（默认不暴露给 LLM 的工具名 / 方法名模式，如 delete*、remove*）
         */
        private List<String> blacklist = new ArrayList<>();

        /**
         * 二次确认工具名模式（如 update*Inventory*、*Order*），命中需用户确认后执行
         */
        private List<String> confirmPatterns = new ArrayList<>();

        /**
         * 二次确认 token 有效期（秒），默认 300 秒（5 分钟）
         */
        private long confirmTokenTtlSeconds = 300L;

    }

    /**
     * LLM 提示词注入防护配置（SubTask 14.2）
     *
     * 用户输入与系统提示词隔离，工具调用指令白名单匹配，危险模式检测。
     */
    @Data
    public static class PromptInjection {

        /**
         * 是否启用危险模式检测
         */
        private boolean enabled = false;

        /**
         * 危险模式正则黑名单（命中即拦截或告警）
         */
        private List<String> patterns = new ArrayList<>();

        /**
         * 命中处置策略：block（拦截，返回 400）或 warn（放行但记录告警日志）
         */
        private String action = "block";

        /**
         * 单用户每分钟告警阈值，超过则升级为限流
         */
        private int alertThresholdPerMinute = 5;

        /**
         * 预编译的正则 Pattern 缓存（懒加载，避免每次调用都编译）
         * <p>使用 volatile + 双重检查锁保证线程安全。
         */
        private transient volatile List<java.util.regex.Pattern> compiledPatterns;

        /**
         * 获取预编译的正则 Pattern 列表。
         * <p>首次调用时将 {@link #patterns} 字符串列表编译为 Pattern 并缓存。
         *
         * @return 预编译 Pattern 列表，若 patterns 为空返回空列表
         */
        public List<java.util.regex.Pattern> getCompiledPatterns() {
            if (compiledPatterns != null) {
                return compiledPatterns;
            }
            synchronized (this) {
                if (compiledPatterns != null) {
                    return compiledPatterns;
                }
                if (patterns == null || patterns.isEmpty()) {
                    compiledPatterns = java.util.Collections.emptyList();
                    return compiledPatterns;
                }
                List<java.util.regex.Pattern> result = new java.util.ArrayList<>(patterns.size());
                for (String pattern : patterns) {
                    try {
                        result.add(java.util.regex.Pattern.compile(pattern));
                    } catch (java.util.regex.PatternSyntaxException e) {
                        // 非法正则跳过，避免影响其他 patterns
                        org.slf4j.LoggerFactory.getLogger("AiSecurityConfiguration")
                                .warn("[getCompiledPatterns][正则编译失败，已跳过，pattern={}, msg={}]", pattern, e.getMessage());
                    }
                }
                compiledPatterns = java.util.Collections.unmodifiableList(result);
                return compiledPatterns;
            }
        }

    }

    /**
     * 敏感数据脱敏配置（SubTask 14.4）
     *
     * PII 识别（手机号、身份证、邮箱、银行卡），送入 LLM 前脱敏，日志脱敏。
     */
    @Data
    public static class PiiMask {

        /**
         * 是否启用 PII 脱敏
         */
        private boolean enabled = false;

        /**
         * 是否对手机号脱敏（1[3-9]\d{9} → 138****1234）
         */
        private boolean maskPhone = true;

        /**
         * 是否对身份证脱敏（保留前 6 后 4）
         */
        private boolean maskIdCard = true;

        /**
         * 是否对邮箱脱敏（保留首字符与域名）
         */
        private boolean maskEmail = true;

        /**
         * 是否对银行卡脱敏（保留后 4）
         */
        private boolean maskBankCard = true;

        /**
         * RAG 文档入库命中 PII 时的处置：mask（脱敏后入库）或 reject（拒绝入库）
         */
        private String ragAction = "mask";

    }

}
