package cn.zhicloud.module.crm.service.clue;

import cn.zhicloud.framework.datasource.config.ZhiCloudDataSourceAutoConfiguration;
import cn.zhicloud.framework.mybatis.config.ZhiCloudMybatisAutoConfiguration;
import cn.zhicloud.framework.mybatis.core.util.MyBatisUtils;
import cn.zhicloud.framework.tenant.config.TenantProperties;
import cn.zhicloud.framework.tenant.core.context.TenantContextHolder;
import cn.zhicloud.framework.tenant.core.db.TenantBaseDO;
import cn.zhicloud.framework.tenant.core.db.TenantDatabaseInterceptor;
import cn.zhicloud.framework.tenant.core.util.TenantUtils;
import cn.zhicloud.framework.test.config.SqlInitializationTestConfiguration;
import cn.zhicloud.module.crm.controller.admin.clue.vo.poolconfig.CrmCluePoolConfigSaveReqVO;
import cn.zhicloud.module.crm.dal.dataobject.clue.CrmCluePoolConfigDO;
import cn.zhicloud.module.crm.dal.mysql.clue.CrmCluePoolConfigMapper;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 线索公海配置的租户隔离回归测试。
 *
 * <p>普通 H2 单测不会注册生产租户拦截器，因此无法发现缺列、漏加租户条件和跨租户覆盖问题。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = CrmCluePoolConfigTenantIsolationTest.TestApplication.class)
@ActiveProfiles("unit-test")
@Sql(scripts = "/sql/clean.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
public class CrmCluePoolConfigTenantIsolationTest {

    @Import({
            ZhiCloudDataSourceAutoConfiguration.class,
            DataSourceAutoConfiguration.class,
            DataSourceTransactionManagerAutoConfiguration.class,
            DruidDataSourceAutoConfigure.class,
            SqlInitializationTestConfiguration.class,
            ZhiCloudMybatisAutoConfiguration.class,
            MybatisPlusAutoConfiguration.class,
            MybatisPlusJoinAutoConfiguration.class,
            CrmCluePoolConfigServiceImpl.class,
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
    private CrmCluePoolConfigService cluePoolConfigService;


    @Resource
    private CrmCluePoolConfigMapper cluePoolConfigMapper;

    @AfterEach
    public void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    public void testCluePoolConfigUsesTenantBaseDO() {
        assertTrue(TenantBaseDO.class.isAssignableFrom(CrmCluePoolConfigDO.class),
                "线索公海配置必须映射 tenant_id，不能只依赖数据库隐式默认值");
    }

    @Test
    public void testCluePoolConfigIsolatedByTenant() {
        saveConfig(1L, 11);
        saveConfig(2L, 22);

        TenantUtils.execute(1L, () -> {
            CrmCluePoolConfigDO config = cluePoolConfigService.getCluePoolConfig();
            assertNotNull(config);
            assertEquals(11, config.getNotifyDays());
            TenantBaseDO tenantDO = assertInstanceOf(TenantBaseDO.class, config);
            assertEquals(1L, tenantDO.getTenantId());
        });
        TenantUtils.execute(2L, () -> {
            CrmCluePoolConfigDO config = cluePoolConfigService.getCluePoolConfig();
            assertNotNull(config);
            assertEquals(22, config.getNotifyDays());
            TenantBaseDO tenantDO = assertInstanceOf(TenantBaseDO.class, config);
            assertEquals(2L, tenantDO.getTenantId());
        });

        TenantUtils.executeIgnore(() -> assertEquals(2, cluePoolConfigMapper.selectList(null).size(),
                "两个租户应各自保留一份配置"));
    }

    private void saveConfig(Long tenantId, Integer notifyDays) {
        CrmCluePoolConfigSaveReqVO reqVO = new CrmCluePoolConfigSaveReqVO();
        reqVO.setEnabled(true);
        reqVO.setContactExpireDays(30);
        reqVO.setNotifyEnabled(true);
        reqVO.setNotifyDays(notifyDays);
        TenantUtils.execute(tenantId, () -> cluePoolConfigService.saveCluePoolConfig(reqVO));
    }

}
