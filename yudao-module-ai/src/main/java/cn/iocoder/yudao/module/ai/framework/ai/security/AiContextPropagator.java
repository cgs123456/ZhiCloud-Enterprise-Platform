package cn.iocoder.yudao.module.ai.framework.ai.security;

import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

/**
 * AI 上下文传播器
 *
 * 在虚拟线程（spring.threads.virtual.enabled=true）和异步场景（CompletableFuture/响应式）下，
 * ThreadLocal 无法自动透传。本工具用于捕获当前线程的租户、安全上下文，并在新线程中恢复与清理。
 *
 * 适用场景：
 * 1. 未来引入异步执行（CompletableFuture/响应式），保证 TenantContext、SecurityContext 不丢失
 * 2. Flux 异步流式对话中，需要透传 TenantContextHolder 的场景（目前用 TenantUtils.executeIgnore 规避）
 *
 * 使用示例：
 * <pre>
 *     ContextSnapshot snapshot = aiContextPropagator.capture();
 *     CompletableFuture.supplyAsync(aiContextPropagator.wrap(() -> {
 *         // 此处可正常获取 TenantContextHolder.getTenantId()、SecurityFrameworkUtils.getLoginUserId()
 *         return doBusiness();
 *     }));
 * </pre>
 *
 * @author fansili
 */
@Component
@Slf4j
public class AiContextPropagator {

    /**
     * 捕获当前线程的上下文快照
     *
     * 保存 TenantContext、SecurityContext，用于后续在新线程中恢复
     *
     * @return 上下文快照
     */
    public ContextSnapshot capture() {
        Long tenantId = TenantContextHolder.getTenantId();
        Long userId = SecurityFrameworkUtils.getLoginUserId();
        SecurityContext securityContext = SecurityContextHolder.getContext();
        return new ContextSnapshot(tenantId, userId, securityContext);
    }

    /**
     * 包装 Supplier，在执行前恢复上下文，执行后清理
     *
     * 适用于 CompletableFuture.supplyAsync 等异步场景
     *
     * @param supplier 原始 Supplier
     * @param <T>      返回值类型
     * @return 包装后的 Supplier，执行时会先恢复上下文
     */
    public <T> Supplier<T> wrap(Supplier<T> supplier) {
        ContextSnapshot snapshot = capture();
        return () -> {
            try {
                snapshot.restore();
                return supplier.get();
            } finally {
                snapshot.cleanup();
            }
        };
    }

    /**
     * 包装 Runnable，在执行前恢复上下文，执行后清理
     *
     * 适用于 CompletableFuture.runAsync、线程池提交等异步场景
     *
     * @param runnable 原始 Runnable
     * @return 包装后的 Runnable，执行时会先恢复上下文
     */
    public Runnable wrap(Runnable runnable) {
        ContextSnapshot snapshot = capture();
        return () -> {
            try {
                snapshot.restore();
                runnable.run();
            } finally {
                snapshot.cleanup();
            }
        };
    }

    /**
     * 上下文快照
     *
     * 保存租户编号、用户编号、安全上下文，用于在新线程中恢复
     */
    public static class ContextSnapshot {

        /**
         * 租户编号
         */
        private final Long tenantId;

        /**
         * 用户编号
         */
        private final Long userId;

        /**
         * 安全上下文（包含 LoginUser 等认证信息）
         */
        private final SecurityContext securityContext;

        public ContextSnapshot(Long tenantId, Long userId, SecurityContext securityContext) {
            this.tenantId = tenantId;
            this.userId = userId;
            this.securityContext = securityContext;
        }

        /**
         * 在当前线程恢复上下文
         *
         * 恢复 TenantContext、SecurityContext
         */
        public void restore() {
            if (tenantId != null) {
                TenantContextHolder.setTenantId(tenantId);
            }
            if (securityContext != null) {
                SecurityContextHolder.setContext(securityContext);
            }
            log.debug("[restore][恢复上下文：tenantId({}) userId({})]", tenantId, userId);
        }

        /**
         * 清理当前线程的上下文
         *
         * 清理 TenantContext、SecurityContext，防止线程复用时的上下文泄漏
         */
        public void cleanup() {
            TenantContextHolder.clear();
            SecurityContextHolder.clearContext();
            log.debug("[cleanup][清理上下文：tenantId({}) userId({})]", tenantId, userId);
        }

        public Long getTenantId() {
            return tenantId;
        }

        public Long getUserId() {
            return userId;
        }

    }

}
