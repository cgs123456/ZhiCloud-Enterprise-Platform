package cn.zhicloud.module.aimultiagent.dal.mysql;

import cn.zhicloud.framework.mybatis.core.mapper.BaseMapperX;
import cn.zhicloud.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.zhicloud.module.aimultiagent.dal.dataobject.MultiAgentExecutionSpanDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 多 Agent 执行链路 Span Mapper
 *
 * @author zhicloud
 */
@Mapper
public interface MultiAgentExecutionSpanMapper extends BaseMapperX<MultiAgentExecutionSpanDO> {

    /**
     * 按执行日志查询全部 Span（按开始时间升序，即执行顺序）
     *
     * @param executionLogId 执行日志编号
     * @return Span 列表
     */
    default List<MultiAgentExecutionSpanDO> selectListByExecutionLogId(Long executionLogId) {
        return selectList(new LambdaQueryWrapperX<MultiAgentExecutionSpanDO>()
                .eq(MultiAgentExecutionSpanDO::getExecutionLogId, executionLogId)
                .orderByAsc(MultiAgentExecutionSpanDO::getStartTime)
                .orderByAsc(MultiAgentExecutionSpanDO::getId));
    }

    /**
     * 按追踪 ID 查询全部 Span（跨执行日志排查用）
     *
     * @param traceId 全链路追踪 ID
     * @return Span 列表
     */
    default List<MultiAgentExecutionSpanDO> selectListByTraceId(String traceId) {
        return selectList(new LambdaQueryWrapperX<MultiAgentExecutionSpanDO>()
                .eq(MultiAgentExecutionSpanDO::getTraceId, traceId)
                .orderByAsc(MultiAgentExecutionSpanDO::getStartTime)
                .orderByAsc(MultiAgentExecutionSpanDO::getId));
    }

}
