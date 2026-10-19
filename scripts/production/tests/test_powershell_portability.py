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

import os
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
    _EXCLUDED = {"node_modules", ".git", "target", "dist", "build", "__pycache__"}
    result = []
    for dirpath, dirnames, filenames in os.walk(REPO_ROOT):
        dirnames[:] = [d for d in dirnames if d not in _EXCLUDED]
        for fn in filenames:
            if fn.endswith(".ps1"):
                result.append(Path(dirpath) / fn)
    return sorted(result)


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

    def test_parse_helper_reports_a_script_that_is_not_parseable(self):
        """本门禁自己的"会咬"证明：一份确定不可解析的 .ps1 必须让 helper 报出至少一行。

        为什么单独立这条：`parse_errors()` 的返回值是"行列表"，`[]` 既可以是"全部可解析"，
        也可以是"helper 根本没把错误收集起来"（例如 `[ref]` 变量未初始化时，
        PowerShell 会抛 `InvalidOperation: [ref] cannot be applied to a variable that does
        not exist`，而调用方只看 stdout —— 于是**坏文件也报 0 错**）。
        计划 §验证 第 7 条那种一行式 `[ref]\$null, [ref]\$e` 就是这种假绿：实测把一份语法错误的
        文件喂给它，输出仍是 `PARSE_ERRORS=0`。没有这条负向用例，整仓库"两宿主 parse_errors=0"
        的所有读数都可能建立在同一个静默失败上。
        """
        broken = "function Broken {\n  $value = 1\n"          # 少一个右大括号：确定性语法错误
        good = "function Fine {\n  param([string]$Name)\n}\n"
        with tempfile.TemporaryDirectory() as temp_dir:
            bad_file = Path(temp_dir) / "broken.ps1"
            bad_file.write_text(broken, encoding="utf-8")
            good_file = Path(temp_dir) / "good.ps1"
            good_file.write_text(good, encoding="utf-8")
            hosts = ["pwsh"] + ([WINDOWS_POWERSHELL] if WINDOWS_POWERSHELL else [])
            for host in hosts:
                with self.subTest(host=host):
                    lines = parse_errors(host, [bad_file])
                    self.assertTrue(lines,
                                    f"{host} 对一份语法错误的 .ps1 报 0 错：门禁在静默失败")
                    self.assertIn(str(bad_file), lines[0])
                    self.assertEqual([], parse_errors(host, [good_file]),
                                     f"{host} 对合法文件报错：判定过严会淹没真信号")

    def test_parse_helper_command_form_initializes_the_error_variable(self):
        """钉住 helper 的写法本身：`$errors = $null` 必须在每个文件的 ParseFile 之前出现。

        只测行为不测写法，将来有人"顺手简化"成 `[ref]$null` 就又回到假绿；
        而这正是我在计划 §验证 里抄到的那个形状。
        """
        self.assertIn("$errors = $null", PARSE_HELPER)
        self.assertIn("[ref]$errors", PARSE_HELPER)
        self.assertNotIn("[ref]$null", PARSE_HELPER.replace("[ref]$tokens", ""),
                         "错误收集变量必须显式初始化，不能传 [ref]$null")


if __name__ == "__main__":
    unittest.main()