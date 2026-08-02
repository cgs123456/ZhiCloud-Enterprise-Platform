package cn.iocoder.yudao.module.datalake.service;

import cn.iocoder.yudao.framework.common.exception.enums.GlobalErrorCodeConstants;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.module.datalake.config.DataLakeProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.regex.Pattern;

/**
 * 历史数据归档服务实现
 *
 * <p>通过 Trino REST API 执行 {@code INSERT INTO iceberg.${tableName} SELECT * FROM mysql.${tableName}
 * WHERE create_time < ?} 将 MySQL 业务库冷数据归档到 Iceberg 数据湖仓，归档成功后删除源表已归档数据。
 *
 * <p>归档状态记录在内存 {@link ConcurrentHashMap} 中（{@link #archiveStatusStore}），
 * 进程重启后状态丢失。生产环境建议替换为 Redis 或 MySQL 持久化存储（如 erp_archive_log 表）。
 *
 * <p>调度通过 Spring {@link TaskScheduler} + {@link CronTrigger} 实现，按 cron 表达式定时触发归档。
 * 若 Spring 上下文未提供 {@link TaskScheduler} Bean，{@link #scheduleArchive} 抛出 {@link IllegalStateException}。
 *
 * @author yudao
 */
@Service
@ConditionalOnProperty(prefix = "yudao.datalake", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class DataArchivalServiceImpl implements DataArchivalService {

    /**
     * 表名白名单正则：仅允许字母/数字/下划线，防止 ${} 拼接 SQL 注入
     */
    private static final Pattern TABLE_NAME_PATTERN = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$");

    /**
     * 归档状态存储：tableName -> 状态信息
     *
     * <p>线程安全 Map，进程级缓存。生产环境建议替换为 Redis 或 MySQL erp_archive_log 表。
     */
    private final Map<String, Map<String, Object>> archiveStatusStore = new ConcurrentHashMap<>();

    /**
     * 调度任务存储：taskId -> ScheduledFuture，可用于取消调度
     */
    private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    private final DataLakeProperties properties;

    private final IcebergCatalogService icebergCatalogService;

    private final ObjectProvider<TaskScheduler> taskSchedulerProvider;

    @Override
    public String archiveTable(String tableName, LocalDate beforeDate) {
        // 1. 输入校验：表名白名单，防止 ${} 拼接注入
        validateTableName(tableName);
        if (beforeDate == null) {
            throw new IllegalArgumentException("beforeDate 不能为空");
        }

        // 2. 生成归档任务 ID 并初始化 RUNNING 状态
        String archiveId = "archive-" + UUID.randomUUID().toString().substring(0, 8);
        Map<String, Object> status = new HashMap<>();
        status.put("tableName", tableName);
        status.put("status", "RUNNING");
        status.put("archiveId", archiveId);
        status.put("beforeDate", beforeDate.toString());
        status.put("archivedRows", 0);
        status.put("startTime", LocalDateTime.now().toString());
        status.put("message", "归档任务已启动");
        archiveStatusStore.put(tableName, status);

        // 3. 构建 SQL：通过 Trino 将 MySQL 冷数据归档到 Iceberg，再删除源表已归档数据
        String insertSql = String.format(
                "INSERT INTO iceberg.%s SELECT * FROM mysql.%s WHERE create_time < DATE '%s'",
                tableName, tableName, beforeDate.toString());
        String deleteSql = String.format(
                "DELETE FROM mysql.%s WHERE create_time < DATE '%s'",
                tableName, beforeDate.toString());

        try {
            // 3.1 执行 INSERT 归档（通过 IcebergCatalogService 调用 Trino REST API）
            Map<String, Object> insertResult = icebergCatalogService.executeUpdate(insertSql);
            if (!Boolean.TRUE.equals(insertResult.get("success"))) {
                markFailed(tableName, "INSERT 归档失败：" + insertResult.get("error"));
                return archiveId;
            }

            // 3.2 删除源表已归档数据
            Map<String, Object> deleteResult = icebergCatalogService.executeUpdate(deleteSql);
            if (!Boolean.TRUE.equals(deleteResult.get("success"))) {
                markFailed(tableName, "DELETE 源表数据失败：" + deleteResult.get("error"));
                return archiveId;
            }

            // 3.3 更新状态为 SUCCESS
            long archivedRows = toLong(deleteResult.get("updateCount"));
            status.put("status", "SUCCESS");
            status.put("archivedRows", archivedRows);
            status.put("lastArchiveTime", LocalDateTime.now().toString());
            status.put("endTime", LocalDateTime.now().toString());
            status.put("message", "归档成功");
            log.info("[archiveTable][归档成功：table={}, beforeDate={}, archivedRows={}]",
                    tableName, beforeDate, archivedRows);
            return archiveId;
        } catch (Exception e) {
            markFailed(tableName, "归档异常：" + e.getMessage());
            log.error("[archiveTable][归档异常：table={}, beforeDate={}]", tableName, beforeDate, e);
            return archiveId;
        }
    }

    @Override
    public Map<String, Object> getArchiveStatus(String tableName) {
        return archiveStatusStore.getOrDefault(tableName, buildUnknownStatus(tableName));
    }

    @Override
    public String scheduleArchive(String tableName, String cron) {
        // 1. 输入校验
        validateTableName(tableName);
        if (cron == null || cron.trim().isEmpty()) {
            throw new IllegalArgumentException("cron 表达式不能为空");
        }

        // 2. 获取 TaskScheduler（可能未启用）
        TaskScheduler taskScheduler = taskSchedulerProvider.getIfAvailable();
        if (taskScheduler == null) {
            log.warn("[scheduleArchive][Spring TaskScheduler 不可用，无法注册调度任务：table={}, cron={}]",
                    tableName, cron);
            throw new ServiceException(GlobalErrorCodeConstants.INTERNAL_SERVER_ERROR, "Spring TaskScheduler 不可用，请确认 @EnableScheduling 已启用");
        }

        // 3. 校验 cron 表达式
        CronTrigger trigger;
        try {
            trigger = new CronTrigger(cron);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("非法 cron 表达式：" + cron, e);
        }

        // 4. 注册调度任务：按 retentionDays 计算 beforeDate，定时触发 archiveTable
        String taskId = "schedule-" + UUID.randomUUID().toString().substring(0, 8);
        LocalDate beforeDate = LocalDate.now().minusDays(properties.getRetentionDays());
        ScheduledFuture<?> future = taskScheduler.schedule(
                () -> archiveTable(tableName, beforeDate),
                trigger);
        scheduledTasks.put(taskId, future);

        log.info("[scheduleArchive][已注册归档调度：taskId={}, table={}, cron={}, beforeDate={}]",
                taskId, tableName, cron, beforeDate);
        return taskId;
    }

    // ============ 内部辅助方法 ============

    /**
     * 校验表名：仅允许字母/数字/下划线，防止 ${} 拼接 SQL 注入
     */
    private void validateTableName(String tableName) {
        if (tableName == null || !TABLE_NAME_PATTERN.matcher(tableName).matches()) {
            throw new IllegalArgumentException("非法表名：" + tableName + "，仅允许字母/数字/下划线");
        }
    }

    /**
     * 标记归档失败
     */
    private void markFailed(String tableName, String message) {
        Map<String, Object> status = archiveStatusStore.get(tableName);
        if (status == null) {
            status = new HashMap<>();
            status.put("tableName", tableName);
            archiveStatusStore.put(tableName, status);
        }
        status.put("status", "FAILED");
        status.put("endTime", LocalDateTime.now().toString());
        status.put("message", message);
        log.warn("[markFailed][归档失败：table={}, message={}]", tableName, message);
    }

    /**
     * 构建未知状态（未查询到归档记录）
     */
    private Map<String, Object> buildUnknownStatus(String tableName) {
        Map<String, Object> status = new HashMap<>();
        status.put("tableName", tableName);
        status.put("status", "UNKNOWN");
        status.put("lastArchiveTime", null);
        status.put("archivedRows", 0);
        status.put("message", "未查询到归档记录");
        return status;
    }

    /**
     * 安全转换为 long
     */
    private long toLong(Object value) {
        return value instanceof Number ? ((Number) value).longValue() : 0L;
    }

}