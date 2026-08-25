package cn.zhicloud.module.ai.config.security;

import cn.zhicloud.framework.common.exception.ServiceException;
import cn.zhicloud.framework.common.exception.enums.GlobalErrorCodeConstants;
import cn.zhicloud.framework.security.core.util.SecurityFrameworkUtils;
import cn.zhicloud.module.ai.config.AiSecurityConfiguration;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 提示词注入防护切面（SubTask 14.2）
 *
 * <p>基于 Spring AOP {@link Aspect} 拦截用户输入入口：
 * <ol>
 *   <li>所有 {@link Tool} 注解方法的 String 类型参数（userInput）。</li>
 *   <li>Controller（{@link RestController} 标记类）中带 {@link RequestBody} 注解的方法参数，
 *       若其类型含 String 字段则提取并匹配。</li>
 * </ol>
 *
 * <h3>处置策略</h3>
 * <ul>
 *   <li>{@code action=block}：命中即抛 {@link GlobalErrorCodeConstants#BAD_REQUEST}，
 *       返回 {@code {"code":400,"msg":"检测到潜在提示词注入"}}</li>
 *   <li>{@code action=warn}：放行但 {@code log.warn} 记录告警；单用户每分钟告警数超过
 *       {@code alertThresholdPerMinute} 时升级为 block。</li>
 * </ul>
 *
 * <h3>顺序</h3>
 * <p>通过 {@link Order} 保证在其他 AOP（如 {@link ToolAccessInterceptor}、
 * {@link cn.zhicloud.module.ai.mcp.tools.McpToolSecurityAspect}）之前执行，
 * 优先阻断注入攻击。
 *
 * @author 智云
 */
@Aspect
@Component
@ConditionalOnProperty(prefix = "zhicloud.ai.security.prompt-injection", name = "enabled", havingValue = "true")
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@Slf4j
public class PromptInjectionAspect {

    /**
     * 阻断错误消息
     */
    private static final String BLOCK_MSG = "检测到潜在提示词注入";

    /**
     * Redis Key 前缀：用于 warn 策略下计数
     */
    private static final String KEY_WARN_COUNTER = "ai:prompt-injection:warn:";

    /**
     * 分钟格式（用于 warn 计数 Key 后缀）
     */
    private static final DateTimeFormatter MINUTE_FMT = DateTimeFormatter.ofPattern("yyyyMMddHHmm");

    /**
     * AI 安全配置
     */
    @Autowired
    private AiSecurityConfiguration aiSecurityConfiguration;

    /**
     * Redis 模板（可选；warn 策略计数依赖）
     */
    @Autowired(required = false)
    @Lazy
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 编译后的正则缓存（懒加载，避免每次调用都编译）
     */
    private volatile Pattern[] compiledPatterns;

    /**
     * 拦截 {@link Tool} 注解方法
     */
    @Around("@annotation(org.springframework.ai.tool.annotation.Tool)")
    public Object aroundTool(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object[] args = joinPoint.getArgs();

        // 1. 提取所有 String 类型参数
        List<String> inputs = extractStringArgs(method, args);
        if (!inputs.isEmpty()) {
            checkInputs(inputs, buildLabel("Tool", method.getName()));
        }

        // 2. 检查 Tool 方法参数对象内的 String 字段（递归一层）
        List<String> nestedInputs = extractNestedStringFields(method, args);
        if (!nestedInputs.isEmpty()) {
            checkInputs(nestedInputs, buildLabel("Tool/nested", method.getName()));
        }

        return joinPoint.proceed();
    }

    /**
     * 拦截 Controller 中带 {@link RequestBody} 的方法
     *
     * <p>切点表达式含义：拦截 {@link RestController} 标记类下的所有方法（含 {@code @RequestMapping} 系列注解），
     * 然后在方法体中检查参数是否带 {@link RequestBody} 且类型含 String 字段。
     */
    @Around("@within(org.springframework.web.bind.annotation.RestController)")
    public Object aroundController(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object[] args = joinPoint.getArgs();
        Parameter[] parameters = method.getParameters();

        // 仅扫描带 @RequestBody 注解的参数
        List<String> inputs = new ArrayList<>();
        for (int i = 0; i < parameters.length && i < args.length; i++) {
            if (parameters[i].getAnnotation(RequestBody.class) == null) {
                continue;
            }
            Object arg = args[i];
            if (arg == null) {
                continue;
            }
            // String 类型直接收集
            if (arg instanceof String str) {
                inputs.add(str);
                continue;
            }
            // POJO 类型：提取 String 字段（递归一层）
            collectStringFields(arg, inputs);
        }

        if (!inputs.isEmpty()) {
            checkInputs(inputs, buildLabel("Controller", method.getName()));
        }

        return joinPoint.proceed();
    }

    /**
     * 校验用户输入列表：依次匹配正则 patterns，命中按 action 处置
     */
    private void checkInputs(List<String> inputs, String label) {
        Pattern[] patterns = getCompiledPatterns();
        if (patterns == null || patterns.length == 0) {
            return;
        }
        for (String input : inputs) {
            if (input == null || input.isEmpty()) {
                continue;
            }
            for (Pattern pattern : patterns) {
                if (pattern.matcher(input).find()) {
                    handleInjectionHit(label, pattern.pattern(), input);
                    return; // 命中一次即处置，无需继续
                }
            }
        }
    }

    /**
     * 处置命中：block 直接抛 400，warn 记录告警并在阈值触发后升级为 block
     */
    private void handleInjectionHit(String label, String pattern, String input) {
        AiSecurityConfiguration.PromptInjection config = aiSecurityConfiguration.getPromptInjection();
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        String action = config.getAction();

        // 截断输入便于日志展示
        String inputPreview = truncate(input, 200);

        if ("warn".equalsIgnoreCase(action)) {
            long current = incrementWarnCounter(userId);
            if (current > config.getAlertThresholdPerMinute()) {
                log.warn("[PromptInjection] warn 阈值触发升级为 block userId={} count={} label={} pattern={} input={}",
                        userId, current, label, pattern, inputPreview);
                throw new ServiceException(GlobalErrorCodeConstants.BAD_REQUEST.getCode(), BLOCK_MSG);
            }
            log.warn("[PromptInjection] 检测到潜在提示词注入（warn 放行）userId={} label={} pattern={} input={}",
                    userId, label, pattern, inputPreview);
            return;
        }

        // 默认 block 策略
        log.warn("[PromptInjection] 检测到潜在提示词注入（block）userId={} label={} pattern={} input={}",
                userId, label, pattern, inputPreview);
        throw new ServiceException(GlobalErrorCodeConstants.BAD_REQUEST.getCode(), BLOCK_MSG);
    }

    /**
     * 递增 warn 计数（按用户 + 分钟维度，1 分钟 TTL）
     */
    private long incrementWarnCounter(Long userId) {
        if (redisTemplate == null || userId == null) {
            return 1L; // Redis 不可用时返回 1，避免影响阻断逻辑
        }
        String minute = LocalDateTime.now().format(MINUTE_FMT);
        String key = KEY_WARN_COUNTER + userId + ":" + minute;
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1L) {
                redisTemplate.expire(key, Duration.ofSeconds(70)); // 略大于 1 分钟
            }
            return count == null ? 1L : count;
        } catch (Exception ex) {
            log.warn("[PromptInjection] Redis 计数失败 key={}", key, ex);
            return 1L;
        }
    }

    /**
     * 提取方法参数中的 String 类型值
     */
    private List<String> extractStringArgs(Method method, Object[] args) {
        List<String> result = new ArrayList<>();
        if (args == null) {
            return result;
        }
        for (Object arg : args) {
            if (arg instanceof String str) {
                result.add(str);
            }
        }
        return result;
    }

    /**
     * 提取方法参数对象中的 String 字段（仅递归一层）
     */
    private List<String> extractNestedStringFields(Method method, Object[] args) {
        List<String> result = new ArrayList<>();
        if (args == null) {
            return result;
        }
        for (Object arg : args) {
            if (arg == null || arg instanceof String) {
                continue;
            }
            collectStringFields(arg, result);
        }
        return result;
    }

    /**
     * 反射收集对象中 String 字段值（仅一层，不递归到嵌套对象）
     */
    private void collectStringFields(Object obj, List<String> result) {
        Class<?> clazz = obj.getClass();
        // 跳过 JDK 标准类型与 Spring 内置类型，避免反射开销
        if (clazz.getName().startsWith("java.") || clazz.getName().startsWith("org.springframework.")) {
            return;
        }
        for (Field field : clazz.getDeclaredFields()) {
            if (field.getType() != String.class) {
                continue;
            }
            try {
                field.setAccessible(true);
                Object value = field.get(obj);
                if (value instanceof String str && !str.isEmpty()) {
                    result.add(str);
                }
            } catch (Exception ex) {
                // 反射失败忽略
            }
        }
    }

    /**
     * 获取当前 HTTP 请求（用于日志关联，可能为 null）
     */
    private HttpServletRequest currentRequest() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes servletAttrs) {
            return servletAttrs.getRequest();
        }
        return null;
    }

    /**
     * 懒加载编译后的正则（DCL 双重检测）
     */
    private Pattern[] getCompiledPatterns() {
        Pattern[] local = compiledPatterns;
        if (local != null) {
            return local;
        }
        synchronized (this) {
            if (compiledPatterns != null) {
                return compiledPatterns;
            }
            List<String> rawPatterns = aiSecurityConfiguration.getPromptInjection().getPatterns();
            if (rawPatterns == null || rawPatterns.isEmpty()) {
                compiledPatterns = new Pattern[0];
            } else {
                List<Pattern> list = new ArrayList<>(rawPatterns.size());
                for (String p : rawPatterns) {
                    if (p == null || p.isEmpty()) {
                        continue;
                    }
                    try {
                        list.add(Pattern.compile(p));
                    } catch (Exception ex) {
                        log.error("[PromptInjection] 正则编译失败，跳过 pattern={}", p, ex);
                    }
                }
                compiledPatterns = list.toArray(new Pattern[0]);
            }
            return compiledPatterns;
        }
    }

    /**
     * 构造日志标签
     */
    private String buildLabel(String category, String methodName) {
        return category + "#" + methodName;
    }

    /**
     * 截断字符串
     */
    private String truncate(String str, int max) {
        if (str == null) {
            return "null";
        }
        return str.length() <= max ? str : str.substring(0, max) + "...(" + str.length() + " chars)";
    }

}
