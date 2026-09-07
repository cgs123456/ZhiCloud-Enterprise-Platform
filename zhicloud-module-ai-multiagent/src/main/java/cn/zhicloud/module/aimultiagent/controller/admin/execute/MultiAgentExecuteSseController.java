package cn.zhicloud.module.aimultiagent.controller.admin.execute;

import cn.zhicloud.framework.tenant.core.context.TenantContextHolder;
import cn.zhicloud.framework.tenant.core.util.TenantUtils;
import cn.zhicloud.module.aimultiagent.controller.admin.execute.vo.MultiAgentExecuteReqVO;
import cn.zhicloud.module.aimultiagent.model.AgentSseEvent;
import cn.zhicloud.module.aimultiagent.service.execute.MultiAgentExecuteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 管理后台 - 多 Agent 编排执行 SSE 实时推送
 *
 * <p>使用 POST + JSON Body（而非 GET 查询参数）：userInput 可能很长，且便于携带
 * Authorization Header（原生 EventSource 无法自定义 Header，前端请用 fetch-event-source）。
 *
 * @author zhicloud
 */
@Tag(name = "管理后台 - 多 Agent 编排执行 SSE 推送")
@RestController
@RequestMapping("/aimultiagent/execute")
@Validated
@Slf4j
public class MultiAgentExecuteSseController {

    /**
     * SSE 推送超时（5 分钟，超长编排由前端重连或改走同步接口）
     */
    private static final long SSE_TIMEOUT_MS = 300_000L;

    /**
     * SSE 执行线程池（虚拟线程：任务阻塞等待 LLM，平台线程代价高）。
     * 虚拟线程均为守护线程，不阻止 JVM 退出，无需显式关闭。
     */
    private static final Executor SSE_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

    @Resource
    private MultiAgentExecuteService executeService;

    @PostMapping(value = "/run-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "执行多 Agent 编排（SSE 实时推送进度）")
    @PreAuthorize("@ss.hasPermission('aimultiagent:execute:run')")
    public SseEmitter runStream(@Valid @RequestBody MultiAgentExecuteReqVO reqVO) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        String traceId = UUID.randomUUID().toString();
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        AtomicBoolean completed = new AtomicBoolean(false);

        Runnable finishOnce = () -> {
            if (completed.compareAndSet(false, true)) {
                emitter.complete();
            }
        };
        emitter.onCompletion(() -> completed.set(true));
        emitter.onTimeout(() -> {
            log.warn("[runStream][SSE 超时关闭，topologyId={}]", reqVO.getTopologyId());
            finishOnce.run();
        });
        // 显式参数：onError 有 Consumer<Throwable>/Runnable 双重载，方法引用会产生二义性编译错误
        emitter.onError((Throwable t) -> finishOnce.run());

        // Runnable 强转：TenantUtils 有 Runnable/Callable 两个重载，块 lambda 会产生二义性编译错误
        SSE_EXECUTOR.execute(() -> TenantUtils.execute(tenantId, (Runnable) () -> {
            // 租户上下文 + traceId 必须显式透传到异步线程（ThreadLocal 不跨线程）
            MDC.put("multiAgentTraceId", traceId);
            try {
                executeService.executeWithSse(reqVO.getTopologyId(), reqVO.getUserInput(), tenantId, event -> {
                    if (completed.get()) {
                        return;
                    }
                    try {
                        emitter.send(SseEmitter.event()
                                .name(event.type())
                                .data(event, MediaType.APPLICATION_JSON));
                    } catch (IOException e) {
                        // 客户端断连：标记完成，后续事件直接丢弃；执行本身继续跑完并落库
                        log.debug("[runStream][客户端断连，停止推送，topologyId={}]", reqVO.getTopologyId(), e);
                        completed.set(true);
                    }
                    if (event.isTerminal()) {
                        finishOnce.run();
                    }
                });
            } catch (Exception e) {
                log.error("[runStream][编排执行异常，topologyId={}]", reqVO.getTopologyId(), e);
                try {
                    emitter.completeWithError(e);
                } catch (Exception ignored) {
                    // 连接已关闭时忽略
                }
            } finally {
                MDC.remove("multiAgentTraceId");
            }
        }));
        return emitter;
    }

}
