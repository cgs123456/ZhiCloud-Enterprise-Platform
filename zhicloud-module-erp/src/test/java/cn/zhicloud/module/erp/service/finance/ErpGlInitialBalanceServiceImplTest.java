package cn.zhicloud.module.erp.service.finance;

import cn.zhicloud.framework.common.exception.ServiceException;
import cn.zhicloud.module.erp.controller.admin.finance.vo.glinitialbalance.ErpGlInitialBalanceExcelVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.glinitialbalance.ErpGlInitialBalanceRespVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.glinitialbalance.ErpGlInitialBalanceSaveReqVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.glinitialbalance.ErpTrialBalanceRespVO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpAccountBookDO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpGlAccountDO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpGlInitialBalanceDO;
import cn.zhicloud.module.erp.dal.mysql.finance.ErpAccountBookMapper;
import cn.zhicloud.module.erp.dal.mysql.finance.ErpGlAccountMapper;
import cn.zhicloud.module.erp.dal.mysql.finance.ErpGlInitialBalanceMapper;
import cn.zhicloud.module.erp.dal.mysql.finance.ErpGlVoucherMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static cn.zhicloud.module.erp.enums.ErrorCodeConstants.GL_INITIAL_BALANCE_IMPORT_AMOUNT_INVALID;
import static cn.zhicloud.module.erp.enums.ErrorCodeConstants.GL_INITIAL_BALANCE_IMPORT_SUBJECT_NOT_EXISTS;
import static cn.zhicloud.module.erp.enums.ErrorCodeConstants.GL_INITIAL_BALANCE_LOCKED;
import static cn.zhicloud.module.erp.enums.ErrorCodeConstants.GL_INITIAL_BALANCE_PROFIT_LOSS_YEAR_OPENING;
import static cn.zhicloud.module.erp.enums.ErrorCodeConstants.GL_INITIAL_BALANCE_SUBJECT_DUPLICATE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link ErpGlInitialBalanceServiceImpl} 单元测试（C6③ / ADR 265）
 *
 * <p>锁死不变式：
 * <ul>
 *   <li>派生年初余额口径（借向 = opening - yearDebit + yearCredit；其余 = opening + yearDebit - yearCredit）；</li>
 *   <li>试算平衡口径（借/贷方向归集、双向按符号拆分、0.005 容差）；</li>
 *   <li>保存护栏（已审核凭证锁定）与校验（科目重复、损益类年初为 0）；</li>
 *   <li>差分 upsert 分流（存量行 updateById / 新行 insert）；</li>
 *   <li>导入行口径（全空行跳过、科目不存在 / 金额非法报错、返回写入行数）。</li>
 * </ul>
 *
 * <p>H2 真表面（列对齐 / 唯一键）由 {@link ErpGlInitialBalanceH2TableTest} 覆盖。
 *
 * @author 智云
 */
@ExtendWith(MockitoExtension.class)
public class ErpGlInitialBalanceServiceImplTest {

    @Mock
    private ErpGlInitialBalanceMapper initialBalanceMapper;
    @Mock
    private ErpGlAccountMapper glAccountMapper;
    @Mock
    private ErpGlAccountService glAccountService;
    @Mock
    private ErpAccountBookMapper accountBookMapper;
    @Mock
    private ErpGlVoucherMapper glVoucherMapper;

    @InjectMocks
    private ErpGlInitialBalanceServiceImpl glInitialBalanceService;

    private static final Long ACCOUNT_SET_ID = 5L;

    private ErpGlAccountDO account(Long id, String code, Integer type, Integer direction, Boolean leaf) {
        return ErpGlAccountDO.builder().id(id).code(code).name("科目" + code)
                .type(type).balanceDirection(direction).isLeaf(leaf).build();
    }

    private ErpGlInitialBalanceDO row(Long accountId, String opening, String yearDebit, String yearCredit) {
        return ErpGlInitialBalanceDO.builder().accountSetId(ACCOUNT_SET_ID).accountId(accountId)
                .openingAmount(new BigDecimal(opening)).yearDebitAmount(new BigDecimal(yearDebit))
                .yearCreditAmount(new BigDecimal(yearCredit)).profitLossAmount(BigDecimal.ZERO).build();
    }

    private ErpGlInitialBalanceSaveReqVO.Balance item(Long subjectId, String opening,
                                                      String yearDebit, String yearCredit, String profitLoss) {
        ErpGlInitialBalanceSaveReqVO.Balance item = new ErpGlInitialBalanceSaveReqVO.Balance();
        item.setSubjectId(subjectId);
        item.setOpeningAmount(new BigDecimal(opening));
        item.setYearDebitAmount(new BigDecimal(yearDebit));
        item.setYearCreditAmount(new BigDecimal(yearCredit));
        item.setProfitLossAmount(new BigDecimal(profitLoss));
        return item;
    }

    private void stubNoVoucher(Long accountSetId) {
        when(accountBookMapper.selectListByAccountSetId(accountSetId)).thenReturn(List.of());
    }

    // ==================== 列表与派生年初余额 ====================

    @Test
    public void testGetInitialBalanceList_derivesYearOpeningByDirection() {
        when(glAccountMapper.selectListByType(10)).thenReturn(List.of(
                account(1L, "1001", 10, 10, true),   // 资产 / 借向
                account(2L, "2001", 20, 20, true),   // 负债 / 贷向
                account(3L, "1002", 10, 10, true))); // 无录入行
        when(initialBalanceMapper.selectListByAccountSetId(ACCOUNT_SET_ID)).thenReturn(List.of(
                row(1L, "1000.0000", "200.0000", "100.0000"),
                row(2L, "500.0000", "100.0000", "50.0000")));

        List<ErpGlInitialBalanceRespVO> result =
                glInitialBalanceService.getInitialBalanceList(ACCOUNT_SET_ID, 10);

        assertEquals(3, result.size());
        ErpGlInitialBalanceRespVO debitRow = find(result, 1L);
        // 借向：1000 - 200 + 100 = 900
        assertEquals(0, new BigDecimal("900.0000").compareTo(debitRow.getYearOpeningAmount()));
        assertEquals("资产", debitRow.getTypeName());
        assertEquals("借方", debitRow.getBalanceDirectionName());
        assertEquals(0, new BigDecimal("1000.0000").compareTo(debitRow.getOpeningAmount()));

        ErpGlInitialBalanceRespVO creditRow = find(result, 2L);
        // 贷向：500 + 100 - 50 = 550
        assertEquals(0, new BigDecimal("550.0000").compareTo(creditRow.getYearOpeningAmount()));
        assertEquals("负债", creditRow.getTypeName());
        assertEquals("贷方", creditRow.getBalanceDirectionName());

        ErpGlInitialBalanceRespVO emptyRow = find(result, 3L);
        assertEquals(0, BigDecimal.ZERO.compareTo(emptyRow.getOpeningAmount()));
        assertEquals(0, BigDecimal.ZERO.compareTo(emptyRow.getYearOpeningAmount()));
        assertEquals(0, BigDecimal.ZERO.compareTo(emptyRow.getProfitLossAmount()));
    }

    // ==================== 试算平衡 ====================

    @Test
    public void testGetTrialBalance_balancedWhenDebitEqualsCredit() {
        when(initialBalanceMapper.selectListByAccountSetId(ACCOUNT_SET_ID)).thenReturn(List.of(
                row(1L, "3000.0000", "0.0000", "0.0000"),
                row(2L, "2000.0000", "0.0000", "0.0000"),
                row(3L, "1000.0000", "0.0000", "0.0000")));
        when(glAccountMapper.selectBatchIds(anyCollection())).thenReturn(List.of(
                account(1L, "1001", 10, 10, true),   // 借向 3000
                account(2L, "2001", 20, 20, true),   // 贷向 2000
                account(3L, "2002", 20, 20, true))); // 贷向 1000

        ErpTrialBalanceRespVO vo = glInitialBalanceService.getTrialBalance(ACCOUNT_SET_ID);

        assertEquals(0, new BigDecimal("3000.0000").compareTo(vo.getOpeningDebitAmount()));
        assertEquals(0, new BigDecimal("3000.0000").compareTo(vo.getOpeningCreditAmount()));
        assertEquals(0, BigDecimal.ZERO.compareTo(vo.getOpeningDifferenceAmount()));
        assertTrue(vo.getBalanced());
    }

    @Test
    public void testGetTrialBalance_unbalancedReportsDifference() {
        when(initialBalanceMapper.selectListByAccountSetId(ACCOUNT_SET_ID)).thenReturn(List.of(
                row(1L, "3000.0000", "0.0000", "0.0000"),
                row(2L, "2000.0000", "0.0000", "0.0000")));
        when(glAccountMapper.selectBatchIds(anyCollection())).thenReturn(List.of(
                account(1L, "1001", 10, 10, true),
                account(2L, "2001", 20, 20, true)));

        ErpTrialBalanceRespVO vo = glInitialBalanceService.getTrialBalance(ACCOUNT_SET_ID);

        assertEquals(0, new BigDecimal("1000.0000").compareTo(vo.getOpeningDifferenceAmount()));
        assertFalse(vo.getBalanced());
    }

    @Test
    public void testGetTrialBalance_bothDirectionSplitsBySignAndSumsYearAmounts() {
        when(initialBalanceMapper.selectListByAccountSetId(ACCOUNT_SET_ID)).thenReturn(List.of(
                row(1L, "-500.0000", "0.0000", "0.0000"),   // 双向负数 → 等额计贷方
                row(2L, "300.0000", "120.0000", "70.0000"))); // 双向正数 → 借方；累计照列
        when(glAccountMapper.selectBatchIds(anyCollection())).thenReturn(List.of(
                account(1L, "6001", 60, 30, true),
                account(2L, "6002", 60, 30, true)));

        ErpTrialBalanceRespVO vo = glInitialBalanceService.getTrialBalance(ACCOUNT_SET_ID);

        assertEquals(0, new BigDecimal("300.0000").compareTo(vo.getOpeningDebitAmount()));
        assertEquals(0, new BigDecimal("500.0000").compareTo(vo.getOpeningCreditAmount()));
        assertEquals(0, new BigDecimal("-200.0000").compareTo(vo.getOpeningDifferenceAmount()));
        assertEquals(0, new BigDecimal("120.0000").compareTo(vo.getYearDebitAmount()));
        assertEquals(0, new BigDecimal("70.0000").compareTo(vo.getYearCreditAmount()));
        assertEquals(0, new BigDecimal("50.0000").compareTo(vo.getYearDifferenceAmount()));
        assertFalse(vo.getBalanced());
    }

    // ==================== 保存护栏与校验 ====================

    @Test
    public void testSave_lockedWhenApprovedVoucherExistsOnAccountSet() {
        when(accountBookMapper.selectListByAccountSetId(ACCOUNT_SET_ID)).thenReturn(List.of(
                ErpAccountBookDO.builder().id(7L).build(),
                ErpAccountBookDO.builder().id(8L).build()));
        when(glVoucherMapper.selectCountByAccountBookIdsAndStatus(anyCollection(), eq(20))).thenReturn(1L);

        ErpGlInitialBalanceSaveReqVO reqVO = new ErpGlInitialBalanceSaveReqVO();
        reqVO.setAccountSetId(ACCOUNT_SET_ID);
        reqVO.setBalances(List.of(item(1L, "100", "0", "0", "0")));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> glInitialBalanceService.saveInitialBalances(reqVO));
        assertEquals(GL_INITIAL_BALANCE_LOCKED.getCode(), ex.getCode());
        verify(initialBalanceMapper, never()).insert(org.mockito.ArgumentMatchers.any(ErpGlInitialBalanceDO.class));
    }

    @Test
    public void testSave_rejectsDuplicateSubjectIds() {
        stubNoVoucher(ACCOUNT_SET_ID);
        ErpGlInitialBalanceSaveReqVO reqVO = new ErpGlInitialBalanceSaveReqVO();
        reqVO.setAccountSetId(ACCOUNT_SET_ID);
        reqVO.setBalances(List.of(
                item(1L, "100", "0", "0", "0"),
                item(1L, "200", "0", "0", "0")));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> glInitialBalanceService.saveInitialBalances(reqVO));
        assertEquals(GL_INITIAL_BALANCE_SUBJECT_DUPLICATE.getCode(), ex.getCode());
    }

    @Test
    public void testSave_rejectsProfitLossSubjectWithNonZeroYearOpening() {
        stubNoVoucher(ACCOUNT_SET_ID);
        when(glAccountService.validateGlAccountIsLeaf(4L))
                .thenReturn(account(4L, "6001", 40, 20, true)); // 收入 / 贷向
        ErpGlInitialBalanceSaveReqVO reqVO = new ErpGlInitialBalanceSaveReqVO();
        reqVO.setAccountSetId(ACCOUNT_SET_ID);
        // 贷向：500 + 0 - 0 = 500 ≠ 0 → 拒绝
        reqVO.setBalances(List.of(item(4L, "500", "0", "0", "0")));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> glInitialBalanceService.saveInitialBalances(reqVO));
        assertEquals(GL_INITIAL_BALANCE_PROFIT_LOSS_YEAR_OPENING.getCode(), ex.getCode());
    }

    @Test
    public void testSave_diffUpsertUpdatesExistingAndInsertsNew() {
        stubNoVoucher(ACCOUNT_SET_ID);
        when(glAccountService.validateGlAccountIsLeaf(1L)).thenReturn(account(1L, "1001", 10, 10, true));
        when(glAccountService.validateGlAccountIsLeaf(2L)).thenReturn(account(2L, "1002", 10, 10, true));
        ErpGlInitialBalanceDO existing = row(1L, "50.0000", "0.0000", "0.0000");
        when(initialBalanceMapper.selectListByAccountSetId(ACCOUNT_SET_ID)).thenReturn(List.of(existing));

        ErpGlInitialBalanceSaveReqVO reqVO = new ErpGlInitialBalanceSaveReqVO();
        reqVO.setAccountSetId(ACCOUNT_SET_ID);
        reqVO.setBalances(List.of(
                item(1L, "100.0000", "10.0000", "20.0000", "0.0000"),
                item(2L, "200.0000", "0.0000", "0.0000", "0.0000")));

        glInitialBalanceService.saveInitialBalances(reqVO);

        // 存量行：updateById，金额就地覆盖
        ArgumentCaptor<ErpGlInitialBalanceDO> updateCaptor =
                ArgumentCaptor.forClass(ErpGlInitialBalanceDO.class);
        verify(initialBalanceMapper).updateById(updateCaptor.capture());
        ErpGlInitialBalanceDO updated = updateCaptor.getValue();
        assertEquals(1L, updated.getAccountId());
        assertEquals(0, new BigDecimal("100.0000").compareTo(updated.getOpeningAmount()));
        assertEquals(0, new BigDecimal("20.0000").compareTo(updated.getYearCreditAmount()));

        // 新行：insert，账套/科目正确
        ArgumentCaptor<ErpGlInitialBalanceDO> insertCaptor =
                ArgumentCaptor.forClass(ErpGlInitialBalanceDO.class);
        verify(initialBalanceMapper).insert(insertCaptor.capture());
        ErpGlInitialBalanceDO inserted = insertCaptor.getValue();
        assertEquals(ACCOUNT_SET_ID, inserted.getAccountSetId());
        assertEquals(2L, inserted.getAccountId());
        assertEquals(0, new BigDecimal("200.0000").compareTo(inserted.getOpeningAmount()));
    }

    // ==================== 导入 ====================

    @Test
    public void testImport_skipsAllEmptyRowsAndReturnsWrittenCount() {
        stubNoVoucher(ACCOUNT_SET_ID);
        when(glAccountMapper.selectByCode("1002")).thenReturn(account(2L, "1002", 10, 10, true));
        when(glAccountService.validateGlAccountIsLeaf(2L)).thenReturn(account(2L, "1002", 10, 10, true));
        when(initialBalanceMapper.selectListByAccountSetId(ACCOUNT_SET_ID)).thenReturn(List.of());

        // 第 1 行：模板未动过（四金额全空）→ 跳过；第 2 行：只填期初 → 写入；第 3 行：整行空白 → 跳过
        ErpGlInitialBalanceExcelVO untouched = ErpGlInitialBalanceExcelVO.builder()
                .subjectCode("1001").subjectName("科目1001").build();
        ErpGlInitialBalanceExcelVO filled = ErpGlInitialBalanceExcelVO.builder()
                .subjectCode("1002").subjectName("科目1002").openingAmount(new BigDecimal("100.0000")).build();
        ErpGlInitialBalanceExcelVO blank = ErpGlInitialBalanceExcelVO.builder().build();

        Integer count = glInitialBalanceService.importInitialBalances(ACCOUNT_SET_ID,
                List.of(untouched, filled, blank));

        assertEquals(1, count);
        ArgumentCaptor<ErpGlInitialBalanceDO> captor = ArgumentCaptor.forClass(ErpGlInitialBalanceDO.class);
        verify(initialBalanceMapper).insert(captor.capture());
        assertEquals(2L, captor.getValue().getAccountId());
        assertEquals(0, new BigDecimal("100.0000").compareTo(captor.getValue().getOpeningAmount()));
        // 未填列按 null=0 落库
        assertEquals(0, BigDecimal.ZERO.compareTo(captor.getValue().getYearDebitAmount()));
        assertEquals(0, BigDecimal.ZERO.compareTo(captor.getValue().getProfitLossAmount()));
    }

    @Test
    public void testImport_unknownSubjectCodeThrowsWithRowNo() {
        when(glAccountMapper.selectByCode("9999")).thenReturn(null);

        ErpGlInitialBalanceExcelVO rowVO = ErpGlInitialBalanceExcelVO.builder()
                .subjectCode("9999").openingAmount(new BigDecimal("1.0000")).build();

        ServiceException ex = assertThrows(ServiceException.class,
                () -> glInitialBalanceService.importInitialBalances(ACCOUNT_SET_ID, List.of(rowVO)));
        assertEquals(GL_INITIAL_BALANCE_IMPORT_SUBJECT_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    public void testImport_negativeAmountThrows() {
        when(glAccountMapper.selectByCode("1001")).thenReturn(account(1L, "1001", 10, 10, true));

        ErpGlInitialBalanceExcelVO rowVO = ErpGlInitialBalanceExcelVO.builder()
                .subjectCode("1001").openingAmount(new BigDecimal("-5.0000")).build();

        ServiceException ex = assertThrows(ServiceException.class,
                () -> glInitialBalanceService.importInitialBalances(ACCOUNT_SET_ID, List.of(rowVO)));
        assertEquals(GL_INITIAL_BALANCE_IMPORT_AMOUNT_INVALID.getCode(), ex.getCode());
    }

    @Test
    public void testExport_returnsLeafRowsWithZeroDefaults() {
        when(glAccountMapper.selectListByLeafOrderByCode()).thenReturn(List.of(
                account(1L, "1001", 10, 10, true),
                account(2L, "1002", 10, 10, true)));
        when(initialBalanceMapper.selectListByAccountSetId(ACCOUNT_SET_ID)).thenReturn(List.of(
                row(1L, "1000.0000", "0.0000", "0.0000")));

        List<ErpGlInitialBalanceExcelVO> result =
                glInitialBalanceService.getInitialBalanceExcelList(ACCOUNT_SET_ID);

        assertEquals(2, result.size());
        assertEquals("1001", result.get(0).getSubjectCode());
        assertEquals(0, new BigDecimal("1000.0000").compareTo(result.get(0).getOpeningAmount()));
        assertNotNull(result.get(1).getSubjectCode());
        assertEquals(0, BigDecimal.ZERO.compareTo(result.get(1).getOpeningAmount()));
    }

    @Test
    public void testImportTemplate_leavesAmountsNull() {
        when(glAccountMapper.selectListByLeafOrderByCode()).thenReturn(List.of(
                account(1L, "1001", 10, 10, true)));

        List<ErpGlInitialBalanceExcelVO> result =
                glInitialBalanceService.getImportTemplateList(ACCOUNT_SET_ID);

        assertEquals(1, result.size());
        assertEquals("1001", result.get(0).getSubjectCode());
        assertEquals(null, result.get(0).getOpeningAmount());
    }

    private ErpGlInitialBalanceRespVO find(List<ErpGlInitialBalanceRespVO> list, Long subjectId) {
        return list.stream().filter(v -> subjectId.equals(v.getSubjectId())).findFirst().orElseThrow();
    }

}
