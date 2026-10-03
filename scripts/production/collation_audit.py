#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""排序规则（collation）漂移只读审计。

背景
----
本仓库的迁移链里，一部分建表语句显式写了 `COLLATE utf8mb4_unicode_ci`，
另一部分没有写、直接继承库默认的 `utf8mb4_0900_ai_ci`。
结果：同一个逻辑键（例如 `code` / `rule_code` / `account_code`）在不同表里
排序规则不一致。MySQL 在比较两种不同排序规则的字符串列时会直接抛
`ERROR 1267 (HY000): Illegal mix of collations`，而不是隐式转换成功——
这条路径只在第一次真正执行时才暴露，属于典型的「上线后炸」缺陷。

本脚本做什么
------------
1. 统计库/表/列的排序规则分布；
2. 找出所有「同名列跨表排序规则不一致」的候选连接键（高危清单）；
3. 找出与目标排序规则不一致的表，并生成**仅文本**的 ALTER 建议语句。

安全边界
--------
* 只发 SELECT，不执行任何 DDL/DML；
* `--database` 只用于 `USE`，不要求沙箱前缀（本脚本可用于预生产/生产评估）；
* 生成的 ALTER 只写进 JSON 输出，绝不自动执行。

用法
----
python scripts/production/collation_audit.py \
    --mysql-client C:\path\to\mysql.exe --host 127.0.0.1 --port 53306 \
    --database zhicloud_sandbox_chain --target-collation utf8mb4_unicode_ci \
    --output ..\docs\enterprise-platform\evidence\production\collation-audit.json
"""
from __future__ import annotations

import argparse
import json
import subprocess
import sys
from datetime import datetime, timezone
from pathlib import Path

MAX_ERR_CHARS = 4000

TARGET_DEFAULT = "utf8mb4_unicode_ci"

SQL_SERVER = (
    "SELECT VERSION() AS version, @@collation_server AS collation_server, "
    "@@collation_database AS collation_database, @@character_set_server AS charset_server;"
)
SQL_TABLE_DIST = (
    "SELECT TABLE_COLLATION AS collation_name, COUNT(*) AS table_count "
    "FROM information_schema.tables WHERE TABLE_SCHEMA = DATABASE() "
    "GROUP BY TABLE_COLLATION ORDER BY table_count DESC;"
)
SQL_COLUMN_DIST = (
    "SELECT COLLATION_NAME AS collation_name, COUNT(*) AS column_count "
    "FROM information_schema.columns WHERE TABLE_SCHEMA = DATABASE() "
    "AND COLLATION_NAME IS NOT NULL GROUP BY COLLATION_NAME ORDER BY column_count DESC;"
)
SQL_CHARSET_DIST = (
    "SELECT CHARACTER_SET_NAME AS charset_name, COUNT(*) AS column_count "
    "FROM information_schema.columns WHERE TABLE_SCHEMA = DATABASE() "
    "AND COLLATION_NAME IS NOT NULL GROUP BY CHARACTER_SET_NAME ORDER BY column_count DESC;"
)
# 同名列跨表排序规则不一致 = 潜在 1267 连接键
SQL_KEY_CONFLICTS = (
    "SELECT COLUMN_NAME AS column_name, "
    "COUNT(DISTINCT COLLATION_NAME) AS collation_count, "
    "COUNT(DISTINCT TABLE_NAME) AS table_count, "
    "GROUP_CONCAT(DISTINCT COLLATION_NAME ORDER BY COLLATION_NAME) AS collation_list, "
    "GROUP_CONCAT(DISTINCT CONCAT(TABLE_NAME, '.', COLUMN_NAME) ORDER BY TABLE_NAME SEPARATOR ', ') AS columns_seen "
    "FROM information_schema.columns WHERE TABLE_SCHEMA = DATABASE() "
    "AND COLLATION_NAME IS NOT NULL "
    "GROUP BY COLUMN_NAME HAVING COUNT(DISTINCT COLLATION_NAME) > 1 "
    "ORDER BY table_count DESC, COLUMN_NAME;"
)
SQL_TABLES_OFF_TARGET = (
    "SELECT TABLE_NAME AS table_name, TABLE_COLLATION AS table_collation, "
    "IFNULL(TABLE_ROWS, 0) AS table_rows, ENGINE AS engine "
    "FROM information_schema.tables WHERE TABLE_SCHEMA = DATABASE() "
    "AND TABLE_TYPE = 'BASE TABLE' AND TABLE_COLLATION <> %s ORDER BY TABLE_NAME;"
)
# 目标排序规则下的重复键风险：转换前必须确认不会撞唯一键
SQL_UNIQUE_INDEXES = (
    "SELECT TABLE_NAME AS table_name, INDEX_NAME AS index_name, "
    "GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) AS columns "
    "FROM information_schema.statistics WHERE TABLE_SCHEMA = DATABASE() "
    "AND NON_UNIQUE = 0 AND TABLE_NAME = %s GROUP BY TABLE_NAME, INDEX_NAME;"
)
SQL_TABLE_ROWS = "SELECT COUNT(*) AS c FROM `%s`;"
# 目标排序规则下的唯一键碰撞预检：同一组唯一键值在目标排序规则下是否重复
SQL_UNIQUE_COLS = (
    "SELECT s.COLUMN_NAME AS column_name, c.COLLATION_NAME AS collation_name "
    "FROM information_schema.statistics s "
    "JOIN information_schema.columns c "
    "  ON c.TABLE_SCHEMA = s.TABLE_SCHEMA AND c.TABLE_NAME = s.TABLE_NAME "
    " AND c.COLUMN_NAME = s.COLUMN_NAME "
    "WHERE s.TABLE_SCHEMA = DATABASE() AND s.NON_UNIQUE = 0 "
    "AND s.TABLE_NAME = %s AND s.INDEX_NAME = %s ORDER BY s.SEQ_IN_INDEX;"
)


def mysql_args(args: argparse.Namespace, database: str | None) -> list[str]:
    cmd = [args.mysql_client, "--protocol=TCP", "-h", args.host, "-P", str(args.port),
           "-u", args.user, "--default-character-set=utf8mb4", "--batch", "--raw"]
    if args.password:
        cmd.append(f"--password={args.password}")
    if database:
        cmd.append(database)
    return cmd


def query(args: argparse.Namespace, sql: str) -> tuple[list[dict], dict]:
    proc = subprocess.run(mysql_args(args, args.database), input=sql.encode("utf-8"),
                          stdout=subprocess.PIPE, stderr=subprocess.PIPE, timeout=args.timeout)
    info = {"exit_code": proc.returncode,
            "stderr": proc.stderr.decode("utf-8", "replace")[:MAX_ERR_CHARS]}
    if proc.returncode != 0:
        return [], info
    lines = proc.stdout.decode("utf-8", "replace").splitlines()
    if not lines:
        return [], info
    header = lines[0].split("\t")
    rows = []
    for line in lines[1:]:
        if line == "":
            continue
        rows.append(dict(zip(header, line.split("\t"))))
    return rows, info


def q(sql: str, *params: str) -> str:
    """极简参数替换：只允许 %s 占位，且参数由调用方保证是标识符/字面量。"""
    out = sql
    for p in params:
        out = out.replace("%s", p, 1)
    return out


def quote_literal(v: str) -> str:
    return "'" + v.replace("\\", "\\\\").replace("'", "''") + "'"


def main() -> int:
    ap = argparse.ArgumentParser(description="排序规则漂移只读审计")
    ap.add_argument("--mysql-client", required=True)
    ap.add_argument("--host", default="127.0.0.1")
    ap.add_argument("--port", type=int, default=53306)
    ap.add_argument("--user", default="root")
    ap.add_argument("--password", default="")
    ap.add_argument("--database", required=True)
    ap.add_argument("--target-collation", default=TARGET_DEFAULT)
    ap.add_argument("--output", required=True)
    ap.add_argument("--timeout", type=int, default=300)
    args = ap.parse_args()

    result: dict = {
        "generated_at": datetime.now(timezone.utc).astimezone().isoformat(),
        "host": args.host, "port": args.port, "database": args.database,
        "target_collation": args.target_collation,
        "server": {}, "table_collation_distribution": [], "column_collation_distribution": [],
        "charset_distribution": [], "join_key_conflicts": [], "tables_off_target": [],
        "duplicate_key_risk": [], "suggested_ddl": [], "errors": [],
    }

    def add_err(name, info):
        if info.get("exit_code") != 0:
            result["errors"].append({"query": name, **info})

    rows, info = query(args, SQL_SERVER)
    result["server"] = rows[0] if rows else {}
    add_err("server", info)

    for key, sql in (("table_collation_distribution", SQL_TABLE_DIST),
                     ("column_collation_distribution", SQL_COLUMN_DIST),
                     ("charset_distribution", SQL_CHARSET_DIST),
                     ("join_key_conflicts", SQL_KEY_CONFLICTS)):
        rows, info = query(args, sql)
        result[key] = rows
        add_err(key, info)

    rows, info = query(args, q(SQL_TABLES_OFF_TARGET, quote_literal(args.target_collation)))
    result["tables_off_target"] = rows
    add_err("tables_off_target", info)

    # 转换前必须确认不会撞唯一键：逐表看是否有数据 + 唯一索引列组合
    for t in result["tables_off_target"]:
        name = t.get("table_name", "")
        if not name:
            continue
        cnt_rows, info = query(args, SQL_TABLE_ROWS % name.replace("`", "``"))
        add_err("row_count", info)
        row_count = int(cnt_rows[0]["c"]) if cnt_rows and cnt_rows[0].get("c", "").isdigit() else None
        uq_rows, info = query(args, q(SQL_UNIQUE_INDEXES, quote_literal(name)))
        add_err("unique_indexes", info)
        collisions = []
        if row_count:
            for u in uq_rows:
                idx = u.get("index_name", "")
                if idx == "PRIMARY":
                    continue
                cols, info = query(args, q(SQL_UNIQUE_COLS, quote_literal(name), quote_literal(idx)))
                add_err("unique_cols", info)
                if not cols:
                    continue
                exprs, order = [], []
                for c in cols:
                    cn = c.get("column_name", "")
                    order.append("`%s`" % cn.replace("`", "``"))
                    if c.get("collation_name"):
                        exprs.append("`%s` COLLATE %s" % (cn.replace("`", "``"), args.target_collation))
                    else:
                        exprs.append("`%s`" % cn.replace("`", "``"))
                # 同一批表达式同时出现在 SELECT 与 GROUP BY，规避 ONLY_FULL_GROUP_BY 1055
                sel = ", ".join("%s AS k%d" % (e, i) for i, e in enumerate(exprs))
                grp = ", ".join(exprs)
                probe = ("SELECT COUNT(*) AS c FROM (SELECT %s FROM `%s` GROUP BY %s HAVING COUNT(*) > 1) x;"
                         % (sel, name.replace("`", "``"), grp))
                hit, info = query(args, probe)
                add_err("collision_probe", info)
                n = int(hit[0]["c"]) if hit and hit[0].get("c", "").isdigit() else None
                collisions.append({"index_name": idx, "columns": order, "duplicate_groups": n})
        result["duplicate_key_risk"].append({
            "table_name": name,
            "row_count": row_count,
            "unique_indexes": [u.get("index_name") for u in uq_rows],
            "checked": row_count is not None and row_count > 0,
            "collisions": collisions,
        })
        result["suggested_ddl"].append(
            "ALTER TABLE `%s` CONVERT TO CHARACTER SET utf8mb4 COLLATE %s;" % (name, args.target_collation))

    result["summary"] = {
        "tables_total": sum(int(r.get("table_count", 0)) for r in result["table_collation_distribution"]),
        "tables_off_target": len(result["tables_off_target"]),
        "join_key_conflict_columns": len(result["join_key_conflicts"]),
        "tables_with_data": sum(1 for r in result["duplicate_key_risk"] if r["checked"]),
        "tables_with_collision": sum(1 for r in result["duplicate_key_risk"]
                                     for c in r.get("collisions", []) if (c.get("duplicate_groups") or 0) > 0),
    }

    out = Path(args.output)
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(json.dumps(result, ensure_ascii=False, indent=2), encoding="utf-8")
    print(json.dumps(result["summary"], ensure_ascii=False, indent=2))
    print(json.dumps(result["server"], ensure_ascii=False))
    print("join_key_conflicts(top10):")
    for r in result["join_key_conflicts"][:10]:
        print("  %-24s tables=%s collations=%s" % (r.get("column_name"), r.get("table_count"), r.get("collation_list")))
    print("errors=%d" % len(result["errors"]))
    return 0 if not result["errors"] else 1


if __name__ == "__main__":
    raise SystemExit(main())