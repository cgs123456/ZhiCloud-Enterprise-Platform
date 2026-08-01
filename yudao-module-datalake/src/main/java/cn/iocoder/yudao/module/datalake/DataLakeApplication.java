package cn.iocoder.yudao.module.datalake;

/**
 * 数据湖仓模块入口标记类。
 *
 * <p>本模块作为 yudao 项目的可选模块，本身不提供独立的 Spring Boot 启动入口，
 * 由 yudao-server 主应用通过 ComponentScan 扫描 {@code cn.iocoder.yudao.module.datalake} 包加载。
 *
 * <p>核心能力：
 * <ol>
 *   <li>Iceberg Catalog 管理（命名空间、表、Schema）—— 通过 Trino REST API 代理</li>
 *   <li>历史数据归档策略定义（占位实现，实际由 Flink CDC / 批量 ETL 完成）</li>
 *   <li>MCP 工具暴露：数据湖表查询、归档状态查询，供 AI Agent 调用</li>
 * </ol>
 *
 * <p>启用方式：
 * <pre>
 * yudao:
 *   datalake:
 *     enabled: true
 *     catalog-uri: http://trino:8080
 *     warehouse-path: s3://yudao-warehouse
 * </pre>
 *
 * @author yudao
 */
public class DataLakeApplication {
}
