package cn.iocoder.yudao.module.airag.controller.admin.chat;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.ratelimiter.core.annotation.RateLimiter;
import cn.iocoder.yudao.framework.ratelimiter.core.keyresolver.impl.UserRateLimiterKeyResolver;
import cn.iocoder.yudao.module.airag.controller.admin.chat.vo.AiragRagChatSendReqVO;
import cn.iocoder.yudao.module.airag.controller.admin.chat.vo.AiragRagChatSendRespVO;
import cn.iocoder.yudao.module.airag.service.rag.AiragRagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

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

}
