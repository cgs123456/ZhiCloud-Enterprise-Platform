#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""多租户隔离契约静态检查（Phase 1 / P0-3）。

先读源码确认拦截器的真实语义，再决定检查什么：

  TenantDatabaseInterceptor#computeIgnoreTable(tableName)
    * TableInfoHelper 找不到实体      -> 忽略租户（不加过滤）
    * 实体继承 TenantBaseDO           -> 不忽略（加过滤）
    * 其它情况                        -> 只有标了 @TenantIgnore 才忽略
  TenantProperties.ignoreTables 默认空集，注释原文：
    「即默认所有表都开启多租户的功能，所以记得添加对应的 tenant_id 字段哟」

因此本检查回答四个问题：
  T1 继承 TenantBaseDO 但表里没有 tenant_id 列   -> 过滤条件引用不存在的列，SQL 报错
  T2 有映射、没标 @TenantIgnore、表里没 tenant_id -> 拦截器照样拼 tenant_id，SQL 报错
  T3 标了 @TenantIgnore 但表里**有** tenant_id    -> 存了租户维度数据却不自动过滤：跨租户可见
  T4 表里有 tenant_id 但没有以它为首列的索引      -> 大表上退化成全表扫描

口径边界（必须一起读）：
  * 本工具读的是仓库 DDL 文本，不是生产 information_schema；
    DDL 里没有的表一律记为 T5「不可判定」，绝不猜测。
  * 不覆盖：@TenantIgnore 打在方法/Controller 上的运行时绕过、Redis Key 隔离、
    MQ 消费者链路、以及 mapper XML 里手写 SQL 的 JOIN 行为。
"""

from __future__ import annotations

import argparse
import json
import re
import subprocess
import sys
from datetime import datetime
from pathlib import Path
from typing import Any

SKIP_DIR_PARTS = {"target", ".git", "node_modules", "build", "dist", "__pycache__"}

TENANT_COLUMN = "tenant_id"

CLASS_RE = re.compile(r"(?:public\s+)?class\s+(\w+)\b[^{]*?extends\s+(\w+)", re.S)
TABLE_NAME_RE = re.compile(r'@TableName\s*\(\s*(?:value\s*=\s*)?"([^"]+)"')
TENANT_IGNORE_RE = re.compile(r"@TenantIgnore")
TENANT_FIELD_RE = re.compile(r"^\s*private\s+[\w<>\[\], .]+?\s+(tenantId|tenant_id)\s*;",
                             re.MULTILINE)
CREATE_TABLE_RE = re.compile(r"CREATE\s+TABLE\s+(?:IF\s+NOT\s+EXISTS\s+)?`?([\w$]+)`?\s*\(",
                             re.IGNORECASE)
INDEX_RE = re.compile(r"\b(?:INDEX|KEY)\s+`?([\w$]+)`?\s*\(([^)]*)\)", re.IGNORECASE)
ALTER_TABLE_RE = re.compile(r"ALTER\s+TABLE\s+`?([\w$]+)`?\s+(.*?);",
                            re.IGNORECASE | re.DOTALL)
ADD_COLUMN_RE = re.compile(
    r"\bADD\s+(?:COLUMN\s+)?(?:IF\s+NOT\s+EXISTS\s+)?`?([\w$]+)`?\s+",
    re.IGNORECASE)
ADD_INDEX_RE = re.compile(
    r"\bADD\s+(?:UNIQUE\s+)?(?:INDEX|KEY)\s+`?([\w$]+)`?\s*\(([^)]*)\)",
    re.IGNORECASE | re.DOTALL)
KEYWORD_PREFIXES = {"primary", "unique", "key", "index", "constraint",
                    "fulltext", "check", "foreign"}


def iter_java(repo: Path):
    for path in repo.rglob("*.java"):
        rel = path.relative_to(repo)
        if any(part in SKIP_DIR_PARTS for part in rel.parts):
            continue
        yield path, rel.as_posix()


def parse_ddl_files(paths: list[Path]) -> dict[str, dict[str, Any]]:
    """Parse all CREATE TABLE statements, then apply ALTER TABLE statements."""
    out: dict[str, dict[str, Any]] = {}
    documents: list[tuple[Path, str]] = []

    # Pass 1: collect the complete CREATE TABLE model. Migration filenames are
    # lexicographic (V108 sorts before V52), so ALTERs cannot be applied while
    # walking files one by one.
    for path in paths:
        try:
            text = path.read_text(encoding="utf-8", errors="replace")
        except OSError:
            continue
        documents.append((path, text))
        pos = 0
        while True:
            m = CREATE_TABLE_RE.search(text, pos)
            if not m:
                break
            start = m.end()
            depth = 1
            i = start
            while i < len(text) and depth:
                if text[i] == "(":
                    depth += 1
                elif text[i] == ")":
                    depth -= 1
                i += 1
            body = text[start:i - 1]
            pos = i
            parts: list[str] = []
            depth = 0
            cur = ""
            for ch in body:
                if ch == "(":
                    depth += 1
                elif ch == ")":
                    depth -= 1
                if ch == "," and depth == 0:
                    parts.append(cur)
                    cur = ""
                else:
                    cur += ch
            parts.append(cur)
            columns: set[str] = set()
            for part in parts:
                part = part.strip()
                if not part:
                    continue
                first = part.split()[0].strip("`").lower()
                if first in KEYWORD_PREFIXES:
                    continue
                columns.add(first)
            indexes: list[tuple[str, str]] = []
            for im in INDEX_RE.finditer(body):
                cols = re.findall(r"`?([\w$]+)`?", im.group(2))
                if cols:
                    indexes.append((im.group(1).lower(), cols[0].lower()))
            out[m.group(1).lower()] = {"columns": columns, "indexes": indexes,
                                       "source": path.name}

    # Pass 2: additive migrations must update the already-complete model.
    for _path, text in documents:
        for alter in ALTER_TABLE_RE.finditer(text):
            table = alter.group(1).lower()
            info = out.get(table)
            if info is None:
                continue
            alter_body = alter.group(2)
            for column in ADD_COLUMN_RE.finditer(alter_body):
                column_name = column.group(1).lower()
                if column_name in KEYWORD_PREFIXES:
                    continue
                info["columns"].add(column_name)
            for index in ADD_INDEX_RE.finditer(alter_body):
                cols = re.findall(r"`?([\w$]+)`?", index.group(2))
                if cols:
                    entry = (index.group(1).lower(), cols[0].lower())
                    if entry not in info["indexes"]:
                        info["indexes"].append(entry)
    return out


def collect_dos(repo: Path) -> list[dict[str, Any]]:
    dos: list[dict[str, Any]] = []
    for path, rel in iter_java(repo):
        try:
            text = path.read_text(encoding="utf-8", errors="replace")
        except OSError:
            continue
        m = CLASS_RE.search(text)
        if not m or m.group(2) not in {"TenantBaseDO", "BaseDO"}:
            continue
        tm = TABLE_NAME_RE.search(text)
        dos.append({
            "class": m.group(1),
            "base": m.group(2),
            "table": tm.group(1).lower() if tm else None,
            "tenant_ignore": bool(TENANT_IGNORE_RE.search(text)),
            "declares_tenant_field": bool(TENANT_FIELD_RE.search(text)),
            "file": rel,
        })
    return dos


def collect_ignore_bypass(repo: Path) -> list[dict[str, str]]:
    """@TenantIgnore 打在方法/类（非 DO）上的绕过点清单。"""
    out: list[dict[str, str]] = []
    for path, rel in iter_java(repo):
        try:
            text = path.read_text(encoding="utf-8", errors="replace")
        except OSError:
            continue
        if "/dal/dataobject/" in rel or "/dataobject/" in rel:
            continue
        for line_no, line in enumerate(text.splitlines(), start=1):
            if TENANT_IGNORE_RE.search(line):
                out.append({"file": rel, "line": line_no, "evidence": line.strip()[:160]})
    return out


def build_report(repo: Path, ddl_paths: list[Path]) -> dict[str, Any]:
    ddl = parse_ddl_files(ddl_paths)
    dos = [d for d in collect_dos(repo) if d["table"]]
    findings: list[dict[str, Any]] = []

    def add(rule: str, code: str, severity: str, item: dict[str, Any], detail: str):
        findings.append({
            "rule": rule, "code": code, "severity": severity,
            "class": item["class"], "table": item["table"], "file": item["file"],
            "detail": detail,
        })

    for d in dos:
        info = ddl.get(d["table"])
        if info is None:
            add("T5", "DO_TABLE_UNVERIFIED", "P1", d,
                "DO 映射的表在仓库 DDL 里不存在，隔离契约无法判定（不猜测）")
            continue
        has_col = TENANT_COLUMN in info["columns"]
        if d["base"] == "TenantBaseDO" and not d["tenant_ignore"] and not has_col:
            add("T1", "TENANT_DO_COLUMN_MISSING", "P0", d,
                "继承 TenantBaseDO 会强制拼 tenant_id 条件，但表里没有该列：查询直接报错")
        elif not d["tenant_ignore"] and not has_col:
            add("T2", "FILTER_APPLIED_WITHOUT_COLUMN", "P0", d,
                "默认所有表开启多租户，拦截器会拼 tenant_id，但表里没有该列：查询直接报错")
        elif d["tenant_ignore"] and has_col:
            add("T3", "IGNORED_BUT_HAS_TENANT_COLUMN", "P0", d,
                "标了 @TenantIgnore 不加租户过滤，但表里存在 tenant_id："
                "跨租户可见，且 DO 不写该字段，写入会落到列默认值")
        if has_col and not any(col == TENANT_COLUMN for _, col in info["indexes"]):
            add("T4", "TENANT_COLUMN_NOT_INDEXED", "P1", d,
                "tenant_id 上没有以它为首列的索引：多租户过滤在大表上退化为全表扫描")

    bypass = collect_ignore_bypass(repo)
    by_rule: dict[str, int] = {}
    by_severity: dict[str, int] = {}
    for f in findings:
        by_rule[f["rule"]] = by_rule.get(f["rule"], 0) + 1
        by_severity[f["severity"]] = by_severity.get(f["severity"], 0) + 1

    verified = sum(1 for d in dos if d["table"] in ddl)
    return {
        "generated_at": datetime.now().astimezone().isoformat(timespec="seconds"),
        "tool": "check_tenant_isolation.py",
        "evidence_class": "static-text (Java DO + repository DDL); not production information_schema",
        "repo_root": str(repo),
        "commit": git_head(repo),
        "ddl_files": [p.name for p in ddl_paths],
        "ddl_table_count": len(ddl),
        "ddl_tables_with_tenant_column": sum(1 for v in ddl.values()
                                             if TENANT_COLUMN in v["columns"]),
        "do_count": len(dos),
        "do_verified_count": verified,
        "do_unverified_count": len(dos) - verified,
        "do_by_base": {b: sum(1 for d in dos if d["base"] == b)
                       for b in ("TenantBaseDO", "BaseDO")},
        "do_tenant_ignore_count": sum(1 for d in dos if d["tenant_ignore"]),
        "finding_count": len(findings),
        "findings_by_rule": by_rule,
        "findings_by_severity": by_severity,
        "tenant_ignore_bypass_points": bypass,
        "tenant_ignore_bypass_count": len(bypass),
        "findings": findings,
        "status": "failed" if by_severity.get("P0") else "passed",
        "disclaimers": [
            "读的是仓库 DDL 文本，不是生产 information_schema；DDL 缺失的表记为不可判定。",
            "不覆盖方法级/Controller 级 @TenantIgnore 的运行时行为、Redis Key 隔离、"
            "MQ 消费链路与 mapper XML 手写 SQL 的 JOIN。",
            "规则语义来自 TenantDatabaseInterceptor 与 TenantProperties 源码，"
            "若拦截器被二改需重新核对。",
        ],
    }


def git_head(repo: Path) -> str:
    try:
        res = subprocess.run(["git", "-C", str(repo), "rev-parse", "HEAD"],
                             capture_output=True, text=True, timeout=30)
        return res.stdout.strip()
    except Exception:
        return ""


def default_ddl_paths(repo: Path) -> list[Path]:
    d = repo / "sql" / "mysql"
    if not d.is_dir():
        return []
    skip = {"aimultiagent_rollback.sql", "qms_rollback.sql"}
    return sorted(p for p in d.glob("*.sql") if p.name not in skip)


def main(argv: list[str] | None = None) -> int:
    ap = argparse.ArgumentParser(description="多租户隔离契约静态检查")
    ap.add_argument("--repo", type=Path, default=Path(__file__).resolve().parents[2])
    ap.add_argument("--ddl-dir", type=Path, help="DDL 目录，默认 <repo>/sql/mysql")
    ap.add_argument("--output", type=Path)
    ap.add_argument("--summary", action="store_true")
    args = ap.parse_args(argv)

    repo = args.repo.resolve()
    ddl_dir = (args.ddl_dir or (repo / "sql" / "mysql")).resolve()
    ddl_paths = sorted(p for p in ddl_dir.glob("*.sql")
                       if p.name not in {"aimultiagent_rollback.sql", "qms_rollback.sql"})
    report = build_report(repo, ddl_paths)

    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(json.dumps(report, ensure_ascii=False, indent=2,
                                          sort_keys=True) + "\n",
                               encoding="utf-8", newline="\n")
    if not args.summary:
        for f in report["findings"][:80]:
            print(f"[{f['severity']}][{f['rule']}] {f['table']} <- {f['class']} :: {f['detail']}")
    print(f"DDL 表 {report['ddl_table_count']}（含 tenant_id "
          f"{report['ddl_tables_with_tenant_column']}）；DO {report['do_count']}"
          f"（可判定 {report['do_verified_count']} / 不可判定 {report['do_unverified_count']}）")
    print(f"命中 {report['finding_count']} {report['findings_by_rule']} "
          f"{report['findings_by_severity']}；@TenantIgnore 非 DO 绕过点 "
          f"{report['tenant_ignore_bypass_count']}")
    print(f"status={report['status']}")
    return 1 if report["status"] == "failed" else 0


if __name__ == "__main__":
    raise SystemExit(main())