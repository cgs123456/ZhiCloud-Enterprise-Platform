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
 * ERP 税务表 Flyway 迁移契约测试。
 *
 * <p>该测试先于迁移文件落地，确保税务表不会继续只存在于 sql/mysql/erp_tax.sql：
 * 空库迁移必须能创建三张表，旧库迁移必须能幂等补齐 AUTO_INCREMENT 与关键索引。
 */
class ErpTaxFlywayMigrationContractTest {

    private static final String MIGRATION = "db/migration/V93__erp_tax_ddl.sql";
    private static final String VERIFICATION = "db/verification/V93_verify.sql";

    @Test
    void migrationMustDefineFinalTaxSchema() {
        String sql = normalize(readRequiredResource(MIGRATION));

        assertContainsAll(sql,
                "create table if not exists erp_tax_rate",
                "create table if not exists erp_tax_invoice",
                "create table if not exists erp_tax_invoice_line");

        assertContainsAll(sql,
                "`id` bigint not null auto_increment",
                "`tenant_id` bigint not null default 0",
                "`deleted` bit(1) not null default 0");

        assertContainsAll(sql,
                "`code` varchar(64) not null",
                "`name` varchar(128) not null",
                "`rate_type` tinyint not null",
                "`rate` decimal(10,6) not null",
                "`is_default` tinyint not null default 0",
                "`effective_date` date default null",
                "`expiry_date` date default null",
                "`remark` varchar(500) default null",
                "`status` tinyint not null default 0");

        assertContainsAll(sql,
                "`invoice_no` varchar(64) not null",
                "`invoice_code` varchar(64) not null",
                "`invoice_type` tinyint not null",
                "`buyer_name` varchar(128) default null",
                "`buyer_tax_no` varchar(64) default null",
                "`seller_name` varchar(128) default null",
                "`seller_tax_no` varchar(64) default null",
                "`invoice_date` date default null",
                "`amount_without_tax` decimal(20,4) not null default 0",
                "`tax_amount` decimal(20,4) not null default 0",
                "`amount_with_tax` decimal(20,4) not null default 0",
                "`source_order_type` varchar(64) default null",
                "`source_order_id` bigint default null");

        assertContainsAll(sql,
                "`invoice_id` bigint not null",
                "`line_no` int not null",
                "`product_name` varchar(128) not null",
                "`specification` varchar(255) default null",
                "`unit` varchar(32) default null",
                "`quantity` decimal(20,4) not null default 0",
                "`unit_price` decimal(20,4) not null default 0",
                "`tax_rate` decimal(10,6) not null default 0");

        assertContainsAll(sql,
                "unique key `uk_erp_tax_rate_code_tenant` (`tenant_id`, `code`)",
                "unique key `uk_erp_tax_invoice_no_code_tenant` (`tenant_id`, `invoice_no`, `invoice_code`)",
                "unique key `uk_erp_tax_invoice_line_invoice_line_tenant` (`tenant_id`, `invoice_id`, `line_no`)");

        assertContainsAll(sql,
                "engine=innodb",
                "default charset=utf8mb4",
                "collate=utf8mb4_unicode_ci");

        assertContainsAll(sql,
                "call p_v93_ensure_auto_increment('erp_tax_rate')",
                "call p_v93_ensure_auto_increment('erp_tax_invoice')",
                "call p_v93_ensure_auto_increment('erp_tax_invoice_line')");
        assertContainsAll(sql,
                "create procedure p_v93_normalize_table_options",
                "call p_v93_normalize_table_options('erp_tax_rate')",
                "call p_v93_normalize_table_options('erp_tax_invoice')",
                "call p_v93_normalize_table_options('erp_tax_invoice_line')",
                "convert to character set utf8mb4 collate utf8mb4_unicode_ci");

        assertContainsAll(sql,
                "create procedure p_v93_normalize_legacy_columns",
                "call p_v93_normalize_legacy_columns('erp_tax_rate')",
                "call p_v93_normalize_legacy_columns('erp_tax_invoice')",
                "call p_v93_normalize_legacy_columns('erp_tax_invoice_line')",
                "modify column `tenant_id` bigint not null default 0",
                "modify column `deleted` bit(1) not null default 0");
    }

    @Test
    void verificationMustGateTablesColumnsAndIndexes() {
        String sql = normalize(readRequiredResource(VERIFICATION));

        assertContainsAll(sql,
                "information_schema.tables",
                "information_schema.columns",
                "information_schema.statistics",
                "erp_tax_rate",
                "erp_tax_invoice",
                "erp_tax_invoice_line",
                "uk_erp_tax_rate_code_tenant",
                "uk_erp_tax_invoice_no_code_tenant",
                "uk_erp_tax_invoice_line_invoice_line_tenant",
                "auto_increment",
                "is_nullable");
    }

    @Test
    void signalMustUseMysqlCompatibleMessageVariable() {
        String sql = normalize(readRequiredResource(MIGRATION));

        assertFalse(sql.contains("signal sqlstate '45000' set message_text = concat("),
                "MySQL 8.4 不接受 SIGNAL ... MESSAGE_TEXT = CONCAT(...)，必须先赋值给变量");
    }
    private static String readRequiredResource(String path) {
        try (InputStream input = ErpTaxFlywayMigrationContractTest.class.getClassLoader().getResourceAsStream(path)) {
            assertNotNull(input, "缺少 Flyway 资源: " + path);
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new AssertionError("读取 Flyway 资源失败: " + path, ex);
        }
    }

    private static String normalize(String sql) {
        return sql.replace("`", "`")
                .replaceAll("\\s+", " ")
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