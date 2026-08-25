package cn.zhicloud.module.datalake.config;

import cn.zhicloud.module.datalake.mcp.DataLakeMcpTool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 数据湖仓模块自动配置
 *
 * <p>通过 {@code zhicloud.datalake.enabled=true} 控制模块是否启用。
 * 默认关闭（enabled=false），不影响现有功能。
 *
 * <h3>启用条件</h3>
 * <ol>
 *   <li>独立部署 Trino + MinIO（S3 兼容存储）</li>
 *   <li>配置 Trino Iceberg catalog 指向 MinIO</li>
 *   <li>设置 {@code zhicloud.datalake.enabled=true}</li>
 * </ol>
 *
 * <h3>加载的 Bean</h3>
 * <ul>
 *   <li>{@link DataLakeProperties}：配置属性（始终加载，无论 enabled 与否）</li>
 *   <li>{@code IcebergCatalogServiceImpl}：Iceberg Catalog 管理服务（enabled=true 时加载，通过 @Service 注解）</li>
 *   <li>{@code DataArchivalServiceImpl}：历史数据归档服务（enabled=true 时加载，通过 @Service 注解）</li>
 *   <li>{@code DataLakeMcpTool}：MCP 工具暴露（enabled=true 时加载，通过 @Component 注解）</li>
 *   <li>{@code dataLakeToolCallbacks}：把 MCP 工具适配为 Spring AI ToolCallback 数组</li>
 * </ul>
 *
 * <p>注意：Service 和 MCP Tool 类使用 {@code @ConditionalOnProperty} 守护，
 * 仅当 enabled=true 时才会被 Spring 容器扫描并实例化。
 *
 * @author zhicloud
 */
@Configuration
@EnableConfigurationProperties(DataLakeProperties.class)
@ConditionalOnProperty(prefix = "zhicloud.datalake", name = "enabled", havingValue = "true")
@Slf4j
public class DataLakeAutoConfiguration {

    public DataLakeAutoConfiguration(DataLakeProperties properties) {
        log.info("[DataLakeAutoConfiguration][初始化数据湖仓模块：catalogUri={}, warehousePath={}, retentionDays={}, archiveTables={}]",
                properties.getCatalogUri(), properties.getWarehousePath(),
                properties.getRetentionDays(), properties.getArchiveTables());
    }

    /**
     * 将 {@link DataLakeMcpTool} 暴露的 {@code @Tool} 方法注册为 Spring AI {@link ToolCallback}，
     * 供 AI Agent（ReAct / Multi-Agent）在启用数据湖仓模块时自动发现并调用。
     *
     * <p><b>类型选择</b>：以 {@code ToolCallback[]} 类型暴露，避免与 zhicloud-module-ai 中
     * {@code List<ToolCallback>} 类型的 {@code toolCallbacks} Bean 产生类型冲突。
     * AiAutoConfiguration 通过 {@code ObjectProvider<ToolCallback[]>} 安全聚合其它模块暴露的工具数组。
     *
     * <p><b>可选注入</b>：{@link DataLakeMcpTool} 上带有 {@code @ConditionalOnBean(IcebergCatalogService.class)}，
     * 即便 {@code enabled=true}，若 Iceberg Catalog 服务未就绪该 Bean 仍可能缺席，
     * 因此使用 {@link ObjectProvider} 惰性获取，缺席时返回空数组而非抛出 NoSuchBeanDefinitionException。
     */
    @Bean
    public ToolCallback[] dataLakeToolCallbacks(ObjectProvider<DataLakeMcpTool> dataLakeMcpToolProvider) {
        DataLakeMcpTool tool = dataLakeMcpToolProvider.getIfAvailable();
        if (tool == null) {
            log.info("[DataLakeAutoConfiguration][DataLakeMcpTool 未就绪，跳过 MCP 工具注册]");
            return new ToolCallback[0];
        }
        ToolCallback[] callbacks = ToolCallbacks.from(tool);
        log.info("[DataLakeAutoConfiguration][注册数据湖仓 MCP 工具成功：count={}]", callbacks.length);
        return callbacks;
    }

}
