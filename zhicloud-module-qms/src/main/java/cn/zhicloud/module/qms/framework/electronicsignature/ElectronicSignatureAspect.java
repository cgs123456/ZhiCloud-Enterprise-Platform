package cn.zhicloud.module.qms.framework.electronicsignature;

import cn.zhicloud.framework.security.core.util.SecurityFrameworkUtils;
import cn.zhicloud.module.qms.service.electronicsignature.ElectronicSignatureLogService;
import cn.hutool.core.util.StrUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.qms.enums.ErrorCodeConstants.*;

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
 * @author 智云
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
        Long signerUserId = authenticate(username, password);

        // 5. 校验「签名人 == 当前会话用户」（21 CFR Part 11 §11.200(a)(1)：签名唯一归属签署人本人）
        //    只校验凭据有效性是不够的：A 登录后填 B 的账号密码同样能通过，
        //    而第 7 步日志记录的是会话用户 A，等于 A 可以用 B 的凭据完成签名并把责任落到自己名下（或反之）。
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        if (loginUserId == null || !loginUserId.equals(signerUserId)) {
            log.warn("[around] electronic signature user mismatch, loginUserId={}, signerUsername={}",
                    loginUserId, username);
            throw exception(ELECTRONIC_SIGNATURE_USER_MISMATCH);
        }

        // 6. 构建操作内容与类型
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String operationType = method.getDeclaringClass().getSimpleName() + "." + method.getName();
        String operationContent = buildOperationContent(joinPoint);

        // 7. 记录签名日志
        //    注意 recordLog 标注 REQUIRES_NEW：签名事实必须独立于业务事务落库。
        //    否则业务方法在第 8 步抛异常回滚时会把签名记录一并回滚，
        //    审计轨迹里将完全看不到「谁在何时尝试签署了什么」，违背 Part 11 §11.10(e) 的审计追踪要求。
        String ipAddress = request != null ? getClientIp(request) : null;
        electronicSignatureLogService.recordLog(loginUserId, electronicSignature.meaning(),
                operationType, operationContent, ipAddress, reason);

        // 8. 放行原方法
        return joinPoint.proceed();
    }

    /**
     * 调用 AdminAuthService.authenticate 验证密码
     *
     * <p>由于 QMS 模块不直接依赖 system 模块，此处通过 ApplicationContext 反射调用，
     * 避免编译期依赖。若密码错误，AdminAuthService 会抛出 AUTH_LOGIN_BAD_CREDENTIALS，
     * 此处捕获后转换为电子签名密码错误。
     *
     * <p><b>安全约束（fail-closed）</b>：认证服务缺失或反射调用失败时，一律拒绝本次操作。
     * 电子签名是 21 CFR Part 11 的强制管控点，任何「降级放行」都等价于签名可被绕过，
     * 因此不得以 WARN 日志的方式静默跳过密码校验。
     *
     * @param username 用户名
     * @param password 密码
     * @return 通过校验的签名人用户编号，供调用方与会话用户比对（fail-closed：取不到则拒绝）
     */
    private Long authenticate(String username, String password) {
        Object adminAuthService = lookupAdminAuthService();
        if (adminAuthService == null) {
            // fail-closed：认证服务不可用时拒绝签名，而非跳过校验
            log.error("[authenticate] AdminAuthService bean not found, reject electronic signature for user: {}", username);
            throw exception(ELECTRONIC_SIGNATURE_AUTH_SERVICE_UNAVAILABLE);
        }
        Method authenticateMethod;
        try {
            authenticateMethod = adminAuthService.getClass().getMethod("authenticate", String.class, String.class);
        } catch (NoSuchMethodException ex) {
            // 认证服务契约不匹配属于装配问题，同样 fail-closed，且与「密码错误」区分开便于排障
            log.error("[authenticate] AdminAuthService#authenticate(String,String) not found on bean {}",
                    adminAuthService.getClass().getName(), ex);
            throw exception(ELECTRONIC_SIGNATURE_AUTH_SERVICE_UNAVAILABLE);
        }
        Object signerUser;
        try {
            // AdminAuthService#authenticate 返回 AdminUserDO；此处以 Object 承接，避免编译期依赖 system 模块
            signerUser = authenticateMethod.invoke(adminAuthService, username, password);
        } catch (InvocationTargetException ex) {
            // 目标方法抛错：绝大多数为凭据校验失败
            log.warn("[authenticate] electronic signature password verify failed for user: {}", username, ex.getTargetException());
            throw exception(ELECTRONIC_SIGNATURE_PASSWORD_ERROR);
        } catch (IllegalAccessException | RuntimeException ex) {
            log.error("[authenticate] electronic signature authenticate invocation error for user: {}", username, ex);
            throw exception(ELECTRONIC_SIGNATURE_AUTH_SERVICE_UNAVAILABLE);
        }
        return extractUserId(signerUser, username);
    }

    /**
     * 从 AdminUserDO 反射取出用户编号
     *
     * <p>fail-closed：返回值为空、无 getId() 方法、或 id 为 null，都说明认证服务契约与预期不符，
     * 此时无法完成「签名人 == 会话用户」的核验，必须拒绝而不是放行。
     */
    private Long extractUserId(Object signerUser, String username) {
        if (signerUser == null) {
            log.error("[extractUserId] authenticate returned null for user: {}, reject signature", username);
            throw exception(ELECTRONIC_SIGNATURE_AUTH_SERVICE_UNAVAILABLE);
        }
        try {
            Object id = signerUser.getClass().getMethod("getId").invoke(signerUser);
            if (!(id instanceof Long)) {
                log.error("[extractUserId] unexpected id type {} from {}, reject signature",
                        id == null ? "null" : id.getClass().getName(), signerUser.getClass().getName());
                throw exception(ELECTRONIC_SIGNATURE_AUTH_SERVICE_UNAVAILABLE);
            }
            return (Long) id;
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
            log.error("[extractUserId] cannot read id from {}, reject signature",
                    signerUser.getClass().getName(), ex);
            throw exception(ELECTRONIC_SIGNATURE_AUTH_SERVICE_UNAVAILABLE);
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
            } catch (BeansException ex) {
                // 单个候选名解析失败不应中断查找，但必须留痕，避免「静默吞异常导致签名被绕过」
                log.warn("[lookupAdminAuthService] resolve bean by name failed: {}", name, ex);
            }
        }
        // 兜底：遍历 Bean 名匹配。
        // 这里必须用「前缀 adminauth + 后缀 service」的强约束，而不是宽泛的 contains("adminauth")：
        // 后者会命中 adminAuthProperties、adminAuthCacheManager 之类的无关 Bean，
        // 而这些 Bean 上不存在 authenticate(String,String)，最终走到
        // ELECTRONIC_SIGNATURE_AUTH_SERVICE_UNAVAILABLE——表现为「签名功能整体不可用」，排障成本极高。
        try {
            for (String name : applicationContext.getBeanDefinitionNames()) {
                String lower = name.toLowerCase();
                if (lower.startsWith("adminauth") && lower.contains("service")) {
                    return applicationContext.getBean(name);
                }
            }
        } catch (BeansException ex) {
            log.warn("[lookupAdminAuthService] resolve bean by name scan failed", ex);
        }
        return null;
    }

    /**
     * 单个参数摘要的最大长度，超出截断，避免超长 VO 撑爆 operation_content 字段
     */
    private static final int MAX_ARG_LEN = 64;

    /**
     * 构建操作内容（方法名 + 参数摘要）
     *
     * <p><b>不得直接 toString 全部参数</b>：审计日志是长期留存且可被质量／审计岗查阅的，
     * 而 QMS 的 SaveReqVO 里普遍带有客诉人姓名、电话、供应商联系人、检验员备注等个人信息，
     * 整体 toString 会把这些 PII 固化进签名日志，既超出「记录签署了什么操作」的必要范围，
     * 也给后续的数据出境／留存合规埋雷。
     *
     * <p>因此只记录可定位业务对象的标量参数（Long/Integer/Boolean/短字符串），
     * 复杂对象仅记录类型名——追溯时凭 operationType + 业务 ID 即可回查原始单据。
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
                sb.append(summarizeArg(args[i]));
            }
            sb.append(")");
        }
        return sb.toString();
    }

    /**
     * 参数摘要：标量原样记录，复杂对象只留类型名（防 PII 外泄）
     */
    private String summarizeArg(Object arg) {
        if (arg == null) {
            return "null";
        }
        if (arg instanceof Number || arg instanceof Boolean || arg instanceof Character) {
            return arg.toString();
        }
        if (arg instanceof CharSequence) {
            String s = arg.toString();
            return s.length() <= MAX_ARG_LEN ? s : s.substring(0, MAX_ARG_LEN) + "...";
        }
        // 复杂对象（SaveReqVO / DO / 集合等）可能含个人信息，只记录类型
        return arg.getClass().getSimpleName();
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
