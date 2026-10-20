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
    python scripts/check_do_ddl.py zhicloud-module-wms sql/mysql/wms.sql
    python scripts/check_do_ddl.py zhicloud-module-wms zhicloud-module-wms/src/test/resources/sql/create_tables.sql

退出码：0 = 一致；1 = 发现缺列。
"""
import re
import sys
from pathlib import Path

# BaseDO / TenantBaseDO 提供的公共审计字段，DO 源文件里看不到，需白名单放行
INHERITED_COLUMNS = {
    "creator", "create_time", "updater", "update_time", "deleted", "tenant_id",
}

FIELD_TYPE_RE = re.compile(
    r"^\s*private\s+(?:static\s+|final\s+|transient\s+)*"
    r"([\w.<>,\[\]\s]+?)\s+(\w+)\s*;",
    re.MULTILINE,
)
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
ALTER_TABLE_RE = re.compile(
    r'^\s*ALTER\s+TABLE\s+[`"]?(\w+)[`"]?\s+(.*?);',
    re.IGNORECASE | re.MULTILINE | re.DOTALL,
)
ALTER_ADD_COLUMN_RE = re.compile(
    r'\bADD\s+(?:COLUMN\s+)?(?:IF\s+NOT\s+EXISTS\s+)?[`"]?(\w+)[`"]?\s+[A-Za-z]',
    re.IGNORECASE,
)
ALTER_NON_COLUMN_TOKENS = {
    'primary', 'unique', 'key', 'index', 'constraint', 'foreign',
    'check', 'fulltext', 'spatial', 'partition',
}


def camel_to_snake(name: str) -> str:
    return re.sub(r"(?<!^)(?=[A-Z])", "_", name).lower()


def _mask_java(text: str, *, strings: bool) -> str:
    """把 Java 注释（以及可选的字符串字面量）替换为空格，保留换行与长度。

    这样字段正则和花括号深度不会命中注释里的 ``@TableField``、嵌套类字段
    或字符串里的 ``{}``；注解值本身需要保留，所以解析列名时只屏蔽注释。
    """
    chars = list(text)
    i = 0
    n = len(text)
    while i < n:
        if text.startswith("/*", i):
            end = text.find("*/", i + 2)
            end = n if end < 0 else end + 2
        elif text.startswith("//", i):
            end = text.find("\n", i + 2)
            end = n if end < 0 else end
        elif strings and text[i] in ('"', "'"):
            quote = text[i]
            end = i + 1
            while end < n:
                if text[end] == "\\":
                    end += 2
                elif text[end] == quote:
                    end += 1
                    break
                else:
                    end += 1
        else:
            i += 1
            continue
        for pos in range(i, min(end, n)):
            if chars[pos] != "\n":
                chars[pos] = " "
        i = end
    return "".join(chars)


def parse_do_field_map(text: str) -> dict[str, str]:
    """解析 DO 顶层字段，返回 {列名: Java 类型}。

    只接受类体第一层的非 static 字段：嵌套类里的 DTO/JSON 字段不是数据库列，
    注释里的 ``@TableField`` 也不是注解。列名规则与 ``check_do_ddl`` 一致。
    """
    commentless = _mask_java(text, strings=False)
    code_only = _mask_java(text, strings=True)
    fields: dict[str, str] = {}
    for match in FIELD_TYPE_RE.finditer(code_only):
        if re.search(r"\bstatic\b", match.group(0)):
            continue
        prefix = code_only[:match.start()]
        if prefix.count("{") - prefix.count("}") != 1:
            continue
        java_type, name = match.group(1).strip(), match.group(2)
        head = code_only[:match.start()]
        boundary = max(head.rfind(";"), head.rfind("}"))
        segment = commentless[boundary + 1:match.start()]
        annotation = TABLE_FIELD_RE.search(segment)
        if annotation:
            body = annotation.group(1)
            if re.search(r"exist\s*=\s*false", body):
                continue
            column = re.search(r'(?:value\s*=\s*)?"([^"]+)"', body)
            if column:
                fields[column.group(1).lower()] = java_type
                continue
        fields[camel_to_snake(name)] = java_type
    return fields


def parse_do(path: Path):
    """解析单个 DO 文件，返回 (表名, {列名})。表名未显式声明时返回 None。"""
    text = path.read_text(encoding="utf-8", errors="replace")
    m = TABLE_NAME_RE.search(text)
    if not m:
        return None, set()
    return m.group(1), set(parse_do_field_map(text))


def parse_ddl(paths):
    """解析多个 SQL 文件，返回 {表名: {列名}}。

    先收集所有 ``CREATE TABLE``，再按文件顺序应用行首的静态
    ``ALTER TABLE ... ADD [COLUMN]``。迁移文件名可能按字符串排序
    （例如 V108 排在 V52 前），因此不能边读文件边把 ALTER 应用到
    尚未解析的 CREATE 上。动态拼接的 ALTER 不在本静态检查器的口径内。
    """
    tables = {}
    documents = []

    # Pass 1: collect complete CREATE TABLE models.
    for p in paths:
        text = Path(p).read_text(encoding="utf-8", errors="replace")
        documents.append(text)
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

    # Pass 2: apply direct, line-start ALTER TABLE ADD COLUMN statements.
    for text in documents:
        for alter in ALTER_TABLE_RE.finditer(text):
            table = alter.group(1).lower()
            cols = tables.setdefault(table, set())
            for add in ALTER_ADD_COLUMN_RE.finditer(alter.group(2)):
                token = add.group(1).lower()
                if token in ALTER_NON_COLUMN_TOKENS:
                    continue
                cols.add(token)
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
