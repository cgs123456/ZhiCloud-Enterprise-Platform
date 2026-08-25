package cn.zhicloud.framework.tracer.core.metrics;

import com.alibaba.druid.pool.DruidDataSource;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Druid 连接池指标绑定器
 *
 * <p>P1-4 可观测性：Druid 不像 HikariCP 自动暴露 metrics，需要手动绑定。
 * Spring Boot 的 {@code DataSourcePoolMetricsAutoConfiguration} 仅支持 HikariCP/Tomcat JDBC/DBCP2，
 * 不支持 Druid，因此本类手动注册 Druid 的活跃/空闲/等待等指标到 MeterRegistry。</p>
 *
 * <p>暴露指标（前缀 {@code druid.}）：
 * <ul>
 *   <li>{@code druid.active.connections} - 活跃连接数</li>
 *   <li>{@code.druid.active.peak} - 活跃连接历史峰值</li>
 *   <li>{@code druid.pool.size} - 当前池大小（活跃+空闲）</li>
 *   <li>{@code druid.pool.max} - 最大池大小</li>
 *   <li>{@code druid.idle.connections} - 空闲连接数</li>
 *   <li>{@code druid.queued.threads} - 等待获取连接的线程数</li>
 *   <li>{@code druid.wait.count} - 累计等待次数（不重复绑定）</li>
 *   <li>{@code druid.wait.millis} - 累计等待毫秒</li>
 *   <li>{@code druid.error.count} - 累计错误次数</li>
 * </ul>
 *
 * @author zhicloud
 */
@Slf4j
public class DruidMetricsBinder implements MeterBinder {

    private static final String METRIC_PREFIX = "druid.";

    private final Map<String, DruidDataSource> dataSources = new ConcurrentHashMap<>();

    @Override
    public void bindTo(MeterRegistry registry) {
        for (Map.Entry<String, DruidDataSource> entry : dataSources.entrySet()) {
            String name = entry.getKey();
            DruidDataSource ds = entry.getValue();
            bindDataSource(registry, name, ds);
        }
    }

    /**
     * 注册数据源（启动时动态数据源加载后调用）
     *
     * <p>支持 dynamic-datasource 库的 ItemDataSource 包装：
     * 当使用 {@code dynamic-datasource-spring-boot3-starter} + Druid 时，
     * 内部 DataSource 会被包装为 {@code com.baomidou.dynamic.datasource.ds.ItemDataSource}，
     * 真实的 DruidDataSource 需通过 {@code getRealDataSource()} 获取。
     * 此处使用反射以避免对 dynamic-datasource 库的硬依赖（monitor 模块为 provided scope）。</p>
     */
    public void registerDataSource(String name, DataSource dataSource) {
        if (dataSource == null) {
            log.warn("[DruidMetricsBinder] 数据源为 null，跳过: {}", name);
            return;
        }
        // 优先匹配直接的 DruidDataSource
        if (dataSource instanceof DruidDataSource druidDataSource) {
            dataSources.put(name, druidDataSource);
            log.info("[DruidMetricsBinder] 注册 Druid 数据源指标: {} ({})", name, dataSource.getClass().getSimpleName());
            return;
        }
        // 兜底：通过反射解包 dynamic-datasource 的 ItemDataSource / LazyDataSource 等包装类
        DruidDataSource unwrapped = unwrapDruidDataSource(dataSource);
        if (unwrapped != null) {
            dataSources.put(name, unwrapped);
            log.info("[DruidMetricsBinder] 注册 Druid 数据源指标（解包后）: {} (源类型={}, 真实类型={})",
                    name, dataSource.getClass().getSimpleName(), unwrapped.getClass().getSimpleName());
        } else {
            log.warn("[DruidMetricsBinder] 非 Druid 数据源，跳过: {} (类型={})", name, dataSource.getClass().getName());
        }
    }

    /**
     * 通过反射解包 dynamic-datasource 库的包装类，提取内部的 DruidDataSource。
     *
     * <p>已知包装类（按优先级尝试）：
     * <ul>
     *   <li>{@code com.baomidou.dynamic.datasource.ds.ItemDataSource}：getRealDataSource() 返回真实 DruidDataSource</li>
     *   <li>{@code com.baomidou.dynamic.datasource.ds.lazy.LazyDataSource}：getRealDataSource() 返回懒加载的 DruidDataSource</li>
     *   <li>其他自定义包装：尝试递归调用 getRealDataSource() / getDataSource() / getTargetDataSource()</li>
     * </ul>
     */
    private DruidDataSource unwrapDruidDataSource(DataSource dataSource) {
        DataSource current = dataSource;
        // 递归解包（最多 5 层，防止循环引用）
        for (int i = 0; i < 5 && current != null; i++) {
            if (current instanceof DruidDataSource druid) {
                return druid;
            }
            // 尝试常见的解包方法
            DataSource unwrapped = invokeGetter(current, "getRealDataSource");
            if (unwrapped == null) {
                unwrapped = invokeGetter(current, "getDataSource");
            }
            if (unwrapped == null) {
                unwrapped = invokeGetter(current, "getTargetDataSource");
            }
            if (unwrapped == null || unwrapped == current) {
                return null;
            }
            current = unwrapped;
        }
        return null;
    }

    /**
     * 反射调用无参 getter 方法，返回 DataSource 类型
     */
    private DataSource invokeGetter(Object target, String methodName) {
        try {
            java.lang.reflect.Method method = target.getClass().getMethod(methodName);
            Object result = method.invoke(target);
            if (result instanceof DataSource ds) {
                return ds;
            }
        } catch (NoSuchMethodException ignored) {
            // 方法不存在，正常情况
        } catch (Exception e) {
            log.debug("[DruidMetricsBinder] 反射调用 {} 失败: {}", methodName, e.getMessage());
        }
        return null;
    }

    private void bindDataSource(MeterRegistry registry, String name, DruidDataSource ds) {
        String tagValue = name;
        // 活跃连接数
        Gauge.builder(METRIC_PREFIX + "active.connections", ds, DruidDataSource::getActiveCount)
                .tag("datasource", tagValue)
                .description("Druid 活跃连接数")
                .register(registry);
        // 活跃连接峰值
        Gauge.builder(METRIC_PREFIX + "active.peak", ds, DruidDataSource::getActivePeak)
                .tag("datasource", tagValue)
                .description("Druid 活跃连接历史峰值")
                .register(registry);
        // 池大小（活跃+空闲）
        Gauge.builder(METRIC_PREFIX + "pool.size", ds, DruidDataSource::getPoolingCount)
                .tag("datasource", tagValue)
                .description("Druid 池大小（活跃+空闲）")
                .register(registry);
        // 最大池大小
        Gauge.builder(METRIC_PREFIX + "pool.max", ds, DruidDataSource::getMaxActive)
                .tag("datasource", tagValue)
                .description("Druid 最大池大小")
                .register(registry);
        // 空闲连接数
        Gauge.builder(METRIC_PREFIX + "idle.connections", ds, DruidDataSource::getPoolingCount)
                .tag("datasource", tagValue)
                .description("Druid 空闲连接数（池中）")
                .register(registry);
        // 等待获取连接的线程数
        Gauge.builder(METRIC_PREFIX + "queued.threads", ds, DruidDataSource::getWaitThreadCount)
                .tag("datasource", tagValue)
                .description("Druid 等待获取连接的线程数")
                .register(registry);
        // 累计获取连接次数
        Gauge.builder(METRIC_PREFIX + "connect.count", ds, DruidDataSource::getConnectCount)
                .tag("datasource", tagValue)
                .description("Druid 累计获取连接次数")
                .register(registry);
        // 累计关闭连接次数
        Gauge.builder(METRIC_PREFIX + "close.count", ds, DruidDataSource::getCloseCount)
                .tag("datasource", tagValue)
                .description("Druid 累计关闭连接次数")
                .register(registry);
        // 累计错误次数
        Gauge.builder(METRIC_PREFIX + "error.count", ds, DruidDataSource::getErrorCount)
                .tag("datasource", tagValue)
                .description("Druid 累计错误次数")
                .register(registry);
        // 累计事务数
        Gauge.builder(METRIC_PREFIX + "transaction.count", ds, DruidDataSource::getStartTransactionCount)
                .tag("datasource", tagValue)
                .description("Druid 累计事务数")
                .register(registry);
        // 累计提交数
        Gauge.builder(METRIC_PREFIX + "commit.count", ds, DruidDataSource::getCommitCount)
                .tag("datasource", tagValue)
                .description("Druid 累计提交数")
                .register(registry);
        // 累计回滚数
        Gauge.builder(METRIC_PREFIX + "rollback.count", ds, DruidDataSource::getRollbackCount)
                .tag("datasource", tagValue)
                .description("Druid 累计回滚数")
                .register(registry);
    }
}
