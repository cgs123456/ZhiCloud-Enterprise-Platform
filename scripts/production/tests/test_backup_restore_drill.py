"""备份 / 恢复 / 回滚演练脚本的测试。

Task 0.5 的验收标准是「证明数据可以恢复」，而不是「备份命令返回 0」。因此这里除了
参数守卫，还专门覆盖恢复校验器：缺少行数或校验和、行数不一致、Flyway 历史不一致，
都必须判定为失败（退出码 2），而不是报成功。
"""

import json
import shutil
import subprocess
import tempfile
import unittest
from pathlib import Path


SCRIPT = Path(__file__).resolve().parents[1] / "backup-restore-drill.ps1"


def run_script(*args, timeout=180):
    return subprocess.run(
        ["pwsh", "-NoProfile", "-File", str(SCRIPT), *args],
        capture_output=True,
        text=True,
        encoding="utf-8",
        errors="replace",
        check=False,
        timeout=timeout,
    )


def write_state(
    path: Path,
    *,
    container: str = "zhicloud-mysql-sandbox-1a2b3c4d",
    host: str = "127.0.0.1",
    port: int = 60395,
    database: str = "zhicloud_prod",
) -> Path:
    path.write_text(
        json.dumps(
            {
                "container_name": container,
                "host": host,
                "port": port,
                "database": database,
                "root_password": "not-a-real-secret",
                "volume": "zhicloud-mysql-sandbox-data-1a2b3c4d",
            }
        ),
        encoding="utf-8",
    )
    return path


def write_snapshot(path: Path, **overrides) -> Path:
    snapshot = {
        "database": "zhicloud_prod",
        "generated_at": "2026-09-24T00:00:00+00:00",
        "probe_table": "zc_drill_probe",
        "row_counts": {"erp_customer": 12, "erp_sale_order": 7},
        "checksums": {"erp_customer": 918273, "erp_sale_order": 64512},
        "flyway": {"history_rows": 96, "max_version": "89"},
    }
    snapshot.update(overrides)
    path.write_text(json.dumps(snapshot), encoding="utf-8")
    return path


class DrillArgumentGuardTest(unittest.TestCase):
    def test_state_file_is_required(self):
        completed = run_script("drill")
        self.assertNotEqual(0, completed.returncode)
        self.assertIn("state", (completed.stdout + completed.stderr).lower())

    def test_missing_state_file_is_rejected(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            state = Path(temp_dir) / "state.json"
            completed = run_script("drill", "-SandboxState", str(state))
        self.assertNotEqual(0, completed.returncode)
        self.assertIn("state", (completed.stdout + completed.stderr).lower())

    def test_production_container_is_rejected(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            state = write_state(
                Path(temp_dir) / "state.json", container="zhicloud-prod-mysql"
            )
            completed = run_script("drill", "-SandboxState", str(state), "-DryRun")
        self.assertNotEqual(0, completed.returncode)
        self.assertIn("sandbox", (completed.stdout + completed.stderr).lower())

    def test_remote_host_is_rejected(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            state = write_state(
                Path(temp_dir) / "state.json", host="db.production.example.com"
            )
            completed = run_script("drill", "-SandboxState", str(state), "-DryRun")
        self.assertNotEqual(0, completed.returncode)
        self.assertIn("localhost", (completed.stdout + completed.stderr).lower())

    def test_target_database_must_differ_from_source(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            state = write_state(Path(temp_dir) / "state.json")
            completed = run_script(
                "drill", "-SandboxState", str(state), "-DryRun",
                "-TargetDatabase", "zhicloud_prod",
            )
        self.assertNotEqual(0, completed.returncode)

    def test_target_database_must_be_drill_scoped(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            state = write_state(Path(temp_dir) / "state.json")
            completed = run_script(
                "drill", "-SandboxState", str(state), "-DryRun",
                "-TargetDatabase", "zhicloud_core_business",
            )
        self.assertNotEqual(0, completed.returncode)
        self.assertIn("drill", (completed.stdout + completed.stderr).lower())

    def test_dryrun_emits_a_plan_without_touching_a_database(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            state = write_state(Path(temp_dir) / "state.json")
            completed = run_script(
                "drill", "-SandboxState", str(state), "-DryRun",
                "-TargetDatabase", "zhicloud_drill_1a2b3c4d",
            )
        self.assertEqual(0, completed.returncode, completed.stdout + completed.stderr)
        plan = json.loads(completed.stdout)
        self.assertTrue(plan["dry_run"])
        self.assertIn("backup", json.dumps(plan).lower())
        self.assertIn("restore", json.dumps(plan).lower())
        self.assertNotIn("not-a-real-secret", json.dumps(plan))


class RestoreVerificationTest(unittest.TestCase):
    def _verify(self, before: dict, after: dict):
        with tempfile.TemporaryDirectory() as temp_dir:
            root = Path(temp_dir)
            write_snapshot(root / "before.json", **before)
            write_snapshot(root / "after.json", **after)
            return run_script(
                "verify",
                "-BeforeSnapshot", str(root / "before.json"),
                "-AfterSnapshot", str(root / "after.json"),
            )

    def test_identical_snapshots_pass(self):
        completed = self._verify({}, {})
        self.assertEqual(0, completed.returncode, completed.stdout + completed.stderr)
        self.assertIn("passed", completed.stdout.lower())

    def test_missing_row_counts_is_rejected(self):
        completed = self._verify({}, {"row_counts": {}})
        self.assertNotEqual(0, completed.returncode)
        self.assertIn("row_counts", (completed.stdout + completed.stderr).lower())

    def test_missing_checksums_is_rejected(self):
        completed = self._verify({}, {"checksums": {}})
        self.assertNotEqual(0, completed.returncode)
        self.assertIn("checksum", (completed.stdout + completed.stderr).lower())

    def test_row_count_mismatch_is_rejected(self):
        completed = self._verify({}, {"row_counts": {"erp_customer": 11, "erp_sale_order": 7}})
        self.assertNotEqual(0, completed.returncode)
        self.assertIn("erp_customer", completed.stdout + completed.stderr)

    def test_checksum_mismatch_is_rejected(self):
        completed = self._verify({}, {"checksums": {"erp_customer": 918274, "erp_sale_order": 64512}})
        self.assertNotEqual(0, completed.returncode)
        self.assertIn("erp_customer", completed.stdout + completed.stderr)

    def test_flyway_history_mismatch_is_rejected(self):
        completed = self._verify({}, {"flyway": {"history_rows": 95, "max_version": "89"}})
        self.assertNotEqual(0, completed.returncode)
        self.assertIn("flyway", (completed.stdout + completed.stderr).lower())

    def test_missing_probe_table_is_rejected(self):
        completed = self._verify({}, {"probe_table": ""})
        self.assertNotEqual(0, completed.returncode)
        self.assertIn("probe", (completed.stdout + completed.stderr).lower())


class LiveDictionaryCompareTest(unittest.TestCase):
    """覆盖真实演练路径：Compare-Snapshots 收到的是 [ordered]@{}（OrderedDictionary），
    而不是 `verify` 命令里由 JSON 反序列化出来的 PSCustomObject。

    回归背景：2026-09-24 首次真机演练时，`$before.row_counts.PSObject.Properties` 对
    OrderedDictionary 返回的是 Count/IsReadOnly/Keys/Values 这些 CLR 属性，
    导致 `[int64]$afterValue.Value` 抛异常、演练被 abort。之前的测试只走 JSON 路径，
    所以全绿却没发现真实路径是坏的。
    """

    def test_live_ordered_dictionary_snapshots_compare(self):
        completed = run_script("selftest")
        self.assertEqual(0, completed.returncode, completed.stdout + completed.stderr)
        payload = json.loads(completed.stdout)
        self.assertTrue(payload["passed"], payload)
        self.assertTrue(payload["live_dictionary"], payload)
        self.assertEqual([], payload["identical_reasons"])
        self.assertEqual(2, payload["table_count"], payload)
        self.assertTrue(payload["checksum_keys_normalized"], payload)
        joined = " ".join(payload["drift_reasons"])
        self.assertIn("row_count_mismatch", joined)
        self.assertIn("erp_customer", joined)


class PasswordTransportTest(unittest.TestCase):
    """R-041：口令只能经进程环境继承给 `-e MYSQL_PWD`，不能出现在 docker 命令行里。

    这条断言之所以需要单独存在：同一个缺陷形态曾被别的测试**反向钉住**（改好脚本反而变红），
    所以这里既断言安全形态存在，也断言泄漏形态不存在。
    """

    @classmethod
    def setUpClass(cls):
        if not SCRIPT.is_file():
            raise unittest.SkipTest(f'missing {SCRIPT}')
        cls.text = SCRIPT.read_text(encoding='utf-8-sig')

    def test_root_password_never_enters_argv(self):
        self.assertEqual(0, self.text.count('MYSQL_PWD=$'), '口令被拼进 docker exec 的 -e 参数值')
        self.assertIn("[Environment]::SetEnvironmentVariable('MYSQL_PWD', $RootPassword, 'Process')", self.text)
        self.assertIn("'-e', 'MYSQL_PWD',", self.text)

    def test_every_container_call_site_declares_its_own_env(self):
        # 三个容器调用点（SQL / mysqldump / shell）各自声明；漏一处就是漏一处缺陷，
        # 而只在入口设置一次会让将来新增的宿主侧客户端静默继承 MYSQL_PWD。
        self.assertEqual(3, self.text.count('Set-DrillMysqlPasswordEnv -RootPassword $script:RootPassword'))
        self.assertEqual(3, self.text.count("'-e', 'MYSQL_PWD',"))


if __name__ == "__main__":
    unittest.main()