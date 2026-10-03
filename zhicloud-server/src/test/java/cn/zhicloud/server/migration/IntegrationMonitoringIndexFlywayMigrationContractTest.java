package cn.zhicloud.server.migration;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * V104 可靠集成监控查询索引迁移契约测试。
 *
 * <p>Outbox/Inbox 的最老滞留查询分别按 status + occurred_at / received_at 取最小值。
 * 没有对应索引时，待处理积压越大，Prometheus 每次抓取触发的扫描越重，
 * 会形成「积压导致监控查询拖慢数据库、数据库变慢又放大积压」的正反馈。
 */
class IntegrationMonitoringIndexFlywayMigrationContractTest {

    private static final String MIGRATION = "db/migration/V104__integration_monitoring_indexes.sql";
    private static final String VERIFICATION = "db/verification/V104_verify.sql";

    @Test
    void migrationMustAddIndexesForPendingAgeQueries() {
        String sql = normalize(readRequiredResource(MIGRATION));

        assertContainsAll(sql,
                "information_schema.statistics",
                "alter table `event_outbox` add index `idx_status_occurred` (`status`, `occurred_at`), algorithm=inplace, lock=none",
                "alter table `event_inbox` add index `idx_status_received` (`status`, `received_at`), algorithm=inplace, lock=none",
                "prepare",
                "execute",
                "deallocate prepare");

        assertFalse(sql.contains("drop table"), "V104 不得删除表");
        assertFalse(sql.contains("delete from"), "V104 不得删除事件数据");
        assertFalse(sql.contains("update event_"), "V104 不得改写事件状态");
        assertFalse(sql.contains("insert into"), "V104 不得写入事件数据");
        assertFalse(sql.contains("truncate"), "V104 不得清表");
        assertFalse(sql.contains("drop index"), "V104 只做加法，不得删除既有索引");
    }

    @Test
    void verificationMustGateBothIndexesAndBeReadOnly() {
        String sql = normalize(readRequiredResource(VERIFICATION));

        assertContainsAll(sql,
                "tmp_v104_verify_expected_index",
                "information_schema.statistics",
                "group_concat(column_name order by seq_in_index)",
                "'event_outbox'",
                "'idx_status_occurred'",
                "'occurred_at'",
                "'event_inbox'",
                "'idx_status_received'",
                "'received_at'",
                "index_verdict",
                "v104_verify_verdict",
                "'pass'",
                "'fail'",
                "'missing'");

        assertFalse(sql.contains("alter table"), "校验脚本不得变更结构");
        assertFalse(sql.contains("delete from"), "校验脚本不得删除事件数据");
        assertFalse(sql.contains("update event_"), "校验脚本不得修改事件数据");
        assertFalse(sql.contains("insert into"), "校验脚本不得写入事件数据");
    }

    private static String readRequiredResource(String path) {
        try (InputStream input = IntegrationMonitoringIndexFlywayMigrationContractTest.class
                .getClassLoader().getResourceAsStream(path)) {
            assertNotNull(input, "缺少 Flyway 资源: " + path);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new AssertionError("读取 Flyway 资源失败: " + path, ex);
        }
    }

    private static String normalize(String sql) {
        return sql.replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT)
                .trim();
    }

    private static void assertContainsAll(String actual, String... expected) {
        List<String> missing = Arrays.stream(expected)
                .filter(value -> !actual.contains(value))
                .toList();
        assertTrue(missing.isEmpty(), () -> "迁移契约缺少: " + missing);
    }
}
