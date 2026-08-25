package cn.zhicloud.module.ai.config.security;

import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.module.ai.config.AiSecurityConfiguration;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.MethodParameter;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PII 敏感数据脱敏响应体处理器（SubTask 14.4）
 *
 * <p>实现 {@link ResponseBodyAdvice} 接口，在 Jackson 序列化前对 String 字段做正则脱敏，
 * 避免直接反射操作业务对象字段。
 *
 * <h3>脱敏规则</h3>
 * <ul>
 *   <li>手机号 {@code 1[3-9]\d{9}} → {@code 138****1234}（保留前 3 后 4）</li>
 *   <li>身份证 {@code \d{17}[\dXx]} → 前 6 + {@code ********} + 后 4</li>
 *   <li>邮箱 {@code [\w.-]+@[\w.-]+\.\w+} → 首字符 + {@code ***} + @ 域名</li>
 *   <li>银行卡 {@code \d{16,19}} → {@code ****} + 后 4</li>
 * </ul>
 *
 * <h3>子开关</h3>
 * <p>通过 {@code zhicloud.ai.security.pii-mask.mask-phone / mask-id-card / mask-email / mask-bank-card}
 * 独立控制每一类 PII 的脱敏开关。
 *
 * <h3>实现要点</h3>
 * <ol>
 *   <li>{@link #supports} 仅对 Jackson 转换器生效，避免对 String/byte[] 等其他转换器误处理。</li>
 *   <li>{@link #beforeBodyWrite}：
 *     <ul>
 *       <li>String body：直接正则替换。</li>
 *       <li>其他对象 body：通过 {@link ObjectMapper#valueToTree(Object)} 转为 Jackson
 *           {@link JsonNode} 树，递归替换所有 TextNode 的值；返回 JsonNode 树由 Spring
 *           Jackson 转换器继续序列化，无需手动反射业务对象字段。</li>
 *     </ul>
 *   </li>
 * </ol>
 *
 * @author 智云
 */
@ControllerAdvice
@ConditionalOnProperty(prefix = "zhicloud.ai.security.pii-mask", name = "enabled", havingValue = "true")
@Order(Ordered.LOWEST_PRECEDENCE) // 在 advice 链最后执行，避免与 GlobalResponseBodyHandler 顺序冲突
@Slf4j
public class PiiMaskConverter implements ResponseBodyAdvice<Object> {

    /**
     * 手机号正则：1[3-9]\d{9}，前后不能是数字（避免 URL 路径中的数字串被误判）
     */
    private static final Pattern PHONE_PATTERN = Pattern.compile("(?<!\\d)1[3-9]\\d{9}(?!\\d)");

    /**
     * 身份证正则：18 位（前 17 位数字 + 末位数字或 X/x），前后不能是数字
     */
    private static final Pattern IDCARD_PATTERN = Pattern.compile("(?<!\\d)\\d{17}[\\dXx](?!\\d)");

    /**
     * 邮箱正则
     */
    private static final Pattern EMAIL_PATTERN = Pattern.compile("[\\w.-]+@[\\w.-]+\\.\\w+");

    /**
     * 银行卡正则：16-19 位数字，前后不能是数字（避免长数字串误判）
     */
    private static final Pattern BANKCARD_PATTERN = Pattern.compile("(?<!\\d)\\d{16,19}(?!\\d)");

    /**
     * Jackson ObjectMapper（zhicloud 默认注入，用于响应序列化）
     */
    @Autowired
    private ObjectMapper objectMapper;

    /**
     * AI 安全配置
     */
    @Autowired
    private AiSecurityConfiguration aiSecurityConfiguration;

    /**
     * 仅对 Jackson JSON 转换器生效（避免影响 String/byte[] 等其他转换器）
     *
     * <p>同时排除非 Controller（@RestController）返回类型，避免误处理内部 RPC 序列化。
     */
    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        if (!MappingJackson2HttpMessageConverter.class.isAssignableFrom(converterType)) {
            return false;
        }
        // 限定为 RestController 标记类的方法
        Class<?> declaringClass = returnType.getContainingClass();
        return declaringClass.isAnnotationPresent(RestController.class);
    }

    /**
     * 在响应序列化前对 String 字段做 PII 脱敏
     *
     * <p>注意：本方法返回 JsonNode 树时，Spring 的 Jackson 转换器会再次序列化该树为 JSON 字符串，
     * 不存在双重序列化反序列化开销，仅做一次树形遍历替换。
     */
    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType,
                                   MediaType selectedContentType,
                                   Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                   org.springframework.http.server.ServerHttpRequest request,
                                   org.springframework.http.server.ServerHttpResponse response) {
        if (body == null) {
            return null;
        }
        // 基本类型直接返回（不涉及 PII）
        if (body instanceof Number || body instanceof Boolean || body instanceof byte[]) {
            return body;
        }
        // String 类型直接脱敏
        if (body instanceof String str) {
            return maskString(str);
        }
        // 处理 CommonResult：保持 body 类型不变，只替换 data 字段为脱敏后的 JsonNode
        // 这样下游 ResponseBodyAdvice（如 GlobalResponseBodyHandler）仍能强转 CommonResult，避免 ClassCastException
        if (body instanceof CommonResult<?> result) {
            Object data = result.getData();
            if (data == null) {
                return body;
            }
            try {
                JsonNode tree = objectMapper.valueToTree(data);
                maskJsonNode(tree);
                // 用 raw type 把 JsonNode 写回 data 字段（Jackson 序列化时会正确处理 JsonNode）
                @SuppressWarnings({"rawtypes", "unchecked"})
                CommonResult raw = (CommonResult) result;
                raw.setData(tree);
                if (log.isDebugEnabled()) {
                    log.debug("[PiiMask#beforeBodyWrite] CommonResult 脱敏完成 data={}, hasMobile={}",
                            data.getClass().getSimpleName(),
                            tree.toString().matches(".*1[3-9]\\d{9}.*"));
                }
                return body;
            } catch (Exception ex) {
                log.warn("[PiiMask] CommonResult 脱敏失败，返回原始 body data type={}",
                        data.getClass().getSimpleName(), ex);
                return body;
            }
        }
        // 其他复杂对象：转 JsonNode 树递归脱敏
        try {
            JsonNode tree = objectMapper.valueToTree(body);
            maskJsonNode(tree);
            return tree;
        } catch (Exception ex) {
            log.warn("[PiiMask] 脱敏失败，返回原始 body type={}", body.getClass().getSimpleName(), ex);
            return body;
        }
    }

    /**
     * 递归遍历 JsonNode 树，将 TextNode 的值替换为脱敏后的字符串
     */
    private void maskJsonNode(JsonNode node) {
        if (node == null) {
            return;
        }
        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;
            Iterator<Map.Entry<String, JsonNode>> fields = objectNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                JsonNode value = entry.getValue();
                if (value.isTextual()) {
                    String masked = maskString(value.asText());
                    objectNode.set(entry.getKey(), TextNode.valueOf(masked));
                } else {
                    maskJsonNode(value);
                }
            }
        } else if (node.isArray()) {
            for (JsonNode element : node) {
                maskJsonNode(element);
            }
        }
        // 其他类型（数字、布尔、null）不做处理
    }

    /**
     * 按开关顺序对字符串做 PII 脱敏
     */
    private String maskString(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        AiSecurityConfiguration.PiiMask config = aiSecurityConfiguration.getPiiMask();
        String result = input;
        try {
            if (config.isMaskPhone()) {
                result = maskPhone(result);
            }
            if (config.isMaskIdCard()) {
                result = maskIdCard(result);
            }
            if (config.isMaskEmail()) {
                result = maskEmail(result);
            }
            if (config.isMaskBankCard()) {
                result = maskBankCard(result);
            }
        } catch (Exception ex) {
            log.warn("[PiiMask] 字符串脱敏异常，返回原值 input={}", truncate(input, 100), ex);
            return input;
        }
        return result;
    }

    /**
     * 手机号脱敏：保留前 3 后 4 → {@code 138****1234}
     */
    private String maskPhone(String input) {
        Matcher matcher = PHONE_PATTERN.matcher(input);
        return matcher.replaceAll(mr -> {
            String phone = mr.group();
            if (phone.length() < 7) {
                return phone;
            }
            return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
        });
    }

    /**
     * 身份证脱敏：保留前 6 后 4 → {@code 110101********1234}
     */
    private String maskIdCard(String input) {
        Matcher matcher = IDCARD_PATTERN.matcher(input);
        return matcher.replaceAll(mr -> {
            String idCard = mr.group();
            if (idCard.length() < 10) {
                return idCard;
            }
            return idCard.substring(0, 6) + "********" + idCard.substring(idCard.length() - 4);
        });
    }

    /**
     * 邮箱脱敏：保留首字符与域名 → {@code a***@example.com}
     */
    private String maskEmail(String input) {
        Matcher matcher = EMAIL_PATTERN.matcher(input);
        return matcher.replaceAll(mr -> {
            String email = mr.group();
            int at = email.indexOf('@');
            if (at <= 0) {
                return email;
            }
            String localPart = email.substring(0, at);
            String domain = email.substring(at); // 含 @
            if (localPart.length() <= 1) {
                return localPart + "***" + domain;
            }
            return localPart.charAt(0) + "***" + domain;
        });
    }

    /**
     * 银行卡脱敏：保留后 4，前缀用 {@code ****} → {@code **** **** **** 1234}
     */
    private String maskBankCard(String input) {
        Matcher matcher = BANKCARD_PATTERN.matcher(input);
        return matcher.replaceAll(mr -> {
            String card = mr.group();
            if (card.length() < 4) {
                return card;
            }
            return "****" + card.substring(card.length() - 4);
        });
    }

    /**
     * 截断字符串便于日志展示
     */
    private String truncate(String str, int max) {
        if (str == null) {
            return "null";
        }
        return str.length() <= max ? str : str.substring(0, max) + "...(" + str.length() + " chars)";
    }

}
