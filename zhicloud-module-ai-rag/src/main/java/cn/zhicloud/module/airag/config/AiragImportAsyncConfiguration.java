package cn.zhicloud.module.airag.config;

import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.framework.tenant.core.util.TenantUtils;
import cn.zhicloud.module.airag.dal.dataobject.AiragDocumentDO;
import cn.zhicloud.module.airag.dal.mysql.AiragDocumentMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * AI RAG 文档导入异步配置
 *
 * <p>为文档向量化导入提供专属线程池（与全局 @Async 线程池隔离），避免大文档解析
 * 长时间占满共享线程池；并在应用启动完成后，把上次进程退出前遗留的「处理中」状态
 * 文档重置为「待处理」，等待重新导入。
 *
 * @author zhicloud
 */
@Configuration
@Slf4j
public class AiragImportAsyncConfiguration {

    /**
     * 文档导入专属线程池 Bean 名称（供 {@code @Async} 指定）
     */
    public static final String IMPORT_EXECUTOR_BEAN_NAME = "airagImportExecutor";

    /**
     * 文档处理状态：处理中（与 AiragRagServiceImpl / AiragDocumentServiceImpl 保持一致）
     */
    private static final int STATUS_PENDING = 0;
    private static final int STATUS_PROCESSING = 1;

    @Resource
    private AiragDocumentMapper documentMapper;

    @Bean(IMPORT_EXECUTOR_BEAN_NAME)
    public ThreadPoolTaskExecutor airagImportExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("airag-import-");
        // 队列满时由调用方线程执行，避免上传请求直接失败
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 优雅停机：等待导入任务完成
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }

    /**
     * 应用启动完成后恢复文档状态：把遗留的 PROCESSING 状态重置为 PENDING。
     *
     * <p>应用重启会中断正在执行的导入任务，这些文档会永远停留在「处理中」状态，
     * 既不能重新触发导入也无法被用户感知失败，因此启动时统一复位。
     */
    @EventListener(ApplicationReadyEvent.class)
    public void recoverProcessingDocuments() {
        TenantUtils.executeIgnore(() -> {
            List<AiragDocumentDO> processingDocuments = documentMapper.selectList(
                    new LambdaQueryWrapperX<AiragDocumentDO>()
                            .eq(AiragDocumentDO::getStatus, STATUS_PROCESSING));
            if (processingDocuments == null || processingDocuments.isEmpty()) {
                return;
            }
            for (AiragDocumentDO document : processingDocuments) {
                documentMapper.updateById(new AiragDocumentDO()
                        .setId(document.getId())
                        .setStatus(STATUS_PENDING));
            }
            log.info("[recoverProcessingDocuments][已将 {} 个遗留「处理中」文档重置为「待处理」]",
                    processingDocuments.size());
        });
    }

}
