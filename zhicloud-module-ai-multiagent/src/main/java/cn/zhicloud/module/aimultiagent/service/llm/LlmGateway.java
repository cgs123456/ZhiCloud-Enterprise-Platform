package cn.zhicloud.module.aimultiagent.service.llm;

import cn.zhicloud.module.aimultiagent.config.ChatClientHelper;
import cn.zhicloud.module.aimultiagent.config.MultiAgentProperties;
import cn.zhicloud.module.aimultiagent.service.metrics.MultiAgentMetrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import jakarta.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * LLM 调用统一门户
 *
 * <p>所有 Agent（Supervisor / Worker / ReAct）的 LLM 调用收敛到此组件，统一处理：
 * <ol>
 *   <li>Prompt 注入防护 —— 前置护栏 system note（{@link PromptInjectionGuard}）；</li>
 *   <li>重试 —— 对超时 / 5xx / 429 等瞬时失败按指数退避重试；</li>
 *   <li>限流 + 熔断 —— 进程内令牌桶限流与失败窗口熔断器；</li>
 *   <li>可观测性 —— 通过 {@link MultiAgentMetrics} 记录耗时、Token、成功率；</li>
 *   <li>Token 估算 —— 复用 CJK 友好的 {@link #estimateTokens}。</li>
 * </ol>
 *
 * <p>失败语义：重试耗尽 / 限流 / 熔断 / LLM 不可用时抛出 {@link LlmCallException}，
 * 调用方维持既有的 {@code try-catch → AgentResult.success=false} 行为。
 *
 * @author zhicloud
 */
@Slf4j
@Component
public class LlmGateway {

    /**
     * LLM 调用线程编号（用于线程名生成）
     */
    private static final AtomicInteger LLM_THREAD_COUNTER = new AtomicInteger(0);

    /**
     * LLM 调用专属有界线程池（类内静态）：
     * core8/max32/queue200，替代 CompletableFuture 默认的公共 ForkJoinPool——
     * 公共池线程数有限且超时后任务仍会占用线程；本池配合 future.cancel(true) 主动中断。
     */
    private static final ThreadPoolExecutor LLM_CALL_EXECUTOR = new ThreadPoolExecutor(
            8, 32, 60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(200),
            runnable -> {
                Thread thread = new Thread(runnable);
                thread.setName("llm-gateway-" + LLM_THREAD_COUNTER.incrementAndGet());
                thread.setDaemon(true);
                return thread;
            });

    private final ChatClientHelper chatClientHelper;
    private final MultiAgentProperties properties;
    private final MultiAgentMetrics metrics;

    private RetryTemplate retryTemplate;

    // 限流：令牌桶
    private final AtomicLong bucket = new AtomicLong(0);
    private volatile long lastRefillNanos = System.nanoTime();

    // 熔断：失败窗口
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private final AtomicLong openUntilNanos = new AtomicLong(0);

    public LlmGateway(ChatClientHelper chatClientHelper, MultiAgentProperties properties, MultiAgentMetrics metrics) {
        this.chatClientHelper = chatClientHelper;
        this.properties = properties;
        this.metrics = metrics;
    }

    @PostConstruct
    public void init() {
        MultiAgentProperties.Llm llm = properties.getLlm();
        // 重试策略：仅对瞬时失败重试，不对 LLM 不可用 / 确定性解析错误重试
        Map<Class<? extends Throwable>, Boolean> retryable = new HashMap<>(8);
        retryable.put(LlmTransientException.class, true);
        retryable.put(TimeoutException.class, true);
        retryable.put(HttpServerErrorException.class, true);
        retryable.put(HttpClientErrorException.TooManyRequests.class, true);
        RetryPolicy retryPolicy = new SimpleRetryPolicy(llm.getRetry().getMaxAttempts(), retryable, true);

        ExponentialBackOffPolicy backOff = new ExponentialBackOffPolicy();
        backOff.setInitialInterval(llm.getRetry().getBackoffBaseMs());
        backOff.setMultiplier(2.0);
        backOff.setMaxInterval(llm.getRetry().getBackoffBaseMs() * 8);

        RetryTemplate template = new RetryTemplate();
        template.setRetryPolicy(retryPolicy);
        template.setBackOffPolicy(backOff);
        this.retryTemplate = template;

        // 令牌桶初始容量 = 每秒允许调用数
        long capacity = Math.max(1, llm.getRateLimitPerSecond());
        this.bucket.set(capacity);
        this.lastRefillNanos = System.nanoTime();
    }

    /**
     * 调用 LLM（使用默认超时），自动前置注入防护。
     */
    public String call(String systemPrompt, String userMessage, String callerTag) {
        return call(null, systemPrompt, userMessage, callerTag, properties.getLlm().getTimeoutSeconds());
    }

    /**
     * 调用 LLM（指定超时），自动前置注入防护。
     */
    public String call(String systemPrompt, String userMessage, String callerTag, int timeoutSeconds) {
        return call(null, systemPrompt, userMessage, callerTag, timeoutSeconds);
    }

    /**
     * 调用 LLM（指定 client 与超时），自动前置注入防护。
     *
     * <p>{@code client} 为 null 时由 {@link ChatClientHelper} 按需构建；否则沿用调用方已获取的 ChatClient
     * （如 ReAct 优先使用的注入 Bean），以保留既有偏好。
     */
    public String call(ChatClient client, String systemPrompt, String userMessage, String callerTag, int timeoutSeconds) {
        long startNanos = System.nanoTime();
        // 1. 限流
        if (!tryAcquire()) {
            long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
            metrics.recordLlmCall(callerTag, elapsed, false, -1, null);
            throw new LlmRateLimitException("LLM 调用被限流（超过 " + properties.getLlm().getRateLimitPerSecond() + " 次/秒）");
        }
        // 2. 熔断
        if (!allowRequest()) {
            long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
            metrics.recordLlmCall(callerTag, elapsed, false, -1, null);
            throw new LlmCircuitOpenException("LLM 熔断器已打开，进入冷却（"
                    + properties.getLlm().getCircuit().getCooldownSeconds() + "s）");
        }
        // 3. 重试调用（前置护栏 system note）
        String guardedSystem = PromptInjectionGuard.GUARD_SYSTEM_NOTE + "\n\n" + (systemPrompt == null ? "" : systemPrompt);
        final int timeout = timeoutSeconds > 0 ? timeoutSeconds : properties.getLlm().getTimeoutSeconds();
        try {
            CallOutcome outcome = retryTemplate.execute(
                    (RetryCallback<CallOutcome, LlmTransientException>) ctx -> doCall(client, guardedSystem, userMessage, timeout));
            long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
            metrics.recordLlmCall(callerTag, elapsed, true, outcome.tokens(), outcome.content());
            onSuccess();
            return outcome.content();
        } catch (Exception e) {
            long elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNanos);
            metrics.recordLlmCall(callerTag, elapsed, false, -1, null);
            onFailure();
            throw new LlmCallException("LLM 调用失败（重试耗尽）", e);
        }
    }

    private CallOutcome doCall(ChatClient providedClient, String system, String user, int timeoutSeconds) {
        ChatClient chatClient = providedClient != null ? providedClient : chatClientHelper.getChatClient();
        // 提交到专属有界线程池执行（队列满时快速失败，由上层重试）
        Future<ChatResponse> future;
        try {
            future = LLM_CALL_EXECUTOR.submit(() ->
                    chatClient.prompt().system(system).user(user).call().chatResponse());
        } catch (RejectedExecutionException e) {
            throw new LlmTransientException("LLM 调用排队已满（队列上限 200）", e);
        }
        try {
            ChatResponse response = future.get(timeoutSeconds, TimeUnit.SECONDS);
            String content = response.getResult().getOutput().getText();
            long tokens = extractTokens(response);
            return new CallOutcome(content == null ? "" : content, tokens);
        } catch (TimeoutException te) {
            // 超时后中断底层调用线程，避免任务在后台无限占用线程池
            future.cancel(true);
            throw new LlmTransientException("LLM 调用超时（>" + timeoutSeconds + "s）", te);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            future.cancel(true);
            throw new LlmTransientException("LLM 调用被中断", ie);
        } catch (ExecutionException ee) {
            Throwable cause = ee.getCause() != null ? ee.getCause() : ee;
            throw new LlmTransientException("LLM 调用失败", cause);
        }
    }

    private long extractTokens(ChatResponse response) {
        try {
            if (response.getMetadata() != null && response.getMetadata().getUsage() != null
                    && response.getMetadata().getUsage().getTotalTokens() != null) {
                return response.getMetadata().getUsage().getTotalTokens();
            }
        } catch (Exception ignored) {
            // 部分模型不返回 usage，回退估算
        }
        return -1;
    }

    /**
     * 估算 Token 数（CJK 友好）：中文约 1 token/字，英文约 4 字符/token。
     */
    public int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        int cjk = 0, ascii = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if ((c >= 0x4E00 && c <= 0x9FFF) || (c >= 0x3000 && c <= 0x303F) || (c >= 0xFF00 && c <= 0xFFEF)) {
                cjk++;
            } else {
                ascii++;
            }
        }
        return cjk + ascii / 4;
    }

    // ==================== 限流（令牌桶） ====================

    private boolean tryAcquire() {
        long now = System.nanoTime();
        long refillNanos = TimeUnit.SECONDS.toNanos(1);
        long last = lastRefillNanos;
        if (now - last >= refillNanos) {
            synchronized (this) {
                if (now - lastRefillNanos >= refillNanos) {
                    long capacity = Math.max(1, properties.getLlm().getRateLimitPerSecond());
                    lastRefillNanos = now;
                    bucket.set(Math.min(capacity, bucket.get() + capacity));
                }
            }
        }
        return bucket.getAndUpdate(v -> v > 0 ? v - 1 : v) > 0;
    }

    // ==================== 熔断（失败窗口） ====================

    private boolean allowRequest() {
        long openUntil = openUntilNanos.get();
        if (openUntil == 0) {
            return true;
        }
        if (System.nanoTime() > openUntil) {
            openUntilNanos.set(0);
            consecutiveFailures.set(0);
            return true;
        }
        return false;
    }

    private void onSuccess() {
        consecutiveFailures.set(0);
        openUntilNanos.set(0);
    }

    private void onFailure() {
        if (consecutiveFailures.incrementAndGet() >= properties.getLlm().getCircuit().getFailureThreshold()) {
            openUntilNanos.set(System.nanoTime()
                    + TimeUnit.SECONDS.toNanos(properties.getLlm().getCircuit().getCooldownSeconds()));
        }
    }

    private record CallOutcome(String content, long tokens) {
    }

    // ==================== 异常类型 ====================

    /**
     * LLM 调用统一失败异常（重试耗尽 / 限流 / 熔断 均由此或其子类向上抛出）
     */
    public static class LlmCallException extends RuntimeException {

        public LlmCallException(String message) {
            super(message);
        }

        public LlmCallException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * LLM 瞬时失败（可重试）：超时 / 网络抖动 / 5xx / 429
     */
    public static class LlmTransientException extends RuntimeException {

        public LlmTransientException(String message) {
            super(message);
        }

        public LlmTransientException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * LLM 调用被限流
     */
    public static class LlmRateLimitException extends RuntimeException {

        public LlmRateLimitException(String message) {
            super(message);
        }
    }

    /**
     * LLM 熔断器已打开
     */
    public static class LlmCircuitOpenException extends RuntimeException {

        public LlmCircuitOpenException(String message) {
            super(message);
        }
    }
}
