#!/usr/bin/env python3
"""检测并修复「工作区文件行尾符与 Git HEAD 版本不一致」造成的虚假 diff。

背景：在 Windows 上用某些编辑器改动文件时，可能把原本 LF 行尾的文件整体
转成 CRLF，导致 `git diff` 把整个文件标记为重写（例如只改 1 行却显示 59 行变更），
严重干扰 code review。

用法：
    python scripts/normalize_eol.py            # 仅检测（dry-run）
    python scripts/normalize_eol.py --fix      # 检测并把行尾改回与 HEAD 一致
"""
import subprocess
import sys
from pathlib import Path


def git(*args: str) -> str:
    return subprocess.run(
        ["git", *args], capture_output=True, check=True
    ).stdout.decode("utf-8", "replace")


def git_bytes(*args: str) -> bytes:
    return subprocess.run(["git", *args], capture_output=True, check=True).stdout


def eol_kind(data: bytes) -> str:
    crlf = data.count(b"\r\n")
    lf = data.count(b"\n") - crlf
    if crlf and lf:
        return "MIXED"
    if crlf:
        return "CRLF"
    if lf:
        return "LF"
    return "NONE"


def main() -> int:
    fix = "--fix" in sys.argv
    # 仅处理已跟踪且被修改（非删除）的文件
    modified = [
        line[3:].strip()
        for line in git("status", "--porcelain").splitlines()
        if line[:2].strip() in {"M", "MM", "AM"} and line[:2] != "??"
    ]

    mismatched = []
    for rel in modified:
        path = Path(rel)
        if not path.is_file():
            continue
        try:
            head = git_bytes("show", f"HEAD:{rel}")
        except subprocess.CalledProcessError:
            continue  # 新增文件，HEAD 中不存在
        work = path.read_bytes()
        head_eol, work_eol = eol_kind(head), eol_kind(work)
        if head_eol == work_eol or "NONE" in (head_eol, work_eol):
            continue
        mismatched.append((rel, head_eol, work_eol))
        if fix and head_eol == "LF":
            path.write_bytes(work.replace(b"\r\n", b"\n"))
        elif fix and head_eol == "CRLF":
            path.write_bytes(work.replace(b"\r\n", b"\n").replace(b"\n", b"\r\n"))

    if not mismatched:
        print("[OK] 所有改动文件的行尾符与 HEAD 一致，无虚假 diff。")
        return 0

    print(f"[WARN] 发现 {len(mismatched)} 个文件行尾符与 HEAD 不一致：")
    for rel, head_eol, work_eol in mismatched:
        action = "已修复" if fix else "需修复"
        print(f"  - {rel}: HEAD={head_eol} -> 工作区={work_eol}  [{action}]")
    return 0 if fix else 1


if __name__ == "__main__":
    raise SystemExit(main())
