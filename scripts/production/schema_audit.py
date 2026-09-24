#!/usr/bin/env python3
"""真实 schema 审计器（Task 0.4）。

设计原则（对应计划里的 Review Gate）：

1. **结论只能来自真库**。期望值可以从静态解析（DO 实体 / DDL 文件 / V90~V92 核验
   SQL）推导，但「缺表 / 缺列 / 自增 / 索引 / 类型」这些判定一律以
   ``information_schema`` 的实际查询结果为准；静态解析与真库不一致时以真库为准，
   并把两边口径差异记录到 ``divergences``，绝不用静态结果冒充动态证据。
2. **只读**。脚本只会执行 ``information_schema`` 查询和 ``SELECT VERSION()``，
   不含任何 DML/DDL；测试 ``test_schema_audit.py`` 对此做门禁。
3. **失败要可见**。退出码：0 = 审计成功且未触发阈值；2 = 审计成功但发现达到阈值的
   问题；1 = 审计无法执行（连接失败、解析失败、SQL 报错）。

用法：

    python scripts/production/schema_audit.py --sandbox-state <state.json> \
        --output ../docs/enterprise-platform/evidence/production/schema-audit.json

    python scripts/production/schema_audit.py --host 127.0.0.1 --port 3306 \
        --user readonly --password-stdin --database zhicloud_platform
"""

from __future__ import annotations

import argparse
import json
import os
import re
import subprocess
import sys
from dataclasses import dataclass, field
from datetime import datetime, timezone
from pathlib import Path
from typing import Any, Callable, Iterable, Sequence

_SCRIPTS_DIR = Path(__file__).resolve().parent.parent
if str(_SCRIPTS_DIR) not in sys.path:
    sys.path.insert(0, str(_SCRIPTS_DIR))

# 复用既有 DO/DDL 静态解析口径，避免同一件事两套规则。
from check_do_ddl import (  # noqa: E402
    CREATE_TABLE_RE,
    INHERITED_COLUMNS,
    TABLE_FIELD_RE,
    camel_to_snake,
)

SEVERITIES = ("P0_BLOCKER", "P1_HIGH", "P2_REVIEW")
SEVERITY_ORDER = {name: index for index, name in enumerate(SEVERITIES)}

# MyBatis-Plus 插入时会自动填充的字段：真库里即使 NOT NULL 也不是「应用写不进去」。
AUTO_FILLED_COLUMNS = set(INHERITED_COLUMNS) | {"id"}

DEFAULT_MODULE_GLOBS = ("zhicloud-module-*/", "zhicloud-framework/*/")
DEFAULT_DDL_GLOB = "sql/mysql/*.sql"
DEFAULT_VERIFY_SQL_DIR = "script/enterprise-platform-upgrade/verification"

FIELD_TYPE_RE = re.compile(
    r"^\s*private\s+(?:static\s+|final\s+|transient\s+)*"
    r"([\w.<>,\[\]\s]+?)\s+(\w+)\s*;",
    re.MULTILINE,
)

DDL_COLUMN_RE = re.compile(
    r'^[`"]?(\w+)[`"]?\s+'
    r"([A-Za-z][A-Za-z0-9]*(?:\s*\([^)]*\))?(?:\s+unsigned)?(?:\s+zerofill)?)",
    re.IGNORECASE,
)
DDL_UNIQUE_RE = re.compile(
    r"^\s*(?:CONSTRAINT\s+[`\"]?\w+[`\"]?\s+)?UNIQUE(?:\s+(?:KEY|INDEX))?\s*"
    r"(?:[`\"]?\w+[`\"]?)?\s*\(([^)]*)\)",
    re.IGNORECASE,
)
DDL_KEY_RE = re.compile(
    r"^\s*(?:KEY|INDEX)\s+[`\"]?\w+[`\"]?\s*\(([^)]*)\)", re.IGNORECASE
)

# MySQL 建表语句里的表级约束行（PRIMARY KEY / CONSTRAINT ... FOREIGN KEY 等）不是列定义。
# 带名字的 UNIQUE / KEY 索引已由上面两个分支抽取，这里只负责「别把它们当成列」。
DDL_CONSTRAINT_RE = re.compile(
    r"^\s*(?:PRIMARY|UNIQUE|KEY|INDEX|CONSTRAINT|FOREIGN|CHECK|FULLTEXT|SPATIAL)\b",
    re.IGNORECASE,
)

# Java 类型 -> 类型族；未列出的类型（List/Map/JSONObject 等）不参与类型比对。
JAVA_TYPE_FAMILIES = {
    "long": "integer", "integer": "integer", "int": "integer", "short": "integer",
    "byte": "integer", "biginteger": "integer", "atomicinteger": "integer",
    "bigdecimal": "decimal", "double": "float", "float": "float",
    "boolean": "boolean",
    "string": "string", "charsequence": "string", "character": "string", "char": "string",
    "localdatetime": "datetime", "offsetdatetime": "datetime", "timestamp": "datetime",
    "date": "datetime", "instant": "datetime",
    "localdate": "date", "localtime": "time",
    "byte[]": "binary",
}

# information_schema.COLUMNS.DATA_TYPE -> 类型族
MYSQL_TYPE_FAMILIES = {
    "bigint": "integer", "int": "integer", "mediumint": "integer",
    "smallint": "integer", "tinyint": "integer", "bit": "boolean",
    "decimal": "decimal", "numeric": "decimal",
    "double": "float", "float": "float", "real": "float",
    "char": "string", "varchar": "string", "tinytext": "string", "text": "string",
    "mediumtext": "string", "longtext": "string", "enum": "string", "set": "string",
    "json": "string",
    "date": "date", "datetime": "datetime", "timestamp": "datetime", "time": "time",
    "year": "date",
    "binary": "binary", "varbinary": "binary", "tinyblob": "binary", "blob": "binary",
    "mediumblob": "binary", "longblob": "binary",
}

# 类型族不同但语义兼容的组合（记为「可接受」，不做 P1 升级）。
COMPATIBLE_FAMILIES = {
    ("integer", "decimal"), ("decimal", "integer"),
    ("boolean", "integer"), ("integer", "boolean"),
}

# 精度/宽度类差异：不阻断上线，但必须人工确认。
WIDTH_FAMILIES = {("date", "datetime"), ("datetime", "date"), ("time", "datetime")}

SQL_SERVER_INFO = (
    "SELECT VERSION(), IFNULL(DATABASE(), ''), IFNULL(@@session.sql_mode, ''), "
    "IFNULL(@@session.time_zone, '')"
)
SQL_TABLES = (
    "SELECT t.TABLE_NAME, IFNULL(t.ENGINE, ''), IFNULL(t.TABLE_ROWS, 0), "
    "IFNULL(t.TABLE_COLLATION, '') "
    "FROM information_schema.TABLES t "
    "WHERE t.TABLE_SCHEMA = DATABASE() AND t.TABLE_TYPE = 'BASE TABLE' "
    "ORDER BY t.TABLE_NAME"
)
SQL_COLUMNS = (
    "SELECT c.TABLE_NAME, c.COLUMN_NAME, c.COLUMN_TYPE, c.DATA_TYPE, c.IS_NULLABLE, "
    "IFNULL(c.COLUMN_DEFAULT, '<NULL>'), IFNULL(c.EXTRA, ''), IFNULL(c.COLUMN_KEY, ''), "
    "IFNULL(c.CHARACTER_MAXIMUM_LENGTH, ''), IFNULL(c.NUMERIC_PRECISION, ''), "
    "IFNULL(c.NUMERIC_SCALE, '') "
    "FROM information_schema.COLUMNS c "
    "WHERE c.TABLE_SCHEMA = DATABASE() "
    "ORDER BY c.TABLE_NAME, c.ORDINAL_POSITION"
)
SQL_INDEXES = (
    "SELECT s.TABLE_NAME, s.INDEX_NAME, s.NON_UNIQUE, s.SEQ_IN_INDEX, "
    "IFNULL(s.COLUMN_NAME, '') "
    "FROM information_schema.STATISTICS s "
    "WHERE s.TABLE_SCHEMA = DATABASE() "
    "ORDER BY s.TABLE_NAME, s.INDEX_NAME, s.SEQ_IN_INDEX"
)

# 审计器会执行的完整 SQL 清单（只读门禁用）。
READ_ONLY_SQL = (SQL_SERVER_INFO, SQL_TABLES, SQL_COLUMNS, SQL_INDEXES)

class SchemaAuditError(RuntimeError):
    """审计无法执行（区别于「审计执行成功但发现问题」）。"""


@dataclass
class ExpectedTable:
    """静态来源推导出的期望结构。"""

    name: str
    columns: dict[str, set[str]] = field(default_factory=dict)
    java_types: dict[str, str] = field(default_factory=dict)
    ddl_types: dict[str, str] = field(default_factory=dict)
    tenant: bool = False
    unique_indexes: set[tuple[str, ...]] = field(default_factory=set)
    indexes: set[tuple[str, ...]] = field(default_factory=set)
    sources: set[str] = field(default_factory=set)

    def add_column(self, column: str, source: str) -> None:
        column = column.lower()
        self.columns.setdefault(column, set()).add(source)
        self.sources.add(source)


@dataclass
class ColumnInfo:
    table: str
    name: str
    column_type: str
    data_type: str
    is_nullable: str
    default: str
    extra: str
    column_key: str
    char_length: str
    numeric_precision: str
    numeric_scale: str

    @property
    def is_boolean_tinyint(self) -> bool:
        return self.data_type.lower() == "tinyint" and self.column_type.lower().startswith("tinyint(1)")

    @property
    def family(self) -> str | None:
        if self.is_boolean_tinyint:
            return "boolean"
        return MYSQL_TYPE_FAMILIES.get(self.data_type.lower())

    @property
    def is_auto_increment(self) -> bool:
        return "auto_increment" in self.extra.lower()

    @property
    def is_generated(self) -> bool:
        return "generated" in self.extra.lower()


@dataclass
class TableInfo:
    name: str
    engine: str
    table_rows: int
    collation: str


@dataclass
class IndexInfo:
    table: str
    name: str
    non_unique: str
    seq: str
    column: str

    @property
    def is_unique(self) -> bool:
        return self.non_unique.strip() in {"0", "false"}


@dataclass
class LiveSchema:
    """真库结构（唯一事实源）。"""

    server_version: str = ""
    database: str = ""
    sql_mode: str = ""
    time_zone: str = ""
    tables: dict[str, TableInfo] = field(default_factory=dict)
    columns: dict[str, dict[str, ColumnInfo]] = field(default_factory=dict)
    index_rows: list[IndexInfo] = field(default_factory=list)

    def index_columns(self, table: str, *, unique: bool | None = None) -> set[tuple[str, ...]]:
        grouped: dict[str, dict[int, str]] = {}
        for row in self.index_rows:
            if row.table != table:
                continue
            if unique is not None and row.is_unique is not unique:
                continue
            try:
                position = int(row.seq)
            except ValueError:
                position = 0
            grouped.setdefault(f"{row.is_unique}:{row.name}", {})[position] = row.column.lower()
        return {tuple(part for _, part in sorted(positions.items())) for positions in grouped.values()}

    def primary_key_columns(self, table: str) -> tuple[str, ...]:
        grouped: dict[int, str] = {}
        for row in self.index_rows:
            if row.table == table and row.name.upper() == "PRIMARY":
                try:
                    grouped[int(row.seq)] = row.column.lower()
                except ValueError:
                    grouped[0] = row.column.lower()
        return tuple(part for _, part in sorted(grouped.items()))

    def has_index(self, table: str, columns: Sequence[str], *, unique: bool | None = None) -> bool:
        wanted = tuple(column.lower() for column in columns)
        return wanted in self.index_columns(table, unique=unique)


@dataclass
class Finding:
    severity: str
    category: str
    table: str
    message: str
    column: str = ""
    expected: str = ""
    actual: str = ""
    confidence: str = "high"
    sources: list[str] = field(default_factory=list)

    def sort_key(self) -> tuple:
        return (
            SEVERITY_ORDER.get(self.severity, 99),
            self.category,
            self.table,
            self.column,
            self.message,
        )

    def to_dict(self) -> dict[str, Any]:
        payload = {
            "severity": self.severity,
            "category": self.category,
            "table": self.table,
            "message": self.message,
            "confidence": self.confidence,
        }
        if self.column:
            payload["column"] = self.column
        if self.expected or self.actual:
            payload["expected"] = self.expected
            payload["actual"] = self.actual
        if self.sources:
            payload["sources"] = sorted(self.sources)
        return payload


@dataclass
class Divergence:
    """静态解析与真库结论不一致：以真库为准，但两边口径差异必须留痕。"""

    kind: str
    table: str
    column: str
    static_says: str
    live_says: str
    resolution: str
    source: str = ""

    def to_dict(self) -> dict[str, Any]:
        return {
            "kind": self.kind,
            "table": self.table,
            "column": self.column,
            "static_says": self.static_says,
            "live_says": self.live_says,
            "resolution": self.resolution,
            "source": self.source,
        }


@dataclass
class AnalysisResult:
    findings: list[Finding] = field(default_factory=list)
    divergences: list[Divergence] = field(default_factory=list)

    def counts_by_severity(self) -> dict[str, int]:
        counts = {severity: 0 for severity in SEVERITIES}
        for finding in self.findings:
            counts[finding.severity] = counts.get(finding.severity, 0) + 1
        return counts

    def counts_by_category(self) -> dict[str, int]:
        counts: dict[str, int] = {}
        for finding in self.findings:
            counts[finding.category] = counts.get(finding.category, 0) + 1
        return dict(sorted(counts.items()))


def parse_do_fields(text: str) -> dict[str, str]:
    """解析单个 DO 源码，返回 {列名: Java 类型}。

    列名口径与 ``scripts/check_do_ddl.py`` 完全一致：``@TableField(value="x")`` 覆盖、
    ``exist = false`` 跳过、其余按驼峰转下划线。
    """
    fields: dict[str, str] = {}
    for match in FIELD_TYPE_RE.finditer(text):
        java_type, name = match.group(1).strip(), match.group(2)
        # 只看本字段声明之前、上一个语句结束符之后的那一段：
        # 旧口径用「前 400 字符 + 最后一个 /**」划窗口，遇到没有 javadoc 的字段时
        # 会把上一条字段的 @TableField 误配到本字段（列名与类型都会被带偏）。
        head = text[:match.start()]
        boundary = max(head.rfind(";"), head.rfind("}"))
        segment = head[boundary + 1:]
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


def parse_do_expectations(module_dirs: Iterable[Path]) -> dict[str, ExpectedTable]:
    tables: dict[str, ExpectedTable] = {}
    for module_dir in module_dirs:
        module_dir = Path(module_dir)
        if not module_dir.exists():
            continue
        for do_file in sorted(module_dir.rglob("*DO.java")):
            if "target" in do_file.parts:
                continue
            text = do_file.read_text(encoding="utf-8", errors="replace")
            name_match = re.search(r'@TableName\s*\(\s*(?:value\s*=\s*)?"([^"]+)"', text)
            if not name_match:
                continue
            table_name = name_match.group(1)
            entry = tables.setdefault(table_name, ExpectedTable(name=table_name))
            fields = parse_do_fields(text)
            for column in fields:
                entry.add_column(column, "do")
            entry.java_types.update(fields)
            if "TenantBaseDO" in text:
                entry.tenant = True
            entry.sources.add("do")
    return tables


def _index_spec_columns(spec: str) -> tuple[str, ...]:
    columns: list[str] = []
    for part in spec.split(","):
        part = part.strip()
        if not part:
            continue
        match = re.match(r'[`"]?(\w+)[`"]?(?:\s*\(\s*\d+\s*\))?', part)
        if match:
            columns.append(match.group(1).lower())
    return tuple(columns)


def parse_ddl_expectations(ddl_files: Iterable[Path]) -> dict[str, ExpectedTable]:
    tables: dict[str, ExpectedTable] = {}
    for ddl_file in ddl_files:
        ddl_file = Path(ddl_file)
        if not ddl_file.exists():
            continue
        text = ddl_file.read_text(encoding="utf-8", errors="replace")
        for match in CREATE_TABLE_RE.finditer(text):
            table_name = match.group(1)
            entry = tables.setdefault(table_name, ExpectedTable(name=table_name))
            for line in match.group(2).splitlines():
                stripped = line.strip().rstrip(",")
                if not stripped or stripped.startswith("--"):
                    continue
                unique = DDL_UNIQUE_RE.match(stripped)
                if unique:
                    columns = _index_spec_columns(unique.group(1))
                    if columns:
                        entry.unique_indexes.add(columns)
                    continue
                key = DDL_KEY_RE.match(stripped)
                if key:
                    columns = _index_spec_columns(key.group(1))
                    if columns:
                        entry.indexes.add(columns)
                    continue
                if DDL_CONSTRAINT_RE.match(stripped):
                    # 表级约束：早期版本会把 PRIMARY KEY (id) 解析成列 primary。
                    continue
                column = DDL_COLUMN_RE.match(stripped)
                if column:
                    column_name = column.group(1).lower()
                    entry.add_column(column_name, "ddl")
                    entry.ddl_types[column_name] = re.sub(r"\s+", " ", column.group(2).strip().lower())
            entry.sources.add("ddl")
    return tables


def parse_verify_expectations(verify_sql_files: Iterable[Path]) -> dict[str, Any]:
    """从冻结的 V9x 核验 SQL 里提取期望值（列清单 / 自增表清单）。"""
    columns: dict[str, set[str]] = {}
    nullable_columns: dict[str, dict[str, str]] = {}
    auto_increment_tables: set[str] = set()
    files: list[str] = []
    for verify_file in verify_sql_files:
        verify_file = Path(verify_file)
        if not verify_file.exists():
            continue
        files.append(str(verify_file))
        text = verify_file.read_text(encoding="utf-8", errors="replace")
        for match in re.finditer(
            r"INSERT\s+INTO\s+tmp_v9[01]_verify_expected\b.*?VALUES(.*?);",
            text,
            re.IGNORECASE | re.DOTALL,
        ):
            for table, column in re.findall(r"\('([^']+)',\s*'([^']+)'\)", match.group(1)):
                columns.setdefault(table.lower(), set()).add(column.lower())
        for match in re.finditer(
            r"WITH\s+expected\s*\(\s*table_name\s*,\s*column_name\s*\)\s*AS\s*\((.*?)\)\s*(?:;|SELECT|,)",
            text,
            re.IGNORECASE | re.DOTALL,
        ):
            for table, column in re.findall(
                r"SELECT\s+'([^']+)'\s*,\s*'([^']+)'", match.group(1), re.IGNORECASE
            ):
                nullable_columns.setdefault(table.lower(), {})[column.lower()] = verify_file.name
        for match in re.finditer(
            r"INSERT\s+INTO\s+tmp_v92_verify_expected\b.*?;", text, re.IGNORECASE | re.DOTALL
        ):
            for table, _module in re.findall(
                r"\('([^']+)',\s*'([^']+)'\)", match.group(0)
            ):
                auto_increment_tables.add(table.lower())
    return {
        "columns": columns,
        "nullable_columns": nullable_columns,
        "auto_increment_tables": sorted(auto_increment_tables),
        "files": files,
    }


def build_expectations(
    module_dirs: Iterable[Path],
    ddl_files: Iterable[Path],
    verify_sql_files: Iterable[Path],
) -> tuple[dict[str, ExpectedTable], dict[str, Any]]:
    """合并三个静态来源；同名列取并集，来源逐列留痕。"""
    merged: dict[str, ExpectedTable] = {}
    do_tables = parse_do_expectations(module_dirs)
    ddl_tables = parse_ddl_expectations(ddl_files)
    verify = parse_verify_expectations(verify_sql_files)

    for source_tables in (do_tables, ddl_tables):
        for name, entry in source_tables.items():
            target = merged.setdefault(name, ExpectedTable(name=name))
            for column, sources in entry.columns.items():
                for source in sources:
                    target.add_column(column, source)
            target.java_types.update(entry.java_types)
            target.ddl_types.update(entry.ddl_types)
            target.unique_indexes |= entry.unique_indexes
            target.indexes |= entry.indexes
            target.tenant = target.tenant or entry.tenant
            target.sources |= entry.sources

    for name, columns in verify["columns"].items():
        target = merged.setdefault(name, ExpectedTable(name=name))
        for column in columns:
            target.add_column(column, "v90_verify")
    for name, columns in verify["nullable_columns"].items():
        target = merged.setdefault(name, ExpectedTable(name=name))
        for column in columns:
            target.add_column(column, "v91_verify")
    for name in verify["auto_increment_tables"]:
        target = merged.setdefault(name, ExpectedTable(name=name))
        target.sources.add("v92_verify")

    sources = {
        "do_tables": len(do_tables),
        "ddl_tables": len(ddl_tables),
        "merged_tables": len(merged),
        "v90_verify_columns": sum(len(value) for value in verify["columns"].values()),
        "v92_verify_tables": len(verify["auto_increment_tables"]),
        "v91_nullable_columns": sum(len(value) for value in verify["nullable_columns"].values()),
        # 期望「可空」的列清单（表 -> 列 -> 来源核验脚本）；analyze 用它做真库比对。
        "nullable_expectations": verify["nullable_columns"],
        "verify_sql_files": verify["files"],
        "ddl_files": [str(Path(path)) for path in ddl_files if Path(path).exists()],
        "module_dirs": [str(Path(path)) for path in module_dirs if Path(path).exists()],
    }
    return merged, sources

MYSQL_CLI_FLAGS = ("--batch", "--raw", "--skip-column-names", "--default-character-set=utf8mb4")


def _split_rows(text: str) -> list[tuple[str, ...]]:
    rows: list[tuple[str, ...]] = []
    for line in text.splitlines():
        if not line.strip():
            continue
        rows.append(tuple(line.split("\t")))
    return rows


def _tail(text: str, limit: int = 800) -> str:
    text = (text or "").strip()
    return text if len(text) <= limit else "..." + text[-limit:]


def make_process_runner(
    argv: Sequence[str],
    *,
    label: str,
    env: Mapping[str, str] | None = None,
    timeout: int = 300,
) -> Callable[[str], list[tuple[str, ...]]]:
    """返回一个把 SQL 通过 stdin 送进 mysql 客户端、解析 TSV 结果的执行器。"""

    def run(sql: str) -> list[tuple[str, ...]]:
        proc = subprocess.run(
            list(argv),
            input=sql,
            capture_output=True,
            text=True,
            encoding="utf-8",
            errors="replace",
            env=None if env is None else dict(env),
            timeout=timeout,
            shell=False,
        )
        if proc.returncode != 0:
            raise SchemaAuditError(
                f"{label} failed with exit code {proc.returncode}: {_tail(proc.stderr or proc.stdout)}"
            )
        return _split_rows(proc.stdout)

    return run


def docker_exec_runner(
    container: str,
    *,
    user: str = "root",
    password: str = "",
    database: str = "",
    docker_bin: str = "docker",
) -> Callable[[str], list[tuple[str, ...]]]:
    """通过 docker exec 在容器内调用 mysql 客户端；容器只允许 localhost 沙箱。

    口令通过 ``MYSQL_PWD`` 环境变量传入容器，不写进任何证据文件。
    """
    argv: list[str] = [docker_bin, "exec", "-i"]
    if password:
        argv += ["-e", f"MYSQL_PWD={password}"]
    argv += [container, "mysql", *MYSQL_CLI_FLAGS, f"-u{user}"]
    if database:
        argv.append(database)
    return make_process_runner(argv, label=f"docker exec {container} mysql")


def mysql_cli_runner(
    *,
    host: str,
    port: int = 3306,
    user: str = "root",
    password: str = "",
    database: str = "",
    mysql_bin: str = "mysql",
) -> Callable[[str], list[tuple[str, ...]]]:
    argv: list[str] = [mysql_bin, *MYSQL_CLI_FLAGS, f"-h{host}", f"-P{port}", f"-u{user}"]
    if database:
        argv.append(database)
    env = dict(os.environ)
    if password:
        env["MYSQL_PWD"] = password
    return make_process_runner(argv, label=f"mysql {host}:{port}", env=env)


def _cell(row: Sequence[str], index: int, default: str = "") -> str:
    if index < len(row):
        return (row[index] or "").strip()
    return default


def load_live_schema(runner: Callable[[str], list[tuple[str, ...]]]) -> LiveSchema:
    schema = LiveSchema()

    info = runner(SQL_SERVER_INFO)
    if info:
        schema.server_version = _cell(info[0], 0)
        schema.database = _cell(info[0], 1)
        schema.sql_mode = _cell(info[0], 2)
        schema.time_zone = _cell(info[0], 3)

    for row in runner(SQL_TABLES):
        name = _cell(row, 0)
        if not name:
            continue
        try:
            rows_estimate = int(_cell(row, 2, "0") or 0)
        except ValueError:
            rows_estimate = -1
        schema.tables[name.lower()] = TableInfo(
            name=name, engine=_cell(row, 1), table_rows=rows_estimate, collation=_cell(row, 3)
        )

    for row in runner(SQL_COLUMNS):
        table = _cell(row, 0).lower()
        name = _cell(row, 1)
        if not table or not name:
            continue
        schema.columns.setdefault(table, {})[name.lower()] = ColumnInfo(
            table=table,
            name=name,
            column_type=_cell(row, 2),
            data_type=_cell(row, 3),
            is_nullable=_cell(row, 4),
            default=_cell(row, 5, "<NULL>"),
            extra=_cell(row, 6),
            column_key=_cell(row, 7),
            char_length=_cell(row, 8),
            numeric_precision=_cell(row, 9),
            numeric_scale=_cell(row, 10),
        )

    for row in runner(SQL_INDEXES):
        table = _cell(row, 0).lower()
        if not table:
            continue
        schema.index_rows.append(
            IndexInfo(
                table=table,
                name=_cell(row, 1),
                non_unique=_cell(row, 2),
                seq=_cell(row, 3, "1"),
                column=_cell(row, 4),
            )
        )
    return schema


def _normalize_java_type(java_type: str) -> str:
    value = re.sub(r"<.*>", "", java_type or "").strip()
    value = value.replace(" ", "")
    if value.endswith("[]"):
        return "byte[]" if value.lower().startswith("byte") else value.lower()
    return value.split(".")[-1].lower()


def _normalize_sql_type(column_type: str) -> str:
    value = (column_type or "").strip().lower()
    value = re.sub(r"\s+", " ", value)
    value = value.replace(" unsigned", "").replace(" zerofill", "")
    return value.strip()


def analyze(
    expected: dict[str, ExpectedTable],
    live: LiveSchema,
    *,
    auto_increment_tables: Iterable[str] = (),
    nullable_expectations: Mapping[str, Mapping[str, str]] | None = None,
) -> AnalysisResult:
    """以真库为准做判定；静态来源只提供「期望」，不直接下结论。"""
    result = AnalysisResult()
    auto_tables = {name.lower() for name in auto_increment_tables}

    for table_name in sorted(expected):
        entry = expected[table_name]
        key = table_name.lower()
        live_columns = live.columns.get(key)
        sources = sorted(entry.sources)
        if live_columns is None:
            result.findings.append(
                Finding(
                    severity="P0_BLOCKER",
                    category="missing_table",
                    table=table_name,
                    message="真库不存在该表；DO/DDL 期望它存在，运行期会直接报错。",
                    expected="表存在",
                    actual="<missing>",
                    sources=sources,
                )
            )
            continue

        for column in sorted(entry.columns):
            column_sources = sorted(entry.columns[column])
            if column in live_columns:
                # id 与 creator/create_time 等基础列由 BaseDO 与 MyBatis-Plus 维护，
                # 静态 DDL 未声明它们属于预期噪声，不作为口径差异留痕。
                if (
                    column_sources == ["do"]
                    and "ddl" in entry.sources
                    and column not in AUTO_FILLED_COLUMNS
                ):
                    result.divergences.append(
                        Divergence(
                            kind="static_do_column_missing_in_live",
                            table=table_name,
                            column=column,
                            static_says="DDL 文件里没有该列（静态解析会判缺失）",
                            live_says="真库存在该列",
                            resolution="live_wins",
                            source="do_vs_ddl",
                        )
                    )
                continue
            if column == "id":
                # 缺 id 由下面的 id_column_missing 单独报，避免同一事实两条 P0。
                continue
            corroborated = column_sources != ["do"]
            result.findings.append(
                Finding(
                    severity="P0_BLOCKER",
                    category="missing_column",
                    table=table_name,
                    column=column,
                    message=(
                        "真库缺列，MyBatis-Plus 读取该字段会报 Unknown column。"
                        if corroborated
                        else "DO 声明了该列但真库不存在，读取路径会报 Unknown column。"
                    ),
                    expected="列存在",
                    actual="<missing>",
                    confidence="high" if corroborated else "medium",
                    sources=column_sources,
                )
            )

        id_column = live_columns.get("id")
        if id_column is None:
            result.findings.append(
                Finding(
                    severity="P0_BLOCKER",
                    category="id_column_missing",
                    table=table_name,
                    column="id",
                    message="真库没有 id 列，MyBatis-Plus 主键映射失效。",
                    expected="id",
                    actual="<missing>",
                    sources=sources,
                )
            )
        else:
            if not id_column.is_auto_increment:
                result.findings.append(
                    Finding(
                        severity="P0_BLOCKER",
                        category="id_not_auto_increment",
                        table=table_name,
                        column="id",
                        message=(
                            "id 列没有 AUTO_INCREMENT：不提供 id 的插入在严格模式下报 1364，"
                            "非严格模式下会插入 id=0 并在第二次插入时撞主键。"
                        ),
                        expected="AUTO_INCREMENT",
                        actual=f"{id_column.column_type} extra={id_column.extra or '<none>'}",
                        sources=sorted(set(sources) | ({"v92_verify"} if key in auto_tables else set())),
                    )
                )
            primary_key = live.primary_key_columns(key)
            if primary_key != ("id",):
                result.findings.append(
                    Finding(
                        severity="P0_BLOCKER",
                        category="primary_key_mismatch",
                        table=table_name,
                        column="id",
                        message="主键不是单列 (id)，MyBatis-Plus 按 id 定位行的假设不成立。",
                        expected="PRIMARY KEY (id)",
                        actual=", ".join(primary_key) or "<none>",
                        sources=sources,
                    )
                )

        if entry.tenant:
            if "tenant_id" not in live_columns:
                result.findings.append(
                    Finding(
                        severity="P1_HIGH",
                        category="tenant_column_missing",
                        table=table_name,
                        column="tenant_id",
                        message="DO 继承 TenantBaseDO，但真库没有 tenant_id，多租户隔离会失败。",
                        expected="tenant_id",
                        actual="<missing>",
                        sources=sources,
                    )
                )
            elif not live.has_index(key, ["tenant_id"]):
                result.findings.append(
                    Finding(
                        severity="P1_HIGH",
                        category="tenant_index_missing",
                        table=table_name,
                        column="tenant_id",
                        message=(
                            "tenant_id 上没有以它为首列的索引：多租户过滤会退化成全表扫描，"
                            "大表上是可观测的性能风险（不阻塞单租户部署）。"
                        ),
                        expected="以 tenant_id 为首列的索引",
                        actual="<none>",
                        confidence="medium",
                        sources=sources,
                    )
                )

        # 没有 id 列的表不是 MyBatis-Plus 形态：插入路径已由 id_column_missing 拦下，
        # 再按 DO 反推 1364 属于重复报警，因此跳过该扫描。
        required_columns = sorted(live_columns.items()) if id_column is not None else []
        for column, info in required_columns:
            if column in entry.columns or column in AUTO_FILLED_COLUMNS:
                continue
            if info.is_nullable.upper() != "NO" or info.default != "<NULL>":
                continue
            if info.is_auto_increment or info.is_generated:
                continue
            result.findings.append(
                Finding(
                    severity="P0_BLOCKER",
                    category="required_column_not_in_do",
                    table=table_name,
                    column=column,
                    message=(
                        "真库该列 NOT NULL 且无默认值，而 DO 里没有这个字段："
                        "MyBatis-Plus 插入会触发 1364。若该列由原生 SQL/mapper 写入，请人工确认后降级。"
                    ),
                    expected="列可空 / 有默认值 / 出现在 DO 中",
                    actual=f"{info.column_type} NOT NULL default=<none>",
                    confidence="medium",
                    sources=sources,
                )
            )

        for column, java_type in sorted(entry.java_types.items()):
            info = live_columns.get(column)
            if info is None:
                continue
            expected_family = JAVA_TYPE_FAMILIES.get(_normalize_java_type(java_type))
            actual_family = info.family
            if not expected_family or not actual_family or expected_family == actual_family:
                continue
            if (expected_family, actual_family) in COMPATIBLE_FAMILIES:
                continue
            pair = (expected_family, actual_family)
            severity = "P2_REVIEW" if pair in WIDTH_FAMILIES else "P1_HIGH"
            category = "column_width_review" if severity == "P2_REVIEW" else "column_type_mismatch"
            result.findings.append(
                Finding(
                    severity=severity,
                    category=category,
                    table=table_name,
                    column=column,
                    message=(
                        f"Java 类型 {java_type}（{expected_family}）与真库类型 {info.column_type}"
                        f"（{actual_family}）不属于同一类型族，MyBatis 映射存在运行期风险。"
                    ),
                    expected=f"{java_type} -> {expected_family}",
                    actual=f"{info.column_type} -> {actual_family}",
                    sources=sorted(entry.columns.get(column, ["do"])),
                )
            )

        for column, ddl_type in sorted(entry.ddl_types.items()):
            info = live_columns.get(column)
            if info is None:
                continue
            if _normalize_sql_type(ddl_type) == _normalize_sql_type(info.column_type):
                continue
            result.findings.append(
                Finding(
                    severity="P2_REVIEW",
                    category="column_type_declaration_drift",
                    table=table_name,
                    column=column,
                    message="DDL 声明的列类型与真库实际类型不一致（宽度/精度类差异，需人工确认）。",
                    expected=ddl_type,
                    actual=info.column_type,
                    confidence="medium",
                    sources=["ddl"],
                )
            )

        for columns in sorted(entry.unique_indexes):
            if not live.has_index(key, columns, unique=True):
                result.findings.append(
                    Finding(
                        severity="P1_HIGH",
                        category="missing_unique_index",
                        table=table_name,
                        column=",".join(columns),
                        message="DDL 声明了唯一键，但真库缺少同列的唯一索引。",
                        expected=f"UNIQUE ({', '.join(columns)})",
                        actual="<none>",
                        sources=["ddl"],
                    )
                )
        for columns in sorted(entry.indexes):
            if not live.has_index(key, columns):
                result.findings.append(
                    Finding(
                        severity="P1_HIGH",
                        category="missing_index",
                        table=table_name,
                        column=",".join(columns),
                        message="DDL 声明了索引，但真库缺少同列索引。",
                        expected=f"KEY ({', '.join(columns)})",
                        actual="<none>",
                        sources=["ddl"],
                    )
                )

    for table_name in sorted(nullable_expectations or {}):
        live_columns = live.columns.get(table_name.lower())
        if live_columns is None:
            continue
        for column, source in sorted((nullable_expectations or {})[table_name].items()):
            info = live_columns.get(column)
            if info is None or info.is_nullable.upper() == "YES":
                continue
            result.findings.append(
                Finding(
                    severity="P0_BLOCKER",
                    category="nullable_expectation_unmet",
                    table=table_name,
                    column=column,
                    message=(
                        "核验 SQL 要求该列可空（V91 放行 phantom NOT NULL），但真库仍为 NOT NULL："
                        "迁移未应用或未生效，插入路径仍可能被 1364 阻断。"
                    ),
                    expected="NULLABLE",
                    actual=f"{info.column_type} {info.is_nullable}",
                    sources=[source],
                )
            )

    result.findings.sort(key=Finding.sort_key)
    result.divergences.sort(key=lambda item: (item.kind, item.table, item.column))
    return result

LOCAL_DATABASE_HOSTS = {"localhost", "127.0.0.1", "::1"}


def build_report(
    *,
    expected: dict[str, ExpectedTable],
    sources: dict[str, Any],
    live: LiveSchema,
    result: AnalysisResult,
    repo_root: Path,
    connection: dict[str, Any],
    threshold: str,
    exit_code: int,
    notes: Sequence[str] = (),
) -> dict[str, Any]:
    by_severity = result.counts_by_severity()
    expected_columns = sum(len(entry.columns) for entry in expected.values())
    present_columns = 0
    for name, entry in expected.items():
        live_columns = live.columns.get(name.lower()) or {}
        present_columns += sum(1 for column in entry.columns if column in live_columns)
    return {
        "status": "passed" if exit_code == 0 else "failed",
        "generated_at": datetime.now(timezone.utc).isoformat(),
        "repo_root": str(repo_root),
        "connection": connection,
        "server": {
            "version": live.server_version,
            "database": live.database,
            "sql_mode": live.sql_mode,
            "time_zone": live.time_zone,
        },
        "sources": sources,
        "summary": {
            "fail_on": threshold,
            "exit_code": exit_code,
            "tables_expected": len(expected),
            "tables_present": sum(1 for name in expected if name.lower() in live.tables),
            "tables_in_database": len(live.tables),
            "columns_expected": expected_columns,
            "columns_present": present_columns,
            "findings_total": len(result.findings),
            "divergences_total": len(result.divergences),
            "by_severity": by_severity,
            "by_category": result.counts_by_category(),
        },
        "findings": [finding.to_dict() for finding in result.findings],
        "divergences": [item.to_dict() for item in result.divergences],
        "notes": list(notes),
    }


def default_module_dirs(repo_root: Path) -> list[Path]:
    dirs = sorted(repo_root.glob("zhicloud-module-*"))
    dirs += sorted((repo_root / "zhicloud-framework").glob("zhicloud-*"))
    return [path for path in dirs if path.is_dir()]


def default_ddl_files(repo_root: Path) -> list[Path]:
    return sorted((repo_root / "sql" / "mysql").glob("*.sql"))


def default_verify_sql_files(repo_root: Path) -> list[Path]:
    directory = repo_root.parent / DEFAULT_VERIFY_SQL_DIR
    return sorted(directory.glob("V*_verify.sql"))


def _read_password(args: argparse.Namespace) -> str:
    if args.password:
        return args.password
    if args.password_stdin:
        return sys.stdin.readline().strip()
    return ""


def resolve_runner(args: argparse.Namespace) -> tuple[Callable[[str], list[tuple[str, ...]]], dict[str, Any]]:
    if args.sandbox_state:
        state_path = Path(args.sandbox_state).expanduser().resolve()
        if not state_path.is_file():
            raise SchemaAuditError(f"sandbox state file not found: {state_path}. Run mysql-sandbox.ps1 start first.")
        try:
            state = json.loads(state_path.read_text(encoding="utf-8-sig"))
        except json.JSONDecodeError as exc:
            raise SchemaAuditError(f"sandbox state file is not valid JSON: {exc}") from exc
        container = state.get("container_name") or ""
        if not container:
            raise SchemaAuditError(f"sandbox state file has no container_name: {state_path}")
        database = args.database or state.get("database") or ""
        runner = docker_exec_runner(
            container,
            user=args.user,
            password=_read_password(args) or str(state.get("root_password") or ""),
            database=database,
            docker_bin=args.docker_bin,
        )
        return runner, {
            "mode": "docker-exec",
            "container": container,
            "database": database,
            "user": args.user,
            "sandbox_state": str(state_path),
            "image": state.get("image", ""),
        }

    host = args.host or "127.0.0.1"
    if host not in LOCAL_DATABASE_HOSTS and not args.allow_remote_host:
        raise SchemaAuditError(
            f"refusing host '{host}': only localhost is allowed unless --allow-remote-host is given "
            "(this tool is an audit, not a production operation)"
        )
    if not args.database:
        raise SchemaAuditError("--database is required when使用 --host 模式")
    runner = mysql_cli_runner(
        host=host,
        port=args.port,
        user=args.user,
        password=_read_password(args),
        database=args.database,
        mysql_bin=args.mysql_bin,
    )
    return runner, {
        "mode": "mysql-cli",
        "host": host,
        "port": args.port,
        "database": args.database,
        "user": args.user,
        "mysql_bin": args.mysql_bin,
    }


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        description="以 information_schema 为事实源的 ZhiCloud schema 审计器（只读）。"
    )
    parser.add_argument("--repo-root", default=None, help="仓库根目录，默认取脚本上两级目录")
    parser.add_argument("--output", default=None, help="JSON 证据输出路径；不传则只打印摘要")
    parser.add_argument("--fail-on", choices=list(SEVERITIES), default="P0_BLOCKER")
    parser.add_argument("--module", action="append", default=[], help="参与静态解析的模块目录，可重复")
    parser.add_argument("--ddl", action="append", default=[], help="参与静态解析的 DDL 文件，可重复")
    parser.add_argument("--verify-sql-dir", default=None, help="V9x 核验 SQL 目录")
    parser.add_argument("--skip-verify-sql", action="store_true", help="不解析 V9x 核验 SQL")
    parser.add_argument("--sandbox-state", default=None, help="mysql-sandbox.ps1 生成的 state.json")
    parser.add_argument("--docker-bin", default="docker")
    parser.add_argument("--host", default=None)
    parser.add_argument("--port", type=int, default=3306)
    parser.add_argument("--user", default="root")
    parser.add_argument("--password", default=None, help="不推荐：优先用 --password-stdin 或沙箱 state")
    parser.add_argument("--password-stdin", action="store_true")
    parser.add_argument("--database", default=None)
    parser.add_argument("--mysql-bin", default="mysql")
    parser.add_argument("--allow-remote-host", action="store_true")
    parser.add_argument("--print-findings", type=int, default=20, help="标准输出里打印的明细条数")
    return parser


def main(argv: Sequence[str] | None = None) -> int:
    args = build_parser().parse_args(argv)
    repo_root = (
        Path(args.repo_root).expanduser().resolve()
        if args.repo_root
        else Path(__file__).resolve().parents[2]
    )

    try:
        module_dirs = [Path(item).resolve() for item in args.module] or default_module_dirs(repo_root)
        ddl_files = [Path(item).resolve() for item in args.ddl] or default_ddl_files(repo_root)
        if args.skip_verify_sql:
            verify_files: list[Path] = []
        elif args.verify_sql_dir:
            verify_files = sorted(Path(args.verify_sql_dir).resolve().glob("V*_verify.sql"))
        else:
            verify_files = default_verify_sql_files(repo_root)

        expected, sources = build_expectations(module_dirs, ddl_files, verify_files)
        if not expected:
            raise SchemaAuditError("no expected tables found; check --module / --ddl paths")

        runner, connection = resolve_runner(args)
        live = load_live_schema(runner)
        # V92 核验 SQL 指定的自增表清单（真库为准，清单只用于补充 sources 标签）。
        auto_increment_tables = [
            name for name, entry in expected.items() if "v92_verify" in entry.sources
        ]
        result = analyze(
            expected,
            live,
            auto_increment_tables=auto_increment_tables,
            nullable_expectations=sources.get("nullable_expectations") or {},
        )
    except (SchemaAuditError, OSError) as exc:
        print(f"[ERROR] {exc}", file=sys.stderr)
        return 1

    threshold_index = SEVERITY_ORDER[args.fail_on]
    triggered = [
        finding for finding in result.findings if SEVERITY_ORDER.get(finding.severity, 99) <= threshold_index
    ]
    exit_code = 2 if triggered else 0

    notes = [
        "只读审计：脚本只查询 information_schema 与 SELECT VERSION()，不修改任何数据。",
        "口径规则：期望来自 DO / DDL / V9x 核验 SQL 的静态解析，结论一律以真库 information_schema 为准；"
        "静态与真库不一致时以真库为准并记录在 divergences。",
        f"P0_BLOCKER 定义：缺表、缺列、主键/自增错误、NOT NULL 无默认列导致插入阻断；"
        f"P1_HIGH：索引/租户字段/类型族不兼容；P2_REVIEW：类型宽度与声明漂移。",
        "P0 未清零前不得进入生产；P1 需要单独的风险接受记录；P2 由业务/DB 负责人人工确认。",
    ]
    if connection.get("mode") == "docker-exec":
        notes.append("本次审计运行在一次性 MySQL 沙箱容器上（非生产库）。")
    else:
        notes.append("本次审计通过 mysql 客户端直连（未使用沙箱）；请确认目标库不是生产库。")

    report = build_report(
        expected=expected,
        sources=sources,
        live=live,
        result=result,
        repo_root=repo_root,
        connection=connection,
        threshold=args.fail_on,
        exit_code=exit_code,
        notes=notes,
    )

    if args.output:
        output_path = Path(args.output).expanduser().resolve()
        output_path.parent.mkdir(parents=True, exist_ok=True)
        output_path.write_text(
            json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
        )
        print(f"[INFO] report written to {output_path}")

    summary = report["summary"]
    print(
        "[INFO] tables expected={tables_expected} present={tables_present} db_tables={tables_in_database}; "
        "columns expected={columns_expected} present={columns_present}".format(**summary)
    )
    print(
        "[INFO] findings total={} P0={} P1={} P2={} divergences={}".format(
            summary["findings_total"],
            summary["by_severity"].get("P0_BLOCKER", 0),
            summary["by_severity"].get("P1_HIGH", 0),
            summary["by_severity"].get("P2_REVIEW", 0),
            summary["divergences_total"],
        )
    )
    for finding in result.findings[: max(0, args.print_findings)]:
        location = finding.table + (f".{finding.column}" if finding.column else "")
        print(f"[{finding.severity}] {finding.category} {location} :: {finding.message}")
    print(f"[INFO] exit code = {exit_code} (fail-on {args.fail_on})")
    return exit_code


if __name__ == "__main__":
    raise SystemExit(main())
