package cn.iocoder.yudao.module.datalake.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 数据湖仓模块自动配置
 *
 * <p>通过 {@code yudao.datalake.enabled=true} 控制模块是否启用。
 * 默认关闭（enabled=false），不影响现有功能。
 *
 * <h3>启用条件</h3>
 * <ol>
 *   <li>独立部署 Trino + MinIO（S3 兼容存储）</li>
 *   <li>配置 Trino Iceberg catalog 指向 MinIO</li>
 *   <li>设置 {@code yudao.datalake.enabled=true}</li>
 * </ol>
 *
 * <h3>加载的 Bean</h3>
 * <ul>
 *   <li>{@link DataLakeProperties}：配置属性（始终加载，无论 enabled 与否）</li>
 *   <li>{@code IcebergCatalogServiceImpl}：Iceberg Catalog 管理服务（enabled=true 时加载，通过 @Service 注解）</li>
 *   <li>{@code DataArchivalServiceImpl}：历史数据归档服务（enabled=true 时加载，通过 @Service 注解）</li>
 *   <li>{@code DataLakeMcpTool}：MCP 工具暴露（enabled=true 时加载，通过 @Component 注解）</li>
 * </ul>
 *
 * <p>注意：Service 和 MCP Tool 类使用 {@code @ConditionalOnProperty} 守护，
 * 仅当 enabled=true 时才会被 Spring 容器扫描并实例化。
 *
 * @author yudao
 */
@Configuration
@EnableConfigurationProperties(DataLakeProperties.class)
@ConditionalOnProperty(prefix = "yudao.datalake", name = "enabled", havingValue = "true")
@Slf4j
public class DataLakeAutoConfiguration {

    public DataLakeAutoConfiguration(DataLakeProperties properties) {
        log.info("[DataLakeAutoConfiguration][初始化数据湖仓模块：catalogUri={}, warehousePath={}, retentionDays={}, archiveTables={}]",
                properties.getCatalogUri(), properties.getWarehousePath(),
                properties.getRetentionDays(), properties.getArchiveTables());
    }

}
