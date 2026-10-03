"""Windows PowerShell 可移植性门禁 CLI。

为什么要有这个 CLI：仓库里的 .ps1 带中文注释，若存成「UTF-8 无 BOM」，
Windows PowerShell 5.1 会按系统 ANSI 代码页（cp936/GBK）解码，GBK 解码器遇到
UTF-8 中以 0x81-0xFE 结尾的汉字时会吞掉紧随的换行，把两行合并成一行并产生语法错误。
unittest 只能给人看，CI 与审计需要一份机器可读证据，所以这里同时提供：
  - 编码规则检查（含非 ASCII 字节的 .ps1 必须带 UTF-8 BOM）；
  - 在 pwsh 7 与 Windows PowerShell 5.1 下逐个解析，记录解析错误；
  - `--json` 打印证据、`--output` 落盘（UTF-8 无 BOM、可严格解析）。

退出码：0=通过；2=门禁失败；1=用法或内部错误。
"""

import argparse
import json
import shutil
import subprocess
import sys
import tempfile
from datetime import datetime, timezone
from pathlib import Path


PARSE_HELPER = """param([string[]]$Files)

$tokens = $null
foreach ($file in $Files) {
    $errors = $null
    [System.Management.Automation.Language.Parser]::ParseFile(
        $file, [ref]$tokens, [ref]$errors) | Out-Null
    foreach ($errorRecord in @($errors)) {
        "{0}`t{1}`t{2}" -f $file, $errorRecord.Extent.StartLineNumber, $errorRecord.ErrorId
    }
}
"""


def utc_now_iso() -> str:
    return datetime.now(timezone.utc).isoformat()


def host_version(host: str) -> str:
    try:
        completed = subprocess.run(
            [host, "-NoProfile", "-Command", "$PSVersionTable.PSVersion.ToString()"],
            capture_output=True, text=True, encoding="utf-8", errors="replace",
            check=False, timeout=60,
        )
    except (OSError, subprocess.SubprocessError):
        return ""
    if completed.returncode != 0:
        return ""
    return completed.stdout.strip()


def discover_scripts(repo_root: Path) -> list[Path]:
    if not repo_root.is_dir():
        return []
    return sorted(repo_root.rglob("*.ps1"))


def find_bom_offenders(repo_root: Path, scripts: list[Path]) -> list[str]:
    offenders = []
    for script in scripts:
        raw = script.read_bytes()
        if any(byte > 127 for byte in raw) and not raw.startswith(b"\xef\xbb\xbf"):
            try:
                offenders.append(str(script.relative_to(repo_root)))
            except ValueError:
                offenders.append(str(script))
    return offenders


def parse_errors(host: str, scripts: list[Path]) -> list[str]:
    if not scripts:
        return []
    with tempfile.TemporaryDirectory() as temp_dir:
        helper = Path(temp_dir) / "parse-check.ps1"
        helper.write_text(PARSE_HELPER, encoding="utf-8")
        completed = subprocess.run(
            [host, "-NoProfile", "-ExecutionPolicy", "Bypass", "-File", str(helper),
             "-Files", *[str(path) for path in scripts]],
            capture_output=True, text=True, encoding="utf-8", errors="replace",
            check=False, timeout=180,
        )
    if completed.returncode != 0:
        return [f"{host} failed to run the parse helper: {completed.stderr.strip()}"]
    return [line for line in completed.stdout.splitlines() if line.strip()]


def current_commit(repo_root: Path) -> str:
    try:
        completed = subprocess.run(
            ["git", "rev-parse", "HEAD"], cwd=str(repo_root),
            capture_output=True, text=True, encoding="utf-8", errors="replace",
            check=False, timeout=30,
        )
    except (OSError, subprocess.SubprocessError):
        return ""
    if completed.returncode != 0:
        return ""
    return completed.stdout.strip()


def build_report(repo_root: Path) -> dict:
    scripts = discover_scripts(repo_root)
    bom_offenders = find_bom_offenders(repo_root, scripts)

    hosts = {}
    candidates = {"pwsh": shutil.which("pwsh"), "windows_powershell": shutil.which("powershell.exe")}
    for name, path in candidates.items():
        if not path:
            continue
        hosts[name] = {
            "path": path,
            "version": host_version(path),
            "parse_errors": parse_errors(path, scripts),
        }

    failure_reasons = []
    if bom_offenders:
        failure_reasons.append(f"bom_offenders: {len(bom_offenders)} script(s)")
    for name, report in hosts.items():
        if report["parse_errors"]:
            failure_reasons.append(f"parse_errors[{name}]: {len(report['parse_errors'])}")

    return {
        "artifact": "powershell-portability",
        "status": "passed" if not failure_reasons else "failed",
        "generated_at": utc_now_iso(),
        "repo_root": str(repo_root),
        "commit": current_commit(repo_root),
        "script_count": len(scripts),
        "rule": "scripts containing non-ASCII bytes must be UTF-8 with BOM",
        "bom_offenders": bom_offenders,
        "hosts": hosts,
        "failure_reasons": failure_reasons,
    }


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Windows PowerShell portability gate")
    parser.add_argument("--repo-root", default=str(Path(__file__).resolve().parents[2]))
    parser.add_argument("--json", action="store_true", help="print the evidence JSON to stdout")
    parser.add_argument("--output", help="write the evidence JSON to this path (UTF-8, no BOM)")
    args = parser.parse_args(argv)

    repo_root = Path(args.repo_root).resolve()
    if not repo_root.is_dir():
        print(f"repo root not found: {repo_root}", file=sys.stderr)
        return 1

    report = build_report(repo_root)
    payload = json.dumps(report, ensure_ascii=False, indent=2) + "\n"

    if args.output:
        out_path = Path(args.output)
        out_path.parent.mkdir(parents=True, exist_ok=True)
        out_path.write_bytes(payload.encode("utf-8"))
        print(f"evidence written to {out_path}")
    if args.json or not args.output:
        sys.stdout.write(payload)

    return 0 if report["status"] == "passed" else 2


if __name__ == "__main__":
    raise SystemExit(main())
