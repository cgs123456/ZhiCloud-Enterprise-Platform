package cn.zhicloud.framework.integration.dal.mysql;

import cn.zhicloud.framework.integration.dal.dataobject.EventOutboxDO;
import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

import static cn.zhicloud.framework.integration.enums.EventStatusConstants.OUTBOX_DEAD;
import static cn.zhicloud.framework.integration.enums.EventStatusConstants.OUTBOX_NEW;
import static cn.zhicloud.framework.integration.enums.EventStatusConstants.OUTBOX_PUBLISHED;
import static cn.zhicloud.framework.integration.enums.EventStatusConstants.OUTBOX_PUBLISHING;

/**
 * 事件发件箱 Mapper（V100）
 *
 * <p>领取（claim）用 DB 层 CAS 而不是「先查后改」：{@code UPDATE ... WHERE id = ? AND status IN (...) AND
 * (next_retry_at IS NULL OR next_retry_at <= ?)} 返回 1 行才算领取成功。多节点并发轮询时，数据库唯一
 * 的行锁保证同一事件只会被一个节点领走，返回 0 的节点直接跳过。
 *
 * <p>领取的同时把 {@code next_retry_at} 推到租约到期时间：租约内不可再领取（并发互斥），
 * 租约过期后 PUBLISHING 事件重新可见（领取方崩溃后的自动恢复）。
 *
 * <p>所有状态迁移都带前置状态条件，避免把 DEAD 事件复活，也避免两个节点对同一行给出相反结论。
 * <p>事件表既受租户拦截器保护，按 id 的状态迁移 SQL 也显式携带 tenant_id，避免拦截器
 * 因 TableInfo 尚未初始化或测试/运行环境差异而失效。全局中继扫描/指标必须显式忽略租户。
 *
 * @author 智云集成治理
 */
@Mapper
public interface EventOutboxMapper extends BaseMapperX<EventOutboxDO> {

    /**
     * 按事件编号 + 租户查询（管理端 DLQ 查询、重放必须使用该入口）
     */
    default EventOutboxDO selectByEventIdAndTenantId(String eventId, Long tenantId) {
        return selectOne(new LambdaQueryWrapper<EventOutboxDO>()
                .eq(EventOutboxDO::getEventId, eventId)
                .eq(EventOutboxDO::getTenantId, tenantId));
    }

    /**
     * 按主键 + 租户查询。失败结算必须先读到正确租户的事件，不能只按 id 读取。
     */
    default EventOutboxDO selectByIdAndTenantId(Long id, Long tenantId) {
        return selectOne(new LambdaQueryWrapper<EventOutboxDO>()
                .eq(EventOutboxDO::getId, id)
                .eq(EventOutboxDO::getTenantId, tenantId));
    }

    /**
     * 查询最老未投递事件的业务发生时间；没有待投递事件时返回 null。
     *
     * <p>只统计 NEW/PUBLISHING，不把 PUBLISHED 历史事件计入滞留时长。
     */
    @Select("SELECT MIN(occurred_at) FROM event_outbox WHERE deleted = 0 "
            + "AND status IN ('" + OUTBOX_NEW + "','" + OUTBOX_PUBLISHING + "')")
    LocalDateTime selectOldestPendingOccurredAt();

    /**
     * 扫描可被领取的事件主键：NEW 用 next_retry_at 控制退避，PUBLISHING 用 lease_until 控制租约。
     * PUBLISHING 租约过期后允许接管，覆盖领取后进程崩溃/失联的残留。
     */
    @Select("SELECT id FROM event_outbox WHERE deleted = 0 AND ("
            + "(status = '" + OUTBOX_NEW + "' AND (next_retry_at IS NULL OR next_retry_at <= #{now})) "
            + "OR (status = '" + OUTBOX_PUBLISHING + "' AND (lease_until IS NULL OR lease_until <= #{now}))) "
            + "ORDER BY id LIMIT #{limit}")
    List<Long> selectClaimableIds(@Param("now") LocalDateTime now, @Param("limit") int limit);

    /**
     * DB 层 CAS 领取：返回 1 表示领取成功，0 表示已被其他节点领走或仍在退避/租约窗口内。
     * 领取成功时写入本次 owner，并把 {@code lease_until} 推到租约到期时刻。
     */
    @Update("UPDATE event_outbox SET status = '" + OUTBOX_PUBLISHING + "', processing_owner = #{owner}, "
            + "lease_until = #{leaseUntil} "
            + "WHERE id = #{id} AND tenant_id = #{tenantId} AND deleted = 0 AND ("
            + "(status = '" + OUTBOX_NEW + "' AND (next_retry_at IS NULL OR next_retry_at <= #{now})) "
            + "OR (status = '" + OUTBOX_PUBLISHING + "' AND (lease_until IS NULL OR lease_until <= #{now})))")
    int claim(@Param("id") Long id, @Param("tenantId") Long tenantId, @Param("owner") String owner,
              @Param("now") LocalDateTime now, @Param("leaseUntil") LocalDateTime leaseUntil);

    /**
     * 标记投递完成；只有当前 owner 仍持有 PUBLISHING 事件时才能结算。
     */
    @Update("UPDATE event_outbox SET status = '" + OUTBOX_PUBLISHED + "', published_at = #{now}, last_error = NULL, "
            + "processing_owner = NULL, lease_until = NULL, next_retry_at = NULL "
            + "WHERE id = #{id} AND tenant_id = #{tenantId} AND deleted = 0 "
            + "AND status = '" + OUTBOX_PUBLISHING + "' AND processing_owner = #{owner}")
    int markPublished(@Param("id") Long id, @Param("tenantId") Long tenantId,
                      @Param("owner") String owner, @Param("now") LocalDateTime now);

    /**
     * 标记重试：重试次数 +1、计算下次可领取时间、保留失败原因；只有当前 owner 能结算。
     */
    @Update("UPDATE event_outbox SET status = '" + OUTBOX_NEW + "', retry_count = retry_count + 1, "
            + "next_retry_at = #{nextRetryAt}, last_error = #{error}, "
            + "processing_owner = NULL, lease_until = NULL "
            + "WHERE id = #{id} AND tenant_id = #{tenantId} AND deleted = 0 "
            + "AND status = '" + OUTBOX_PUBLISHING + "' AND processing_owner = #{owner}")
    int markRetry(@Param("id") Long id, @Param("tenantId") Long tenantId, @Param("owner") String owner,
                  @Param("nextRetryAt") LocalDateTime nextRetryAt, @Param("error") String error);

    /**
     * 标记死信：停止自动重试，等待人工重放；只有当前 owner 能结算。
     */
    @Update("UPDATE event_outbox SET status = '" + OUTBOX_DEAD + "', retry_count = retry_count + 1, "
            + "last_error = #{error}, next_retry_at = NULL, "
            + "processing_owner = NULL, lease_until = NULL "
            + "WHERE id = #{id} AND tenant_id = #{tenantId} AND deleted = 0 "
            + "AND status = '" + OUTBOX_PUBLISHING + "' AND processing_owner = #{owner}")
    int markDead(@Param("id") Long id, @Param("tenantId") Long tenantId, @Param("owner") String owner,
                 @Param("error") String error);


    /**
     * 协调重放：将死信，或“已投递但消费侧仍有死信”的事件重新放回待投递队列。
     *
     * <p>必须带租户与前置状态条件，避免管理端重复点击、跨租户操作或与中继线程并发时覆盖状态。
     * 清空 published_at，避免状态已回到 NEW 但投递时间仍显示为已完成的矛盾。
     */
    @Update("UPDATE event_outbox SET status = '" + OUTBOX_NEW + "', retry_count = 0, next_retry_at = NULL, "
            + "last_error = NULL, published_at = NULL, processing_owner = NULL, lease_until = NULL "
            + "WHERE event_id = #{eventId} AND tenant_id = #{tenantId} AND deleted = 0 "
            + "AND status IN ('" + OUTBOX_DEAD + "','" + OUTBOX_PUBLISHED + "')")
    int requeueForReplay(@Param("eventId") String eventId, @Param("tenantId") Long tenantId);

}
