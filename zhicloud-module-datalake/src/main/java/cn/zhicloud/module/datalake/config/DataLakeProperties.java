package cn.zhicloud.module.datalake.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Collections;
import java.util.List;

/**
 * 数据湖仓配置属性
 *
 * <p>对应配置前缀 {@code zhicloud.datalake}。默认关闭（{@link #enabled} = false），
 * 启用后才会加载相关 Bean（IcebergCatalogService、DataArchivalService、DataLakeMcpTool 等）。
 *
 * <h3>配置示例</h3>
 * <pre>
 * zhicloud:
 *   datalake:
 *     enabled: true
 *     catalog-uri: http://trino:8080
 *     warehouse-path: s3://zhicloud-warehouse
 *     retention-days: 365
 *     batch-size: 10000
 *     archive-tables:
 *       - mes_pro_work_order
 *       - wms_inventory_log
 *       - erp_gl_voucher
 * </pre>
 *
 * <h3>环境变量注入</h3>
 * <ul>
 *   <li>{@code DATALAKE_ENABLED}：是否启用</li>
 *   <li>{@code DATALAKE_CATALOG_URI}：Trino / Iceberg REST Catalog 地址</li>
 *   <li>{@code DATALAKE_WAREHOUSE_PATH}：归档存储路径（S3/HDFS）</li>
 * </ul>
 *
 * @author zhicloud
 */
@ConfigurationProperties(prefix = "zhicloud.datalake")
@Data
public class DataLakeProperties {

    /**
     * 是否启用数据湖仓模块（默认关闭）
     *
     * <p>启用条件：需独立部署 Trino + MinIO（S3 兼容存储）。未部署时保持关闭，
     * 避免启动期因连接 Trino 失败而报错。
     */
    private Boolean enabled = false;

    /**
     * Iceberg REST Catalog URI
     *
     * <p>通常为 Trino 的 HTTP 端点（{@code http://trino:8080}），
     * 通过 {@code /v1/statement} 接口发送 SQL 语句。
     */
    private String catalogUri = "http://trino:8080";

    /**
     * 归档存储路径（S3/HDFS）
     *
     * <p>Iceberg 表数据的物理存储位置。生产环境建议使用 S3 或 MinIO。
     */
    private String warehousePath = "s3://zhicloud-warehouse";

    /**
     * 默认归档保留天数
     *
     * <p>超过此天数的业务数据视为冷数据，可归档到 Iceberg。
     */
    private Integer retentionDays = 365;

    /**
     * 归档批处理大小
     *
     * <p>Flink CDC 或批量 ETL 单次处理的记录数。
     */
    private Integer batchSize = 10000;

    /**
     * 需要归档的表列表
     *
     * <p>配置后，归档调度任务会按表名依次执行归档。
     * 表名需与业务库（MySQL）中的表名一致。
     */
    private List<String> archiveTables = Collections.emptyList();

}
