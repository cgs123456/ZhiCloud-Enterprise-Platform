package cn.iocoder.yudao.module.ai.mcp.tools;

import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.ai.tool.annotation.Tool;

import java.lang.reflect.Method;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * MCP Tool 审计日志 AOP 切面
 *
 * SubTask 7.4c：拦截所有 {@link Tool} 注解方法，记录调用审计信息。
 *
 * 审计字段：
 *  - 调用时间（startTime / endTime）
 *  - 调用者（userId，从 SecurityContextHolder 获取）
 *  - 工具名（@Tool.name 或方法签名）
 *  - 参数（args 列表）
 *  - 返回值摘要（截断到 500 字符，避免日志爆炸）
 *  - 耗时（毫秒）
 *
 * 落地策略：
 *  - 当前阶段：通过 slf4j 以结构化 JSON 输出到日志文件，便于 ELK/Loki 采集
 *  - 后续扩展：可改为持久化到 mcp_tool_log 表（需新建 DO/Mapper/SQL 迁移）
 *
 * 异常处理：
 *  - 业务方法抛出异常时，记录异常信息和耗时，原异常重新抛出
 *  - 审计本身失败不影响业务（catch + log.error）
 *
 * @author 芋道源码
 */
@Aspect
@Configuration
@Slf4j
public class McpToolAuditLogAspect {

    /**
     * 返回值摘要最大长度，超过则截断
     */
    private static final int RESULT_SUMMARY_MAX_LENGTH = 500;

    @Autowired(required = false)
    @Lazy
    private ObjectMapper objectMapper;

    /**
     * 拦截所有带 {@link Tool} 注解的方法
     */
    @Around("@annotation(org.springframework.ai.tool.annotation.Tool)")
    public Object aroundToolCall(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Tool toolAnnotation = method.getAnnotation(Tool.class);
        String toolName = (toolAnnotation != null && !toolAnnotation.name().isEmpty())
                ? toolAnnotation.name() : method.getName();

        LocalDateTime startTime = LocalDateTime.now();
        long startMillis = System.currentTimeMillis();
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        Object[] args = joinPoint.getArgs();

        Object result = null;
        Throwable error = null;
        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable ex) {
            error = ex;
            throw ex;
        } finally {
            long costMillis = System.currentTimeMillis() - startMillis;
            try {
                writeAuditLog(toolName, method, userId, args, result, error, startTime, costMillis);
            } catch (Exception logEx) {
                log.error("[MCP Tool 审计] 写入审计日志失败 toolName={}", toolName, logEx);
            }
        }
    }

    /**
     * 写入审计日志（结构化 INFO 级别）
     */
    private void writeAuditLog(String toolName, Method method, Long userId, Object[] args,
                               Object result, Throwable error, LocalDateTime startTime, long costMillis) {
        String paramSummary = summarizeArgs(args, method);
        String resultSummary = error == null ? summarizeResult(result) : "ERROR: " + error.getClass().getSimpleName()
                + ": " + error.getMessage();
        String status = error == null ? "SUCCESS" : "FAILURE";

        log.info("[MCP Tool 审计] toolName={} | status={} | userId={} | startTime={} | costMs={} | params={} | result={}",
                toolName, status, userId, startTime, costMillis, paramSummary, resultSummary);
    }

    /**
     * 参数摘要：参数名 + 值（截断长字符串）
     */
    private String summarizeArgs(Object[] args, Method method) {
        if (args == null || args.length == 0) {
            return "[]";
        }
        String[] paramNames = method.getParameters().length > 0
                ? Arrays.stream(method.getParameters()).map(p -> p.getName()).toArray(String[]::new)
                : new String[0];
        List<String> entries = new java.util.ArrayList<>(args.length);
        for (int i = 0; i < args.length; i++) {
            String name = i < paramNames.length ? paramNames[i] : "arg" + i;
            String value = safeToString(args[i]);
            entries.add(name + "=" + value);
        }
        String joined = entries.toString();
        return truncate(joined, RESULT_SUMMARY_MAX_LENGTH);
    }

    /**
     * 返回值摘要
     */
    private String summarizeResult(Object result) {
        if (result == null) {
            return "null";
        }
        String str = safeToString(result);
        return truncate(str, RESULT_SUMMARY_MAX_LENGTH);
    }

    private String safeToString(Object obj) {
        if (obj == null) {
            return "null";
        }
        try {
            // 优先用 ObjectMapper 序列化复杂对象
            if (objectMapper != null && !(obj instanceof String) && !(obj instanceof Number)
                    && !(obj instanceof Boolean)) {
                return objectMapper.writeValueAsString(obj);
            }
            return obj.toString();
        } catch (Exception ex) {
            return obj.getClass().getSimpleName() + "@" + Integer.toHexString(System.identityHashCode(obj));
        }
    }

    private String truncate(String str, int max) {
        if (str == null) {
            return "null";
        }
        return str.length() <= max ? str : str.substring(0, max) + "...(" + str.length() + " chars)";
    }

}
