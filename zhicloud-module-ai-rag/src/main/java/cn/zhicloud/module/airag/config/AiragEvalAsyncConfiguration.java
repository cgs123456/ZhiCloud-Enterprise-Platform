package cn.zhicloud.module.airag.config;

import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.framework.tenant.core.util.TenantUtils;
import cn.zhicloud.module.airag.dal.dataobject.eval.AiragEvalReportDO;
import cn.zhicloud.module.airag.dal.mysql.eval.AiragEvalReportMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;

import java.util.List;
import java.util.concurrent.Executors;

/**
 * AI RAG 批量评估异步配置
 *
 * <p>批量评估每题触发 2 次 LLM 调用（作答 + 打分），耗时以分钟计，必须与 Web 工作线程隔离。
 * 使用虚拟线程执行器：阻塞等待 LLM 的代价极小；并在应用启动后把上次退出前遗留的
 * 「执行中」报告标记为失败，避免前端永远轮询不到结果。
 *
 * @author zhicloud
 */
@Configuration
@Slf4j
public class AiragEvalAsyncConfiguration {

    /**
     * 批量评估专属执行器 Bean 名称（供 {@code @Async} 指定）
     */
    public static final String EVAL_EXECUTOR_BEAN_NAME = "airagEvalExecutor";

    /**
     * 报告状态：执行中 / 失败（与 RagEvalBatchService 常量保持一致，避免循环依赖）
     */
    private static final int REPORT_STATUS_RUNNING = 0;
    private static final int REPORT_STATUS_FAILED = 2;

    @Resource
    private AiragEvalReportMapper reportMapper;

    @Bean(EVAL_EXECUTOR_BEAN_NAME)
    public AsyncTaskExecutor airagEvalExecutor() {
        // 虚拟线程：每个评估任务独占一个虚拟线程，阻塞等待 LLM 时几乎零代价
        return new TaskExecutorAdapter(Executors.newVirtualThreadPerTaskExecutor());
    }

    /**
     * 应用启动后恢复遗留报告：把「执行中」标记为失败（进程退出导致的中断无法继续，
     * 用户可重新触发；保留 errorMsg 说明原因）。
     */
    @EventListener(ApplicationReadyEvent.class)
    public void recoverRunningReports() {
        TenantUtils.executeIgnore(() -> {
            List<AiragEvalReportDO> running = reportMapper.selectList(
                    new LambdaQueryWrapperX<AiragEvalReportDO>()
                            .eq(AiragEvalReportDO::getStatus, REPORT_STATUS_RUNNING));
            if (running == null || running.isEmpty()) {
                return;
            }
            for (AiragEvalReportDO report : running) {
                reportMapper.updateById(new AiragEvalReportDO()
                        .setId(report.getId())
                        .setStatus(REPORT_STATUS_FAILED)
                        .setErrorMsg("应用重启导致评估中断，请重新触发"));
            }
            log.info("[recoverRunningReports][已将 {} 个遗留「执行中」评估报告标记为失败]", running.size());
        });
    }

}
