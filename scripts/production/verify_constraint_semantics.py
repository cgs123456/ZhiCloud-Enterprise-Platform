#!/usr/bin/env python3
"""Verify V116/V117 database-constraint semantics on an isolated MySQL sandbox.

This verifier is deliberately sandbox-only. It records evidence that can support
a narrow MySQL behavior claim, while keeping production readiness, Gate 3 and
the related risk items OPEN.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import re
import subprocess
import sys
import time
import uuid
from datetime import datetime
from pathlib import Path
from typing import Any

REPO_ROOT = Path(__file__).resolve().parents[2]
V116_MIGRATION = (
    REPO_ROOT
    / "zhicloud-server/src/main/resources/db/migration/V116__system_users_active_username_unique.sql"
)
V117_MIGRATION = (
    REPO_ROOT
    / "zhicloud-server/src/main/resources/db/migration/V117__crm_receivable_idempotency.sql"
)
V116_VERIFY = (
    REPO_ROOT
    / "zhicloud-server/src/main/resources/db/verification/V116_verify.sql"
)
V117_VERIFY = (
    REPO_ROOT
    / "zhicloud-server/src/main/resources/db/verification/V117_verify.sql"
)
CONTRACT = REPO_ROOT / "scripts/production/migration-contract.json"
DEFAULT_OUTPUT_DIR = REPO_ROOT / "docs/enterprise-platform/evidence/production"
SAFE_DB_RE = re.compile(r"^zhicloud_sandbox[A-Za-z0-9_]+$")
MYSQL_ERROR_RE = re.compile(r"\bERROR\s+(\d{4})\b")
DISCLAIMER = (
    "FAKE-SIMULATED-DATA: isolated schema/data only; "
    "production_import_allowed=false"
)
MAX_TEXT = 16000
DEFAULT_ROWS = 100_000
DEFAULT_DATABASE = "zhicloud_sandbox_constraint_semantics"

CAVEATS = [
    "Synthetic minimal schema and synthetic row counts; this is not a production-scale migration proof.",
    "isolated MySQL 8.4.x sandbox only; exact server version is recorded in evidence; non-MySQL dialects and their equivalent constraints remain unverified.",
    "No replication, backup/restore, production traffic, long-running DML, or MDL contention was exercised.",
    "InnoDB DATA_LENGTH/INDEX_LENGTH are approximate and timings are single-run observations.",
    "This evidence does not prove Flyway parser behavior, application transaction rollback, BPM/Flowable behavior, or production readiness.",
    "Repository metadata records HEAD plus a dirty-worktree digest; a dirty worktree is not a frozen release, so verifier_sha256 identifies the executed script.",
]


def assert_sandbox_database(name: str) -> None:
    """Reject every database outside the sandbox namespace."""
    if not name or not SAFE_DB_RE.fullmatch(name):
        raise ValueError(f"unsafe database name: {name!r}")


def parse_mysql_error_code(stderr: str) -> int | None:
    """Return the MySQL numeric error code, if the client reported one."""
    match = MYSQL_ERROR_RE.search(stderr or "")
    return int(match.group(1)) if match else None


def classify_concurrent_results(results: list[dict[str, Any]]) -> dict[str, Any]:
    """Require exactly one committed writer and one duplicate-key loser."""
    success_count = sum(1 for item in results if item.get("exit_code") == 0)
    duplicate_key_count = sum(
        1 for item in results if item.get("mysql_error_code") == 1062
    )
    other_failure_count = sum(
        1
        for item in results
        if item.get("exit_code") != 0 and item.get("mysql_error_code") != 1062
    )
    passed = (
        len(results) == 2
        and success_count == 1
        and duplicate_key_count == 1
        and other_failure_count == 0
    )
    return {
        "passed": passed,
        "success_count": success_count,
        "duplicate_key_count": duplicate_key_count,
        "other_failure_count": other_failure_count,
    }


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def git_output(*args: str) -> str | None:
    """Return stripped git output, or None when git cannot answer."""
    try:
        completed = subprocess.run(
            ["git", "-C", str(REPO_ROOT), *args],
            capture_output=True,
            text=True,
            encoding="utf-8",
            errors="replace",
            check=False,
        )
    except OSError:
        return None
    if completed.returncode != 0:
        return None
    return completed.stdout.strip()


def repository_metadata() -> dict[str, Any]:
    """Record the exact script hash and the commit/worktree context of the run."""
    commit = git_output("rev-parse", "HEAD")
    branch = git_output("branch", "--show-current")
    status = git_output("status", "--porcelain=v1", "-uall")
    verifier = Path(__file__).resolve()
    return {
        "repo_commit": commit,
        "repo_branch": branch or None,
        "repo_worktree_dirty": bool(status),
        "repo_worktree_status_count": len(status.splitlines()) if status is not None else None,
        "repo_worktree_status_sha256": (
            hashlib.sha256(status.encode("utf-8")).hexdigest() if status is not None else None
        ),
        "verifier_path": str(verifier),
        "verifier_sha256": sha256(verifier),
    }


def build_evidence(
    server: dict[str, Any],
    artifacts: dict[str, Any],
    scenarios: list[dict[str, Any]],
    *,
    caveats: list[str] | None = None,
) -> dict[str, Any]:
    """Build fail-closed evidence with explicit non-production state."""
    failed = sum(1 for scenario in scenarios if not scenario.get("passed"))
    status = "passed" if scenarios and failed == 0 else "failed"
    return {
        "generated_at": datetime.now().astimezone().isoformat(),
        "disclaimer": DISCLAIMER,
        "production_import_allowed": False,
        "production_ready": False,
        "gate3_passed": False,
        "r011_status": "OPEN",
        "r029_status": "OPEN",
        "independent_review": False,
        "repository": repository_metadata(),
        "scope": (
            "MySQL sandbox behavior only: V116 active-username uniqueness and "
            "soft-delete recreation; V117 idempotency-key uniqueness and "
            "soft-delete occupancy; synthetic DDL cost."
        ),
        "evidence_boundaries": {
            "database": "isolated native MySQL sandbox",
            "schema": "minimal tables required by V116/V117",
            "execution": "original migration SQL files via mysql.exe",
            "production_import_allowed": False,
        },
        "server": server,
        "artifacts": artifacts,
        "scenarios": scenarios,
        "caveats": list(caveats or []),
        "summary": {
            "total": len(scenarios),
            "passed": len(scenarios) - failed,
            "failed": failed,
        },
        "status": status,
    }


def sql_literal(value: str) -> str:
    """Render a small, trusted test value as a MySQL string literal."""
    return "'" + value.replace("\\", "\\\\").replace("'", "''") + "'"


def build_v116_concurrent_sql(username: str, sleep_seconds: int = 2) -> str:
    return (
        "START TRANSACTION;\n"
        "INSERT INTO `system_users` (`tenant_id`, `username`, `deleted`) "
        f"VALUES (1, {sql_literal(username)}, 0);\n"
        f"SELECT SLEEP({int(sleep_seconds)});\n"
        "COMMIT;\n"
    )


def build_v117_concurrent_sql(idempotency_key: str, sleep_seconds: int = 2) -> str:
    return (
        "START TRANSACTION;\n"
        "INSERT INTO `crm_receivable` "
        "(`tenant_id`, `idempotency_key`, `payload_hash`, `deleted`) "
        f"VALUES (1, {sql_literal(idempotency_key)}, REPEAT('a', 64), 0);\n"
        f"SELECT SLEEP({int(sleep_seconds)});\n"
        "COMMIT;\n"
    )


def build_ddl_seed_sql(table: str, rows: int) -> str:
    if rows < 1:
        raise ValueError("rows must be positive")
    if table == "system_users":
        insert = (
            "INSERT INTO `system_users` (`tenant_id`, `username`, `deleted`)\n"
            "WITH RECURSIVE seq(n) AS (\n"
            "  SELECT 1\n"
            "  UNION ALL\n"
            f"  SELECT n + 1 FROM seq WHERE n < {rows}\n"
            ")\n"
            "SELECT 1 + MOD(n - 1, 100),\n"
            "       CONCAT('ddl_user_', LPAD(n, 12, '0')),\n"
            "       0\n"
            "  FROM seq;\n"
        )
    elif table == "crm_receivable":
        insert = (
            "INSERT INTO `crm_receivable` (`tenant_id`, `deleted`)\n"
            "WITH RECURSIVE seq(n) AS (\n"
            "  SELECT 1\n"
            "  UNION ALL\n"
            f"  SELECT n + 1 FROM seq WHERE n < {rows}\n"
            ")\n"
            "SELECT 1 + MOD(n - 1, 100), 0\n"
            "  FROM seq;\n"
        )
    else:
        raise ValueError(f"unsupported seed table: {table!r}")
    return f"SET SESSION cte_max_recursion_depth = {rows + 100};\n{insert}"
def build_analyze_table_sql(table: str) -> str:
    if table not in {"system_users", "crm_receivable"}:
        raise ValueError(f"unsupported table: {table!r}")
    return f"ANALYZE TABLE `{table}`;"

def mysql_args(args: argparse.Namespace, database: str | None = None) -> list[str]:
    command = [
        args.mysql_client,
        "--protocol=TCP",
        "-h",
        args.host,
        "-P",
        str(args.port),
        "-u",
        args.user,
        "--default-character-set=utf8mb4",
        "--batch",
        "--raw",
        "--skip-column-names",
        "--connect-timeout=10",
    ]
    if args.password:
        command.append(f"--password={args.password}")
    if database:
        assert_sandbox_database(database)
        command.append(database)
    return command


def _completed_record(
    proc: subprocess.CompletedProcess[bytes], started: float, *, slot: int | None = None
) -> dict[str, Any]:
    stderr = proc.stderr.decode("utf-8", "replace")[:MAX_TEXT]
    record: dict[str, Any] = {
        "exit_code": proc.returncode,
        "duration_s": round(time.perf_counter() - started, 3),
        "stdout": proc.stdout.decode("utf-8", "replace")[:MAX_TEXT],
        "stderr": stderr,
        "mysql_error_code": parse_mysql_error_code(stderr),
    }
    if slot is not None:
        record["slot"] = slot
    return record


def run_sql(
    args: argparse.Namespace,
    database: str | None,
    sql: str,
    *,
    check: bool = False,
) -> dict[str, Any]:
    started = time.perf_counter()
    proc = subprocess.run(
        mysql_args(args, database),
        input=sql.encode("utf-8"),
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        timeout=args.timeout,
    )
    record = _completed_record(proc, started)
    if check and proc.returncode != 0:
        raise RuntimeError(f"SQL failed ({proc.returncode}): {record['stderr']}")
    return record


def run_sql_file(
    args: argparse.Namespace,
    database: str,
    path: Path,
    *,
    check: bool = False,
) -> dict[str, Any]:
    started = time.perf_counter()
    with path.open("rb") as stream:
        proc = subprocess.run(
            mysql_args(args, database),
            stdin=stream,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            timeout=args.timeout,
        )
    record = _completed_record(proc, started)
    record["file"] = str(path)
    if check and proc.returncode != 0:
        raise RuntimeError(f"SQL file failed ({proc.returncode}): {record['stderr']}")
    return record


def query_rows(args: argparse.Namespace, database: str, sql: str) -> list[list[str]]:
    record = run_sql(args, database, sql, check=True)
    return [line.split("\t") for line in record["stdout"].splitlines() if line]


def query_scalar(args: argparse.Namespace, database: str, sql: str) -> str:
    rows = query_rows(args, database, sql)
    if not rows or not rows[0]:
        raise RuntimeError(f"query returned no rows: {sql}")
    return rows[0][0]


def create_database(args: argparse.Namespace, database: str) -> dict[str, Any]:
    assert_sandbox_database(database)
    return run_sql(
        args,
        None,
        f"DROP DATABASE IF EXISTS `{database}`; "
        f"CREATE DATABASE `{database}` CHARACTER SET utf8mb4 "
        "COLLATE utf8mb4_0900_ai_ci;",
        check=True,
    )


def drop_database(args: argparse.Namespace, database: str) -> None:
    assert_sandbox_database(database)
    run_sql(args, None, f"DROP DATABASE IF EXISTS `{database}`;", check=True)


def setup_v116_schema(args: argparse.Namespace, database: str) -> dict[str, Any]:
    return run_sql(
        args,
        database,
        "CREATE TABLE `system_users` ("
        "`id` bigint unsigned NOT NULL AUTO_INCREMENT,"
        "`tenant_id` bigint NOT NULL,"
        "`username` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,"
        "`deleted` tinyint NOT NULL DEFAULT 0,"
        "PRIMARY KEY (`id`),"
        "KEY `idx_system_users_username` (`username`)"
        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;",
    )


def setup_v117_schema(args: argparse.Namespace, database: str) -> dict[str, Any]:
    return run_sql(
        args,
        database,
        "CREATE TABLE `crm_receivable` ("
        "`id` bigint unsigned NOT NULL AUTO_INCREMENT,"
        "`tenant_id` bigint NOT NULL,"
        "`deleted` tinyint NOT NULL DEFAULT 0,"
        "PRIMARY KEY (`id`),"
        "KEY `idx_crm_receivable_tenant` (`tenant_id`)"
        ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;",
    )


def table_stats(args: argparse.Namespace, database: str, table: str) -> dict[str, Any]:
    if table not in {"system_users", "crm_receivable"}:
        raise ValueError(f"unsupported table: {table!r}")
    row_count = int(query_scalar(args, database, f"SELECT COUNT(*) FROM `{table}`;"))
    rows = query_rows(
        args,
        database,
        "SELECT COALESCE(data_length, 0), COALESCE(index_length, 0), "
        "COALESCE(table_rows, 0) "
        "FROM information_schema.tables "
        f"WHERE table_schema = DATABASE() AND table_name = '{table}';",
    )
    if not rows or len(rows[0]) != 3:
        raise RuntimeError(f"missing information_schema row for {table}")
    return {
        "row_count": row_count,
        "data_length_bytes": int(rows[0][0]),
        "index_length_bytes": int(rows[0][1]),
        "estimated_rows": int(rows[0][2]),
    }


def index_definition(
    args: argparse.Namespace, database: str, table: str, index_name: str
) -> dict[str, Any]:
    if table not in {"system_users", "crm_receivable"}:
        raise ValueError(f"unsupported table: {table!r}")
    rows = query_rows(
        args,
        database,
        "SELECT GROUP_CONCAT(column_name ORDER BY seq_in_index), "
        "MIN(non_unique), COUNT(*) "
        "FROM information_schema.statistics "
        "WHERE table_schema = DATABASE() "
        f"AND table_name = '{table}' AND index_name = '{index_name}' "
        "GROUP BY index_name;",
    )
    if not rows:
        return {"present": False, "columns": None, "non_unique": None, "column_count": 0}
    row = rows[0]
    return {
        "present": True,
        "columns": row[0],
        "non_unique": int(row[1]),
        "column_count": int(row[2]),
    }


def run_concurrent_sql(
    args: argparse.Namespace, database: str, sql: str
) -> list[dict[str, Any]]:
    procs: list[tuple[int, float, subprocess.Popen[bytes]]] = []
    for slot in (1, 2):
        started = time.perf_counter()
        proc = subprocess.Popen(
            mysql_args(args, database),
            stdin=subprocess.PIPE,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
        )
        procs.append((slot, started, proc))
    for _, _, proc in procs:
        if proc.stdin is None:
            raise RuntimeError("mysql client stdin was not created")
        proc.stdin.write(sql.encode("utf-8"))
        proc.stdin.close()

    records: list[dict[str, Any]] = []
    for slot, started, proc in procs:
        try:
            stdout, stderr = proc.communicate(timeout=args.timeout)
            completed = subprocess.CompletedProcess(
                proc.args, proc.returncode, stdout, stderr
            )
        except subprocess.TimeoutExpired:
            proc.kill()
            stdout, stderr = proc.communicate()
            completed = subprocess.CompletedProcess(proc.args, 124, stdout, stderr)
        records.append(_completed_record(completed, started, slot=slot))
    return records
def assertion(name: str, passed: bool, expected: Any, actual: Any) -> dict[str, Any]:
    return {
        "name": name,
        "passed": bool(passed),
        "expected": expected,
        "actual": actual,
    }


def scenario(
    scenario_id: str,
    purpose: str,
    assertions: list[dict[str, Any]],
    **extra: Any,
) -> dict[str, Any]:
    result = {
        "id": scenario_id,
        "purpose": purpose,
        "passed": bool(assertions) and all(item["passed"] for item in assertions),
        "assertions": assertions,
    }
    result.update(extra)
    return result


def run_v116_concurrent_case(args: argparse.Namespace, database: str) -> dict[str, Any]:
    setup = create_database(args, database)
    schema = setup_v116_schema(args, database)
    migration = run_sql_file(args, database, V116_MIGRATION)
    concurrent = run_concurrent_sql(
        args, database, build_v116_concurrent_sql("v116-race-user", args.sleep_seconds)
    )
    classification = classify_concurrent_results(concurrent)
    active_count = int(
        query_scalar(
            args,
            database,
            "SELECT COUNT(*) FROM `system_users` WHERE `deleted` = 0;",
        )
    )
    checks = [
        assertion("database_created", setup["exit_code"] == 0, 0, setup["exit_code"]),
        assertion("schema_created", schema["exit_code"] == 0, 0, schema["exit_code"]),
        assertion("migration_applied", migration["exit_code"] == 0, 0, migration["exit_code"]),
        assertion("one_committed_writer", classification["success_count"] == 1, 1, classification["success_count"]),
        assertion("one_duplicate_key_loser", classification["duplicate_key_count"] == 1, 1, classification["duplicate_key_count"]),
        assertion("no_other_concurrent_failure", classification["other_failure_count"] == 0, 0, classification["other_failure_count"]),
        assertion("active_row_count", active_count == 1, 1, active_count),
    ]
    return scenario(
        "v116_concurrent_active_username",
        "Two concurrent inserts of the same active tenant username must commit exactly one row and reject the other with MySQL 1062.",
        checks,
        migration=migration,
        concurrent=concurrent,
        classification=classification,
        active_row_count=active_count,
    )


def run_v116_soft_delete_case(args: argparse.Namespace, database: str) -> dict[str, Any]:
    setup = create_database(args, database)
    schema = setup_v116_schema(args, database)
    migration = run_sql_file(args, database, V116_MIGRATION)
    insert = run_sql(
        args,
        database,
        "INSERT INTO `system_users` (`tenant_id`, `username`, `deleted`) "
        "VALUES (1, 'v116-recreate-user', 0);",
    )
    soft_delete = run_sql(
        args,
        database,
        "UPDATE `system_users` SET `deleted` = 1 "
        "WHERE `tenant_id` = 1 AND `username` = 'v116-recreate-user';",
    )
    recreate = run_sql(
        args,
        database,
        "INSERT INTO `system_users` (`tenant_id`, `username`, `deleted`) "
        "VALUES (1, 'v116-recreate-user', 0);",
    )
    active_count = int(
        query_scalar(
            args,
            database,
            "SELECT COUNT(*) FROM `system_users` WHERE `deleted` = 0;",
        )
    )
    total_count = int(query_scalar(args, database, "SELECT COUNT(*) FROM `system_users`;"))
    checks = [
        assertion("database_created", setup["exit_code"] == 0, 0, setup["exit_code"]),
        assertion("schema_created", schema["exit_code"] == 0, 0, schema["exit_code"]),
        assertion("migration_applied", migration["exit_code"] == 0, 0, migration["exit_code"]),
        assertion("initial_insert_committed", insert["exit_code"] == 0, 0, insert["exit_code"]),
        assertion("soft_delete_committed", soft_delete["exit_code"] == 0, 0, soft_delete["exit_code"]),
        assertion("recreate_after_soft_delete", recreate["exit_code"] == 0, 0, recreate["exit_code"]),
        assertion("single_active_row", active_count == 1, 1, active_count),
        assertion("historical_row_preserved", total_count == 2, 2, total_count),
    ]
    return scenario(
        "v116_soft_delete_recreate",
        "A soft-deleted username must not block recreation, while the deleted historical row remains present.",
        checks,
        migration=migration,
        insert=insert,
        soft_delete=soft_delete,
        recreate=recreate,
        active_row_count=active_count,
        total_row_count=total_count,
    )


def run_v117_concurrent_case(args: argparse.Namespace, database: str) -> dict[str, Any]:
    setup = create_database(args, database)
    schema = setup_v117_schema(args, database)
    migration = run_sql_file(args, database, V117_MIGRATION)
    concurrent = run_concurrent_sql(
        args, database, build_v117_concurrent_sql("v117-race-key", args.sleep_seconds)
    )
    classification = classify_concurrent_results(concurrent)
    row_count = int(query_scalar(args, database, "SELECT COUNT(*) FROM `crm_receivable`;"))
    checks = [
        assertion("database_created", setup["exit_code"] == 0, 0, setup["exit_code"]),
        assertion("schema_created", schema["exit_code"] == 0, 0, schema["exit_code"]),
        assertion("migration_applied", migration["exit_code"] == 0, 0, migration["exit_code"]),
        assertion("one_committed_writer", classification["success_count"] == 1, 1, classification["success_count"]),
        assertion("one_duplicate_key_loser", classification["duplicate_key_count"] == 1, 1, classification["duplicate_key_count"]),
        assertion("no_other_concurrent_failure", classification["other_failure_count"] == 0, 0, classification["other_failure_count"]),
        assertion("receivable_row_count", row_count == 1, 1, row_count),
    ]
    return scenario(
        "v117_concurrent_idempotency_key",
        "Two concurrent receivables with the same tenant idempotency key must commit exactly one row and reject the other with MySQL 1062.",
        checks,
        migration=migration,
        concurrent=concurrent,
        classification=classification,
        receivable_row_count=row_count,
    )


def run_v117_soft_delete_case(args: argparse.Namespace, database: str) -> dict[str, Any]:
    setup = create_database(args, database)
    schema = setup_v117_schema(args, database)
    migration = run_sql_file(args, database, V117_MIGRATION)
    insert = run_sql(
        args,
        database,
        "INSERT INTO `crm_receivable` "
        "(`tenant_id`, `idempotency_key`, `payload_hash`, `deleted`) "
        "VALUES (1, 'v117-recreate-key', REPEAT('a', 64), 0);",
    )
    soft_delete = run_sql(
        args,
        database,
        "UPDATE `crm_receivable` SET `deleted` = 1 "
        "WHERE `tenant_id` = 1 AND `idempotency_key` = 'v117-recreate-key';",
    )
    recreate = run_sql(
        args,
        database,
        "INSERT INTO `crm_receivable` "
        "(`tenant_id`, `idempotency_key`, `payload_hash`, `deleted`) "
        "VALUES (1, 'v117-recreate-key', REPEAT('b', 64), 0);",
    )
    row_count = int(query_scalar(args, database, "SELECT COUNT(*) FROM `crm_receivable`;"))
    checks = [
        assertion("database_created", setup["exit_code"] == 0, 0, setup["exit_code"]),
        assertion("schema_created", schema["exit_code"] == 0, 0, schema["exit_code"]),
        assertion("migration_applied", migration["exit_code"] == 0, 0, migration["exit_code"]),
        assertion("initial_insert_committed", insert["exit_code"] == 0, 0, insert["exit_code"]),
        assertion("soft_delete_committed", soft_delete["exit_code"] == 0, 0, soft_delete["exit_code"]),
        assertion("recreate_after_soft_delete_rejected", recreate["exit_code"] != 0, "non-zero", recreate["exit_code"]),
        assertion("duplicate_key_error_code", recreate["mysql_error_code"] == 1062, 1062, recreate["mysql_error_code"]),
        assertion("row_count_unchanged", row_count == 1, 1, row_count),
    ]
    return scenario(
        "v117_soft_delete_permanent_key",
        "A soft-deleted receivable idempotency key must remain occupied and reject reuse with MySQL 1062.",
        checks,
        migration=migration,
        insert=insert,
        soft_delete=soft_delete,
        recreate=recreate,
        receivable_row_count=row_count,
    )
def run_ddl_cost_case(args: argparse.Namespace, database: str) -> dict[str, Any]:
    setup = create_database(args, database)
    schema_users = setup_v116_schema(args, database)
    schema_crm = setup_v117_schema(args, database)
    seed_users = run_sql(args, database, build_ddl_seed_sql("system_users", args.rows))
    seed_crm = run_sql(args, database, build_ddl_seed_sql("crm_receivable", args.rows))
    analyze_users_before = run_sql(
        args, database, build_analyze_table_sql("system_users"), check=True
    )
    analyze_crm_before = run_sql(
        args, database, build_analyze_table_sql("crm_receivable"), check=True
    )
    before_users = table_stats(args, database, "system_users")
    before_crm = table_stats(args, database, "crm_receivable")
    migration_v116 = run_sql_file(args, database, V116_MIGRATION)
    duplicate_users = int(
        query_scalar(
            args,
            database,
            "SELECT COUNT(*) FROM ("
            "SELECT 1 FROM `system_users` WHERE `deleted` = 0 "
            "GROUP BY `tenant_id`, `username` HAVING COUNT(*) > 1"
            ") AS duplicates;",
        )
    )
    index_v116 = index_definition(
        args, database, "system_users", "uk_system_users_tenant_active_username"
    )
    migration_v117 = run_sql_file(args, database, V117_MIGRATION)
    duplicate_crm = int(
        query_scalar(
            args,
            database,
            "SELECT COUNT(*) FROM ("
            "SELECT 1 FROM `crm_receivable` WHERE `idempotency_key` IS NOT NULL "
            "GROUP BY `tenant_id`, `idempotency_key` HAVING COUNT(*) > 1"
            ") AS duplicates;",
        )
    )
    index_v117 = index_definition(
        args, database, "crm_receivable", "uk_crm_receivable_idempotency"
    )
    analyze_users_after = run_sql(
        args, database, build_analyze_table_sql("system_users"), check=True
    )
    analyze_crm_after = run_sql(
        args, database, build_analyze_table_sql("crm_receivable"), check=True
    )
    after_users = table_stats(args, database, "system_users")
    after_crm = table_stats(args, database, "crm_receivable")
    checks = [
        assertion("database_created", setup["exit_code"] == 0, 0, setup["exit_code"]),
        assertion("schema_users_created", schema_users["exit_code"] == 0, 0, schema_users["exit_code"]),
        assertion("schema_crm_created", schema_crm["exit_code"] == 0, 0, schema_crm["exit_code"]),
        assertion("users_seeded", seed_users["exit_code"] == 0, 0, seed_users["exit_code"]),
        assertion("crm_seeded", seed_crm["exit_code"] == 0, 0, seed_crm["exit_code"]),
        assertion("analyze_users_before", analyze_users_before["exit_code"] == 0, 0, analyze_users_before["exit_code"]),
        assertion("analyze_crm_before", analyze_crm_before["exit_code"] == 0, 0, analyze_crm_before["exit_code"]),
        assertion("users_row_count_before", before_users["row_count"] == args.rows, args.rows, before_users["row_count"]),
        assertion("crm_row_count_before", before_crm["row_count"] == args.rows, args.rows, before_crm["row_count"]),
        assertion("v116_migration_applied", migration_v116["exit_code"] == 0, 0, migration_v116["exit_code"]),
        assertion("v116_no_duplicates", duplicate_users == 0, 0, duplicate_users),
        assertion("v116_unique_index", index_v116["present"] and index_v116["non_unique"] == 0 and index_v116["column_count"] == 2, True, index_v116),
        assertion("v117_migration_applied", migration_v117["exit_code"] == 0, 0, migration_v117["exit_code"]),
        assertion("v117_no_duplicates", duplicate_crm == 0, 0, duplicate_crm),
        assertion("v117_unique_index", index_v117["present"] and index_v117["non_unique"] == 0 and index_v117["column_count"] == 2, True, index_v117),
        assertion("users_row_count_after", after_users["row_count"] == args.rows, args.rows, after_users["row_count"]),
        assertion("crm_row_count_after", after_crm["row_count"] == args.rows, args.rows, after_crm["row_count"]),
        assertion("analyze_users_after", analyze_users_after["exit_code"] == 0, 0, analyze_users_after["exit_code"]),
        assertion("analyze_crm_after", analyze_crm_after["exit_code"] == 0, 0, analyze_crm_after["exit_code"]),
    ]
    return scenario(
        "synthetic_ddl_cost",
        "Measure V116/V117 DDL on minimal synthetic tables with the requested row count; this is not a production-volume proof.",
        checks,
        rows=args.rows,
        migrations={"v116": migration_v116, "v117": migration_v117},
        analyze={
            "before": {"system_users": analyze_users_before, "crm_receivable": analyze_crm_before},
            "after": {"system_users": analyze_users_after, "crm_receivable": analyze_crm_after},
        },
        table_stats={
            "before": {"system_users": before_users, "crm_receivable": before_crm},
            "after": {"system_users": after_users, "crm_receivable": after_crm},
        },
        duplicate_counts={"v116_active_username": duplicate_users, "v117_idempotency_key": duplicate_crm},
        indexes={"v116": index_v116, "v117": index_v117},
    )


def server_metadata(args: argparse.Namespace) -> dict[str, Any]:
    record = run_sql(
        args,
        None,
        "SELECT VERSION(), @@version_comment, @@sql_mode, "
        "@@transaction_isolation, @@innodb_lock_wait_timeout, "
        "@@log_bin, @@server_id, @@innodb_buffer_pool_size;",
        check=True,
    )
    fields = record["stdout"].strip().split("\t")
    keys = [
        "version",
        "version_comment",
        "sql_mode",
        "transaction_isolation",
        "innodb_lock_wait_timeout",
        "log_bin",
        "server_id",
        "innodb_buffer_pool_size",
    ]
    return {key: (fields[index] if index < len(fields) else None) for index, key in enumerate(keys)}


def contract_metadata() -> dict[str, Any]:
    contract = json.loads(CONTRACT.read_text(encoding="utf-8"))
    entries = {int(item["version"]): item for item in contract.get("migrations", [])}
    checks: list[dict[str, Any]] = []
    for version, migration, verification in (
        (116, V116_MIGRATION, V116_VERIFY),
        (117, V117_MIGRATION, V117_VERIFY),
    ):
        entry = entries.get(version, {})
        actual = {
            "migration_sha256": sha256(migration),
            "verification_sha256": sha256(verification),
        }
        expected = {
            "migration_sha256": entry.get("sha256"),
            "verification_sha256": entry.get("verification_sha256"),
        }
        checks.append(
            {
                "version": version,
                "expected": expected,
                "actual": actual,
                "match": expected == actual,
            }
        )
    return {
        "contract_path": str(CONTRACT),
        "checks": checks,
        "match": all(item["match"] for item in checks),
    }


def run_all(args: argparse.Namespace) -> dict[str, Any]:
    assert_sandbox_database(args.database)
    run_id = datetime.now().strftime("%Y%m%d_%H%M%S") + "_" + uuid.uuid4().hex[:6]
    cases = [
        ("v116_concurrent_active_username", run_v116_concurrent_case),
        ("v116_soft_delete_recreate", run_v116_soft_delete_case),
        ("v117_concurrent_idempotency_key", run_v117_concurrent_case),
        ("v117_soft_delete_permanent_key", run_v117_soft_delete_case),
        ("synthetic_ddl_cost", run_ddl_cost_case),
    ]
    scenarios: list[dict[str, Any]] = []
    for index, (scenario_id, case) in enumerate(cases, start=1):
        database = f"{args.database}_{run_id}_{index:02d}"
        try:
            result = case(args, database)
        except Exception as exc:  # keep evidence complete even when one case fails
            result = {
                "id": scenario_id,
                "purpose": "Case failed before assertions were complete.",
                "passed": False,
                "assertions": [],
                "error": f"{type(exc).__name__}: {exc}",
            }
        if args.keep_databases:
            result["database_kept"] = database
        else:
            try:
                drop_database(args, database)
                result.setdefault("assertions", []).append(
                    assertion("sandbox_database_cleaned", True, True, True)
                )
            except Exception as exc:
                result.setdefault("assertions", []).append(
                    assertion("sandbox_database_cleaned", False, True, f"{type(exc).__name__}: {exc}")
                )
                result["passed"] = False
        scenarios.append(result)

    artifacts = {
        "v116_migration": str(V116_MIGRATION),
        "v116_migration_sha256": sha256(V116_MIGRATION),
        "v117_migration": str(V117_MIGRATION),
        "v117_migration_sha256": sha256(V117_MIGRATION),
        "v116_verification": str(V116_VERIFY),
        "v116_verification_sha256": sha256(V116_VERIFY),
        "v117_verification": str(V117_VERIFY),
        "v117_verification_sha256": sha256(V117_VERIFY),
        "contract": contract_metadata(),
    }
    return build_evidence(server_metadata(args), artifacts, scenarios, caveats=CAVEATS)
def parse_args(argv: list[str] | None = None) -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Verify V116/V117 constraint semantics on isolated native MySQL"
    )
    parser.add_argument("--mysql-client", required=True)
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=53306)
    parser.add_argument("--user", default="root")
    parser.add_argument("--password", default="")
    parser.add_argument("--database", default=DEFAULT_DATABASE)
    parser.add_argument("--rows", type=int, default=DEFAULT_ROWS)
    parser.add_argument("--sleep-seconds", type=int, default=2)
    parser.add_argument("--timeout", type=int, default=600)
    parser.add_argument("--output", default="")
    parser.add_argument("--keep-databases", action="store_true")
    return parser.parse_args(argv)


def default_output_path() -> Path:
    stamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    return DEFAULT_OUTPUT_DIR / f"constraint-semantics-mysql-sandbox-{stamp}.json"


def main(argv: list[str] | None = None) -> int:
    args = parse_args(argv)
    try:
        evidence = run_all(args)
    except Exception as exc:
        evidence = build_evidence(
            {},
            {},
            [],
            caveats=CAVEATS
            + [f"verifier failed before scenarios completed: {type(exc).__name__}: {exc}"],
        )
    output = Path(args.output) if args.output else default_output_path()
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(evidence, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    digest = sha256(output)
    sidecar = output.with_name(output.name + ".sha256")
    sidecar.write_text(f"{digest}  {output.name}\n", encoding="utf-8")
    print(
        json.dumps(
            {
                "status": evidence.get("status"),
                "summary": evidence.get("summary"),
                "output": str(output),
                "sha256": digest,
            },
            ensure_ascii=False,
            indent=2,
        )
    )
    return 0 if evidence.get("status") == "passed" else 1


if __name__ == "__main__":
    sys.exit(main())