#!/usr/bin/env python3
"""Run V107 negative tests against an isolated native MySQL sandbox.

This verifier is intentionally fail-closed. It only operates on databases whose
name starts with ``zhicloud_sandbox`` and writes machine-readable evidence that
is explicitly forbidden from production import.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import re
import subprocess
import sys
import time
from datetime import datetime
from pathlib import Path
from typing import Any

REPO_ROOT = Path(__file__).resolve().parents[2]
DEFAULT_MIGRATION = REPO_ROOT / "zhicloud-server/src/main/resources/db/migration/V107__event_tenant_unique_keys.sql"
DEFAULT_VERIFY = REPO_ROOT / "zhicloud-server/src/main/resources/db/verification/V107_verify.sql"
V100_MIGRATION = REPO_ROOT / "zhicloud-server/src/main/resources/db/migration/V100__event_outbox_inbox.sql"
CONTRACT = REPO_ROOT / "scripts/production/migration-contract.json"
DEFAULT_OUTPUT = REPO_ROOT.parent / "docs/enterprise-platform/evidence/production/v107-negative-mysql-sandbox.json"
SAFE_DB_RE = re.compile(r"^zhicloud_sandbox[A-Za-z0-9_]+$")
DISCLAIMER = "FAKE-SIMULATED-DATA: isolated schema/data only; production_import_allowed=false"
MAX_TEXT = 12000


def assert_sandbox_database(name: str) -> None:
    """Reject any database outside the sandbox namespace."""
    if not name or not SAFE_DB_RE.fullmatch(name):
        raise ValueError(f"unsafe database name: {name!r}")


def contains_expected_error(text: str, fragment: str) -> bool:
    return fragment in (text or "")


def sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def build_evidence(
    server: dict[str, Any],
    artifacts: dict[str, Any],
    scenarios: list[dict[str, Any]],
    *,
    contract_match: bool = True,
) -> dict[str, Any]:
    failed = sum(1 for scenario in scenarios if not scenario.get("passed"))
    status = "passed" if scenarios and failed == 0 and contract_match else "failed"
    return {
        "generated_at": datetime.now().astimezone().isoformat(),
        "disclaimer": DISCLAIMER,
        "production_import_allowed": False,
        "scope": (
            "V107 negative behavior only: duplicate data, wrong indexes and missing "
            "tenant columns must block migration; cross-tenant equal event IDs must coexist."
        ),
        "evidence_boundaries": {
            "setup_scope": "V100 event_outbox/event_inbox schema plus V107 focused migration",
            "execution_path": "native mysql 8.4.9 client",
            "flyway_parser_covered": False,
            "warning": "This evidence does not replace a full Flyway chain run on the pinned production versions.",
        },
        "server": server,
        "artifacts": artifacts,
        "contract_match": contract_match,
        "scenarios": scenarios,
        "summary": {
            "total": len(scenarios),
            "passed": len(scenarios) - failed,
            "failed": failed,
        },
        "status": status,
    }


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
    ]
    if args.password:
        command.append(f"--password={args.password}")
    if database:
        assert_sandbox_database(database)
        command.append(database)
    return command


def _completed_record(proc: subprocess.CompletedProcess[bytes], started: float) -> dict[str, Any]:
    return {
        "exit_code": proc.returncode,
        "duration_s": round(time.perf_counter() - started, 3),
        "stdout": proc.stdout.decode("utf-8", "replace")[:MAX_TEXT],
        "stderr": proc.stderr.decode("utf-8", "replace")[:MAX_TEXT],
    }


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
        f"DROP DATABASE IF EXISTS `{database}`; CREATE DATABASE `{database}` "
        "CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;",
        check=True,
    )


def drop_database(args: argparse.Namespace, database: str) -> None:
    assert_sandbox_database(database)
    run_sql(args, None, f"DROP DATABASE IF EXISTS `{database}`;", check=True)


def indexes(args: argparse.Namespace, database: str, table: str) -> dict[str, dict[str, Any]]:
    rows = query_rows(
        args,
        database,
        "SELECT index_name, MIN(non_unique), "
        "GROUP_CONCAT(column_name ORDER BY seq_in_index) "
        "FROM information_schema.statistics "
        f"WHERE table_schema = DATABASE() AND table_name = '{table}' "
        "GROUP BY index_name ORDER BY index_name;",
    )
    result: dict[str, dict[str, Any]] = {}
    for row in rows:
        if len(row) != 3:
            continue
        result[row[0]] = {"non_unique": int(row[1]), "columns": row[2]}
    return result


def assertion(name: str, passed: bool, expected: Any, actual: Any) -> dict[str, Any]:
    return {
        "name": name,
        "passed": bool(passed),
        "expected": expected,
        "actual": actual,
    }


def finish_scenario(
    scenario_id: str,
    purpose: str,
    assertions: list[dict[str, Any]],
    *,
    migration: dict[str, Any],
    notes: list[str] | None = None,
) -> dict[str, Any]:
    return {
        "id": scenario_id,
        "purpose": purpose,
        "passed": all(item["passed"] for item in assertions),
        "assertions": assertions,
        "migration": migration,
        "notes": notes or [],
    }


def setup_v100(args: argparse.Namespace, database: str) -> dict[str, Any]:
    create_database(args, database)
    return run_sql_file(args, database, V100_MIGRATION, check=True)


def run_outbox_duplicate_case(args: argparse.Namespace, database: str) -> dict[str, Any]:
    setup = setup_v100(args, database)
    drop_legacy = run_sql(args, database, "ALTER TABLE event_outbox DROP INDEX uk_event_id;", check=True)
    seed = run_sql(
        args,
        database,
        "INSERT INTO event_outbox "
        "(event_id, tenant_id, aggregate_type, aggregate_id, event_type, payload) VALUES "
        "('v107-dup-outbox', 1, 'test', '1', 'test.event.v1', '{}'),"
        "('v107-dup-outbox', 1, 'test', '2', 'test.event.v1', '{}');",
        check=True,
    )
    state_before_migration = indexes(args, database, "event_outbox")
    migration = run_sql_file(args, database, DEFAULT_MIGRATION)
    state = indexes(args, database, "event_outbox")
    expected = "V107 duplicate rows block unique index uk_event_tenant_event on event_outbox"
    checks = [
        assertion("setup_legacy_index_removed_for_duplicate_simulation", "uk_event_id" not in state_before_migration, True, "uk_event_id" in state_before_migration),
        assertion("duplicate_seed_inserted", seed["exit_code"] == 0, 0, seed["exit_code"]),
        assertion("migration_blocked", migration["exit_code"] != 0, "non-zero", migration["exit_code"]),
        assertion("expected_signal", contains_expected_error(migration["stderr"], expected), expected, migration["stderr"]),
        assertion("new_index_not_created", "uk_event_tenant_event" not in state, True, "uk_event_tenant_event" in state),
    ]
    return finish_scenario(
        "outbox_same_tenant_duplicate_blocked",
        "Existing duplicate outbox rows must fail closed before the new unique key is created.",
        checks,
        migration=migration,
        notes=[f"setup_v100_exit={setup['exit_code']}", f"drop_legacy_exit={drop_legacy['exit_code']}"],
    )


def run_inbox_duplicate_case(args: argparse.Namespace, database: str) -> dict[str, Any]:
    setup = setup_v100(args, database)
    drop_legacy = run_sql(args, database, "ALTER TABLE event_inbox DROP INDEX uk_consumer_event;", check=True)
    seed = run_sql(
        args,
        database,
        "INSERT INTO event_inbox "
        "(tenant_id, consumer_name, event_id, event_type) VALUES "
        "(1, 'v107-consumer', 'v107-dup-inbox', 'test.event.v1'),"
        "(1, 'v107-consumer', 'v107-dup-inbox', 'test.event.v1');",
        check=True,
    )
    inbox_state_before_migration = indexes(args, database, "event_inbox")
    migration = run_sql_file(args, database, DEFAULT_MIGRATION)
    outbox_state = indexes(args, database, "event_outbox")
    inbox_state = indexes(args, database, "event_inbox")
    expected = "V107 duplicate rows block unique index uk_consumer_tenant_event on event_inbox"
    checks = [
        assertion("setup_legacy_index_removed_for_duplicate_simulation", "uk_consumer_event" not in inbox_state_before_migration, True, "uk_consumer_event" in inbox_state_before_migration),
        assertion("duplicate_seed_inserted", seed["exit_code"] == 0, 0, seed["exit_code"]),
        assertion("migration_blocked", migration["exit_code"] != 0, "non-zero", migration["exit_code"]),
        assertion("expected_signal", contains_expected_error(migration["stderr"], expected), expected, migration["stderr"]),
        assertion("new_inbox_index_not_created", "uk_consumer_tenant_event" not in inbox_state, True, "uk_consumer_tenant_event" in inbox_state),
        assertion("outbox_partial_ddl_visible", "uk_event_tenant_event" in outbox_state, True, "uk_event_tenant_event" in outbox_state),
    ]
    return finish_scenario(
        "inbox_same_tenant_consumer_duplicate_blocked",
        "Existing duplicate inbox rows must fail closed and leave no new inbox unique key.",
        checks,
        migration=migration,
        notes=[
            f"setup_v100_exit={setup['exit_code']}",
            f"drop_legacy_exit={drop_legacy['exit_code']}",
            "MySQL DDL is non-transactional; the already-created outbox index is expected partial progress and makes retry idempotent.",
        ],
    )


def run_wrong_index_case(args: argparse.Namespace, database: str) -> dict[str, Any]:
    setup = setup_v100(args, database)
    setup_index = run_sql(
        args,
        database,
        "ALTER TABLE event_outbox ADD UNIQUE KEY uk_event_tenant_event (event_id, tenant_id);",
        check=True,
    )
    migration = run_sql_file(args, database, DEFAULT_MIGRATION)
    state = indexes(args, database, "event_outbox")
    expected = "V107 index mismatch: event_outbox.uk_event_tenant_event expected=tenant_id,event_id actual=event_id,tenant_id"
    checks = [
        assertion("migration_blocked", migration["exit_code"] != 0, "non-zero", migration["exit_code"]),
        assertion("expected_signal", contains_expected_error(migration["stderr"], expected), expected, migration["stderr"]),
        assertion("wrong_index_not_silently_replaced", state.get("uk_event_tenant_event", {}).get("columns") == "event_id,tenant_id", "event_id,tenant_id", state.get("uk_event_tenant_event")),
        assertion("legacy_index_preserved", "uk_event_id" in state, True, "uk_event_id" in state),
    ]
    return finish_scenario(
        "wrong_unique_column_order_blocked",
        "An existing same-name unique index with the wrong column order must block migration.",
        checks,
        migration=migration,
        notes=[f"setup_v100_exit={setup['exit_code']}", f"setup_index_exit={setup_index['exit_code']}"],
    )


def run_non_unique_index_case(args: argparse.Namespace, database: str) -> dict[str, Any]:
    setup = setup_v100(args, database)
    setup_index = run_sql(
        args,
        database,
        "ALTER TABLE event_outbox ADD KEY uk_event_tenant_event (tenant_id, event_id);",
        check=True,
    )
    migration = run_sql_file(args, database, DEFAULT_MIGRATION)
    state = indexes(args, database, "event_outbox")
    expected = "V107 index mismatch: event_outbox.uk_event_tenant_event expected=tenant_id,event_id actual=tenant_id,event_id"
    checks = [
        assertion("migration_blocked", migration["exit_code"] != 0, "non-zero", migration["exit_code"]),
        assertion("expected_signal", contains_expected_error(migration["stderr"], expected), expected, migration["stderr"]),
        assertion("non_unique_index_not_silently_accepted", state.get("uk_event_tenant_event", {}).get("non_unique") == 1, 1, state.get("uk_event_tenant_event", {}).get("non_unique")),
        assertion("legacy_index_preserved", "uk_event_id" in state, True, "uk_event_id" in state),
    ]
    return finish_scenario(
        "non_unique_index_blocked",
        "An existing same-name non-unique index must block migration.",
        checks,
        migration=migration,
        notes=[f"setup_v100_exit={setup['exit_code']}", f"setup_index_exit={setup_index['exit_code']}"],
    )


def run_missing_column_case(args: argparse.Namespace, database: str) -> dict[str, Any]:
    setup = setup_v100(args, database)
    setup_change = run_sql(
        args,
        database,
        "ALTER TABLE event_outbox DROP INDEX idx_tenant_aggregate; "
        "ALTER TABLE event_outbox DROP COLUMN tenant_id;",
        check=True,
    )
    migration = run_sql_file(args, database, DEFAULT_MIGRATION)
    column_count = int(query_scalar(
        args,
        database,
        "SELECT COUNT(*) FROM information_schema.columns "
        "WHERE table_schema = DATABASE() AND table_name = 'event_outbox' AND column_name = 'tenant_id';",
    ))
    expected = "V107 column missing: event_outbox.tenant_id"
    checks = [
        assertion("migration_blocked", migration["exit_code"] != 0, "non-zero", migration["exit_code"]),
        assertion("expected_signal", contains_expected_error(migration["stderr"], expected), expected, migration["stderr"]),
        assertion("missing_column_confirmed", column_count == 0, 0, column_count),
    ]
    return finish_scenario(
        "missing_tenant_column_blocked",
        "A missing tenant_id column must block migration before any index is changed.",
        checks,
        migration=migration,
        notes=[f"setup_v100_exit={setup['exit_code']}", f"setup_change_exit={setup_change['exit_code']}"],
    )


def run_positive_tenant_boundary_case(args: argparse.Namespace, database: str) -> dict[str, Any]:
    setup = setup_v100(args, database)
    first = run_sql_file(args, database, DEFAULT_MIGRATION)
    second = run_sql_file(args, database, DEFAULT_MIGRATION)
    verify = run_sql_file(args, database, DEFAULT_VERIFY)

    cross_tenant = run_sql(
        args,
        database,
        "INSERT INTO event_outbox "
        "(event_id, tenant_id, aggregate_type, aggregate_id, event_type, payload) VALUES "
        "('v107-shared-id', 1, 'test', '1', 'test.event.v1', '{}'),"
        "('v107-shared-id', 2, 'test', '2', 'test.event.v1', '{}');"
        "INSERT INTO event_inbox (tenant_id, consumer_name, event_id, event_type) VALUES "
        "(1, 'v107-consumer', 'v107-shared-id', 'test.event.v1'),"
        "(2, 'v107-consumer', 'v107-shared-id', 'test.event.v1');",
        check=True,
    )
    duplicate_outbox = run_sql(
        args,
        database,
        "INSERT INTO event_outbox "
        "(event_id, tenant_id, aggregate_type, aggregate_id, event_type, payload) "
        "VALUES ('v107-shared-id', 1, 'test', '3', 'test.event.v1', '{}');",
    )
    duplicate_inbox = run_sql(
        args,
        database,
        "INSERT INTO event_inbox (tenant_id, consumer_name, event_id, event_type) "
        "VALUES (1, 'v107-consumer', 'v107-shared-id', 'test.event.v1');",
    )
    outbox_state = indexes(args, database, "event_outbox")
    inbox_state = indexes(args, database, "event_inbox")
    checks = [
        assertion("first_migration_succeeded", first["exit_code"] == 0, 0, first["exit_code"]),
        assertion("idempotent_rerun_succeeded", second["exit_code"] == 0, 0, second["exit_code"]),
        assertion("verification_passed", verify["exit_code"] == 0 and re.search(r"(?m)^V107\tPASS\r?$", verify["stdout"]) is not None, "V107=PASS", verify["stdout"]),
        assertion("cross_tenant_equal_event_ids_accepted", cross_tenant["exit_code"] == 0, 0, cross_tenant["exit_code"]),
        assertion("same_tenant_outbox_duplicate_rejected", duplicate_outbox["exit_code"] != 0 and "uk_event_tenant_event" in duplicate_outbox["stderr"], "duplicate key", duplicate_outbox["stderr"]),
        assertion("same_tenant_inbox_duplicate_rejected", duplicate_inbox["exit_code"] != 0 and "uk_consumer_tenant_event" in duplicate_inbox["stderr"], "duplicate key", duplicate_inbox["stderr"]),
        assertion("legacy_outbox_index_removed", "uk_event_id" not in outbox_state, True, "uk_event_id" in outbox_state),
        assertion("legacy_inbox_index_removed", "uk_consumer_event" not in inbox_state, True, "uk_consumer_event" in inbox_state),
        assertion("new_outbox_index_correct", outbox_state.get("uk_event_tenant_event", {}).get("non_unique") == 0 and outbox_state.get("uk_event_tenant_event", {}).get("columns") == "tenant_id,event_id", "UNIQUE tenant_id,event_id", outbox_state.get("uk_event_tenant_event")),
        assertion("new_inbox_index_correct", inbox_state.get("uk_consumer_tenant_event", {}).get("non_unique") == 0 and inbox_state.get("uk_consumer_tenant_event", {}).get("columns") == "tenant_id,consumer_name,event_id", "UNIQUE tenant_id,consumer_name,event_id", inbox_state.get("uk_consumer_tenant_event")),
    ]
    return finish_scenario(
        "cross_tenant_equal_event_ids_and_idempotent_rerun",
        "After migration, equal event IDs in different tenants coexist while same-tenant duplicates are rejected.",
        checks,
        migration=first,
        notes=[f"setup_v100_exit={setup['exit_code']}", f"second_migration_exit={second['exit_code']}", f"verify_exit={verify['exit_code']}"],
    )


def contract_hashes() -> dict[str, Any]:
    data = json.loads(CONTRACT.read_text(encoding="utf-8"))
    entry = next(item for item in data["migrations"] if item["version"] == 107)
    actual = {
        "migration_sha256": sha256(DEFAULT_MIGRATION),
        "verification_sha256": sha256(DEFAULT_VERIFY),
    }
    expected = {
        "migration_sha256": entry["sha256"],
        "verification_sha256": entry["verification_sha256"],
    }
    return {"expected": expected, "actual": actual, "match": expected == actual}


def server_metadata(args: argparse.Namespace) -> dict[str, Any]:
    record = run_sql(args, None, "SELECT VERSION(), @@sql_mode, @@transaction_isolation;", check=True)
    fields = record["stdout"].strip().split("\t")
    return {
        "version": fields[0] if fields else None,
        "sql_mode": fields[1] if len(fields) > 1 else None,
        "transaction_isolation": fields[2] if len(fields) > 2 else None,
    }


def run_all(args: argparse.Namespace) -> dict[str, Any]:
    assert_sandbox_database(args.database)
    scenarios: list[dict[str, Any]] = []
    cases = [
        run_outbox_duplicate_case,
        run_inbox_duplicate_case,
        run_wrong_index_case,
        run_non_unique_index_case,
        run_missing_column_case,
        run_positive_tenant_boundary_case,
    ]
    for index, case in enumerate(cases, start=1):
        database = f"{args.database}_{index:02d}"
        try:
            scenarios.append(case(args, database))
        finally:
            try:
                drop_database(args, database)
            except Exception as exc:  # evidence must still be written
                scenarios.append({
                    "id": f"cleanup_{index:02d}",
                    "purpose": "Drop isolated sandbox database",
                    "passed": False,
                    "assertions": [],
                    "migration": {},
                    "notes": [f"cleanup failed: {exc}"],
                })
    artifacts = {
        "v100_migration": str(V100_MIGRATION),
        "v100_migration_sha256": sha256(V100_MIGRATION),
        "v107_migration": str(DEFAULT_MIGRATION),
        "v107_migration_sha256": sha256(DEFAULT_MIGRATION),
        "v107_verification": str(DEFAULT_VERIFY),
        "v107_verification_sha256": sha256(DEFAULT_VERIFY),
    }
    contract = contract_hashes()
    return build_evidence(server_metadata(args), {**artifacts, "contract": contract}, scenarios, contract_match=contract["match"])


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Run V107 fail-closed negative tests on isolated MySQL")
    parser.add_argument("--mysql-client", required=True)
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=53306)
    parser.add_argument("--user", default="root")
    parser.add_argument("--password", default="")
    parser.add_argument("--database", default="zhicloud_sandbox_v107_negative")
    parser.add_argument("--output", default=str(DEFAULT_OUTPUT))
    parser.add_argument("--timeout", type=int, default=120)
    return parser.parse_args()


def main() -> int:
    args = parse_args()
    try:
        evidence = run_all(args)
    except Exception as exc:
        evidence = {
            "generated_at": datetime.now().astimezone().isoformat(),
            "disclaimer": DISCLAIMER,
            "production_import_allowed": False,
            "scope": "V107 negative behavior only",
            "scenarios": [],
            "summary": {"total": 0, "passed": 0, "failed": 1},
            "status": "failed",
            "error": str(exc),
        }
    output = Path(args.output)
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(evidence, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(evidence, ensure_ascii=False, indent=2))
    return 0 if evidence.get("status") == "passed" else 1


if __name__ == "__main__":
    sys.exit(main())


