package cn.iocoder.yudao.module.ai.config.security;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.exception.enums.GlobalErrorCodeConstants;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.ai.config.AiSecurityConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 提示词注入防护 Advisor（P0-1：ChatClient 调用链内防护）
 *
 * <p>本 Advisor 与现有的 {@link PromptInjectionAspect}（AOP）形成内外两层防护：
 * <ul>
 *   <li>外层 {@code PromptInjectionAspect}：拦截 HTTP 入口 Controller 方法参数</li>
 *   <li>内层 本 Advisor：拦截 ChatClient.call() 内部链路（含 Tool 循环、Stream 子调用）</li>
 * </ul>
 *
 * <h3>关键设计</h3>
 * <p>实现 Spring AI 1.1.x 的 {@link CallAdvisor} 与 {@link StreamAdvisor} 接口，覆盖同步与流式调用：
 * <ol>
 *   <li>取出 user message</li>
 *   <li>用配置中的正则 patterns 检测</li>
 *   <li>命中且 action=block → 抛 {@link ServiceException}(BAD_REQUEST)，阻断 ChatClient 调用</li>
 *   <li>命中且 action=warn → 计数 + 告警日志，放行</li>
 *   <li>未命中 → 正常放行到下一个 Advisor</li>
 * </ol>
 * <p>流式路径 {@link #adviseStream} 复用同样的检测逻辑，命中 + block 时抛 {@link ServiceException}，
 * Flux 在订阅时会立即触发 onError 终止整条流。
 *
 * @author yudao
 */
@Component
@ConditionalOnProperty(prefix = "yudao.ai.security.prompt-injection", name = "enabled", havingValue = "true")
@Slf4j
public class PromptInjectionAdvisor implements CallAdvisor, StreamAdvisor {

    /**
     * Advisor 执行顺序：在 Tool Calling Advisor 之前执行（HIGHEST_PRECEDENCE + 100）
     * 确保用户输入在被 Tool 循环处理前先过注入检测
     */
    @Override
    public String getName() {
        return "PromptInjectionAdvisor";
    }

    @Override
    public int getOrder() {
        return 100;
    }

    @Autowired
    private AiSecurityConfiguration config;

    @Autowired(required = false)
    @org.springframework.context.annotation.Lazy
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 单用户每分钟告警计数 Redis Key 前缀
     */
    private static final String KEY_ALERT_COUNT = "ai:sec:pi:alert:";

    /**
     * 1 分钟窗口
     */
    private static final Duration ALERT_WINDOW = Duration.ofMinutes(1);

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        // 1. 提取 user message（可能多条，只检查最后一条用户输入）
        String userInput = extractUserInput(request);
        if (userInput == null || userInput.isEmpty()) {
            return chain.nextCall(request);
        }

        // 2. 用配置的正则 patterns 检测
        AiSecurityConfiguration.PromptInjection cfg = config.getPromptInjection();
        List<Pattern> patterns = cfg.getCompiledPatterns();
        if (patterns == null || patterns.isEmpty()) {
            return chain.nextCall(request);
        }

        String matchedPattern = null;
        for (Pattern p : patterns) {
            if (p.matcher(userInput).find()) {
                matchedPattern = p.pattern();
                break;
            }
        }

        if (matchedPattern == null) {
            // 未命中，正常放行
            return chain.nextCall(request);
        }

        // 3. 命中处置
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        String action = cfg.getAction();
        if ("block".equalsIgnoreCase(action)) {
            log.warn("[adviseCall][命中提示词注入并拦截，userId={}, pattern={}, inputHead={}]",
                    userId, matchedPattern, truncate(userInput, 80));
            throw new ServiceException(GlobalErrorCodeConstants.BAD_REQUEST.getCode(), "检测到潜在提示词注入");
        }

        // action=warn：计数 + 告警，放行
        long alertCount = incrementAlertCount(userId);
        if (alertCount > cfg.getAlertThresholdPerMinute()) {
            // 超阈值升级为 block
            log.error("[adviseCall][提示词注入告警超阈值，升级为拦截，userId={}, alertCount={}, threshold={}]",
                    userId, alertCount, cfg.getAlertThresholdPerMinute());
            throw new ServiceException(GlobalErrorCodeConstants.BAD_REQUEST.getCode(), "提示词注入告警超阈值，请求被拦截");
        }

        log.warn("[adviseCall][命中提示词注入并告警放行，userId={}, alertCount={}, pattern={}]",
                userId, alertCount, matchedPattern);
        return chain.nextCall(request);
    }

    /**
     * 流式调用入口（StreamAroundAdvisor 实现）
     *
     * <p>复用 {@link #extractUserInput} 与配置的 patterns 检测逻辑，确保流式场景下注入防护不失效。
     * 命中处置策略：
     * <ul>
     *   <li>action=block：抛 {@link ServiceException}（Flux 在订阅时立即触发 onError，终止整条流）</li>
     *   <li>action=warn：计数 + 告警日志，超阈值升级为 block，否则放行到下游 chain</li>
     *   <li>未命中：放行到下游 chain</li>
     * </ul>
     *
     * @param request ChatClient 请求
     * @param chain   下游 Advisor 链
     * @return Flux 流式响应
     */
    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
        // 1. 提取 user message
        String userInput = extractUserInput(request);
        if (userInput == null || userInput.isEmpty()) {
            return chain.nextStream(request);
        }

        // 2. 用配置的正则 patterns 检测
        AiSecurityConfiguration.PromptInjection cfg = config.getPromptInjection();
        List<Pattern> patterns = cfg.getCompiledPatterns();
        if (patterns == null || patterns.isEmpty()) {
            return chain.nextStream(request);
        }

        String matchedPattern = null;
        for (Pattern p : patterns) {
            if (p.matcher(userInput).find()) {
                matchedPattern = p.pattern();
                break;
            }
        }

        if (matchedPattern == null) {
            // 未命中，正常放行
            return chain.nextStream(request);
        }

        // 3. 命中处置
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        String action = cfg.getAction();
        if ("block".equalsIgnoreCase(action)) {
            log.warn("[adviseStream][命中提示词注入并拦截，userId={}, pattern={}, inputHead={}]",
                    userId, matchedPattern, truncate(userInput, 80));
            throw new ServiceException(GlobalErrorCodeConstants.BAD_REQUEST.getCode(), "检测到潜在提示词注入");
        }

        // action=warn：计数 + 告警，放行
        long alertCount = incrementAlertCount(userId);
        if (alertCount > cfg.getAlertThresholdPerMinute()) {
            log.error("[adviseStream][提示词注入告警超阈值，升级为拦截，userId={}, alertCount={}, threshold={}]",
                    userId, alertCount, cfg.getAlertThresholdPerMinute());
            throw new ServiceException(GlobalErrorCodeConstants.BAD_REQUEST.getCode(), "提示词注入告警超阈值，请求被拦截");
        }

        log.warn("[adviseStream][命中提示词注入并告警放行，userId={}, alertCount={}, pattern={}]",
                userId, alertCount, matchedPattern);
        return chain.nextStream(request);
    }

    /**
     * 提取 ChatClientRequest 中的最后一条 UserMessage
     */
    private String extractUserInput(ChatClientRequest request) {
        if (request.prompt() == null) {
            return null;
        }
        List<org.springframework.ai.chat.messages.Message> messages = request.prompt().getInstructions();
        if (messages == null || messages.isEmpty()) {
            return null;
        }
        // 从尾部找第一条 UserMessage
        for (int i = messages.size() - 1; i >= 0; i--) {
            var msg = messages.get(i);
            if (msg instanceof UserMessage um) {
                return um.getText();
            }
        }
        return null;
    }

    /**
     * 告警计数（Redis 滑窗）
     */
    private long incrementAlertCount(Long userId) {
        if (redisTemplate == null) {
            return 1L;
        }
        String key = KEY_ALERT_COUNT + (userId == null ? "anon" : userId);
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, ALERT_WINDOW);
        }
        return count == null ? 0L : count;
    }

    /**
     * 截断字符串用于日志（避免日志过长）
     */
    private String truncate(String s, int maxLen) {
        if (s == null) {
            return "";
        }
        return s.length() > maxLen ? s.substring(0, maxLen) + "..." : s;
    }

}
