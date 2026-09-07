package cn.zhicloud.module.aimultiagent.dal.mysql;

import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.aimultiagent.dal.dataobject.MultiAgentCheckpointDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 多 Agent 编排检查点 Mapper
 *
 * <p>租约语义：同一 execution 下同一时刻只允许一个执行者持有租约。
 * 获取与释放均为单条原子 UPDATE（WHERE 条件做乐观校验），返回值即是否成功。
 *
 * @author zhicloud
 */
@Mapper
public interface MultiAgentCheckpointMapper extends BaseMapperX<MultiAgentCheckpointDO> {

    /**
     * 按执行日志查询全部检查点（按 id 升序，即写入顺序）
     *
     * @param executionLogId 执行日志编号
     * @return 检查点列表
     */
    default List<MultiAgentCheckpointDO> selectListByExecutionLogId(Long executionLogId) {
        return selectList(new LambdaQueryWrapperX<MultiAgentCheckpointDO>()
                .eq(MultiAgentCheckpointDO::getExecutionLogId, executionLogId)
                .orderByAsc(MultiAgentCheckpointDO::getId));
    }

    /**
     * 查询某次执行的最新检查点
     *
     * @param executionLogId 执行日志编号
     * @return 最新检查点，无则返回 null
     */
    default MultiAgentCheckpointDO selectLatestByExecutionLogId(Long executionLogId) {
        return selectOne(new LambdaQueryWrapperX<MultiAgentCheckpointDO>()
                .eq(MultiAgentCheckpointDO::getExecutionLogId, executionLogId)
                .orderByDesc(MultiAgentCheckpointDO::getId)
                .last("LIMIT 1"));
    }

    /**
     * 查询所有超时的未释放租约（供超时扫描释放）
     *
     * @return 超时检查点列表
     */
    default List<MultiAgentCheckpointDO> selectExpiredLeases() {
        return selectList(new LambdaQueryWrapperX<MultiAgentCheckpointDO>()
                .isNotNull(MultiAgentCheckpointDO::getLeaseToken)
                .lt(MultiAgentCheckpointDO::getLeaseExpireTime, java.time.LocalDateTime.now()));
    }

    /**
     * 获取租约（原子操作）
     *
     * <p>仅当目标行当前无租约或租约已过期时成功，返回更新行数（1=获取成功，0=被他人持有）。
     *
     * @param id         检查点编号
     * @param leaseToken 本次持有的租约令牌
     * @param timeoutMs  租约有效期（毫秒）
     * @return 更新行数
     */
    @Update("UPDATE aimultiagent_checkpoint SET lease_token = #{leaseToken}, "
            + "lease_expire_time = DATE_ADD(NOW(3), INTERVAL #{timeoutMs} MICROSECOND) "
            + "WHERE id = #{id} AND (lease_token IS NULL OR lease_expire_time < NOW(3))")
    int acquireLease(@Param("id") Long checkpointId,
                     @Param("leaseToken") String leaseToken,
                     @Param("timeoutMs") long timeoutMs);

    /**
     * 释放租约（原子操作，仅持有者可释放）
     *
     * @param id         检查点编号
     * @param leaseToken 持有的租约令牌
     * @return 更新行数
     */
    @Update("UPDATE aimultiagent_checkpoint SET lease_token = NULL, lease_expire_time = NULL "
            + "WHERE id = #{id} AND lease_token = #{leaseToken}")
    int releaseLease(@Param("id") Long checkpointId,
                     @Param("leaseToken") String leaseToken);

}
