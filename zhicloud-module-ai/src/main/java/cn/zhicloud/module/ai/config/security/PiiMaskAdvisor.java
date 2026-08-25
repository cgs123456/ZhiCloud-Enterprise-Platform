package cn.zhicloud.module.ai.config.security;

import cn.zhicloud.module.ai.config.AiSecurityConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PII 敏感数据脱敏 Advisor（P0-1：ChatClient 调用链内脱敏）
 *
 * <p>本 Advisor 与现有的 {@link PiiMaskConverter}（HTTP ResponseBodyAdvice）形成内外两层防护：
 * <ul>
 *   <li>外层 {@code PiiMaskConverter}：拦截 HTTP 响应序列化前的 JSON 字段（业务对象 → HTTP 响应）</li>
 *   <li>内层 本 Advisor：拦截 ChatClient.call() 内部链路：
 *     <ul>
 *       <li>请求方向：UserMessage 送入 LLM 前的 PII 脱敏（防止用户 PII 泄漏到外部 LLM API）</li>
 *       <li>响应方向：AssistantMessage 送回业务前的 PII 脱敏（防止 LLM 在回答中泄漏 PII）</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * <h3>关键设计</h3>
 * <p>实现 Spring AI 1.1.x 的 {@link CallAdvisor} 与 {@link StreamAdvisor} 接口，覆盖同步与流式调用：
 * <ol>
 *   <li>从 {@code request.prompt().getInstructions()} 取 messages，对 UserMessage 做 PII 脱敏</li>
 *   <li>用 {@code request.mutate().prompt(newPrompt).build()} 构造新的 {@link ChatClientRequest}</li>
 *   <li>调用 {@code chain.nextCall(modifiedRequest)} 获取响应</li>
 *   <li>对响应中的 {@link AssistantMessage} 做 PII 脱敏，用 {@code response.mutate().chatResponse(newResp).build()} 构造新响应</li>
 *   <li>所有 mutate 操作包裹 try-catch，运行时异常时回退原始请求/响应，确保安全</li>
 * </ol>
 *
 * <h3>子开关</h3>
 * <p>通过 {@code zhicloud.ai.security.pii-mask.mask-phone / mask-id-card / mask-email / mask-bank-card}
 * 独立控制每一类 PII 的脱敏开关，与 {@link PiiMaskConverter} 共用配置。
 *
 * <p>流式路径 {@link #adviseStream} 复用 {@link #maskRequest} 与 {@link #maskResponse}：请求侧同步脱敏后
 * 调用 {@code chain.nextStream}，响应侧用 {@code Flux.map} 对每个 chunk 调用 {@link #maskResponse} 脱敏。
 *
 * <p><b>已知限制</b>：按 chunk 脱敏，跨边界 PII 可能漏检（如手机号被切到两个 chunk 中），
 * 依赖外层 {@link PiiMaskConverter} 在 HTTP 响应序列化前兜底脱敏。
 *
 * @author zhicloud
 */
@Component
@ConditionalOnProperty(prefix = "zhicloud.ai.security.pii-mask", name = "enabled", havingValue = "true")
@Slf4j
public class PiiMaskAdvisor implements CallAdvisor, StreamAdvisor {

    /**
     * 手机号正则：前后不能是数字（避免 URL 路径中的数字串被误判）
     *
     * C6 修复：与 {@link PiiMaskConverter#PHONE_PATTERN} 对齐，补齐零宽断言 (?<!\d) 和 (?!\d)
     */
    private static final Pattern PHONE_PATTERN = Pattern.compile("(?<!\\d)1[3-9]\\d{9}(?!\\d)");

    /**
     * 身份证正则：18 位（前 17 位数字 + 末位数字或 X/x），前后不能是数字
     *
     * C6 修复：与 {@link PiiMaskConverter#IDCARD_PATTERN} 对齐，补齐零宽断言
     */
    private static final Pattern IDCARD_PATTERN = Pattern.compile("(?<!\\d)\\d{17}[\\dXx](?!\\d)");

    /**
     * 邮箱正则
     */
    private static final Pattern EMAIL_PATTERN = Pattern.compile("[\\w.-]+@[\\w.-]+\\.\\w+");

    /**
     * 银行卡正则：16-19 位数字，前后不能是数字（避免长数字串误判）
     *
     * C6 修复：与 {@link PiiMaskConverter#BANKCARD_PATTERN} 对齐，补齐零宽断言
     */
    private static final Pattern BANKCARD_PATTERN = Pattern.compile("(?<!\\d)\\d{16,19}(?!\\d)");

    @Autowired
    private AiSecurityConfiguration config;

    /**
     * Advisor 执行顺序：在 PromptInjectionAdvisor（order=100）之后执行，
     * 确保先做注入检测（可能直接拦截），再做 PII 脱敏（放行后的请求才需要脱敏）。
     */
    @Override
    public String getName() {
        return "PiiMaskAdvisor";
    }

    @Override
    public int getOrder() {
        return 200;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        // 1. 请求侧脱敏：UserMessage → 脱敏后传入 LLM
        ChatClientRequest maskedRequest = maskRequest(request);
        // 2. 调用链
        ChatClientResponse response = chain.nextCall(maskedRequest);
        // 3. 响应侧脱敏：AssistantMessage → 脱敏后返回业务
        return maskResponse(response);
    }

    /**
     * 流式调用入口（StreamAroundAdvisor 实现）
     *
     * <p>请求侧：复用 {@link #maskRequest} 同步处理 UserMessage 中的 PII。
     * 响应侧：用 {@code Flux.map} 对每个 chunk 调用 {@link #maskResponse} 脱敏。
     *
     * <p><b>已知限制</b>：按 chunk 脱敏，跨边界 PII 可能漏检，依赖外层 {@link PiiMaskConverter} 兜底。
     *
     * @param request ChatClient 请求
     * @param chain   下游 Advisor 链
     * @return Flux 流式响应（每个 chunk 已做 PII 脱敏）
     */
    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
        // 1. 请求侧脱敏（同步）
        ChatClientRequest maskedRequest = maskRequest(request);
        // 2. 调用下游 chain，对每个 chunk 做响应侧脱敏
        return chain.nextStream(maskedRequest).map(this::maskSingleChunk);
    }

    /**
     * 对单个流式 chunk 调用 {@link #maskResponse} 做 PII 脱敏
     *
     * <p>兜底处理：maskResponse 内部异常会回退原始响应，因此不会因脱敏失败中断流。
     */
    private ChatClientResponse maskSingleChunk(ChatClientResponse chunk) {
        return maskResponse(chunk);
    }

    /**
     * 请求侧 PII 脱敏：对 messages 列表中的 UserMessage 文本做脱敏
     *
     * <p>使用 Spring AI 1.1.x 的 {@code request.mutate().prompt(newPrompt).build()} builder 模式。
     * 若 builder API 不可用或失败，回退原始请求，由外层 {@link PiiMaskConverter} 兜底脱敏。
     */
    private ChatClientRequest maskRequest(ChatClientRequest request) {
        Prompt prompt = request.prompt();
        if (prompt == null) {
            return request;
        }
        List<Message> messages = prompt.getInstructions();
        if (messages == null || messages.isEmpty()) {
            return request;
        }
        boolean hasUserMessage = false;
        boolean modified = false;
        List<Message> newMessages = new ArrayList<>(messages.size());
        for (Message msg : messages) {
            if (msg instanceof UserMessage um) {
                hasUserMessage = true;
                String original = um.getText();
                if (original == null || original.isEmpty()) {
                    newMessages.add(msg);
                    continue;
                }
                String masked = maskString(original);
                if (!original.equals(masked)) {
                    modified = true;
                    newMessages.add(new UserMessage(masked));
                } else {
                    newMessages.add(msg);
                }
            } else {
                newMessages.add(msg);
            }
        }
        if (!hasUserMessage || !modified) {
            return request;
        }
        try {
            Prompt newPrompt = new Prompt(newMessages, prompt.getOptions());
            return request.mutate().prompt(newPrompt).build();
        } catch (Throwable t) {
            log.warn("[maskRequest] 修改 ChatClientRequest 失败，回退原始请求 err={}", t.toString());
            return request;
        }
    }

    /**
     * 响应侧 PII 脱敏：对 ChatResponse 中的 AssistantMessage 文本做脱敏
     *
     * <p>使用 Spring AI 1.1.x 的 {@code response.mutate().chatResponse(newResp).build()} builder 模式。
     * 若 builder API 不可用或失败，回退原始响应，由外层 {@link PiiMaskConverter} 兜底脱敏。
     */
    private ChatClientResponse maskResponse(ChatClientResponse response) {
        if (response == null) {
            return null;
        }
        ChatResponse chatResponse = response.chatResponse();
        if (chatResponse == null) {
            return response;
        }
        List<Generation> generations = chatResponse.getResults();
        if (generations == null || generations.isEmpty()) {
            return response;
        }
        boolean modified = false;
        List<Generation> newGenerations = new ArrayList<>(generations.size());
        for (Generation gen : generations) {
            AssistantMessage am = gen.getOutput();
            if (am == null || am.getText() == null || am.getText().isEmpty()) {
                newGenerations.add(gen);
                continue;
            }
            String original = am.getText();
            String masked = maskString(original);
            if (original.equals(masked)) {
                newGenerations.add(gen);
            } else {
                modified = true;
                AssistantMessage newAm = new AssistantMessage(masked);
                newGenerations.add(new Generation(newAm));
            }
        }
        if (!modified) {
            return response;
        }
        try {
            ChatResponse newChatResponse = new ChatResponse(newGenerations);
            return response.mutate().chatResponse(newChatResponse).build();
        } catch (Throwable t) {
            log.warn("[maskResponse] 修改 ChatClientResponse 失败，回退原始响应 err={}", t.toString());
            return response;
        }
    }

    // ====== Masking logic（与 PiiMaskConverter 一致，便于独立维护 Advisor 链内的脱敏规则） ======

    /**
     * 按开关顺序对字符串做 PII 脱敏
     */
    private String maskString(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        AiSecurityConfiguration.PiiMask cfg = config.getPiiMask();
        String result = input;
        try {
            if (cfg.isMaskPhone()) {
                result = maskPhone(result);
            }
            if (cfg.isMaskIdCard()) {
                result = maskIdCard(result);
            }
            if (cfg.isMaskEmail()) {
                result = maskEmail(result);
            }
            if (cfg.isMaskBankCard()) {
                result = maskBankCard(result);
            }
        } catch (Exception ex) {
            log.warn("[maskString] 字符串脱敏异常，返回原值 err={}", ex.toString());
            return input;
        }
        return result;
    }

    /**
     * 手机号脱敏：保留前 3 后 4 → {@code 138****1234}
     */
    private String maskPhone(String input) {
        Matcher m = PHONE_PATTERN.matcher(input);
        return m.replaceAll(r -> {
            String s = r.group();
            return s.length() < 7 ? s : s.substring(0, 3) + "****" + s.substring(s.length() - 4);
        });
    }

    /**
     * 身份证脱敏：保留前 6 后 4 → {@code 110101********1234}
     */
    private String maskIdCard(String input) {
        Matcher m = IDCARD_PATTERN.matcher(input);
        return m.replaceAll(r -> {
            String s = r.group();
            return s.length() < 10 ? s : s.substring(0, 6) + "********" + s.substring(s.length() - 4);
        });
    }

    /**
     * 邮箱脱敏：保留首字符与域名 → {@code a***@example.com}
     */
    private String maskEmail(String input) {
        Matcher m = EMAIL_PATTERN.matcher(input);
        return m.replaceAll(r -> {
            String s = r.group();
            int at = s.indexOf('@');
            if (at <= 0) {
                return s;
            }
            String localPart = s.substring(0, at);
            String domain = s.substring(at); // 含 @
            if (localPart.length() <= 1) {
                return localPart + "***" + domain;
            }
            return localPart.charAt(0) + "***" + domain;
        });
    }

    /**
     * 银行卡脱敏：保留后 4，前缀用 {@code ****} → {@code ****1234}
     */
    private String maskBankCard(String input) {
        Matcher m = BANKCARD_PATTERN.matcher(input);
        return m.replaceAll(r -> {
            String s = r.group();
            return s.length() < 4 ? s : "****" + s.substring(s.length() - 4);
        });
    }

}
