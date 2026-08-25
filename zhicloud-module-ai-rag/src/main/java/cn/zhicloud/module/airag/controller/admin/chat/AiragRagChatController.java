package cn.zhicloud.module.airag.controller.admin.chat;

import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.framework.ratelimiter.core.annotation.RateLimiter;
import cn.zhicloud.framework.ratelimiter.core.keyresolver.impl.UserRateLimiterKeyResolver;
import cn.zhicloud.module.airag.controller.admin.chat.vo.AiragRagChatSendReqVO;
import cn.zhicloud.module.airag.controller.admin.chat.vo.AiragRagChatSendRespVO;
import cn.zhicloud.module.airag.service.rag.AiragRagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import static cn.zhicloud.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - AI RAG 对话")
@RestController
@RequestMapping("/airag/chat")
@Validated
public class AiragRagChatController {

    @Resource
    private AiragRagService ragService;

    @PostMapping("/send")
    @Operation(summary = "发送问题，获取 RAG 回答")
    @PreAuthorize("@ss.hasPermission('airag:chat:send')")
    // P0-4 修复：启用用户级限流，60 秒内最多 10 次 RAG 对话，防止 LLM 成本滥用
    @RateLimiter(time = 60, count = 10, keyResolver = UserRateLimiterKeyResolver.class)
    public CommonResult<AiragRagChatSendRespVO> sendChat(@RequestBody @Valid AiragRagChatSendReqVO sendReqVO) {
        String content = ragService.chat(sendReqVO.getKnowledgeId(), sendReqVO.getQuestion());
        return success(new AiragRagChatSendRespVO().setContent(content));
    }

    @PostMapping(value = "/send-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "发送问题，流式获取 RAG 回答", description = "SSE 流式返回，响应较快")
    @PreAuthorize("@ss.hasPermission('airag:chat:send')")
    // 与同步端点保持一致：启用用户级限流，防止 LLM 成本滥用
    @RateLimiter(time = 60, count = 10, keyResolver = UserRateLimiterKeyResolver.class)
    public Flux<CommonResult<AiragRagChatSendRespVO>> sendChatStream(@RequestBody @Valid AiragRagChatSendReqVO sendReqVO) {
        return ragService.chatStream(sendReqVO.getKnowledgeId(), sendReqVO.getQuestion())
                .map(content -> success(new AiragRagChatSendRespVO().setContent(content)));
    }

}
