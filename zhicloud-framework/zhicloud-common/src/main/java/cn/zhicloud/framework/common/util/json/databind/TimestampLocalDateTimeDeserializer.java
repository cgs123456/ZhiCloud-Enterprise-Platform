package cn.zhicloud.framework.common.util.json.databind;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * 基于时间戳的 LocalDateTime 反序列化器
 *
 * <p>支持两种输入（§12.220 真实缺陷修复）：
 * <ul>
 *   <li><b>数字（EpochMillis）</b>：既有行为，全仓既有前端页面都按毫秒数传参，必须保持；</li>
 *   <li><b>ISO 字符串</b>（如 {@code "2026-08-01T10:00:00"}，可带 {@code T} 或空格分隔、可带秒或毫秒）：
 *       此前这类输入会被 {@code getValueAsLong()} 解析成 0，静默落库成 epoch 0
 *       （东八区即 1970-01-01 07:30:00），表单「保存成功」但时间是 1970，
 *       且时段冲突检测等功能因此失效。新增域（OA 会议预约/日程等）的前端按
 *       ISO 字符串传参，必须兼容。</li>
 * </ul>
 *
 * @author 老五
 */
public class TimestampLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {

    /** 兼容的字符串格式：ISO 的 T 分隔与常见空格分隔，秒可选 */
    private static final DateTimeFormatter[] FORMATTERS = new DateTimeFormatter[] {
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm[:ss[.SSSSSSSS]]"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd[ ][HH:mm[:ss[.SSSSSSSS]]]")
    };

    public static final TimestampLocalDateTimeDeserializer INSTANCE = new TimestampLocalDateTimeDeserializer();

    @Override
    public LocalDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String text = p.getValueAsString();
        // 1) 数字输入：EpochMillis（既有行为，优先）
        if (text != null && !text.isEmpty()) {
            try {
                long millis = Long.parseLong(text.trim());
                return LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault());
            } catch (NumberFormatException ignored) {
                // 非数字，落到字符串解析
            }
            // 2) 字符串输入：按兼容格式解析
            for (DateTimeFormatter f : FORMATTERS) {
                try {
                    return LocalDateTime.parse(text.trim(), f);
                } catch (Exception ignored) {
                    // 尝试下一个格式
                }
            }
            // 3) 兜底：ISO_DATE_TIME（带时区偏移等完整形态）
            return LocalDateTime.parse(text.trim());
        }
        // 4) 空/null：交给 Bean Validation 处理
        return null;
    }

}
