package cn.zhicloud.module.ai.service.chatmemory.dto;

/**
 * Chat 记忆消息 DTO
 *
 * 用于在 Redis 中存储单条聊天消息（短期记忆）。
 *
 * @param id         消息唯一 ID（UUID）
 * @param role       角色：SYSTEM / USER / ASSISTANT / TOOL
 * @param content    消息内容
 * @param timestamp  时间戳（毫秒）
 * @param tokenCount Token 数估算
 *
 * @author zhicloud
 */
public record ChatMessageDTO(
        String id,
        String role,
        String content,
        Long timestamp,
        Integer tokenCount
) {
}
