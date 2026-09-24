import json
import os
import re
import shutil
import subprocess
import tempfile
import unittest
from pathlib import Path

SCRIPT = Path(__file__).resolve().parents[1] / "mysql-sandbox.ps1"
HEALTH_SQL = Path(__file__).resolve().parents[1] / "sql" / "sandbox_health.sql"

READ_ONLY_FORBIDDEN = re.compile(
    r"\b(INSERT|UPDATE|DELETE|DROP|ALTER|CREATE|TRUNCATE|REPLACE|GRANT|REVOKE)\b",
    re.IGNORECASE,
)


def docker_available():
    docker = shutil.which("docker")
    if docker is None:
        return False
    probe = subprocess.run(
        [docker, "info", "--format", "{{.ServerVersion}}"],
        capture_output=True, text=True, check=False, timeout=60,
    )
    return probe.returncode == 0


def run_script(*args, timeout=120):
    return subprocess.run(
        ["pwsh", "-NoProfile", "-File", str(SCRIPT), *args],
        capture_output=True,
        text=True,
        encoding="utf-8",
        errors="replace",
        check=False,
        timeout=timeout,
    )


class SandboxHealthSqlTest(unittest.TestCase):
    def test_health_sql_is_read_only(self):
        text = HEALTH_SQL.read_text(encoding="utf-8")
        stripped = "\n".join(
            line for line in text.splitlines() if not line.strip().startswith("--")
        )
        match = READ_ONLY_FORBIDDEN.search(stripped)
        self.assertIsNone(match, f"health SQL must stay read-only, found: {match}")

    def test_health_sql_reports_required_metrics(self):
        text = HEALTH_SQL.read_text(encoding="utf-8")
        for metric in (
            "server_version",
            "character_set_server",
            "collation_server",
            "session_time_zone",
            "sql_mode",
            "sandbox_database_present",
        ):
            self.assertIn(metric, text)


class SandboxArgumentGuardTest(unittest.TestCase):
    def test_unknown_command_is_rejected(self):
        completed = run_script("bogus-command")
        self.assertNotEqual(0, completed.returncode)
        self.assertIn("bogus-command", completed.stdout + completed.stderr)

    def test_predictable_container_name_is_rejected(self):
        completed = run_script("start", "-DryRun", "-Name", "zhicloud-mysql-sandbox")
        self.assertNotEqual(0, completed.returncode)
        self.assertIn("random", (completed.stdout + completed.stderr).lower())

    def test_production_looking_container_name_is_rejected(self):
        completed = run_script("start", "-DryRun", "-Name", "zhicloud-prod-mysql")
        self.assertNotEqual(0, completed.returncode)
        self.assertIn("random", (completed.stdout + completed.stderr).lower())

    def test_non_localhost_bind_address_is_rejected(self):
        for address in ("0.0.0.0", "10.0.0.5", "::"):
            with self.subTest(address=address):
                completed = run_script("start", "-DryRun", "-BindAddress", address)
                self.assertNotEqual(0, completed.returncode)
                self.assertIn("localhost", (completed.stdout + completed.stderr).lower())

    def test_remote_database_host_is_rejected(self):
        completed = run_script("health", "-DatabaseHost", "db.production.example.com")
        self.assertNotEqual(0, completed.returncode)
        self.assertIn("localhost", (completed.stdout + completed.stderr).lower())

    def test_mutable_image_tag_is_rejected(self):
        completed = run_script("start", "-DryRun", "-Image", "mysql:latest")
        self.assertNotEqual(0, completed.returncode)
        self.assertIn("latest", (completed.stdout + completed.stderr).lower())

    def test_unpinned_image_is_rejected(self):
        completed = run_script("start", "-DryRun", "-Image", "mysql:8.4")
        self.assertNotEqual(0, completed.returncode)
        self.assertIn("digest", (completed.stdout + completed.stderr).lower())

    def test_default_image_is_locked_to_digest(self):
        completed = run_script("start", "-DryRun", "-RepoRoot", str(Path(__file__).resolve().parents[3]))
        self.assertEqual(0, completed.returncode, completed.stdout + completed.stderr)
        payload = json.loads(completed.stdout)
        self.assertTrue(payload["image"].startswith("mysql:8.4@sha256:"))
        self.assertIn("--character-set-server=utf8mb4", payload["docker_run_args"])
        self.assertIn("--default-time-zone=+08:00", payload["docker_run_args"])
        self.assertIn("127.0.0.1::3306", payload["docker_run_args"])
        self.assertTrue(all("0.0.0.0" not in arg for arg in payload["docker_run_args"]))

    @unittest.skipUnless(docker_available(), "docker daemon is not available")
    def test_existing_volume_reuse_is_rejected(self):
        volume = "zhicloud-mysql-sandbox-data-deadbeef"
        docker = shutil.which("docker")
        subprocess.run([docker, "volume", "rm", "-f", volume], capture_output=True, check=False)
        subprocess.run([docker, "volume", "create", volume], capture_output=True, check=True)
        try:
            completed = run_script("start", "-DryRun", "-Volume", volume)
        finally:
            subprocess.run([docker, "volume", "rm", "-f", volume], capture_output=True, check=False)

        self.assertNotEqual(0, completed.returncode)
        self.assertIn("volume", (completed.stdout + completed.stderr).lower())

    def test_health_without_state_is_rejected(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            state = Path(temp_dir) / "state.json"
            completed = run_script("health", "-StateFile", str(state))
        self.assertNotEqual(0, completed.returncode)
        self.assertIn("state", (completed.stdout + completed.stderr).lower())


@unittest.skipUnless(docker_available(), "docker daemon is not available")
class SandboxLifecycleTest(unittest.TestCase):
    def _sandbox_args(self, temp_dir):
        return [
            "-StateFile", str(Path(temp_dir) / "state.json"),
            "-EvidenceFile", str(Path(temp_dir) / "evidence.json"),
        ]

    def _force_cleanup(self, temp_dir, box):
        """兜底回收：state 仍在就走脚本 stop，否则按名字精确删除本次沙箱资源。"""
        temp_dir = Path(temp_dir)
        state_file = temp_dir / "state.json"
        if state_file.exists():
            run_script("stop", *self._sandbox_args(temp_dir), "-SkipEvidence", timeout=300)
            if not state_file.exists():
                return

        docker = shutil.which("docker")
        if docker is None or not box.get("container_name"):
            return
        subprocess.run(
            [docker, "rm", "--force", box["container_name"]],
            capture_output=True, check=False,
        )
        subprocess.run(
            [docker, "volume", "rm", "--force", box["volume"]],
            capture_output=True, check=False,
        )

    def test_start_health_stop_roundtrip(self):
        temp = tempfile.TemporaryDirectory()
        # addCleanup 后进先出：先注册临时目录清理，保证兜底清理执行时 state 文件还在。
        self.addCleanup(temp.cleanup)
        temp_dir = temp.name
        box = {"container_name": None, "volume": None}
        self.addCleanup(self._force_cleanup, temp_dir, box)

        args = self._sandbox_args(temp_dir)
        started = run_script("start", *args, timeout=300)
        self.assertEqual(0, started.returncode, started.stdout + started.stderr)

        state = json.loads((Path(temp_dir) / "state.json").read_text(encoding="utf-8"))
        container = state["container_name"]
        volume = state["volume"]
        box.update(state)
        self.assertRegex(container, r"^zhicloud-mysql-sandbox-[0-9a-f]{8}$")
        self.assertRegex(volume, r"^zhicloud-mysql-sandbox-data-[0-9a-f]{8}$")

        docker = shutil.which("docker")
        published = subprocess.run(
            [docker, "port", container, "3306/tcp"],
            capture_output=True, text=True, check=True,
        ).stdout.strip()
        self.assertTrue(published.startswith("127.0.0.1:"), published)

        health = run_script("health", *args, timeout=300)
        self.assertEqual(0, health.returncode, health.stdout + health.stderr)
        evidence = json.loads((Path(temp_dir) / "evidence.json").read_text(encoding="utf-8"))
        self.assertEqual("passed", evidence["status"])
        self.assertEqual("utf8mb4", evidence["metrics"]["character_set_server"])
        self.assertTrue(evidence["metrics"]["collation_server"].startswith("utf8mb4"))
        self.assertIn(evidence["metrics"]["session_time_zone"], ("+08:00", "Asia/Shanghai"))
        self.assertEqual("08:00:00", evidence["metrics"]["now_utc_offset"])
        self.assertIn("STRICT_TRANS_TABLES", evidence["metrics"]["sql_mode"])
        self.assertTrue(evidence["metrics"]["server_version"].startswith("8.4"))
        self.assertEqual("yes", evidence["metrics"]["sandbox_database_present"])
        self.assertNotIn("password", json.dumps(evidence).lower())
        self.assertEqual([], evidence["failure_reasons"])

        stopped = run_script("stop", *args, timeout=300)
        self.assertEqual(0, stopped.returncode, stopped.stdout + stopped.stderr)
        inspect = subprocess.run(
            [docker, "inspect", container],
            capture_output=True, text=True, check=False,
        )
        self.assertNotEqual(0, inspect.returncode, "container must be removed")
        volume_ls = subprocess.run(
            [docker, "volume", "ls", "--format", "{{.Name}}"],
            capture_output=True, text=True, check=True,
        ).stdout.split()
        self.assertNotIn(volume, volume_ls, "temporary volume must not be reused or left behind")
        self.assertFalse((Path(temp_dir) / "state.json").exists())

if __name__ == "__main__":
    unittest.main()
