#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""V90/V91/V92 影子库执行与前后核验（Phase 1 / P0-1）。

定位：把“迁移只做了静态契约检查”推进为“在真实 MySQL 上真的跑过一次”。

工作方式：
  1) 读取一次性 MySQL 沙箱 state（mysql-sandbox.ps1 start 产出）；
  2) 用 mysqldump --no-data 把源库（沙箱内的 zhicloud_prod）克隆成影子库；
  3) 在影子库上顺序执行 V90 / V91 / V92；
  4) 执行前后各取一次 information_schema 全量列元数据，对比三组目标：
       - V90：DO 有而 DDL 缺的 71 个列
       - V91：25 个“幽灵 NOT NULL 且无默认”的写入阻断列
       - V92：95 张缺 AUTO_INCREMENT 的表
  5) 顺序跑 3 个只读核验 SQL，原样保存输出；
  6) 产出 JSON 证据。

安全边界：
  * 只在 sandbox state 指定的容器里运行；容器名必须以 zhicloud-mysql-sandbox 开头；
  * 只写影子库（zhicloud_shadow_*），源库只读；
  * 库里只有 schema，没有业务数据量，因此本证据不能代表生产迁移耗时。
"""
from __future__ import annotations

import argparse
import json
import os
import subprocess
import sys
import time
from datetime import datetime
from pathlib import Path
from typing import Any

HERE = Path(__file__).resolve().parent
REPO_ROOT = HERE.parents[1]          # <repo>/scripts/production -> <repo>
WORKSPACE_ROOT = REPO_ROOT.parent    # zhicloud-audit-src2 的同级工作区
DEFAULT_UPGRADE_DIR = WORKSPACE_ROOT / "script" / "enterprise-platform-upgrade"
DEFAULT_EVIDENCE_DIR = WORKSPACE_ROOT / "docs" / "enterprise-platform" / "evidence"
DEFAULT_OUTPUT = DEFAULT_EVIDENCE_DIR / "production" / "migration-shadow-run.json"

SANDBOX_PREFIX = "zhicloud-mysql-sandbox"
SHADOW_PREFIX = "zhicloud_shadow"
MIGRATIONS = ["V90__fix_erp_bill_master_columns.sql",
              "V91__unblock_erp_phantom_not_null_columns.sql",
              "V92__ensure_id_auto_increment.sql"]
VERIFY_SQL = ["V90_verify.sql", "V91_verify.sql", "V92_verify.sql"]

MAX_OUTPUT_CHARS = 8000


# --------------------------------------------------------------------------
# docker / mysql 原语
# --------------------------------------------------------------------------
def run_docker(args: list[str], stdin_bytes: bytes | None = None,
               timeout: int = 1800, env: dict[str, str] | None = None) -> dict[str, Any]:
    started = time.perf_counter()
    proc = subprocess.run(
        ["docker", *args],
        input=stdin_bytes,
        stdout=subprocess.PIPE,
        stderr=subprocess.PIPE,
        timeout=timeout,
        env=env,
    )
    return {
        "argv": ["docker", *args[:6]],
        "exit_code": proc.returncode,
        "stdout": proc.stdout.decode("utf-8", "replace"),
        "stderr": proc.stderr.decode("utf-8", "replace"),
        "duration_s": round(time.perf_counter() - started, 3),
    }


def mysql_exec(state: dict[str, Any], sql: str, database: str | None = None,
               timeout: int = 1800) -> dict[str, Any]:
    # 口令只进子进程环境：本函数的返回值会被整体写进证据 JSON，而 "argv" 记录取的是 args 前 6 项，
    # 也就是 `-e MYSQL_PWD=<值>` 恰好落在被记录的那一段里（R-041/R-044）。
    cmd = ["exec", "-i", "-e", "MYSQL_PWD",
           state["container_name"], "mysql", "-uroot",
           "--default-character-set=utf8mb4", "-B", "-N", "--raw"]
    if database:
        cmd += ["--database", database]
    return run_docker(cmd, stdin_bytes=sql.encode("utf-8"), timeout=timeout,
                      env={**os.environ, "MYSQL_PWD": state["root_password"]})


def mysql_pipe(state: dict[str, Any], shell_cmd: str,
               timeout: int = 1800) -> dict[str, Any]:
    cmd = ["exec", "-e", "MYSQL_PWD",
           state["container_name"], "sh", "-c", shell_cmd]
    return run_docker(cmd, timeout=timeout,
                      env={**os.environ, "MYSQL_PWD": state["root_password"]})


def parse_tsv(stdout: str) -> list[list[str]]:
    """解析 mysql -B -N --raw 的输出：行内以 TAB 分隔。"""
    rows: list[list[str]] = []
    for line in stdout.splitlines():
        if line == "":
            continue
        rows.append(line.split("\t"))
    return rows


# --------------------------------------------------------------------------
# 目标集合（来自静态 evidence）
# --------------------------------------------------------------------------
def load_targets(evidence_dir: Path) -> dict[str, Any]:
    drift = json.loads((evidence_dir / "do_ddl_drift.json").read_text(encoding="utf-8"))
    blockers = json.loads((evidence_dir / "insert_blockers.json").read_text(encoding="utf-8"))
    auto = json.loads((evidence_dir / "id_auto_increment_audit.json").read_text(encoding="utf-8"))

    missing: list[dict[str, str]] = []
    for table, info in sorted(drift.get("tables", {}).items()):
        for item in info.get("missing_in_ddl", []) or []:
            column = item["column"] if isinstance(item, dict) else item
            missing.append({"table": table, "column": column})

    block = [{"table": b["table"], "column": b["column"]}
             for b in blockers.get("blockers", [])]
    autolist = [{"table": t["table"]} for t in auto.get("tables", [])]
    return {
        "v90_missing_columns": missing,
        "v91_blockers": block,
        "v92_auto_tables": autolist,
        "source_files": ["do_ddl_drift.json", "insert_blockers.json",
                         "id_auto_increment_audit.json"],
    }


# --------------------------------------------------------------------------
# information_schema 探针
# --------------------------------------------------------------------------
PROBE_SQL = (
    "SELECT TABLE_NAME, COLUMN_NAME, IS_NULLABLE, "
    "IFNULL(COLUMN_DEFAULT, '<NULL>'), IFNULL(EXTRA, ''), COLUMN_TYPE "
    "FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='{schema}' "
    "ORDER BY TABLE_NAME, COLUMN_NAME;"
)


def probe_columns(state: dict[str, Any], schema: str) -> dict[tuple[str, str], dict[str, str]]:
    res = mysql_exec(state, PROBE_SQL.format(schema=schema))
    if res["exit_code"] != 0:
        raise RuntimeError(f"probe failed: {res['stderr'][:400]}")
    out: dict[tuple[str, str], dict[str, str]] = {}
    for row in parse_tsv(res["stdout"]):
        if len(row) < 6:
            continue
        table, column, nullable, default, extra, coltype = row[:6]
        out[(table, column)] = {
            "is_nullable": nullable,
            "column_default": default,
            "extra": extra,
            "column_type": coltype,
        }
    return out


def evaluate(columns: dict[tuple[str, str], dict[str, str]],
             targets: dict[str, Any]) -> dict[str, Any]:
    def missing_columns() -> list[str]:
        bad = []
        for item in targets["v90_missing_columns"]:
            key = (item["table"], item["column"])
            if key not in columns:
                bad.append(f"{item['table']}.{item['column']}")
        return bad

    def blocking() -> list[str]:
        bad = []
        for item in targets["v91_blockers"]:
            meta = columns.get((item["table"], item["column"]))
            if meta is None:
                continue  # 列已不存在，不再阻断写入
            if (meta["is_nullable"] == "NO"
                    and meta["column_default"] == "<NULL>"
                    and "auto_increment" not in meta["extra"].lower()
                    and "DEFAULT_GENERATED" not in meta["extra"]):
                bad.append(f"{item['table']}.{item['column']}")
        return bad

    def no_auto() -> list[str]:
        bad = []
        for item in targets["v92_auto_tables"]:
            meta = columns.get((item["table"], "id"))
            if meta is None:
                bad.append(f"{item['table']}.id(missing_table)")
            elif "auto_increment" not in meta["extra"].lower():
                bad.append(f"{item['table']}.id")
        return bad

    return {
        "v90_missing_after": missing_columns(),
        "v91_blocking_after": blocking(),
        "v92_no_auto_after": no_auto(),
    }


def count_tables(state: dict[str, Any], schema: str) -> int:
    res = mysql_exec(state, f"SELECT COUNT(*) FROM information_schema.TABLES "
                            f"WHERE TABLE_SCHEMA='{schema}' AND TABLE_TYPE='BASE TABLE';")
    if res["exit_code"] != 0 or not res["stdout"].strip():
        return -1
    return int(res["stdout"].strip().splitlines()[0])


# --------------------------------------------------------------------------
# 主流程
# --------------------------------------------------------------------------
def build_plan(state: dict[str, Any], shadow: str, source: str,
               mig_dir: Path, ver_dir: Path) -> list[str]:
    return [
        f"container: {state['container_name']} ({state['image']})",
        f"clone: {source} --no-data--> {shadow}",
        *(f"apply: {mig_dir / m}" for m in MIGRATIONS),
        *(f"verify: {ver_dir / v}" for v in VERIFY_SQL),
        f"drop: {shadow}",
    ]


def main(argv: list[str] | None = None) -> int:
    ap = argparse.ArgumentParser(description="V90/V91/V92 影子库执行与核验")
    ap.add_argument("--sandbox-state", required=True, type=Path)
    ap.add_argument("--source-database", default="zhicloud_prod")
    ap.add_argument("--shadow-database", default="")
    ap.add_argument("--migrations-dir", type=Path, default=DEFAULT_UPGRADE_DIR / "migrations")
    ap.add_argument("--verification-dir", type=Path, default=DEFAULT_UPGRADE_DIR / "verification")
    ap.add_argument("--evidence-dir", type=Path, default=DEFAULT_EVIDENCE_DIR)
    ap.add_argument("--output", type=Path, default=DEFAULT_OUTPUT)
    ap.add_argument("--keep-shadow", action="store_true", help="保留影子库用于人工复查")
    ap.add_argument("--rounds", type=int, default=1,
                    help="迁移回放轮次；>1 用于验证重复执行幂等（Flyway 失败重试场景）")
    ap.add_argument("--dry-run", action="store_true")
    args = ap.parse_args(argv)

    state = json.loads(args.sandbox_state.read_text(encoding="utf-8"))
    if not str(state.get("container_name", "")).startswith(SANDBOX_PREFIX):
        print(f"[FAIL] 容器名 {state.get('container_name')} 不是一次性沙箱（前缀 {SANDBOX_PREFIX}）")
        return 2
    shadow = args.shadow_database or f"{SHADOW_PREFIX}_{time.strftime('%Y%m%d%H%M%S')}"
    if not shadow.startswith(SHADOW_PREFIX):
        print(f"[FAIL] 影子库名必须以 {SHADOW_PREFIX} 开头")
        return 2

    if args.dry_run:
        for line in build_plan(state, shadow, args.source_database,
                               args.migrations_dir, args.verification_dir):
            print(line)
        return 0

    started = time.perf_counter()
    report: dict[str, Any] = {
        "generated_at": datetime.now().astimezone().isoformat(timespec="seconds"),
        "disclaimer": "sandbox-only; schema-only database; not a production migration run",
        "sandbox": {
            "container_name": state["container_name"],
            "image": state["image"],
            "database": state.get("database"),
            "host_port": state.get("host_port"),
        },
        "source_database": args.source_database,
        "shadow_database": shadow,
        "targets": load_targets(args.evidence_dir),
        "steps": {},
    }

    ver = mysql_exec(state, "SELECT VERSION();")
    report["steps"]["mysql_version"] = {
        "version": ver["stdout"].strip(), "exit_code": ver["exit_code"]}
    if ver["exit_code"] != 0:
        print("[FAIL] 无法连接沙箱 MySQL"); print(ver["stderr"][:400]); return 1

    src_tables = count_tables(state, args.source_database)
    report["steps"]["source_table_count"] = src_tables

    create = mysql_exec(state, f"CREATE DATABASE IF NOT EXISTS `{shadow}` "
                              f"CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;")
    report["steps"]["create_shadow"] = {
        "exit_code": create["exit_code"], "stderr": create["stderr"][:400]}
    if create["exit_code"] != 0:
        print("[FAIL] 影子库创建失败"); print(create["stderr"][:400]); return 1

    clone = mysql_pipe(state,
                       f"mysqldump -uroot --no-data --skip-lock-tables --routines "
                       f"--triggers --events {args.source_database} "
                       f"| mysql -uroot --database={shadow}",
                       timeout=1800)
    report["steps"]["clone_schema"] = clone
    if clone["exit_code"] != 0:
        print("[FAIL] 影子库克隆失败"); print(clone["stderr"][:800]); return 1

    shadow_tables = count_tables(state, shadow)
    report["steps"]["shadow_table_count_after_clone"] = shadow_tables

    before = probe_columns(state, shadow)
    report["before"] = evaluate(before, report["targets"])
    report["before"]["column_count"] = len(before)

    applied: list[dict[str, Any]] = []
    for round_no in range(1, max(1, args.rounds) + 1):
        for name in MIGRATIONS:
            path = args.migrations_dir / name
            if not path.is_file():
                applied.append({"round": round_no, "migration": name,
                                "status": "missing_file"})
                continue
            res = mysql_exec(state, path.read_text(encoding="utf-8"),
                             database=shadow, timeout=3600)
            applied.append({
                "round": round_no,
                "migration": name,
                "exit_code": res["exit_code"],
                "status": "ok" if res["exit_code"] == 0 else "failed",
                "duration_s": res["duration_s"],
                "stdout": res["stdout"][:MAX_OUTPUT_CHARS],
                "stderr": res["stderr"][:MAX_OUTPUT_CHARS],
            })
    report["steps"]["migrations"] = applied
    report["rounds"] = max(1, args.rounds)

    after = probe_columns(state, shadow)
    report["after"] = evaluate(after, report["targets"])
    report["after"]["column_count"] = len(after)
    report["shadow_table_count_after_migration"] = count_tables(state, shadow)

    verify_runs = []
    for name in VERIFY_SQL:
        path = args.verification_dir / name
        if not path.is_file():
            verify_runs.append({"sql": name, "status": "missing_file"})
            continue
        res = mysql_exec(state, path.read_text(encoding="utf-8"), database=shadow,
                         timeout=1800)
        verify_runs.append({
            "sql": name,
            "exit_code": res["exit_code"],
            "status": "ok" if res["exit_code"] == 0 else "failed",
            "duration_s": res["duration_s"],
            "row_count": len(parse_tsv(res["stdout"])),
            "stdout": res["stdout"][:MAX_OUTPUT_CHARS],
            "stderr": res["stderr"][:MAX_OUTPUT_CHARS],
        })
    report["steps"]["verification_sql"] = verify_runs

    b, a = report["before"], report["after"]
    report["delta"] = {
        "v90_missing": {"before": len(b["v90_missing_after"]),
                        "after": len(a["v90_missing_after"]),
                        "remaining": a["v90_missing_after"][:50]},
        "v91_blocking": {"before": len(b["v91_blocking_after"]),
                         "after": len(a["v91_blocking_after"]),
                         "remaining": a["v91_blocking_after"][:50]},
        "v92_no_auto": {"before": len(b["v92_no_auto_after"]),
                        "after": len(a["v92_no_auto_after"]),
                        "remaining": a["v92_no_auto_after"][:50]},
        "table_count": {"source": src_tables,
                        "after_clone": shadow_tables,
                        "after_migration": report["shadow_table_count_after_migration"]},
    }

    migrations_ok = all(m.get("status") == "ok" for m in applied)
    verify_ok = all(v.get("status") == "ok" for v in verify_runs)
    report["status"] = "passed" if (migrations_ok and verify_ok) else "failed"
    report["total_duration_s"] = round(time.perf_counter() - started, 3)

    if not args.keep_shadow:
        drop = mysql_exec(state, f"DROP DATABASE IF EXISTS `{shadow}`;")
        report["steps"]["drop_shadow"] = {"exit_code": drop["exit_code"],
                                          "stderr": drop["stderr"][:800]}
    else:
        report["steps"]["drop_shadow"] = {"skipped": "keep-shadow"}

    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(report, ensure_ascii=False, indent=2,
                                      sort_keys=True) + "\n",
                           encoding="utf-8", newline="\n")
    print(f"[{'PASS' if report['status'] == 'passed' else 'FAIL'}] V90/V91/V92 影子库执行：{report['status']}")
    print(f"  V90 缺列 {report['delta']['v90_missing']['before']} -> {report['delta']['v90_missing']['after']}")
    print(f"  V91 阻断 {report['delta']['v91_blocking']['before']} -> {report['delta']['v91_blocking']['after']}")
    print(f"  V92 缺自增 {report['delta']['v92_no_auto']['before']} -> {report['delta']['v92_no_auto']['after']}")
    print(f"  证据: {args.output}")
    return 0 if report["status"] == "passed" else 1


if __name__ == "__main__":
    raise SystemExit(main())
