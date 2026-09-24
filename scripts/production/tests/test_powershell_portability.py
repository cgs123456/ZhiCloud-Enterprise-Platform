"""PowerShell 脚本可移植性门禁。

背景（2026-09-24 实测）：仓库里的 .ps1 带中文注释，但若保存为「UTF-8 无 BOM +
LF」，Windows PowerShell 5.1 会按系统 ANSI 代码页（cp936/GBK）解码。GBK 解码器
在遇到 UTF-8 中以 0x81-0xFE 结尾的汉字（如“。”= E3 80 82）时会吞掉紧随的换行符，
把两行合并成一行，进而产生语法错误。

实证三类脚本：
- scripts/production/verify-build.ps1 与 mysql-sandbox.ps1：本轮发现的缺陷，已修。
- scripts/download-bge-model.ps1：上游既有缺陷，在 5.1 下 L77/L141 报
  UnexpectedToken，而脚本自述支持 “Windows PowerShell 5.1+”。

修复方式统一为：UTF-8 带 BOM（5.1 与 7 都能正确解码），并在本测试中固化门禁。
"""

import shutil
import subprocess
import tempfile
import unittest
from pathlib import Path


TESTS_DIR = Path(__file__).resolve().parent
PRODUCTION_DIR = TESTS_DIR.parent
REPO_ROOT = PRODUCTION_DIR.parents[1]
WINDOWS_POWERSHELL = shutil.which("powershell.exe")

PARSE_HELPER = """
param([string[]]$Files)

$tokens = $null
foreach ($file in $Files) {
    $errors = $null
    [System.Management.Automation.Language.Parser]::ParseFile(
        $file, [ref]$tokens, [ref]$errors) | Out-Null
    foreach ($errorRecord in @($errors)) {
        "{0}`t{1}`t{2}" -f $file, $errorRecord.Extent.StartLineNumber, $errorRecord.ErrorId
    }
}
""".lstrip()


def powershell_scripts() -> list[Path]:
    if not REPO_ROOT.is_dir():
        return []
    return sorted(REPO_ROOT.rglob("*.ps1"))


def parse_errors(host: str, scripts: list[Path]) -> list[str]:
    with tempfile.TemporaryDirectory() as temp_dir:
        helper = Path(temp_dir) / "parse-check.ps1"
        helper.write_text(PARSE_HELPER, encoding="utf-8")
        completed = subprocess.run(
            [
                host,
                "-NoProfile",
                "-ExecutionPolicy", "Bypass",
                "-File", str(helper),
                "-Files", *[str(path) for path in scripts],
            ],
            capture_output=True,
            text=True,
            encoding="utf-8",
            errors="replace",
            check=False,
            timeout=120,
        )
        if completed.returncode != 0:
            raise AssertionError(
                f"{host} failed to run the parse helper: {completed.stdout}{completed.stderr}"
            )
        return [line for line in completed.stdout.splitlines() if line.strip()]


class PowerShellScriptPortabilityTest(unittest.TestCase):
    def test_repo_contains_powershell_scripts(self):
        self.assertTrue(
            powershell_scripts(),
            f"no .ps1 found under {REPO_ROOT}; the gate would vacuously pass",
        )

    def test_non_ascii_scripts_carry_utf8_bom(self):
        offenders = []
        for script in powershell_scripts():
            raw = script.read_bytes()
            if any(byte > 127 for byte in raw):
                if not raw.startswith(b"\xef\xbb\xbf"):
                    offenders.append(str(script.relative_to(REPO_ROOT)))
        self.assertEqual(
            [],
            offenders,
            "scripts with non-ASCII bytes must be UTF-8 with BOM, otherwise "
            "Windows PowerShell 5.1 decodes them as ANSI",
        )

    @unittest.skipIf(WINDOWS_POWERSHELL is None, "Windows PowerShell is not available")
    def test_scripts_parse_under_powershell_7_and_51(self):
        scripts = powershell_scripts()
        report = {}
        for host in ("pwsh", WINDOWS_POWERSHELL):
            report[host] = parse_errors(host, scripts)

        for host, lines in report.items():
            self.assertEqual(
                [],
                lines,
                f"{host} could not parse every PowerShell script in the repository",
            )


if __name__ == "__main__":
    unittest.main()