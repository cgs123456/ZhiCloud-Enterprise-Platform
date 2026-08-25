package cn.zhicloud.framework.tenant.core.job;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.util.StrUtil;
import cn.zhicloud.framework.common.util.json.JsonUtils;
import cn.zhicloud.framework.tenant.core.service.TenantFrameworkService;
import cn.zhicloud.framework.tenant.core.util.TenantUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

/**
 * 多租户 JobHandler AOP
 * 任务执行时，会按照租户逐个执行 Job 的逻辑
 *
 * 注意，需要保证 JobHandler 的幂等性。因为 Job 因为某个租户执行失败重试时，之前执行成功的租户也会再次执行。
 *
 * @author 智云
 */
@Aspect
@RequiredArgsConstructor
@Slf4j
public class TenantJobAspect {

    /**
     * 多租户 Job 并行执行的并发度上限，可通过 zhicloud.tenant.job.parallelism 配置，默认 8
     */
    @Value("${zhicloud.tenant.job.parallelism:8}")
    private Integer jobParallelism;

    private final TenantFrameworkService tenantFrameworkService;

    @Around("@annotation(tenantJob)")
    public String around(ProceedingJoinPoint joinPoint, TenantJob tenantJob) {
        // 获得租户列表
        List<Long> tenantIds = tenantFrameworkService.getTenantIds();
        if (CollUtil.isEmpty(tenantIds)) {
            return null;
        }

        // 使用虚拟线程 + 信号量限流并行执行，替代 parallelStream（避免占用公共 ForkJoinPool、无法控制并发度）
        Map<Long, String> results = new ConcurrentHashMap<>();
        Map<Long, Throwable> errors = new ConcurrentHashMap<>();
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Semaphore semaphore = new Semaphore(jobParallelism);
            tenantIds.forEach(tenantId -> executor.submit(() -> {
                // 通过信号量控制并发度；虚拟线程阻塞等待代价极小
                semaphore.acquire();
                try {
                    TenantUtils.execute(tenantId, () -> {
                        try {
                            Object result = joinPoint.proceed();
                            results.put(tenantId, StrUtil.toStringOrEmpty(result));
                        } catch (Throwable e) {
                            errors.put(tenantId, e);
                            results.put(tenantId, ExceptionUtil.getRootCauseMessage(e));
                        }
                    });
                } finally {
                    semaphore.release();
                }
                return null;
            }));
            // try-with-resources 关闭时，会等待所有已提交的租户任务执行完成
        }

        // 统一汇总失败的租户，便于排查
        if (!errors.isEmpty()) {
            log.error("[around][多租户 Job 执行完成，共 {} 个租户失败：{}]",
                    errors.size(), StrUtil.join(StrUtil.COMMA, errors.keySet()), errors.values().iterator().next());
        }
        return JsonUtils.toJsonString(results);
    }

}
