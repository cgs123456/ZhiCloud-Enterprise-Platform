package cn.iocoder.yudao.module.mes.service.wm.materialstock;

import cn.iocoder.yudao.framework.datasource.config.YudaoDataSourceAutoConfiguration;
import cn.iocoder.yudao.framework.inventory.config.InventoryAutoConfiguration;
import cn.iocoder.yudao.framework.inventory.dal.dataobject.InventoryItemDO;
import cn.iocoder.yudao.framework.inventory.dal.mysql.InventoryItemMapper;
import cn.iocoder.yudao.framework.inventory.service.InventoryReconcileReport;
import cn.iocoder.yudao.framework.inventory.service.InventoryReconciliationService;
import cn.iocoder.yudao.framework.mybatis.config.YudaoMybatisAutoConfiguration;
import cn.iocoder.yudao.framework.test.config.SqlInitializationTestConfiguration;
import cn.iocoder.yudao.module.mes.dal.dataobject.wm.materialstock.MesWmMaterialStockDO;
import cn.iocoder.yudao.module.mes.dal.mysql.wm.materialstock.MesWmMaterialStockMapper;
import com.alibaba.druid.spring.boot3.autoconfigure.DruidDataSourceAutoConfigure;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.github.yulichang.autoconfigure.MybatisPlusJoinAutoConfiguration;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * MES 库存投影 × 共享真值源 日终对账集成测试（M2 只读投影适配端到端验证）
 *
 * <p>仅导入 DB + 共享 Starter 自动配置 + MES mapper 扫描，避免加载 tenant/redis 等无关自动配置导致 H2 缺表。
 * 验证 {@link MesInventoryProjectionReader} 经 SPI 被 {@link InventoryReconciliationService} 收集，
 * 并在投影与真值一致/不一致两种场景下给出正确结论。
 *
 * @author 智云库存治理
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = MesInventoryReconciliationTest.TestApplication.class)
@ActiveProfiles("unit-test")
@Sql(scripts = "/sql/inventory_clean.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
public class MesInventoryReconciliationTest {

    @Import({
            YudaoDataSourceAutoConfiguration.class,
            DataSourceAutoConfiguration.class,
            DataSourceTransactionManagerAutoConfiguration.class,
            DruidDataSourceAutoConfigure.class,
            SqlInitializationTestConfiguration.class,
            YudaoMybatisAutoConfiguration.class,
            MybatisPlusAutoConfiguration.class,
            MybatisPlusJoinAutoConfiguration.class,
            InventoryAutoConfiguration.class,
            MesInventoryProjectionReader.class
    })
    @MapperScan("cn.iocoder.yudao.module.mes.dal.mysql")
    public static class TestApplication {
    }

    @Resource
    private InventoryItemMapper inventoryItemMapper;
    @Resource
    private MesWmMaterialStockMapper mesWmMaterialStockMapper;
    @Resource
    private InventoryReconciliationService inventoryReconciliationService;

    @Test
    void reconcileConsistentWhenProjectionMatchesTruth() {
        mesWmMaterialStockMapper.insert(mesStock(100L, 10L, 1L, 2L, 5L, new BigDecimal("40")));
        inventoryItemMapper.insert(item(100L, 10L, 1L, 2L, 5L, "B5", new BigDecimal("40")));

        InventoryReconcileReport report = inventoryReconciliationService.reconcile();

        assertTrue(report.isConsistent(), "投影与真值数量一致时应判定一致");
        assertEquals(1, report.getSourceCount());
        assertEquals(1, report.getProjectionCount());
    }

    @Test
    void reconcileDetectsQuantityMismatch() {
        mesWmMaterialStockMapper.insert(mesStock(200L, 20L, 3L, 4L, 6L, new BigDecimal("50")));
        // 真值源谎报为 30，期望对账捕获不一致
        inventoryItemMapper.insert(item(200L, 20L, 3L, 4L, 6L, "B6", new BigDecimal("30")));

        InventoryReconcileReport report = inventoryReconciliationService.reconcile();

        assertFalse(report.isConsistent(), "投影(50)与真值(30)不一致时应捕获");
        assertEquals(1, report.getMismatches().size());
        InventoryReconcileReport.Mismatch m = report.getMismatches().get(0);
        assertEquals("mes", m.getSource());
        assertEquals(200L, m.getItemId());
        assertEquals(20L, m.getWarehouseId());
        assertEquals(0, new BigDecimal("50").compareTo(m.getProjectionQuantity()));
        assertEquals(0, new BigDecimal("30").compareTo(m.getTruthQuantity()));
    }

    private MesWmMaterialStockDO mesStock(Long itemId, Long warehouseId, Long locationId,
                                          Long areaId, Long batchId, BigDecimal qty) {
        return MesWmMaterialStockDO.builder()
                .itemId(itemId).warehouseId(warehouseId).locationId(locationId)
                .areaId(areaId).batchId(batchId).quantity(qty).build();
    }

    private InventoryItemDO item(Long itemId, Long warehouseId, Long locationId,
                                 Long areaId, Long batchId, String batchCode, BigDecimal qty) {
        return InventoryItemDO.builder()
                .itemId(itemId).warehouseId(warehouseId).locationId(locationId)
                .areaId(areaId).batchId(batchId).batchCode(batchCode)
                .quantity(qty).lockedCount(BigDecimal.ZERO).tenantId(0L).build();
    }

}
