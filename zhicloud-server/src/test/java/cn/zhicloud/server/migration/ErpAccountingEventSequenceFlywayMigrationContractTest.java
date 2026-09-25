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
 * ERP 会计事件期次 Flyway 迁移契约测试。
 *
 * <p>V96 只负责把 erp_accounting_event 的来源唯一键扩展为包含 sequence_no，
 * 使部分开票/收款/付款能够按期次落地独立事件。迁移不得伪造任何历史会计事件，
 * 也不得回填历史事件期次：历史事件的 sequence_no 取默认值 0（主事件）。</p>
 */
class ErpAccountingEventSequenceFlywayMigrationContractTest {

    private static final String MIGRATION = "db/migration/V96__erp_accounting_event_sequence.sql";
    private static final String VERIFICATION = "db/verification/V96_verify.sql";

    @Test
    void migrationMustAddSequenceColumnAndExtendSourceUniqueKeyIdempotently() {
        String sql = normalize(readRequiredResource(MIGRATION));

        assertContainsAll(sql,
                "erp_accounting_event",
                "sequence_no",
                "int not null default 0",
                "期次",
                "p_v96_add_column_if_not_exists",
                "information_schema.columns",
                "information_schema.tables",
                "signal sqlstate '45000'");

        assertContainsAll(sql,
                "p_v96_replace_source_unique_key",
                "uk_erp_accounting_event_source",
                "information_schema.statistics",
                "group_concat(column_name order by seq_in_index)",
                "locate('sequence_no', v_columns)",
                "drop index `uk_erp_accounting_event_source`",
                "add unique key `uk_erp_accounting_event_source`",
                "concat(",
                "prepare v96_key_stmt",
                "deallocate prepare v96_key_stmt");

        // 期次必须进入来源唯一键，否则第二次部分结算仍会被唯一键拒绝。
        assertContainsAll(sql,
                "`tenant_id`, `event_type`, `source_doc_type`, `source_doc_id`, '",
                "'`source_doc_line_id`, `source_revision`, `sequence_no`, `reversal_of_event_id`)");

        assertFalse(sql.contains("insert into erp_accounting_event"),
                "期次迁移不得伪造历史会计事件");
        assertFalse(sql.contains("update erp_accounting_event"),
                "期次迁移不得回填历史事件期次");
        assertFalse(sql.contains("drop table"),
                "期次迁移不得删除业务表");
    }

    @Test
    void verificationMustGateSequenceColumnAndExtendedUniqueKey() {
        String sql = normalize(readRequiredResource(VERIFICATION));

        assertContainsAll(sql,
                "information_schema.columns",
                "information_schema.statistics",
                "erp_accounting_event",
                "sequence_no",
                "uk_erp_accounting_event_source",
                "sequence_no,reversal_of_event_id",
                "tmp_v96_verify_expected_column",
                "tmp_v96_verify_expected_index",
                "v96_verify_verdict",
                "'pass'",
                "'fail'");

        assertContainsAll(sql,
                "select e.table_name",
                "column_verdict",
                "index_verdict",
                "group_concat(column_name order by seq_in_index)",
                "count(*)");

        assertContainsAll(sql,
                "'erp_accounting_event', 'sequence_no', 'int'");

        assertFalse(sql.contains("update erp_accounting_event"),
                "校验脚本不得修改业务数据");
        assertFalse(sql.contains("alter table"),
                "校验脚本不得变更结构");
    }

    private static String readRequiredResource(String path) {
        try (InputStream input = ErpAccountingEventSequenceFlywayMigrationContractTest.class
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