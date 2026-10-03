package cn.zhicloud.module.crm.service.receivable;

import cn.zhicloud.framework.datasource.config.ZhiCloudDataSourceAutoConfiguration;
import cn.zhicloud.framework.mybatis.config.ZhiCloudMybatisAutoConfiguration;
import cn.zhicloud.framework.mybatis.core.util.MyBatisUtils;
import cn.zhicloud.framework.tenant.config.TenantProperties;
import cn.zhicloud.framework.tenant.core.context.TenantContextHolder;
import cn.zhicloud.framework.tenant.core.db.TenantBaseDO;
import cn.zhicloud.framework.tenant.core.db.TenantDatabaseInterceptor;
import cn.zhicloud.framework.tenant.core.util.TenantUtils;
import cn.zhicloud.framework.test.config.SqlInitializationTestConfiguration;
import cn.zhicloud.module.crm.dal.dataobject.receivable.CrmReceivableDO;
import cn.zhicloud.module.crm.dal.dataobject.receivable.CrmReceivablePlanDO;
import cn.zhicloud.module.crm.dal.mysql.receivable.CrmReceivableMapper;
import cn.zhicloud.module.crm.dal.mysql.receivable.CrmReceivablePlanMapper;
import cn.zhicloud.module.crm.enums.common.CrmAuditStatusEnum;
import com.alibaba.druid.spring.boot3.autoconfigure.DruidDataSourceAutoConfigure;
import com.baomidou.mybatisplus.autoconfigure.MybatisPlusAutoConfiguration;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.github.yulichang.autoconfigure.MybatisPlusJoinAutoConfiguration;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 回款与回款计划的租户隔离回归测试。
 *
 * <p>普通 H2 单测不注册生产租户拦截器，无法发现 DO 未继承 {@link TenantBaseDO} 时
 * 查询和插入遗漏 tenant_id 的问题。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = CrmReceivableTenantIsolationTest.TestApplication.class)
@ActiveProfiles("unit-test")
@Sql(scripts = "/sql/clean.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
public class CrmReceivableTenantIsolationTest {

    @Import({
            ZhiCloudDataSourceAutoConfiguration.class,
            DataSourceAutoConfiguration.class,
            DataSourceTransactionManagerAutoConfiguration.class,
            DruidDataSourceAutoConfigure.class,
            SqlInitializationTestConfiguration.class,
            ZhiCloudMybatisAutoConfiguration.class,
            MybatisPlusAutoConfiguration.class,
            MybatisPlusJoinAutoConfiguration.class,
            TenantInterceptorTestConfiguration.class
    })
    public static class TestApplication {
    }

    @org.springframework.boot.test.context.TestConfiguration(proxyBeanMethods = false)
    public static class TenantInterceptorTestConfiguration {

        @Bean
        public TenantProperties tenantProperties() {
            return new TenantProperties();
        }

        @Bean
        @org.springframework.context.annotation.Lazy(false)
        public TenantLineInnerInterceptor tenantLineInnerInterceptor(TenantProperties properties,
                                                                     MybatisPlusInterceptor interceptor) {
            TenantLineInnerInterceptor inner = new TenantLineInnerInterceptor(
                    new TenantDatabaseInterceptor(properties));
            MyBatisUtils.addInterceptor(interceptor, inner, 0);
            return inner;
        }
    }

    @Resource
    private CrmReceivableMapper receivableMapper;

    @Resource
    private CrmReceivablePlanMapper receivablePlanMapper;

    @AfterEach
    public void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    public void testReceivableAndPlanUseTenantBaseDO() {
        assertTrue(TenantBaseDO.class.isAssignableFrom(CrmReceivableDO.class),
                "回款必须映射 tenant_id，不能只依赖数据库隐式默认值");
        assertTrue(TenantBaseDO.class.isAssignableFrom(CrmReceivablePlanDO.class),
                "回款计划必须映射 tenant_id，不能只依赖数据库隐式默认值");
    }

    @Test
    public void testSameIdempotencyKeyIsIsolatedByTenant() {
        Long receivableId1 = insertReceivable(1L, "same-idempotency-key", "REC-TENANT-1");
        Long receivableId2 = insertReceivable(2L, "same-idempotency-key", "REC-TENANT-2");

        TenantUtils.execute(1L, () -> {
            CrmReceivableDO row = receivableMapper.selectByIdempotencyKey("same-idempotency-key");
            assertNotNull(row);
            assertEquals(receivableId1, row.getId());
            assertEquals(1L, assertInstanceOf(TenantBaseDO.class, row).getTenantId());
        });
        TenantUtils.execute(2L, () -> {
            CrmReceivableDO row = receivableMapper.selectByIdempotencyKey("same-idempotency-key");
            assertNotNull(row);
            assertEquals(receivableId2, row.getId());
            assertEquals(2L, assertInstanceOf(TenantBaseDO.class, row).getTenantId());
        });
    }

    @Test
    public void testReceivablePlanIsIsolatedByTenant() {
        Long planId1 = insertReceivablePlan(1L, "PLAN-TENANT-1");
        Long planId2 = insertReceivablePlan(2L, "PLAN-TENANT-2");

        TenantUtils.execute(1L, () -> {
            CrmReceivablePlanDO row = receivablePlanMapper.selectById(planId1);
            assertNotNull(row);
            assertEquals(1L, assertInstanceOf(TenantBaseDO.class, row).getTenantId());
        });
        TenantUtils.execute(2L, () -> {
            CrmReceivablePlanDO row = receivablePlanMapper.selectById(planId2);
            assertNotNull(row);
            assertEquals(2L, assertInstanceOf(TenantBaseDO.class, row).getTenantId());
        });
    }

    private Long insertReceivable(Long tenantId, String idempotencyKey, String no) {
        CrmReceivableDO row = CrmReceivableDO.builder()
                .no(no)
                .contractId(100L)
                .price(new BigDecimal("10.00"))
                .auditStatus(CrmAuditStatusEnum.DRAFT.getStatus())
                .idempotencyKey(idempotencyKey)
                .payloadHash("hash-" + tenantId)
                .version(0)
                .build();
        TenantUtils.execute(tenantId, () -> receivableMapper.insert(row));
        return row.getId();
    }

    private Long insertReceivablePlan(Long tenantId, String no) {
        CrmReceivablePlanDO row = CrmReceivablePlanDO.builder()
                .period(1)
                .contractId(100L)
                .price(new BigDecimal("10.00"))
                .remark(no)
                .version(0)
                .build();
        TenantUtils.execute(tenantId, () -> receivablePlanMapper.insert(row));
        return row.getId();
    }

}
