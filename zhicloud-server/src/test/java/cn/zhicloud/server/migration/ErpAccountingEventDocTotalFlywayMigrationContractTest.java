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
 * ERP 会计事件单据勾稽总额 Flyway 迁移契约测试。
 *
 * <p>V99 只负责给 erp_accounting_event 增加 doc_total_amount（来源单据勾稽总额），
 * 让应用层「同源单据同事件类型累计金额不得超额」的守卫有可审计、可复算的落库基线。
 * 迁移不得回填历史基线，也不得伪造或改写会计事件金额。</p>
 *
 * <p>求和范围契约：勾稽必须把冲销事件计入带符号求和。冲销事件是负向事件，承担
 * “释放额度”的作用；一旦在迁移或校验脚本里过滤掉它，反审核后重新过账会被误判为
 * 超额。这里用契约测试把该口径锁死。</p>
 */
class ErpAccountingEventDocTotalFlywayMigrationContractTest {

    private static final String MIGRATION = "db/migration/V99__erp_accounting_event_doc_total.sql";
    private static final String VERIFICATION = "db/verification/V99_verify.sql";

    @Test
    void migrationMustAddDocTotalColumnIdempotently() {
        String sql = normalize(readRequiredResource(MIGRATION));

        assertContainsAll(sql,
                "erp_accounting_event",
                "doc_total_amount",
                "decimal(20,4) default null",
                "p_v99_add_column_if_not_exists",
                "information_schema.columns",
                "information_schema.tables",
                "signal sqlstate '45000'",
                "alter table",
                "add column",
                "prepare v99_column_stmt",
                "deallocate prepare v99_column_stmt",
                "drop procedure if exists p_v99_add_column_if_not_exists");

        assertFalse(sql.contains("insert into erp_accounting_event"),
                "勾稽总额迁移不得伪造会计事件");
        assertFalse(sql.contains("update erp_accounting_event"),
                "勾稽总额迁移不得回填历史基线");
        assertFalse(sql.contains("drop table"),
                "勾稽总额迁移不得删除业务表");
        assertFalse(sql.contains("reversal_of_event_id = 0"),
                "迁移语义不得把冲销事件排除在勾稽求和之外");
    }

    @Test
    void verificationMustGateDocTotalAndSignedReconciliation() {
        String sql = normalize(readRequiredResource(VERIFICATION));

        assertContainsAll(sql,
                "information_schema.columns",
                "erp_accounting_event",
                "doc_total_amount",
                "sum(amount_with_tax)",
                "doc_total_amount is not null",
                "tmp_v99_verify_expected_column",
                "group_verdict",
                "v99_verify_verdict",
                "'baseline_inconsistent'",
                "'exceeded'",
                "'pass'",
                "'fail'");

        assertFalse(sql.contains("reversal_of_event_id = 0"),
                "校验脚本不得过滤冲销事件：过滤后已释放额度不会抵消，反审核再过账会假阳性 FAIL");
        assertFalse(sql.contains("update erp_accounting_event"),
                "校验脚本不得修改业务数据");
        assertFalse(sql.contains("alter table"),
                "校验脚本不得变更结构");
        assertFalse(sql.contains("insert into erp_accounting_event"),
                "校验脚本不得写入会计事件");
    }

    private static String readRequiredResource(String path) {
        try (InputStream input = ErpAccountingEventDocTotalFlywayMigrationContractTest.class
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