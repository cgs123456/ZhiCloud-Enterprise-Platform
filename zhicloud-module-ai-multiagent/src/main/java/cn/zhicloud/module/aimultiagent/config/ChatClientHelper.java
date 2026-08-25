package cn.zhicloud.module.aimultiagent.config;

import cn.zhicloud.framework.common.exception.enums.GlobalErrorCodeConstants;
import cn.zhicloud.framework.common.exception.ServiceException;
import cn.zhicloud.module.ai.dal.dataobject.model.AiModelDO;
import cn.zhicloud.module.ai.enums.model.AiModelTypeEnum;
import cn.zhicloud.module.ai.service.model.AiModelService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * ChatClient 获取助手
 *
 * 设计要点：
 *  1. 通过 {@code @Autowired(required=false)} 注入 {@link AiModelService}，避免无 LLM API key 时启动失败；
 *  2. 每次调用 {@link #getChatClient()} 时从 AiModelService 获取默认对话模型并构建 ChatClient，
 *     与 zhicloud-module-ai-rag 的 {@code AiragRagServiceImpl} 保持一致；
 *  3. 当 AiModelService 不可用或无默认模型时，抛出明确异常，由调用方决定是否熔断。
 *
 * @author zhicloud
 */
@Component
@Slf4j
public class ChatClientHelper {

    /**
     * zhicloud-module-ai 的模型服务，用于获取默认 ChatModel。
     * zhicloud-server 同时引入 zhicloud-module-ai 与本模块时可用。
     * 使用 required=false，避免无 LLM API key 时容器启动失败。
     */
    @Autowired(required = false)
    private AiModelService aiModelService;

    /**
     * 检查 LLM 是否可用
     *
     * @return true 表示 AiModelService 已注入，可以尝试获取 ChatModel
     */
    public boolean isAvailable() {
        return aiModelService != null;
    }

    /**
     * 获取 ChatClient
     *
     * 优先使用 zhicloud-module-ai 的默认对话模型构建 ChatClient。
     * 若 AiModelService 不可用，则抛出 IllegalStateException。
     *
     * @return ChatClient 实例
     */
    public ChatClient getChatClient() {
        if (aiModelService == null) {
            throw new ServiceException(GlobalErrorCodeConstants.INTERNAL_SERVER_ERROR, "AiModelService 不可用，请确保 zhicloud-module-ai 已加载且配置了 LLM API Key");
        }
        AiModelDO model = aiModelService.getRequiredDefaultModel(AiModelTypeEnum.CHAT.getType());
        ChatModel chatModel = aiModelService.getChatModel(model.getId());
        return ChatClient.builder(chatModel).build();
    }

}
