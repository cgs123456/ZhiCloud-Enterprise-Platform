package cn.zhicloud.framework.integration.dal.mysql;

import cn.zhicloud.framework.integration.dal.dataobject.EventInboxDO;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

import static cn.zhicloud.framework.integration.enums.EventStatusConstants.INBOX_DEAD;
import static cn.zhicloud.framework.integration.enums.EventStatusConstants.INBOX_FAILED;
import static cn.zhicloud.framework.integration.enums.EventStatusConstants.INBOX_PROCESSED;
import static cn.zhicloud.framework.integration.enums.EventStatusConstants.INBOX_RECEIVED;

/**
 * 事件收件箱 Mapper（V100）
 *
 * <p>去重由 {@code uk_consumer_event} 唯一键保证：插入冲突后回查，已 PROCESSED 的跳过，
 * 处于 RECEIVED / FAILED 的走 CAS 抢占（防止同一事件被两个线程同时执行业务）。
 *
 * @author 智云集成治理
 */
@Mapper
public interface EventInboxMapper extends BaseMapperX<EventInboxDO> {

    /**
     * 按 (消费者, 事件编号) 查询去重记录
     */
    default EventInboxDO selectByConsumerAndEvent(String consumerName, String eventId) {
        return selectOne(new LambdaQueryWrapper<EventInboxDO>()
                .eq(EventInboxDO::getConsumerName, consumerName)
                .eq(EventInboxDO::getEventId, eventId));
    }

    /**
     * 查询最老待消费事件的接收时间；没有待消费事件时返回 null。
     *
     * <p>只统计 RECEIVED/FAILED，不把已完成的 PROCESSED 或停止自动消费的 DEAD 计入。
     */
    @Select("SELECT MIN(received_at) FROM event_inbox WHERE deleted = 0 "
            + "AND status IN ('" + INBOX_RECEIVED + "','" + INBOX_FAILED + "')")
    LocalDateTime selectOldestPendingReceivedAt();

    /**
     * CAS 抢占未完成的记录：仅当状态仍为 RECEIVED / FAILED 时可执行
     */
    @Update("UPDATE event_inbox SET status = '" + INBOX_RECEIVED + "' WHERE id = #{id} AND deleted = 0 "
            + "AND status IN ('" + INBOX_RECEIVED + "','" + INBOX_FAILED + "')")
    int claim(@Param("id") Long id);

    /**
     * 标记处理成功
     */
    @Update("UPDATE event_inbox SET status = '" + INBOX_PROCESSED + "', processed_at = #{now}, error = NULL "
            + "WHERE id = #{id}")
    int markProcessed(@Param("id") Long id, @Param("now") LocalDateTime now);

    /**
     * 标记处理失败并递增重试次数
     */
    @Update("UPDATE event_inbox SET status = '" + INBOX_FAILED + "', retry_count = retry_count + 1, error = #{error} "
            + "WHERE id = #{id}")
    int markFailed(@Param("id") Long id, @Param("error") String error);

    /**
     * 标记死信并递增重试次数
     */
    @Update("UPDATE event_inbox SET status = '" + INBOX_DEAD + "', retry_count = retry_count + 1, error = #{error} "
            + "WHERE id = #{id}")
    int markDead(@Param("id") Long id, @Param("error") String error);

    /**
     * 人工重放：把死信记录重置为可再次消费（清空重试次数与失败原因）
     */
    @Update("UPDATE event_inbox SET status = '" + INBOX_FAILED + "', retry_count = 0, error = NULL "
            + "WHERE id = #{id} AND status = '" + INBOX_DEAD + "'")
    int replay(@Param("id") Long id);

}
