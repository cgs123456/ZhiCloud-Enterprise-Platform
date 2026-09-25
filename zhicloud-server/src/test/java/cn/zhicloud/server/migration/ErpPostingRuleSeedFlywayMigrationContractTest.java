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
 * ERP 默认过账规则 Flyway 迁移契约测试。
 *
 * <p>V95 只负责提供平台默认规则和基础科目，不把默认规则视为法人最终会计政策。
 * 租户可覆盖同编码规则和科目；未覆盖时由服务层回退到 tenant_id=0 的默认模板。</p>
 */
class ErpPostingRuleSeedFlywayMigrationContractTest {

    private static final String MIGRATION = "db/migration/V95__erp_posting_rule_seed.sql";
    private static final String VERIFICATION = "db/verification/V95_verify.sql";

    @Test
    void migrationMustSeedDefaultAccountsAndVersionedRulesIdempotently() {
        String sql = normalize(readRequiredResource(MIGRATION));

        assertContainsAll(sql,
                "insert into erp_gl_account",
                "'1403'",
                "'22210101'",
                "where not exists",
                "'1122'",
                "'6001'",
                "'22210102'",
                "'6401'",
                "'1405'",
                "tenant_id",
                "0");

        assertContainsAll(sql,
                "insert into erp_posting_rule",
                "'erp_purchase_receipt_accrual'",
                "'purchase_receipt_accrual'",
                "'v1'",
                "'erp_payment_posted'",
                "'payment_posted'",
                "'v1'",
                "effective_from",
                "'erp_sales_shipment_posted'",
                "'sales_shipment_posted'",
                "'v1'",
                "'erp_receipt_posted'",
                "'receipt_posted'",
                "'v1'",
                "published_at");

        assertContainsAll(sql,
                "select id into @v95_purchase_rule_id",
                "select id into @v95_payment_rule_id",
                "select id into @v95_sales_rule_id",
                "select id into @v95_receipt_rule_id",
                "insert into erp_posting_rule_line",
                "'1403'",
                "'22210101'",
                "'2202'",
                "'1002'",
                "'amountwithouttax'",
                "'taxamount'",
                "'amountwithtax'",
                "'costamount'");

        assertContainsAll(sql,
                "purchase_receipt_accrual",
                "payment_posted",
                "sales_shipment_posted",
                "receipt_posted",
                "entry_side",
                "line_no",
                "where not exists");

        assertFalse(sql.contains("insert into erp_accounting_event"),
                "默认规则迁移不得伪造历史会计事件");
        assertFalse(sql.contains("insert into erp_accounting_posting"),
                "默认规则迁移不得伪造历史过账结果");
    }

    @Test
    void verificationMustGateSeedDataAndBalancedRuleLines() {
        String sql = normalize(readRequiredResource(VERIFICATION));

        assertContainsAll(sql,
                "information_schema.tables",
                "information_schema.columns",
                "erp_gl_account",
                "erp_posting_rule",
                "erp_posting_rule_line",
                "'1403'",
                "'22210101'",
                "'erp_purchase_receipt_accrual'",
                "'1122'",
                "'6001'",
                "'22210102'",
                "'6401'",
                "'1405'",
                "'erp_payment_posted'",
                "v95_verify_verdict",
                "'erp_sales_shipment_posted'",
                "'erp_receipt_posted'",
                "pass");

        assertContainsAll(sql,
                "delimiter $$",
                "create procedure v95_verify_run()",
                "@v95_schema_fail_count",
                "call v95_verify_run()",
                "'fail'");

        assertFalse(sql.contains("drop temporary table if exists tmp_v95_verify_failure; "
                        + "insert into tmp_v95_verify_failure"),
                "校验脚本不得在删除失败表后继续写入");
        assertContainsAll(sql,
                "tmp_v95_verify_expected_line",
                "collate utf8mb4_unicode_ci",
                "amountwithouttax",
                "taxamount",
                "amountwithtax",
                "costamount",
                "entry_side",
                "account_code",
                "left join",
                "rule_id");
    }

    private static String readRequiredResource(String path) {
        try (InputStream input = ErpPostingRuleSeedFlywayMigrationContractTest.class
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