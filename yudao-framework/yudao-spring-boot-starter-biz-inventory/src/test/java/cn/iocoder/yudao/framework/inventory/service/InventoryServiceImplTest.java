package cn.iocoder.yudao.framework.inventory.service;

import cn.iocoder.yudao.framework.datasource.config.YudaoDataSourceAutoConfiguration;
import cn.iocoder.yudao.framework.inventory.config.InventoryAutoConfiguration;
import cn.iocoder.yudao.framework.inventory.dal.mysql.InventoryItemMapper;
import cn.iocoder.yudao.framework.inventory.enums.ErrorCodeConstants;
import cn.iocoder.yudao.framework.inventory.service.InventoryProjection;
import cn.iocoder.yudao.framework.inventory.service.InventoryProjectionReader;
import cn.iocoder.yudao.framework.inventory.service.InventoryReconcileReport;
import cn.iocoder.yudao.framework.inventory.service.InventoryReconciliationService;
import cn.iocoder.yudao.framework.mybatis.config.YudaoMybatisAutoConfiguration;
import cn.iocoder.yudao.framework.test.config.SqlInitializationTestConfiguration;
import com.alibaba.druid.spring.boot3.autoconfigure.DruidDataSourceAutoConfigure;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.github.yulichang.autoconfigure.MybatisPlusJoinAutoConfiguration;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 共享库存 Service 并发正确性测试（P1-4 三件套）
 *
 * <p>仅导入 DB + 本 Starter 自动配置，避免加载 tenant/redis/security 等无关自动配置导致 H2 缺表。
 *
 * @author 智云库存治理
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = InventoryServiceImplTest.TestApplication.class)
@ActiveProfiles("unit-test")
@Sql(scripts = "/sql/clean.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
public class InventoryServiceImplTest {

    @Import({
            YudaoDataSourceAutoConfiguration.class,
            DataSourceAutoConfiguration.class,
            DataSourceTransactionManagerAutoConfiguration.class,
            DruidDataSourceAutoConfigure.class,
            SqlInitializationTestConfiguration.class,
            YudaoMybatisAutoConfiguration.class,
            MybatisPlusAutoConfiguration.class,
            MybatisPlusJoinAutoConfiguration.class,
            InventoryAutoConfiguration.class
    })
    public static class TestApplication {
    }

    @Resource
    private InventoryService inventoryService;

    @Resource
    private InventoryItemMapper inventoryItemMapper;

    @Resource
    private InventoryReconciliationService inventoryReconciliationService;

    @Test
    public void testConcurrentAdd() throws Exception {
        int threads = 20;
        BigDecimal per = new BigDecimal("10");
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            futures.add(pool.submit(() -> {
                try {
                    start.await();
                    inventoryService.add(1L, 1L, 0L, 0L, 0L, "B1", per);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            }));
        }
        start.countDown();
        done.await(30, TimeUnit.SECONDS);
        pool.shutdown();
        // 任一线程抛异常则在此暴露
        for (Future<?> f : futures) {
            f.get();
        }
        BigDecimal avail = inventoryService.getAvailableQuantity(1L, 1L, 0L, 0L, 0L);
        assertEquals(0, per.multiply(BigDecimal.valueOf(threads)).compareTo(avail), "并发 add 后可用数量应等于各线程增量之和");
    }

    @Test
    public void testDeductInsufficient() {
        inventoryService.add(2L, 1L, 0L, 0L, 0L, "B2", new BigDecimal("5"));
        assertServiceException(() -> inventoryService.deduct(2L, 1L, 0L, 0L, 0L, new BigDecimal("10")),
                ErrorCodeConstants.INVENTORY_QUANTITY_NOT_ENOUGH);
    }

    @Test
    public void testReserveAndRelease() {
        inventoryService.add(3L, 1L, 0L, 0L, 0L, "B3", new BigDecimal("100"));
        inventoryService.reserve(3L, 1L, 0L, 0L, 0L, new BigDecimal("30"));
        assertEquals(0, new BigDecimal("70").compareTo(inventoryService.getAvailableQuantity(3L, 1L, 0L, 0L, 0L)));
        inventoryService.release(3L, 1L, 0L, 0L, 0L, new BigDecimal("30"));
        assertEquals(0, new BigDecimal("100").compareTo(inventoryService.getAvailableQuantity(3L, 1L, 0L, 0L, 0L)));
    }

    @Test
    public void testGetOrCreateIdempotent() {
        Long id1 = inventoryService.add(4L, 1L, 0L, 0L, 0L, "B4", new BigDecimal("1"));
        Long id2 = inventoryService.add(4L, 1L, 0L, 0L, 0L, "B4", new BigDecimal("2"));
        assertEquals(id1, id2, "同一复合键应复用同一库存条目");
        assertEquals(0, new BigDecimal("3").compareTo(inventoryService.getAvailableQuantity(4L, 1L, 0L, 0L, 0L)));
    }

    @Test
    public void testReconcileNoReadersIsConsistent() {
        inventoryService.add(5L, 1L, 0L, 0L, 0L, "B5", new BigDecimal("10"));
        InventoryReconcileReport report = inventoryReconciliationService.reconcile();
        assertTrue(report.isConsistent(), "无投影读取器时不应产生不一致项");
    }

    @Test
    public void testReconcileDetectsMismatch() {
        inventoryService.add(6L, 1L, 0L, 0L, 0L, "B6", new BigDecimal("50"));
        // 投影方谎报数量为 30，期望对账捕获不一致
        InventoryProjectionReader fakeReader = () -> {
            InventoryProjection p = new InventoryProjection();
            p.setSource("mes");
            p.setItemId(6L);
            p.setWarehouseId(1L);
            p.setLocationId(0L);
            p.setAreaId(0L);
            p.setBatchId(0L);
            p.setQuantity(new BigDecimal("30"));
            p.setLockedCount(BigDecimal.ZERO);
            return java.util.List.of(p);
        };
        // 直接注入临时 reader 进行单测
        InventoryReconciliationService svc = new InventoryReconciliationService();
        setField(svc, "inventoryItemMapper", inventoryItemMapper);
        setField(svc, "projectionReaders", java.util.List.of(fakeReader));
        InventoryReconcileReport report = svc.reconcile();
        assertFalse(report.isConsistent());
        assertEquals(1, report.getMismatches().size());
    }

    private void setField(Object target, String name, Object value) {
        try {
            var f = target.getClass().getDeclaredField(name);
            f.setAccessible(true);
            f.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
