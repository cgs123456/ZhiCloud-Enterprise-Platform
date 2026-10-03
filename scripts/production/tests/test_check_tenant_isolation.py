"""多租户隔离契约检查器的回归测试（Phase 1 / P0-3）。

真实发现（2026-09-25 首扫，仓库 DDL 332 表 / 568 DO）：
  * `system_sms_code` 表有 `tenant_id bigint NOT NULL DEFAULT 0`，
    但 `SmsCodeDO extends BaseDO` 且标了 `@TenantIgnore` —— 查询不加租户过滤、
    写入落到默认值 0，属于真实的跨租户可见性缺口（T3，唯一 P0）。
  * 22 张表里没有 tenant_id 的 DO **全部**标了 @TenantIgnore，
    说明上游契约本身是自洽的（T1/T2 为 0，作为门禁保留）。

本测试固化的是「规则语义」，不是「仓库已安全」。"""

import sys
import tempfile
import unittest
from pathlib import Path

TESTS_DIR = Path(__file__).resolve().parent
PRODUCTION_DIR = TESTS_DIR.parent
sys.path.insert(0, str(PRODUCTION_DIR))

import check_tenant_isolation as tnt  # noqa: E402


def write(root: Path, rel: str, text: str) -> Path:
    path = root / rel
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding="utf-8", newline="\n")
    return path


def do_java(cls, base, table, ignore=False, tenant_field=False):
    ann = "@TenantIgnore\n" if ignore else ""
    field = "    private Long tenantId;\n" if tenant_field else ""
    return (f'@TableName("{table}")\n{ann}'
            f'public class {cls} extends {base} {{\n    private Long id;\n{field}}}\n')


def ddl(table, with_tenant=True, tenant_index=True, index_style="KEY"):
    cols = "  `id` bigint NOT NULL,\n"
    if with_tenant:
        cols += "  `tenant_id` bigint NOT NULL DEFAULT 0,\n"
    idx = ""
    if with_tenant and tenant_index:
        idx = f"  {index_style} `idx_tenant` (`tenant_id`),\n"
    elif not tenant_index:
        idx = f"  {index_style} `idx_other` (`id`),\n"
    return f"CREATE TABLE `{table}` (\n{cols}{idx}  PRIMARY KEY (`id`)\n);\n"


class DdlParseTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.root = Path(self.tmp.name)

    def tearDown(self):
        self.tmp.cleanup()

    def _parse(self, text):
        p = write(self.root, "sql/mysql/demo.sql", text)
        return tnt.parse_ddl_files([p])

    def test_key_style_index_is_recognised(self):
        """仓库 DDL 用的是 `KEY idx_tenant (tenant_id)`，不是 INDEX。"""
        got = self._parse(ddl("t1", index_style="KEY"))
        self.assertIn("t1", got)
        self.assertIn("tenant_id", got["t1"]["columns"])
        self.assertEqual(got["t1"]["indexes"], [("idx_tenant", "tenant_id")])

    def test_index_style_index_is_recognised(self):
        got = self._parse(ddl("t2", index_style="INDEX"))
        self.assertEqual(got["t2"]["indexes"], [("idx_tenant", "tenant_id")])

    def test_primary_key_is_not_treated_as_column(self):
        got = self._parse(ddl("t3"))
        self.assertNotIn("primary", got["t3"]["columns"])
        self.assertIn("id", got["t3"]["columns"])

    def test_nested_paren_in_column_definition(self):
        got = self._parse("CREATE TABLE `t4` (\n  `a` decimal(10, 2) NOT NULL,\n"
                          "  `tenant_id` bigint NOT NULL\n);\n")
        self.assertIn("a", got["t4"]["columns"])
        self.assertIn("tenant_id", got["t4"]["columns"])

    def test_add_column_alter_is_applied(self):
        got = self._parse(ddl("t5", with_tenant=False) +
                          "ALTER TABLE `t5` ADD COLUMN `tenant_id` BIGINT NOT NULL DEFAULT 0;\n")
        self.assertIn("tenant_id", got["t5"]["columns"])

    def test_add_index_alter_is_applied(self):
        got = self._parse(ddl("t6", with_tenant=False) +
                          "ALTER TABLE `t6` ADD INDEX `idx_tenant` (`tenant_id`, `deleted`);\n")
        self.assertIn(("idx_tenant", "tenant_id"), got["t6"]["indexes"])

    def test_alter_is_applied_after_all_create_tables(self):
        # Real migrations are lexicographically sorted: V108 sorts before V52.
        write(self.root, "sql/mysql/V108__add.sql",
              "ALTER TABLE `t7` ADD COLUMN `tenant_id` BIGINT NOT NULL DEFAULT 0;\n"
              "ALTER TABLE `t7` ADD INDEX `idx_tenant` (`tenant_id`, `deleted`);\n")
        write(self.root, "sql/mysql/V52__create.sql", ddl("t7", with_tenant=False))
        paths = sorted((self.root / "sql" / "mysql").glob("*.sql"))
        got = tnt.parse_ddl_files(paths)
        self.assertIn("tenant_id", got["t7"]["columns"])
        self.assertIn(("idx_tenant", "tenant_id"), got["t7"]["indexes"])


class RuleTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.root = Path(self.tmp.name)
        (self.root / "sql" / "mysql").mkdir(parents=True, exist_ok=True)

    def tearDown(self):
        self.tmp.cleanup()

    def _scan(self, java_rel, java_text, ddl_text):
        write(self.root, java_rel, java_text)
        write(self.root, "sql/mysql/demo.sql", ddl_text)
        paths = sorted((self.root / "sql" / "mysql").glob("*.sql"))
        return tnt.build_report(self.root, paths)

    def _rules(self, rep):
        return {(f["rule"], f["table"]) for f in rep["findings"]}

    def test_t1_tenant_do_without_column(self):
        rep = self._scan("m/A.java", do_java("A", "TenantBaseDO", "t_a"),
                         ddl("t_a", with_tenant=False))
        self.assertIn(("T1", "t_a"), self._rules(rep))
        self.assertEqual(rep["findings_by_severity"].get("P0"), 1)

    def test_t2_filter_applied_without_column(self):
        rep = self._scan("m/B.java", do_java("B", "BaseDO", "t_b"),
                         ddl("t_b", with_tenant=False))
        self.assertIn(("T2", "t_b"), self._rules(rep))

    def test_t2_is_suppressed_by_tenant_ignore(self):
        rep = self._scan("m/C.java", do_java("C", "BaseDO", "t_c", ignore=True),
                         ddl("t_c", with_tenant=False))
        self.assertEqual(rep["findings"], [])

    def test_t3_ignored_but_has_tenant_column(self):
        rep = self._scan("m/D.java", do_java("D", "BaseDO", "t_d", ignore=True),
                         ddl("t_d", with_tenant=True))
        self.assertIn(("T3", "t_d"), self._rules(rep))
        self.assertEqual(rep["findings_by_severity"].get("P0"), 1)

    def test_t4_tenant_column_not_indexed(self):
        rep = self._scan("m/E.java", do_java("E", "TenantBaseDO", "t_e"),
                         ddl("t_e", tenant_index=False))
        self.assertIn(("T4", "t_e"), self._rules(rep))

    def test_t4_suppressed_when_indexed(self):
        rep = self._scan("m/F.java", do_java("F", "TenantBaseDO", "t_f"),
                         ddl("t_f", tenant_index=True))
        self.assertEqual(rep["findings"], [])

    def test_t5_unverified_table_is_not_guessed(self):
        """表里有没有 tenant_id 不知道 -> 记 T5(P1)，绝不判 P0。"""
        rep = self._scan("m/G.java", do_java("G", "TenantBaseDO", "t_missing"),
                         ddl("t_other"))
        self.assertIn(("T5", "t_missing"), self._rules(rep))
        self.assertNotIn("P0", rep["findings_by_severity"])

    def test_consistent_repo_passes(self):
        rep = self._scan("m/H.java", do_java("H", "TenantBaseDO", "t_h"),
                         ddl("t_h"))
        self.assertEqual(rep["status"], "passed")

    def test_additive_migration_satisfies_tenant_contract(self):
        """CREATE 后通过 ALTER 补租户列和索引，是合法的迁移演进方式。"""
        rep = self._scan(
            "m/I.java", do_java("I", "TenantBaseDO", "t_i"),
            ddl("t_i", with_tenant=False) +
            "ALTER TABLE `t_i` ADD COLUMN `tenant_id` BIGINT NOT NULL DEFAULT 0;\n"
            "ALTER TABLE `t_i` ADD INDEX `idx_tenant` (`tenant_id`, `deleted`);\n",
        )
        self.assertNotIn(("T1", "t_i"), self._rules(rep))
        self.assertNotIn(("T4", "t_i"), self._rules(rep))
        self.assertEqual(rep["status"], "passed")


class ReportTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.root = Path(self.tmp.name)

    def tearDown(self):
        self.tmp.cleanup()

    def test_report_counts_and_disclaimers(self):
        write(self.root, "m/A.java", do_java("A", "BaseDO", "t_a", ignore=True))
        write(self.root, "sql/mysql/demo.sql", ddl("t_a"))
        rep = tnt.build_report(self.root, sorted((self.root / "sql" / "mysql").glob("*.sql")))
        self.assertEqual(rep["do_count"], 1)
        self.assertEqual(rep["do_verified_count"], 1)
        self.assertEqual(rep["do_unverified_count"], 0)
        self.assertEqual(rep["do_by_base"]["BaseDO"], 1)
        self.assertEqual(rep["status"], "failed")
        self.assertGreaterEqual(len(rep["disclaimers"]), 3)
        self.assertIn("information_schema", rep["disclaimers"][0])


if __name__ == "__main__":
    unittest.main()