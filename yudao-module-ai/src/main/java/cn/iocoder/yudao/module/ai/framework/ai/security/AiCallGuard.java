package cn.iocoder.yudao.module.ai.framework.ai.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.ai.enums.ErrorCodeConstants.AI_CALL_CIRCUIT_OPEN;
import static cn.iocoder.yudao.module.ai.enums.ErrorCodeConstants.AI_CALL_TIMEOUT;

/**
 * AI 调用保护器：为 LLM 调用提供超时控制和熔断保护
 *
 * <p>背景：当前 {@code ChatClient.prompt().call()} 同步调用 LLM 时，缺少超时与熔断保护。
 * 当 LLM API 不可用或响应缓慢时，请求线程会被长时间阻塞，可能引发雪崩效应。
 * 本工具通过 {@link CompletableFuture#orTimeout} 实现超时控制，通过计数熔断器实现失败快速熔断。</p>
 *
 * <p>熔断器状态机：</p>
 * <ul>
 *   <li>CLOSED（正常）：正常放行调用，统计连续失败次数</li>
 *   <li>OPEN（熔断）：连续失败达到阈值后打开，直接拒绝调用</li>
 *   <li>HALF_OPEN（半开）：熔断恢复时间到达后，放行单次探测调用，成功则恢复 CLOSED，失败则重新进入 OPEN</li>
 * </ul>
 *
 * <p>线程安全：使用 {@link AtomicInteger} 和 {@link AtomicLong} 保证计数与时间戳的线程安全。</p>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * @Resource
 * private AiCallGuard aiCallGuard;
 *
 * public String chat(ChatClient chatClient, String userMessage) {
 *     return aiCallGuard.callWithGuard(() ->
 *         chatClient.prompt().user(userMessage).call().content()
 *     );
 * }
 * }</pre>
 *
 * @author 芋道源码
 */
@Component
@Slf4j
public class AiCallGuard {

    /** 默认超时时间：60 秒 */
    private static final long DEFAULT_TIMEOUT_SECONDS = 60;
    /** 默认连续失败阈值：5 次后触发熔断 */
    private static final int DEFAULT_FAILURE_THRESHOLD = 5;
    /** 默认熔断恢复时间：30 秒 */
    private static final long DEFAULT_RECOVERY_SECONDS = 30;

    /** 当前连续失败计数 */
    private final AtomicInteger failureCount = new AtomicInteger(0);
    /** 熔断打开时间戳（毫秒，0 表示未熔断） */
    private final AtomicLong circuitOpenTime = new AtomicLong(0);

    /** 调用超时时间（秒） */
    private final long timeoutSeconds;
    /** 触发熔断的连续失败次数 */
    private final int failureThreshold;
    /** 熔断后恢复所需时间（秒） */
    private final long recoverySeconds;

    /**
     * 默认构造方法：使用默认参数（60s 超时 / 5 次失败 / 30s 恢复）
     */
    public AiCallGuard() {
        this(DEFAULT_TIMEOUT_SECONDS, DEFAULT_FAILURE_THRESHOLD, DEFAULT_RECOVERY_SECONDS);
    }

    /**
     * 自定义参数构造方法
     *
     * @param timeoutSeconds   超时时间（秒），必须大于 0
     * @param failureThreshold 触发熔断的连续失败次数，必须大于 0
     * @param recoverySeconds  熔断恢复时间（秒），必须大于 0
     */
    public AiCallGuard(long timeoutSeconds, int failureThreshold, long recoverySeconds) {
        if (timeoutSeconds <= 0) {
            throw new IllegalArgumentException("timeoutSeconds 必须大于 0，当前：" + timeoutSeconds);
        }
        if (failureThreshold <= 0) {
            throw new IllegalArgumentException("failureThreshold 必须大于 0，当前：" + failureThreshold);
        }
        if (recoverySeconds <= 0) {
            throw new IllegalArgumentException("recoverySeconds 必须大于 0，当前：" + recoverySeconds);
        }
        this.timeoutSeconds = timeoutSeconds;
        this.failureThreshold = failureThreshold;
        this.recoverySeconds = recoverySeconds;
    }

    /**
     * 在熔断与超时保护下执行 LLM 调用
     *
     * <p>处理逻辑：</p>
     * <ol>
     *   <li>检查熔断器状态，若 OPEN 则直接抛出 {@link ServiceException}（{@link ErrorCodeConstants#AI_CALL_CIRCUIT_OPEN}）</li>
     *   <li>使用 {@link CompletableFuture#supplyAsync} 异步执行同步调用，并通过 {@link CompletableFuture#orTimeout} 设置超时</li>
     *   <li>调用成功：{@link #recordSuccess()} 复位失败计数</li>
     *   <li>调用超时：{@link #recordFailure()} 累加失败，并抛出 {@link ServiceException}（{@link ErrorCodeConstants#AI_CALL_TIMEOUT}）</li>
     *   <li>调用异常：{@link #recordFailure()} 累加失败，并原样抛出底层异常</li>
     * </ol>
     *
     * @param call 同步 LLM 调用（如 {@code chatClient.prompt().call().content()}）
     * @param <T>  返回值类型
     * @return 调用结果
     * @throws ServiceException 熔断打开或调用超时时抛出
     */
    public <T> T callWithGuard(Supplier<T> call) {
        // 1. 熔断检查
        if (isOpen()) {
            log.warn("[callWithGuard] 熔断器处于 OPEN 状态，拒绝调用");
            throw exception(AI_CALL_CIRCUIT_OPEN);
        }

        // 2. 异步执行 + 超时控制
        CompletableFuture<T> future = CompletableFuture.supplyAsync(call)
                .orTimeout(timeoutSeconds, TimeUnit.SECONDS);
        try {
            T result = future.join();
            recordSuccess();
            return result;
        } catch (CompletionException e) {
            Throwable cause = e.getCause();
            // 3. 超时
            if (cause instanceof TimeoutException) {
                recordFailure();
                log.warn("[callWithGuard] LLM 调用超时，超过 {} 秒", timeoutSeconds);
                throw exception(AI_CALL_TIMEOUT);
            }
            // 4. 其他异常：记录失败后原样抛出
            recordFailure();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new RuntimeException(cause);
        }
    }

    /**
     * 检查熔断器是否处于 OPEN 状态
     *
     * <p>若 OPEN 时间已超过 {@link #recoverySeconds}，则尝试进入 HALF_OPEN 状态（复位 circuitOpenTime）。</p>
     *
     * @return true 表示熔断器打开，调用应被拒绝；false 表示放行
     */
    public boolean isOpen() {
        long openTime = circuitOpenTime.get();
        if (openTime == 0) {
            return false;
        }
        long elapsedSeconds = (System.currentTimeMillis() - openTime) / 1000L;
        if (elapsedSeconds >= recoverySeconds) {
            // 半开：通过 CAS 复位 circuitOpenTime，允许一次探测调用
            if (circuitOpenTime.compareAndSet(openTime, 0)) {
                log.info("[isOpen] 熔断器进入 HALF_OPEN 状态，允许探测调用");
                return false;
            }
            return true;
        }
        return true;
    }

    /**
     * 记录一次成功调用：复位失败计数，关闭熔断器
     */
    public void recordSuccess() {
        failureCount.set(0);
        circuitOpenTime.set(0);
    }

    /**
     * 记录一次失败调用：累加失败计数，达到阈值后打开熔断器
     */
    public void recordFailure() {
        int count = failureCount.incrementAndGet();
        if (count >= failureThreshold) {
            long now = System.currentTimeMillis();
            circuitOpenTime.set(now);
            log.error("[recordFailure] 连续失败 {} 次达到阈值 {}，熔断器 OPEN，将在 {} 秒后尝试恢复",
                    count, failureThreshold, recoverySeconds);
        }
    }

}
