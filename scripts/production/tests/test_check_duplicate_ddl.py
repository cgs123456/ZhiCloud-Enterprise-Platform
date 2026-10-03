"""重复建表门禁的回归测试。

背景（2026-09-26）：
  迁移链上 11 张表被两个迁移 CREATE TABLE 重复创建，其中 erp_period /
  erp_period_close / ai_nl2sql_query_history 的三份定义并不一致。Flyway 让
  版本小的迁移先建表，但“后补入小版本迁移”的历史库会保留大版本结构，
  Java DO 于是可能读到完全不同的列集合。

本测试固化两类证据：
  1. 真实仓库扫描：11 组重复、3 组冲突、门禁 PASS；
  2. 失败闭合：未登记、清单腐化、收敛迁移缺失、期望列无法推导都必须 FAIL。
"""

import json
import sys
import tempfile
import unittest
from pathlib import Path

TESTS_DIR = Path(__file__).resolve().parent
PRODUCTION_DIR = TESTS_DIR.parent
sys.path.insert(0, str(PRODUCTION_DIR))

import check_duplicate_ddl as dup  # noqa: E402

REPO_MIGRATIONS = (PRODUCTION_DIR.parents[1]
                   / "zhicloud-server" / "src" / "main" / "resources" / "db" / "migration")
REPO_ALLOWLIST = PRODUCTION_DIR / "duplicate_ddl_allowlist.json"


class RepoScanTest(unittest.TestCase):
    def test_real_repo_has_eleven_duplicate_groups(self):
        report = dup.build_report(REPO_MIGRATIONS, REPO_ALLOWLIST)
        self.assertEqual(report["status"], "passed", report["findings"])
        self.assertEqual(report["duplicate_groups"], 11)
        self.assertEqual(report["conflicting_groups"], 3)

    def test_conflicting_tables_are_registered_with_canonical_v6_or_v38(self):
        report = dup.build_report(REPO_MIGRATIONS, REPO_ALLOWLIST)
        canon = {row["table"]: row["canonical_migration"] for row in report["tables"]}
        self.assertEqual(canon["erp_period"], "V6__erp_period_close.sql")
        self.assertEqual(canon["erp_period_close"], "V6__erp_period_close.sql")
        self.assertEqual(canon["ai_nl2sql_query_history"], "V38__ai_nl2sql_history.sql")

    def test_identical_groups_need_no_column_reconciliation(self):
        report = dup.build_report(REPO_MIGRATIONS, REPO_ALLOWLIST)
        identical = {row["table"] for row in report["tables"] if row["definitions_identical"]}
        self.assertEqual(len(identical), 8)
        self.assertNotIn("erp_period", identical)


class ParserTest(unittest.TestCase):
    def test_index_lines_are_not_treated_as_columns(self):
        text = (
            "CREATE TABLE IF NOT EXISTS `demo` (\n"
            "  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',\n"
            "  `code` VARCHAR(20) NOT NULL COMMENT '编码',\n"
            "  PRIMARY KEY (`id`),\n"
            "  UNIQUE KEY `uk_code` (`code`),\n"
            "  KEY `idx_code` (`code`)\n"
            ") ENGINE=InnoDB;\n"
        )
        parsed = dup.parse_create_tables(text, "V1__demo.sql")
        self.assertEqual(len(parsed), 1)
        self.assertEqual(sorted(parsed[0]["columns"]), ["code", "id"])

    def test_comment_is_stripped_from_definition(self):
        text = "CREATE TABLE demo (\n  id BIGINT COMMENT '主键'\n);\n"
        parsed = dup.parse_create_tables(text, "V1__demo.sql")
        self.assertEqual(parsed[0]["columns"]["id"], "id BIGINT")

    def test_nested_parentheses_do_not_break_block_scan(self):
        text = (
            "CREATE TABLE demo (\n"
            "  amount DECIMAL(20,4) DEFAULT 0 COMMENT '金额',\n"
            "  name VARCHAR(32) DEFAULT NULL\n"
            ");\n"
        )
        parsed = dup.parse_create_tables(text, "V1__demo.sql")
        self.assertEqual(sorted(parsed[0]["columns"]), ["amount", "name"])


def write(path: Path, text: str) -> Path:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding="utf-8", newline="\n")
    return path


class FailClosedTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.root = Path(self.tmp.name)
        self.migrations = self.root / "migrations"
        write(self.migrations / "V1__demo.sql",
              "CREATE TABLE IF NOT EXISTS demo (\n"
              "  id BIGINT NOT NULL AUTO_INCREMENT,\n"
              "  code VARCHAR(20) NOT NULL\n"
              ");\n")
        write(self.migrations / "V2__demo.sql",
              "CREATE TABLE IF NOT EXISTS demo (\n"
              "  id BIGINT PRIMARY KEY,\n"
              "  name VARCHAR(128) DEFAULT NULL\n"
              ");\n")
        write(self.migrations / "V98__reconcile_duplicate_table_ddl.sql",
              "-- demo 收敛\n"
              "ALTER TABLE demo ADD COLUMN code VARCHAR(20) DEFAULT NULL;\n"
              "ALTER TABLE demo ADD COLUMN name VARCHAR(128) DEFAULT NULL;\n")
        self.allowlist = write(
            self.root / "allowlist.json",
            json.dumps({
                "schema_version": 1,
                "tables": [{
                    "table": "demo",
                    "creating_migrations": ["V1__demo.sql", "V2__demo.sql"],
                    "definitions_identical": False,
                    "canonical_migration": "V1__demo.sql",
                    "reconciled_by": "V98__reconcile_duplicate_table_ddl.sql",
                    "expected_columns": ["id", "code", "name"],
                }],
            }, ensure_ascii=False, indent=2) + "\n",
        )

    def tearDown(self):
        self.tmp.cleanup()

    def _rules(self):
        report = dup.build_report(self.migrations, self.allowlist)
        return {f["rule"] for f in report["findings"]}, report

    def test_registered_duplicate_passes(self):
        rules, report = self._rules()
        self.assertEqual(report["status"], "passed", report["findings"])
        self.assertEqual(rules, set())

    def test_unregistered_duplicate_fails(self):
        write(self.allowlist, json.dumps({"schema_version": 1, "tables": []}))
        rules, report = self._rules()
        self.assertIn("D1", rules)
        self.assertEqual(report["status"], "failed")

    def test_stale_allowlist_entry_fails(self):
        (self.migrations / "V2__demo.sql").unlink()
        rules, _ = self._rules()
        self.assertIn("D2", rules)

    def test_missing_reconciliation_migration_fails(self):
        (self.migrations / "V98__reconcile_duplicate_table_ddl.sql").unlink()
        rules, _ = self._rules()
        self.assertIn("D3", rules)

    def test_canonical_migration_outside_creators_fails(self):
        data = json.loads(self.allowlist.read_text(encoding="utf-8"))
        data["tables"][0]["canonical_migration"] = "V99__nope.sql"
        write(self.allowlist, json.dumps(data, ensure_ascii=False, indent=2) + "\n")
        rules, _ = self._rules()
        self.assertIn("D4", rules)

    def test_expected_column_not_derivable_fails(self):
        data = json.loads(self.allowlist.read_text(encoding="utf-8"))
        data["tables"][0]["expected_columns"] = ["id", "code", "name", "ghost"]
        write(self.allowlist, json.dumps(data, ensure_ascii=False, indent=2) + "\n")
        rules, _ = self._rules()
        self.assertIn("D5", rules)

    def test_expected_column_not_covered_by_reconciliation_fails(self):
        # name 只出现在 V2 定义里，收敛脚本不再提到它 -> 无法保证收敛
        write(self.migrations / "V98__reconcile_duplicate_table_ddl.sql",
              "-- demo 收敛（只补 code）\n"
              "ALTER TABLE demo ADD COLUMN code VARCHAR(20) DEFAULT NULL;\n")
        rules, _ = self._rules()
        self.assertIn("D6", rules)

    def test_creating_migration_set_mismatch_fails(self):
        data = json.loads(self.allowlist.read_text(encoding="utf-8"))
        data["tables"][0]["creating_migrations"] = ["V1__demo.sql"]
        write(self.allowlist, json.dumps(data, ensure_ascii=False, indent=2) + "\n")
        rules, _ = self._rules()
        self.assertIn("D7", rules)

    def test_invalid_allowlist_schema_fails(self):
        write(self.allowlist, json.dumps({"schema_version": 2, "tables": []}))
        rules, _ = self._rules()
        self.assertIn("D8", rules)


if __name__ == "__main__":
    unittest.main()
