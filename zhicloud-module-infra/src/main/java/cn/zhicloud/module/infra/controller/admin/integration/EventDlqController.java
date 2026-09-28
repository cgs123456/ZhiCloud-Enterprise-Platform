package cn.zhicloud.module.infra.controller.admin.integration;

import cn.hutool.core.util.StrUtil;
import cn.zhicloud.framework.apilog.core.annotation.ApiAccessLog;
import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.common.util.object.BeanUtils;
import cn.zhicloud.framework.integration.service.EventDlqReplayCommand;
import cn.zhicloud.framework.integration.service.EventDlqService;
import cn.zhicloud.module.infra.controller.admin.integration.vo.EventDlqInboxPageReqVO;
import cn.zhicloud.module.infra.controller.admin.integration.vo.EventDlqInboxRespVO;
import cn.zhicloud.module.infra.controller.admin.integration.vo.EventDlqOutboxPageReqVO;
import cn.zhicloud.module.infra.controller.admin.integration.vo.EventDlqOutboxRespVO;
import cn.zhicloud.module.infra.controller.admin.integration.vo.EventDlqReplayLogPageReqVO;
import cn.zhicloud.module.infra.controller.admin.integration.vo.EventDlqReplayLogRespVO;
import cn.zhicloud.module.infra.controller.admin.integration.vo.EventDlqReplayReqVO;
import cn.zhicloud.module.infra.controller.admin.integration.vo.EventDlqReplayRespVO;
import cn.zhicloud.module.infra.enums.ErrorCodeConstants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static cn.zhicloud.framework.apilog.core.enums.OperateTypeEnum.OTHER;
import static cn.zhicloud.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.zhicloud.framework.common.pojo.CommonResult.success;
import static cn.zhicloud.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static cn.zhicloud.framework.security.core.util.SecurityFrameworkUtils.getLoginUserNickname;

/**
 * 管理后台 - 可靠集成死信运营
 *
 * <p>重放接口只接受事件、原因和时间窗。操作者必须从服务端登录上下文注入，客户端无法伪造，
 * 否则审计记录无法证明“谁在什么依据下执行了重放”。
 */
@Tag(name = "管理后台 - 集成死信")
@RestController
@RequestMapping("/infra/event-dlq")
@Validated
@ConditionalOnProperty(prefix = "zhicloud.integration", name = "enabled",
        havingValue = "true", matchIfMissing = true)
public class EventDlqController {

    @Resource
    private EventDlqService eventDlqService;

    @GetMapping("/outbox/page")
    @Operation(summary = "获得 Outbox 死信分页")
    @PreAuthorize("@ss.hasPermission('infra:event-dlq:query')")
    public CommonResult<PageResult<EventDlqOutboxRespVO>> getDeadOutboxPage(
            @Valid EventDlqOutboxPageReqVO pageReqVO) {
        return success(BeanUtils.toBean(eventDlqService.getDeadOutboxPage(
                pageReqVO, pageReqVO.getEventId(), pageReqVO.getAggregateType(),
                pageReqVO.getEventType(), pageReqVO.getFromTime(), pageReqVO.getToTime()),
                EventDlqOutboxRespVO.class));
    }

    @GetMapping("/inbox/page")
    @Operation(summary = "获得 Inbox 死信分页")
    @PreAuthorize("@ss.hasPermission('infra:event-dlq:query')")
    public CommonResult<PageResult<EventDlqInboxRespVO>> getDeadInboxPage(
            @Valid EventDlqInboxPageReqVO pageReqVO) {
        return success(BeanUtils.toBean(eventDlqService.getDeadInboxPage(
                pageReqVO, pageReqVO.getEventId(), pageReqVO.getConsumerName(),
                pageReqVO.getEventType(), pageReqVO.getFromTime(), pageReqVO.getToTime()),
                EventDlqInboxRespVO.class));
    }

    @GetMapping("/replay-log/page")
    @Operation(summary = "获得死信重放审计分页")
    @PreAuthorize("@ss.hasPermission('infra:event-dlq:query')")
    public CommonResult<PageResult<EventDlqReplayLogRespVO>> getReplayLogPage(
            @Valid EventDlqReplayLogPageReqVO pageReqVO) {
        return success(BeanUtils.toBean(eventDlqService.getReplayLogPage(
                pageReqVO, pageReqVO.getEventId(), pageReqVO.getOperatorId(),
                pageReqVO.getResult(), pageReqVO.getFromTime(), pageReqVO.getToTime()),
                EventDlqReplayLogRespVO.class));
    }
    @PostMapping("/replay")
    @Operation(summary = "单条协调重放死信")
    @PreAuthorize("@ss.hasPermission('infra:event-dlq:replay')")
    @ApiAccessLog(operateType = OTHER)
    public CommonResult<EventDlqReplayRespVO> replay(@Valid @RequestBody EventDlqReplayReqVO reqVO) {
        return success(BeanUtils.toBean(eventDlqService.replay(buildCommand(reqVO)),
                EventDlqReplayRespVO.class));
    }

    @PostMapping("/replay-batch")
    @Operation(summary = "批量协调重放死信")
    @PreAuthorize("@ss.hasPermission('infra:event-dlq:replay')")
    @ApiAccessLog(operateType = OTHER)
    public CommonResult<List<EventDlqReplayRespVO>> replayBatch(
            @Valid @RequestBody EventDlqReplayReqVO reqVO) {
        return success(BeanUtils.toBean(eventDlqService.replayBatch(buildCommand(reqVO)),
                EventDlqReplayRespVO.class));
    }

    private EventDlqReplayCommand buildCommand(EventDlqReplayReqVO reqVO) {
        Long operatorId = getLoginUserId();
        if (operatorId == null) {
            throw exception(ErrorCodeConstants.EVENT_DLQ_OPERATOR_NOT_LOGIN);
        }
        String operatorName = getLoginUserNickname();
        if (StrUtil.isBlank(operatorName)) {
            // 昵称可能因历史账号或上游身份同步缺失，但不能因此阻断故障恢复；用户编号仍可追责。
            operatorName = "user-" + operatorId;
        }
        return EventDlqReplayCommand.builder()
                .eventIds(reqVO.getEventIds())
                .operatorId(operatorId)
                .operatorName(operatorName)
                .reason(reqVO.getReason())
                .fromTime(reqVO.getFromTime())
                .toTime(reqVO.getToTime())
                .build();
    }

}