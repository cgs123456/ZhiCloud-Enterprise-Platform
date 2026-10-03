package cn.zhicloud.module.crm.service.receivable;

import cn.zhicloud.framework.common.exception.ServiceException;
import cn.zhicloud.module.bpm.api.task.BpmProcessInstanceApi;
import cn.zhicloud.module.bpm.api.task.dto.BpmProcessInstanceCreateReqDTO;
import cn.zhicloud.module.crm.controller.admin.receivable.vo.receivable.CrmReceivableSaveReqVO;
import cn.zhicloud.module.crm.dal.dataobject.contract.CrmContractDO;
import cn.zhicloud.module.crm.dal.dataobject.receivable.CrmReceivableDO;
import cn.zhicloud.module.crm.dal.mysql.contract.CrmContractMapper;
import cn.zhicloud.module.crm.dal.mysql.receivable.CrmReceivableMapper;
import cn.zhicloud.module.crm.enums.common.CrmAuditStatusEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link CrmReceivableServiceImpl} 的并发与外部副作用边界测试。
 */
@ExtendWith(MockitoExtension.class)
public class CrmReceivableServiceConcurrencyTest {

    private static final Long RECEIVABLE_ID = 1L;
    private static final Long CONTRACT_ID = 10L;
    private static final Long USER_ID = 20L;

    @Mock
    private CrmReceivableMapper receivableMapper;
    @Mock
    private CrmContractMapper contractMapper;
    @Mock
    private BpmProcessInstanceApi bpmProcessInstanceApi;

    private CrmReceivableServiceImpl receivableService;

    @BeforeEach
    void setUp() {
        receivableService = new CrmReceivableServiceImpl();
        ReflectionTestUtils.setField(receivableService, "receivableMapper", receivableMapper);
        ReflectionTestUtils.setField(receivableService, "contractMapper", contractMapper);
        ReflectionTestUtils.setField(receivableService, "bpmProcessInstanceApi", bpmProcessInstanceApi);
    }

    @Test
    void updateReceivable_shouldFailWhenOptimisticVersionChanged() {
        CrmReceivableDO oldReceivable = draftReceivable();
        CrmContractDO contract = approvedContract();
        CrmReceivableSaveReqVO updateReqVO = new CrmReceivableSaveReqVO()
                .setId(RECEIVABLE_ID)
                .setOwnerUserId(USER_ID)
                .setContractId(CONTRACT_ID)
                .setPrice(new BigDecimal("10.00"))
                .setReturnTime(LocalDateTime.of(2026, 9, 27, 10, 0));

        when(receivableMapper.selectById(RECEIVABLE_ID)).thenReturn(oldReceivable);
        when(contractMapper.selectByIdForUpdate(CONTRACT_ID)).thenReturn(contract);
        when(receivableMapper.selectListByContractIdAndStatus(eq(CONTRACT_ID), anyCollection()))
                .thenReturn(Collections.emptyList());
        when(receivableMapper.updateById(org.mockito.ArgumentMatchers.<CrmReceivableDO>any())).thenReturn(0);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> receivableService.updateReceivable(updateReqVO));
        assertEquals(1_020_020_000, exception.getCode());
        verify(receivableMapper).updateById(org.mockito.ArgumentMatchers.argThat((CrmReceivableDO update) -> update != null && Integer.valueOf(0).equals(update.getVersion())));
    }

    @Test
    void submitReceivable_shouldClaimBeforeCallingBpm() {
        CrmReceivableDO draft = draftReceivable();
        lenient().when(receivableMapper.selectById(RECEIVABLE_ID)).thenReturn(draft);
        when(receivableMapper.selectByIdForUpdate(RECEIVABLE_ID)).thenReturn(draft);
        when(receivableMapper.updateById(org.mockito.ArgumentMatchers.<CrmReceivableDO>any())).thenReturn(1);
        when(bpmProcessInstanceApi.createProcessInstance(eq(USER_ID), any(BpmProcessInstanceCreateReqDTO.class)))
                .thenReturn("process-1");

        receivableService.submitReceivable(RECEIVABLE_ID, USER_ID);

        InOrder order = inOrder(receivableMapper, bpmProcessInstanceApi);
        order.verify(receivableMapper).selectByIdForUpdate(RECEIVABLE_ID);
        order.verify(receivableMapper).updateById(org.mockito.ArgumentMatchers.argThat((CrmReceivableDO update) ->
                update != null
                        && Integer.valueOf(CrmAuditStatusEnum.PROCESS.getStatus()).equals(update.getAuditStatus())
                        && Integer.valueOf(0).equals(update.getVersion())));
        order.verify(bpmProcessInstanceApi).createProcessInstance(eq(USER_ID), any(BpmProcessInstanceCreateReqDTO.class));
        order.verify(receivableMapper).updateById(org.mockito.ArgumentMatchers.argThat((CrmReceivableDO update) ->
                update != null
                        && "process-1".equals(update.getProcessInstanceId())
                        && Integer.valueOf(1).equals(update.getVersion())));
    }

    @Test
    void submitReceivable_shouldNotCallBpmWhenClaimFails() {
        CrmReceivableDO draft = draftReceivable();
        lenient().when(receivableMapper.selectById(RECEIVABLE_ID)).thenReturn(draft);
        when(receivableMapper.selectByIdForUpdate(RECEIVABLE_ID)).thenReturn(draft);
        when(receivableMapper.updateById(org.mockito.ArgumentMatchers.<CrmReceivableDO>any())).thenReturn(0);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> receivableService.submitReceivable(RECEIVABLE_ID, USER_ID));
        assertEquals(1_020_020_000, exception.getCode());
        verify(bpmProcessInstanceApi, never()).createProcessInstance(any(), any());
    }

    private CrmReceivableDO draftReceivable() {
        return CrmReceivableDO.builder()
                .id(RECEIVABLE_ID)
                .no("REC-001")
                .contractId(CONTRACT_ID)
                .customerId(30L)
                .ownerUserId(USER_ID)
                .price(new BigDecimal("10.00"))
                .auditStatus(CrmAuditStatusEnum.DRAFT.getStatus())
                .version(0)
                .build();
    }

    private CrmContractDO approvedContract() {
        return CrmContractDO.builder()
                .id(CONTRACT_ID)
                .customerId(30L)
                .totalPrice(new BigDecimal("100.00"))
                .auditStatus(CrmAuditStatusEnum.APPROVE.getStatus())
                .build();
    }

}
