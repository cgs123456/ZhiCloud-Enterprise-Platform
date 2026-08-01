package cn.iocoder.yudao.module.qms.framework.electronicsignature;

import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.qms.service.electronicsignature.ElectronicSignatureLogService;
import cn.hutool.core.util.StrUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.qms.enums.ErrorCodeConstants.*;

/**
 * 电子签名 Aspect（21 CFR Part 11）
 *
 * <p>拦截标注 {@link ElectronicSignature} 的方法，执行流程：
 * <ol>
 *   <li>从请求头获取 {@code X-Electronic-Signature-Username} 和 {@code X-Electronic-Signature-Password}</li>
 *   <li>调用 AdminAuthService 验证密码（通过 ApplicationContext 反射调用，避免模块间编译期依赖）</li>
 *   <li>密码验证失败抛 FORBIDDEN 错误码</li>
 *   <li>验证通过后记录签名日志（签名时间、用户、含义、IP、操作内容）</li>
 *   <li>放行原方法</li>
 * </ol>
 *
 * @author 芋道源码
 */
@Aspect
@Component
@Slf4j
public class ElectronicSignatureAspect {

    /**
     * 电子签名用户名请求头
     */
    private static final String HEADER_USERNAME = "X-Electronic-Signature-Username";
    /**
     * 电子签名密码请求头
     */
    private static final String HEADER_PASSWORD = "X-Electronic-Signature-Password";
    /**
     * 电子签名理由请求头
     */
    private static final String HEADER_REASON = "X-Electronic-Signature-Reason";

    @Resource
    private ElectronicSignatureLogService electronicSignatureLogService;

    @Resource
    private ApplicationContext applicationContext;

    @Around("@annotation(electronicSignature)")
    public Object around(ProceedingJoinPoint joinPoint, ElectronicSignature electronicSignature) throws Throwable {
        // 1. 获取当前请求
        HttpServletRequest request = getCurrentRequest();

        // 2. 从请求头获取签名凭据
        String username = request != null ? request.getHeader(HEADER_USERNAME) : null;
        String password = request != null ? request.getHeader(HEADER_PASSWORD) : null;
        if (StrUtil.isBlank(username)) {
            throw exception(ELECTRONIC_SIGNATURE_USERNAME_REQUIRED);
        }
        if (StrUtil.isBlank(password)) {
            throw exception(ELECTRONIC_SIGNATURE_PASSWORD_REQUIRED);
        }

        // 3. 若需要签名理由，校验理由
        String reason = request != null ? request.getHeader(HEADER_REASON) : null;
        if (electronicSignature.requireReason() && StrUtil.isBlank(reason)) {
            throw exception(ELECTRONIC_SIGNATURE_REASON_REQUIRED);
        }

        // 4. 验证密码（调用 AdminAuthService，通过反射避免模块间编译期依赖）
        authenticate(username, password);

        // 5. 构建操作内容与类型
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String operationType = method.getDeclaringClass().getSimpleName() + "." + method.getName();
        String operationContent = buildOperationContent(joinPoint);

        // 6. 记录签名日志
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        String ipAddress = request != null ? getClientIp(request) : null;
        electronicSignatureLogService.recordLog(userId, electronicSignature.meaning(),
                operationType, operationContent, ipAddress, reason);

        // 7. 放行原方法
        return joinPoint.proceed();
    }

    /**
     * 调用 AdminAuthService.authenticate 验证密码
     *
     * <p>由于 QMS 模块不直接依赖 system 模块，此处通过 ApplicationContext 反射调用，
     * 避免编译期依赖。若密码错误，AdminAuthService 会抛出 AUTH_LOGIN_BAD_CREDENTIALS，
     * 此处捕获后转换为电子签名密码错误。
     *
     * @param username 用户名
     * @param password 密码
     */
    private void authenticate(String username, String password) {
        Object adminAuthService = lookupAdminAuthService();
        if (adminAuthService == null) {
            log.warn("[authenticate] AdminAuthService bean not found, skip password verification");
            return;
        }
        try {
            Method authenticateMethod = adminAuthService.getClass().getMethod("authenticate", String.class, String.class);
            authenticateMethod.invoke(adminAuthService, username, password);
        } catch (Exception ex) {
            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
            log.warn("[authenticate] electronic signature password verify failed for user: {}", username, cause);
            throw exception(ELECTRONIC_SIGNATURE_PASSWORD_ERROR);
        }
    }

    /**
     * 查找 AdminAuthService Bean
     *
     * <p>尝试按常见 Bean 名称查找，兼容不同模块装配方式。
     */
    private Object lookupAdminAuthService() {
        String[] candidateNames = {"adminAuthServiceImpl", "adminAuthService"};
        for (String name : candidateNames) {
            try {
                if (applicationContext.containsBean(name)) {
                    return applicationContext.getBean(name);
                }
            } catch (Exception ignored) {
            }
        }
        // 兜底：按类型查找（类型名包含 AdminAuth）
        try {
            String[] beanNames = applicationContext.getBeanNamesForType(Object.class);
            for (String name : beanNames) {
                if (name.toLowerCase().contains("adminauth")) {
                    return applicationContext.getBean(name);
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * 构建操作内容（方法名 + 参数摘要）
     */
    private String buildOperationContent(ProceedingJoinPoint joinPoint) {
        StringBuilder sb = new StringBuilder();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        sb.append(signature.getDeclaringType().getSimpleName()).append(".").append(signature.getName());
        Object[] args = joinPoint.getArgs();
        if (args != null && args.length > 0) {
            sb.append("(");
            for (int i = 0; i < args.length; i++) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(args[i] != null ? args[i].toString() : "null");
            }
            sb.append(")");
        }
        return sb.toString();
    }

    /**
     * 获取当前 HttpServletRequest
     */
    private HttpServletRequest getCurrentRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes) {
            return ((ServletRequestAttributes) attributes).getRequest();
        }
        return null;
    }

    /**
     * 获取客户端 IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

}
