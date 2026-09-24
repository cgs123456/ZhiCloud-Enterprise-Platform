"""迁移 SQL lint 与影子库执行器的回归测试。

先 RED 后 GREEN 的证据（2026-09-24 真机）：
  * V90/V91/V92 通过静态契约检查，却在 MySQL 8.4.11 上三条全部执行失败；
  * 根因是 `DELIMITER ;-- 注释`（R1）与 `PREPARE ... FROM 局部变量`（R2）；
  * 本测试把这两类缺陷固化为门禁，防止再次混入迁移脚本。
"""

import sys
import unittest
from pathlib import Path

TESTS_DIR = Path(__file__).resolve().parent
PRODUCTION_DIR = TESTS_DIR.parent
sys.path.insert(0, str(PRODUCTION_DIR))

import sql_migration_lint as lint  # noqa: E402

MIGRATIONS_DIR = (PRODUCTION_DIR.parents[1].parent
                  / "script" / "enterprise-platform-upgrade" / "migrations")


class DelimiterRuleTest(unittest.TestCase):
    def test_trailing_comment_after_delimiter_is_violation(self):
        text = "DELIMITER $$\nCREATE PROCEDURE p()\nBEGIN\n  SELECT 1;\nEND$$\nDELIMITER ;-- 注释\nSELECT 1;\n"
        findings = lint.lint_text(text, "demo.sql")
        rules = {f["rule"] for f in findings}
        self.assertIn("R1", rules)
        self.assertIn("R3", rules)

    def test_clean_delimiter_passes(self):
        text = "DELIMITER $$\nCREATE PROCEDURE p()\nBEGIN\n  SELECT 1;\nEND$$\nDELIMITER ;\nSELECT 1;\n"
        self.assertEqual(lint.lint_text(text, "demo.sql"), [])

    def test_odd_delimiter_count_is_violation(self):
        text = "DELIMITER $$\nSELECT 1$$\n"
        rules = {f["rule"] for f in lint.lint_text(text, "demo.sql")}
        self.assertIn("R3", rules)


class PrepareRuleTest(unittest.TestCase):
    def test_local_variable_is_violation(self):
        text = "PREPARE s FROM v_sql;\n"
        findings = lint.lint_text(text, "demo.sql")
        self.assertTrue(any(f["rule"] == "R2" for f in findings))

    def test_user_variable_passes(self):
        self.assertEqual(lint.lint_text("PREPARE s FROM @v_sql;\n", "demo.sql"), [])

    def test_literal_passes(self):
        self.assertEqual(lint.lint_text("PREPARE s FROM 'SELECT 1';\n", "demo.sql"), [])

    def test_prepare_inside_comment_is_ignored(self):
        text = "-- PREPARE s FROM v_sql;\nSELECT 1;\n"
        self.assertEqual(lint.lint_text(text, "demo.sql"), [])


class DestructiveRuleTest(unittest.TestCase):
    def test_drop_business_table_is_violation(self):
        text = "DROP TABLE erp_sale_order;\n"
        self.assertTrue(any(f["rule"] == "R4" for f in lint.lint_text(text, "d.sql")))

    def test_temporary_table_operations_allowed(self):
        text = ("CREATE TEMPORARY TABLE IF NOT EXISTS tmp_x (id INT);\n"
                "TRUNCATE TABLE tmp_x;\n"
                "DROP TEMPORARY TABLE IF EXISTS tmp_x;\n")
        self.assertEqual(lint.lint_text(text, "d.sql"), [])

    def test_delete_without_where_is_violation(self):
        text = "DELETE FROM erp_sale_order;\n"
        self.assertTrue(any(f["rule"] == "R4" for f in lint.lint_text(text, "d.sql")))

    def test_delete_with_where_passes(self):
        text = "DELETE FROM erp_sale_order WHERE id = 1;\n"
        self.assertEqual(lint.lint_text(text, "d.sql"), [])


class TrailingNewlineRuleTest(unittest.TestCase):
    def test_missing_newline_is_violation(self):
        self.assertTrue(any(f["rule"] == "R5" for f in lint.lint_text("SELECT 1;", "d.sql")))


class RealMigrationsTest(unittest.TestCase):
    """对真实交付的 V90/V91/V92 做门禁：修好之后必须保持 0 违规。"""

    def test_shipped_migrations_are_clean(self):
        if not MIGRATIONS_DIR.is_dir():
            self.skipTest(f"迁移目录不存在: {MIGRATIONS_DIR}")
        files = sorted(MIGRATIONS_DIR.glob("V9*.sql"))
        self.assertEqual(len(files), 3, f"预期 3 个迁移文件，实际 {len(files)}")
        report = lint.lint_paths(files)
        self.assertEqual(report["status"], "passed",
                         f"迁移 lint 失败: {report['files']}")


if __name__ == "__main__":
    unittest.main()
