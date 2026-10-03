package cn.zhicloud.server.migration;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ERP 退货与红字发票过账规则种子 Flyway 迁移契约测试。
 *
 * <p>V97 为销售退货和采购退货提供 tenant_id=0 的默认规则模板，使退货能生成红字分录。
 * 迁移不得伪造任何会计事件、凭证或历史业务数据。</p>
 *
 * <p>断言分三层：逐行锁定（行号 + 借贷方向 + 科目 + 金额表达式）→ 借贷表达式平衡
 * （按金额口径把借贷两侧折算成同一向量后必须相等）→ 校验脚本门禁（行数与借贷映射
 * 同时进入 PASS/FAIL 判定）。任一层都能单独杀死方向翻转类变异。</p>
 */
class ErpReturnPostingRuleSeedFlywayMigrationContractTest {

    private static final String MIGRATION = "db/migration/V97__erp_return_posting_rule_seed.sql";
    private static final String VERIFICATION = "db/verification/V97_verify.sql";

    /** 金额表达式口径向量：amountWithTax = amountWithoutTax + taxAmount，costAmount 独立。 */
    private static final Map<String, int[]> EXPRESSION_WEIGHTS = Map.of(
            "amountwithouttax", new int[] {1, 0, 0},
            "taxamount", new int[] {0, 1, 0},
            "amountwithtax", new int[] {1, 1, 0},
            "costamount", new int[] {0, 0, 1});

    private static final String SALES_SIDE_MAP =
            "10:amountWithoutTax|10:taxAmount|20:amountWithTax|10:costAmount|20:costAmount";
    private static final String PURCHASE_SIDE_MAP =
            "20:amountWithoutTax|20:taxAmount|10:amountWithTax";

    @Test
    void migrationMustSeedSalesAndPurchaseReturnRulesIdempotently() {
        String sql = normalize(readRequiredResource(MIGRATION));

        assertContainsAll(sql,
                "erp_posting_rule",
                "erp_posting_rule_line",
                "erp_sales_return_posted",
                "erp_purchase_return_posted",
                "sales_return_posted",
                "purchase_return_posted",
                "where not exists",
                "v97_sales_return_rule_id",
                "v97_purchase_return_rule_id",
                "amountwithouttax",
                "taxamount",
                "amountwithtax",
                "costamount");

        // 销售退货红字规则：逐行锁定行号、借贷方向、科目与金额表达式。
        assertContainsAll(sql,
                "@v97_sales_return_rule_id, 1, 10, '6001', '', 'amountwithouttax'",
                "@v97_sales_return_rule_id, 2, 10, '22210102', '', 'taxamount'",
                "@v97_sales_return_rule_id, 3, 20, '1122', '', 'amountwithtax'",
                "@v97_sales_return_rule_id, 4, 10, '1405', '', 'costamount'",
                "@v97_sales_return_rule_id, 5, 20, '6401', '', 'costamount'");

        // 采购退货红字规则：贷记原材料与进项税额，借记应付账款。
        assertContainsAll(sql,
                "@v97_purchase_return_rule_id, 1, 20, '1403', '', 'amountwithouttax'",
                "@v97_purchase_return_rule_id, 2, 20, '22210101', '', 'taxamount'",
                "@v97_purchase_return_rule_id, 3, 10, '2202', '', 'amountwithtax'");

        assertFalse(sql.contains("insert into erp_accounting_event"),
                "退货规则迁移不得伪造会计事件");
        assertFalse(sql.contains("insert into erp_gl_voucher"),
                "退货规则迁移不得伪造凭证");
        assertFalse(sql.contains("drop table"),
                "退货规则迁移不得删除业务表");
    }

    @Test
    void migrationMustKeepLineNumbersContiguousAndSidesValid() {
        String sql = normalize(readRequiredResource(MIGRATION));

        List<RuleLine> sales = parseRuleLines(sql, "@v97_sales_return_rule_id");
        List<RuleLine> purchase = parseRuleLines(sql, "@v97_purchase_return_rule_id");

        assertEquals(List.of(1, 2, 3, 4, 5), lineNumbers(sales),
                "销售退货规则行号必须连续 1..5，缺失或重号都会让过账行序不确定");
        assertEquals(List.of(1, 2, 3), lineNumbers(purchase),
                "采购退货规则行号必须连续 1..3，缺失或重号都会让过账行序不确定");

        for (RuleLine line : concat(sales, purchase)) {
            assertTrue(line.entrySide() == 10 || line.entrySide() == 20,
                    () -> "规则行借贷方向只能是 10(借)/20(贷)，实际为 " + line.entrySide());
        }
    }

    @Test
    void migrationMustKeepDebitAndCreditExpressionsBalancedPerRule() {
        String sql = normalize(readRequiredResource(MIGRATION));

        assertBalanced("ERP_SALES_RETURN_POSTED", parseRuleLines(sql, "@v97_sales_return_rule_id"));
        assertBalanced("ERP_PURCHASE_RETURN_POSTED", parseRuleLines(sql, "@v97_purchase_return_rule_id"));
    }

    @Test
    void verificationMustGateReturnRulesLineCountsAndDirections() {
        String sql = normalize(readRequiredResource(VERIFICATION));

        assertContainsAll(sql,
                "erp_posting_rule",
                "erp_posting_rule_line",
                "erp_gl_account",
                "erp_sales_return_posted",
                "erp_purchase_return_posted",
                "tmp_v97_verify_expected_rule",
                "v97_verify_verdict",
                "'pass'",
                "'fail'",
                "count(*)",
                "expected_side_map",
                "wrong_direction_maps");

        // 校验脚本必须逐规则锁定期望行数，避免规则行缺失仍判 PASS。
        assertContainsAll(sql,
                "'erp_sales_return_posted', 'sales_return_posted', 5",
                "'erp_purchase_return_posted', 'purchase_return_posted', 3",
                "checked_lines = 8");

        // 校验脚本必须把期望借贷映射写进门禁，方向被翻转时 Q4 不能仍判 PASS。
        assertContainsAll(sql,
                "'" + SALES_SIDE_MAP.toLowerCase(Locale.ROOT) + "'",
                "'" + PURCHASE_SIDE_MAP.toLowerCase(Locale.ROOT) + "'",
                "rg.wrong_direction_maps = 0",
                "separator '|'");

        assertFalse(sql.contains("insert into erp_accounting_event"),
                "校验脚本不得写入会计事件");
        assertFalse(sql.contains("alter table"),
                "校验脚本不得变更结构");
    }

    private static void assertBalanced(String ruleCode, List<RuleLine> lines) {
        int[] debit = new int[3];
        int[] credit = new int[3];
        for (RuleLine line : lines) {
            int[] weight = EXPRESSION_WEIGHTS.get(line.amountExpression());
            assertNotNull(weight, () -> ruleCode + " 第 " + line.lineNo()
                    + " 行金额表达式不在白名单: " + line.amountExpression());
            int[] target = line.entrySide() == 10 ? debit : credit;
            for (int i = 0; i < weight.length; i++) {
                target[i] += weight[i];
            }
        }
        assertArrayEquals(debit, credit, () -> ruleCode + " 借贷金额表达式不平衡: 借="
                + Arrays.toString(debit) + " 贷=" + Arrays.toString(credit)
                + "（口径：amountWithTax = amountWithoutTax + taxAmount，costAmount 独立）");
    }

    private static List<RuleLine> parseRuleLines(String normalizedSql, String ruleIdVariable) {
        Pattern pattern = Pattern.compile(
                Pattern.quote(ruleIdVariable) + ", (\\d+), (\\d+), '(\\d+)', '', '([a-z]+)'");
        Matcher matcher = pattern.matcher(normalizedSql);
        List<RuleLine> lines = new ArrayList<>();
        while (matcher.find()) {
            lines.add(new RuleLine(Integer.parseInt(matcher.group(1)),
                    Integer.parseInt(matcher.group(2)),
                    matcher.group(3),
                    matcher.group(4)));
        }
        assertFalse(lines.isEmpty(), () -> "未解析到任何规则行，检查迁移写法: " + ruleIdVariable);
        return lines;
    }

    private static List<Integer> lineNumbers(List<RuleLine> lines) {
        return lines.stream().map(RuleLine::lineNo).toList();
    }

    private static List<RuleLine> concat(List<RuleLine> first, List<RuleLine> second) {
        List<RuleLine> all = new ArrayList<>(first);
        all.addAll(second);
        return all;
    }

    private static String readRequiredResource(String path) {
        try (InputStream input = ErpReturnPostingRuleSeedFlywayMigrationContractTest.class
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

    private record RuleLine(int lineNo, int entrySide, String accountCode, String amountExpression) {
    }
}
