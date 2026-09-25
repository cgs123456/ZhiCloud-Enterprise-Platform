package cn.zhicloud.module.erp.service.finance;

import cn.zhicloud.module.erp.dal.dataobject.accounting.ErpAccountingEventDO;
import cn.zhicloud.module.erp.dal.dataobject.accounting.ErpAccountingPostingDO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpGlVoucherDO;
import cn.zhicloud.module.erp.dal.mysql.accounting.ErpAccountingEventMapper;
import cn.zhicloud.module.erp.dal.mysql.accounting.ErpAccountingPostingMapper;
import cn.zhicloud.module.erp.dal.mysql.finance.ErpGlAccountMapper;
import cn.zhicloud.module.erp.dal.mysql.finance.ErpGlVoucherEntryMapper;
import cn.zhicloud.module.erp.dal.mysql.finance.ErpGlVoucherMapper;
import cn.zhicloud.module.erp.enums.accounting.ErpAccountingEventStatusEnum;
import cn.zhicloud.module.erp.enums.accounting.ErpAccountingPostingStatusEnum;
import cn.zhicloud.module.erp.enums.finance.ErpGlVoucherStatusEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ErpGlVoucherAccountingStatusTest {

    @Mock
    private ErpGlVoucherMapper glVoucherMapper;
    @Mock
    private ErpGlVoucherEntryMapper glVoucherEntryMapper;
    @Mock
    private ErpGlAccountMapper glAccountMapper;
    @Mock
    private ErpPeriodService periodService;
    @Mock
    private ErpAccountingPostingMapper accountingPostingMapper;
    @Mock
    private ErpAccountingEventMapper accountingEventMapper;

    private ErpGlVoucherServiceImpl glVoucherService;

    @BeforeEach
    void setUp() {
        glVoucherService = new ErpGlVoucherServiceImpl();
        ReflectionTestUtils.setField(glVoucherService, "glVoucherMapper", glVoucherMapper);
        ReflectionTestUtils.setField(glVoucherService, "glVoucherEntryMapper", glVoucherEntryMapper);
        ReflectionTestUtils.setField(glVoucherService, "glAccountMapper", glAccountMapper);
        ReflectionTestUtils.setField(glVoucherService, "periodService", periodService);
        setFieldIfPresent("accountingPostingMapper", accountingPostingMapper);
        setFieldIfPresent("accountingEventMapper", accountingEventMapper);
    }

    @Test
    void approvingVoucherMarksLinkedEventAndPostingPosted() {
        ErpAccountingPostingDO posting = new ErpAccountingPostingDO().setId(1L).setEventId("event-1")
                .setVoucherId(501L).setStatus(ErpAccountingPostingStatusEnum.POSTING.getStatus());
        ErpAccountingEventDO event = new ErpAccountingEventDO().setId(100L).setEventId("event-1")
                .setStatus(ErpAccountingEventStatusEnum.POSTING.getStatus());
        lenient().when(glVoucherMapper.selectById(501L)).thenReturn(draftVoucher(501L));
        lenient().when(glVoucherEntryMapper.selectListByVoucherId(501L)).thenReturn(List.of());
        lenient().when(accountingPostingMapper.selectListByVoucherId(501L)).thenReturn(List.of(posting));
        lenient().when(accountingEventMapper.selectByEventId("event-1")).thenReturn(event);
        lenient().when(accountingPostingMapper.updateById(any(ErpAccountingPostingDO.class))).thenReturn(1);
        lenient().when(accountingEventMapper.updateById(any(ErpAccountingEventDO.class))).thenReturn(1);

        glVoucherService.approveGlVoucher(501L);

        ArgumentCaptor<ErpAccountingEventDO> eventCaptor = ArgumentCaptor.forClass(ErpAccountingEventDO.class);
        verify(accountingEventMapper).updateById(eventCaptor.capture());
        assertEquals(ErpAccountingEventStatusEnum.POSTED.getStatus(), eventCaptor.getValue().getStatus());
        assertNotNull(eventCaptor.getValue().getPostedAt());

        ArgumentCaptor<ErpAccountingPostingDO> postingCaptor = ArgumentCaptor.forClass(ErpAccountingPostingDO.class);
        verify(accountingPostingMapper).updateById(postingCaptor.capture());
        assertEquals(ErpAccountingPostingStatusEnum.POSTED.getStatus(), postingCaptor.getValue().getStatus());
        assertNotNull(postingCaptor.getValue().getPostedAt());
    }

    @Test
    void approvingVoucherDoesNotOverwriteAlreadyPostedStatus() {
        LocalDateTime postedAt = LocalDateTime.of(2026, 9, 25, 10, 30);
        ErpAccountingPostingDO posting = new ErpAccountingPostingDO().setId(1L).setEventId("event-1")
                .setVoucherId(501L).setStatus(ErpAccountingPostingStatusEnum.POSTED.getStatus())
                .setPostedAt(postedAt);
        ErpAccountingEventDO event = new ErpAccountingEventDO().setId(100L).setEventId("event-1")
                .setStatus(ErpAccountingEventStatusEnum.POSTED.getStatus()).setPostedAt(postedAt);
        lenient().when(glVoucherMapper.selectById(501L)).thenReturn(draftVoucher(501L));
        lenient().when(glVoucherEntryMapper.selectListByVoucherId(501L)).thenReturn(List.of());
        lenient().when(accountingPostingMapper.selectListByVoucherId(501L)).thenReturn(List.of(posting));
        lenient().when(accountingEventMapper.selectByEventId("event-1")).thenReturn(event);

        glVoucherService.approveGlVoucher(501L);

        verify(accountingEventMapper, never()).updateById(any(ErpAccountingEventDO.class));
        verify(accountingPostingMapper, never()).updateById(any(ErpAccountingPostingDO.class));
        assertEquals(postedAt, event.getPostedAt());
        assertEquals(postedAt, posting.getPostedAt());
    }

    private void setFieldIfPresent(String fieldName, Object value) {
        try {
            ReflectionTestUtils.setField(glVoucherService, fieldName, value);
        } catch (IllegalArgumentException ignored) {
            // RED 阶段依赖尚未接入；测试仍会因缺少状态推进调用而失败。
        }
    }

    private static ErpGlVoucherDO draftVoucher(Long id) {
        return new ErpGlVoucherDO().setId(id).setVoucherNo("V001")
                .setVoucherDate(LocalDate.of(2026, 9, 25))
                .setStatus(ErpGlVoucherStatusEnum.DRAFT.getStatus());
    }
}