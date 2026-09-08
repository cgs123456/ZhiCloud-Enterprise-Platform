package cn.zhicloud.module.aimultiagent.service.trace;

import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.module.aimultiagent.controller.admin.trace.vo.MultiAgentSpanRespVO;
import cn.zhicloud.module.aimultiagent.controller.admin.trace.vo.MultiAgentTraceRespVO;
import cn.zhicloud.module.aimultiagent.controller.admin.trace.vo.MultiAgentTraceSummaryVO;
import cn.zhicloud.module.aimultiagent.dal.dataobject.MultiAgentExecutionLogDO;
import cn.zhicloud.module.aimultiagent.dal.dataobject.MultiAgentExecutionSpanDO;
import cn.zhicloud.module.aimultiagent.dal.mysql.MultiAgentExecutionLogMapper;
import cn.zhicloud.module.aimultiagent.dal.mysql.MultiAgentExecutionSpanMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDateTime;
import java.util.List;

import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.module.aimultiagent.enums.ErrorCodeConstants.EXECUTION_LOG_NOT_EXISTS;

/**
 * 多 Agent 执行链路追踪 Service 实现
 *
 * <p>埋点写入降级容错：失败只告警，绝不抛异常中断编排主流程。
 *
 * @author zhicloud
 */
@Service
@Validated
@Slf4j
public class MultiAgentSpanServiceImpl implements MultiAgentSpanService {

    @Resource
    private MultiAgentExecutionSpanMapper spanMapper;
    @Resource
    private MultiAgentExecutionLogMapper executionLogMapper;

    @Override
    public Long startSpan(Long executionLogId, String traceId, String spanType,
                          String workerName, Integer taskIndex, String taskId) {
        if (executionLogId == null) {
            return null;
        }
        try {
            MultiAgentExecutionSpanDO span = new MultiAgentExecutionSpanDO();
            span.setExecutionLogId(executionLogId);
            span.setTraceId(traceId);
            span.setSpanType(spanType);
            span.setWorkerName(workerName);
            span.setTaskIndex(taskIndex);
            span.setTaskId(taskId);
            span.setStatus(STATUS_RUNNING);
            span.setStartTime(LocalDateTime.now());
            spanMapper.insert(span);
            return span.getId();
        } catch (Exception e) {
            log.warn("[startSpan][Span 开启写入失败，executionLogId={}, spanType={}]",
                    executionLogId, spanType, e);
            return null;
        }
    }

    @Override
    public void finishSpan(Long spanId, boolean success, Integer tokens, Long durationMs,
                           String errorMsg, String outputExcerpt) {
        if (spanId == null) {
            return;
        }
        try {
            MultiAgentExecutionSpanDO span = new MultiAgentExecutionSpanDO();
            span.setId(spanId);
            span.setStatus(success ? STATUS_SUCCESS : STATUS_FAILED);
            span.setTokens(tokens);
            span.setDurationMs(durationMs);
            span.setEndTime(LocalDateTime.now());
            span.setErrorMsg(errorMsg);
            span.setOutputExcerpt(outputExcerpt);
            spanMapper.updateById(span);
        } catch (Exception e) {
            log.warn("[finishSpan][Span 结束写入失败，spanId={}]", spanId, e);
        }
    }

    @Override
    public List<MultiAgentExecutionSpanDO> getSpans(Long executionLogId) {
        return spanMapper.selectListByExecutionLogId(executionLogId);
    }

    @Override
    public MultiAgentTraceRespVO getTrace(Long executionLogId) {
        MultiAgentExecutionLogDO logDO = executionLogMapper.selectById(executionLogId);
        if (logDO == null) {
            throw exception(EXECUTION_LOG_NOT_EXISTS);
        }
        List<MultiAgentExecutionSpanDO> spans = spanMapper.selectListByExecutionLogId(executionLogId);
        MultiAgentTraceRespVO respVO = new MultiAgentTraceRespVO();
        respVO.setExecutionLogId(executionLogId);
        respVO.setTraceId(logDO.getTraceId());
        respVO.setSpans(BeanUtils.toBean(spans, MultiAgentSpanRespVO.class));
        TraceSummary summary = MultiAgentTraceAggregator.summarize(spans);
        respVO.setSummary(BeanUtils.toBean(summary, MultiAgentTraceSummaryVO.class));
        return respVO;
    }

}
