package cn.zhicloud.framework.inventory.service;

import cn.zhicloud.framework.datasource.config.ZhiCloudDataSourceAutoConfiguration;
import cn.zhicloud.framework.inventory.config.InventoryAutoConfiguration;
import cn.zhicloud.framework.inventory.dal.dataobject.InventoryItemDO;
import cn.zhicloud.framework.inventory.dal.mysql.InventoryItemMapper;
import cn.zhicloud.framework.mybatis.config.ZhiCloudMybatisAutoConfiguration;
import cn.zhicloud.framework.mybatis.core.util.MyBatisUtils;
import cn.zhicloud.framework.tenant.config.TenantProperties;
import cn.zhicloud.framework.tenant.core.context.TenantContextHolder;
import cn.zhicloud.framework.tenant.core.db.TenantDatabaseInterceptor;
import cn.zhicloud.framework.tenant.core.util.TenantUtils;
import cn.zhicloud.framework.test.config.SqlInitializationTestConfiguration;
import com.alibaba.druid.spring.boot3.autoconfigure.DruidDataSourceAutoConfigure;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.github.yulichang.autoconfigure.MybatisPlusJoinAutoConfiguration;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 库存租户隔离回归测试。
 *
 * <p>现有 {@link InventoryServiceImplTest} 未加载租户拦截器，只能证明单租户并发正确性，
 * 不能证明生产多租户数据面。本测试刻意复用生产 {@link TenantDatabaseInterceptor}，
 * 固化以下边界：非 0 租户写入租户号、同复合键跨租户独立、跨租户更新不可见、
 * 缺租户上下文 fail-closed、对账查询租户作用域。
 *
 * <p>本测试使用 H2 隔离环境，不能替代生产等价 MySQL、真实租户数据源拓扑或独立复核。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = InventoryTenantIsolationTest.TestApplication.class)
@ActiveProfiles("unit-test")
@Sql(scripts = "/sql/clean.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/sql/clean.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
public class InventoryTenantIsolationTest {

    @Import({
            ZhiCloudDataSourceAutoConfiguration.class,
            DataSourceAutoConfiguration.class,
            DataSourceTransactionManagerAutoConfiguration.class,
            DruidDataSourceAutoConfigure.class,
            SqlInitializationTestConfiguration.class,
            ZhiCloudMybatisAutoConfiguration.class,
            MybatisPlusAutoConfiguration.class,
            MybatisPlusJoinAutoConfiguration.class,
            InventoryAutoConfiguration.class,
            TenantInterceptorTestConfiguration.class
    })
    public static class TestApplication {
    }

    /**
     * 只装配租户 DB 拦截器，避免引入 Redis、Web、Security 等无关生产依赖。
     */
    @TestConfiguration(proxyBeanMethods = false)
    public static class TenantInterceptorTestConfiguration {

        @Bean
        public TenantProperties tenantProperties() {
            return new TenantProperties();
        }

        @Bean
        public TenantLineInnerInterceptor tenantLineInnerInterceptor(TenantProperties properties,
                                                                     MybatisPlusInterceptor interceptor) {
            TenantLineInnerInterceptor inner = new TenantLineInnerInterceptor(
                    new TenantDatabaseInterceptor(properties));
            MyBatisUtils.addInterceptor(interceptor, inner, 0);
            return inner;
        }
    }

    @Resource
    private InventoryService inventoryService;

    @Resource
    private MybatisPlusInterceptor mybatisPlusInterceptor;

    @Resource
    private TenantLineInnerInterceptor tenantLineInnerInterceptor;

    @Resource
    private InventoryItemMapper inventoryItemMapper;

    @Resource
    private InventoryReconciliationService inventoryReconciliationService;

    @BeforeEach
    public void setUp() {
        TenantContextHolder.clear();
        assertTrue(mybatisPlusInterceptor.getInterceptors().contains(tenantLineInnerInterceptor),
                "租户拦截器必须注册在 MyBatis 拦截器链中，否则本测试会失去租户隔离验证意义");
    }

    @AfterEach
    public void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    public void testNonZeroTenantInsertPersistsCurrentTenant() {
        Long id = TenantUtils.execute(2L, () -> inventoryService.add(
                101L, 201L, 0L, 0L, 0L, "T2-B1", new BigDecimal("10")));

        assertEquals(2L, readWithoutTenant(id).getTenantId(),
                "非 0 租户写入 inventory_item 时必须持久化当前租户号，不能回落到 tenant_id=0");
    }

    @Test
    public void testSameCompositeKeyIsIndependentAcrossTenants() {
        Long tenant1Id = TenantUtils.execute(1L, () -> inventoryService.add(
                102L, 202L, 0L, 0L, 0L, "T1-B1", new BigDecimal("10")));
        Long tenant2Id = TenantUtils.execute(2L, () -> inventoryService.add(
                102L, 202L, 0L, 0L, 0L, "T2-B1", new BigDecimal("20")));

        assertNotEquals(tenant1Id, tenant2Id,
                "不同租户的同业务复合键必须各自建行，不能被全局唯一键合并或冲突");
        assertEquals(1L, readWithoutTenant(tenant1Id).getTenantId());
        assertEquals(2L, readWithoutTenant(tenant2Id).getTenantId());
        assertEquals(0, new BigDecimal("10").compareTo(
                TenantUtils.execute(1L, () -> inventoryService.getAvailableQuantity(102L, 202L, 0L, 0L, 0L))));
        assertEquals(0, new BigDecimal("20").compareTo(
                TenantUtils.execute(2L, () -> inventoryService.getAvailableQuantity(102L, 202L, 0L, 0L, 0L))));
    }

    @Test
    public void testCrossTenantUpdateCannotChangeOtherTenantRow() {
        Long tenant2Id = TenantUtils.execute(2L, () -> inventoryService.add(
                103L, 203L, 0L, 0L, 0L, "T2-B1", new BigDecimal("10")));

        TenantContextHolder.setTenantId(1L);
        int rows = inventoryItemMapper.updateCountIncrement(tenant2Id, new BigDecimal("5"));

        assertEquals(0, rows, "租户 1 不得通过裸 UPDATE 修改租户 2 的库存行");
        assertEquals(0, new BigDecimal("10").compareTo(
                TenantUtils.execute(2L, () -> inventoryService.getAvailableQuantity(103L, 203L, 0L, 0L, 0L))),
                "跨租户更新失败后，目标租户库存必须保持不变");
    }

    @Test
    public void testMissingTenantContextFailsClosed() {
        TenantContextHolder.clear();

        assertThrows(RuntimeException.class, () -> inventoryService.add(
                104L, 204L, 0L, 0L, 0L, "NO-TENANT", new BigDecimal("1")),
                "缺少租户上下文时不得静默写入 tenant_id=0");
        assertEquals(0L, TenantUtils.executeIgnore(() -> inventoryItemMapper.selectCount(null)),
                "缺租户上下文被拒绝后，不应留下库存行");
    }

    @Test
    public void testReconcileOnlyReadsCurrentTenantTruthSource() {
        TenantUtils.execute(1L, () -> inventoryService.add(
                105L, 205L, 0L, 0L, 0L, "T1-B1", new BigDecimal("10")));
        TenantUtils.execute(2L, () -> inventoryService.add(
                106L, 206L, 0L, 0L, 0L, "T2-B1", new BigDecimal("20")));

        TenantContextHolder.setTenantId(1L);
        InventoryReconcileReport report = inventoryReconciliationService.reconcile();

        assertEquals(1, report.getSourceCount(),
                "日终对账的真值源查询必须只读取当前租户库存，不能全租户扫描");
        assertTrue(report.isConsistent(), "无投影读取器时，当前租户对账结果应一致");
    }

    private InventoryItemDO readWithoutTenant(Long id) {
        return TenantUtils.executeIgnore(() -> inventoryItemMapper.selectById(id));
    }

}
