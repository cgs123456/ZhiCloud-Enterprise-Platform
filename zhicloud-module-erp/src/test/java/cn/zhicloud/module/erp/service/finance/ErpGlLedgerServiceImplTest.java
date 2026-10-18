package cn.zhicloud.module.erp.service.finance;

import cn.zhicloud.module.erp.controller.admin.finance.vo.glledger.ErpGlLedgerColumnVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.glledger.ErpGlLedgerDetailExcelVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.glledger.ErpGlLedgerDetailRespVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.glledger.ErpGlLedgerGeneralExcelVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.glledger.ErpGlLedgerGeneralRespVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.glledger.ErpGlLedgerMultiColumnExcelDataVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.glledger.ErpGlLedgerMultiColumnRespVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.glledger.ErpGlLedgerReqVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.glledger.ErpGlLedgerSubjectBalanceExcelVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.glledger.ErpGlLedgerSubjectBalanceRespVO;
import cn.zhicloud.module.erp.controller.admin.finance.vo.glledger.ErpGlLedgerSubjectRespVO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpAccountBookDO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpGlAccountDO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpGlInitialBalanceDO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpGlVoucherDO;
import cn.zhicloud.module.erp.dal.dataobject.finance.ErpGlVoucherEntryDO;
import cn.zhicloud.module.erp.dal.mysql.finance.ErpAccountBookMapper;
import cn.zhicloud.module.erp.dal.mysql.finance.ErpGlAccountMapper;
import cn.zhicloud.module.erp.dal.mysql.finance.ErpGlInitialBalanceMapper;
import cn.zhicloud.module.erp.dal.mysql.finance.ErpGlVoucherEntryMapper;
import cn.zhicloud.module.erp.dal.mysql.finance.ErpGlVoucherMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

/**
 * {@link ErpGlLedgerServiceImpl} 单元测试（C6② / ADR 266）
 *
 * <p>钉住账表口径：方向系数与借贷拆分、父级聚合不做二次变换、δ（本年累计是否含期初行的本年累计）
 * 的启用年/跨年两分支、四族行模型与专栏金额、科目树活动集与祖先补齐、空账套返回空结果。
 *
 * @author 智云
 */
@ExtendWith(MockitoExtension.class)
public class ErpGlLedgerServiceImplTest {

    private static final Long ACCOUNT_SET_ID = 9001L;
    private static final Long BOOK_ID = 11L;

    @Mock
    private ErpAccountBookMapper accountBookMapper;
    @Mock
    private ErpGlVoucherMapper glVoucherMapper;
    @Mock
    private ErpGlVoucherEntryMapper glVoucherEntryMapper;
    @Mock
    private ErpGlAccountMapper glAccountMapper;
    @Mock
    private ErpGlInitialBalanceMapper initialBalanceMapper;
    @InjectMocks
    private ErpGlLedgerQueryEngine engine;

    private ErpGlLedgerServiceImpl service;

    @BeforeEach
    public void setUp() {
        service = new ErpGlLedgerServiceImpl(engine, new ErpGlLedgerRowAssembler(engine));
    }

    // ==================== 明细账 ====================

    @Test
    public void testGetDetailList_debitSubjectOpeningRunningAndTotals() {
        stubLedger(List.of(account(1001L, 0L, "1001", "库存现金", 10, 10, 1, true)),
                List.of(voucher(201L, LocalDate.of(2026, 1, 15), "记-0001")),
                List.of(entry(301L, 201L, 1001L, "1001", "库存现金", "收现款", "500", "0", 1)),
                List.of(initialRow(1001L, "1000", "200", "0")));

        List<ErpGlLedgerDetailRespVO> rows = service.getDetailList(req("2026-01", "2026-01", 1001L));

        assertEquals(4, rows.size());
        ErpGlLedgerDetailRespVO opening = rows.get(0);
        assertEquals(1, opening.getRowType());
        assertEquals("期初余额", opening.getDigest());
        assertEquals("2026-01", opening.getPeriod());
        assertEquals("借", opening.getBalanceDirection());
        assertAmt("1000", opening.getBalance());
        assertAmt("0", opening.getDebitAmount());
        assertAmt("0", opening.getCreditAmount());

        ErpGlLedgerDetailRespVO detail = rows.get(1);
        assertEquals(2, detail.getRowType());
        assertEquals(301L, detail.getEntryId());
        assertEquals(201L, detail.getVoucherId());
        assertEquals("记-0001", detail.getVoucherNumber());
        assertEquals("收现款", detail.getDigest());
        assertEquals("2026-01", detail.getPeriod());
        assertEquals(LocalDate.of(2026, 1, 15), detail.getAccountDate());
        assertAmt("500", detail.getDebitAmount());
        assertAmt("1500", detail.getBalance());

        ErpGlLedgerDetailRespVO period = rows.get(2);
        assertEquals(3, period.getRowType());
        assertEquals("本期合计", period.getDigest());
        assertAmt("500", period.getDebitAmount());
        assertAmt("1500", period.getBalance());

        ErpGlLedgerDetailRespVO year = rows.get(3);
        assertEquals(4, year.getRowType());
        assertEquals("本年累计", year.getDigest());
        // δ=1（无更早凭证）：本年累计借方 = 期初行 200 + 凭证 500
        assertAmt("700", year.getDebitAmount());
        assertAmt("0", year.getCreditAmount());
        assertAmt("1500", year.getBalance());
    }

    @Test
    public void testGetDetailList_creditSubjectDirectionSplit() {
        stubLedger(List.of(account(2001L, 0L, "2001", "短期借款", 20, 20, 1, true)),
                List.of(voucher(202L, LocalDate.of(2026, 1, 20), "记-0002")),
                List.of(entry(302L, 202L, 2001L, "2001", "短期借款", "借入", "0", "500", 1)),
                List.of(initialRow(2001L, "800", "0", "100")));

        List<ErpGlLedgerDetailRespVO> rows = service.getDetailList(req("2026-01", "2026-01", 2001L));

        assertEquals(4, rows.size());
        ErpGlLedgerDetailRespVO opening = rows.get(0);
        assertEquals("贷", opening.getBalanceDirection());
        assertAmt("800", opening.getBalance());
        assertAmt("0", opening.getDebitAmount());
        assertAmt("0", opening.getCreditAmount());

        ErpGlLedgerDetailRespVO detail = rows.get(1);
        assertEquals("贷", detail.getBalanceDirection());
        assertAmt("1300", detail.getBalance());
        assertAmt("0", detail.getDebitAmount());
        assertAmt("500", detail.getCreditAmount());

        ErpGlLedgerDetailRespVO year = rows.get(3);
        assertAmt("0", year.getDebitAmount());
        // 本年累计贷方 = 期初行 100 + 凭证 500
        assertAmt("600", year.getCreditAmount());
    }

    @Test
    public void testGetDetailList_parentSubtreeAggregationKeepsChildDirection() {
        stubLedger(List.of(account(1000L, 0L, "1000", "货币资金", 10, 10, 1, false),
                        account(1001L, 1000L, "1001", "库存现金", 10, 10, 2, true),
                        account(1002L, 1000L, "1002", "其他货币资金", 10, 20, 2, true)),
                List.of(), List.of(),
                List.of(initialRow(1001L, "100", "0", "0"),
                        initialRow(1002L, "200", "0", "0")));

        List<ErpGlLedgerDetailRespVO> rows = service.getDetailList(req("2026-01", "2026-01", 1000L));

        // 无分录 → 期初 + 本期合计 + 本年累计三行
        assertEquals(3, rows.size());
        // 子树净额 = +100（借向子科目）- 200（贷向子科目）= -100 → 展示贷方 100
        assertEquals("贷", rows.get(0).getBalanceDirection());
        assertAmt("100", rows.get(0).getBalance());
        assertAmt("100", rows.get(2).getBalance());
    }

    @Test
    public void testGetDetailList_crossYearQueryExcludesInitialYearToDate() {
        stubLedger(List.of(account(1001L, 0L, "1001", "库存现金", 10, 10, 1, true)),
                List.of(voucher(203L, LocalDate.of(2026, 10, 8), "记-0003")),
                List.of(entry(303L, 203L, 1001L, "1001", "库存现金", "收现款", "500", "0", 1)),
                List.of(initialRow(1001L, "1000", "200", "0")));

        List<ErpGlLedgerDetailRespVO> rows = service.getDetailList(req("2027-01", "2027-01", 1001L));

        assertEquals(3, rows.size());
        // 期初 = 启用锚点 1000 + 2026-10 已发生的净额 500
        assertAmt("1500", rows.get(0).getBalance());
        assertAmt("1500", rows.get(1).getBalance());
        // 跨年查询：存在更早凭证 → δ=0，本年累计不再叠加启用年的本年累计 200
        assertAmt("0", rows.get(2).getDebitAmount());
        assertAmt("0", rows.get(2).getCreditAmount());
        assertAmt("1500", rows.get(2).getBalance());
    }

    @Test
    public void testGetDetailList_withoutInitialBalanceStartsFromZero() {
        stubLedger(List.of(account(1001L, 0L, "1001", "库存现金", 10, 10, 1, true)),
                List.of(voucher(204L, LocalDate.of(2026, 2, 10), "记-0004")),
                List.of(entry(304L, 204L, 1001L, "1001", "库存现金", "收现款", "200", "0", 1)),
                List.of());

        List<ErpGlLedgerDetailRespVO> rows = service.getDetailList(req("2026-02", "2026-02", 1001L));

        assertEquals(4, rows.size());
        assertAmt("0", rows.get(0).getBalance());
        assertAmt("200", rows.get(1).getBalance());
        assertAmt("200", rows.get(3).getDebitAmount());
    }

    @Test
    public void testGetDetailList_bothDirectionSubjectTreatedAsDebitAnchor() {
        stubLedger(List.of(account(1001L, 0L, "1001", "库存现金", 10, 30, 1, true)),
                List.of(voucher(205L, LocalDate.of(2026, 3, 5), "记-0005")),
                List.of(entry(305L, 205L, 1001L, "1001", "库存现金", "付出", "0", "250", 1)),
                List.of(initialRow(1001L, "100", "0", "0")));

        List<ErpGlLedgerDetailRespVO> rows = service.getDetailList(req("2026-03", "2026-03", 1001L));

        // 双向(30) 按借方口径锚定：期初借 100，发生贷 250 → 明细行翻到贷方 150
        assertEquals("借", rows.get(0).getBalanceDirection());
        assertAmt("100", rows.get(0).getBalance());
        assertEquals("贷", rows.get(1).getBalanceDirection());
        assertAmt("150", rows.get(1).getBalance());
    }

    @Test
    public void testGetDetailList_unknownSubjectReturnsEmpty() {
        stubLedger(List.of(account(1001L, 0L, "1001", "库存现金", 10, 10, 1, true)),
                List.of(), List.of(), List.of(initialRow(1001L, "100", "0", "0")));

        assertTrue(service.getDetailList(req("2026-01", "2026-01", 9999L)).isEmpty());
        assertTrue(service.getDetailExcelList(req("2026-01", "2026-01", 9999L)).isEmpty());
    }

    // ==================== 总账 ====================

    @Test
    public void testGetGeneralList_monthRowsAndYearTotal() {
        stubLedger(List.of(account(1000L, 0L, "1000", "货币资金", 10, 10, 1, false),
                        account(1001L, 1000L, "1001", "库存现金", 10, 10, 2, true),
                        account(6601L, 0L, "6601", "管理费用", 50, 10, 1, true)),
                List.of(voucher(206L, LocalDate.of(2026, 1, 10), "记-0006"),
                        voucher(207L, LocalDate.of(2026, 3, 5), "记-0007")),
                List.of(entry(306L, 206L, 1001L, "1001", "库存现金", "收现款", "300", "0", 1),
                        entry(307L, 207L, 1001L, "1001", "库存现金", "付现款", "0", "50", 1)),
                List.of(initialRow(1001L, "1000", "100", "0")));
        ErpGlLedgerReqVO reqVO = req("2026-01", "2026-03", null);
        reqVO.setMinLevel(1);
        reqVO.setMaxLevel(1);

        List<ErpGlLedgerGeneralRespVO> rows = service.getGeneralList(reqVO);

        // 6601 无数据被跳过；1000 有数据 → 期初 + 2026-01 + 2026-03 + 本年累计
        assertEquals(4, rows.size());
        for (ErpGlLedgerGeneralRespVO row : rows) {
            assertEquals(1000L, row.getSubjectId());
        }
        assertEquals(1, rows.get(0).getRowType());
        assertEquals("期初余额", rows.get(0).getDigest());
        assertAmt("1000", rows.get(0).getBalance());
        assertEquals(2, rows.get(1).getRowType());
        assertEquals("2026-01", rows.get(1).getPeriod());
        assertEquals("本月合计", rows.get(1).getDigest());
        assertAmt("300", rows.get(1).getDebitAmount());
        assertAmt("1300", rows.get(1).getBalance());
        assertEquals("2026-03", rows.get(2).getPeriod());
        assertAmt("50", rows.get(2).getCreditAmount());
        assertAmt("1250", rows.get(2).getBalance());
        // 总账的本年累计行 rowType = 4（明细账的"本期合计"才是 3）
        assertEquals(4, rows.get(3).getRowType());
        assertAmt("400", rows.get(3).getDebitAmount());
        assertAmt("50", rows.get(3).getCreditAmount());
        assertAmt("1250", rows.get(3).getBalance());
    }

    @Test
    public void testGetGeneralList_reversedCodeRangeReturnsEmpty() {
        stubLedger(List.of(account(1000L, 0L, "1000", "货币资金", 10, 10, 1, false),
                        account(1001L, 1000L, "1001", "库存现金", 10, 10, 2, true)),
                List.of(), List.of(), List.of(initialRow(1001L, "1000", "0", "0")));
        ErpGlLedgerReqVO reqVO = req("2026-01", "2026-01", null);
        reqVO.setMinLevel(1);
        reqVO.setMaxLevel(8);
        reqVO.setStartSubjectId(1001L);
        reqVO.setEndSubjectId(1000L);

        assertTrue(service.getGeneralList(reqVO).isEmpty());
        assertTrue(service.getSubjectBalanceList(reqVO).isEmpty());
    }

    // ==================== 科目余额表 ====================

    @Test
    public void testGetSubjectBalanceList_onlyAccountsWithDataAndAncestors() {
        stubLedger(List.of(account(1000L, 0L, "1000", "货币资金", 10, 10, 1, false),
                        account(1001L, 1000L, "1001", "库存现金", 10, 10, 2, true),
                        account(2201L, 0L, "2201", "短期借款", 20, 20, 1, true),
                        account(6601L, 0L, "6601", "管理费用", 50, 10, 1, true)),
                List.of(), List.of(), List.of(initialRow(1001L, "500", "0", "0")));
        ErpGlLedgerReqVO reqVO = req("2026-01", "2026-01", null);
        reqVO.setMinLevel(1);
        reqVO.setMaxLevel(8);

        List<ErpGlLedgerSubjectBalanceRespVO> rows = service.getSubjectBalanceList(reqVO);

        // 2201/6601 无数据被剔除，仅保留有数据的 1001 与其祖先 1000
        assertEquals(1, rows.size());
        ErpGlLedgerSubjectBalanceRespVO root = rows.get(0);
        assertEquals(1000L, root.getSubjectId());
        assertEquals("row:1000", root.getNodeKey());
        assertEquals(1, root.getNodeType());
        assertEquals(1, root.getLevel());
        assertNotNull(root.getChildren());
        assertEquals(1, root.getChildren().size());
        ErpGlLedgerSubjectBalanceRespVO child = root.getChildren().get(0);
        assertEquals(1001L, child.getSubjectId());
        assertEquals(2, child.getLevel());
        assertAmt("500", child.getOpeningDebitAmount());
        assertAmt("0", child.getOpeningCreditAmount());
        assertAmt("500", child.getEndingDebitAmount());
        assertAmt("0", child.getEndingCreditAmount());
    }

    @Test
    public void testGetSubjectBalanceList_levelAndCodeRangeFilter() {
        stubLedger(List.of(account(1000L, 0L, "1000", "货币资金", 10, 10, 1, false),
                        account(1001L, 1000L, "1001", "库存现金", 10, 10, 2, true)),
                List.of(), List.of(), List.of(initialRow(1001L, "500", "0", "0")));
        ErpGlLedgerReqVO reqVO = req("2026-01", "2026-01", null);
        reqVO.setMinLevel(2);
        reqVO.setMaxLevel(8);
        reqVO.setStartSubjectId(1000L);
        reqVO.setEndSubjectId(1001L);

        List<ErpGlLedgerSubjectBalanceRespVO> rows = service.getSubjectBalanceList(reqVO);

        // 级次下限 2 剔除父级，编码闭区间 [1000, 1001] 保留子级 → 扁平单根
        assertEquals(1, rows.size());
        assertEquals(1001L, rows.get(0).getSubjectId());
        assertTrue(rows.get(0).getChildren() == null || rows.get(0).getChildren().isEmpty());
    }

    @Test
    public void testGetSubjectBalanceList_parentAggregatesChildOpeningWithoutSecondTransform() {
        stubLedger(List.of(account(1000L, 0L, "1000", "货币资金", 10, 10, 1, false),
                        account(1001L, 1000L, "1001", "库存现金", 10, 10, 2, true),
                        account(1002L, 1000L, "1002", "其他货币资金", 10, 20, 2, true)),
                List.of(), List.of(),
                List.of(initialRow(1001L, "100", "0", "0"),
                        initialRow(1002L, "200", "0", "0")));
        ErpGlLedgerReqVO reqVO = req("2026-01", "2026-01", null);
        reqVO.setMinLevel(1);
        reqVO.setMaxLevel(8);

        List<ErpGlLedgerSubjectBalanceRespVO> rows = service.getSubjectBalanceList(reqVO);

        assertEquals(1, rows.size());
        ErpGlLedgerSubjectBalanceRespVO root = rows.get(0);
        assertEquals(1000L, root.getSubjectId());
        assertAmt("0", root.getOpeningDebitAmount());
        assertAmt("100", root.getOpeningCreditAmount());
        assertAmt("100", root.getEndingCreditAmount());
        assertEquals(2, root.getChildren().size());
        assertEquals(1001L, root.getChildren().get(0).getSubjectId());
        assertAmt("100", root.getChildren().get(0).getOpeningDebitAmount());
        assertEquals(1002L, root.getChildren().get(1).getSubjectId());
        assertAmt("200", root.getChildren().get(1).getOpeningCreditAmount());
    }

    // ==================== 多栏账 ====================

    @Test
    public void testGetMultiColumn_columnsAndColumnAmounts() {
        stubLedger(List.of(account(6601L, 0L, "6601", "管理费用", 50, 10, 1, false),
                        account(660101L, 6601L, "660101", "办公费", 50, 10, 2, true),
                        account(660102L, 6601L, "660102", "差旅费", 50, 20, 2, true),
                        account(660103L, 6601L, "660103", "培训费", 50, 10, 2, true)),
                List.of(voucher(208L, LocalDate.of(2026, 4, 10), "记-0008")),
                List.of(entry(308L, 208L, 660101L, "660101", "办公费", "办公", "400", "0", 1),
                        entry(309L, 208L, 660102L, "660102", "差旅费", "差旅", "0", "150", 2)),
                List.of(initialRow(660101L, "200", "0", "0"),
                        initialRow(660102L, "300", "0", "0")));

        ErpGlLedgerMultiColumnRespVO result = service.getMultiColumn(req("2026-04", "2026-04", 6601L));

        assertEquals(3, result.getColumns().size());
        List<ErpGlLedgerColumnVO> columns = result.getColumns();
        assertEquals(660101L, columns.get(0).getSubjectId());
        assertEquals(10, columns.get(0).getBalanceDirection());
        assertEquals(660102L, columns.get(1).getSubjectId());
        assertEquals(20, columns.get(1).getBalanceDirection());
        assertEquals(660103L, columns.get(2).getSubjectId());

        List<ErpGlLedgerDetailRespVO> rows = result.getRows();
        assertEquals(5, rows.size());
        // 期初行：子树净额 -100 → 贷方 100；专栏按子科目自然方向折算
        ErpGlLedgerDetailRespVO opening = rows.get(0);
        assertEquals(1, opening.getRowType());
        assertEquals("贷", opening.getBalanceDirection());
        assertAmt("100", opening.getBalance());
        assertColumn(660101L, "200", opening.getColumnAmounts());
        assertColumn(660102L, "300", opening.getColumnAmounts());
        assertColumn(660103L, "0", opening.getColumnAmounts());
        // 明细行：各自只命中一个专栏
        ErpGlLedgerDetailRespVO first = rows.get(1);
        assertColumn(660101L, "400", first.getColumnAmounts());
        assertAmt("300", first.getBalance());
        ErpGlLedgerDetailRespVO second = rows.get(2);
        assertColumn(660102L, "150", second.getColumnAmounts());
        assertAmt("150", second.getBalance());
        // 本期合计行：专栏合计
        ErpGlLedgerDetailRespVO period = rows.get(3);
        assertEquals(3, period.getRowType());
        assertAmt("400", period.getDebitAmount());
        assertAmt("150", period.getCreditAmount());
        assertColumn(660101L, "400", period.getColumnAmounts());
        assertColumn(660102L, "150", period.getColumnAmounts());
        // 本年累计行不带专栏
        ErpGlLedgerDetailRespVO year = rows.get(4);
        assertEquals(4, year.getRowType());
        assertAmt("400", year.getDebitAmount());
        assertAmt("150", year.getCreditAmount());
        assertTrue(year.getColumnAmounts() == null);
    }

    @Test
    public void testGetMultiColumn_leafSubjectHasNoColumns() {
        stubLedger(List.of(account(1001L, 0L, "1001", "库存现金", 10, 10, 1, true)),
                List.of(), List.of(), List.of(initialRow(1001L, "100", "0", "0")));

        ErpGlLedgerMultiColumnRespVO result = service.getMultiColumn(req("2026-01", "2026-01", 1001L));

        assertTrue(result.getColumns().isEmpty());
        assertEquals(3, result.getRows().size());
        assertAmt("100", result.getRows().get(0).getBalance());
    }

    // ==================== 明细账科目树 ====================

    @Test
    public void testGetDetailSubjectList_activeAccountsWithAncestors() {
        stubLedger(List.of(account(1000L, 0L, "1000", "货币资金", 10, 10, 1, false),
                        account(1001L, 1000L, "1001", "库存现金", 10, 10, 2, true),
                        account(6601L, 0L, "6601", "管理费用", 50, 10, 1, true),
                        account(6602L, 0L, "6602", "财务费用", 50, 20, 1, true),
                        account(6603L, 0L, "6603", "销售费用", 50, 10, 1, true)),
                List.of(voucher(209L, LocalDate.of(2026, 5, 5), "记-0009")),
                List.of(entry(310L, 209L, 1001L, "1001", "库存现金", "收现款", "100", "0", 1)),
                List.of(initialRow(1001L, "0", "0", "0"),
                        initialRow(6602L, "50", "0", "0"),
                        initialRow(6603L, "0", "0", "0")));

        List<ErpGlLedgerSubjectRespVO> list = service.getDetailSubjectList(req("2026-05", "2026-05", null));

        // 期间内有发生额的 1001 + 祖先 1000；期初非零的 6602；期初全零的 6603 与无发生的 6601 剔除
        assertEquals(3, list.size());
        assertEquals(1000L, list.get(0).getId());
        assertEquals(1001L, list.get(1).getId());
        assertEquals(2, list.get(1).getLevel());
        assertEquals(6602L, list.get(2).getId());
    }

    // ==================== 空账套与导出 ====================

    @Test
    public void testAccountSetWithoutBooksReturnsEmptyResults() {
        when(accountBookMapper.selectListByAccountSetId(ACCOUNT_SET_ID)).thenReturn(List.of());

        ErpGlLedgerReqVO reqVO = req("2026-01", "2026-01", 1001L);
        assertTrue(service.getDetailList(reqVO).isEmpty());
        assertTrue(service.getGeneralList(reqVO).isEmpty());
        assertTrue(service.getSubjectBalanceList(reqVO).isEmpty());
        assertTrue(service.getDetailSubjectList(reqVO).isEmpty());
        ErpGlLedgerMultiColumnRespVO multiColumn = service.getMultiColumn(reqVO);
        assertTrue(multiColumn.getColumns().isEmpty());
        assertTrue(multiColumn.getRows().isEmpty());
        assertTrue(service.getDetailExcelList(reqVO).isEmpty());
        assertTrue(service.getGeneralExcelList(reqVO).isEmpty());
        assertTrue(service.getSubjectBalanceExcelList(reqVO).isEmpty());
        assertTrue(service.getMultiColumnExcelData(reqVO).getRows().isEmpty());
    }

    @Test
    public void testExportLists_matchLedgerRows() {
        stubLedger(List.of(account(1000L, 0L, "1000", "货币资金", 10, 10, 1, false),
                        account(1001L, 1000L, "1001", "库存现金", 10, 10, 2, true)),
                List.of(voucher(210L, LocalDate.of(2026, 1, 15), "记-0010")),
                List.of(entry(311L, 210L, 1001L, "1001", "库存现金", "收现款", "500", "0", 1)),
                List.of(initialRow(1001L, "1000", "0", "0")));

        List<ErpGlLedgerDetailExcelVO> detail = service.getDetailExcelList(req("2026-01", "2026-01", 1001L));
        assertEquals(4, detail.size());
        assertEquals("", detail.get(0).getAccountDate());
        assertEquals("期初余额", detail.get(0).getDigest());
        assertEquals("2026-01-15", detail.get(1).getAccountDate());
        assertEquals("记-0010", detail.get(1).getVoucherNumber());
        assertEquals("收现款", detail.get(1).getDigest());
        assertAmt("500", detail.get(1).getDebitAmount());
        assertAmt("1500", detail.get(1).getBalance());

        ErpGlLedgerReqVO levelReq = req("2026-01", "2026-01", null);
        levelReq.setMinLevel(1);
        levelReq.setMaxLevel(1);
        List<ErpGlLedgerGeneralExcelVO> general = service.getGeneralExcelList(levelReq);
        // 默认域 = 级次 1 → 仅 1000 有数据 → 期初 + 2026-01 + 本年累计
        assertEquals(3, general.size());
        assertEquals("1000", general.get(0).getSubjectCode());
        assertEquals("期初余额", general.get(0).getDigest());

        List<ErpGlLedgerSubjectBalanceExcelVO> balance = service.getSubjectBalanceExcelList(levelReq);
        assertEquals(1, balance.size());
        assertEquals("1000", balance.get(0).getSubjectCode());
        assertEquals(1, balance.get(0).getLevel());
        assertAmt("1000", balance.get(0).getOpeningDebitAmount());

        ErpGlLedgerMultiColumnExcelDataVO multiColumn =
                service.getMultiColumnExcelData(req("2026-01", "2026-01", 1000L));
        // 动态表头 = 固定 7 列 + 直接子科目列（1000 的直接子科目 1001 → 共 8 列）
        assertEquals(8, multiColumn.getHead().size());
        assertEquals(List.of("日期"), multiColumn.getHead().get(0));
        assertEquals(List.of("余额"), multiColumn.getHead().get(6));
        assertEquals(List.of("1001/库存现金"), multiColumn.getHead().get(7));
        assertEquals(8, multiColumn.getRows().get(0).size());
        assertEquals(4, multiColumn.getRows().size());
        assertEquals("2026-01-15", multiColumn.getRows().get(1).get(0));
    }

    // ==================== 夹具 ====================

    private void stubLedger(List<ErpGlAccountDO> accounts, List<ErpGlVoucherDO> vouchers,
                            List<ErpGlVoucherEntryDO> entries, List<ErpGlInitialBalanceDO> initialRows) {
        ErpAccountBookDO book = new ErpAccountBookDO();
        book.setId(BOOK_ID);
        book.setAccountSetId(ACCOUNT_SET_ID);
        when(accountBookMapper.selectListByAccountSetId(ACCOUNT_SET_ID)).thenReturn(List.of(book));
        when(glAccountMapper.selectList(null)).thenReturn(new ArrayList<>(accounts));
        when(glVoucherMapper.selectList(any())).thenReturn(new ArrayList<>(vouchers));
        if (!vouchers.isEmpty()) {
            when(glVoucherEntryMapper.selectListByVoucherIds(anyList())).thenReturn(new ArrayList<>(entries));
        }
        when(initialBalanceMapper.selectListByAccountSetId(ACCOUNT_SET_ID))
                .thenReturn(new ArrayList<>(initialRows));
    }

    private static ErpGlLedgerReqVO req(String startMonth, String endMonth, Long subjectId) {
        ErpGlLedgerReqVO reqVO = new ErpGlLedgerReqVO();
        reqVO.setAccountSetId(ACCOUNT_SET_ID);
        reqVO.setStartMonth(startMonth);
        reqVO.setEndMonth(endMonth);
        reqVO.setSubjectId(subjectId);
        return reqVO;
    }

    private static ErpGlAccountDO account(Long id, Long parentId, String code, String name,
                                          Integer type, Integer balanceDirection, Integer level, Boolean isLeaf) {
        ErpGlAccountDO account = new ErpGlAccountDO();
        account.setId(id);
        account.setParentId(parentId);
        account.setCode(code);
        account.setName(name);
        account.setType(type);
        account.setBalanceDirection(balanceDirection);
        account.setLevel(level);
        account.setIsLeaf(isLeaf);
        account.setStatus(0);
        account.setSort(0);
        return account;
    }

    private static ErpGlVoucherDO voucher(Long id, LocalDate voucherDate, String voucherNo) {
        ErpGlVoucherDO voucher = new ErpGlVoucherDO();
        voucher.setId(id);
        voucher.setAccountBookId(BOOK_ID);
        voucher.setVoucherDate(voucherDate);
        voucher.setVoucherNo(voucherNo);
        voucher.setStatus(20);
        return voucher;
    }

    private static ErpGlVoucherEntryDO entry(Long id, Long voucherId, Long accountId, String accountCode,
                                             String accountName, String summary, String debit, String credit,
                                             Integer sort) {
        ErpGlVoucherEntryDO entry = new ErpGlVoucherEntryDO();
        entry.setId(id);
        entry.setVoucherId(voucherId);
        entry.setAccountId(accountId);
        entry.setAccountCode(accountCode);
        entry.setAccountName(accountName);
        entry.setSummary(summary);
        entry.setDebitAmount(new BigDecimal(debit));
        entry.setCreditAmount(new BigDecimal(credit));
        entry.setSort(sort);
        return entry;
    }

    private static ErpGlInitialBalanceDO initialRow(Long accountId, String opening, String yearDebit,
                                                    String yearCredit) {
        ErpGlInitialBalanceDO row = new ErpGlInitialBalanceDO();
        row.setAccountSetId(ACCOUNT_SET_ID);
        row.setAccountId(accountId);
        row.setOpeningAmount(new BigDecimal(opening));
        row.setYearDebitAmount(new BigDecimal(yearDebit));
        row.setYearCreditAmount(new BigDecimal(yearCredit));
        row.setProfitLossAmount(BigDecimal.ZERO);
        return row;
    }

    private static void assertAmt(String expected, BigDecimal actual) {
        assertNotNull(actual, "金额为 null");
        assertEquals(0, new BigDecimal(expected).compareTo(actual),
                "金额不符：期望 " + expected + "，实际 " + actual.toPlainString());
    }

    private static void assertColumn(Long subjectId, String expected, Map<Long, BigDecimal> columnAmounts) {
        assertNotNull(columnAmounts, "专栏金额为 null（科目 " + subjectId + "）");
        assertAmt(expected, columnAmounts.get(subjectId));
    }

}
