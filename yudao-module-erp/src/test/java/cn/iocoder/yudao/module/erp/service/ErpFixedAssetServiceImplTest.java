package cn.iocoder.yudao.module.erp.service;

import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpFixedAssetDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpFixedAssetDepreciationDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpPeriodDO;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpFixedAssetDepreciationMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpFixedAssetMapper;
import cn.iocoder.yudao.module.erp.enums.finance.ErpDepreciationMethodEnum;
import cn.iocoder.yudao.module.erp.enums.finance.ErpFixedAssetStatusEnum;
import cn.iocoder.yudao.module.erp.service.finance.ErpFixedAssetServiceImpl;
import cn.iocoder.yudao.module.erp.service.finance.ErpPeriodService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * {@link ErpFixedAssetServiceImpl} 的单元测试
 *
 * <p>覆盖直线法、DDB（双倍余额递减法）、SYD（年数总和法）折旧算法，含尾差处理。
 *
 * @author 芋道源码
 */
@ExtendWith(MockitoExtension.class)
public class ErpFixedAssetServiceImplTest {

    @Mock
    private ErpFixedAssetMapper fixedAssetMapper;
    @Mock
    private ErpFixedAssetDepreciationMapper depreciationMapper;
    @Mock
    private ErpPeriodService periodService;

    @InjectMocks
    private ErpFixedAssetServiceImpl fixedAssetService;

    private static final Long ASSET_ID = 1L;
    private static final Long PERIOD_ID = 10L;
    private static final Long DEP_ID = 100L;

    /**
     * 模拟 MyBatis-Plus 的主键回填行为
     *
     * <p>生产环境下 {@code useGeneratedKeys} 会在 insert 后把自增主键写回实体对象，
     * 而 Mockito 的空 mock 不会，因此需要显式桩出该副作用，否则被测方法返回的 ID 恒为 null。
     */
    private void mockInsertWithGeneratedId(Long generatedId) {
        when(depreciationMapper.insert(any(ErpFixedAssetDepreciationDO.class))).thenAnswer(invocation -> {
            ErpFixedAssetDepreciationDO arg = invocation.getArgument(0);
            arg.setId(generatedId);
            return 1;
        });
    }

    /**
     * 构建一个标准测试资产
     */
    private ErpFixedAssetDO buildAsset(Integer method, BigDecimal originalValue,
                                        BigDecimal salvageValue, int usefulLifeMonths,
                                        int depreciatedMonths, BigDecimal accumulatedDep) {
        return ErpFixedAssetDO.builder()
                .id(ASSET_ID).code("FA-001").name("测试资产")
                .originalValue(originalValue).salvageValue(salvageValue)
                .usefulLifeMonths(usefulLifeMonths).depreciationMethod(method)
                .depreciatedMonths(depreciatedMonths)
                .accumulatedDepreciation(accumulatedDep)
                .netBookValue(originalValue.subtract(accumulatedDep))
                .status(ErpFixedAssetStatusEnum.IN_USE.getStatus())
                .build();
    }

    private void mockPeriod() {
        ErpPeriodDO period = ErpPeriodDO.builder().id(PERIOD_ID).code("202607").build();
        lenient().when(periodService.getPeriod(PERIOD_ID)).thenReturn(period);
    }

    // ==================== 直线法（SL） ====================

    @Test
    public void testStraightLine_normalMonthly() {
        // 原值 12000，残值 0，使用 12 月，已折旧 0 月
        // 月折旧额 = 12000 / 12 = 1000
        ErpFixedAssetDO asset = buildAsset(ErpDepreciationMethodEnum.STRAIGHT_LINE.getMethod(),
                new BigDecimal("12000"), BigDecimal.ZERO, 12, 0, BigDecimal.ZERO);
        when(fixedAssetMapper.selectById(ASSET_ID)).thenReturn(asset);
        when(depreciationMapper.selectByFixedAssetIdAndPeriodId(ASSET_ID, PERIOD_ID)).thenReturn(null);
        // 模拟 MyBatis-Plus insert 的主键回填：真实环境下 useGeneratedKeys 会把自增 ID 写回实体，
        // 而 Mock 的 insert 不会，导致 Service 返回的 depreciation.getId() 恒为 null
        mockInsertWithGeneratedId(DEP_ID);
        mockPeriod();

        Long depId = fixedAssetService.calculateMonthlyDepreciation(ASSET_ID, PERIOD_ID);

        assertNotNull(depId);
        assertEquals(DEP_ID, depId);
        ArgumentCaptor<ErpFixedAssetDepreciationDO> captor = ArgumentCaptor.forClass(ErpFixedAssetDepreciationDO.class);
        verify(depreciationMapper).insert(captor.capture());
        // 月折旧额 = 1000.00
        assertEquals(0, new BigDecimal("1000.00").compareTo(captor.getValue().getDepreciationAmount()));
    }

    @Test
    public void testStraightLine_tailMonth_adjustToExact() {
        // 原值 10000，残值 0，使用 12 月，已折旧 11 月，累计折旧 9200
        // 最后一个月尾差处理：10000 - 0 - 9200 = 800（而非 10000/12=833.33）
        ErpFixedAssetDO asset = buildAsset(ErpDepreciationMethodEnum.STRAIGHT_LINE.getMethod(),
                new BigDecimal("10000"), BigDecimal.ZERO, 12, 11, new BigDecimal("9200"));
        when(fixedAssetMapper.selectById(ASSET_ID)).thenReturn(asset);
        when(depreciationMapper.selectByFixedAssetIdAndPeriodId(ASSET_ID, PERIOD_ID)).thenReturn(null);
        mockPeriod();

        fixedAssetService.calculateMonthlyDepreciation(ASSET_ID, PERIOD_ID);

        ArgumentCaptor<ErpFixedAssetDepreciationDO> captor = ArgumentCaptor.forClass(ErpFixedAssetDepreciationDO.class);
        verify(depreciationMapper).insert(captor.capture());
        // 尾差处理：剩余可折旧额 = 10000 - 9200 = 800
        assertEquals(0, new BigDecimal("800.00").compareTo(captor.getValue().getDepreciationAmount()));
    }

    // ==================== DDB（双倍余额递减法） ====================

    @Test
    public void testDdb_normalMonthly() {
        // 原值 100000，残值 0，使用 60 月（5 年），已折旧 0 月
        // 月折旧率 = 2/60 = 0.0333...
        // 月折旧额 = 100000 * 2/60 = 3333.33
        ErpFixedAssetDO asset = buildAsset(ErpDepreciationMethodEnum.DOUBLE_DECLINING_BALANCE.getMethod(),
                new BigDecimal("100000"), BigDecimal.ZERO, 60, 0, BigDecimal.ZERO);
        when(fixedAssetMapper.selectById(ASSET_ID)).thenReturn(asset);
        when(depreciationMapper.selectByFixedAssetIdAndPeriodId(ASSET_ID, PERIOD_ID)).thenReturn(null);
        mockPeriod();

        fixedAssetService.calculateMonthlyDepreciation(ASSET_ID, PERIOD_ID);

        ArgumentCaptor<ErpFixedAssetDepreciationDO> captor = ArgumentCaptor.forClass(ErpFixedAssetDepreciationDO.class);
        verify(depreciationMapper).insert(captor.capture());
        // DDB 月折旧额 = 100000 * 2 / 60 = 3333.33
        BigDecimal expected = new BigDecimal("100000").multiply(new BigDecimal("2"))
                .divide(new BigDecimal("60"), 2, RoundingMode.HALF_UP);
        assertEquals(0, expected.compareTo(captor.getValue().getDepreciationAmount()));
    }

    @Test
    public void testDdb_lastTwoYears_switchToStraightLine() {
        // 原值 100000，残值 0，使用 60 月（5 年），已折旧 36 月（3 年）
        // 剩余月数 = 24（最后两年），切换为直线法
        // 假设累计折旧 = 70000，账面净值 = 30000
        // 直线法月折旧 = 30000 / 24 = 1250.00
        ErpFixedAssetDO asset = buildAsset(ErpDepreciationMethodEnum.DOUBLE_DECLINING_BALANCE.getMethod(),
                new BigDecimal("100000"), BigDecimal.ZERO, 60, 36, new BigDecimal("70000"));
        when(fixedAssetMapper.selectById(ASSET_ID)).thenReturn(asset);
        when(depreciationMapper.selectByFixedAssetIdAndPeriodId(ASSET_ID, PERIOD_ID)).thenReturn(null);
        mockPeriod();

        fixedAssetService.calculateMonthlyDepreciation(ASSET_ID, PERIOD_ID);

        ArgumentCaptor<ErpFixedAssetDepreciationDO> captor = ArgumentCaptor.forClass(ErpFixedAssetDepreciationDO.class);
        verify(depreciationMapper).insert(captor.capture());
        // 最后两年切换直线法：(100000-70000-0) / 24 = 1250.00
        assertEquals(0, new BigDecimal("1250.00").compareTo(captor.getValue().getDepreciationAmount()));
    }

    @Test
    public void testDdb_tailMonth_adjustToExact() {
        // 原值 10000，残值 0，使用 60 月，已折旧 59 月，累计折旧 9800
        // 最后一个月：10000 - 0 - 9800 = 200
        ErpFixedAssetDO asset = buildAsset(ErpDepreciationMethodEnum.DOUBLE_DECLINING_BALANCE.getMethod(),
                new BigDecimal("10000"), BigDecimal.ZERO, 60, 59, new BigDecimal("9800"));
        when(fixedAssetMapper.selectById(ASSET_ID)).thenReturn(asset);
        when(depreciationMapper.selectByFixedAssetIdAndPeriodId(ASSET_ID, PERIOD_ID)).thenReturn(null);
        mockPeriod();

        fixedAssetService.calculateMonthlyDepreciation(ASSET_ID, PERIOD_ID);

        ArgumentCaptor<ErpFixedAssetDepreciationDO> captor = ArgumentCaptor.forClass(ErpFixedAssetDepreciationDO.class);
        verify(depreciationMapper).insert(captor.capture());
        // 尾差：10000 - 9800 = 200
        assertEquals(0, new BigDecimal("200.00").compareTo(captor.getValue().getDepreciationAmount()));
    }

    // ==================== SYD（年数总和法） ====================

    @Test
    public void testSyd_normalMonthly() {
        // 原值 100000，残值 0，使用 60 月（5 年），已折旧 0 月
        // 年数总和 = 5*6/2 = 15
        // 第一年折旧率 = 5/15
        // 月折旧额 = 100000 * 5 / 15 / 12 = 2777.78
        ErpFixedAssetDO asset = buildAsset(ErpDepreciationMethodEnum.SUM_OF_YEARS_DIGITS.getMethod(),
                new BigDecimal("100000"), BigDecimal.ZERO, 60, 0, BigDecimal.ZERO);
        when(fixedAssetMapper.selectById(ASSET_ID)).thenReturn(asset);
        when(depreciationMapper.selectByFixedAssetIdAndPeriodId(ASSET_ID, PERIOD_ID)).thenReturn(null);
        mockPeriod();

        fixedAssetService.calculateMonthlyDepreciation(ASSET_ID, PERIOD_ID);

        ArgumentCaptor<ErpFixedAssetDepreciationDO> captor = ArgumentCaptor.forClass(ErpFixedAssetDepreciationDO.class);
        verify(depreciationMapper).insert(captor.capture());
        // SYD 月折旧额 = 100000 * 5 / 15 / 12 = 2777.78
        BigDecimal expected = new BigDecimal("100000")
                .multiply(new BigDecimal("5"))
                .divide(new BigDecimal("15"), 6, RoundingMode.HALF_UP)
                .divide(new BigDecimal("12"), 2, RoundingMode.HALF_UP);
        assertEquals(0, expected.compareTo(captor.getValue().getDepreciationAmount()));
    }

    @Test
    public void testSyd_tailMonth_adjustToExact() {
        // 原值 10000，残值 0，使用 60 月，已折旧 59 月，累计折旧 9500
        // 最后一个月：10000 - 0 - 9500 = 500
        ErpFixedAssetDO asset = buildAsset(ErpDepreciationMethodEnum.SUM_OF_YEARS_DIGITS.getMethod(),
                new BigDecimal("10000"), BigDecimal.ZERO, 60, 59, new BigDecimal("9500"));
        when(fixedAssetMapper.selectById(ASSET_ID)).thenReturn(asset);
        when(depreciationMapper.selectByFixedAssetIdAndPeriodId(ASSET_ID, PERIOD_ID)).thenReturn(null);
        mockPeriod();

        fixedAssetService.calculateMonthlyDepreciation(ASSET_ID, PERIOD_ID);

        ArgumentCaptor<ErpFixedAssetDepreciationDO> captor = ArgumentCaptor.forClass(ErpFixedAssetDepreciationDO.class);
        verify(depreciationMapper).insert(captor.capture());
        // 尾差：10000 - 9500 = 500
        assertEquals(0, new BigDecimal("500.00").compareTo(captor.getValue().getDepreciationAmount()));
    }

    // ==================== 边界条件 ====================

    @Test
    public void testDepreciation_fullyDepreciated_throwsException() {
        // 已折旧月数 >= 使用年限，应抛出异常
        ErpFixedAssetDO asset = buildAsset(ErpDepreciationMethodEnum.STRAIGHT_LINE.getMethod(),
                new BigDecimal("10000"), BigDecimal.ZERO, 12, 12, new BigDecimal("10000"));
        when(fixedAssetMapper.selectById(ASSET_ID)).thenReturn(asset);

        assertThrows(Exception.class, () ->
                fixedAssetService.calculateMonthlyDepreciation(ASSET_ID, PERIOD_ID));
    }

    @Test
    public void testDepreciation_assetNotInUse_throwsException() {
        // 资产状态不是在用
        ErpFixedAssetDO asset = buildAsset(ErpDepreciationMethodEnum.STRAIGHT_LINE.getMethod(),
                new BigDecimal("10000"), BigDecimal.ZERO, 12, 0, BigDecimal.ZERO);
        asset.setStatus(ErpFixedAssetStatusEnum.IDLE.getStatus());
        when(fixedAssetMapper.selectById(ASSET_ID)).thenReturn(asset);

        assertThrows(Exception.class, () ->
                fixedAssetService.calculateMonthlyDepreciation(ASSET_ID, PERIOD_ID));
    }

    @Test
    public void testDepreciation_duplicatePeriod_throwsException() {
        // 该期间已有折旧记录，应抛出异常
        ErpFixedAssetDO asset = buildAsset(ErpDepreciationMethodEnum.STRAIGHT_LINE.getMethod(),
                new BigDecimal("10000"), BigDecimal.ZERO, 12, 0, BigDecimal.ZERO);
        when(fixedAssetMapper.selectById(ASSET_ID)).thenReturn(asset);
        when(depreciationMapper.selectByFixedAssetIdAndPeriodId(ASSET_ID, PERIOD_ID))
                .thenReturn(new ErpFixedAssetDepreciationDO());

        assertThrows(Exception.class, () ->
                fixedAssetService.calculateMonthlyDepreciation(ASSET_ID, PERIOD_ID));
    }

    @Test
    public void testDepreciation_assetNotFound_throwsException() {
        when(fixedAssetMapper.selectById(ASSET_ID)).thenReturn(null);
        assertThrows(Exception.class, () ->
                fixedAssetService.calculateMonthlyDepreciation(ASSET_ID, PERIOD_ID));
    }

    @Test
    public void testApproveDepreciation_normal() {
        // 准备一条待审核折旧记录
        ErpFixedAssetDepreciationDO dep = ErpFixedAssetDepreciationDO.builder()
                .id(100L).status(10).build();
        when(depreciationMapper.selectById(100L)).thenReturn(dep);

        // 调用
        fixedAssetService.approveDepreciation(100L);

        // 断言：状态更新为已审核，凭证编号已生成
        ArgumentCaptor<ErpFixedAssetDepreciationDO> captor = ArgumentCaptor.forClass(ErpFixedAssetDepreciationDO.class);
        verify(depreciationMapper).updateById(captor.capture());
        assertEquals(20, captor.getValue().getStatus());
        assertNotNull(captor.getValue().getVoucherNo());
    }

}
