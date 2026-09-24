"""影子库执行器的纯函数测试（不连数据库）。"""

import sys
import unittest
from pathlib import Path

TESTS_DIR = Path(__file__).resolve().parent
PRODUCTION_DIR = TESTS_DIR.parent
sys.path.insert(0, str(PRODUCTION_DIR))

import migration_shadow_run as msr  # noqa: E402


class ParseTsvTest(unittest.TestCase):
    def test_tab_separated_rows(self):
        self.assertEqual(msr.parse_tsv("a\tb\nc\td\n"), [["a", "b"], ["c", "d"]])

    def test_blank_lines_skipped(self):
        self.assertEqual(msr.parse_tsv("a\n\nb\n"), [["a"], ["b"]])

    def test_empty_output(self):
        self.assertEqual(msr.parse_tsv(""), [])


class EvaluateTest(unittest.TestCase):
    def _columns(self, spec):
        return {(t, c): {"is_nullable": n, "column_default": d, "extra": e,
                         "column_type": "bigint"}
                for (t, c, n, d, e) in spec}

    def test_missing_column_detected(self):
        cols = self._columns([("t1", "id", "NO", "<NULL>", "auto_increment")])
        targets = {"v90_missing_columns": [{"table": "t1", "column": "amount"}],
                   "v91_blockers": [], "v92_auto_tables": []}
        self.assertEqual(msr.evaluate(cols, targets)["v90_missing_after"], ["t1.amount"])

    def test_blocking_column_requires_not_null_no_default(self):
        targets = {"v90_missing_columns": [],
                   "v91_blockers": [{"table": "t1", "column": "code"}],
                   "v92_auto_tables": []}
        blocking = self._columns([("t1", "code", "NO", "<NULL>", "")])
        self.assertEqual(msr.evaluate(blocking, targets)["v91_blocking_after"], ["t1.code"])
        nullable = self._columns([("t1", "code", "YES", "<NULL>", "")])
        self.assertEqual(msr.evaluate(nullable, targets)["v91_blocking_after"], [])
        defaulted = self._columns([("t1", "code", "NO", "0", "")])
        self.assertEqual(msr.evaluate(defaulted, targets)["v91_blocking_after"], [])
        auto = self._columns([("t1", "code", "NO", "<NULL>", "auto_increment")])
        self.assertEqual(msr.evaluate(auto, targets)["v91_blocking_after"], [])

    def test_dropped_column_no_longer_blocks(self):
        targets = {"v90_missing_columns": [],
                   "v91_blockers": [{"table": "t1", "column": "code"}],
                   "v92_auto_tables": []}
        self.assertEqual(msr.evaluate({}, targets)["v91_blocking_after"], [])

    def test_auto_increment_detection(self):
        targets = {"v90_missing_columns": [], "v91_blockers": [],
                   "v92_auto_tables": [{"table": "t1"}, {"table": "t2"},
                                       {"table": "t3"}]}
        cols = self._columns([("t1", "id", "NO", "<NULL>", "auto_increment"),
                              ("t2", "id", "NO", "<NULL>", "")])
        out = msr.evaluate(cols, targets)["v92_no_auto_after"]
        self.assertEqual(out, ["t2.id", "t3.id(missing_table)"])


class LoadTargetsTest(unittest.TestCase):
    def test_loads_three_evidence_files(self):
        evidence = (PRODUCTION_DIR.parents[1].parent / "docs"
                    / "enterprise-platform" / "evidence")
        if not evidence.is_dir():
            self.skipTest(f"evidence 目录不存在: {evidence}")
        targets = msr.load_targets(evidence)
        self.assertEqual(len(targets["v90_missing_columns"]), 71)
        self.assertEqual(len(targets["v91_blockers"]), 25)
        self.assertEqual(len(targets["v92_auto_tables"]), 95)
        for item in targets["v90_missing_columns"]:
            self.assertIsInstance(item["column"], str)


class SandboxGuardTest(unittest.TestCase):
    def test_plan_mentions_container_and_verify_sql(self):
        state = {"container_name": "zhicloud-mysql-sandbox-x", "image": "mysql:8.4"}
        plan = msr.build_plan(state, "zhicloud_shadow_1", "zhicloud_prod",
                              Path("/m"), Path("/v"))
        joined = "\n".join(plan)
        self.assertIn("zhicloud-mysql-sandbox-x", joined)
        self.assertIn("zhicloud_shadow_1", joined)
        self.assertIn("V92_verify.sql", joined)


if __name__ == "__main__":
    unittest.main()
