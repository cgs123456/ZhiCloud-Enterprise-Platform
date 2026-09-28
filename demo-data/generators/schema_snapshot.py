# -*- coding: utf-8 -*-
"""静态 DDL 快照解析器。

目的：为模拟数据生成器提供**唯一的列真值来源**，禁止生成器凭空编造列名。

边界：
  * 只做静态解析，不连接任何数据库，不执行任何 DDL/DML。
  * 按 Flyway 版本号顺序回放 CREATE TABLE / ALTER TABLE，得到"迁移链末端"的列集合。
  * 不解析触发器、存储过程、视图；不保证表达式默认值、字符集、分区等语义完整。
  * 未覆盖的 DDL 语法会被记录在 warnings 里，不静默吞掉。
"""

from __future__ import annotations

import argparse
import hashlib
import json
import re
from pathlib import Path
from typing import Any

REPO = Path(__file__).resolve().parents[2]
DEFAULT_MIGRATIONS = REPO / "zhicloud-server" / "src" / "main" / "resources" / "db" / "migration"
DEFAULT_EXTRA = REPO / "sql" / "mysql"

_VERSION_RE = re.compile(r"^V(\d+(?:\.\d+)*)__", re.I)
_CREATE_RE = re.compile(
    r"CREATE\s+TABLE\s+(?:IF\s+NOT\s+EXISTS\s+)?([`\"\[]?)([A-Za-z0-9_]+)\1?\s*\(", re.I
)
_ALTER_RE = re.compile(r"ALTER\s+TABLE\s+([`\"\[]?)([A-Za-z0-9_]+)\1?\s+", re.I)
_CONSTRAINT_PREFIX = re.compile(
    r"^(PRIMARY\s+KEY|UNIQUE|KEY|INDEX|FULLTEXT|SPATIAL|CONSTRAINT|CHECK|FOREIGN\s+KEY)\b", re.I
)


def _version_key(version: str) -> tuple[int, ...]:
    return tuple(int(p) for p in version.split("."))


def strip_comments(text: str) -> str:
    """去掉行注释与块注释，保留字符串字面量内的内容（尽力而为）。"""
    out: list[str] = []
    i, n = 0, len(text)
    quote: str | None = None
    while i < n:
        ch = text[i]
        nxt = text[i + 1] if i + 1 < n else ""
        if quote:
            out.append(ch)
            if ch == "\\" and quote in ("'", '"'):
                if i + 1 < n:
                    out.append(text[i + 1])
                    i += 2
                    continue
            elif ch == quote:
                quote = None
            i += 1
            continue
        if ch in ("'", '"', "`"):
            quote = ch
            out.append(ch)
            i += 1
            continue
        if ch == "#" or (ch == "-" and nxt == "-" and (i + 2 >= n or text[i + 2] in " \t\r\n")):
            while i < n and text[i] != "\n":
                i += 1
            continue
        if ch == "/" and nxt == "*":
            depth = 1
            i += 2
            while i < n and depth:
                if text[i] == "/" and i + 1 < n and text[i + 1] == "*":
                    depth += 1
                    i += 2
                elif text[i] == "*" and i + 1 < n and text[i + 1] == "/":
                    depth -= 1
                    i += 2
                else:
                    i += 1
            continue
        out.append(ch)
        i += 1
    return "".join(out)


def split_top_level(body: str) -> list[str]:
    """按顶层逗号切分 DDL 片段，忽略括号内的逗号。"""
    parts: list[str] = []
    depth = 0
    quote: str | None = None
    cur: list[str] = []
    for ch in body:
        if quote:
            cur.append(ch)
            if ch == quote:
                quote = None
            continue
        if ch in ("'", '"', "`"):
            quote = ch
            cur.append(ch)
            continue
        if ch == "(":
            depth += 1
        elif ch == ")":
            depth -= 1
        if ch == "," and depth == 0:
            parts.append("".join(cur).strip())
            cur = []
            continue
        cur.append(ch)
    if "".join(cur).strip():
        parts.append("".join(cur).strip())
    return [p for p in parts if p]


def unquote(name: str) -> str:
    return name.strip().strip('`"[]')


_ATTR_RE = re.compile(
    r"\s(?=(?:NOT\s+NULL|NULL|DEFAULT|COMMENT|PRIMARY\s+KEY|AUTO_INCREMENT|UNIQUE|"
    r"CHARACTER\s+SET|COLLATE|ON\s+UPDATE|ON\s+DELETE|GENERATED|STORED|VIRTUAL|REFERENCES|"
    r"CHECK|SRID|INVISIBLE|ENFORCED)\b)",
    re.I,
)


def parse_column(fragment: str) -> dict[str, Any] | None:
    m = re.match(r"([`\"\[]?)([A-Za-z0-9_]+)\1?\s+(.+)$", fragment.strip(), re.S)
    if not m:
        return None
    name = m.group(2)
    remainder = m.group(3).strip()
    cut = _ATTR_RE.search(" " + remainder)
    if cut:
        idx = max(cut.start() - 1, 0)
        ctype, rest = remainder[:idx].strip(), remainder[idx:].strip()
    else:
        ctype, rest = remainder, ""
    ctype = re.sub(r"\s+", " ", ctype).upper()
    if not re.match(r"^[A-Za-z]+", ctype):
        return None
    whole = fragment.strip()
    return {
        "name": name,
        "type": ctype,
        "nullable": not re.search(r"\bNOT\s+NULL\b", rest, re.I)
        and not re.search(r"\bPRIMARY\s+KEY\b", whole, re.I),
        "auto_increment": bool(re.search(r"\bAUTO_INCREMENT\b", rest, re.I)),
        "has_default": bool(re.search(r"\bDEFAULT\b", rest, re.I)),
        "inline_primary_key": bool(re.search(r"\bPRIMARY\s+KEY\b", whole, re.I)),
        "raw": re.sub(r"\s+", " ", whole),
    }


def _file_sort_key(item: tuple[str, Path]) -> tuple:
    version, path = item
    # 版本号文件按版本升序；无版本号文件排在其后并按文件名升序（避免 hash 随机化）
    if re.match(r"^\d+(\.\d+)*$", version):
        parts = _version_key(version)
        parts = parts + (0,) * (6 - len(parts))  # 定长补齐，避免不同段数之间类型错位
        return (0,) + parts + (path.name,)
    return (1, 0, path.name)


def ordered_files(directories: list[Path], warnings: list[str]) -> list[tuple[str, Path]]:
    """返回 (version, path)，迁移按版本号升序；附加 DDL 排在迁移之后。"""
    items: list[tuple[str, Path]] = []
    for d in directories:
        if not d.exists():
            warnings.append(f"目录不存在，已跳过: {d}")
            continue
        for p in sorted(d.glob("*.sql")):
            m = _VERSION_RE.match(p.name)
            items.append((m.group(1) if m else p.stem, p))
    items.sort(key=_file_sort_key)
    return items


def build_snapshot(directories: list[Path]) -> dict[str, Any]:
    warnings: list[str] = []
    tables: dict[str, dict[str, Any]] = {}
    for version, path in ordered_files(directories, warnings):
        raw = path.read_text(encoding="utf-8", errors="replace")
        text = strip_comments(raw)
        rel = str(path.relative_to(REPO)).replace("\\", "/")

        for m in _CREATE_RE.finditer(text):
            table = unquote(m.group(2)).lower()
            start = m.end() - 1
            depth = 0
            quote: str | None = None
            end = None
            for i in range(start, len(text)):
                ch = text[i]
                if quote:
                    if ch == quote:
                        quote = None
                    continue
                if ch in ("'", '"', "`"):
                    quote = ch
                    continue
                if ch == "(":
                    depth += 1
                elif ch == ")":
                    depth -= 1
                    if depth == 0:
                        end = i
                        break
            if end is None:
                warnings.append(f"{rel}: CREATE TABLE {table} 括号未闭合，已跳过")
                continue
            body = text[start + 1:end]
            cols: list[dict[str, Any]] = []
            pk: list[str] = []
            for frag in split_top_level(body):
                if _CONSTRAINT_PREFIX.match(frag):
                    pk_m = re.search(r"PRIMARY\s+KEY\s*\(([^)]*)\)", frag, re.I)
                    if pk_m:
                        pk = [unquote(c) for c in pk_m.group(1).split(",")]
                    continue
                col = parse_column(frag)
                if col is None:
                    warnings.append(f"{rel}: {table}: 无法解析片段 -> {frag[:80]}")
                    continue
                cols.append(col)
            if not pk:
                pk = [c["name"] for c in cols if c.get("inline_primary_key")]
            if table in tables:
                warnings.append(f"{rel}: {table} 重复定义 CREATE TABLE，按列并集合并")
            if table in tables:
                known = {c["name"].lower() for c in tables[table]["columns"]}
                for c in cols:
                    if c["name"].lower() not in known:
                        tables[table]["columns"].append(c)
                if pk and not tables[table]["primary_key"]:
                    tables[table]["primary_key"] = pk
                tables[table]["version"] = version
            else:
                tables[table] = {
                    "columns": cols,
                    "primary_key": pk,
                    "source": rel,
                    "version": version,
                }

        for m in _ALTER_RE.finditer(text):
            table = unquote(m.group(2)).lower()
            tail = text[m.end():]
            tail = tail[: re.search(r";", tail).start()] if re.search(r";", tail) else tail[:2000]
            if table not in tables and not re.search(r"\bADD\b", tail, re.I):
                warnings.append(f"{rel}: ALTER TABLE {table} 目标表未解析到，已跳过")
                continue
            rec = tables.setdefault(
                table, {"columns": [], "primary_key": [], "source": rel, "version": version}
            )
            # ALTER TABLE 片段按顶层逗号切分，逐片段判定（避免嵌套括号正则）
            for frag in split_top_level(tail):
                f = frag.strip()
                if re.match(r"^ADD\s+PRIMARY\s+KEY\s*\(", f, re.I):
                    pk2 = re.search(r"PRIMARY\s+KEY\s*\(([^)]*)\)", f, re.I)
                    if pk2:
                        rec["primary_key"] = [unquote(c) for c in pk2.group(1).split(",")]
                    continue
                if re.match(r"^ADD\s+(?:UNIQUE|KEY|INDEX|FULLTEXT|SPATIAL|CONSTRAINT|FOREIGN\s+KEY)\b", f, re.I):
                    continue
                m_add = re.match(r"^ADD\s+(?:COLUMN\s+)?(.+)$", f, re.I | re.S)
                if m_add:
                    col = parse_column(m_add.group(1).strip())
                    if col is None:
                        warnings.append(f"{rel}: {table}: ALTER ADD 无法解析 -> {f[:80]}")
                        continue
                    if any(c["name"].lower() == col["name"].lower() for c in rec["columns"]):
                        continue
                    rec["columns"].append(col)
                    continue
                m_drop = re.match(r"^DROP\s+(?:COLUMN\s+)?([A-Za-z0-9_]+)", f, re.I)
                if m_drop:
                    name = m_drop.group(1).lower()
                    rec["columns"] = [c for c in rec["columns"] if c["name"].lower() != name]
                    continue
                if re.match(r"^DROP\s+(?:PRIMARY\s+KEY|KEY|INDEX|CONSTRAINT|FOREIGN\s+KEY)\b", f, re.I):
                    if re.match(r"^DROP\s+PRIMARY\s+KEY\b", f, re.I):
                        rec["primary_key"] = []
                    continue
                m_chg = re.match(
                    r"^CHANGE\s+(?:COLUMN\s+)?([`\"\[]?)([A-Za-z0-9_]+)\1?\s+([`\"\[]?)([A-Za-z0-9_]+)\3?\s+(.+)$",
                    f, re.I | re.S)
                if m_chg:
                    old, new, typedef = m_chg.group(2), m_chg.group(4), m_chg.group(5)
                    rec["columns"] = [c for c in rec["columns"] if c["name"].lower() != old.lower()]
                    col = parse_column(f"{new} {typedef}")
                    if col:
                        rec["columns"].append(col)
                    continue
                m_mod = re.match(r"^MODIFY\s+(?:COLUMN\s+)?([`\"\[]?)([A-Za-z0-9_]+)\1?\s+(.+)$", f, re.I | re.S)
                if m_mod:
                    name, typedef = m_mod.group(2), m_mod.group(3)
                    col = parse_column(f"{name} {typedef}")
                    if not col:
                        continue
                    replaced = False
                    for i, c in enumerate(rec["columns"]):
                        if c["name"].lower() == name.lower():
                            rec["columns"][i] = col
                            replaced = True
                            break
                    if not replaced:
                        rec["columns"].append(col)
                    continue

    for rec in tables.values():
        rec["column_names"] = [c["name"] for c in rec["columns"]]

    return {
        "tables": {k: tables[k] for k in sorted(tables)},
        "table_count": len(tables),
        "warnings": warnings,
        "sources": [str(d) for d in directories],
    }


def merge_snapshots(primary: dict[str, Any], secondary: dict[str, Any]) -> dict[str, Any]:
    """以迁移链为主、全新安装脚本为补充；差异记为漂移，不静默取舍。"""
    tables = {k: dict(v) for k, v in primary["tables"].items()}
    for t in tables.values():
        t["source_kind"] = "migration_chain"
    drift: list[dict[str, Any]] = []
    for name, rec in secondary["tables"].items():
        if name not in tables:
            merged = dict(rec)
            merged["source_kind"] = "install_script_only"
            tables[name] = merged
            continue
        cur = tables[name]
        pcols = {c["name"].lower() for c in cur["columns"]}
        scols = {c["name"].lower() for c in rec["columns"]}
        only_s = sorted(scols - pcols)
        only_p = sorted(pcols - scols)
        if only_s or only_p:
            drift.append(
                {"table": name, "only_in_install_script": only_s, "only_in_migration_chain": only_p}
            )
        if not cur["primary_key"] and rec["primary_key"]:
            cur["primary_key"] = list(rec["primary_key"])
    for t in tables.values():
        t["column_names"] = [c["name"] for c in t["columns"]]
    return {
        "tables": {k: tables[k] for k in sorted(tables)},
        "table_count": len(tables),
        "warnings": list(primary["warnings"]) + list(secondary["warnings"]),
        "drift": drift,
        "sources": {
            "migration_chain": primary["sources"],
            "install_script": secondary["sources"],
        },
    }


def main(argv: list[str] | None = None) -> int:
    ap = argparse.ArgumentParser(description="从 DDL 生成静态 schema 快照（不连数据库）")
    ap.add_argument("--migrations-dir", default=str(DEFAULT_MIGRATIONS))
    ap.add_argument("--extra-ddl-dir", default=str(DEFAULT_EXTRA), help="附加 DDL 目录（全新安装脚本）")
    ap.add_argument("--output", help="输出 JSON 路径")
    ap.add_argument("--summary", action="store_true", help="只打印汇总")
    args = ap.parse_args(argv)

    primary = build_snapshot([Path(args.migrations_dir)])
    secondary = build_snapshot([Path(args.extra_ddl_dir)]) if args.extra_ddl_dir else {"tables": {}, "warnings": [], "sources": []}
    snap = merge_snapshots(primary, secondary)
    payload = json.dumps(snap, ensure_ascii=False, indent=2, sort_keys=False)
    # payload 摘要：不含自身字段、可复现；file_sha256 为落盘文件的实际字节摘要
    digest = hashlib.sha256(payload.encode("utf-8")).hexdigest()
    snap["payload_sha256"] = digest

    if args.output:
        out = Path(args.output)
        out.parent.mkdir(parents=True, exist_ok=True)
        text = json.dumps(snap, ensure_ascii=False, indent=2) + "\n"
        out.write_bytes(text.encode("utf-8"))  # 按字节写入，避免 Windows 换行翻译影响摘要
        file_digest = hashlib.sha256(text.encode("utf-8")).hexdigest()
        print(f"已写入 {out}；tables={snap['table_count']}；warnings={len(snap['warnings'])}")
        print(f"payload_sha256={digest}")
        print(f"file_sha256={file_digest}")

    if args.summary or not args.output:
        print(f"tables={snap['table_count']} warnings={len(snap['warnings'])}")
        for w in snap["warnings"][:20]:
            print("  WARN", w)
        if not args.output:
            print(f"payload_sha256={digest}")
        for t in ("erp_sale_order", "mes_pro_work_order", "wms_inbound_order", "crm_contract"):
            rec = snap["tables"].get(t)
            if rec:
                print(f"  {t}: cols={len(rec['column_names'])} pk={rec['primary_key']} src={rec['source']}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
