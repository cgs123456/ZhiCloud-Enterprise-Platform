package cn.zhicloud.framework.tenant.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.boot.LazyInitializationBeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 固化租户 SQL 拦截器在全局懒加载开启时仍必须注册的语义。
 *
 * <p>库存租户隔离缺陷的根因之一，是测试环境通过
 * {@code spring.main.lazy-initialization=true} 让拦截器 Bean 从未实例化；
 * 本测试直接覆盖该自动配置路径，不依赖库存测试中的手工注册配置。</p>
 */
class ZhiCloudTenantAutoConfigurationLazyInitializationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ZhiCloudTenantAutoConfiguration.class))
            .withUserConfiguration(MybatisTestConfiguration.class)
            .withPropertyValues("spring.main.lazy-initialization=true")
            .withInitializer(context ->
                    context.addBeanFactoryPostProcessor(new LazyInitializationBeanFactoryPostProcessor()));

    @Test
    void tenantLineInterceptorIsRegisteredWhenGlobalLazyInitializationIsEnabled() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();

            MybatisPlusInterceptor interceptor = context.getBean(MybatisPlusInterceptor.class);

            assertThat(interceptor.getInterceptors())
                    .filteredOn(TenantLineInnerInterceptor.class::isInstance)
                    .hasSize(1);
        });
    }

    @Configuration(proxyBeanMethods = false)
    static class MybatisTestConfiguration {

        @Bean
        MybatisPlusInterceptor mybatisPlusInterceptor() {
            return new MybatisPlusInterceptor();
        }
    }
}