package cn.zhicloud.framework.mq.redis.core.stream;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.Limit;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.RedisStreamCommands;
import org.springframework.data.redis.connection.stream.ByteRecord;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.PendingMessages;
import org.springframework.data.redis.connection.stream.PendingMessagesSummary;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamInfo.XInfoConsumers;
import org.springframework.data.redis.connection.stream.StreamInfo.XInfoGroups;
import org.springframework.data.redis.connection.stream.StreamInfo.XInfoStream;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link StreamSafeRedisStreamCommands} 空读豁免的边界守卫：
 * 只有"EmptyMap cannot be cast to List"这一条已知形态被豁免为空列表，
 * 其余 ClassCastException 必须原样重抛（不吞真实数据解码错误），正常结果原样透传。
 */
class StreamSafeRedisStreamCommandsTest {

    /** 按需可配置的桩实现：默认所有方法不可触碰，xRead/xReadGroup 行为由测试给定。 */
    private static class StubCommands implements RedisStreamCommands {
        private List<ByteRecord> readResult;
        private RuntimeException readError;
        private Long xLenResult;

        @Override
        public List<ByteRecord> xReadGroup(Consumer consumer, StreamReadOptions readOptions, StreamOffset<byte[]>... streams) {
            if (readError != null) {
                throw readError;
            }
            return readResult;
        }

        @Override
        public List<ByteRecord> xRead(StreamReadOptions readOptions, StreamOffset<byte[]>... streams) {
            if (readError != null) {
                throw readError;
            }
            return readResult;
        }

        @Override
        public Long xLen(byte[] key) {
            return xLenResult;
        }

        private static UnsupportedOperationException notTouched(String method) {
            return new UnsupportedOperationException("测试不应触碰 " + method);
        }

        @Override public Long xAck(byte[] key, String group, RecordId... recordIds) { throw notTouched("xAck"); }
        @Override public RecordId xAdd(MapRecord<byte[], byte[], byte[]> record, XAddOptions options) { throw notTouched("xAdd"); }
        @Override public List<RecordId> xClaimJustId(byte[] key, String group, String newOwner, XClaimOptions options) { throw notTouched("xClaimJustId"); }
        @Override public List<ByteRecord> xClaim(byte[] key, String group, String newOwner, XClaimOptions options) { throw notTouched("xClaim"); }
        @Override public Long xDel(byte[] key, RecordId... recordIds) { throw notTouched("xDel"); }
        @Override public String xGroupCreate(byte[] key, String groupName, ReadOffset readOffset) { throw notTouched("xGroupCreate"); }
        @Override public String xGroupCreate(byte[] key, String groupName, ReadOffset readOffset, boolean mkStream) { throw notTouched("xGroupCreate"); }
        @Override public Boolean xGroupDelConsumer(byte[] key, Consumer consumer) { throw notTouched("xGroupDelConsumer"); }
        @Override public Boolean xGroupDestroy(byte[] key, String groupName) { throw notTouched("xGroupDestroy"); }
        @Override public XInfoStream xInfo(byte[] key) { throw notTouched("xInfo"); }
        @Override public XInfoGroups xInfoGroups(byte[] key) { throw notTouched("xInfoGroups"); }
        @Override public XInfoConsumers xInfoConsumers(byte[] key, String groupName) { throw notTouched("xInfoConsumers"); }
        @Override public PendingMessagesSummary xPending(byte[] key, String groupName) { throw notTouched("xPending"); }
        @Override public PendingMessages xPending(byte[] key, String groupName, XPendingOptions options) { throw notTouched("xPending"); }
        @Override public List<ByteRecord> xRange(byte[] key, Range<String> range, Limit limit) { throw notTouched("xRange"); }
        @Override public List<ByteRecord> xRevRange(byte[] key, Range<String> range, Limit limit) { throw notTouched("xRevRange"); }
        @Override public Long xTrim(byte[] key, long count) { throw notTouched("xTrim"); }
        @Override public Long xTrim(byte[] key, long count, boolean approximateTrimming) { throw notTouched("xTrim"); }
    }

    private static ClassCastException emptyMapToListCce() {
        // 与 redisson 4.6.x~4.8.x 在 JDK21 下抛出的消息逐字一致
        return new ClassCastException("class java.util.Collections$EmptyMap cannot be cast to class java.util.List"
                + " (java.util.Collections$EmptyMap and java.util.List are in module java.base of loader 'bootstrap')");
    }

    private final Consumer consumer = Consumer.from("g", "c");
    private final StreamReadOptions readOptions = StreamReadOptions.empty().count(10);
    private final StreamOffset<byte[]> streamOffset = StreamOffset.create("t".getBytes(), ReadOffset.lastConsumed());

    @Test
    void emptyBlockingReadIsFlattenedToEmptyList() {
        StubCommands stub = new StubCommands();
        stub.readError = emptyMapToListCce();
        StreamSafeRedisStreamCommands commands = new StreamSafeRedisStreamCommands(stub);
        List<ByteRecord> result = commands.xReadGroup(consumer, readOptions, streamOffset);
        assertTrue(result.isEmpty(), "空读必须回到接口声明的空列表语义");
        assertTrue(commands.xRead(readOptions, streamOffset).isEmpty(), "xRead 同样受豁免保护");
    }

    @Test
    void foreignCastErrorIsRethrownNotSwallowed() {
        StubCommands stub = new StubCommands();
        stub.readError = new ClassCastException("class java.lang.Foo cannot be cast to class java.util.List");
        StreamSafeRedisStreamCommands commands = new StreamSafeRedisStreamCommands(stub);
        assertThrows(ClassCastException.class, () -> commands.xReadGroup(consumer, readOptions, streamOffset));
        assertThrows(ClassCastException.class, () -> commands.xRead(readOptions, streamOffset));
    }

    @Test
    void normalResultIsPassedThroughUnchanged() {
        StubCommands stub = new StubCommands();
        stub.readResult = Collections.emptyList();
        StreamSafeRedisStreamCommands commands = new StreamSafeRedisStreamCommands(stub);
        assertSame(stub.readResult, commands.xReadGroup(consumer, readOptions, streamOffset));
        assertSame(stub.readResult, commands.xRead(readOptions, streamOffset));
    }

    @Test
    void otherMethodsDelegate() {
        StubCommands stub = new StubCommands();
        stub.xLenResult = 42L;
        StreamSafeRedisStreamCommands commands = new StreamSafeRedisStreamCommands(stub);
        assertEquals(42L, commands.xLen("t".getBytes()));
    }

}
