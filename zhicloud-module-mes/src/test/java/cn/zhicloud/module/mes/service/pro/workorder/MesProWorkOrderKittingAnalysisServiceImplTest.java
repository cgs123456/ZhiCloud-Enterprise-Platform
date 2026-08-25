package cn.zhicloud.module.mes.service.pro.workorder;

import cn.zhicloud.framework.test.core.ut.BaseDbUnitTest;
import cn.zhicloud.module.mes.controller.admin.pro.workorder.vo.kitting.MesProWorkOrderKittingLineRespVO;
import cn.zhicloud.module.mes.controller.admin.pro.workorder.vo.kitting.MesProWorkOrderKittingSummaryRespVO;
import cn.zhicloud.module.mes.dal.dataobject.md.item.MesMdItemDO;
import cn.zhicloud.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderBomDO;
import cn.zhicloud.module.mes.dal.dataobject.pro.workorder.MesProWorkOrderDO;
import cn.zhicloud.module.mes.dal.dataobject.wm.itemreceipt.MesWmItemReceiptDO;
import cn.zhicloud.module.mes.dal.dataobject.wm.itemreceipt.MesWmItemReceiptLineDO;
import cn.zhicloud.module.mes.dal.dataobject.wm.materialstock.MesWmMaterialStockDO;
import cn.zhicloud.module.mes.dal.mysql.md.item.MesMdItemMapper;
import cn.zhicloud.module.mes.dal.mysql.pro.workorder.MesProWorkOrderBomMapper;
import cn.zhicloud.module.mes.dal.mysql.pro.workorder.MesProWorkOrderMapper;
import cn.zhicloud.module.mes.dal.mysql.wm.itemreceipt.MesWmItemReceiptLineMapper;
import cn.zhicloud.module.mes.dal.mysql.wm.itemreceipt.MesWmItemReceiptMapper;
import cn.zhicloud.module.mes.dal.mysql.wm.materialstock.MesWmMaterialStockMapper;
import cn.zhicloud.module.mes.enums.MesOrderStatusConstants;
import cn.zhicloud.module.mes.enums.pro.MesProWorkOrderKittingStatusEnum;
import cn.zhicloud.module.mes.enums.pro.MesProWorkOrderStatusEnum;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.baomidou.mybatisplus.core.conditions.Wrapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static cn.zhicloud.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.zhicloud.module.mes.enums.ErrorCodeConstants.PRO_KITTING_ANALYSIS_WORK_ORDER_BOM_EMPTY;
import static cn.zhicloud.module.mes.enums.ErrorCodeConstants.PRO_WORK_ORDER_NOT_EXISTS;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * {@link MesProWorkOrderKittingAnalysisServiceImpl} 的单元测试（mock mapper，无需 H2 表）
 */
@Import(MesProWorkOrderKittingAnalysisServiceImpl.class)
public class MesProWorkOrderKittingAnalysisServiceImplTest extends BaseDbUnitTest {

    private static final Long WORK_ORDER_ID = 1L;
    private static final Long PRODUCT_ID = 200L;
    private static final Long ITEM_ID = 300L;
    private static final LocalDateTime REQUEST_DATE = LocalDateTime.of(2024, 1, 1, 10, 0, 0);

    @MockitoBean
    private MesProWorkOrderMapper workOrderMapper;
    @MockitoBean
    private MesProWorkOrderBomMapper workOrderBomMapper;
    @MockitoBean
    private MesWmMaterialStockMapper materialStockMapper;
    @MockitoBean
    private MesWmItemReceiptMapper itemReceiptMapper;
    @MockitoBean
    private MesWmItemReceiptLineMapper itemReceiptLineMapper;
    @MockitoBean
    private MesMdItemMapper itemMapper;

    @Resource
    private MesProWorkOrderKittingAnalysisServiceImpl kittingAnalysisService;

    @BeforeEach
    public void setUp() {
        when(workOrderMapper.selectById(WORK_ORDER_ID)).thenReturn(buildWorkOrder(new BigDecimal("10")));
        when(itemMapper.selectById(PRODUCT_ID)).thenReturn(buildItem(PRODUCT_ID, "P-001", "产品A"));
    }

    // ========== 构造方法 ==========

    private MesProWorkOrderDO buildWorkOrder(BigDecimal quantity) {
        return new MesProWorkOrderDO().setId(WORK_ORDER_ID).setCode("WO-001").setName("工单A")
                .setProductId(PRODUCT_ID).setQuantity(quantity).setRequestDate(REQUEST_DATE)
                .setStatus(MesProWorkOrderStatusEnum.CONFIRMED.getStatus());
    }

    private MesProWorkOrderBomDO buildBom(Long id, Long itemId, BigDecimal quantity) {
        return new MesProWorkOrderBomDO().setId(id).setWorkOrderId(WORK_ORDER_ID)
                .setItemId(itemId).setQuantity(quantity).setRemark("备注" + id);
    }

    private MesMdItemDO buildItem(Long id, String code, String name) {
        return new MesMdItemDO().setId(id).setCode(code).setName(name)
                .setSpecification("规格-" + code).setUnitMeasureId(9L);
    }

    private MesWmMaterialStockDO buildStock(Long itemId, BigDecimal quantity) {
        return new MesWmMaterialStockDO().setId(itemId).setItemId(itemId)
                .setQuantity(quantity).setFrozen(false);
    }

    private MesWmItemReceiptLineDO buildReceiptLine(Long itemId, BigDecimal quantity) {
        return new MesWmItemReceiptLineDO().setId(itemId).setReceiptId(500L)
                .setItemId(itemId).setReceivedQuantity(quantity);
    }

    // ========== mock 辅助方法 ==========

    private void mockBoms(MesProWorkOrderBomDO... boms) {
        when(workOrderBomMapper.selectListByWorkOrderId(WORK_ORDER_ID)).thenReturn(Arrays.asList(boms));
    }

    private void mockItems(MesMdItemDO... items) {
        when(itemMapper.selectByIds(any())).thenReturn(Arrays.asList(items));
    }

    private void mockStocks(MesWmMaterialStockDO... stocks) {
        when(materialStockMapper.selectList(any(Wrapper.class))).thenReturn(Arrays.asList(stocks));
    }

    /**
     * mock 在途：存在一张待入库单，行数据为入参
     */
    private void mockInTransit(MesWmItemReceiptLineDO... lines) {
        when(itemReceiptMapper.selectList(any())).thenReturn(Collections.singletonList(
                new MesWmItemReceiptDO().setId(500L).setCode("RC-001")
                        .setStatus(MesOrderStatusConstants.APPROVED)));
        when(itemReceiptLineMapper.selectList(any())).thenReturn(Arrays.asList(lines));
    }

    // ========== 异常分支 ==========

    @Test
    public void testAnalyzeKitting_workOrderNotExists() {
        when(workOrderMapper.selectById(WORK_ORDER_ID)).thenReturn(null);
        assertServiceException(() -> kittingAnalysisService.analyzeKitting(WORK_ORDER_ID), PRO_WORK_ORDER_NOT_EXISTS);
    }

    @Test
    public void testAnalyzeKitting_bomEmpty() {
        when(workOrderBomMapper.selectListByWorkOrderId(WORK_ORDER_ID)).thenReturn(Collections.emptyList());
        assertServiceException(() -> kittingAnalysisService.analyzeKitting(WORK_ORDER_ID),
                PRO_KITTING_ANALYSIS_WORK_ORDER_BOM_EMPTY);
    }

    // ========== 齐套状态 ==========

    @Test
    public void testAnalyzeKitting_fullyKitted() {
        mockBoms(buildBom(1L, ITEM_ID, new BigDecimal("2")));
        mockItems(buildItem(ITEM_ID, "M-001", "物料A"));
        mockStocks(buildStock(ITEM_ID, new BigDecimal("50")));

        MesProWorkOrderKittingSummaryRespVO summary = kittingAnalysisService.analyzeKitting(WORK_ORDER_ID);

        assertEquals(1, summary.getTotalLineCount());
        assertEquals(1, summary.getFullyKittedCount());
        assertEquals(0, summary.getPartialCount());
        assertEquals(0, summary.getShortageCount());
        assertTrue(summary.getFullyKitted());
        assertEquals(0, summary.getKittingRate().compareTo(new BigDecimal("100.00")));

        MesProWorkOrderKittingLineRespVO line = summary.getLines().get(0);
        assertEquals(MesProWorkOrderKittingStatusEnum.FULLY_KITTED.getStatus(), line.getKittingStatus());
        assertEquals(MesProWorkOrderKittingStatusEnum.FULLY_KITTED.getName(), line.getKittingStatusName());
        assertEquals("M-001", line.getItemCode());
        assertEquals("物料A", line.getItemName());
        assertEquals("规格-M-001", line.getSpecification());
        assertEquals(9L, line.getUnitMeasureId());
        assertEquals(0, line.getRequiredQuantity().compareTo(new BigDecimal("20")));
        assertEquals(0, line.getStockQuantity().compareTo(new BigDecimal("50")));
        assertEquals(0, line.getInTransitQuantity().compareTo(BigDecimal.ZERO));
        assertEquals(0, line.getAvailableQuantity().compareTo(new BigDecimal("50")));
        assertEquals(0, line.getShortageQuantity().compareTo(BigDecimal.ZERO));
        assertEquals("备注1", line.getRemark());
    }

    @Test
    public void testAnalyzeKitting_partial() {
        mockBoms(buildBom(1L, ITEM_ID, new BigDecimal("2")));
        mockItems(buildItem(ITEM_ID, "M-001", "物料A"));
        mockStocks(buildStock(ITEM_ID, new BigDecimal("5")));
        mockInTransit(buildReceiptLine(ITEM_ID, new BigDecimal("20")));

        MesProWorkOrderKittingSummaryRespVO summary = kittingAnalysisService.analyzeKitting(WORK_ORDER_ID);

        assertEquals(1, summary.getPartialCount());
        assertEquals(0, summary.getShortageCount());
        assertTrue(summary.getFullyKitted());
        MesProWorkOrderKittingLineRespVO line = summary.getLines().get(0);
        assertEquals(MesProWorkOrderKittingStatusEnum.PARTIAL.getStatus(), line.getKittingStatus());
        assertEquals(0, line.getInTransitQuantity().compareTo(new BigDecimal("20")));
        assertEquals(0, line.getAvailableQuantity().compareTo(new BigDecimal("25")));
        assertEquals(0, line.getShortageQuantity().compareTo(BigDecimal.ZERO));
    }

    @Test
    public void testAnalyzeKitting_shortage() {
        mockBoms(buildBom(1L, ITEM_ID, new BigDecimal("2")));
        mockItems(buildItem(ITEM_ID, "M-001", "物料A"));
        mockStocks(buildStock(ITEM_ID, new BigDecimal("5")));

        MesProWorkOrderKittingSummaryRespVO summary = kittingAnalysisService.analyzeKitting(WORK_ORDER_ID);

        assertEquals(1, summary.getShortageCount());
        assertFalse(summary.getFullyKitted());
        assertEquals(0, summary.getKittingRate().compareTo(new BigDecimal("0.00")));
        MesProWorkOrderKittingLineRespVO line = summary.getLines().get(0);
        assertEquals(MesProWorkOrderKittingStatusEnum.SHORTAGE.getStatus(), line.getKittingStatus());
        assertEquals(0, line.getShortageQuantity().compareTo(new BigDecimal("15")));
    }

    @Test
    public void testAnalyzeKitting_stockEqualsRequired() {
        // 边界：库存刚好等于需求量 → 齐套
        mockBoms(buildBom(1L, ITEM_ID, new BigDecimal("2")));
        mockStocks(buildStock(ITEM_ID, new BigDecimal("20")));

        MesProWorkOrderKittingSummaryRespVO summary = kittingAnalysisService.analyzeKitting(WORK_ORDER_ID);

        assertEquals(MesProWorkOrderKittingStatusEnum.FULLY_KITTED.getStatus(),
                summary.getLines().get(0).getKittingStatus());
    }

    @Test
    public void testAnalyzeKitting_availableEqualsRequired() {
        // 边界：库存 + 在途刚好等于需求量 → 部分齐套
        mockBoms(buildBom(1L, ITEM_ID, new BigDecimal("2")));
        mockStocks(buildStock(ITEM_ID, new BigDecimal("15")));
        mockInTransit(buildReceiptLine(ITEM_ID, new BigDecimal("5")));

        MesProWorkOrderKittingSummaryRespVO summary = kittingAnalysisService.analyzeKitting(WORK_ORDER_ID);

        assertEquals(MesProWorkOrderKittingStatusEnum.PARTIAL.getStatus(),
                summary.getLines().get(0).getKittingStatus());
    }

    @Test
    public void testAnalyzeKitting_mixedStatus() {
        Long itemB = 301L;
        Long itemC = 302L;
        mockBoms(buildBom(1L, ITEM_ID, new BigDecimal("1")),
                buildBom(2L, itemB, new BigDecimal("1")),
                buildBom(3L, itemC, new BigDecimal("1")));
        mockItems(buildItem(ITEM_ID, "M-001", "物料A"),
                buildItem(itemB, "M-002", "物料B"),
                buildItem(itemC, "M-003", "物料C"));
        // A：库存充足；B：库存不足但在途可补；C：全部不足
        mockStocks(buildStock(ITEM_ID, new BigDecimal("100")),
                buildStock(itemB, new BigDecimal("1")),
                buildStock(itemC, new BigDecimal("1")));
        mockInTransit(buildReceiptLine(itemB, new BigDecimal("99")));

        MesProWorkOrderKittingSummaryRespVO summary = kittingAnalysisService.analyzeKitting(WORK_ORDER_ID);

        assertEquals(3, summary.getTotalLineCount());
        assertEquals(1, summary.getFullyKittedCount());
        assertEquals(1, summary.getPartialCount());
        assertEquals(1, summary.getShortageCount());
        assertFalse(summary.getFullyKitted());
        // 齐套率 = (1 + 1) / 3 * 100 = 66.67
        assertEquals(0, summary.getKittingRate().compareTo(new BigDecimal("66.67")));
    }

    // ========== 汇总信息 ==========

    @Test
    public void testAnalyzeKitting_summaryBaseInfo() {
        mockBoms(buildBom(1L, ITEM_ID, new BigDecimal("1")));

        MesProWorkOrderKittingSummaryRespVO summary = kittingAnalysisService.analyzeKitting(WORK_ORDER_ID);

        assertEquals(WORK_ORDER_ID, summary.getWorkOrderId());
        assertEquals("WO-001", summary.getWorkOrderCode());
        assertEquals("工单A", summary.getWorkOrderName());
        assertEquals(PRODUCT_ID, summary.getProductId());
        assertEquals("P-001", summary.getProductCode());
        assertEquals("产品A", summary.getProductName());
        assertEquals(MesProWorkOrderStatusEnum.CONFIRMED.getStatus(), summary.getWorkOrderStatus());
        assertEquals(REQUEST_DATE, summary.getRequestDate());
        assertEquals(0, summary.getWorkOrderQuantity().compareTo(new BigDecimal("10")));
        assertEquals(1, summary.getLines().size());
    }

    @Test
    public void testAnalyzeKitting_productItemNotExists() {
        when(itemMapper.selectById(PRODUCT_ID)).thenReturn(null);
        mockBoms(buildBom(1L, ITEM_ID, new BigDecimal("1")));

        MesProWorkOrderKittingSummaryRespVO summary = kittingAnalysisService.analyzeKitting(WORK_ORDER_ID);

        assertNull(summary.getProductCode());
        assertNull(summary.getProductName());
    }

    @Test
    public void testAnalyzeKitting_itemInfoNotExists() {
        // 物料主数据查不到 → 行上的物料信息为空，但数量仍然计算
        mockBoms(buildBom(1L, ITEM_ID, new BigDecimal("3")));
        mockItems();

        MesProWorkOrderKittingSummaryRespVO summary = kittingAnalysisService.analyzeKitting(WORK_ORDER_ID);

        MesProWorkOrderKittingLineRespVO line = summary.getLines().get(0);
        assertNull(line.getItemCode());
        assertNull(line.getItemName());
        assertEquals(ITEM_ID, line.getItemId());
        assertEquals(WORK_ORDER_ID, line.getWorkOrderId());
        assertEquals(0, line.getRequiredQuantity().compareTo(new BigDecimal("30")));
    }

    // ========== 空值 / 边界 ==========

    @Test
    public void testAnalyzeKitting_nullQuantity() {
        // 工单数量与 BOM 用量均为空 → 需求量为 0，视为齐套
        when(workOrderMapper.selectById(WORK_ORDER_ID)).thenReturn(buildWorkOrder(null));
        mockBoms(buildBom(1L, ITEM_ID, null));

        MesProWorkOrderKittingSummaryRespVO summary = kittingAnalysisService.analyzeKitting(WORK_ORDER_ID);

        MesProWorkOrderKittingLineRespVO line = summary.getLines().get(0);
        assertEquals(0, line.getBomQuantity().compareTo(BigDecimal.ZERO));
        assertEquals(0, line.getWorkOrderQuantity().compareTo(BigDecimal.ZERO));
        assertEquals(0, line.getRequiredQuantity().compareTo(BigDecimal.ZERO));
        assertEquals(MesProWorkOrderKittingStatusEnum.FULLY_KITTED.getStatus(), line.getKittingStatus());
        assertEquals(0, summary.getKittingRate().compareTo(new BigDecimal("100.00")));
    }

    @Test
    public void testAnalyzeKitting_bomItemIdNull() {
        // BOM 行物料为空 → 不查询物料/库存/在途
        mockBoms(buildBom(1L, null, new BigDecimal("2")));

        MesProWorkOrderKittingSummaryRespVO summary = kittingAnalysisService.analyzeKitting(WORK_ORDER_ID);

        assertEquals(1, summary.getTotalLineCount());
        MesProWorkOrderKittingLineRespVO line = summary.getLines().get(0);
        assertNull(line.getItemId());
        assertEquals(0, line.getStockQuantity().compareTo(BigDecimal.ZERO));
        assertEquals(0, line.getShortageQuantity().compareTo(new BigDecimal("20")));
        verify(itemMapper, never()).selectByIds(any());
        verify(materialStockMapper, never()).selectList(any(Wrapper.class));
        verify(itemReceiptMapper, never()).selectList(any());
    }

    @Test
    public void testAnalyzeKitting_stockQuantityNullAndMerge() {
        // 同一物料多条库存记录：null 视为 0，其余累加
        mockBoms(buildBom(1L, ITEM_ID, new BigDecimal("1")));
        mockStocks(buildStock(ITEM_ID, null), buildStock(ITEM_ID, new BigDecimal("4")),
                buildStock(ITEM_ID, new BigDecimal("6")));

        MesProWorkOrderKittingSummaryRespVO summary = kittingAnalysisService.analyzeKitting(WORK_ORDER_ID);

        MesProWorkOrderKittingLineRespVO line = summary.getLines().get(0);
        assertEquals(0, line.getStockQuantity().compareTo(new BigDecimal("10")));
        assertEquals(MesProWorkOrderKittingStatusEnum.FULLY_KITTED.getStatus(), line.getKittingStatus());
    }

    @Test
    public void testAnalyzeKitting_inTransitQuantityNullAndMerge() {
        // 同一物料多条在途行：null 视为 0，其余累加
        mockBoms(buildBom(1L, ITEM_ID, new BigDecimal("1")));
        mockInTransit(buildReceiptLine(ITEM_ID, null), buildReceiptLine(ITEM_ID, new BigDecimal("3")),
                buildReceiptLine(ITEM_ID, new BigDecimal("7")));

        MesProWorkOrderKittingSummaryRespVO summary = kittingAnalysisService.analyzeKitting(WORK_ORDER_ID);

        MesProWorkOrderKittingLineRespVO line = summary.getLines().get(0);
        assertEquals(0, line.getInTransitQuantity().compareTo(new BigDecimal("10")));
        assertEquals(MesProWorkOrderKittingStatusEnum.PARTIAL.getStatus(), line.getKittingStatus());
    }

    @Test
    public void testAnalyzeKitting_receiptsEmpty() {
        // 无待入库单 → 不查询入库单行
        mockBoms(buildBom(1L, ITEM_ID, new BigDecimal("1")));
        when(itemReceiptMapper.selectList(any())).thenReturn(Collections.emptyList());

        MesProWorkOrderKittingSummaryRespVO summary = kittingAnalysisService.analyzeKitting(WORK_ORDER_ID);

        assertEquals(0, summary.getLines().get(0).getInTransitQuantity().compareTo(BigDecimal.ZERO));
        verify(itemReceiptLineMapper, never()).selectList(any());
    }

    @Test
    public void testAnalyzeKitting_noStockAndNoInTransit() {
        mockBoms(buildBom(1L, ITEM_ID, new BigDecimal("1")));

        MesProWorkOrderKittingSummaryRespVO summary = kittingAnalysisService.analyzeKitting(WORK_ORDER_ID);

        MesProWorkOrderKittingLineRespVO line = summary.getLines().get(0);
        assertEquals(0, line.getStockQuantity().compareTo(BigDecimal.ZERO));
        assertEquals(0, line.getInTransitQuantity().compareTo(BigDecimal.ZERO));
        assertEquals(MesProWorkOrderKittingStatusEnum.SHORTAGE.getStatus(), line.getKittingStatus());
        assertEquals(0, line.getShortageQuantity().compareTo(new BigDecimal("10")));
        assertFalse(summary.getFullyKitted());
    }

}
