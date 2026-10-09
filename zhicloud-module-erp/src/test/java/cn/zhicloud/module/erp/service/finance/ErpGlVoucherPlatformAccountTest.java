package cn.zhicloud.module.erp.service.finance;

import cn.zhicloud.framework.tenant.core.context.TenantContextHolder;
import cn.zhicloud.module.erp.controller.admin.finance.vo.glvoucher.ErpGlVoucherEntryReqVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.glvoucher.ErpGlVoucherSaveReqVO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpGlAccountDO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpGlVoucherDO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpGlVoucherEntryDO;
import cn.zhicloud.module.erp.dal.mysql.accounting.ErpAccountingPostingMapper;
import cn.zhicloud.module.erp.dal.mysql.finance.ErpGlAccountMapper;
import cn.zhicloud.module.erp.dal.mysql.finance.ErpGlVoucherEntryMapper;
import cn.zhicloud.module.erp.dal.mysql.finance.ErpGlVoucherMapper;
import cn.zhicloud.module.erp.enums.finance.ErpGlVoucherStatusEnum;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ErpGlVoucherPlatformAccountTest {

    @Mock
    private ErpGlVoucherMapper glVoucherMapper;

    @Mock
    private ErpGlVoucherEntryMapper glVoucherEntryMapper;

    @Mock
    private ErpGlAccountMapper glAccountMapper;

    @Mock
    private ErpAccountingPostingMapper accountingPostingMapper;

    private ErpGlVoucherServiceImpl glVoucherService;

    @BeforeEach
    void setUp() {
        glVoucherService = new ErpGlVoucherServiceImpl();
        ReflectionTestUtils.setField(glVoucherService, "glVoucherMapper", glVoucherMapper);
        ReflectionTestUtils.setField(glVoucherService, "glVoucherEntryMapper", glVoucherEntryMapper);
        ReflectionTestUtils.setField(glVoucherService, "glAccountMapper", glAccountMapper);
        ReflectionTestUtils.setField(glVoucherService, "accountingPostingMapper", accountingPostingMapper);
        TenantContextHolder.setTenantId(9001L);
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void createGlVoucherUsesPlatformAccountWhenTenantAccountMissing() {
        ErpGlAccountDO platformAccount = platformAccount();
        when(glAccountMapper.selectById(3L)).thenAnswer(invocation ->
                Long.valueOf(0L).equals(TenantContextHolder.getTenantId()) ? platformAccount : null);
        when(glVoucherMapper.selectByVoucherNo("V-9001-1")).thenReturn(null);
        when(glVoucherMapper.insert(any(ErpGlVoucherDO.class))).thenAnswer(invocation -> {
            ((ErpGlVoucherDO) invocation.getArgument(0)).setId(501L);
            return 1;
        });

        ErpGlVoucherSaveReqVO request = new ErpGlVoucherSaveReqVO();
        request.setVoucherNo("V-9001-1");
        request.setVoucherDate(LocalDate.of(2026, 10, 8));
        request.setEntries(List.of(entry(3L, BigDecimal.ONE, BigDecimal.ZERO),
                entry(3L, BigDecimal.ZERO, BigDecimal.ONE)));

        assertEquals(501L, glVoucherService.createGlVoucher(request));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ErpGlVoucherEntryDO>> entriesCaptor = ArgumentCaptor.forClass(List.class);
        verify(glVoucherEntryMapper).insertBatch(entriesCaptor.capture());
        assertEquals("1122", entriesCaptor.getValue().get(0).getAccountCode());
        assertEquals("应收账款", entriesCaptor.getValue().get(0).getAccountName());
    }

    @Test
    void approveGlVoucherUpdatesPlatformAccountInPlatformTenant() {
        ErpGlAccountDO platformAccount = platformAccount();
        when(glAccountMapper.selectById(3L)).thenAnswer(invocation ->
                Long.valueOf(0L).equals(TenantContextHolder.getTenantId()) ? platformAccount : null);
        when(glVoucherMapper.selectById(501L)).thenReturn(draftVoucher());
        when(glVoucherEntryMapper.selectListByVoucherId(501L)).thenReturn(List.of(
                entryDO(3L, BigDecimal.ONE, BigDecimal.ZERO),
                entryDO(3L, BigDecimal.ZERO, BigDecimal.ONE)));
        when(accountingPostingMapper.selectListByVoucherId(501L)).thenReturn(List.of());
        when(glAccountMapper.updateById(any(ErpGlAccountDO.class))).thenAnswer(invocation -> {
            assertEquals(Long.valueOf(0L), TenantContextHolder.getTenantId());
            return 1;
        });

        glVoucherService.approveGlVoucher(501L);

        ArgumentCaptor<ErpGlAccountDO> accountCaptor = ArgumentCaptor.forClass(ErpGlAccountDO.class);
        // 聚合后同一科目只更新一次，借贷金额在同一行中同时体现
        verify(glAccountMapper, times(1)).updateById(accountCaptor.capture());
        assertEquals(BigDecimal.ONE, accountCaptor.getValue().getCurrentDebit());
        assertEquals(BigDecimal.ONE, accountCaptor.getValue().getCurrentCredit());
    }

    private static ErpGlAccountDO platformAccount() {
        ErpGlAccountDO account = new ErpGlAccountDO();
        account.setId(3L);
        account.setCode("1122");
        account.setName("应收账款");
        account.setIsLeaf(true);
        account.setTenantId(0L);
        account.setCurrentDebit(BigDecimal.ZERO);
        account.setCurrentCredit(BigDecimal.ZERO);
        account.setOpeningDebit(BigDecimal.ZERO);
        account.setOpeningCredit(BigDecimal.ZERO);
        return account;
    }

    private static ErpGlVoucherDO draftVoucher() {
        ErpGlVoucherDO voucher = new ErpGlVoucherDO();
        voucher.setId(501L);
        voucher.setVoucherNo("V-9001-1");
        voucher.setStatus(ErpGlVoucherStatusEnum.DRAFT.getStatus());
        return voucher;
    }

    private static ErpGlVoucherEntryDO entryDO(Long accountId, BigDecimal debit, BigDecimal credit) {
        ErpGlVoucherEntryDO entry = new ErpGlVoucherEntryDO();
        entry.setAccountId(accountId);
        entry.setDebitAmount(debit);
        entry.setCreditAmount(credit);
        return entry;
    }

    private static ErpGlVoucherEntryReqVO entry(Long accountId, BigDecimal debit, BigDecimal credit) {
        ErpGlVoucherEntryReqVO entry = new ErpGlVoucherEntryReqVO();
        entry.setAccountId(accountId);
        entry.setDebitAmount(debit);
        entry.setCreditAmount(credit);
        entry.setSort(1);
        entry.setSummary("platform account");
        return entry;
    }
}
