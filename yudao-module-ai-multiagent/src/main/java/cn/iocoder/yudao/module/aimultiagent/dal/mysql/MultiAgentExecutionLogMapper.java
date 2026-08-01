package cn.iocoder.yudao.module.aimultiagent.dal.mysql;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.aimultiagent.dal.dataobject.MultiAgentExecutionLogDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 多 Agent 执行日志 Mapper
 *
 * @author yudao
 */
@Mapper
public interface MultiAgentExecutionLogMapper extends BaseMapperX<MultiAgentExecutionLogDO> {

    /**
     * 按拓扑 ID 查询执行日志列表（按创建时间倒序）
     *
     * @param topologyId 拓扑 ID
     * @return 执行日志列表
     */
    default List<MultiAgentExecutionLogDO> selectListByTopologyId(Long topologyId) {
        return selectList(new LambdaQueryWrapperX<MultiAgentExecutionLogDO>()
                .eq(MultiAgentExecutionLogDO::getTopologyId, topologyId)
                .orderByDesc(MultiAgentExecutionLogDO::getCreateTime));
    }

}
