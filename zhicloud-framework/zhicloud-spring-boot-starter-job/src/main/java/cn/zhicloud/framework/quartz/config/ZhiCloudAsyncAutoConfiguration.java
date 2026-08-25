package cn.zhicloud.framework.quartz.config;

import com.alibaba.ttl.TtlRunnable;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步任务 Configuration
 */
@AutoConfiguration
@EnableAsync
public class ZhiCloudAsyncAutoConfiguration {

    /**
     * 审计日志专属线程池 Bean 名称（供 {@code @Async("auditLogExecutor")} 指定）
     */
    public static final String AUDIT_LOG_EXECUTOR_BEAN_NAME = "auditLogExecutor";

    @Bean
    public BeanPostProcessor threadPoolTaskExecutorBeanPostProcessor() {
        return new BeanPostProcessor() {

            @Override
            @SuppressWarnings("PatternVariableCanBeUsed")
            public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
                // 处理 ThreadPoolTaskExecutor
                if (bean instanceof ThreadPoolTaskExecutor) {
                    ThreadPoolTaskExecutor executor = (ThreadPoolTaskExecutor) bean;
                    executor.setTaskDecorator(TtlRunnable::get);
                    return executor;
                }
                // 处理 SimpleAsyncTaskExecutor
                // 参考 https://t.zsxq.com/CBoks 增加
                if (bean instanceof SimpleAsyncTaskExecutor) {
                    SimpleAsyncTaskExecutor executor = (SimpleAsyncTaskExecutor) bean;
                    executor.setTaskDecorator(TtlRunnable::get);
                    return executor;
                }
                return bean;
            }

        };
    }

    /**
     * 审计日志专属线程池：操作日志 / API 访问日志 / API 错误日志的异步写入与业务线程池隔离。
     *
     * <p>队列满时采用 DiscardOldestPolicy（丢弃最老的日志任务）——审计日志可容忍有界丢失，
     * 但不能反压业务请求或拖垮全局线程池。
     */
    @Bean(AUDIT_LOG_EXECUTOR_BEAN_NAME)
    public ThreadPoolTaskExecutor auditLogExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(10000);
        executor.setThreadNamePrefix("audit-log-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardOldestPolicy());
        // 优雅停机：等待已入队的日志落库
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

}
