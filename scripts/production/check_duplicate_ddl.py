#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""重复建表门禁：同一张表被多个迁移 CREATE TABLE 时，必须登记并解释。

为什么需要这道门禁：
  Flyway 按版本号升序执行，版本小的迁移先建表，版本大的迁移因
  `CREATE TABLE IF NOT EXISTS` 直接空转。但“先上线过大版本迁移、后补入小版本
  迁移”的历史库里，生效的可能是后上线的大版本结构。当两份定义不一致时，
  Java DO 可能读到完全不同的列集合（erp_period 的 V6/V10 就是实例）。

本门禁不连数据库，只做文本解析。它保证：
  1. 任何被多个迁移创建的表都必须在 allowlist 中登记；
  2. 登记项必须声明“权威定义迁移”和“收敛迁移”；
  3. 登记的期望列必须能从现有定义推导出来，且被收敛迁移或权威定义覆盖；
  4. allowlist 与实际扫描结果不一致时失败（防止清单腐化）。

它不证明迁移能在 MySQL 上执行；执行证据仍需要影子库/预生产真机运行。
"""
from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
DEFAULT_MIGRATIONS = (
    REPO_ROOT / "zhicloud-server" / "src" / "main" / "resources" / "db" / "migration"
)
DEFAULT_ALLOWLIST = Path(__file__).resolve().parent / "duplicate_ddl_allowlist.json"

CREATE_RE = re.compile(
    r"CREATE\s+TABLE\s+(?:IF\s+NOT\s+EXISTS\s+)?`?(?P<table>[A-Za-z0-9_]+)`?\s*\(",
    re.IGNORECASE,
)
VERSION_RE = re.compile(r"^V(?P<version>\d+(?:\.\d+)*)__")
NON_COLUMN_PREFIXES = (
    "PRIMARY KEY", "UNIQUE KEY", "UNIQUE INDEX", "UNIQUE", "KEY", "INDEX",
    "CONSTRAINT", "FOREIGN KEY", "CHECK", "FULLTEXT", "SPATIAL", "PERIOD FOR",
)

RULES = {
    "D1": "DUPLICATE_CREATE_UNREGISTERED",
    "D2": "ALLOWLIST_STALE",
    "D3": "RECONCILIATION_MISSING",
    "D4": "CANONICAL_NOT_A_CREATOR",
    "D5": "EXPECTED_COLUMN_NOT_DERIVABLE",
    "D6": "EXPECTED_COLUMN_NOT_COVERED",
    "D7": "CREATING_SET_MISMATCH",
    "D8": "ALLOWLIST_SCHEMA_INVALID",
}


def _version_key(filename: str) -> tuple:
    match = VERSION_RE.match(filename)
    if not match:
        return (10 ** 9,)
    return tuple(int(part) for part in match.group("version").split("."))


def _finding(rule: str, target: str, message: str) -> dict:
    return {"rule": rule, "code": RULES[rule], "file": target, "message": message}


def _strip_comment(line: str) -> str:
    """去掉尾部 COMMENT '...'，保留列定义主体用于比对。"""
    idx = line.upper().find(" COMMENT ")
    if idx != -1:
        return line[:idx].strip()
    return line.strip()


def parse_create_tables(text: str, filename: str) -> list[dict]:
    """解析单个迁移文件里的 CREATE TABLE 语句（含列定义与位置）。"""
    results: list[dict] = []
    for match in CREATE_RE.finditer(text):
        open_idx = text.index("(", match.end() - 1)
        depth = 0
        end_idx = open_idx
        for pos in range(open_idx, len(text)):
            if text[pos] == "(":
                depth += 1
            elif text[pos] == ")":
                depth -= 1
                if depth == 0:
                    end_idx = pos
                    break
        body = text[open_idx + 1:end_idx]
        columns: dict[str, str] = {}
        for raw_line in body.splitlines():
            line = raw_line.strip().rstrip(",")
            if not line or line.startswith("--"):
                continue
            upper = line.upper()
            if upper.startswith(NON_COLUMN_PREFIXES):
                continue
            name = line.split()[0].strip("`").lower()
            if not re.fullmatch(r"[a-z_][a-z0-9_]*", name):
                continue
            columns[name] = _strip_comment(line)
        results.append({
            "table": match.group("table").lower(),
            "file": filename,
            "columns": columns,
            "raw": " ".join(body.split()),
        })
    return results


def scan_migrations(migrations_dir: Path) -> dict[str, list[dict]]:
    created: dict[str, list[dict]] = {}
    if not migrations_dir.is_dir():
        return created
    for path in sorted(migrations_dir.glob("V*.sql"), key=lambda p: _version_key(p.name)):
        text = path.read_text(encoding="utf-8", errors="replace")
        for item in parse_create_tables(text, path.name):
            created.setdefault(item["table"], []).append(item)
    return created


def _load_allowlist(path: Path) -> tuple[dict, list[dict]]:
    findings: list[dict] = []
    try:
        data = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exc:
        return {}, [_finding("D8", str(path), f"invalid allowlist json: {exc}")]
    if not isinstance(data, dict) or data.get("schema_version") != 1:
        return {}, [_finding("D8", str(path), "allowlist schema_version must be 1")]
    if not isinstance(data.get("tables"), list):
        return {}, [_finding("D8", str(path), "allowlist tables must be a list")]
    for item in data["tables"]:
        if not isinstance(item, dict):
            findings.append(_finding("D8", str(path), "allowlist entry must be an object"))
            continue
        for key in ("table", "creating_migrations", "canonical_migration",
                    "reconciled_by", "expected_columns"):
            if key not in item:
                findings.append(_finding("D8", str(path), f"entry missing key: {key}"))
        if "table" in item and not isinstance(item["table"], str):
            findings.append(_finding("D8", str(path), "table must be a string"))
    return data, findings


def build_report(migrations_dir: Path, allowlist_path: Path) -> dict:
    created = scan_migrations(migrations_dir)
    duplicates = {name: items for name, items in created.items() if len(items) > 1}
    allowlist, findings = _load_allowlist(allowlist_path)

    entries = {}
    for item in allowlist.get("tables", []):
        if not isinstance(item, dict) or not isinstance(item.get("table"), str):
            continue
        entries[item["table"].lower()] = item

    tables_report = []
    for name in sorted(duplicates):
        items = duplicates[name]
        files = [i["file"] for i in items]
        definitions_identical = len({i["raw"] for i in items}) == 1
        union_columns = sorted({c for i in items for c in i["columns"]})
        entry = entries.get(name)

        row = {
            "table": name,
            "creating_migrations": files,
            "definitions_identical": definitions_identical,
            "union_columns": union_columns,
            "registered": entry is not None,
            "canonical_migration": (entry or {}).get("canonical_migration"),
            "reconciled_by": (entry or {}).get("reconciled_by"),
        }
        tables_report.append(row)

        if entry is None:
            findings.append(_finding(
                "D1", name,
                f"table is created by {len(files)} migrations ({', '.join(files)}) "
                f"but is not registered in the allowlist",
            ))
            continue

        if sorted(str(m) for m in entry.get("creating_migrations", [])) != sorted(files):
            findings.append(_finding(
                "D7", name,
                f"allowlist creating_migrations {entry.get('creating_migrations')} "
                f"does not match scan {files}",
            ))
        canonical = str(entry.get("canonical_migration") or "")
        if canonical not in files:
            findings.append(_finding(
                "D4", name, f"canonical_migration {canonical!r} is not one of {files}",
            ))
        reconciled = str(entry.get("reconciled_by") or "")
        reconciled_path = migrations_dir / reconciled
        reconciled_text = ""
        if not reconciled or not reconciled_path.is_file():
            findings.append(_finding(
                "D3", name, f"reconciliation migration is missing: {reconciled}",
            ))
        else:
            reconciled_text = reconciled_path.read_text(encoding="utf-8", errors="replace")
            if name not in reconciled_text and not definitions_identical:
                findings.append(_finding(
                    "D3", name,
                    f"reconciliation migration {reconciled} never mentions the table",
                ))
        canonical_columns = set()
        for item in items:
            if item["file"] == canonical:
                canonical_columns = set(item["columns"])
        expected = [str(c).lower() for c in entry.get("expected_columns", [])]
        for column in expected:
            if column not in union_columns:
                findings.append(_finding(
                    "D5", name,
                    f"expected column {column!r} is not defined by any creating migration",
                ))
                continue
            if column in canonical_columns:
                continue
            if column not in reconciled_text:
                findings.append(_finding(
                    "D6", name,
                    f"expected column {column!r} is neither in the canonical definition "
                    f"nor referenced by {reconciled}",
                ))

    for name in sorted(entries):
        if name not in duplicates:
            findings.append(_finding(
                "D2", name,
                "allowlist entry exists but the table is no longer created more than once",
            ))

    return {
        "tool": "check_duplicate_ddl.py",
        "status": "passed" if not findings else "failed",
        "migrations_dir": str(migrations_dir),
        "allowlist": str(allowlist_path),
        "scanned_tables": len(created),
        "duplicate_groups": len(duplicates),
        "conflicting_groups": sum(1 for r in tables_report if not r["definitions_identical"]),
        "registered_groups": len(entries),
        "findings": findings,
        "tables": tables_report,
        "limits": [
            "Text-only parsing; it does not connect to MySQL.",
            "A registered duplicate is not proven safe until the reconciliation "
            "migration is executed on a real database.",
        ],
    }


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="重复建表门禁（不连数据库）")
    parser.add_argument("--migrations-dir", type=Path, default=DEFAULT_MIGRATIONS)
    parser.add_argument("--allowlist", type=Path, default=DEFAULT_ALLOWLIST)
    parser.add_argument("--json", action="store_true", help="输出 JSON")
    args = parser.parse_args(argv)

    report = build_report(args.migrations_dir, args.allowlist)
    if args.json:
        print(json.dumps(report, ensure_ascii=False, indent=2, sort_keys=True))
    else:
        for finding in report["findings"]:
            print(f"[{finding['rule']}] {finding['file']}: {finding['message']}")
        print(f"{report['status'].upper()}: tables={report['scanned_tables']} "
              f"duplicate_groups={report['duplicate_groups']} "
              f"conflicting={report['conflicting_groups']} "
              f"findings={len(report['findings'])}")
    return 0 if report["status"] == "passed" else 1


if __name__ == "__main__":
    raise SystemExit(main())
