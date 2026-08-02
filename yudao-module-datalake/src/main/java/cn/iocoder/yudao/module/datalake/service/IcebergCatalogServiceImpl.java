package cn.iocoder.yudao.module.datalake.service;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.exception.enums.GlobalErrorCodeConstants;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.module.datalake.config.DataLakeProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Iceberg Catalog 管理服务实现
 *
 * <p>通过 Spring {@link RestClient} 调用 Trino REST API（{@code /v1/statement}）发送 SQL 语句，
 * 操作 Iceberg 表。由于 Trino 不在编译期依赖中，此处仅通过 HTTP 协议交互。
 *
 * <h3>Trino REST API 响应结构</h3>
 * <pre>
 * {
 *   "id": "20210817_123456_00001_abcde",
 *   "columns": [{"name": "namespace", "type": "varchar"}, ...],
 *   "data": [["ns1"], ["ns2"]],
 *   "nextUri": "http://trino:8080/v1/statement/.../1",   // 存在表示还有更多结果
 *   "error": null
 * }
 * </pre>
 *
 * <p>实现要点：
 * <ol>
 *   <li>发送 SQL 后，若响应包含 {@code nextUri}，则继续 GET 该 URI 获取后续结果，直到无 nextUri</li>
 *   <li>Trino 不可达时，记录 WARN 日志并返回空结果，避免影响主应用启动</li>
 *   <li>所有查询操作仅用于只读 SELECT，DDL/DML 由调用方（白名单校验）保证安全</li>
 * </ol>
 *
 * @author yudao
 */
@Service
@ConditionalOnProperty(prefix = "yudao.datalake", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
@Slf4j
public class IcebergCatalogServiceImpl implements IcebergCatalogService {

    /**
     * Trino 单次查询的最大分页跟进次数，防止异常情况下死循环
     */
    private static final int MAX_NEXT_PAGES = 100;

    /**
     * SQL 标识符白名单：仅允许「字母/下划线开头 + 字母数字下划线」，长度 ≤ 128。
     *
     * <p>Trino 的 {@code /v1/statement} 接口不支持占位符绑定，命名空间与表名只能字符串拼接，
     * 因此必须在拼接前做严格白名单校验，杜绝 {@code ods; DROP TABLE x --} 之类的注入。
     */
    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]{0,127}$");

    /**
     * 建表列定义片段的合法性校验：必须由小括号包裹，且不得出现语句分隔符与注释符。
     */
    private static final Pattern COLUMN_DEF_FORBIDDEN = Pattern.compile("(;|--|/\\*|\\*/)");

    /**
     * Trino 查询响应超时时间
     */
    private static final Duration QUERY_TIMEOUT = Duration.ofSeconds(30);

    private final DataLakeProperties properties;

    /**
     * 校验 SQL 标识符（命名空间 / 表名），非法直接抛业务异常。
     *
     * @param value 待校验值
     * @param field 字段名（用于错误信息定位）
     * @return 校验通过的原值，便于链式使用
     */
    private static String checkIdentifier(String value, String field) {
        if (value == null || !IDENTIFIER_PATTERN.matcher(value).matches()) {
            throw new ServiceException(GlobalErrorCodeConstants.BAD_REQUEST,
                    String.format("非法的 %s：%s（仅允许字母、数字、下划线，且不能以数字开头）", field, value));
        }
        return value;
    }

    /**
     * 校验建表列定义片段，必须形如 {@code (col1 TYPE, col2 TYPE)}，且不含语句分隔符 / 注释符。
     */
    private static void checkColumnDefinition(String schema) {
        String trimmed = schema == null ? null : schema.trim();
        if (trimmed == null || !trimmed.startsWith("(") || !trimmed.endsWith(")")
                || COLUMN_DEF_FORBIDDEN.matcher(trimmed).find()) {
            throw new ServiceException(GlobalErrorCodeConstants.BAD_REQUEST,
                    "非法的建表列定义，必须为小括号包裹的列声明且不得包含 ; 或注释符");
        }
    }

    /**
     * 执行 Trino SQL 并返回原始响应 Map
     *
     * @param sql SQL 语句
     * @return Trino 响应（包含 columns / data / nextUri / error）
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> executeStatement(String sql) {
        RestClient restClient = RestClient.builder()
                .baseUrl(properties.getCatalogUri())
                .build();
        try {
            ResponseEntity<String> response = restClient.post()
                    .uri("/v1/statement")
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(sql)
                    .retrieve()
                    .toEntity(String.class);
            return JsonUtils.parseObject(response.getBody(), Map.class);
        } catch (Exception e) {
            log.warn("[executeStatement][调用 Trino 失败，sql={}, catalogUri={}, 原因={}]",
                    sql, properties.getCatalogUri(), e.getMessage());
            return Collections.emptyMap();
        }
    }

    /**
     * 跟进 nextUri 获取后续分页结果
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> fetchNext(String nextUri) {
        RestClient restClient = RestClient.builder().build();
        try {
            ResponseEntity<String> response = restClient.get()
                    .uri(nextUri)
                    .retrieve()
                    .toEntity(String.class);
            return JsonUtils.parseObject(response.getBody(), Map.class);
        } catch (Exception e) {
            log.warn("[fetchNext][跟进 Trino nextUri 失败，uri={}, 原因={}]", nextUri, e.getMessage());
            return Collections.emptyMap();
        }
    }

    /**
     * 收集所有分页的 data，合并为一个二维数组
     *
     * @param response 首次响应
     * @return 所有数据行（每行是一个 Object 数组）
     */
    @SuppressWarnings("unchecked")
    private List<List<Object>> collectAllData(Map<String, Object> response) {
        List<List<Object>> allData = new ArrayList<>();
        if (response == null || response.isEmpty()) {
            return allData;
        }
        // 1. 检查错误
        Object error = response.get("error");
        if (error != null) {
            log.warn("[collectAllData][Trino 返回错误: {}]", error);
            return allData;
        }
        // 2. 收集首屏数据
        Object data = response.get("data");
        if (data instanceof List) {
            allData.addAll((List<List<Object>>) data);
        }
        // 3. 跟进 nextUri
        int pageCount = 0;
        Object nextUri = response.get("nextUri");
        while (nextUri instanceof String && pageCount < MAX_NEXT_PAGES) {
            Map<String, Object> nextResponse = fetchNext((String) nextUri);
            if (nextResponse.isEmpty()) {
                break;
            }
            Object nextError = nextResponse.get("error");
            if (nextError != null) {
                log.warn("[collectAllData][Trino 分页返回错误: {}]", nextError);
                break;
            }
            Object nextData = nextResponse.get("data");
            if (nextData instanceof List) {
                allData.addAll((List<List<Object>>) nextData);
            }
            nextUri = nextResponse.get("nextUri");
            pageCount++;
        }
        return allData;
    }

    /**
     * 提取列名列表
     */
    @SuppressWarnings("unchecked")
    private List<String> extractColumnNames(Map<String, Object> response) {
        if (response == null || response.isEmpty()) {
            return Collections.emptyList();
        }
        Object columns = response.get("columns");
        if (!(columns instanceof List)) {
            return Collections.emptyList();
        }
        List<String> names = new ArrayList<>();
        for (Object column : (List<Object>) columns) {
            if (column instanceof Map) {
                Object name = ((Map<String, Object>) column).get("name");
                if (name != null) {
                    names.add(name.toString());
                }
            }
        }
        return names;
    }

    @Override
    public List<String> listNamespaces() {
        Map<String, Object> response = executeStatement("SHOW SCHEMAS FROM iceberg");
        if (response.isEmpty()) {
            return Collections.emptyList();
        }
        List<List<Object>> data = collectAllData(response);
        List<String> namespaces = new ArrayList<>();
        for (List<Object> row : data) {
            if (!row.isEmpty() && row.get(0) != null) {
                namespaces.add(row.get(0).toString());
            }
        }
        log.info("[listNamespaces][查询到命名空间 {} 个: {}]", namespaces.size(), namespaces);
        return namespaces;
    }

    @Override
    public List<String> listTables(String namespace) {
        checkIdentifier(namespace, "namespace");
        Map<String, Object> response = executeStatement("SHOW TABLES FROM iceberg." + namespace);
        if (response.isEmpty()) {
            return Collections.emptyList();
        }
        List<List<Object>> data = collectAllData(response);
        List<String> tables = new ArrayList<>();
        for (List<Object> row : data) {
            if (!row.isEmpty() && row.get(0) != null) {
                tables.add(row.get(0).toString());
            }
        }
        log.info("[listTables][命名空间 {} 查询到表 {} 个: {}]", namespace, tables.size(), tables);
        return tables;
    }

    @Override
    public List<Map<String, Object>> queryTable(String sql) {
        Map<String, Object> response = executeStatement(sql);
        if (response.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> columnNames = extractColumnNames(response);
        List<List<Object>> data = collectAllData(response);
        List<Map<String, Object>> rows = new ArrayList<>(data.size());
        for (List<Object> row : data) {
            Map<String, Object> rowMap = new LinkedHashMap<>();
            for (int i = 0; i < columnNames.size() && i < row.size(); i++) {
                rowMap.put(columnNames.get(i), row.get(i));
            }
            rows.add(rowMap);
        }
        log.info("[queryTable][查询返回 {} 行, {} 列]", rows.size(), columnNames.size());
        return rows;
    }

    @Override
    public void createTable(String namespace, String table, String schema) {
        // 示例 DDL: CREATE TABLE iceberg.ods.mes_pro_work_order (id BIGINT, name VARCHAR)
        // WITH (format = 'PARQUET', location = 's3://yudao-warehouse/ods/mes_pro_work_order')
        checkIdentifier(namespace, "namespace");
        checkIdentifier(table, "table");
        checkColumnDefinition(schema);
        String sql = String.format("CREATE TABLE iceberg.%s.%s %s " +
                        "WITH (format = 'PARQUET', location = '%s/%s/%s')",
                namespace, table, schema, properties.getWarehousePath(), namespace, table);
        Map<String, Object> response = executeStatement(sql);
        if (response.isEmpty()) {
            log.warn("[createTable][创建表失败，可能 Trino 不可达: iceberg.{}.{}]", namespace, table);
            return;
        }
        Object error = response.get("error");
        if (error != null) {
            log.warn("[createTable][创建表失败: iceberg.{}.{}, 错误: {}]", namespace, table, error);
        } else {
            log.info("[createTable][创建表成功: iceberg.{}.{}]", namespace, table);
        }
    }

    @Override
    public Map<String, String> getTableSchema(String namespace, String table) {
        checkIdentifier(namespace, "namespace");
        checkIdentifier(table, "table");
        String sql = String.format("SHOW COLUMNS FROM iceberg.%s.%s", namespace, table);
        Map<String, Object> response = executeStatement(sql);
        if (response.isEmpty()) {
            return Collections.emptyMap();
        }
        List<List<Object>> data = collectAllData(response);
        Map<String, String> schema = new LinkedHashMap<>();
        for (List<Object> row : data) {
            // SHOW COLUMNS 返回: Column | Type | Extra | Comment
            if (row.size() >= 2 && row.get(0) != null && row.get(1) != null) {
                schema.put(row.get(0).toString(), row.get(1).toString());
            }
        }
        log.info("[getTableSchema][表 iceberg.{}.{} 共 {} 列]", namespace, table, schema.size());
        return schema;
    }

    @Override
    public Map<String, Object> executeUpdate(String sql) {
        Map<String, Object> result = new HashMap<>();
        Map<String, Object> response = executeStatement(sql);
        if (response.isEmpty()) {
            result.put("success", false);
            result.put("updateCount", 0L);
            result.put("error", "Trino 不可达或响应为空");
            return result;
        }
        Object error = response.get("error");
        if (error != null) {
            result.put("success", false);
            result.put("updateCount", 0L);
            result.put("error", error.toString());
            return result;
        }
        result.put("success", true);
        result.put("updateCount", extractUpdateCount(response));
        result.put("error", null);
        log.info("[executeUpdate][执行成功：updateCount={}]", result.get("updateCount"));
        return result;
    }

    /**
     * 从 Trino DML 响应中提取影响行数
     *
     * <p>Trino INSERT/DELETE 响应结构：{@code {"updateType":"DELETE","stats":{"processedRows":N}}}
     */
    @SuppressWarnings("unchecked")
    private long extractUpdateCount(Map<String, Object> response) {
        Object stats = response.get("stats");
        if (stats instanceof Map) {
            Object processedRows = ((Map<String, Object>) stats).get("processedRows");
            if (processedRows instanceof Number) {
                return ((Number) processedRows).longValue();
            }
        }
        return 0L;
    }
}
