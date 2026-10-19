# -*- coding: utf-8 -*-
"""静态 DDL 快照解析器。

目的：为模拟数据生成器提供**唯一的列真值来源**，禁止生成器凭空编造列名。

回放契约（§12.280 对齐定稿；守卫见 scripts/production/tests/test_schema_snapshot_replay.py）：
  * 语句级顺序回放：按 Flyway 版本号逐文件、逐语句重放 CREATE/ALTER/DROP TABLE，
    DROP 按语句顺序生效（V208 清退的旧 /hr 表据此正确消失；快照 587 表=列级与
    沙箱 DB 全量 diff 清零的真值锚）。
  * 文件一律 utf-8-sig 读取：V44 形态的 BOM 头历史上曾把首条 SET 内嵌 DDL 压头吞掉。
  * DELIMITER 块内的 CREATE PROCEDURE 逐体捕获；**过程作用域=文件**（真实链
    V22/V98/V205 的过程与 CALL 全部同文件，跨文件不可见即契约）。
  * CALL 过程按三种方式落地：①p_*_add_column/make_nullable/widen_column 动作族
    名字+实参直接生效；②过程体符号执行（全新安装假设=护栏真分支，SET @x:=CONCAT
    →PREPARE/EXECUTE→同一派发，IN 实参参数绑定）+ 融合 IF 体内顶层 DDL carve；
    ③未定义且名字疑似列动作且实参不足的 CALL 记 warning，数据型 helper 静默。
  * SET @x := IF(护栏cond,'ALTER…','DO 0') 的 _embedded_ddl：字面量内 `''` 成对转义
    解析（V44/V205 注释），相邻字符串字面量按 MySQL 语义拼接（仅空白相连；逗号/
    括号即断，护栏假分支的 'SELECT skip' 等不粘连）。
  * 未知/不可解析 DDL 记 warning 不吞；结果里的 `dropped` 表由主链 DROP 记账，
    merge_snapshots 以此阻断安装脚本（sql/ 目录）对已清退表的"诈尸复活"并留痕。

静默边界（不建模，属契约而非缺陷）：
  * 不解析触发器、视图、函数；索引/断言/数据族（INSERT/UPDATE 种子）不进快照；
  * SELECT-INTO/动态 SQL 中无法常量化的变量保持近似（护栏假分支按 DO 0 处理）；
  * 表达式默认值、字符集、分区等仅文本留痕，不做语义求值。
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
_DROPTABLE_RE = re.compile(r"^DROP\s+TABLE\s+(.+)$", re.I | re.S)
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




def split_statements(text: str) -> list[str]:
    """引号感知的 ';' 顶层切分：字符串/标识符引号内的 ';' 不切分。

    V211 这类把 ALTER 串进 SET @var 字面量的语句按整体保留；
    V154 类 `COMMENT 'a;b'` 内嵌分号不误切。语句不含结尾 ';'。
    """
    parts: list[str] = []
    cur: list[str] = []
    quote: str | None = None
    i, n = 0, len(text)
    while i < n:
        ch = text[i]
        if quote:
            cur.append(ch)
            if ch == "\\" and quote in ("'", '"') and i + 1 < n:
                cur.append(text[i + 1])
                i += 2
                continue
            if ch == quote:
                quote = None
            i += 1
            continue
        if ch in ("'", '"', "`"):
            quote = ch
            cur.append(ch)
            i += 1
            continue
        if ch == ";":
            stmt = "".join(cur).strip()
            if stmt:
                parts.append(stmt)
            cur = []
            i += 1
            continue
        cur.append(ch)
        i += 1
    tail = "".join(cur).strip()
    if tail:
        parts.append(tail)
    return parts




def _embedded_ddl(stmt: str):
    """提取 SET @var := IF(cond, 'ALTER ...', ...) 类条件 DDL 的字面量语句。

    背景（§12.280 对齐）：V211 等迁移用 information_schema 计数护栏把真 DDL 串进
    SET 语句的字面量，静态回放若直接放行 SET 会漏掉整个迁移目标（location_id）。
    语义取舍：全新安装回放时护栏条件恒为"未存在"，按字面量回放即与真实安装一致；
    若目标已在模型中，ADD/CREATE 分支的同名去重天然幂等，DROP TABLE IF EXISTS 也安全。
    只认 ALTER/DROP/CREATE TABLE 三种前缀，其余字面量一律忽略。
    相邻字符串字面量拼接（MySQL）受支持：以纯空白相连才并入同一语句；
    逗号/括号/其它 token 分隔即为分支字面量（护栏真/假分支互不粘连）。
    """
    prefix = re.compile(r"^\s*(?:ALTER\s+TABLE|DROP\s+TABLE|CREATE\s+TABLE)\b", re.I)
    found = None
    i, n = 0, len(stmt)
    # 相邻字面量拼接（MySQL）：自上一字面量结束以来只出现空白才允许拼接；
    # 逗号/括号/任何 token 都会断开拼接（V44/V05 的 'DO 0', 'ALTER…' 形态即受此保护）
    break_concat = True
    while i < n:
        ch = stmt[i]
        if ch != "'":
            if not ch.isspace():
                break_concat = True
            i += 1
            continue
        j = i + 1
        buf = []
        while j < n:
            c = stmt[j]
            if c == "'" and j + 1 < n and stmt[j + 1] == "'":
                buf.append("'")
                j += 2
                continue
            if c == "\\" and j + 1 < n:
                buf.append(stmt[j + 1])
                j += 2
                continue
            if c == "'":
                break
            buf.append(c)
            j += 1
        lit = "".join(buf)
        if found is None:
            if prefix.match(lit):
                found = lit
        elif not break_concat:
            # 相邻拼接片段：继续并入（无论是否 DDL 前缀）
            found += lit
        i = j + 1
        break_concat = False
    return found


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


def _unescape_string(literal: str) -> str:
    """SQL 字符串字面量内容还原：'' 与 \\ 两种转义都处理。"""
    out: list[str] = []
    i, n = 0, len(literal)
    while i < n:
        ch = literal[i]
        if ch == "'" and i + 1 < n and literal[i + 1] == "'":
            out.append("'")
            i += 2
            continue
        if ch == "\\" and i + 1 < n:
            out.append(literal[i + 1])
            i += 2
            continue
        out.append(ch)
        i += 1
    return "".join(out)


def _split_on(text: str, token: str) -> list[str]:
    """按 delimiter token 顶层切分（引号感知）；token=';' 即普通分号切分。"""
    parts: list[str] = []
    cur: list[str] = []
    quote: str | None = None
    i, n = 0, len(text)
    tlen = len(token)
    while i < n:
        ch = text[i]
        if quote:
            cur.append(ch)
            if ch == "\\" and quote in ("'", '"') and i + 1 < n:
                cur.append(text[i + 1])
                i += 2
                continue
            if ch == quote:
                quote = None
            i += 1
            continue
        if ch in ("'", '"', "`"):
            quote = ch
            cur.append(ch)
            i += 1
            continue
        if token != ";" and text.startswith(token, i):
            stmt = "".join(cur).strip()
            if stmt:
                parts.append(stmt)
            cur = []
            i += tlen
            continue
        if token == ";" and ch == ";":
            stmt = "".join(cur).strip()
            if stmt:
                parts.append(stmt)
            cur = []
            i += 1
            continue
        cur.append(ch)
        i += 1
    tail = "".join(cur).strip()
    if tail:
        parts.append(tail)
    return parts


def tokenize_script(text: str) -> list[str]:
    """DELIMITER 指令感知的脚本语句序列（指令总占一行的迁移文件形态）。"""
    chunks = re.split(r"(?mi)^([ \t]*DELIMITER[ \t]+\S+[ \t]*)$", text)
    stmts: list[str] = []
    delim = ";"
    for idx, chunk in enumerate(chunks):
        if idx % 2 == 1:
            tok = chunk.strip().split(None, 1)[1].strip()
            # 'DELIMITER ;' 是恢复默认边界：';' 本身就是 token，不能剥掉再判空
            # （剥成空会让 delimiter 永远停在上一 token，其后所有语句粘成一整块）
            if tok.endswith(";") and tok != ";":
                tok = tok[:-1].strip()
            if len(tok) >= 2 and tok[0] == tok[-1] and tok[0] in "\"'`":
                tok = tok.strip(tok[0])
            delim = tok or ";"
            continue
        if not chunk or not chunk.strip():
            continue
        stmts.extend(_split_on(chunk, delim))
    return [piece for piece in stmts if piece]


def build_snapshot(directories: list[Path]) -> dict[str, Any]:
    """按文件→语句顺序回放 CREATE/ALTER/DROP/CALL（含过程体嵌套）。"""
    warnings: list[str] = []
    tables: dict[str, dict[str, Any]] = {}
    dropped: dict[str, str] = {}

    def _ensure_table(name: str, source_rel: str | None, ver: str | None) -> dict[str, Any]:
        rec = tables.get(name.lower())
        if rec is None:
            rec = {"columns": [], "primary_key": [], "source": source_rel, "version": ver}
            tables[name.lower()] = rec
        elif rec.get("source") is None:
            rec["source"] = source_rel
            rec["version"] = ver
        return rec

    for version, path in ordered_files(directories, warnings):
        raw = path.read_text(encoding="utf-8-sig", errors="replace")
        text = strip_comments(raw)
        rel = str(path.relative_to(REPO)).replace("\\", "/")
        procs: dict[str, dict[str, Any]] = {}

        def _handle_create_stmt(stmt: str) -> None:
            sm = _CREATE_RE.match(stmt)
            if sm is None:
                return
            table = unquote(sm.group(2)).lower()
            start = sm.end() - 1
            depth = 0
            quote: str | None = None
            end = None
            for i in range(start, len(stmt)):
                ch = stmt[i]
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
                return
            body = stmt[start + 1:end]
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

        def _handle_alter_stmt(stmt: str) -> None:
            am = _ALTER_RE.match(stmt)
            if am is None:
                return
            table = unquote(am.group(2)).lower()
            tail = stmt[am.end():]
            if table not in tables and not re.search(r"\bADD\b", tail, re.I):
                warnings.append(f"{rel}: ALTER TABLE {table} 目标表未解析到，已跳过")
                return
            rec = _ensure_table(table, rel, version)
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
                m_drop = re.match(
                    r"^DROP\s+(?:COLUMN\s+)?([`\"\[]?)([A-Za-z0-9_]+)\1?", f, re.I)
                if m_drop:
                    cname = m_drop.group(2).lower()
                    rec["columns"] = [c for c in rec["columns"] if c["name"].lower() != cname]
                    continue
                if re.match(r"^DROP\s+(?:PRIMARY\s+KEY|KEY|INDEX|CONSTRAINT|FOREIGN\s+KEY)\b", f, re.I):
                    if re.match(r"^DROP\s+PRIMARY\s+KEY\b", f, re.I):
                        rec["primary_key"] = []
                    continue
                m_chg = re.match(
                    r"^CHANGE\s+(?:COLUMN\s+)?([`\"\[]?)([A-Za-z0-9_]+)\1?\s+([`\"\[]?)([A-Za-z0-9_]+)\3?\s+(.+)$",
                    f, re.I | re.S)
                if m_chg:
                    old_name, new_name, typedef = m_chg.group(2), m_chg.group(4), m_chg.group(5)
                    rec["columns"] = [c for c in rec["columns"] if c["name"].lower() != old_name.lower()]
                    col = parse_column(f"{new_name} {typedef}")
                    if col:
                        rec["columns"].append(col)
                    continue
                m_mod = re.match(
                    r"^MODIFY\s+(?:COLUMN\s+)?([`\"\[]?)([A-Za-z0-9_]+)\1?\s+(.+)$",
                    f, re.I | re.S)
                if m_mod:
                    cname, typedef = m_mod.group(2), m_mod.group(3)
                    col = parse_column(f"{cname} {typedef}")
                    if not col:
                        continue
                    replaced = False
                    for i, c in enumerate(rec["columns"]):
                        if c["name"].lower() == cname.lower():
                            rec["columns"][i] = col
                            replaced = True
                            break
                    if not replaced:
                        rec["columns"].append(col)
                    continue
                if re.match(r"^(?:ALGORITHM|LOCK)\s*=\s*\w+", f, re.I) or re.match(
                        r"^CONVERT\s+TO\b", f, re.I):
                    # MySQL 8 在线 DDL 指示与字符集整体转换：不改变列集合（§12.280）
                    continue
                warnings.append(f"{rel}: {table}: 无法识别的 ALTER 片段 -> {f[:80]}")

        def _handle_drop_stmt(stmt: str, source_rel: str = None) -> None:
            dm = _DROPTABLE_RE.match(stmt)
            if dm is None:
                return
            src_rel = source_rel or rel
            names_raw = dm.group(1).strip()
            names_part = re.sub(r"^IF\s+EXISTS\s+", "", names_raw, flags=re.I)
            has_if_exists = names_part != names_raw
            for name in [unquote(part) for part in split_top_level(names_part)]:
                table = name.lower()
                if table in tables:
                    del tables[table]
                    dropped[table] = src_rel
                elif has_if_exists:
                    pass
                else:
                    warnings.append(
                        f"{src_rel}: DROP TABLE {table} 目标表不在模型中（链外删除或拼写差异），已记录")

        def _handle_call_stmt(stmt: str, depth: int = 0) -> None:  # noqa: C901
            cm = re.match(r"^CALL\s+`?([A-Za-z0-9_]+)`?\s*\(", stmt, re.I)
            if cm is None:
                return
            proc = cm.group(1).lower()

            def args_bound(name: str):
                """CONCAT 元素名解析：先查本过程 IN 参数，再查已 SET 的本地变量。"""
                if name.lower() in [p.lower() for p in param_names]:
                    return env.get(name.lower())
                return vars_rt.get(name)
            if depth > 2:
                warnings.append(f"{rel}: CALL {proc} 嵌套深度超过 2，已跳过")
                return
            start_i = cm.end() - 1
            d = 0
            quote: str | None = None
            end_i = None
            for i in range(start_i, len(stmt)):
                ch = stmt[i]
                if quote:
                    if ch == quote:
                        quote = None
                    continue
                if ch in ("'", '"', "`"):
                    quote = ch
                    continue
                if ch == "(":
                    d += 1
                elif ch == ")":
                    d -= 1
                    if d == 0:
                        end_i = i
                        break
            if end_i is None:
                warnings.append(f"{rel}: CALL {proc} 括号未闭合，已跳过")
                return
            args_text = stmt[start_i + 1:end_i]
            args = []
            for piece in (split_top_level(args_text) if args_text.strip() else []):
                arg = piece.strip()
                if len(arg) >= 2 and arg[0] == arg[-1] and arg[0] in "\"'`":
                    args.append(_unescape_string(arg[1:-1]))
                else:
                    args.append(arg)
            proc_rec = procs.get(proc)
            # 动作族一：add_column / add_column_if_not_exists(table, column, definition)
            if re.search(r"add_column(?:_if_not_exists)?$", proc) and len(args) >= 3:
                table, column, typedef = args[0], args[1], args[2].strip()
                col = parse_column(f"{column} {typedef}")
                if col is None:
                    warnings.append(f"{rel}: CALL {proc} 列定义无法解析 -> {column}")
                    return
                rec = _ensure_table(table, rel, version)
                if any(c["name"].lower() == col["name"].lower() for c in rec["columns"]):
                    return
                rec["columns"].append(col)
                return
            # 动作族二：make_nullable(table, column)
            if proc.endswith("make_nullable") and len(args) >= 2:
                table, column = args[0], args[1]
                rec = _ensure_table(table, rel, version)
                hit = False
                for c in rec["columns"]:
                    if c["name"].lower() == column.lower():
                        c["nullable"] = True
                        hit = True
                        break
                if not hit:
                    warnings.append(f"{rel}: CALL {proc} 目标列缺失 -> {table}.{column}")
                return
            # 动作族三：widen_column(table, column, type[, ...]) —— 保属性只换类型
            if proc.endswith("widen_column") and len(args) >= 3:
                table, column, typedef = args[0], args[1], args[2].strip()
                col = parse_column(f"{column} {typedef}")
                if col is None:
                    warnings.append(f"{rel}: CALL {proc} 列定义无法解析 -> {column}")
                    return
                rec = _ensure_table(table, rel, version)
                old = next(
                    (c for c in rec["columns"] if c["name"].lower() == column.lower()), None)
                if old is None:
                    warnings.append(f"{rel}: CALL {proc} 目标列缺失 -> {table}.{column}")
                    return
                new = {**old, "type": col["type"], "raw": col["raw"]}
                rec["columns"][rec["columns"].index(old)] = new
                return
            # 兜底：体符号执行（全新安装假定 IF 护卫真分支；同名 ADD/CREATE 幂等去重）。
            #   SET @x := CONCAT(字面量|参数|已定变量) -> 常量拼接；
            #   PREPARE p FROM @x / EXECUTE p -> @x 组成的 DDL 走同一派发；
            #   SIGNAL/SELECT/数据 DML/自检族 -> 静默忽略（快照不建模数据）。
            body = (proc_rec or {}).get("body") or ""
            param_names = (proc_rec or {}).get("params") or []
            if not body and proc_rec is None and re.search(
                    r"(?:add_column|make_nullable|widen_column)$", proc):
                warnings.append(f"{rel}: CALL {proc} 过程定义未见（疑似列动作），已记录")
                return
            env: dict[str, str | None] = {}
            for i, pname in enumerate(param_names):
                env[pname] = args[i].strip() if i < len(args) else None
            vars_rt: dict[str, str | None] = {}
            prepared: dict[str, str | None] = {}

            def args_bound(name: str):
                """CONCAT 元素名解析：先查本过程 IN 参数，再查已 SET 的本地变量。"""
                if name.lower() in [p.lower() for p in param_names]:
                    return env.get(name.lower())
                return vars_rt.get(name)

            def _route(text: str) -> None:
                head3 = text.lstrip()
                if _CREATE_RE.match(head3):
                    _handle_create_stmt(head3)
                elif _ALTER_RE.match(head3):
                    _handle_alter_stmt(head3)
                elif _DROPTABLE_RE.match(head3):
                    _handle_drop_stmt(head3)

            def _eval_concat(expr: str):
                """CONCAT(字面量|已绑参数|已定变量) -> 常量字符串；任一元素不可求值 -> None。"""
                open_i = expr.find("(")
                close_i = expr.rfind(")")
                if open_i < 0 or close_i <= open_i:
                    return None
                inner_body = expr[open_i + 1:close_i]
                parts: list[str] = []
                for piece in split_top_level(inner_body):
                    el = piece.strip()
                    if not el:
                        continue
                    if el[0] == "'":
                        parts.append(_unescape_string(el[1:-1]))
                    elif re.match(r"^[A-Za-z_][A-Za-z0-9_]*$", el):
                        val = args_bound(el)
                        if val is None:
                            return None
                        parts.append(val)
                    else:
                        return None
                return "".join(parts)

            def _carve_fragment(head2: str) -> None:
                """融合段处理：抽 SET @x := CONCAT(...) 与顶层 DDL 动词子句。"""
                m_set = re.search(r"\bSET\s+@?(\w+)\s*(?::=|=)\s*CONCAT\s*\(", head2, re.I)
                if m_set:
                    open_i = head2.find("(", m_set.end() - 1)
                    close_i = None
                    d = 0
                    quote2: str | None = None
                    for i in range(open_i, len(head2)):
                        c = head2[i]
                        if quote2:
                            if c == "\\" and quote2 in ("'", '"'):
                                i_skip = True
                            elif c == quote2:
                                quote2 = None
                            continue
                        if c in ("'", '"', "`"):
                            quote2 = c
                            continue
                        if c == "(":
                            d += 1
                        elif c == ")":
                            d -= 1
                            if d == 0:
                                close_i = i
                                break
                    if close_i is not None:
                        var = m_set.group(1)
                        vars_rt[var] = _eval_concat(head2[m_set.start():close_i + 1])
                # 顶层 DDL 动词子句（括号深度 0 且引号外）
                i, n = 0, len(head2)
                quote3: str | None = None
                d3 = 0
                while i < n:
                    ch = head2[i]
                    if quote3:
                        if ch == quote3:
                            quote3 = None
                        i += 1
                        continue
                    if ch in ("'", '"', "`"):
                        quote3 = ch
                        i += 1
                        continue
                    if ch == "(":
                        d3 += 1
                    elif ch == ")":
                        d3 -= 1
                    if d3 == 0:
                        mm = re.match(r"(?i)(ALTER\s+TABLE|CREATE\s+TABLE|DROP\s+TABLE)\b", head2[i:])
                        if mm:
                            j = i
                            q4: str | None = None
                            d4 = 0
                            while j < n:
                                c2 = head2[j]
                                if q4:
                                    if c2 == q4:
                                        q4 = None
                                    j += 1
                                    continue
                                if c2 in ("'", '"', "`"):
                                    q4 = c2
                                    j += 1
                                    continue
                                if c2 == "(":
                                    d4 += 1
                                elif c2 == ")":
                                    d4 -= 1
                                elif c2 == ";" and d4 == 0:
                                    break
                                j += 1
                            sub = head2[i:j].strip()
                            if sub:
                                _route(sub)
                            i = j
                            continue
                    i += 1

            for inner in tokenize_script(body):
                ihead = inner.lstrip()
                m_prep = re.match(r"^PREPARE\s+(\w+)\s+FROM\s+@?(\w+)\s*$", ihead, re.I)
                if m_prep:
                    prepared[m_prep.group(1)] = vars_rt.get(m_prep.group(2))
                    continue
                m_exec = re.match(r"^EXECUTE\s+(\w+)\s*$", ihead, re.I)
                if m_exec:
                    composed = prepared.get(m_exec.group(1))
                    if composed:
                        _route(composed)
                    continue
                if re.match(r"^DEALLOCATE\s+PREPARE\s+(\w+)\s*$", ihead, re.I):
                    prepared.pop(re.match(r"^DEALLOCATE\s+PREPARE\s+(\w+)\s*$", ihead, re.I).group(1), None)
                    continue
                if re.match(r"^CALL\s+`?[A-Za-z0-9_]+`?\s*\(", ihead, re.I):
                    _handle_call_stmt(inner, depth + 1)
                    continue
                m_set_direct = re.match(
                    r"^SET\s+@?(\w+)\s*(?::=|=)\s*CONCAT\s*\(", ihead, re.I)
                if m_set_direct:
                    # 纯 SET 段（未与 IF 融合）
                    open_i = ihead.find("(", m_set_direct.end() - 1)
                    close_i2 = None
                    d5 = 0
                    q5: str | None = None
                    for i2 in range(open_i, len(ihead)):
                        c3 = ihead[i2]
                        if q5:
                            if c3 == q5:
                                q5 = None
                            continue
                        if c3 in ("'", '"', "`"):
                            q5 = c3
                            continue
                        if c3 == "(":
                            d5 += 1
                        elif c3 == ")":
                            d5 -= 1
                            if d5 == 0:
                                close_i2 = i2
                                break
                    if close_i2 is not None:
                        vars_rt[m_set_direct.group(1)] = _eval_concat(ihead[m_set_direct.start():close_i2 + 1])
                    continue
                if re.match(r"^(?:IF|THEN|ELSE|ELSIF|END|DECLARE|SELECT|SIGNAL|UPDATE|DELETE)\b", ihead, re.I):
                    # 融合段：先抽 SET-CONCAT / 顶层 DDL，再丢弃纯控制流残片
                    _carve_fragment(ihead)
                    continue
                if (_CREATE_RE.match(ihead) or _ALTER_RE.match(ihead)
                        or _DROPTABLE_RE.match(ihead)):
                    _route(inner)

        for stmt in tokenize_script(text):
            head = stmt.lstrip()
            # 迁移链正文里的条件 DDL 预展开（第一轮语义保留）
            if re.match(r"^(?:SET|SELECT)\b", head, re.I):
                inner = _embedded_ddl(stmt)
                if inner is not None:
                    stmt = inner
                    head = stmt.lstrip()
            m_proc = re.match(r"^CREATE\s+PROCEDURE\s+`?([A-Za-z0-9_]+)`?\s*\(", head, re.I)
            if m_proc is not None:
                name = m_proc.group(1).lower()
                first_begin = re.search(r"\bBEGIN\b", head, re.I)
                last_end = head.rfind("END")
                if first_begin is None or last_end < 0 or last_end <= first_begin.end():
                    warnings.append(f"{rel}: CREATE PROCEDURE {name} 体界识别失败，已跳过")
                    continue
                # 头部参数名清单（CALL 按位置绑定给执行器用）
                decl = []
                hm = re.match(
                    r"^CREATE\s+PROCEDURE\s+(?:IF\s+NOT\s+EXISTS\s+)?`?[A-Za-z0-9_]+`?\s*\((.*)\)\s*$",
                    head[:first_begin.start()], re.I | re.S)
                if hm:
                    for piece in split_top_level(hm.group(1)):
                        pm = re.match(r"^\s*(?:IN|OUT|INOUT)\s+`?(\w+)`?", piece, re.I)
                        if pm:
                            decl.append(pm.group(1).lower())
                procs[name] = {
                    "body": head[first_begin.end():last_end],
                    "params": decl,
                    "source": rel,
                }
                continue
            if re.match(r"^DROP\s+PROCEDURE\b", head, re.I):
                m_dp = re.match(
                    r"^DROP\s+PROCEDURE\s+(?:IF\s+EXISTS\s+)?`?([A-Za-z0-9_]+)`?", head, re.I)
                if m_dp:
                    procs.pop(m_dp.group(1).lower(), None)
                continue
            sm = _CREATE_RE.match(head)
            if sm is not None:
                _handle_create_stmt(stmt)
                continue
            if _ALTER_RE.match(head):
                _handle_alter_stmt(stmt)
                continue
            if _DROPTABLE_RE.match(head):
                _handle_drop_stmt(stmt)
                continue
            if re.match(r"^CALL\b", head, re.I):
                _handle_call_stmt(stmt)
                continue

    for rec in tables.values():
        rec["column_names"] = [c["name"] for c in rec["columns"]]

    return {
        "tables": {k: tables[k] for k in sorted(tables)},
        "table_count": len(tables),
        "warnings": warnings,
        "dropped": dropped,
        "sources": [str(d) for d in directories],
    }


def merge_snapshots(primary: dict[str, Any], secondary: dict[str, Any]) -> dict[str, Any]:
    """以迁移链为主、全新安装脚本为补充；差异记为漂移，不静默取舍。"""
    tables = {k: dict(v) for k, v in primary["tables"].items()}
    for t in tables.values():
        t["source_kind"] = "migration_chain"
    drift: list[dict[str, Any]] = []
    # DROP 感知：迁移链已显式 DROP 的表不容许安装脚本按"补充"复活。
    # （hr.sql 仍 CREATE 着 V208 已清退的 hr_attendance/hr_salary/hr_performance——
    #   全新安装的真实终态是"建了随即被迁移链删掉"，快照必须如实反映，合并需跳过并留痕。）
    dropped = primary.get("dropped") or {}
    warned = []
    for name, rec in secondary["tables"].items():
        if name in dropped:
            warned.append(
                f"sql 层 {name} 已被迁移链 DROP（{dropped[name]}），"
                f"安装脚本仍继续创建已无终态意义，合并时跳过")
            continue
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
        "warnings": list(primary["warnings"]) + warned + list(secondary["warnings"]),
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
