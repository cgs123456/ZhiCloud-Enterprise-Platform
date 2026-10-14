package cn.zhicloud.framework.common.util.json.databind;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * TimestampLocalDateTimeDeserializer 测试
 *
 * <p>用真实 ObjectMapper + 字段绑定验证（比 mock JsonParser 更贴近真实链路），
 * 锁死三种输入（§12.220 修复：此前 ISO 字符串被 {@code getValueAsLong()} 静默
 * 解析成 0，东八区落库成 1970-01-01 07:30:00）：
 * ① EpochMillis 数字（既有前端行为，必须保持）
 * ② ISO 字符串（T 分隔 / 空格分隔 / 秒可选）
 * ③ 非法字符串抛异常（不静默吞成 epoch 0）
 */
public class TimestampLocalDateTimeDeserializerTest {

    private static class Holder {
        public LocalDateTime time;
    }

    private final ObjectMapper mapper;

    public TimestampLocalDateTimeDeserializerTest() {
        mapper = new ObjectMapper();
        SimpleModule m = new SimpleModule();
        m.addDeserializer(LocalDateTime.class, TimestampLocalDateTimeDeserializer.INSTANCE);
        mapper.registerModule(m);
        // 与 ZhiCloudJacksonAutoConfiguration 同款：数字也允许（禁用 FAIL_ON_NUMBERS）
        mapper.disable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
    }

    private LocalDateTime read(String json) throws Exception {
        return mapper.readValue(json, Holder.class).time;
    }

    @Test
    public void epochMillisStillWorks() throws Exception {
        long millis = 1754032800000L; // 2025-08-01T10:00:00Z
        assertEquals(LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(millis),
                java.time.ZoneId.systemDefault()), read("{\"time\":" + millis + "}"));
    }

    @Test
    public void isoStringWithTSeparator() throws Exception {
        assertEquals(LocalDateTime.of(2026, 8, 1, 10, 0, 0),
                read("{\"time\":\"2026-08-01T10:00:00\"}"));
    }

    @Test
    public void isoStringWithoutSeconds() throws Exception {
        assertEquals(LocalDateTime.of(2026, 8, 1, 10, 0, 0),
                read("{\"time\":\"2026-08-01T10:00\"}"));
    }

    @Test
    public void isoStringWithSpaceSeparator() throws Exception {
        assertEquals(LocalDateTime.of(2026, 8, 1, 10, 0, 0),
                read("{\"time\":\"2026-08-01 10:00:00\"}"));
    }

    @Test
    public void nullValueReturnsNull() throws Exception {
        assertEquals(null, read("{\"time\":null}"));
    }

    @Test
    public void invalidStringThrows() {
        // 不再静默变成 epoch 0，而是报错让调用方感知
        assertThrows(Exception.class, () -> read("{\"time\":\"not-a-date\"}"));
    }
}
