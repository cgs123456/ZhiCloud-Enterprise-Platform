package cn.zhicloud.module.aimultiagent.service.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 多 Agent 编排可观测性指标
 *
 * <p>基于 Micrometer（zhicloud-server 默认集成 Actuator）。当 {@link MeterRegistry} 不可用时
 * （如模块独立运行、未引入 Actuator），所有方法空操作，不影响主流程。
 *
 * @author zhicloud
 */
@Slf4j
@Component
public class MultiAgentMetrics {

    @Nullable
    private final MeterRegistry registry;

    @Autowired(required = false)
    public MultiAgentMetrics(@Nullable MeterRegistry registry) {
        this.registry = registry;
        if (registry == null) {
            log.warn("[MultiAgentMetrics][MeterRegistry 不可用，指标采集关闭]");
        }
    }

    /**
     * 记录一次 LLM 调用（耗时 / 成功率 / Token 消耗）。
     *
     * @param caller         调用方标识（supervisor / react / worker:xxx）
     * @param elapsedMs      耗时（毫秒）
     * @param success        是否成功
     * @param tokens         真实 Token 消耗（&lt;0 表示未知，回退估算）
     * @param contentFallback 真实用量缺失时的文本回退估算来源
     */
    public void recordLlmCall(String caller, long elapsedMs, boolean success, long tokens, String contentFallback) {
        if (registry == null) {
            return;
        }
        Timer.builder("multiagent.llm.call")
                .tag("caller", caller)
                .tag("success", String.valueOf(success))
                .register(registry)
                .record(elapsedMs, TimeUnit.MILLISECONDS);
        long tokenCount = tokens >= 0 ? tokens : estimateFallback(contentFallback);
        if (tokenCount > 0) {
            Counter.builder("multiagent.llm.tokens")
                    .tag("caller", caller)
                    .register(registry)
                    .increment(tokenCount);
        }
    }

    /**
     * 记录一次工具调用命中
     */
    public void recordToolCall(String tool) {
        if (registry == null) {
            return;
        }
        Counter.builder("multiagent.tool.call").tag("tool", tool).register(registry).increment();
    }

    /**
     * 记录一次工具调用失败
     */
    public void recordToolFailure(String tool) {
        if (registry == null) {
            return;
        }
        Counter.builder("multiagent.tool.failure").tag("tool", tool).register(registry).increment();
    }

    /**
     * 开始记录一次编排执行耗时
     */
    public Timer.Sample startExecute() {
        return registry == null ? null : Timer.start(registry);
    }

    /**
     * 结束记录一次编排执行耗时
     */
    public void recordExecute(Timer.Sample sample, String topology, boolean success) {
        if (sample == null || registry == null) {
            return;
        }
        sample.stop(Timer.builder("multiagent.execute")
                .tag("topology", topology)
                .tag("success", String.valueOf(success))
                .register(registry));
    }

    /**
     * CJK 友好的 Token 估算（中文约 1 token/字，英文约 4 字符/token）
     */
    private long estimateFallback(String text) {
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
}
