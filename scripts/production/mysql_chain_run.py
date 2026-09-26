#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Flyway 迁移链真机执行器（本机 MySQL，不依赖 Docker）。

背景：
  scripts/production 里已有的 mysql 相关脚本（migration_shadow_run.py、
  tests/test_mysql_sandbox.py）全部依赖 docker。当前环境 docker 守护进程无法启动
  （com.docker.service 拒绝启动，WSL 仅有已停止的 docker-desktop），
  因此 V96~V102 一直停留在「静态 + JVM 契约 + 单测」证据，没有真机执行证据。
  本脚本用本机 mysqld（ZIP 版，见 mysql_native_sandbox.md）补上这一层。

做什么：
  1. 在沙箱库（库名必须以 zhicloud_sandbox 开头）上按 Flyway 版本号顺序执行
     zhicloud-server/src/main/resources/db/migration/*.sql；
  2. 遇错即停（与 Flyway 默认行为一致），记录出错文件与 stderr；
  3. 执行指定的 verification/*.sql，原样保留输出；
  4. 落 JSON 证据。

不做什么（安全边界）：
  * 只对 --database 指定的库操作，且库名必须以 zhicloud_sandbox 开头，否则拒绝运行；
  * 不读生产库、不导出业务数据、不做 DATA 变更（除迁移脚本自身外）。

用法：
  python scripts/production/mysql_chain_run.py \
      --mysql-client C:\path\to\mysql.exe --host 127.0.0.1 --port 53306 \
      --database zhicloud_sandbox_chain --verify V101 V102
"""
from __future__ import annotations

import argparse
import json
import re
import subprocess
import sys
import time
from datetime import datetime, timezone
from pathlib import Path

HERE = Path(__file__).resolve().parent
REPO_ROOT = HERE.parents[1]
DEFAULT_MIGRATION_DIR = REPO_ROOT / "zhicloud-server" / "src" / "main" / "resources" / "db" / "migration"
DEFAULT_VERIFY_DIR = REPO_ROOT / "zhicloud-server" / "src" / "main" / "resources" / "db" / "verification"
DEFAULT_OUTPUT = (REPO_ROOT.parent / "docs" / "enterprise-platform" / "evidence" / "production"
                  / "mysql-chain-run.json")

SAFE_DB_PREFIX = "zhicloud_sandbox"
VERSION_RE = re.compile(r"^V(\d+)(?:\.(\d+))?__")

MAX_ERR_CHARS = 12000


def version_key(name: str) -> tuple:
    m = VERSION_RE.match(name)
    if not m:
        # 非 V 前缀文件排在最后，按文件名排序，避免静默跳过
        return (10 ** 9, 0, name)
    return (int(m.group(1)), int(m.group(2) or 0), name)


def mysql_args(args: argparse.Namespace, database: str | None) -> list[str]:
    cmd = [args.mysql_client, "--protocol=TCP", "-h", args.host, "-P", str(args.port),
           "-u", args.user, "--default-character-set=utf8mb4", "--batch", "--raw"]
    if args.password:
        cmd.append(f"--password={args.password}")
    if database:
        cmd.append(database)
    return cmd


def run_sql_file(args: argparse.Namespace, path: Path, database: str) -> dict:
    started = time.perf_counter()
    with path.open("rb") as fh:
        proc = subprocess.run(mysql_args(args, database), stdin=fh,
                              stdout=subprocess.PIPE, stderr=subprocess.PIPE,
                              timeout=args.timeout)
    return {
        "file": path.name,
        "bytes": path.stat().st_size,
        "exit_code": proc.returncode,
        "duration_s": round(time.perf_counter() - started, 3),
        "stdout": proc.stdout.decode("utf-8", "replace")[:MAX_ERR_CHARS],
        "stderr": proc.stderr.decode("utf-8", "replace")[:MAX_ERR_CHARS],
    }


def run_sql_text(args: argparse.Namespace, sql: str, database: str | None) -> dict:
    started = time.perf_counter()
    proc = subprocess.run(mysql_args(args, database), input=sql.encode("utf-8"),
                          stdout=subprocess.PIPE, stderr=subprocess.PIPE,
                          timeout=args.timeout)
    return {
        "exit_code": proc.returncode,
        "duration_s": round(time.perf_counter() - started, 3),
        "stdout": proc.stdout.decode("utf-8", "replace"),
        "stderr": proc.stderr.decode("utf-8", "replace")[:MAX_ERR_CHARS],
    }


def main() -> int:
    ap = argparse.ArgumentParser(description="本机 MySQL 上真机执行 Flyway 迁移链")
    ap.add_argument("--mysql-client", required=True, help="mysql.exe 绝对路径")
    ap.add_argument("--host", default="127.0.0.1")
    ap.add_argument("--port", type=int, default=53306)
    ap.add_argument("--user", default="root")
    ap.add_argument("--password", default="")
    ap.add_argument("--database", default="zhicloud_sandbox_chain")
    ap.add_argument("--migration-dir", default=str(DEFAULT_MIGRATION_DIR))
    ap.add_argument("--verification-dir", default=str(DEFAULT_VERIFY_DIR))
    ap.add_argument("--verify", nargs="*", default=[], help="要执行的 verify 脚本版本，如 V101 V102")
    ap.add_argument("--from-version", default=None, help="只执行 >= 该版本的迁移，如 V101")
    ap.add_argument("--continue-on-error", action="store_true",
                    help="出错后继续执行（默认与 Flyway 一致：遇错即停）")
    ap.add_argument("--no-drop", action="store_true", help="不删库重建，直接在现有库上执行")
    ap.add_argument("--timeout", type=int, default=1800)
    ap.add_argument("--output", default=str(DEFAULT_OUTPUT))
    args = ap.parse_args()

    if not args.database.startswith(SAFE_DB_PREFIX):
        print(f"[FAIL] 只允许操作以 {SAFE_DB_PREFIX} 开头的沙箱库，收到：{args.database}", file=sys.stderr)
        return 2

    mig_dir = Path(args.migration_dir)
    files = sorted([p for p in mig_dir.glob("V*.sql")], key=lambda p: version_key(p.name))
    if args.from_version:
        floor = version_key(args.from_version + "__")
        files = [p for p in files if version_key(p.name) >= floor]

    result: dict = {
        "generated_at": datetime.now(timezone.utc).astimezone().isoformat(),
        "host": args.host,
        "port": args.port,
        "database": args.database,
        "migration_dir": str(mig_dir),
        "files_total": len(files),
        "from_version": args.from_version,
        "continue_on_error": args.continue_on_error,
        "results": [],
        "files_failed": 0,
        "stopped_at": None,
        "verify": [],
        "server_metrics": {},
        "table_count": None,
    }

    version_res = run_sql_text(args, "SELECT VERSION(), @@sql_mode, @@collation_server, @@time_zone, @@transaction_isolation;", None)
    result["server_metrics"] = version_res

    if not args.no_drop:
        ddl = run_sql_text(args, f"DROP DATABASE IF EXISTS `{args.database}`;", None)
        if ddl["exit_code"] != 0:
            result["results"].append({"file": "<drop database>", **ddl})
        cdb = run_sql_text(args, f"CREATE DATABASE `{args.database}` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;", None)
        result["results"].append({"file": "<create database>", **cdb})
        if cdb["exit_code"] != 0:
            result["status"] = "failed"
            print(json.dumps(result, ensure_ascii=False, indent=2))
            return 1

    for path in files:
        r = run_sql_file(args, path, args.database)
        result["results"].append(r)
        if r["exit_code"] != 0:
            result["files_failed"] += 1
            if not args.continue_on_error:
                result["stopped_at"] = path.name
                break

    cnt = run_sql_text(args,
                       "SELECT COUNT(*) FROM information_schema.tables "
                       "WHERE table_schema = %s;" % ("'" + args.database + "'",), None)
    result["table_count"] = cnt["stdout"].strip().splitlines()[-1] if cnt["exit_code"] == 0 else None

    for v in args.verify:
        vp = Path(args.verification_dir) / f"{v}_verify.sql"
        if not vp.exists():
            result["verify"].append({"script": f"{v}_verify.sql", "missing": True})
            continue
        vr = run_sql_file(args, vp, args.database)
        result["verify"].append({"script": vp.name, **vr})

    result["status"] = "passed" if result["files_failed"] == 0 else "failed"
    out = Path(args.output)
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps({k: v for k, v in result.items() if k != "results"}, ensure_ascii=False, indent=2))
    print(f"\n[summary] files={len(files)} failed={result['files_failed']} stopped_at={result['stopped_at']} tables={result['table_count']}")
    return 0 if result["files_failed"] == 0 else 1


if __name__ == "__main__":
    raise SystemExit(main())