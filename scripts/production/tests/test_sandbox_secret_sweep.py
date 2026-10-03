# -*- coding: utf-8 -*-
"""R-043 补偿控制的行为级测试：SandboxSecretSweep.ps1。

这里刻意**不用 skip**：pwsh 找不到就是 error，不是 pass。
一个"环境缺工具所以自动跳过"的守卫永远绿，也永远什么都没守。

被测契约三条：只认精确形状、只删足够老的、其它一律不碰。
"""
from __future__ import annotations

import json
import os
import shutil
import subprocess
import tempfile
import unittest
from datetime import datetime, timedelta, timezone
from pathlib import Path

MODULE = Path(__file__).resolve().parents[1] / "SandboxSecretSweep.ps1"
KNOWN_PWSH = Path(r"C:\tools\powershell-7.6.6\pwsh.exe")
GUID_A = "a" * 32
GUID_B = "b" * 32

HARNESS_TEMPLATE = """$ErrorActionPreference = 'Stop'
. '@@MODULE@@'
$now = [datetime]::Parse('@@NOW@@')
$out = Clear-StaleRunnerEnvFiles -DirectoryPath '@@DIR@@' -MinAgeHours @@AGE@@ -Now $now
$out | ConvertTo-Json -Depth 6 -Compress | Write-Output
"""


def find_pwsh() -> str:
    found = shutil.which("pwsh")
    if found:
        return found
    if KNOWN_PWSH.is_file():
        return str(KNOWN_PWSH)
    raise AssertionError(
        f"pwsh 不可用（PATH 与 {KNOWN_PWSH} 都没有）：本测试不允许跳过，否则守卫形同虚设")


class StaleRunnerEnvSweepTest(unittest.TestCase):
    def setUp(self):
        self.base = Path(tempfile.mkdtemp(prefix="zc-sweep-"))
        self.fixtures = self.base / "fixtures"
        self.fixtures.mkdir()
        self.harness = self.base / "harness.ps1"
        self.now_utc = datetime.now(timezone.utc).replace(microsecond=0)
        # 传"本地墙上时间"给 PowerShell：[datetime]::Parse 对无后缀串按本地时区解释，
        # 函数内部再 ToUniversalTime，与 os.utime 写入的绝对时刻才对得上。
        self.now_local_text = self.now_utc.astimezone().replace(tzinfo=None).isoformat(sep=" ")

    def tearDown(self):
        shutil.rmtree(self.base, ignore_errors=True)

    def _make(self, name: str, age_hours: float) -> Path:
        p = self.fixtures / name
        p.write_text("MASTER_DATASOURCE_PASSWORD=placeholder-not-a-real-secret\n", encoding="utf-8")
        stamp = (self.now_utc - timedelta(hours=age_hours)).timestamp()
        os.utime(p, (stamp, stamp))
        return p

    def _run(self, age_hours: int) -> dict:
        text = (HARNESS_TEMPLATE
                .replace("@@MODULE@@", MODULE.as_posix())
                .replace("@@NOW@@", self.now_local_text)
                .replace("@@DIR@@", self.fixtures.as_posix())
                .replace("@@AGE@@", str(int(age_hours))))
        self.harness.write_text(text, encoding="utf-8")
        proc = subprocess.run([find_pwsh(), "-NoProfile", "-ExecutionPolicy", "Bypass",
                               "-File", str(self.harness)],
                              capture_output=True, text=True, encoding="utf-8",
                              errors="replace", timeout=180)
        if proc.returncode != 0:
            raise AssertionError(f"harness exit={proc.returncode} stderr={proc.stderr.strip()[:400]}")
        line = [l for l in proc.stdout.splitlines() if l.strip().startswith("{")][-1]
        return json.loads(line)

    def test_only_exact_shape_and_only_old_files_are_removed(self):
        old_match = self._make(f"zhicloud-runner-env-{GUID_A}.txt", 48)
        fresh_match = self._make(f"zhicloud-runner-env-{GUID_B}.txt", 1)
        wrong_shape = self._make("zhicloud-runner-env-not-a-guid.txt", 96)
        unrelated = self._make("notes.txt", 96)

        out = self._run(24)

        # 扫描数必须等于夹具数：对不上就说明"没扫到"被当成了"没有可删的"。
        self.assertEqual(4, out["scanned_files"])
        self.assertEqual(1, out["candidates"])
        self.assertEqual([old_match.name], [r["name"] for r in out["removed"]])
        self.assertEqual([], out["failed"])
        self.assertFalse(old_match.exists())
        for survivor in (fresh_match, wrong_shape, unrelated):
            self.assertTrue(survivor.exists(), f"{survivor.name} 不该被删")

    def test_age_threshold_of_zero_is_refused(self):
        target = self._make(f"zhicloud-runner-env-{GUID_A}.txt", 48)
        proc = subprocess.run(
            [find_pwsh(), "-NoProfile", "-ExecutionPolicy", "Bypass", "-Command",
             f". '{MODULE.as_posix()}'; "
             f"Clear-StaleRunnerEnvFiles -DirectoryPath '{self.fixtures.as_posix()}' -MinAgeHours 0"],
            capture_output=True, text=True, encoding="utf-8", errors="replace", timeout=180)
        self.assertNotEqual(0, proc.returncode, "MinAgeHours=0 会删掉并发运行正在用的文件，必须拒绝")
        self.assertIn("MinAgeHours", proc.stderr + proc.stdout)
        self.assertTrue(target.exists(), "被拒绝的调用不能已经先把文件删了")

    def test_missing_directory_reports_skipped_instead_of_inventing_success(self):
        proc = subprocess.run(
            [find_pwsh(), "-NoProfile", "-ExecutionPolicy", "Bypass", "-Command",
             f". '{MODULE.as_posix()}'; "
             f"(Clear-StaleRunnerEnvFiles -DirectoryPath '{(self.base / 'nope').as_posix()}' "
             "| ConvertTo-Json -Compress)"],
            capture_output=True, text=True, encoding="utf-8", errors="replace", timeout=180)
        self.assertEqual(0, proc.returncode, proc.stderr[:300])
        payload = json.loads(proc.stdout.strip().splitlines()[-1])
        self.assertEqual("directory-missing", payload["Skipped"])
        self.assertEqual(0, payload["Candidates"])

    def test_module_is_bom_plus_lf_and_exports_both_functions(self):
        raw = MODULE.read_bytes()
        self.assertTrue(raw.startswith(b"\xef\xbb\xbf"), "5.1 需要 BOM 才能读非 ASCII 的 .ps1")
        self.assertNotIn(b"\r\n", raw, "必须是 LF")
        text = raw.decode("utf-8-sig")
        self.assertIn("function Get-StaleRunnerEnvFile", text)
        self.assertIn("function Clear-StaleRunnerEnvFiles", text)
        # 形状判定必须收紧到 GUID('N') + .txt，否则前缀相似的用户文件会被误删
        self.assertIn("'^zhicloud-runner-env-[0-9a-f]{32}\\.txt$'", text)


if __name__ == "__main__":
    unittest.main()
