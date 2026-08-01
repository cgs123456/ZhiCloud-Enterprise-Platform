package cn.iocoder.yudao.framework.tracer.config;

import cn.iocoder.yudao.framework.tracer.core.metrics.DruidMetricsBinder;
import com.baomidou.dynamic.datasource.DynamicRoutingDataSource;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;
import java.util.Map;

/**
 * Metrics 配置类
 *
 * @author 芋道源码
 */
@AutoConfiguration
@Slf4j
@ConditionalOnClass({MeterRegistryCustomizer.class})
@ConditionalOnProperty(prefix = "yudao.metrics", value = "enable", matchIfMissing = true) // 允许使用 yudao.metrics.enable=false 禁用 Metrics
public class YudaoMetricsAutoConfiguration {

    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags(
            @Value("${spring.application.name}") String applicationName) {
        return registry -> registry.config().commonTags("application", applicationName);
    }

    /**
     * P1-4 可观测性：Druid 连接池指标绑定器
     *
     * <p>支持两种数据源形态：
     * <ul>
     *   <li>动态数据源（{@link DynamicRoutingDataSource}）：注入 @Primary DataSource，cast 后遍历其内部 master/slave 等 DataSource 并注册</li>
     *   <li>单数据源：直接注册（如未启用 dynamic-datasource）</li>
     * </ul>
     * 该 Bean 实现 {@link MeterBinder}，Spring Boot 会自动调用 {@link MeterBinder#bindTo(MeterRegistry)}
     * 将指标注册到 MeterRegistry。运行时由 {@link DruidMetricsBinder#registerDataSource} 在数据源初始化后调用。</p>
     */
    @Bean
    public DruidMetricsBinder druidMetricsBinder(DataSource dataSource, MeterRegistry meterRegistry) {
        DruidMetricsBinder binder = new DruidMetricsBinder();
        // 强制依赖 DataSource，确保数据源先初始化（@Primary 注入的实际是 DynamicRoutingDataSource）
        if (dataSource instanceof DynamicRoutingDataSource dynamicDs) {
            Map<String, DataSource> inner = dynamicDs.getDataSources();
            for (Map.Entry<String, DataSource> entry : inner.entrySet()) {
                binder.registerDataSource(entry.getKey(), entry.getValue());
            }
            log.info("[YudaoMetricsAutoConfiguration] Druid 指标绑定器已注册动态数据源: {}", inner.keySet());
        } else {
            // 兜底：单数据源场景
            binder.registerDataSource("default", dataSource);
            log.info("[YudaoMetricsAutoConfiguration] Druid 指标绑定器已注册默认数据源: {}", dataSource.getClass().getSimpleName());
        }
        // P1-4：显式调用 bindTo，确保指标立即注册到 MeterRegistry
        // （MeterRegistryPostProcessor 可能在本 Bean 创建前已处理过 MeterRegistry，导致自动绑定失效）
        binder.bindTo(meterRegistry);
        log.info("[YudaoMetricsAutoConfiguration] Druid 指标已绑定到 MeterRegistry");
        return binder;
    }

}
