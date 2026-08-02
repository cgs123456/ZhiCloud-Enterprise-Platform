package cn.iocoder.yudao.module.ai.service.chatmemory;

import cn.iocoder.yudao.framework.common.exception.enums.GlobalErrorCodeConstants;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.ai.dal.dataobject.model.AiModelDO;
import cn.iocoder.yudao.module.ai.enums.model.AiModelTypeEnum;
import cn.iocoder.yudao.module.ai.service.chatmemory.dto.ChatMessageDTO;
import cn.iocoder.yudao.module.ai.service.model.AiModelService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Chat 记忆服务实现
 *
 * 设计要点：
 *  1. 短期记忆：Redis List 结构存储最近 N 条消息（默认 20），key: chatmem:short:{tenantId}:{conversationId}
 *  2. 长期记忆：当消息数超过阈值（默认 30）时，将前 20 条压缩为摘要，保留最近 10 条
 *     摘要 key: chatmem:summary:{tenantId}:{conversationId}
 *  3. 多租户隔离：通过 {@link TenantContextHolder} 获取 tenantId 注入 key
 *  4. TTL：7 天自动过期
 *  5. 容器启动安全：{@code @Autowired(required=false)} 注入 ChatClient/AiModelService，无 LLM key 时不影响启动
 *
 * @author yudao
 */
@Service
@Slf4j
public class ChatMemoryServiceImpl implements ChatMemoryService {

    /**
     * Redis Key 前缀
     */
    private static final String KEY_SHORT_PREFIX = "chatmem:short:";
    private static final String KEY_SUMMARY_PREFIX = "chatmem:summary:";

    /**
     * 默认最近 N 条消息
     */
    private static final int DEFAULT_LAST_N = 20;

    /**
     * 短期记忆保留上限（触发压缩后保留的最近条数）
     */
    private static final int SHORT_RETAIN_AFTER_COMPRESS = 10;

    /**
     * 触发压缩的消息阈值
     */
    private static final int COMPRESS_THRESHOLD = 30;

    /**
     * 压缩时取出的旧消息条数（前 N 条）
     */
    private static final int COMPRESS_OLD_COUNT = 20;

    /**
     * TTL：7 天
     */
    private static final Duration TTL = Duration.ofDays(7);

    /**
     * Redis 模板（yudao-spring-boot-starter-redis 自动配置）
     */
    private final RedisTemplate<String, Object> redisTemplate;

    /**
     * ChatClient Bean（可选；yudao-module-ai 未直接提供时为 null）
     */
    @Autowired(required = false)
    private ChatClient chatClient;

    /**
     * AiModelService（可选；用于在 ChatClient 不可用时按需构建 ChatModel 进行摘要压缩）
     */
    @Autowired(required = false)
    private AiModelService aiModelService;

    public ChatMemoryServiceImpl(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public ChatMessageDTO addMessage(String conversationId, String messageType, String content) {
        if (StrUtil.isBlank(conversationId)) {
            throw new IllegalArgumentException("conversationId 不能为空");
        }
        ChatMessageDTO message = new ChatMessageDTO(
                IdUtil.fastSimpleUUID(),
                StrUtil.blankToDefault(messageType, "USER"),
                StrUtil.nullToDefault(content, ""),
                System.currentTimeMillis(),
                estimateTokens(content)
        );
        String key = buildShortKey(conversationId);
        redisTemplate.opsForList().rightPush(key, message);
        redisTemplate.expire(key, TTL);
        log.info("[addMessage][conversationId={}, role={}, tokens={}]", conversationId, message.role(), message.tokenCount());
        return message;
    }

    @Override
    public List<ChatMessageDTO> getMessages(String conversationId, Integer lastN) {
        if (StrUtil.isBlank(conversationId)) {
            return Collections.emptyList();
        }
        int n = (lastN == null || lastN <= 0) ? DEFAULT_LAST_N : lastN;
        String key = buildShortKey(conversationId);
        Long size = redisTemplate.opsForList().size(key);
        if (size == null || size == 0) {
            return Collections.emptyList();
        }
        // 从列表尾部取最近 n 条（LRANGE size-n .. size-1）
        long start = Math.max(0, size - n);
        List<Object> raw = redisTemplate.opsForList().range(key, start, size - 1);
        if (CollUtil.isEmpty(raw)) {
            return Collections.emptyList();
        }
        List<ChatMessageDTO> result = new ArrayList<>(raw.size());
        for (Object item : raw) {
            if (item instanceof ChatMessageDTO dto) {
                result.add(dto);
            } else if (item != null) {
                // 兼容反序列化为 LinkedHashMap 等情况
                try {
                    ChatMessageDTO dto = convert(item);
                    if (dto != null) {
                        result.add(dto);
                    }
                } catch (Exception e) {
                    log.warn("[getMessages][消息反序列化失败：{}]", item.getClass().getSimpleName());
                }
            }
        }
        return result;
    }

    @Override
    public void clearMemory(String conversationId) {
        if (StrUtil.isBlank(conversationId)) {
            return;
        }
        redisTemplate.delete(buildShortKey(conversationId));
        redisTemplate.delete(buildSummaryKey(conversationId));
        log.info("[clearMemory][清空会话记忆，conversationId={}]", conversationId);
    }

    @Override
    public String summarizeAndCompress(String conversationId) {
        if (StrUtil.isBlank(conversationId)) {
            return null;
        }
        String shortKey = buildShortKey(conversationId);
        Long size = redisTemplate.opsForList().size(shortKey);
        if (size == null || size <= COMPRESS_THRESHOLD) {
            log.info("[summarizeAndCompress][未达压缩阈值，conversationId={}, size={}]", conversationId, size);
            return null;
        }
        // 1. 取出前 COMPRESS_OLD_COUNT 条作为待压缩旧消息
        long oldEnd = COMPRESS_OLD_COUNT - 1;
        List<Object> oldRaw = redisTemplate.opsForList().range(shortKey, 0, oldEnd);
        if (CollUtil.isEmpty(oldRaw)) {
            return null;
        }
        List<ChatMessageDTO> oldMessages = new ArrayList<>(oldRaw.size());
        for (Object item : oldRaw) {
            ChatMessageDTO dto = convert(item);
            if (dto != null) {
                oldMessages.add(dto);
            }
        }
        // 2. 调用 LLM 压缩
        String newSummary;
        try {
            newSummary = callLlmSummarize(conversationId, oldMessages);
        } catch (Exception e) {
            log.error("[summarizeAndCompress][LLM 摘要失败，conversationId={}]", conversationId, e);
            return null;
        }
        // 3. 合并旧摘要 + 新摘要
        String existingSummary = getSummary(conversationId);
        String mergedSummary;
        if (StrUtil.isNotBlank(existingSummary)) {
            mergedSummary = existingSummary + "\n\n---\n\n" + newSummary;
        } else {
            mergedSummary = newSummary;
        }
        // 4.1 写入摘要 key
        String summaryKey = buildSummaryKey(conversationId);
        redisTemplate.opsForValue().set(summaryKey, mergedSummary, TTL);
        // 4.2 删除已压缩的旧消息（LTRIM 保留 oldEnd+1 .. -1）
        redisTemplate.opsForList().trim(shortKey, COMPRESS_OLD_COUNT, -1);
        redisTemplate.expire(shortKey, TTL);
        log.info("[summarizeAndCompress][压缩完成，conversationId={}, 压缩前={}, 压缩后={}, 摘要长度={}]",
                conversationId, size, size - COMPRESS_OLD_COUNT, mergedSummary.length());
        return newSummary;
    }

    @Override
    public String getSummary(String conversationId) {
        if (StrUtil.isBlank(conversationId)) {
            return null;
        }
        Object value = redisTemplate.opsForValue().get(buildSummaryKey(conversationId));
        if (value == null) {
            return null;
        }
        return value instanceof String s ? s : value.toString();
    }

    // ==================== 内部方法 ====================

    /**
     * 构造短期记忆 Redis Key（含租户隔离）
     */
    private String buildShortKey(String conversationId) {
        return KEY_SHORT_PREFIX + getTenantSegment() + ":" + conversationId;
    }

    /**
     * 构造长期摘要 Redis Key（含租户隔离）
     */
    private String buildSummaryKey(String conversationId) {
        return KEY_SUMMARY_PREFIX + getTenantSegment() + ":" + conversationId;
    }

    /**
     * 获取租户段（null 时使用 "0" 占位，保证 key 不为空）
     */
    private String getTenantSegment() {
        Long tenantId = TenantContextHolder.getTenantId();
        return tenantId == null ? "0" : String.valueOf(tenantId);
    }

    /**
     * 调用 LLM 进行摘要压缩
     */
    private String callLlmSummarize(String conversationId, List<ChatMessageDTO> oldMessages) {
        String promptContent = buildSummarizePrompt(oldMessages);
        // 1. 优先使用 ChatClient Bean
        if (chatClient != null) {
            return chatClient.prompt()
                    .system("你是一个对话记忆压缩助手。请将给定的多轮对话历史压缩为简洁的中文摘要，"
                            + "保留关键事实、用户意图与已确认的结论。")
                    .user(promptContent)
                    .call()
                    .content();
        }
        // 2. 兜底：通过 AiModelService 构建临时 ChatClient
        if (aiModelService != null) {
            AiModelDO model = aiModelService.getRequiredDefaultModel(AiModelTypeEnum.CHAT.getType());
            ChatModel chatModel = aiModelService.getChatModel(model.getId());
            ChatClient client = ChatClient.builder(chatModel).build();
            return client.prompt()
                    .system("你是一个对话记忆压缩助手。请将给定的多轮对话历史压缩为简洁的中文摘要，"
                            + "保留关键事实、用户意图与已确认的结论。")
                    .user(promptContent)
                    .call()
                    .content();
        }
        throw new ServiceException(GlobalErrorCodeConstants.INTERNAL_SERVER_ERROR, "无可用 LLM（ChatClient 与 AiModelService 均未注入），无法执行摘要压缩");
    }

    /**
     * 构造摘要压缩 Prompt
     */
    private String buildSummarizePrompt(List<ChatMessageDTO> oldMessages) {
        StringBuilder sb = new StringBuilder();
        sb.append("请将以下对话历史压缩为简洁的中文摘要：\n\n");
        for (ChatMessageDTO msg : oldMessages) {
            sb.append("[").append(msg.role()).append("] ").append(msg.content()).append("\n");
        }
        sb.append("\n请输出摘要内容（不要包含额外解释）：");
        return sb.toString();
    }

    /**
     * 将 Redis 反序列化的对象转为 ChatMessageDTO
     */
    @SuppressWarnings("unchecked")
    private ChatMessageDTO convert(Object item) {
        if (item == null) {
            return null;
        }
        if (item instanceof ChatMessageDTO dto) {
            return dto;
        }
        // Jackson 反序列化 LinkedHashMap 时，手动转换
        try {
            java.util.Map<String, Object> map = (java.util.Map<String, Object>) item;
            return new ChatMessageDTO(
                    (String) map.get("id"),
                    (String) map.get("role"),
                    (String) map.get("content"),
                    map.get("timestamp") instanceof Number n ? n.longValue() : null,
                    map.get("tokenCount") instanceof Number n ? n.intValue() : null
            );
        } catch (Exception e) {
            log.warn("[convert][转换失败：{}]", e.getMessage());
            return null;
        }
    }

    /**
     * 粗略估算 Token 数（4 字符 ≈ 1 token）
     */
    private int estimateTokens(String text) {
        if (StrUtil.isBlank(text)) {
            return 0;
        }
        return text.length() / 4;
    }

}
