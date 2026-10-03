package cn.zhicloud.module.infra.controller.admin.integration;

import cn.zhicloud.framework.common.pojo.CommonResult;
import cn.zhicloud.framework.common.pojo.PageResult;
import cn.zhicloud.framework.integration.dal.dataobject.EventDlqReplayLogDO;
import cn.zhicloud.framework.integration.service.EventDlqReplayCommand;
import cn.zhicloud.framework.integration.service.EventDlqReplayResult;
import cn.zhicloud.framework.integration.service.EventDlqService;
import cn.zhicloud.framework.security.core.LoginUser;
import cn.zhicloud.module.infra.controller.admin.integration.vo.EventDlqReplayLogPageReqVO;
import cn.zhicloud.module.infra.controller.admin.integration.vo.EventDlqReplayLogRespVO;
import cn.zhicloud.module.infra.controller.admin.integration.vo.EventDlqReplayReqVO;
import cn.zhicloud.module.infra.controller.admin.integration.vo.EventDlqReplayRespVO;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class EventDlqControllerTest {

    @Mock
    private EventDlqService eventDlqService;

    @InjectMocks
    private EventDlqController controller;

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @AfterEach
    public void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void testReplayInjectsAuthenticatedOperatorAndReturnsServiceResult() {
        login(1001L, "集成管理员");
        EventDlqReplayResult serviceResult = EventDlqReplayResult.builder()
                .eventId("evt-1").success(true).outboxReplayed(true).inboxReplayed(1).build();
        when(eventDlqService.replay(any(EventDlqReplayCommand.class))).thenReturn(serviceResult);

        CommonResult<EventDlqReplayRespVO> result = controller.replay(replayReq("evt-1"));

        assertTrue(result.getData().isSuccess());
        assertEquals("evt-1", result.getData().getEventId());
        ArgumentCaptor<EventDlqReplayCommand> captor = ArgumentCaptor.forClass(EventDlqReplayCommand.class);
        verify(eventDlqService).replay(captor.capture());
        assertEquals(1001L, captor.getValue().getOperatorId());
        assertEquals("集成管理员", captor.getValue().getOperatorName());
        assertEquals(List.of("evt-1"), captor.getValue().getEventIds());
        assertEquals("consumer fixed in change-20260926", captor.getValue().getReason());
    }

    @Test
    public void testReplayFallsBackToUserIdWhenNicknameBlank() {
        login(1001L, " ");
        EventDlqReplayResult serviceResult = EventDlqReplayResult.builder()
                .eventId("evt-1").success(true).build();
        when(eventDlqService.replay(any(EventDlqReplayCommand.class))).thenReturn(serviceResult);

        controller.replay(replayReq("evt-1"));

        ArgumentCaptor<EventDlqReplayCommand> captor = ArgumentCaptor.forClass(EventDlqReplayCommand.class);
        verify(eventDlqService).replay(captor.capture());
        assertEquals("user-1001", captor.getValue().getOperatorName());
    }

    @Test
    public void testBatchReturnsPerEventResults() {
        login(1001L, "集成管理员");
        when(eventDlqService.replayBatch(any(EventDlqReplayCommand.class))).thenReturn(List.of(
                EventDlqReplayResult.builder().eventId("evt-1").success(true).build(),
                EventDlqReplayResult.failure("evt-2", "not replayable")));

        CommonResult<List<EventDlqReplayRespVO>> result = controller.replayBatch(replayReq("evt-1", "evt-2"));

        assertEquals(2, result.getData().size());
        assertTrue(result.getData().get(0).isSuccess());
        assertFalse(result.getData().get(1).isSuccess());
        assertEquals("not replayable", result.getData().get(1).getMessage());
    }

    @Test
    public void testReplayRequestRejectsMoreThanServiceLimit() {
        EventDlqReplayReqVO reqVO = replayReq();
        List<String> eventIds = new ArrayList<>();
        for (int i = 0; i <= EventDlqService.MAX_BATCH_SIZE; i++) {
            eventIds.add("evt-" + i);
        }
        reqVO.setEventIds(eventIds);

        Set<ConstraintViolation<EventDlqReplayReqVO>> violations = validator.validate(reqVO);

        assertFalse(violations.isEmpty());
    }

    @Test
    public void testReplayLogPagePassesFiltersAndMapsAuditFields() {
        EventDlqReplayLogPageReqVO reqVO = new EventDlqReplayLogPageReqVO();
        reqVO.setEventId("evt-log-1");
        reqVO.setOperatorId(1001L);
        reqVO.setResult(EventDlqReplayResult.RESULT_SUCCESS);
        reqVO.setFromTime(LocalDateTime.of(2026, 9, 1, 0, 0));
        reqVO.setToTime(LocalDateTime.of(2026, 9, 30, 23, 59, 59));
        EventDlqReplayLogDO row = EventDlqReplayLogDO.builder()
                .id(9L).tenantId(1L).eventId("evt-log-1").operatorId(1001L)
                .operatorName("集成管理员").reason("consumer fixed").result(EventDlqReplayResult.RESULT_SUCCESS)
                .outboxReplayed(true).inboxReplayed(2).build();
        when(eventDlqService.getReplayLogPage(reqVO, reqVO.getEventId(), reqVO.getOperatorId(),
                reqVO.getResult(), reqVO.getFromTime(), reqVO.getToTime()))
                .thenReturn(new PageResult<>(List.of(row), 1L));

        CommonResult<PageResult<EventDlqReplayLogRespVO>> result = controller.getReplayLogPage(reqVO);

        assertEquals(1L, result.getData().getTotal());
        assertEquals(1, result.getData().getList().size());
        EventDlqReplayLogRespVO actual = result.getData().getList().get(0);
        assertEquals("evt-log-1", actual.getEventId());
        assertEquals("集成管理员", actual.getOperatorName());
        assertEquals(2, actual.getInboxReplayed());
        assertTrue(actual.getOutboxReplayed());
    }

    @Test
    public void testReplayLogQueryPermissionContract() throws NoSuchMethodException {
        Method method = EventDlqController.class.getMethod("getReplayLogPage", EventDlqReplayLogPageReqVO.class);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertNotNull(preAuthorize);
        assertEquals("@ss.hasPermission('infra:event-dlq:query')", preAuthorize.value());
    }
    @Test
    public void testReplayEndpointPermissionContract() throws NoSuchMethodException {
        Method method = EventDlqController.class.getMethod("replay", EventDlqReplayReqVO.class);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertNotNull(preAuthorize);
        assertEquals("@ss.hasPermission('infra:event-dlq:replay')", preAuthorize.value());
    }

    @Test
    public void testQueryEndpointPermissionContract() throws NoSuchMethodException {
        Method method = EventDlqController.class.getMethod("getDeadOutboxPage",
                cn.zhicloud.module.infra.controller.admin.integration.vo.EventDlqOutboxPageReqVO.class);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);

        assertNotNull(preAuthorize);
        assertEquals("@ss.hasPermission('infra:event-dlq:query')", preAuthorize.value());
    }

    private EventDlqReplayReqVO replayReq(String... eventIds) {
        EventDlqReplayReqVO reqVO = new EventDlqReplayReqVO();
        reqVO.setEventIds(List.of(eventIds));
        reqVO.setReason("consumer fixed in change-20260926");
        reqVO.setFromTime(LocalDateTime.now().minusDays(1));
        reqVO.setToTime(LocalDateTime.now().plusDays(1));
        return reqVO;
    }

    private void login(Long userId, String nickname) {
        LoginUser loginUser = new LoginUser();
        loginUser.setId(userId);
        Map<String, String> info = new HashMap<>();
        info.put(LoginUser.INFO_KEY_NICKNAME, nickname);
        loginUser.setInfo(info);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, List.of()));
    }

}