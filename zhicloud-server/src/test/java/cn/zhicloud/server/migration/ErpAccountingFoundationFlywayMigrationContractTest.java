package cn.zhicloud.server.migration;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ERP 会计事件与凭证底座 Flyway 迁移契约测试。
 *
 * <p>该测试先于 V94 落地，锁定会计事件、过账、规则、结算分配、发票来源
 * 以及凭证来源字段和幂等唯一键，避免只写 Java 实体却无法在真实库执行。
 */
class ErpAccountingFoundationFlywayMigrationContractTest {

    private static final String MIGRATION = "db/migration/V94__erp_accounting_foundation.sql";
    private static final String VERIFICATION = "db/verification/V94_verify.sql";

    @Test
    void migrationMustCreateAccountingFoundationTables() {
        String sql = normalize(readRequiredResource(MIGRATION));

        assertContainsAll(sql,
                "create table if not exists erp_accounting_event",
                "create table if not exists erp_accounting_posting",
                "create table if not exists erp_posting_rule",
                "create table if not exists erp_posting_rule_line",
                "create table if not exists erp_finance_settlement_allocation",
                "create table if not exists erp_tax_invoice_source");
    }

    @Test
    void eventTableMustCarrySourceAmountAndRetryState() {
        String sql = normalize(readRequiredResource(MIGRATION));

        assertContainsAll(sql,
                "`event_id` varchar(64) not null",
                "`event_type` varchar(64) not null",
                "`source_doc_type` varchar(64) not null",
                "`source_doc_id` bigint not null",
                "`source_doc_no` varchar(64) not null default ''",
                "`source_doc_line_id` bigint not null default 0",
                "`source_revision` int not null default 0",
                "`event_date` date not null",
                "`accounting_date` date not null",
                "`period_id` bigint not null default 0",
                "`period_code` varchar(32) not null default ''",
                "`amount_without_tax` decimal(20,4) not null default 0",
                "`tax_amount` decimal(20,4) not null default 0",
                "`amount_with_tax` decimal(20,4) not null default 0",
                "`cost_amount` decimal(20,4) not null default 0",
                "`currency` varchar(8) not null default 'cny'",
                "`exchange_rate` decimal(20,8) not null default 1",
                "`status` tinyint not null default 0",
                "`idempotency_key` varchar(128) not null",
                "`rule_set_version` varchar(32) not null default ''",
                "`payload_hash` char(64) not null",
                "`reversal_of_event_id` bigint not null default 0",
                "`retry_count` int not null default 0",
                "`next_retry_time` datetime default null",
                "`last_error` varchar(500) not null default ''",
                "`occurred_at` datetime not null",
                "`posted_at` datetime default null");
    }

    @Test
    void migrationMustBackfillCostAmountForAlreadyMigratedDatabases() {
        String sql = normalize(readRequiredResource(MIGRATION));

        assertContainsAll(sql,
                "`cost_amount` decimal(20,4) not null default 0 comment '成本金额（销售成本结转等，非成本类事件为 0）'",
                "call p_v94_add_column_if_not_exists('erp_accounting_event', 'cost_amount',"
                        + " 'decimal(20,4) not null default 0 comment ''成本金额（销售成本结转等，非成本类事件为 0）''')");
    }
    @Test
    void migrationMustDefineTenantScopedIdempotencyKeys() {
        String sql = normalize(readRequiredResource(MIGRATION));

        assertContainsAll(sql,
                "unique key `uk_erp_accounting_event_business_id` (`tenant_id`, `event_id`)",
                "unique key `uk_erp_accounting_event_idempotency` (`tenant_id`, `idempotency_key`)",
                "unique key `uk_erp_accounting_event_source` (`tenant_id`, `event_type`, `source_doc_type`, `source_doc_id`, `source_doc_line_id`, `source_revision`, `reversal_of_event_id`)",
                "unique key `uk_erp_accounting_posting_event_rule` (`tenant_id`, `event_id`, `posting_rule_id`, `rule_version`)",
                "unique key `uk_erp_posting_rule_code_version` (`tenant_id`, `rule_code`, `version`)",
                "unique key `uk_erp_posting_rule_line_no` (`tenant_id`, `rule_id`, `line_no`)",
                "unique key `uk_erp_finance_settlement_allocation_identity` (`tenant_id`, `finance_doc_type`, `finance_doc_item_id`, `invoice_id`, `invoice_line_id`, `source_doc_type`, `source_doc_id`, `reversal_of_allocation_id`)",
                "unique key `uk_erp_tax_invoice_source_identity` (`tenant_id`, `invoice_line_id`, `source_doc_type`, `source_doc_id`, `source_doc_line_id`)");
    }

    @Test
    void postingRuleMustSupportMultiLineEntries() {
        String sql = normalize(readRequiredResource(MIGRATION));

        assertContainsAll(sql,
                "create table if not exists erp_posting_rule_line",
                "`rule_id` bigint not null",
                "`line_no` int not null",
                "`entry_side` tinyint not null",
                "`account_code` varchar(50) not null default ''",
                "`account_resolver` varchar(64) not null default ''",
                "`amount_expression` varchar(255) not null",
                "unique key `uk_erp_posting_rule_line_no` (`tenant_id`, `rule_id`, `line_no`)");
        assertFalse(sql.contains("`debit_account_code`"),
                "规则头不得退化为固定单一借方科目");
        assertFalse(sql.contains("`credit_account_code`"),
                "规则头不得退化为固定单一贷方科目");
    }

    @Test
    void voucherMustExposeAuditableSourceFields() {
        String sql = normalize(readRequiredResource(MIGRATION));

        assertContainsAll(sql,
                "call p_v94_add_column_if_not_exists('erp_gl_voucher', 'source_event_id', 'bigint not null default 0 comment ''来源会计事件 id''')",
                "call p_v94_add_column_if_not_exists('erp_gl_voucher', 'source_doc_type', 'varchar(64) not null default '''' comment ''来源单据类型''')",
                "call p_v94_add_column_if_not_exists('erp_gl_voucher', 'source_doc_id', 'bigint not null default 0 comment ''来源单据 id''')",
                "call p_v94_add_column_if_not_exists('erp_gl_voucher', 'source_doc_no', 'varchar(64) not null default '''' comment ''来源单据编号''')",
                "idx_erp_gl_voucher_source_event",
                "idx_erp_gl_voucher_source_doc");
    }

    @Test
    void verificationMustGateTablesColumnsAndUniqueIndexes() {
        String sql = normalize(readRequiredResource(VERIFICATION));

        assertContainsAll(sql,
                "information_schema.tables",
                "information_schema.columns",
                "information_schema.statistics",
                "erp_accounting_event",
                "erp_accounting_posting",
                "erp_posting_rule",
                "erp_posting_rule_line",
                "erp_finance_settlement_allocation",
                "erp_tax_invoice_source",
                "source_event_id",
                "cost_amount",
                "uk_erp_accounting_event_idempotency",
                "uk_erp_accounting_event_source",
                "uk_erp_accounting_posting_event_rule",
                "uk_erp_posting_rule_code_version",
                "uk_erp_posting_rule_line_no",
                "uk_erp_finance_settlement_allocation_identity",
                "uk_erp_tax_invoice_source_identity",
                "v94_verify_verdict");
    }

    @Test
    void migrationMustNotUseMysqlUnsupportedSignalConcatOrLocalPrepare() {
        String sql = normalize(readRequiredResource(MIGRATION));

        assertFalse(sql.contains("signal sqlstate '45000' set message_text = concat("),
                "MySQL 8.4 不接受 SIGNAL ... MESSAGE_TEXT = CONCAT(...)，必须先赋值给变量");
        assertFalse(sql.matches(".*prepare\\s+\\w+\\s+from\\s+(?!@|'|\")\\w+.*"),
                "PREPARE ... FROM 只能使用用户变量或字符串字面量");
    }

    private static String readRequiredResource(String path) {
        try (InputStream input = ErpAccountingFoundationFlywayMigrationContractTest.class
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
        List<String> missing = java.util.Arrays.stream(expected)
                .filter(value -> !actual.contains(value))
                .toList();
        assertTrue(missing.isEmpty(), () -> "迁移契约缺少: " + missing);
    }
}