package cn.zhicloud.framework.mq.redis.core.stream;

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

/**
 * Redis Stream 命令包装：把"空阻塞读"还原为 {@link RedisStreamCommands} 声明的 List 语义。
 *
 * <p>缺陷起源：Redisson 核心的 {@code RedisExecutor#emptyBlockingResult} 按**命令名**返回空结果，
 * XREAD / XREADGROUP 名下一律回 {@code Collections.emptyMap()}（为原生 RStream 的 Map 返回语义设计）；
 * 但 redisson-spring-data 的 {@code RedissonStreamCommands#xReadGroup} / {@code #xRead} 声明的是
 * {@code List<ByteRecord>} 并对结果做了强类型强转 —— 流为空时每次阻塞读都抛
 * {@code java.util.Collections$EmptyMap cannot be cast to java.util.List}。
 *
 * <p>考证：javap 字节码核对 redisson-spring-data-35 的 4.6.1 / 4.7.0 / 4.8.0（截至 2026-10 的最新版）
 * 该强转完全一致，上游未修（redisson#7225 只修了原生 API 方向 EmptyList→Map，spring-data 适配器方向
 * EmptyMap→List 未覆盖，EmptyMapConvertor convert(null)=emptyMap 且 checkcast List 在四个分支原样存在）。
 * 装配点见 {@link StreamSafeRedissonConnectionFactory}。
 *
 * <p>豁免边界：仅对这一条已知的确切强转消息返回空列表；其余 ClassCastException 原样重抛
 * （fail-closed，不吞真实的数据解码错误）。上游修复后应当移除本豁免（标记：EmptyMap-cast-exemption）。
 */
public class StreamSafeRedisStreamCommands implements RedisStreamCommands {

    /**
     * 仅豁免这一条：JDK 21 下 CCE 消息的固定形态（emptyBlockingResult(XREADGROUP 等命令名) → EmptyMap，
     * 适配器 checkcast List）。字符串在 emptyBlockingResult 改动前视为稳定契约。
     */
    static final String EMPTY_MAP_TO_LIST_CCE =
            "java.util.Collections$EmptyMap cannot be cast to class java.util.List";

    private final RedisStreamCommands delegate;

    public StreamSafeRedisStreamCommands(RedisStreamCommands delegate) {
        this.delegate = delegate;
    }

    @Override
    public List<ByteRecord> xReadGroup(Consumer consumer, StreamReadOptions readOptions, StreamOffset<byte[]>... streams) {
        try {
            return delegate.xReadGroup(consumer, readOptions, streams);
        } catch (ClassCastException e) {
            if (String.valueOf(e.getMessage()).contains(EMPTY_MAP_TO_LIST_CCE)) {
                return Collections.emptyList();
            }
            throw e;
        }
    }

    @Override
    public List<ByteRecord> xRead(StreamReadOptions readOptions, StreamOffset<byte[]>... streams) {
        try {
            return delegate.xRead(readOptions, streams);
        } catch (ClassCastException e) {
            if (String.valueOf(e.getMessage()).contains(EMPTY_MAP_TO_LIST_CCE)) {
                return Collections.emptyList();
            }
            throw e;
        }
    }

    @Override
    public Long xAck(byte[] key, String group, RecordId... recordIds) {
        return delegate.xAck(key, group, recordIds);
    }

    @Override
    public RecordId xAdd(MapRecord<byte[], byte[], byte[]> record, XAddOptions options) {
        return delegate.xAdd(record, options);
    }

    @Override
    public List<RecordId> xClaimJustId(byte[] key, String group, String newOwner, XClaimOptions options) {
        return delegate.xClaimJustId(key, group, newOwner, options);
    }

    @Override
    public List<ByteRecord> xClaim(byte[] key, String group, String newOwner, XClaimOptions options) {
        return delegate.xClaim(key, group, newOwner, options);
    }

    @Override
    public Long xDel(byte[] key, RecordId... recordIds) {
        return delegate.xDel(key, recordIds);
    }

    @Override
    public String xGroupCreate(byte[] key, String groupName, ReadOffset readOffset) {
        return delegate.xGroupCreate(key, groupName, readOffset);
    }

    @Override
    public String xGroupCreate(byte[] key, String groupName, ReadOffset readOffset, boolean mkStream) {
        return delegate.xGroupCreate(key, groupName, readOffset, mkStream);
    }

    @Override
    public Boolean xGroupDelConsumer(byte[] key, Consumer consumer) {
        return delegate.xGroupDelConsumer(key, consumer);
    }

    @Override
    public Boolean xGroupDestroy(byte[] key, String groupName) {
        return delegate.xGroupDestroy(key, groupName);
    }

    @Override
    public XInfoStream xInfo(byte[] key) {
        return delegate.xInfo(key);
    }

    @Override
    public XInfoGroups xInfoGroups(byte[] key) {
        return delegate.xInfoGroups(key);
    }

    @Override
    public XInfoConsumers xInfoConsumers(byte[] key, String groupName) {
        return delegate.xInfoConsumers(key, groupName);
    }

    @Override
    public Long xLen(byte[] key) {
        return delegate.xLen(key);
    }

    @Override
    public PendingMessagesSummary xPending(byte[] key, String groupName) {
        return delegate.xPending(key, groupName);
    }

    @Override
    public PendingMessages xPending(byte[] key, String groupName, XPendingOptions options) {
        return delegate.xPending(key, groupName, options);
    }

    @Override
    public List<ByteRecord> xRange(byte[] key, Range<String> range, Limit limit) {
        return delegate.xRange(key, range, limit);
    }

    @Override
    public List<ByteRecord> xRevRange(byte[] key, Range<String> range, Limit limit) {
        return delegate.xRevRange(key, range, limit);
    }

    @Override
    public Long xTrim(byte[] key, long count) {
        return delegate.xTrim(key, count);
    }

    @Override
    public Long xTrim(byte[] key, long count, boolean approximateTrimming) {
        return delegate.xTrim(key, count, approximateTrimming);
    }

}
