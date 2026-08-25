package cn.zhicloud.module.datalake.service;

import java.time.LocalDate;
import java.util.Map;

/**
 * 历史数据归档服务
 *
 * <p>定义将 MySQL 业务库中的冷数据归档到 Iceberg 数据湖仓的策略接口。
 *
 * <h3>归档执行方</h3>
 * <p>实际的归档操作（数据迁移）应由独立的 Flink CDC 任务或批量 ETL 任务完成，
 * 而非本服务直接执行。本服务提供：
 * <ul>
 *   <li>归档策略定义（保留天数、批处理大小、归档表清单）</li>
 *   <li>归档状态查询（占位实现，未来对接归档任务的状态存储）</li>
 *   <li>归档调度触发（占位实现，未来对接调度系统）</li>
 * </ul>
 *
 * <p>本接口作为 SPI（Service Provider Interface），未来可通过实现该接口接入
 * Flink CDC、DataX、Spark 等不同的归档引擎。
 *
 * @author zhicloud
 */
public interface DataArchivalService {

    /**
     * 归档指定日期之前的业务数据到 Iceberg
     *
     * <p>当前为占位实现：仅记录日志，不实际迁移数据。
     * 实际归档由 Flink CDC 或批量 ETL 任务完成。
     *
     * @param tableName  业务表名（如 mes_pro_work_order）
     * @param beforeDate 归档此日期之前的数据
     * @return 归档任务标识（当前占位返回 null）
     */
    String archiveTable(String tableName, LocalDate beforeDate);

    /**
     * 查询归档状态
     *
     * @param tableName 业务表名
     * @return 归档状态信息（包含状态、最后归档时间、已归档行数等）
     */
    Map<String, Object> getArchiveStatus(String tableName);

    /**
     * 调度归档任务
     *
     * <p>当前为占位实现：仅记录调度配置，不实际触发调度。
     * 未来对接调度系统（如 XXL-Job / zhicloud-spring-boot-starter-job）。
     *
     * @param tableName 业务表名
     * @param cron      Cron 表达式（如 {@code 0 0 2 * * ?} 每天 2 点执行）
     * @return 调度任务标识
     */
    String scheduleArchive(String tableName, String cron);

}
