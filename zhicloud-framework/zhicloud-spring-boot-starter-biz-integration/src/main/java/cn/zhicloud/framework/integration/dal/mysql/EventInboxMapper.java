package cn.zhicloud.framework.integration.dal.mysql;

import cn.zhicloud.framework.integration.dal.dataobject.EventInboxDO;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

import static cn.zhicloud.framework.integration.enums.EventStatusConstants.INBOX_DEAD;
import static cn.zhicloud.framework.integration.enums.EventStatusConstants.INBOX_FAILED;
import static cn.zhicloud.framework.integration.enums.EventStatusConstants.INBOX_PROCESSED;
import static cn.zhicloud.framework.integration.enums.EventStatusConstants.INBOX_PROCESSING;
import static cn.zhicloud.framework.integration.enums.EventStatusConstants.INBOX_RECEIVED;

/**
 * 事件收件箱 Mapper（V100）
 *
 * <p>去重由 {@code uk_consumer_tenant_event} 唯一键保证：插入冲突后回查，已 PROCESSED 的跳过，
 * 处于 RECEIVED / FAILED / 已过期 PROCESSING 的走 CAS 抢占。领取和结算都带 processing_owner
 * fencing，避免旧 worker 在租约被接管后覆盖新 worker 的状态。
 *
 * @author 智云集成治理
 */
@Mapper
public interface EventInboxMapper extends BaseMapperX<EventInboxDO> {

    /**
     * 按 (租户, 消费者, 事件编号) 查询去重记录。
     *
     * <p>所有入口都强制显式携带 tenant_id，不能依赖租户拦截器替 SQL 补条件。
     */
    default EventInboxDO selectByConsumerAndEvent(String consumerName, String eventId, Long tenantId) {
        return selectOne(new LambdaQueryWrapper<EventInboxDO>()
                .eq(EventInboxDO::getTenantId, tenantId)
                .eq(EventInboxDO::getConsumerName, consumerName)
                .eq(EventInboxDO::getEventId, eventId));
    }

    /**
     * 查询最老待消费事件的接收时间；没有待消费事件时返回 null。
     *
     * <p>统计 RECEIVED/PROCESSING/FAILED，不把已完成的 PROCESSED 或停止自动消费的 DEAD 计入。
     * PROCESSING 可能是崩溃/失联节点留下的记录，必须计入滞留监控，否则卡死事件不可见。
     */
    @Select("SELECT MIN(received_at) FROM event_inbox WHERE deleted = 0 "
            + "AND status IN ('" + INBOX_RECEIVED + "','" + INBOX_PROCESSING + "','" + INBOX_FAILED + "')")
    LocalDateTime selectOldestPendingReceivedAt();

    /**
     * CAS 抢占未完成或租约已过期的记录，并写入本次 owner/租约。
     *
     * <p>RECEIVED/FAILED 可直接领取；PROCESSING 只有在 lease_until 为空或已到期时才允许接管。
     */
    @Update("UPDATE event_inbox SET status = '" + INBOX_PROCESSING + "', processing_owner = #{owner}, "
            + "claimed_at = #{now}, lease_until = #{leaseUntil} "
            + "WHERE id = #{id} AND tenant_id = #{tenantId} AND deleted = 0 "
            + "AND (status IN ('" + INBOX_RECEIVED + "','" + INBOX_FAILED + "') "
            + "OR (status = '" + INBOX_PROCESSING + "' "
            + "AND (lease_until IS NULL OR lease_until <= #{now})))")
    int claim(@Param("id") Long id, @Param("tenantId") Long tenantId, @Param("owner") String owner,
              @Param("now") LocalDateTime now, @Param("leaseUntil") LocalDateTime leaseUntil);

    /**
     * 标记处理成功；只有当前 owner 仍持有 PROCESSING 记录时才能结算。
     */
    @Update("UPDATE event_inbox SET status = '" + INBOX_PROCESSED + "', processed_at = #{now}, error = NULL, "
            + "processing_owner = NULL, lease_until = NULL, claimed_at = NULL "
            + "WHERE id = #{id} AND tenant_id = #{tenantId} AND deleted = 0 "
            + "AND status = '" + INBOX_PROCESSING + "' AND processing_owner = #{owner}")
    int markProcessed(@Param("id") Long id, @Param("tenantId") Long tenantId, @Param("owner") String owner,
                      @Param("now") LocalDateTime now);

    /**
     * 标记处理失败并递增重试次数；只有当前 owner 仍持有 PROCESSING 记录时才能结算。
     */
    @Update("UPDATE event_inbox SET status = '" + INBOX_FAILED + "', retry_count = retry_count + 1, "
            + "error = #{error}, processing_owner = NULL, lease_until = NULL, claimed_at = NULL "
            + "WHERE id = #{id} AND tenant_id = #{tenantId} AND deleted = 0 "
            + "AND status = '" + INBOX_PROCESSING + "' AND processing_owner = #{owner}")
    int markFailed(@Param("id") Long id, @Param("tenantId") Long tenantId, @Param("owner") String owner,
                   @Param("error") String error);

    /**
     * 标记死信并递增重试次数；只有当前 owner 仍持有 PROCESSING 记录时才能结算。
     */
    @Update("UPDATE event_inbox SET status = '" + INBOX_DEAD + "', retry_count = retry_count + 1, "
            + "error = #{error}, processing_owner = NULL, lease_until = NULL, claimed_at = NULL "
            + "WHERE id = #{id} AND tenant_id = #{tenantId} AND deleted = 0 "
            + "AND status = '" + INBOX_PROCESSING + "' AND processing_owner = #{owner}")
    int markDead(@Param("id") Long id, @Param("tenantId") Long tenantId, @Param("owner") String owner,
                 @Param("error") String error);

    /**
     * 租户内人工重放：更新条件同时校验 id、tenant_id 与 DEAD 状态，并清理残留 owner/租约。
     */
    @Update("UPDATE event_inbox SET status = '" + INBOX_FAILED + "', retry_count = 0, error = NULL, "
            + "processing_owner = NULL, lease_until = NULL, claimed_at = NULL "
            + "WHERE id = #{id} AND tenant_id = #{tenantId} AND deleted = 0 AND status = '" + INBOX_DEAD + "'")
    int replayByTenantId(@Param("id") Long id, @Param("tenantId") Long tenantId);

    /**
     * 查询当前租户下某事件的所有 DEAD 消费者记录。
     */
    default List<EventInboxDO> selectDeadByEventIdAndTenantId(String eventId, Long tenantId) {
        return selectList(new LambdaQueryWrapper<EventInboxDO>()
                .eq(EventInboxDO::getTenantId, tenantId)
                .eq(EventInboxDO::getEventId, eventId)
                .eq(EventInboxDO::getStatus, INBOX_DEAD)
                .orderByAsc(EventInboxDO::getId));
    }

}
