#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""迁移 SQL 静态 lint：专门拦截“静态契约检查通过、真机 MySQL 拒绝执行”的写法。

背景（2026-09-24 真机证据）：
  V90/V91/V92 通过了 verify_upgrade_migrations.py 的静态契约检查，但在 MySQL 8.4.11
  上执行全部失败。根因两类，都不是业务逻辑问题，而是 SQL 客户端/服务器语义：
    1) `DELIMITER ;-- 注释`：mysql 客户端把 DELIMITER 之后的**整行剩余内容**当作
       新分隔符，于是分隔符变成 `;-- 注释`，后续语句永远不结束。
    2) `PREPARE stmt FROM v_sql`（局部变量）：MySQL 的 PREPARE ... FROM 只接受
       **字符串字面量或用户变量 @x**，不接受 DECLARE 出来的局部变量。

本 lint 不连数据库，纯文本解析，可进 CI。

规则：
  R1 DELIMITER_TRAILING_TEXT   DELIMITER 行在分隔符之后还有内容（含注释）
  R2 PREPARE_FROM_LOCAL_VAR    PREPARE ... FROM 使用了非 @ 变量 / 非字面量
  R3 DELIMITER_UNBALANCED      DELIMITER 指令数量不是偶数，或未成对收尾
  R4 DESTRUCTIVE_STATEMENT     出现 DROP TABLE / TRUNCATE / 无 WHERE 的 DELETE
  R5 MISSING_TRAILING_NEWLINE  文件末尾没有换行
"""
from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path

RE_DELIMITER = re.compile(r"^\s*DELIMITER\s+(\S+)(.*)$", re.IGNORECASE)
RE_PREPARE = re.compile(r"\bPREPARE\s+(\w+)\s+FROM\s+([^\s;]+)", re.IGNORECASE)
RE_TEMP_CREATE = re.compile(
    r"\bCREATE\s+TEMPORARY\s+TABLE\s+(?:IF\s+NOT\s+EXISTS\s+)?`?(\w+)`?", re.IGNORECASE)
RE_DROP_TABLE = re.compile(
    r"\bDROP\s+(TEMPORARY\s+)?TABLE\s+(?:IF\s+EXISTS\s+)?`?(\w+)`?", re.IGNORECASE)
RE_TRUNCATE = re.compile(r"\bTRUNCATE\s+(?:TABLE\s+)?`?(\w+)`?", re.IGNORECASE)
RE_DELETE = re.compile(r"\bDELETE\s+FROM\s+`?(\w+)`?(?![^;]*\bWHERE\b)",
                       re.IGNORECASE | re.DOTALL)

RULES = {
    "R1": "DELIMITER_TRAILING_TEXT",
    "R2": "PREPARE_FROM_LOCAL_VAR",
    "R3": "DELIMITER_UNBALANCED",
    "R4": "DESTRUCTIVE_STATEMENT",
    "R5": "MISSING_TRAILING_NEWLINE",
}


def strip_sql_comments(text: str) -> str:
    """把整行注释（-- ...）移除，保留行号，避免注释里的关键词误报。"""
    out = []
    for line in text.splitlines():
        idx = line.find("--")
        if idx != -1 and line[:idx].strip() == "":
            out.append(line[:idx])
        else:
            out.append(line)
    return "\n".join(out)


def lint_text(text: str, name: str = "<memory>") -> list[dict[str, object]]:
    findings: list[dict[str, object]] = []
    lines = text.splitlines()

    # R1 / R3 必须看原始行（注释在 DELIMITER 行尾正是缺陷本身）
    delimiter_lines: list[tuple[int, str, bool]] = []
    for no, line in enumerate(lines, start=1):
        m = RE_DELIMITER.match(line)
        if not m:
            continue
        delimiter, rest = m.group(1), m.group(2)
        trailing = rest.strip()
        delimiter_lines.append((no, delimiter, bool(trailing)))
        if trailing:
            findings.append({
                "rule": "R1", "code": RULES["R1"], "file": name, "line": no,
                "message": "DELIMITER 之后还有内容；mysql 客户端会把整行剩余内容当作分隔符",
                "evidence": line.strip()[:160],
            })

    if len(delimiter_lines) % 2 != 0:
        findings.append({
            "rule": "R3", "code": RULES["R3"], "file": name,
            "line": delimiter_lines[-1][0] if delimiter_lines else 0,
            "message": f"DELIMITER 指令数量为奇数（{len(delimiter_lines)}），自定义分隔符未成对收尾",
            "evidence": "",
        })
    else:
        for i in range(0, len(delimiter_lines), 2):
            open_no, open_delim, _ = delimiter_lines[i]
            close_no, close_delim, _ = delimiter_lines[i + 1]
            if close_delim != ";":
                findings.append({
                    "rule": "R3", "code": RULES["R3"], "file": name, "line": close_no,
                    "message": f"DELIMITER 未恢复为 ';'（当前为 '{close_delim}'）",
                    "evidence": f"open@{open_no}={open_delim}; close@{close_no}={close_delim}",
                })

    # 迁移自建的临时表允许 TRUNCATE / DROP / 全表 DELETE
    temp_tables = {m.group(1).lower() for m in RE_TEMP_CREATE.finditer(text)}

    code = strip_sql_comments(text)
    for no, line in enumerate(code.splitlines(), start=1):
        for m in RE_PREPARE.finditer(line):
            source = m.group(2)
            if source.startswith("@") or source.startswith("'") or source.startswith('"'):
                continue
            findings.append({
                "rule": "R2", "code": RULES["R2"], "file": name, "line": no,
                "message": f"PREPARE {m.group(1)} FROM {source}：MySQL 只接受字面量或 @ 用户变量",
                "evidence": line.strip()[:160],
            })
        for m in RE_DROP_TABLE.finditer(line):
            if m.group(1) or m.group(2).lower() in temp_tables:
                continue
            findings.append({
                "rule": "R4", "code": RULES["R4"], "file": name, "line": no,
                "message": f"迁移中 DROP 了业务表 {m.group(2)}", "evidence": line.strip()[:160],
            })
        for m in RE_TRUNCATE.finditer(line):
            if m.group(1).lower() in temp_tables:
                continue
            findings.append({
                "rule": "R4", "code": RULES["R4"], "file": name, "line": no,
                "message": f"迁移中 TRUNCATE 了业务表 {m.group(1)}", "evidence": line.strip()[:160],
            })
        for m in RE_DELETE.finditer(line):
            if m.group(1).lower() in temp_tables:
                continue
            findings.append({
                "rule": "R4", "code": RULES["R4"], "file": name, "line": no,
                "message": f"DELETE FROM {m.group(1)} 未带 WHERE", "evidence": line.strip()[:160],
            })

    if text and not text.endswith("\n"):
        findings.append({
            "rule": "R5", "code": RULES["R5"], "file": name, "line": len(lines),
            "message": "文件末尾缺少换行", "evidence": "",
        })
    return findings


def lint_paths(paths: list[Path]) -> dict[str, object]:
    files = []
    for path in paths:
        text = path.read_text(encoding="utf-8")
        findings = lint_text(text, path.name)
        files.append({"file": str(path), "name": path.name,
                      "violation_count": len(findings), "violations": findings})
    total = sum(f["violation_count"] for f in files)
    return {
        "tool": "sql_migration_lint.py",
        "rules": RULES,
        "scanned_files": len(files),
        "total_violations": total,
        "status": "passed" if total == 0 else "failed",
        "files": files,
    }


def main(argv: list[str] | None = None) -> int:
    ap = argparse.ArgumentParser(description="迁移 SQL 静态 lint（不连数据库）")
    ap.add_argument("paths", nargs="*", type=Path, help="SQL 文件或目录")
    ap.add_argument("--json", action="store_true", help="输出 JSON")
    args = ap.parse_args(argv)

    expanded: list[Path] = []
    for p in args.paths:
        if p.is_dir():
            expanded.extend(sorted(p.glob("*.sql")))
        elif p.is_file():
            expanded.append(p)
    if not expanded:
        print("[FAIL] 未指定 SQL 文件或目录", file=sys.stderr)
        return 2

    report = lint_paths(expanded)
    if args.json:
        print(json.dumps(report, ensure_ascii=False, indent=2, sort_keys=True))
    else:
        for f in report["files"]:
            for v in f["violations"]:
                print(f"[{v['rule']}] {f['name']}:{v['line']} {v['message']}")
                if v["evidence"]:
                    print(f"      {v['evidence']}")
        print(f"{'PASS' if report['status'] == 'passed' else 'FAIL'}: "
              f"{report['scanned_files']} 个文件，{report['total_violations']} 处违规")
    return 0 if report["status"] == "passed" else 1


if __name__ == "__main__":
    raise SystemExit(main())
