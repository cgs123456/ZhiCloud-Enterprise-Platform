package cn.zhicloud.module.ai.service.chatmemory;

import cn.zhicloud.module.ai.service.chatmemory.dto.ChatMessageDTO;

import java.util.List;

/**
 * Chat 记忆服务接口
 *
 * 提供短期记忆（最近 N 条）、长期记忆（摘要压缩）与多租户隔离能力。
 *
 * 设计要点：
 *  1. 短期记忆：当前会话最近 N 条消息，存 Redis（key: chatmem:short:{tenantId}:{conversationId}）
 *  2. 长期记忆：超过阈值的旧消息压缩为摘要，存 Redis（key: chatmem:summary:{tenantId}:{conversationId}）
 *  3. 多租户隔离：key 中加入 tenantId，通过 {@code TenantContextHolder} 获取
 *  4. TTL：7 天自动过期
 *
 * @author zhicloud
 */
public interface ChatMemoryService {

    /**
     * 添加消息到会话记忆
     *
     * @param conversationId 会话 ID
     * @param messageType    消息类型：SYSTEM / USER / ASSISTANT / TOOL
     * @param content        消息内容
     * @return 已添加的 ChatMessageDTO
     */
    ChatMessageDTO addMessage(String conversationId, String messageType, String content);

    /**
     * 获取会话最近的消息列表（短期记忆）
     *
     * @param conversationId 会话 ID
     * @param lastN          最近 N 条（null 或 <=0 时使用默认值 20）
     * @return 消息列表（按时间正序）
     */
    List<ChatMessageDTO> getMessages(String conversationId, Integer lastN);

    /**
     * 清空会话记忆（短期 + 长期摘要）
     *
     * @param conversationId 会话 ID
     */
    void clearMemory(String conversationId);

    /**
     * 压缩并摘要长期记忆
     *
     * 当消息数超过阈值时，将旧消息调用 LLM 压缩为摘要，保留最近若干条。
     *
     * @param conversationId 会话 ID
     * @return 压缩后的摘要文本（null 表示未触发压缩或压缩失败）
     */
    String summarizeAndCompress(String conversationId);

    /**
     * 获取会话的长期摘要
     *
     * @param conversationId 会话 ID
     * @return 摘要文本（无摘要时返回 null）
     */
    String getSummary(String conversationId);

}
