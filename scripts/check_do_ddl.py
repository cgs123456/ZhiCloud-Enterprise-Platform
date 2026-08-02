#!/usr/bin/env python3
"""校验 MyBatis-Plus DO 实体字段与建表 DDL 列定义的一致性。

背景：DO 里新增字段但忘记同步改 DDL，是本仓库反复出现的一类缺陷。
症状分两种：
  1. 生产 DDL 缺列 → 运行期 `Unknown column 'xxx'`，接口直接 500；
  2. H2 单测 DDL 缺列 → 单测报 `BadSqlGrammarException: Column "xxx" not found`。
两者都不会在编译期暴露，只能靠运行才发现，因此需要静态校验兜底。

用法：
    python scripts/check_do_ddl.py <module-dir> <ddl.sql> [<ddl2.sql> ...]

示例：
    python scripts/check_do_ddl.py yudao-module-wms sql/mysql/wms.sql
    python scripts/check_do_ddl.py yudao-module-wms yudao-module-wms/src/test/resources/sql/create_tables.sql

退出码：0 = 一致；1 = 发现缺列。
"""
import re
import sys
from pathlib import Path

# BaseDO / TenantBaseDO 提供的公共审计字段，DO 源文件里看不到，需白名单放行
INHERITED_COLUMNS = {
    "creator", "create_time", "updater", "update_time", "deleted", "tenant_id",
}

FIELD_RE = re.compile(
    r"^\s*private\s+(?:static\s+|final\s+|transient\s+)*"
    r"[\w.<>,\[\]\s]+?\s+(\w+)\s*;",
    re.MULTILINE,
)
TABLE_NAME_RE = re.compile(r'@TableName\s*\(\s*(?:value\s*=\s*)?"([^"]+)"')
TABLE_FIELD_RE = re.compile(r'@TableField\s*\(([^)]*)\)')
CREATE_TABLE_RE = re.compile(
    r'CREATE\s+TABLE\s+(?:IF\s+NOT\s+EXISTS\s+)?[`"]?(\w+)[`"]?\s*\((.*?)\n\)\s*',
    re.IGNORECASE | re.DOTALL,
)


def camel_to_snake(name: str) -> str:
    return re.sub(r"(?<!^)(?=[A-Z])", "_", name).lower()


def parse_do(path: Path):
    """解析单个 DO 文件，返回 (表名, {列名})。表名未显式声明时返回 None。"""
    text = path.read_text(encoding="utf-8", errors="replace")
    m = TABLE_NAME_RE.search(text)
    if not m:
        return None, set()
    table = m.group(1)

    columns = set()
    for fm in FIELD_RE.finditer(text):
        field = fm.group(1)
        # 向前回看该字段的注解块，识别 exist=false / 自定义列名
        head = text[max(0, fm.start() - 400):fm.start()]
        ann = TABLE_FIELD_RE.search(head[head.rfind("/**"):] if "/**" in head else head)
        if ann:
            body = ann.group(1)
            if re.search(r"exist\s*=\s*false", body):
                continue
            cm = re.search(r'(?:value\s*=\s*)?"([^"]+)"', body)
            if cm:
                columns.add(cm.group(1).lower())
                continue
        columns.add(camel_to_snake(field))
    return table, columns


def parse_ddl(paths):
    """解析多个 SQL 文件，返回 {表名: {列名}}。"""
    tables = {}
    for p in paths:
        text = Path(p).read_text(encoding="utf-8", errors="replace")
        for m in CREATE_TABLE_RE.finditer(text):
            table = m.group(1).lower()
            cols = set()
            for line in m.group(2).splitlines():
                line = line.strip().rstrip(",")
                if not line or line.startswith("--"):
                    continue
                cm = re.match(r'[`"]?(\w+)[`"]?\s+\w', line)
                if not cm:
                    continue
                token = cm.group(1).upper()
                if token in {"PRIMARY", "UNIQUE", "KEY", "INDEX", "CONSTRAINT", "FOREIGN"}:
                    continue
                cols.add(cm.group(1).lower())
            tables.setdefault(table, set()).update(cols)
    return tables


def main() -> int:
    if len(sys.argv) < 3:
        print(__doc__)
        return 2
    module_dir, ddl_files = Path(sys.argv[1]), sys.argv[2:]
    ddl_tables = parse_ddl(ddl_files)

    problems = []
    checked = 0
    for do_file in module_dir.rglob("*DO.java"):
        if "/target/" in do_file.as_posix():
            continue
        table, columns = parse_do(do_file)
        if not table:
            continue
        checked += 1
        ddl_cols = ddl_tables.get(table.lower())
        if ddl_cols is None:
            problems.append((table, do_file.name, "整表缺失", []))
            continue
        missing = sorted(c for c in columns if c not in ddl_cols and c not in INHERITED_COLUMNS)
        if missing:
            problems.append((table, do_file.name, "缺列", missing))

    print(f"[INFO] 已检查 {checked} 个 DO，DDL 中解析到 {len(ddl_tables)} 张表")
    if not problems:
        print("[OK] DO 字段与 DDL 列定义完全一致。")
        return 0

    print(f"[WARN] 发现 {len(problems)} 处不一致：")
    for table, do_name, kind, missing in problems:
        detail = f"（缺少 {len(missing)} 列：{', '.join(missing)}）" if missing else ""
        print(f"  - {table:<32} [{kind}] <- {do_name} {detail}")
    return 1


if __name__ == "__main__":
    raise SystemExit(main())
