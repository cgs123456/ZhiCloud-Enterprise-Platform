package cn.iocoder.yudao.module.erp.service;

import cn.iocoder.yudao.module.erp.dal.dataobject.finance.cost.ErpActualCostDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.cost.ErpCostItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.cost.ErpCostVarianceDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.cost.ErpStandardCostDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.cost.ErpWorkOrderCostDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.cost.ErpActualCostMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.cost.ErpCostItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.cost.ErpCostVarianceMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.cost.ErpStandardCostMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.cost.ErpWorkOrderCostMapper;
import cn.iocoder.yudao.module.erp.enums.finance.cost.ErpCostItemTypeEnum;
import cn.iocoder.yudao.module.erp.enums.finance.cost.ErpStandardCostStatusEnum;
import cn.iocoder.yudao.module.erp.enums.finance.cost.ErpVarianceTypeEnum;
import cn.iocoder.yudao.module.erp.service.finance.cost.ErpCostCalculationServiceImpl;
import cn.iocoder.yudao.module.erp.service.finance.cost.bom.ErpBomComponent;
import cn.iocoder.yudao.module.erp.service.finance.cost.bom.ErpBomProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * {@link ErpCostCalculationServiceImpl} 的单元测试
 *
 * <p>覆盖成本卷积算法（标准成本汇总、BOM 递归卷积、差异分析）。
 *
 * @author 芋道源码
 */
@ExtendWith(MockitoExtension.class)
public class ErpCostCalculationServiceImplTest {

    @Mock
    private ErpStandardCostMapper standardCostMapper;
    @Mock
    private ErpActualCostMapper actualCostMapper;
    @Mock
    private ErpCostVarianceMapper costVarianceMapper;
    @Mock
    private ErpWorkOrderCostMapper workOrderCostMapper;
    @Mock
    private ErpCostItemMapper costItemMapper;
    @Mock
    private ErpBomProvider bomProvider;

    @InjectMocks
    private ErpCostCalculationServiceImpl costCalculationService;

    private static final Long PRODUCT_ID = 1001L;
    private static final String COST_PERIOD = "202607";
    private static final Long COST_ITEM_MATERIAL = 1L;
    private static final Long COST_ITEM_LABOR = 2L;

    @BeforeEach
    void setUp() {
        // 默认返回空 BOM，退化为仅汇总本层标准成本
        lenient().when(bomProvider.getBomComponents(anyLong())).thenReturn(Collections.emptyList());
    }

    // ==================== 标准成本卷积 ====================

    @Test
    public void testCalculateStandardCost_noBom_returnsBaseCost() {
        // 准备：产品有一条已生效的标准成本记录
        ErpStandardCostDO sc = ErpStandardCostDO.builder()
                .id(1L).productId(PRODUCT_ID).costItemId(COST_ITEM_MATERIAL)
                .standardCost(new BigDecimal("100.00"))
                .status(ErpStandardCostStatusEnum.EFFECTIVE.getStatus())
                .build();
        when(standardCostMapper.selectEffectiveListByProduct(eq(PRODUCT_ID), any(LocalDate.class)))
                .thenReturn(Collections.singletonList(sc));

        // 调用
        List<BigDecimal> result = costCalculationService.calculateStandardCostByConvolution(PRODUCT_ID, COST_PERIOD);

        // 断言：无 BOM 时，卷积结果 = 本层标准成本
        assertEquals(1, result.size());
        assertEquals(0, new BigDecimal("100.00").compareTo(result.get(0)));
        // 验证状态被回写为已生效
        assertEquals(ErpStandardCostStatusEnum.EFFECTIVE.getStatus(), sc.getStatus());
        verify(standardCostMapper).updateById(sc);
    }

    @Test
    public void testCalculateStandardCost_withBom_recursiveConvolution() {
        // 准备：成品 P1 标准成本 100，BOM 子件 P2（用量 2）+ P3（用量 1）
        // P2 标准成本 30，P3 标准成本 20
        // 卷积结果 = 100 + 30*2 + 20*1 = 180
        ErpStandardCostDO sc1 = ErpStandardCostDO.builder()
                .id(1L).productId(PRODUCT_ID).costItemId(COST_ITEM_MATERIAL)
                .standardCost(new BigDecimal("100.00"))
                .status(ErpStandardCostStatusEnum.EFFECTIVE.getStatus())
                .build();
        when(standardCostMapper.selectEffectiveListByProduct(eq(PRODUCT_ID), any(LocalDate.class)))
                .thenReturn(Collections.singletonList(sc1));
        // 递归卷积的第一层就是成品自身，需桩出 P1 的本层标准成本，否则严格桩会因参数不匹配抛
        // PotentialStubbingProblem（calculateConvolutedCostRecursive 会先查询根节点的 base cost）
        when(standardCostMapper.selectByProductAndCostItem(eq(PRODUCT_ID), eq(COST_ITEM_MATERIAL), any(LocalDate.class)))
                .thenReturn(sc1);
        // BOM: P1 -> [P2(2), P3(1)]
        when(bomProvider.getBomComponents(PRODUCT_ID)).thenReturn(Arrays.asList(
                new ErpBomComponent(2001L, new BigDecimal("2")),
                new ErpBomComponent(3001L, new BigDecimal("1"))
        ));
        // P2 的标准成本
        ErpStandardCostDO sc2 = ErpStandardCostDO.builder()
                .id(2L).productId(2001L).costItemId(COST_ITEM_MATERIAL)
                .standardCost(new BigDecimal("30.00")).build();
        when(standardCostMapper.selectByProductAndCostItem(eq(2001L), eq(COST_ITEM_MATERIAL), any(LocalDate.class)))
                .thenReturn(sc2);
        // P3 的标准成本
        ErpStandardCostDO sc3 = ErpStandardCostDO.builder()
                .id(3L).productId(3001L).costItemId(COST_ITEM_MATERIAL)
                .standardCost(new BigDecimal("20.00")).build();
        when(standardCostMapper.selectByProductAndCostItem(eq(3001L), eq(COST_ITEM_MATERIAL), any(LocalDate.class)))
                .thenReturn(sc3);

        // 调用
        List<BigDecimal> result = costCalculationService.calculateStandardCostByConvolution(PRODUCT_ID, COST_PERIOD);

        // 断言：卷积结果 = 100 + 30*2 + 20*1 = 180
        assertEquals(1, result.size());
        assertEquals(0, new BigDecimal("180.00").compareTo(result.get(0)));
    }

    @Test
    public void testCalculateStandardCost_circularDependency_handled() {
        // 准备：P1 -> P2 -> P1（循环依赖），应被 visited Set 检测并跳过
        ErpStandardCostDO sc1 = ErpStandardCostDO.builder()
                .id(1L).productId(PRODUCT_ID).costItemId(COST_ITEM_MATERIAL)
                .standardCost(new BigDecimal("100.00"))
                .status(ErpStandardCostStatusEnum.EFFECTIVE.getStatus())
                .build();
        when(standardCostMapper.selectEffectiveListByProduct(eq(PRODUCT_ID), any(LocalDate.class)))
                .thenReturn(Collections.singletonList(sc1));
        // BOM: P1 -> P2(1), P2 -> P1(1) 形成环
        when(bomProvider.getBomComponents(PRODUCT_ID)).thenReturn(
                Collections.singletonList(new ErpBomComponent(2001L, new BigDecimal("1"))));
        when(bomProvider.getBomComponents(2001L)).thenReturn(
                Collections.singletonList(new ErpBomComponent(PRODUCT_ID, new BigDecimal("1"))));
        ErpStandardCostDO sc2 = ErpStandardCostDO.builder()
                .id(2L).productId(2001L).costItemId(COST_ITEM_MATERIAL)
                .standardCost(new BigDecimal("50.00")).build();
        when(standardCostMapper.selectByProductAndCostItem(eq(2001L), eq(COST_ITEM_MATERIAL), any(LocalDate.class)))
                .thenReturn(sc2);
        // P1 在递归子层被 visited，返回 0
        when(standardCostMapper.selectByProductAndCostItem(eq(PRODUCT_ID), eq(COST_ITEM_MATERIAL), any(LocalDate.class)))
                .thenReturn(sc1);

        // 调用：不应抛出 StackOverflowError
        List<BigDecimal> result = costCalculationService.calculateStandardCostByConvolution(PRODUCT_ID, COST_PERIOD);

        // 断言：卷积结果 = 100 + 50*1 + 0（P1 被环检测跳过）= 150
        assertEquals(1, result.size());
        assertEquals(0, new BigDecimal("150.00").compareTo(result.get(0)));
    }

    @Test
    public void testCalculateStandardCost_emptyData_throwsException() {
        // 准备：无标准成本记录
        when(standardCostMapper.selectEffectiveListByProduct(eq(PRODUCT_ID), any(LocalDate.class)))
                .thenReturn(Collections.emptyList());

        // 调用并断言：抛出异常
        assertThrows(Exception.class, () ->
                costCalculationService.calculateStandardCostByConvolution(PRODUCT_ID, COST_PERIOD));
    }

    // ==================== 实际成本归集 ====================

    @Test
    public void testCollectActualCost_normalPath() {
        // 准备：两条工单成本记录
        ErpWorkOrderCostDO wo1 = ErpWorkOrderCostDO.builder()
                .id(1L).productId(PRODUCT_ID).costPeriod(COST_PERIOD)
                .materialCost(new BigDecimal("500")).laborCost(new BigDecimal("200"))
                .overheadCost(new BigDecimal("100")).outsourcingCost(new BigDecimal("50"))
                .quantity(new BigDecimal("10")).build();
        ErpWorkOrderCostDO wo2 = ErpWorkOrderCostDO.builder()
                .id(2L).productId(PRODUCT_ID).costPeriod(COST_PERIOD)
                .materialCost(new BigDecimal("300")).laborCost(new BigDecimal("100"))
                .overheadCost(new BigDecimal("50")).outsourcingCost(BigDecimal.ZERO)
                .quantity(new BigDecimal("5")).build();
        when(workOrderCostMapper.selectListByProductAndPeriod(PRODUCT_ID, COST_PERIOD))
                .thenReturn(Arrays.asList(wo1, wo2));
        // 成本项目列表（材料 + 人工）
        ErpCostItemDO itemMaterial = ErpCostItemDO.builder()
                .id(COST_ITEM_MATERIAL).type(ErpCostItemTypeEnum.MATERIAL.getType()).status(0).build();
        ErpCostItemDO itemLabor = ErpCostItemDO.builder()
                .id(COST_ITEM_LABOR).type(ErpCostItemTypeEnum.LABOR.getType()).status(0).build();
        when(costItemMapper.selectListByStatus(0)).thenReturn(Arrays.asList(itemMaterial, itemLabor));
        // 无已存在记录
        when(actualCostMapper.selectByProductAndPeriod(eq(PRODUCT_ID), eq(COST_PERIOD), anyLong())).thenReturn(null);

        // 调用
        int count = costCalculationService.collectActualCostFromWorkOrders(PRODUCT_ID, COST_PERIOD);

        // 断言：材料(500+300=800) + 人工(200+100=300) = 2 条记录
        assertEquals(2, count);
        // 验证插入了 2 条实际成本记录
        verify(actualCostMapper, times(2)).insert(any(ErpActualCostDO.class));
    }

    @Test
    public void testCollectActualCost_emptyWorkOrders_throwsException() {
        when(workOrderCostMapper.selectListByProductAndPeriod(PRODUCT_ID, COST_PERIOD))
                .thenReturn(Collections.emptyList());
        assertThrows(Exception.class, () ->
                costCalculationService.collectActualCostFromWorkOrders(PRODUCT_ID, COST_PERIOD));
    }

    // ==================== 差异分析 ====================

    @Test
    public void testAnalyzeVariance_unfavorableVariance() {
        // 准备：实际成本 120，标准成本 100，差异 +20（不利差异）
        ErpActualCostDO actual = ErpActualCostDO.builder()
                .id(1L).productId(PRODUCT_ID).costPeriod(COST_PERIOD).costItemId(COST_ITEM_MATERIAL)
                .actualCost(new BigDecimal("120")).build();
        when(actualCostMapper.selectListByProductAndPeriod(PRODUCT_ID, COST_PERIOD))
                .thenReturn(Collections.singletonList(actual));
        ErpStandardCostDO standard = ErpStandardCostDO.builder()
                .id(1L).productId(PRODUCT_ID).costItemId(COST_ITEM_MATERIAL)
                .standardCost(new BigDecimal("100")).build();
        when(standardCostMapper.selectEffectiveListByProduct(eq(PRODUCT_ID), any(LocalDate.class)))
                .thenReturn(Collections.singletonList(standard));

        // 调用
        int count = costCalculationService.analyzeVariance(PRODUCT_ID, COST_PERIOD);

        // 断言：生成 1 条不利差异记录
        assertEquals(1, count);
        ArgumentCaptor<ErpCostVarianceDO> captor = ArgumentCaptor.forClass(ErpCostVarianceDO.class);
        verify(costVarianceMapper).insert(captor.capture());
        ErpCostVarianceDO variance = captor.getValue();
        assertEquals(0, new BigDecimal("20").compareTo(variance.getVarianceAmount()));
        assertEquals(ErpVarianceTypeEnum.UNFAVORABLE.getType(), variance.getVarianceType());
    }

    @Test
    public void testAnalyzeVariance_favorableVariance() {
        // 准备：实际成本 80，标准成本 100，差异 -20（有利差异）
        ErpActualCostDO actual = ErpActualCostDO.builder()
                .id(1L).productId(PRODUCT_ID).costPeriod(COST_PERIOD).costItemId(COST_ITEM_MATERIAL)
                .actualCost(new BigDecimal("80")).build();
        when(actualCostMapper.selectListByProductAndPeriod(PRODUCT_ID, COST_PERIOD))
                .thenReturn(Collections.singletonList(actual));
        ErpStandardCostDO standard = ErpStandardCostDO.builder()
                .id(1L).productId(PRODUCT_ID).costItemId(COST_ITEM_MATERIAL)
                .standardCost(new BigDecimal("100")).build();
        when(standardCostMapper.selectEffectiveListByProduct(eq(PRODUCT_ID), any(LocalDate.class)))
                .thenReturn(Collections.singletonList(standard));

        // 调用
        int count = costCalculationService.analyzeVariance(PRODUCT_ID, COST_PERIOD);

        // 断言：生成 1 条有利差异记录
        assertEquals(1, count);
        ArgumentCaptor<ErpCostVarianceDO> captor = ArgumentCaptor.forClass(ErpCostVarianceDO.class);
        verify(costVarianceMapper).insert(captor.capture());
        ErpCostVarianceDO variance = captor.getValue();
        assertEquals(0, new BigDecimal("-20").compareTo(variance.getVarianceAmount()));
        assertEquals(ErpVarianceTypeEnum.FAVORABLE.getType(), variance.getVarianceType());
    }

    @Test
    public void testAnalyzeVariance_noVariance_skipped() {
        // 准备：实际成本 = 标准成本，无差异，应跳过
        ErpActualCostDO actual = ErpActualCostDO.builder()
                .id(1L).productId(PRODUCT_ID).costPeriod(COST_PERIOD).costItemId(COST_ITEM_MATERIAL)
                .actualCost(new BigDecimal("100")).build();
        when(actualCostMapper.selectListByProductAndPeriod(PRODUCT_ID, COST_PERIOD))
                .thenReturn(Collections.singletonList(actual));
        ErpStandardCostDO standard = ErpStandardCostDO.builder()
                .id(1L).productId(PRODUCT_ID).costItemId(COST_ITEM_MATERIAL)
                .standardCost(new BigDecimal("100")).build();
        when(standardCostMapper.selectEffectiveListByProduct(eq(PRODUCT_ID), any(LocalDate.class)))
                .thenReturn(Collections.singletonList(standard));

        // 调用
        int count = costCalculationService.analyzeVariance(PRODUCT_ID, COST_PERIOD);

        // 断言：无差异记录生成
        assertEquals(0, count);
        verify(costVarianceMapper, never()).insert(any(ErpCostVarianceDO.class));
    }

    @Test
    public void testAnalyzeVariance_emptyActual_throwsException() {
        when(actualCostMapper.selectListByProductAndPeriod(PRODUCT_ID, COST_PERIOD))
                .thenReturn(Collections.emptyList());
        assertThrows(Exception.class, () ->
                costCalculationService.analyzeVariance(PRODUCT_ID, COST_PERIOD));
    }

}
