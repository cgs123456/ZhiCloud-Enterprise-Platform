/**
 * 数据湖仓模块（Apache Iceberg + Trino）
 *
 * <p>Spring Modulith 模块声明（A3）。本模块为可选模块，默认关闭（zhicloud.datalake.enabled=false）。
 *
 * <p>核心定位：
 * <ul>
 *   <li>历史数据冷归档：MySQL 热数据 → Flink CDC / 批量 ETL → Iceberg 冷数据</li>
 *   <li>AI 训练数据源：通过 MCP 工具暴露数据湖查询能力给 AI Agent</li>
 *   <li>BI 分析加速：Trino 分布式查询，避免重查询压垮业务库</li>
 * </ul>
 *
 * <p>架构约束：
 * <ul>
 *   <li>Iceberg/Trino/Flink 不作为编译期依赖，运行时通过 REST API 与独立部署的服务交互</li>
 *   <li>本模块仅做配置管理、查询代理（Trino REST API）、MCP 工具暴露</li>
 *   <li>默认关闭（enabled=false），不影响现有功能</li>
 * </ul>
 *
 * @author zhicloud
 */
@org.springframework.modulith.ApplicationModule(displayName = "数据湖仓模块（Iceberg + Trino）")
package cn.zhicloud.module.datalake;
