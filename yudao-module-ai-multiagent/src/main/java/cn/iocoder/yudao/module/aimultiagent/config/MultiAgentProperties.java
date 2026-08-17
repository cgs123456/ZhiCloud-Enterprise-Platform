package cn.iocoder.yudao.module.aimultiagent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 多 Agent 编排模块配置属性
 *
 * <p>前缀 {@code yudao.aimultiagent}，用于把散落在各 Agent 中的魔法数
 * （LLM 超时、重试、限流、熔断、工具分页上限、拓扑深度 / Token 预算、ReAct 步数等）集中配置化，
 * 运行时可通过 {@code application.yml} 覆盖；默认值与现有硬编码保持一致，切换后行为不变。
 *
 * @author yudao
 */
@ConfigurationProperties(prefix = "yudao.aimultiagent")
public class MultiAgentProperties {

    /**
     * LLM 调用相关配置
     */
    private final Llm llm = new Llm();

    /**
     * Worker 工具调用相关配置
     */
    private final Worker worker = new Worker();

    /**
     * Supervisor 编排相关配置
     */
    private final Supervisor supervisor = new Supervisor();

    /**
     * ReAct 循环相关配置
     */
    private final React react = new React();

    public Llm getLlm() {
        return llm;
    }

    public Worker getWorker() {
        return worker;
    }

    public Supervisor getSupervisor() {
        return supervisor;
    }

    public React getReact() {
        return react;
    }

    public static class Llm {

        /**
         * 单次 LLM 调用超时（秒）。超时即视为本次尝试失败，触发重试或最终失败。
         */
        private int timeoutSeconds = 60;

        /**
         * LLM 调用限流：每秒允许的最大调用数（令牌桶容量 = 该值，突发上限同值）。
         */
        private int rateLimitPerSecond = 5;

        /**
         * 重试配置
         */
        private final Retry retry = new Retry();

        /**
         * 熔断器配置
         */
        private final Circuit circuit = new Circuit();

        public int getTimeoutSeconds() {
            return timeoutSeconds;
        }

        public void setTimeoutSeconds(int timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
        }

        public int getRateLimitPerSecond() {
            return rateLimitPerSecond;
        }

        public void setRateLimitPerSecond(int rateLimitPerSecond) {
            this.rateLimitPerSecond = rateLimitPerSecond;
        }

        public Retry getRetry() {
            return retry;
        }

        public Circuit getCircuit() {
            return circuit;
        }

        public static class Retry {

            /**
             * 最大尝试次数（含首次）
             */
            private int maxAttempts = 3;

            /**
             * 退避初始间隔（毫秒）
             */
            private long backoffBaseMs = 1000;

            public int getMaxAttempts() {
                return maxAttempts;
            }

            public void setMaxAttempts(int maxAttempts) {
                this.maxAttempts = maxAttempts;
            }

            public long getBackoffBaseMs() {
                return backoffBaseMs;
            }

            public void setBackoffBaseMs(long backoffBaseMs) {
                this.backoffBaseMs = backoffBaseMs;
            }
        }

        public static class Circuit {

            /**
             * 连续失败达到该阈值后熔断
             */
            private int failureThreshold = 5;

            /**
             * 熔断后冷却时长（秒），冷却结束后进入半开试探
             */
            private int cooldownSeconds = 30;

            public int getFailureThreshold() {
                return failureThreshold;
            }

            public void setFailureThreshold(int failureThreshold) {
                this.failureThreshold = failureThreshold;
            }

            public int getCooldownSeconds() {
                return cooldownSeconds;
            }

            public void setCooldownSeconds(int cooldownSeconds) {
                this.cooldownSeconds = cooldownSeconds;
            }
        }
    }

    public static class Worker {

        /**
         * 工具分页默认条数
         */
        private int defaultLimit = 10;

        /**
         * 工具分页上限（防止异常大分页拖垮业务库）
         */
        private int maxLimit = 50;

        public int getDefaultLimit() {
            return defaultLimit;
        }

        public void setDefaultLimit(int defaultLimit) {
            this.defaultLimit = defaultLimit;
        }

        public int getMaxLimit() {
            return maxLimit;
        }

        public void setMaxLimit(int maxLimit) {
            this.maxLimit = maxLimit;
        }
    }

    public static class Supervisor {

        /**
         * 最大调用深度默认值（拓扑未配置时）
         */
        private int maxDepthDefault = 5;

        /**
         * Token 预算上限默认值（拓扑未配置时）
         */
        private int maxTokenBudgetDefault = 10000;

        public int getMaxDepthDefault() {
            return maxDepthDefault;
        }

        public void setMaxDepthDefault(int maxDepthDefault) {
            this.maxDepthDefault = maxDepthDefault;
        }

        public int getMaxTokenBudgetDefault() {
            return maxTokenBudgetDefault;
        }

        public void setMaxTokenBudgetDefault(int maxTokenBudgetDefault) {
            this.maxTokenBudgetDefault = maxTokenBudgetDefault;
        }
    }

    public static class React {

        /**
         * 默认步数上限
         */
        private int maxSteps = 10;

        /**
         * 默认 Token 预算
         */
        private int maxTokenBudget = 4000;

        /**
         * 默认超时（秒）
         */
        private int timeoutSeconds = 60;

        public int getMaxSteps() {
            return maxSteps;
        }

        public void setMaxSteps(int maxSteps) {
            this.maxSteps = maxSteps;
        }

        public int getMaxTokenBudget() {
            return maxTokenBudget;
        }

        public void setMaxTokenBudget(int maxTokenBudget) {
            this.maxTokenBudget = maxTokenBudget;
        }

        public int getTimeoutSeconds() {
            return timeoutSeconds;
        }

        public void setTimeoutSeconds(int timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
        }
    }
}
