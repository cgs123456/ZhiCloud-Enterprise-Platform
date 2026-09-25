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
 *
 * @author 智云集成治理
 */
@Mapper
public interface EventOutboxMapper extends BaseMapperX<EventOutboxDO> {

    /**
     * 按事件编号查询（幂等登记回查用）
     */
    default EventOutboxDO selectByEventId(String eventId) {
        return selectOne(new LambdaQueryWrapper<EventOutboxDO>()
                .eq(EventOutboxDO::getEventId, eventId));
    }

    /**
     * 扫描可被领取的事件主键：NEW 与 PUBLISHING 都可再领取（PUBLISHING 覆盖「领取后进程崩溃」的残留），
     * 且 next_retry_at 为空或已到期。
     */
    @Select("SELECT id FROM event_outbox WHERE deleted = 0 AND status IN ('" + OUTBOX_NEW + "','" + OUTBOX_PUBLISHING + "') "
            + "AND (next_retry_at IS NULL OR next_retry_at <= #{now}) ORDER BY id LIMIT #{limit}")
    List<Long> selectClaimableIds(@Param("now") LocalDateTime now, @Param("limit") int limit);

    /**
     * DB 层 CAS 领取：返回 1 表示领取成功，0 表示已被其他节点领走或仍在退避窗口内。
     * 领取成功时把 {@code next_retry_at} 推到租约到期时刻。
     */
    @Update("UPDATE event_outbox SET status = '" + OUTBOX_PUBLISHING + "', next_retry_at = #{leaseAt} "
            + "WHERE id = #{id} AND deleted = 0 AND status IN ('" + OUTBOX_NEW + "','" + OUTBOX_PUBLISHING + "') "
            + "AND (next_retry_at IS NULL OR next_retry_at <= #{now})")
    int claim(@Param("id") Long id, @Param("now") LocalDateTime now, @Param("leaseAt") LocalDateTime leaseAt);

    /**
     * 标记投递完成（只允许从 PUBLISHING 迁移）
     */
    @Update("UPDATE event_outbox SET status = '" + OUTBOX_PUBLISHED + "', published_at = #{now}, last_error = NULL "
            + "WHERE id = #{id} AND status = '" + OUTBOX_PUBLISHING + "'")
    int markPublished(@Param("id") Long id, @Param("now") LocalDateTime now);

    /**
     * 标记重试：重试次数 +1、计算下次可领取时间、保留失败原因
     */
    @Update("UPDATE event_outbox SET status = '" + OUTBOX_NEW + "', retry_count = retry_count + 1, "
            + "next_retry_at = #{nextRetryAt}, last_error = #{error} WHERE id = #{id} AND status = '" + OUTBOX_PUBLISHING + "'")
    int markRetry(@Param("id") Long id, @Param("nextRetryAt") LocalDateTime nextRetryAt, @Param("error") String error);

    /**
     * 标记死信：停止自动重试，等待人工重放
     */
    @Update("UPDATE event_outbox SET status = '" + OUTBOX_DEAD + "', retry_count = retry_count + 1, last_error = #{error} "
            + "WHERE id = #{id} AND status = '" + OUTBOX_PUBLISHING + "'")
    int markDead(@Param("id") Long id, @Param("error") String error);

    /**
     * 人工重放：把死信事件重新放回待投递队列（清空重试次数与退避窗口）
     */
    @Update("UPDATE event_outbox SET status = '" + OUTBOX_NEW + "', retry_count = 0, next_retry_at = NULL, last_error = NULL "
            + "WHERE id = #{id} AND status = '" + OUTBOX_DEAD + "'")
    int replay(@Param("id") Long id);

}