#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""只读审计生产 bootstrap 管理员，并输出可复核的 JSON 证据。

这个脚本不创建、不修改、不删除任何数据库对象。它只查询已经执行过
bootstrap 的 MySQL 数据库，并把关键事实固化到带时间戳的证据文件中。

边界：
    * 只支持 MySQL；其他 7 套方言仍未在目标实库验证。
    * 数据库查询可以证明账号、角色、岗位、哈希形态和索引状态，
      但不能证明外部日志、备份、终端历史中没有出现过明文口令。
    * 脚本本身不接收明文口令，也不会把数据库口令写入 evidence。
"""
from __future__ import annotations

import argparse
import hashlib
import json
import os
import re
import subprocess
import sys
from datetime import datetime, timezone
from pathlib import Path

REPO = Path(__file__).resolve().parents[2]
TENANT_ID = 1
ROLE_ID = 1
POST_ID = 4
INDEX_NAME = "uk_system_users_tenant_active_username"
USERNAME_RE = re.compile(r"^[A-Za-z][A-Za-z0-9_.-]{2,29}$")
ALL_DIALECTS = ("mysql", "postgresql", "highgo", "kingbase", "opengauss", "oracle", "sqlserver", "dm")


class AuditError(RuntimeError):
    """数据库查询或证据校验失败。"""


def validate_username(username: str) -> None:
    if not USERNAME_RE.fullmatch(username):
        raise AuditError("username 必须为 3-30 位字母开头，且只能包含字母、数字、点、下划线和连字符。")


def mysql_command(args: argparse.Namespace, database: str | None) -> list[str]:
    """构造不把口令放进 argv 的 mysql 客户端命令。"""
    cmd = [
        args.mysql_client,
        "--protocol=TCP",
        f"--host={args.host}",
        f"--port={args.port}",
        f"--user={args.user}",
        "--batch",
        "--raw",
        "--skip-column-names",
        "--connect-timeout=10",
    ]
    if database:
        cmd.append(database)
    return cmd


def mysql_env(args: argparse.Namespace) -> dict[str, str]:
    env = os.environ.copy()
    if args.password_env:
        if args.password_env not in os.environ:
            raise AuditError(f"环境变量 {args.password_env} 不存在")
        env["MYSQL_PWD"] = os.environ[args.password_env]
    return env


def run_query(args: argparse.Namespace, sql: str, database: str | None = None) -> str:
    database = args.database if database is None else database
    try:
        proc = subprocess.run(
            mysql_command(args, database),
            input=sql.encode("utf-8"),
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            env=mysql_env(args),
            timeout=args.timeout,
        )
    except (OSError, subprocess.TimeoutExpired) as exc:
        raise AuditError(f"执行 mysql 查询失败：{exc}") from exc
    if proc.returncode != 0:
        stderr = proc.stderr.decode("utf-8", "replace").strip()
        raise AuditError(f"mysql 查询退出码 {proc.returncode}：{stderr[-2000:]}")
    return proc.stdout.decode("utf-8", "replace").strip()


def _rows(output: str) -> list[str]:
    if output == "":
        return [""]
    return output.splitlines()


def query_scalar(query, sql: str) -> str:
    rows = _rows(query(sql))
    if len(rows) != 1:
        raise AuditError(f"期望单行标量结果，实际得到 {len(rows)} 行：{sql[:120]}")
    return rows[0].strip()


def query_row(query, sql: str, columns: int) -> tuple[str, ...]:
    rows = _rows(query(sql))
    if len(rows) != 1:
        raise AuditError(f"期望单行结果，实际得到 {len(rows)} 行：{sql[:120]}")
    values = tuple(rows[0].split("\t"))
    if len(values) != columns:
        raise AuditError(f"期望 {columns} 列结果，实际得到 {len(values)} 列：{sql[:120]}")
    return values


def _int(value: str, label: str) -> int:
    try:
        return int(value)
    except (TypeError, ValueError) as exc:
        raise AuditError(f"{label} 不是整数：{value!r}") from exc


def _sha256(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def collect_evidence(args: argparse.Namespace, query_func=None) -> dict:
    """查询目标库并返回 evidence 字典。query_func 仅用于单元测试注入。"""
    validate_username(args.username)
    query = query_func or (lambda sql: run_query(args, sql, args.database))

    server = query_row(query, "SELECT DATABASE(), VERSION();", 2)
    user_count = _int(query_scalar(
        query,
        f"SELECT COUNT(*) FROM system_users "
        f"WHERE tenant_id={TENANT_ID} AND username='{args.username}';",
    ), "user_count")

    user_id = None
    tenant_id = None
    status = None
    deleted = None
    password_hash_length = None
    password_hash_prefix = None
    if user_count == 1:
        row = query_row(
            query,
            f"SELECT id, tenant_id, status, CAST(deleted AS UNSIGNED), "
            f"CHAR_LENGTH(password), LEFT(password, 4) FROM system_users "
            f"WHERE tenant_id={TENANT_ID} AND username='{args.username}';",
            6,
        )
        user_id = _int(row[0], "user_id")
        tenant_id = _int(row[1], "tenant_id")
        status = _int(row[2], "status")
        deleted = _int(row[3], "deleted")
        password_hash_length = _int(row[4], "password_hash_length")
        password_hash_prefix = row[5]

    role_count = 0
    role_ids = ""
    post_count = 0
    post_ids = ""
    if user_id is not None:
        role_count = _int(query_scalar(
            query,
            f"SELECT COUNT(*) FROM system_user_role "
            f"WHERE user_id={user_id} AND role_id={ROLE_ID};",
        ), "role_binding_count")
        role_ids = query_scalar(
            query,
            f"SELECT COALESCE(GROUP_CONCAT(role_id ORDER BY role_id), '<none>') "
            f"FROM system_user_role WHERE user_id={user_id};",
        )
        post_count = _int(query_scalar(
            query,
            f"SELECT COUNT(*) FROM system_user_post "
            f"WHERE user_id={user_id} AND post_id={POST_ID};",
        ), "post_binding_count")
        post_ids = query_scalar(
            query,
            f"SELECT COALESCE(GROUP_CONCAT(post_id ORDER BY post_id), '<none>') "
            f"FROM system_user_post WHERE user_id={user_id};",
        )

    index_row = query_row(
        query,
        f"SELECT index_name, non_unique, "
        f"COALESCE(GROUP_CONCAT(column_name ORDER BY seq_in_index), '<none>') "
        f"FROM information_schema.statistics "
        f"WHERE table_schema=DATABASE() AND table_name='system_users' "
        f"AND index_name='{INDEX_NAME}' "
        f"GROUP BY index_name, non_unique;",
        3,
    )
    duplicate_active_groups = _int(query_scalar(
        query,
        "SELECT COUNT(*) FROM ("
        "SELECT tenant_id, active_username FROM system_users "
        "WHERE deleted=0 GROUP BY tenant_id, active_username HAVING COUNT(*) > 1"
        ") AS duplicate_active_usernames;",
    ), "duplicate_active_groups")

    index_name, index_non_unique, index_columns = index_row
    index_ok = (
        index_name == INDEX_NAME
        and _int(index_non_unique, "index_non_unique") == 0
        and index_columns == "tenant_id,active_username"
    )
    hash_ok = (
        password_hash_length == 60
        and password_hash_prefix in ("$2a$", "$2b$", "$2y$")
    )
    checks = {
        "database_reachable": True,
        "single_tenant_user": user_count == 1,
        "enabled": status == 0,
        "not_deleted": deleted == 0,
        "bcrypt_hash_shape": hash_ok,
        "role_binding_exact": role_count == 1 and role_ids == str(ROLE_ID),
        "post_binding_exact": post_count == 1 and post_ids == str(POST_ID),
        "active_username_unique_index": index_ok,
        "no_duplicate_active_usernames": duplicate_active_groups == 0,
    }
    failures = [name for name, passed in checks.items() if not passed]
    evidence = {
        "generated_at": datetime.now(timezone.utc).astimezone().isoformat(),
        "status": "passed" if not failures else "failed",
        "scope": "MySQL sandbox bootstrap administrator read-only audit",
        "production_ready": False,
        "gate3_passed": False,
        "database": args.database,
        "mysql": {
            "database_reported": server[0],
            "server_version": server[1],
            "host": args.host,
            "port": args.port,
            "user": args.user,
            "client": args.mysql_client,
        },
        "bootstrap": {
            "username": args.username,
            "tenant_id": tenant_id,
            "user_count": user_count,
            "user_id": user_id,
            "status": status,
            "deleted": deleted,
            "password_hash_length": password_hash_length,
            "password_hash_prefix": password_hash_prefix,
            "role_id": ROLE_ID,
            "role_binding_count": role_count,
            "role_ids": role_ids,
            "post_id": POST_ID,
            "post_binding_count": post_count,
            "post_ids": post_ids,
        },
        "database_checks": {
            "active_username_unique_index": index_name if index_ok else None,
            "active_username_unique_index_columns": index_columns,
            "active_username_duplicate_groups": duplicate_active_groups,
        },
        "checks": checks,
        "failures": failures,
        "dialect_scope": {
            "executed_in_this_evidence": ["mysql"],
            "not_executed_in_this_evidence": [d for d in ALL_DIALECTS if d != "mysql"],
            "note": "其余 7 套模板仍只有渲染器和静态契约证据。",
        },
        "secret_handling": {
            "plaintext_password_persisted": False,
            "plaintext_password_supplied_to_auditor": False,
            "database_password_hash_only": hash_ok,
            "note": "本审计器只接收数据库连接参数，不接收 bootstrap 明文口令；"
                    "数据库查询只能证明口令以 BCrypt 哈希形态存在，"
                    "不能单独证明外部日志、终端历史或备份中不存在明文。",
        },
        "artifacts": {
            "auditor_sha256": _sha256(Path(__file__).resolve()),
        },
        "caveats": [
            "V116/V117 concurrency and soft-delete semantics are not proven by this audit.",
            "BPM/Flowable transaction rollback is not proven by this audit.",
            "Only MySQL is audited here; other dialects remain unverified in target databases.",
            "Production readiness and Gate 3 remain false until all blocking checks pass.",
        ],
    }
    return evidence


def main(argv: list[str] | None = None) -> int:
    ap = argparse.ArgumentParser(description="只读审计 bootstrap 管理员并输出 JSON 证据")
    ap.add_argument("--mysql-client", required=True, help="mysql.exe 绝对路径")
    ap.add_argument("--host", default="127.0.0.1")
    ap.add_argument("--port", type=int, default=3306)
    ap.add_argument("--database", required=True)
    ap.add_argument("--user", default="root")
    ap.add_argument("--password-env", help="数据库口令所在环境变量；不会写入 argv 或 evidence")
    ap.add_argument("--username", default="zhicloud_admin")
    ap.add_argument("--out", type=Path, required=True, help="evidence JSON 输出路径")
    ap.add_argument("--timeout", type=int, default=60)
    args = ap.parse_args(argv)

    try:
        evidence = collect_evidence(args)
    except AuditError as exc:
        print(f"[FAIL] {exc}", file=sys.stderr)
        return 2

    args.out.parent.mkdir(parents=True, exist_ok=True)
    args.out.write_text(json.dumps(evidence, ensure_ascii=False, indent=2) + "\n",
                        encoding="utf-8", newline="\n")
    print(json.dumps({k: evidence[k] for k in
                      ("status", "database", "bootstrap", "checks", "failures")},
                     ensure_ascii=False, indent=2))
    return 0 if evidence["status"] == "passed" else 1


if __name__ == "__main__":
    raise SystemExit(main())
