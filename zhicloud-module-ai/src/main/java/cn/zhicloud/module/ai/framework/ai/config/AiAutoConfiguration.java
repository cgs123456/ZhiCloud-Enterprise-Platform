package cn.zhicloud.module.ai.framework.ai.config;

import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import cn.zhicloud.module.ai.framework.ai.core.model.AiModelFactory;
import cn.zhicloud.module.ai.framework.ai.core.model.AiModelFactoryImpl;
import cn.zhicloud.module.ai.framework.ai.core.model.baichuan.BaiChuanChatModel;
import cn.zhicloud.module.ai.framework.ai.core.model.doubao.DouBaoChatModel;
import cn.zhicloud.module.ai.framework.ai.core.model.gemini.GeminiChatModel;
import cn.zhicloud.module.ai.framework.ai.core.model.grok.GrokChatModel;
import cn.zhicloud.module.ai.framework.ai.core.model.hunyuan.HunYuanChatModel;
import cn.zhicloud.module.ai.framework.ai.core.model.midjourney.api.MidjourneyApi;
import cn.zhicloud.module.ai.framework.ai.core.model.minimax.MiniMaxChatModel;
import cn.zhicloud.module.ai.framework.ai.core.model.moonshot.MoonshotChatModel;
import cn.zhicloud.module.ai.framework.ai.core.model.siliconflow.SiliconFlowApiConstants;
import cn.zhicloud.module.ai.framework.ai.core.model.siliconflow.SiliconFlowChatModel;
import cn.zhicloud.module.ai.framework.ai.core.model.stepfun.StepFunChatModel;
import cn.zhicloud.module.ai.framework.ai.core.model.suno.api.SunoApi;
import cn.zhicloud.module.ai.framework.ai.core.model.xinghuo.XingHuoChatModel;
import cn.zhicloud.module.ai.framework.ai.core.model.yiyan.YiYanChatModel;
import cn.zhicloud.module.ai.framework.ai.core.model.zhipu.ZhiPuChatModel;
import cn.zhicloud.module.ai.framework.ai.core.webserch.AiWebSearchClient;
import cn.zhicloud.module.ai.framework.ai.core.webserch.bocha.AiBoChaWebSearchClient;
import cn.zhicloud.module.ai.mcp.tools.ErpMcpTools;
import cn.zhicloud.module.ai.mcp.tools.MesMcpTools;
import cn.zhicloud.module.ai.mcp.tools.QmsMcpTools;
import cn.zhicloud.module.ai.mcp.tools.WmsMcpTools;
import cn.zhicloud.module.ai.tool.method.PersonService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.deepseek.DeepSeekChatOptions;
import org.springframework.ai.deepseek.api.DeepSeekApi;
import org.springframework.ai.embedding.BatchingStrategy;
import org.springframework.ai.embedding.TokenCountBatchingStrategy;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tokenizer.JTokkitTokenCountEstimator;
import org.springframework.ai.tokenizer.TokenCountEstimator;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.vectorstore.milvus.autoconfigure.MilvusServiceClientProperties;
import org.springframework.ai.vectorstore.milvus.autoconfigure.MilvusVectorStoreProperties;
import org.springframework.ai.vectorstore.qdrant.autoconfigure.QdrantVectorStoreProperties;
import org.springframework.ai.vectorstore.redis.autoconfigure.RedisVectorStoreProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 智云 AI 自动配置
 *
 * @author fansili
 */
@Configuration
@EnableConfigurationProperties({ ZhiCloudAiProperties.class,
        QdrantVectorStoreProperties.class, // 解析 Qdrant 配置
        RedisVectorStoreProperties.class, // 解析 Redis 配置
        MilvusVectorStoreProperties.class, MilvusServiceClientProperties.class // 解析 Milvus 配置
})
@Slf4j
public class AiAutoConfiguration {

    @Bean
    public AiModelFactory aiModelFactory() {
        return new AiModelFactoryImpl();
    }

    // P1-4 可观测性：移除 @ConditionalOnMissingBean ObservationRegistry NOOP 兜底 Bean
    // 原因：NOOP 会抢先注册并覆盖 Spring Boot ObservationAutoConfiguration 提供的 DefaultObservationRegistry
    //       导致 http.server.requests / gen_ai.* / db.* 等指标全部丢失
    // 修复：让 Spring Boot 自动注入 DefaultObservationRegistry，ChatModel/WebMvc/RestTemplate 等组件均使用真实 Registry
    // 引用 issue：https://t.zsxq.com/CuPu4（原兜底是为解决未引入 actuator 时 ChatModel 创建报错，当前项目已强制依赖 actuator）

    // ========== 各种 AI Client 创建 ==========

    @Bean
    @ConditionalOnProperty(value = "zhicloud.ai.gemini.enable", havingValue = "true")
    public GeminiChatModel geminiChatModel(ZhiCloudAiProperties zhicloudAiProperties) {
        ZhiCloudAiProperties.Gemini properties = zhicloudAiProperties.getGemini();
        return buildGeminiChatClient(properties);
    }

    public GeminiChatModel buildGeminiChatClient(ZhiCloudAiProperties.Gemini properties) {
        if (StrUtil.isEmpty(properties.getModel())) {
            properties.setModel(GeminiChatModel.MODEL_DEFAULT);
        }
        OpenAiChatModel openAiChatModel = OpenAiChatModel.builder()
                .openAiApi(OpenAiApi.builder()
                        .baseUrl(GeminiChatModel.BASE_URL)
                        .completionsPath(GeminiChatModel.COMPLETE_PATH)
                        .apiKey(properties.getApiKey())
                        .build())
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(properties.getModel())
                        .temperature(properties.getTemperature())
                        .maxTokens(properties.getMaxTokens())
                        .topP(properties.getTopP())
                        .build())
                .toolCallingManager(getToolCallingManager())
                .build();
        return new GeminiChatModel(openAiChatModel);
    }

    @Bean
    @ConditionalOnProperty(value = "zhicloud.ai.doubao.enable", havingValue = "true")
    public DouBaoChatModel douBaoChatClient(ZhiCloudAiProperties zhicloudAiProperties) {
        ZhiCloudAiProperties.DouBao properties = zhicloudAiProperties.getDoubao();
        return buildDouBaoChatClient(properties);
    }

    public DouBaoChatModel buildDouBaoChatClient(ZhiCloudAiProperties.DouBao properties) {
        if (StrUtil.isEmpty(properties.getModel())) {
            properties.setModel(DouBaoChatModel.MODEL_DEFAULT);
        }
        DeepSeekChatModel openAiChatModel = DeepSeekChatModel.builder()
                .deepSeekApi(DeepSeekApi.builder()
                        .baseUrl(DouBaoChatModel.BASE_URL)
                        .completionsPath(DouBaoChatModel.COMPLETE_PATH)
                        .apiKey(properties.getApiKey())
                        .build())
                .defaultOptions(DeepSeekChatOptions.builder()
                        .model(properties.getModel())
                        .temperature(properties.getTemperature())
                        .maxTokens(properties.getMaxTokens())
                        .topP(properties.getTopP())
                        .build())
                .toolCallingManager(getToolCallingManager())
                .build();
        return new DouBaoChatModel(openAiChatModel);
    }

    @Bean
    @ConditionalOnProperty(value = "zhicloud.ai.siliconflow.enable", havingValue = "true")
    public SiliconFlowChatModel siliconFlowChatClient(ZhiCloudAiProperties zhicloudAiProperties) {
        ZhiCloudAiProperties.SiliconFlow properties = zhicloudAiProperties.getSiliconflow();
        return buildSiliconFlowChatClient(properties);
    }

    public SiliconFlowChatModel buildSiliconFlowChatClient(ZhiCloudAiProperties.SiliconFlow properties) {
        if (StrUtil.isEmpty(properties.getModel())) {
            properties.setModel(SiliconFlowApiConstants.MODEL_DEFAULT);
        }
        DeepSeekChatModel openAiChatModel = DeepSeekChatModel.builder()
                .deepSeekApi(DeepSeekApi.builder()
                        .baseUrl(SiliconFlowApiConstants.DEFAULT_BASE_URL)
                        .apiKey(properties.getApiKey())
                        .build())
                .defaultOptions(DeepSeekChatOptions.builder()
                        .model(properties.getModel())
                        .temperature(properties.getTemperature())
                        .maxTokens(properties.getMaxTokens())
                        .topP(properties.getTopP())
                        .build())
                .toolCallingManager(getToolCallingManager())
                .build();
        return new SiliconFlowChatModel(openAiChatModel);
    }

    @Bean
    @ConditionalOnProperty(value = "zhicloud.ai.hunyuan.enable", havingValue = "true")
    public HunYuanChatModel hunYuanChatClient(ZhiCloudAiProperties zhicloudAiProperties) {
        ZhiCloudAiProperties.HunYuan properties = zhicloudAiProperties.getHunyuan();
        return buildHunYuanChatClient(properties);
    }

    public HunYuanChatModel buildHunYuanChatClient(ZhiCloudAiProperties.HunYuan properties) {
        if (StrUtil.isEmpty(properties.getModel())) {
            properties.setModel(HunYuanChatModel.MODEL_DEFAULT);
        }
        if (StrUtil.isEmpty(properties.getBaseUrl())) {
            properties.setBaseUrl(HunYuanChatModel.BASE_URL);
        }
        // 创建 DeepSeekChatModel、HunYuanChatModel 对象
        DeepSeekChatModel openAiChatModel = DeepSeekChatModel.builder()
                .deepSeekApi(DeepSeekApi.builder()
                        .baseUrl(properties.getBaseUrl())
                        .completionsPath(HunYuanChatModel.COMPLETE_PATH)
                        .apiKey(properties.getApiKey())
                        .build())
                .defaultOptions(DeepSeekChatOptions.builder()
                        .model(properties.getModel())
                        .temperature(properties.getTemperature())
                        .maxTokens(properties.getMaxTokens())
                        .topP(properties.getTopP())
                        .build())
                .toolCallingManager(getToolCallingManager())
                .build();
        return new HunYuanChatModel(openAiChatModel);
    }

    @Bean
    @ConditionalOnProperty(value = "zhicloud.ai.xinghuo.enable", havingValue = "true")
    public XingHuoChatModel xingHuoChatClient(ZhiCloudAiProperties zhicloudAiProperties) {
        ZhiCloudAiProperties.XingHuo properties = zhicloudAiProperties.getXinghuo();
        return buildXingHuoChatClient(properties);
    }

    public XingHuoChatModel buildXingHuoChatClient(ZhiCloudAiProperties.XingHuo properties) {
        if (StrUtil.isEmpty(properties.getModel())) {
            properties.setModel(XingHuoChatModel.MODEL_DEFAULT);
        }
        return XingHuoChatModel.builder()
                .apiKey(properties.getApiKey())
                .options(DeepSeekChatOptions.builder()
                        .model(properties.getModel())
                        .temperature(properties.getTemperature())
                        .maxTokens(properties.getMaxTokens())
                        .topP(properties.getTopP())
                        .build())
                .build();
    }

    @Bean
    @ConditionalOnProperty(value = "zhicloud.ai.baichuan.enable", havingValue = "true")
    public BaiChuanChatModel baiChuanChatClient(ZhiCloudAiProperties zhicloudAiProperties) {
        ZhiCloudAiProperties.BaiChuan properties = zhicloudAiProperties.getBaichuan();
        return buildBaiChuanChatClient(properties);
    }

    public BaiChuanChatModel buildBaiChuanChatClient(ZhiCloudAiProperties.BaiChuan properties) {
        if (StrUtil.isEmpty(properties.getModel())) {
            properties.setModel(BaiChuanChatModel.MODEL_DEFAULT);
        }
        DeepSeekChatModel deepSeekChatModel = DeepSeekChatModel.builder()
                .deepSeekApi(DeepSeekApi.builder()
                        .baseUrl(BaiChuanChatModel.BASE_URL)
                        .apiKey(properties.getApiKey())
                        .completionsPath(BaiChuanChatModel.COMPLETE_PATH)
                        .build())
                .defaultOptions(DeepSeekChatOptions.builder()
                        .model(properties.getModel())
                        .temperature(properties.getTemperature())
                        .maxTokens(properties.getMaxTokens())
                        .topP(properties.getTopP())
                        .build())
                .toolCallingManager(getToolCallingManager())
                .build();
        return new BaiChuanChatModel(deepSeekChatModel);
    }

    @Bean
    @ConditionalOnProperty(value = "zhicloud.ai.yiyan.enable", havingValue = "true")
    public YiYanChatModel yiYanChatClient(ZhiCloudAiProperties zhicloudAiProperties) {
        ZhiCloudAiProperties.YiYan properties = zhicloudAiProperties.getYiyan();
        return buildYiYanChatClient(properties);
    }

    public YiYanChatModel buildYiYanChatClient(ZhiCloudAiProperties.YiYan properties) {
        if (StrUtil.isEmpty(properties.getModel())) {
            properties.setModel(YiYanChatModel.MODEL_DEFAULT);
        }
        return new YiYanChatModel(buildDeepSeekCompatibleChatModel(
                StrUtil.blankToDefault(properties.getBaseUrl(), YiYanChatModel.BASE_URL),
                null, properties.getApiKey(), properties.getModel(), properties.getTemperature(),
                properties.getMaxTokens(), properties.getTopP()));
    }

    @Bean
    @ConditionalOnProperty(value = "zhicloud.ai.zhipu.enable", havingValue = "true")
    public ZhiPuChatModel zhiPuChatClient(ZhiCloudAiProperties zhicloudAiProperties) {
        ZhiCloudAiProperties.ZhiPu properties = zhicloudAiProperties.getZhipu();
        return buildZhiPuChatClient(properties);
    }

    public ZhiPuChatModel buildZhiPuChatClient(ZhiCloudAiProperties.ZhiPu properties) {
        if (StrUtil.isEmpty(properties.getModel())) {
            properties.setModel(ZhiPuChatModel.MODEL_DEFAULT);
        }
        DeepSeekChatModel deepSeekChatModel = DeepSeekChatModel.builder()
                .deepSeekApi(DeepSeekApi.builder()
                        .baseUrl(StrUtil.blankToDefault(properties.getBaseUrl(), ZhiPuChatModel.BASE_URL))
                        .apiKey(properties.getApiKey())
                        .build())
                .defaultOptions(DeepSeekChatOptions.builder()
                        .model(properties.getModel())
                        .temperature(properties.getTemperature())
                        .maxTokens(properties.getMaxTokens())
                        .topP(properties.getTopP())
                        .build())
                .build();
        return new ZhiPuChatModel(deepSeekChatModel);
    }

    @Bean
    @ConditionalOnProperty(value = "zhicloud.ai.minimax.enable", havingValue = "true")
    public MiniMaxChatModel miniMaxChatClient(ZhiCloudAiProperties zhicloudAiProperties) {
        ZhiCloudAiProperties.MiniMax properties = zhicloudAiProperties.getMinimax();
        return buildMiniMaxChatClient(properties);
    }

    public MiniMaxChatModel buildMiniMaxChatClient(ZhiCloudAiProperties.MiniMax properties) {
        if (StrUtil.isEmpty(properties.getModel())) {
            properties.setModel(MiniMaxChatModel.MODEL_DEFAULT);
        }
        return new MiniMaxChatModel(buildDeepSeekCompatibleChatModel(
                StrUtil.blankToDefault(properties.getBaseUrl(), MiniMaxChatModel.BASE_URL),
                null, properties.getApiKey(), properties.getModel(), properties.getTemperature(),
                properties.getMaxTokens(), properties.getTopP()));
    }

    @Bean
    @ConditionalOnProperty(value = "zhicloud.ai.moonshot.enable", havingValue = "true")
    public MoonshotChatModel moonshotChatClient(ZhiCloudAiProperties zhicloudAiProperties) {
        ZhiCloudAiProperties.Moonshot properties = zhicloudAiProperties.getMoonshot();
        return buildMoonshotChatClient(properties);
    }

    public MoonshotChatModel buildMoonshotChatClient(ZhiCloudAiProperties.Moonshot properties) {
        if (StrUtil.isEmpty(properties.getModel())) {
            properties.setModel(MoonshotChatModel.MODEL_DEFAULT);
        }
        return new MoonshotChatModel(buildDeepSeekCompatibleChatModel(
                StrUtil.blankToDefault(properties.getBaseUrl(), MoonshotChatModel.BASE_URL),
                MoonshotChatModel.COMPLETE_PATH, properties.getApiKey(), properties.getModel(),
                properties.getTemperature(), properties.getMaxTokens(), properties.getTopP()));
    }

    @Bean
    @ConditionalOnProperty(value = "zhicloud.ai.stepfun.enable", havingValue = "true")
    public StepFunChatModel stepFunChatClient(ZhiCloudAiProperties zhicloudAiProperties) {
        ZhiCloudAiProperties.StepFun properties = zhicloudAiProperties.getStepfun();
        return buildStepFunChatClient(properties);
    }

    public StepFunChatModel buildStepFunChatClient(ZhiCloudAiProperties.StepFun properties) {
        if (StrUtil.isEmpty(properties.getModel())) {
            properties.setModel(StepFunChatModel.MODEL_DEFAULT);
        }
        return new StepFunChatModel(buildDeepSeekCompatibleChatModel(
                StrUtil.blankToDefault(properties.getBaseUrl(), StepFunChatModel.BASE_URL),
                StepFunChatModel.COMPLETE_PATH, properties.getApiKey(), properties.getModel(),
                properties.getTemperature(), properties.getMaxTokens(), properties.getTopP()));
    }

    private static DeepSeekChatModel buildDeepSeekCompatibleChatModel(String baseUrl, String completionsPath,
                                                                     String apiKey, String model,
                                                                     Double temperature, Integer maxTokens,
                                                                     Double topP) {
        DeepSeekApi.Builder apiBuilder = DeepSeekApi.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey);
        if (StrUtil.isNotEmpty(completionsPath)) {
            apiBuilder.completionsPath(completionsPath);
        }
        return DeepSeekChatModel.builder()
                .deepSeekApi(apiBuilder.build())
                .defaultOptions(DeepSeekChatOptions.builder()
                        .model(model)
                        .temperature(temperature)
                        .maxTokens(maxTokens)
                        .topP(topP)
                        .build())
                .toolCallingManager(getToolCallingManager())
                .build();
    }

    @Bean
    @ConditionalOnProperty(value = "zhicloud.ai.midjourney.enable", havingValue = "true")
    public MidjourneyApi midjourneyApi(ZhiCloudAiProperties zhicloudAiProperties) {
        ZhiCloudAiProperties.Midjourney config = zhicloudAiProperties.getMidjourney();
        return new MidjourneyApi(config.getBaseUrl(), config.getApiKey(), config.getNotifyUrl());
    }

    @Bean
    @ConditionalOnProperty(value = "zhicloud.ai.suno.enable", havingValue = "true")
    public SunoApi sunoApi(ZhiCloudAiProperties zhicloudAiProperties) {
        return new SunoApi(zhicloudAiProperties.getSuno().getBaseUrl());
    }

    @Bean
    @ConditionalOnProperty(value = "zhicloud.ai.grok.enable", havingValue = "true")
    public GrokChatModel grokChatClient(ZhiCloudAiProperties zhicloudAiProperties) {
        ZhiCloudAiProperties.Grok properties = zhicloudAiProperties.getGrok();
        return buildGrokChatClient(properties);
    }

    public GrokChatModel buildGrokChatClient(ZhiCloudAiProperties.Grok properties) {
        if (StrUtil.isEmpty(properties.getModel())) {
            properties.setModel(GrokChatModel.MODEL_DEFAULT);
        }
        OpenAiChatModel openAiChatModel = OpenAiChatModel.builder()
                .openAiApi(OpenAiApi.builder()
                        .baseUrl(StrUtil.blankToDefault(properties.getBaseUrl(), GrokChatModel.BASE_URL))
                        .completionsPath(GrokChatModel.COMPLETE_PATH)
                        .apiKey(properties.getApiKey())
                        .build())
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(properties.getModel())
                        .temperature(properties.getTemperature())
                        .maxTokens(properties.getMaxTokens())
                        .topP(properties.getTopP())
                        .build())
                .toolCallingManager(getToolCallingManager())
                .build();
        return new GrokChatModel(openAiChatModel);
    }

    // ========== RAG 相关 ==========

    @Bean
    public TokenCountEstimator tokenCountEstimator() {
        return new JTokkitTokenCountEstimator();
    }

    @Bean
    public BatchingStrategy batchingStrategy() {
        return new TokenCountBatchingStrategy();
    }

    private static ToolCallingManager getToolCallingManager() {
        return SpringUtil.getBean(ToolCallingManager.class);
    }

    // ========== Web Search 相关 ==========

    @Bean
    @ConditionalOnProperty(value = "zhicloud.ai.web-search.enable", havingValue = "true")
    public AiWebSearchClient webSearchClient(ZhiCloudAiProperties zhicloudAiProperties) {
        return new AiBoChaWebSearchClient(zhicloudAiProperties.getWebSearch().getApiKey());
    }

    // ========== MCP 相关 ==========

    /**
     * 参考自 <a href="https://docs.spring.io/spring-ai/reference/api/mcp/mcp-client-boot-starter-docs.html">MCP Server Boot Starter</>
     *
     * 注册所有 {@link org.springframework.ai.tool.annotation.Tool} 标注的 Bean：
     *  - {@link PersonService}：示例工具
     *  - {@link WmsMcpTools} / {@link MesMcpTools} / {@link ErpMcpTools} / {@link QmsMcpTools}：业务模块包装工具
     *    （通过 {@code ObjectProvider} 安全注入，仅在对应业务模块存在时注册）
     */
    @Bean
    public List<ToolCallback> toolCallbacks(PersonService personService,
                                            ObjectProvider<WmsMcpTools> wmsMcpToolsProvider,
                                            ObjectProvider<MesMcpTools> mesMcpToolsProvider,
                                            ObjectProvider<ErpMcpTools> erpMcpToolsProvider,
                                            ObjectProvider<QmsMcpTools> qmsMcpToolsProvider,
                                            ObjectProvider<ToolCallback[]> externalToolArrays) {
        List<ToolCallback> callbacks = new ArrayList<>();
        // 示例工具
        callbacks.addAll(List.of(ToolCallbacks.from(personService)));
        // WMS MCP Tools（仅当 WMS 模块加载时存在）
        WmsMcpTools wmsMcpTools = wmsMcpToolsProvider.getIfAvailable();
        if (wmsMcpTools != null) {
            callbacks.addAll(List.of(ToolCallbacks.from(wmsMcpTools)));
            log.info("[AiAutoConfiguration] 注册 WmsMcpTools 成功");
        }
        // MES MCP Tools（仅当 MES 模块加载时存在）
        MesMcpTools mesMcpTools = mesMcpToolsProvider.getIfAvailable();
        if (mesMcpTools != null) {
            callbacks.addAll(List.of(ToolCallbacks.from(mesMcpTools)));
            log.info("[AiAutoConfiguration] 注册 MesMcpTools 成功");
        }
        // ERP MCP Tools（仅当 ERP 模块加载时存在）
        ErpMcpTools erpMcpTools = erpMcpToolsProvider.getIfAvailable();
        if (erpMcpTools != null) {
            callbacks.addAll(List.of(ToolCallbacks.from(erpMcpTools)));
            log.info("[AiAutoConfiguration] 注册 ErpMcpTools 成功");
        }
        // QMS MCP Tools（仅当 QMS 模块加载时存在）
        QmsMcpTools qmsMcpTools = qmsMcpToolsProvider.getIfAvailable();
        if (qmsMcpTools != null) {
            callbacks.addAll(List.of(ToolCallbacks.from(qmsMcpTools)));
            log.info("[AiAutoConfiguration] 注册 QmsMcpTools 成功");
        }
        // 其它模块（如数据湖仓）通过 ToolCallback[] 暴露的 MCP 工具，避免与上方 List<ToolCallback> 类型冲突
        for (ToolCallback[] arr : externalToolArrays) {
            if (arr != null) {
                callbacks.addAll(Arrays.asList(arr));
                log.info("[AiAutoConfiguration] 聚合外部模块 ToolCallback 数组成功");
            }
        }
        return callbacks;
    }

}
