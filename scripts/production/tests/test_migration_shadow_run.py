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


class ShadowRunPasswordTransportTest(unittest.TestCase):
    """R-041/R-044：shadow runner 的 `argv` 前 6 项会被整体写进证据 JSON，
    所以 `-e MYSQL_PWD=<值>` 那种写法不是"只在进程表里可见"，而是**把明文口令落进证据文件**。
    这里既做静态断言，也把 run_docker 打桩做一次真调用。
    """

    def setUp(self):
        self.module = Path(__file__).resolve().parents[1] / "migration_shadow_run.py"
        self.src = self.module.read_text(encoding="utf-8")

    def test_argv_template_no_longer_interpolates_the_password(self):
        self.assertIn('"-e", "MYSQL_PWD",', self.src)
        self.assertEqual(0, self.src.count("MYSQL_PWD={state['root_password']}"))

    def test_built_command_line_carries_no_secret(self):
        import importlib.util

        spec = importlib.util.spec_from_file_location("msr_under_test", str(self.module))
        self.assertIsNotNone(spec)
        mod = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(mod)

        captured = {}

        def fake_run_docker(args, stdin_bytes=None, timeout=1800, env=None):
            captured["args"] = list(args)
            captured["env"] = dict(env or {})
            return {"exit_code": 0, "stdout": "", "stderr": ""}

        mod.run_docker = fake_run_docker
        state = {"container_name": "zhicloud-mysql-sandbox-deadbeef", "root_password": "SECRET-VALUE"}

        mod.mysql_exec(state, "SELECT 1")
        self.assertNotIn("SECRET-VALUE", " ".join(captured["args"]))
        self.assertIn("-e", captured["args"])
        self.assertIn("MYSQL_PWD", captured["args"])
        self.assertEqual("SECRET-VALUE", captured["env"].get("MYSQL_PWD"))

        mod.mysql_pipe(state, "echo x")
        self.assertNotIn("SECRET-VALUE", " ".join(captured["args"]))
        self.assertEqual("SECRET-VALUE", captured["env"].get("MYSQL_PWD"))


if __name__ == "__main__":
    unittest.main()
