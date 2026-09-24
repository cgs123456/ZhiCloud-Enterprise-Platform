"""powershell_portability.py 门禁 CLI 的测试。

为什么需要 CLI：unittest 只能给人看，CI 与审计需要一份机器可读证据
（脚本数量、两个宿主、解析错误、BOM 违规）。本测试先于实现编写。
"""

import json
import shutil
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path


TESTS_DIR = Path(__file__).resolve().parent
PRODUCTION_DIR = TESTS_DIR.parent
REPO_ROOT = PRODUCTION_DIR.parents[1]
SCRIPT = PRODUCTION_DIR / "powershell_portability.py"


def run_cli(*args, timeout=300):
    return subprocess.run(
        [sys.executable, str(SCRIPT), *args],
        capture_output=True,
        text=True,
        encoding="utf-8",
        errors="replace",
        check=False,
        timeout=timeout,
    )


class PowerShellPortabilityCliTest(unittest.TestCase):
    def test_reports_zero_parse_errors_for_this_repository(self):
        completed = run_cli("--repo-root", str(REPO_ROOT), "--json")
        self.assertEqual(0, completed.returncode, completed.stdout + completed.stderr)
        payload = json.loads(completed.stdout)
        self.assertEqual("passed", payload["status"], payload)
        self.assertGreater(payload["script_count"], 0)
        self.assertEqual([], payload["bom_offenders"])
        self.assertTrue(payload["hosts"], payload)
        for host, report in payload["hosts"].items():
            self.assertEqual([], report["parse_errors"], f"{host}: {report}")
        self.assertIn("commit", payload)

    def test_detects_non_ascii_script_without_bom(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            root = Path(temp_dir)
            (root / "bad.ps1").write_text("# 中文注释\nWrite-Output 1\n", encoding="utf-8")
            completed = run_cli("--repo-root", str(root), "--json")
        self.assertEqual(2, completed.returncode, completed.stdout + completed.stderr)
        payload = json.loads(completed.stdout)
        self.assertEqual("failed", payload["status"], payload)
        self.assertIn("bad.ps1", " ".join(payload["bom_offenders"]))
        self.assertIn("bom_offenders", " ".join(payload["failure_reasons"]))

    def test_evidence_json_is_strict_and_bom_free(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            out = Path(temp_dir) / "portability.json"
            completed = run_cli("--repo-root", str(REPO_ROOT), "--output", str(out))
            self.assertEqual(0, completed.returncode, completed.stdout + completed.stderr)
            raw = out.read_bytes()
        self.assertFalse(raw.startswith(b"\xef\xbb\xbf"), "evidence JSON must be UTF-8 without BOM")
        payload = json.loads(raw.decode("utf-8"))
        self.assertEqual("passed", payload["status"], payload)

    @unittest.skipIf(shutil.which("pwsh") is None, "pwsh is not available")
    def test_pwsh_version_is_recorded(self):
        payload = json.loads(run_cli("--repo-root", str(REPO_ROOT), "--json").stdout)
        pwsh = payload["hosts"].get("pwsh")
        self.assertIsNotNone(pwsh, payload)
        self.assertTrue(pwsh["version"], pwsh)


if __name__ == "__main__":
    unittest.main()
