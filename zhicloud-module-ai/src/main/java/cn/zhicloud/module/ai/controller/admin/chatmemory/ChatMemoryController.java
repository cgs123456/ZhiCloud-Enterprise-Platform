package cn.zhicloud.module.ai.controller.admin.chatmemory;

import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.module.ai.controller.admin.chatmemory.vo.ChatMemoryRespVO;
import cn.zhicloud.module.ai.service.chatmemory.ChatMemoryService;
import cn.zhicloud.module.ai.service.chatmemory.dto.ChatMessageDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.List;

import static cn.zhicloud.framework.common.pojo.CommonResult.success;

/**
 * Chat 记忆管理控制器
 *
 * 提供短期记忆查询、清空与长期记忆压缩的 REST API。
 *
 * @author zhicloud
 */
@Tag(name = "管理后台 - Chat 记忆")
@RestController
@RequestMapping("/ai/chat-memory")
@Validated
@Slf4j
public class ChatMemoryController {

    @Resource
    private ChatMemoryService chatMemoryService;

    @GetMapping("/messages")
    @Operation(summary = "获取会话记忆消息列表")
    @Parameter(name = "conversationId", required = true, description = "会话 ID", example = "conv-1024")
    @Parameter(name = "lastN", description = "最近 N 条（默认 20）", example = "20")
    @PreAuthorize("@ss.hasPermission('ai:chat-memory:query')")
    public CommonResult<ChatMemoryRespVO> getMessages(
            @RequestParam("conversationId") String conversationId,
            @RequestParam(value = "lastN", required = false) Integer lastN) {
        List<ChatMessageDTO> messages = chatMemoryService.getMessages(conversationId, lastN);
        String summary = chatMemoryService.getSummary(conversationId);
        ChatMemoryRespVO respVO = new ChatMemoryRespVO();
        respVO.setConversationId(conversationId);
        respVO.setMessages(messages != null ? messages : Collections.emptyList());
        respVO.setSummary(summary);
        return success(respVO);
    }

    @DeleteMapping("/clear")
    @Operation(summary = "清空会话记忆")
    @Parameter(name = "conversationId", required = true, description = "会话 ID", example = "conv-1024")
    @PreAuthorize("@ss.hasPermission('ai:chat-memory:delete')")
    public CommonResult<Boolean> clearMemory(@RequestParam("conversationId") String conversationId) {
        chatMemoryService.clearMemory(conversationId);
        return success(true);
    }

    @PostMapping("/compress")
    @Operation(summary = "压缩会话长期记忆", description = "当消息数超过阈值时，将旧消息压缩为摘要")
    @Parameter(name = "conversationId", required = true, description = "会话 ID", example = "conv-1024")
    @PreAuthorize("@ss.hasPermission('ai:chat-memory:compress')")
    public CommonResult<String> compress(@RequestParam("conversationId") String conversationId) {
        String summary = chatMemoryService.summarizeAndCompress(conversationId);
        return success(summary);
    }

}
