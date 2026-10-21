#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""生产首次初始化：渲染 *_bootstrap.sql 并注入随机管理员口令。

背景（安全基线 R7）：
    历史安装脚本 sql/<dialect>/*.sql 里预置了 20 个 system_users 账号，
    口令是公开的弱 BCrypt(cost=04) 哈希，导入生产即等于把后门写进库里。
    这些行已经从生产安装脚本中移出（见 demo-data/sql/<dialect>/*_demo_seed.sql，禁止导入生产），
    生产只允许通过本脚本创建**唯一一个**初始管理员：
      1. 口令由 secrets 模块随机生成（默认 12 字节 URL-safe，16 字符，匹配登录接口上限）；
      2. 口令哈希用 BCrypt cost=12、$2a$ 前缀生成（Spring Security 可校验）；
      3. 明文口令只在本次 stdout 打印一次，不落盘、不写日志、不进版本库。

边界（不要把它当成"账号体系已安全"）：
    * 本脚本只解决"初始口令不可预测"，不解决口令轮换、MFA、口令策略、审计告警；
    * 首次登录后必须立刻改口令并接入密钥管理系统，本脚本不保证这点；
    * 除 MySQL 外，其他方言只渲染 SQL 文本，需要人工用对应客户端执行（--dry-run）。

用法：
    python scripts/production/init_bootstrap_admin.py --dialect mysql \
        --mysql-client C:\path\to\mysql.exe --host 127.0.0.1 --port 3306 \
        --database zhicloud_platform --user root
    # 只渲染不执行（Oracle / DM / KingBase / ... 用这种）：
    python scripts/production/init_bootstrap_admin.py --dialect oracle --out bootstrap_rendered.sql
"""
from __future__ import annotations

import argparse
import json
import re
import secrets
import subprocess
import sys
import tempfile
from pathlib import Path

REPO = Path(__file__).resolve().parents[2]

TEMPLATES = {
    "mysql": "sql/mysql/zhicloud_platform_bootstrap.sql",
    "dm": "sql/dm/ruoyi-vue-pro-dm8_bootstrap.sql",
    "highgo": "sql/highgo/ruoyi-vue-pro_bootstrap.sql",
    "kingbase": "sql/kingbase/ruoyi-vue-pro_bootstrap.sql",
    "opengauss": "sql/opengauss/ruoyi-vue-pro_bootstrap.sql",
    "oracle": "sql/oracle/ruoyi-vue-pro_bootstrap.sql",
    "postgresql": "sql/postgresql/ruoyi-vue-pro_bootstrap.sql",
    "sqlserver": "sql/sqlserver/ruoyi-vue-pro_bootstrap.sql",
}
USER_PLACEHOLDER = "__ZHICLOUD_BOOTSTRAP_USERNAME__"
HASH_PLACEHOLDER = "__ZHICLOUD_BOOTSTRAP_PASSWORD_HASH__"
DEFAULT_USERNAME = "zhicloud_admin"
DEFAULT_COST = 12
DEFAULT_PASSWORD_BYTES = 12
# 登录接口 AuthLoginReqVO 的口令长度上限（@Length(min = 4, max = 16)），超过就登不进去
MAX_PASSWORD_LENGTH = 16
ROLE_ID = 1
POST_ID = 4
TENANT_ID = 1
USERNAME_RE = re.compile(r"^[A-Za-z][A-Za-z0-9_.-]{2,29}$")

# V1.1/V115 中公开种子账号的用户名。初始化管理员不能复用这些名字，
# 否则 V115 在存量库上会把刚创建的管理员当成种子账号清理掉。
KNOWN_SEED_USERNAMES = frozenset({
    "admin", "zhicloud", "yuanma", "test", "admin107", "admin108",
    "admin109", "admin110", "newobject", "aoteman", "hrmgr", "aotemane",
    "admin123", "goudan", "wwbwwb", "admin1", "test01", "a00001",
    "aoteman001",
})

# BCrypt 哈希字面量：用于确认模板里没有被写死已知口令
BCRYPT_LITERAL = re.compile(r"\$2[aby]\$\d{2}\$[./A-Za-z0-9]{53}")


def validate_username(username: str) -> None:
    """限制用户名，避免 SQL 注入和 V115 种子账号冲突。"""
    if not USERNAME_RE.fullmatch(username):
        raise SystemExit(
            "[FAIL] username 必须为 3-30 位字母开头，且只能包含字母、数字、点、下划线和连字符。")
    if username.lower() in KNOWN_SEED_USERNAMES:
        raise SystemExit(
            f"[FAIL] username={username!r} 是 V1.1/V115 公开种子账号名；"
            "请使用独立的非种子管理员账号名。")


def _strip_sql_comments(text: str) -> str:
    lines = []
    for line in text.splitlines():
        idx = line.find("--")
        if idx >= 0:
            line = line[:idx]
        lines.append(line)
    return "\n".join(lines)


def _insert_statement(text: str, table: str) -> str | None:
    pattern = re.compile(
        rf'\bINSERT\s+(?:IGNORE\s+)?INTO\s+(?:[`"\[])?{re.escape(table)}(?:[`"\]])?\b(.*?);',
        re.IGNORECASE | re.DOTALL,
    )
    match = pattern.search(text)
    return match.group(0) if match else None


def _split_sql_list(value: str) -> list[str]:
    """按顶层逗号切分 SQL 列/值列表，忽略字符串和括号内的逗号。"""
    parts: list[str] = []
    start = 0
    depth = 0
    quote: str | None = None
    i = 0
    while i < len(value):
        ch = value[i]
        if quote:
            if ch == quote:
                if i + 1 < len(value) and value[i + 1] == quote:
                    i += 2
                    continue
                quote = None
        elif ch in ("'", '"'):
            quote = ch
        elif ch == "(":
            depth += 1
        elif ch == ")":
            depth = max(0, depth - 1)
        elif ch == "," and depth == 0:
            parts.append(value[start:i].strip())
            start = i + 1
        i += 1
    parts.append(value[start:].strip())
    return parts


def _parenthesized_content(text: str, start: int) -> tuple[str, int]:
    """读取从 start 处左括号开始的平衡括号内容，忽略字符串中的括号。"""
    if start < 0 or start >= len(text) or text[start] != "(":
        raise ValueError("expected '('")
    depth = 0
    quote: str | None = None
    i = start
    while i < len(text):
        ch = text[i]
        if quote:
            if ch == quote:
                if i + 1 < len(text) and text[i + 1] == quote:
                    i += 2
                    continue
                quote = None
        elif ch in ("'", '"'):
            quote = ch
        elif ch == "(":
            depth += 1
        elif ch == ")":
            depth -= 1
            if depth == 0:
                return text[start + 1:i], i + 1
        i += 1
    raise ValueError("unbalanced parentheses")


def _required_value(statement: str, table: str, column: str, expected: str) -> None:
    """校验 INSERT ... (columns) VALUES (values) 中的指定字面量列值。

    不能使用非贪婪正则直接匹配括号，因为 PostgreSQL/Oracle 等模板的
    VALUES 中会包含 nextval(...)、currval(...) 等嵌套函数，非贪婪匹配会在
    第一个右括号处提前截断。
    """
    insert_match = re.search(
        rf"\bINSERT\s+(?:IGNORE\s+)?INTO\s+(?:[`\"\[])?{re.escape(table)}(?:[`\"\]])?\b",
        statement,
        re.IGNORECASE,
    )
    if not insert_match:
        raise SystemExit(f"[FAIL] {table} 缺少 INSERT 语句。")
    columns_start = statement.find("(", insert_match.end())
    if columns_start < 0:
        raise SystemExit(f"[FAIL] {table} 不是可校验的 VALUES INSERT。")
    try:
        columns_text, columns_end = _parenthesized_content(statement, columns_start)
    except ValueError:
        raise SystemExit(f"[FAIL] {table} 的列清单括号不平衡。")
    values_match = re.search(r"\bVALUES\b", statement[columns_end:], re.IGNORECASE)
    if not values_match:
        raise SystemExit(f"[FAIL] {table} 不是可校验的单行 VALUES INSERT。")
    values_start = statement.find("(", columns_end + values_match.end())
    if values_start < 0:
        raise SystemExit(f"[FAIL] {table} 的 VALUES 缺少左括号。")
    try:
        values_text, _ = _parenthesized_content(statement, values_start)
    except ValueError:
        raise SystemExit(f"[FAIL] {table} 的 VALUES 括号不平衡。")
    columns = [re.sub(r'[`"\[\]\s]', "", item).lower()
               for item in _split_sql_list(columns_text)]
    values = _split_sql_list(values_text)
    try:
        actual = values[columns.index(column.lower())].strip()
    except (ValueError, IndexError):
        raise SystemExit(f"[FAIL] {table} 缺少列 {column} 或对应值。")
    if actual != expected:
        raise SystemExit(f"[FAIL] {table}.{column} 必须为 {expected}，当前为 {actual!r}。")


def _dialect_for_template(template: Path) -> str:
    """返回模板对应的方言名；自定义模板路径无法匹配时使用父目录名。"""
    try:
        resolved = template.resolve()
    except OSError:
        resolved = template
    for dialect, rel in TEMPLATES.items():
        try:
            if (REPO / rel).resolve() == resolved:
                return dialect
        except OSError:
            continue
    return template.parent.name


def validate_template_text(template: Path, text: str) -> None:
    """在替换口令前检查 bootstrap 模板的结构和安全契约。"""
    if USER_PLACEHOLDER not in text or HASH_PLACEHOLDER not in text:
        raise SystemExit(
            f"[FAIL] 模板 {template} 缺少占位符 {USER_PLACEHOLDER} / {HASH_PLACEHOLDER}；"
            "拒绝渲染，避免把未初始化的模板写进生产库。")

    if BCRYPT_LITERAL.search(text):
        raise SystemExit(f"[FAIL] 模板 {template} 已包含 BCrypt 哈希字面量，拒绝渲染。")

    user_stmt = _insert_statement(text, "system_users")
    if not user_stmt:
        raise SystemExit(f"[FAIL] 模板 {template} 缺少 system_users INSERT。")
    if not re.search(r"\btenant_id\b", user_stmt, re.IGNORECASE):
        raise SystemExit(f"[FAIL] 模板 {template} 的 system_users INSERT 缺少 tenant_id。")

    # 生产初始化必须是可审计的：重复账号不能静默任选一条更新，
    # 且非 MySQL 方言当前没有唯一约束，必须在脚本层显式阻断。
    if not re.search(r"\bCOUNT\s*\(\s*\*\s*\)", text, re.IGNORECASE):
        raise SystemExit(f"[FAIL] 模板 {template} 缺少重复账号 COUNT(*) 守卫。")
    if not re.search(
        r"SELECT[\s\S]{0,160}?COUNT\s*\(\s*\*\s*\)[\s\S]{0,300}?"
        r"FROM\s+[`\"]?system_users[`\"]?[\s\S]{0,300}?"
        r"[`\"]?tenant_id[`\"]?\s*=\s*1",
        text,
        re.IGNORECASE,
    ):
        raise SystemExit(f"[FAIL] 模板 {template} 的账号预检未按 tenant_id=1 限定。")

    dialect = _dialect_for_template(template)
    if dialect == "mysql":
        required_mysql_boundary = ("MySQL 执行边界：", "DELIMITER", "JDBC")
        if not all(marker in text for marker in required_mysql_boundary):
            raise SystemExit(
                f"[FAIL] 模板 {template} 缺少 MySQL 客户端执行边界；"
                "必须明确 DELIMITER 不能由 JDBC/普通批量执行器解析。")
    elif "执行边界：" not in text:
        raise SystemExit(
            f"[FAIL] 模板 {template} 缺少“执行边界：”声明；"
            "非 MySQL 方言在真实库验证前必须保留单次受控执行说明。")
    if re.search(r"\b(?:VALUES|SELECT)\s*\(?\s*1\s*,", user_stmt, re.IGNORECASE):
        raise SystemExit(f"[FAIL] 模板 {template} 使用了固定用户 ID=1，拒绝渲染。")

    role_stmt = _insert_statement(text, "system_user_role")
    if not role_stmt:
        raise SystemExit(f"[FAIL] 模板 {template} 缺少 system_user_role INSERT。")
    _required_value(role_stmt, "system_user_role", "role_id", str(ROLE_ID))

    post_stmt = _insert_statement(text, "system_user_post")
    if not post_stmt:
        raise SystemExit(f"[FAIL] 模板 {template} 缺少 system_user_post INSERT。")
    _required_value(post_stmt, "system_user_post", "post_id", str(POST_ID))

    body = _strip_sql_comments(user_stmt)
    if re.search(r"[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}", body):
        raise SystemExit(f"[FAIL] 模板 {template} 的 system_users INSERT 含真实邮箱字面量。")
    if re.search(r"(?<!\d)1[3-9]\d{9}(?!\d)", body):
        raise SystemExit(f"[FAIL] 模板 {template} 的 system_users INSERT 含真实手机号字面量。")


def render(template: Path, username: str, password_hash: str) -> str:
    validate_username(username)
    if not BCRYPT_LITERAL.fullmatch(password_hash):
        raise SystemExit("[FAIL] password_hash 不是合法的 BCrypt 字面量。")
    text = template.read_text(encoding="utf-8")
    validate_template_text(template, text)
    text = text.replace(USER_PLACEHOLDER, username).replace(HASH_PLACEHOLDER, password_hash)
    # 渲染后只能出现“本次生成的同一个”哈希；MySQL/SQL Server 模板会在
    # INSERT 与 UPDATE 中各引用一次，因此按唯一值而不是出现次数校验。
    found = BCRYPT_LITERAL.findall(text)
    if not found or set(found) != {password_hash}:
        raise SystemExit(
            f"[FAIL] 渲染结果包含非本次生成的口令哈希（发现 {len(found)} 处），拒绝写出。")
    return text


def mysql_args(args: argparse.Namespace, database: str | None) -> list[str]:
    cmd = [args.mysql_client, "--protocol=TCP", "-h", args.host, "-P", str(args.port),
           "-u", args.user]
    if args.password_env:
        import os
        cmd.append(f"-p{os.environ.get(args.password_env, '')}")
    if database:
        cmd.append(database)
    return cmd


def run_sql_file(args: argparse.Namespace, path: Path, database: str) -> dict:
    with path.open("rb") as fh:
        proc = subprocess.run(mysql_args(args, database), stdin=fh,
                              capture_output=True, timeout=180)
    return {"exit_code": proc.returncode,
            "stdout": proc.stdout.decode("utf-8", "replace")[-2000:],
            "stderr": proc.stderr.decode("utf-8", "replace")[-2000:]}


def run_sql_text(args: argparse.Namespace, sql: str, database: str | None) -> dict:
    proc = subprocess.run(mysql_args(args, database), input=sql.encode("utf-8"),
                          capture_output=True, timeout=180)
    return {"exit_code": proc.returncode,
            "stdout": proc.stdout.decode("utf-8", "replace").strip(),
            "stderr": proc.stderr.decode("utf-8", "replace")[-2000:]}


def main(argv: list[str] | None = None) -> int:
    ap = argparse.ArgumentParser(description="生产首次初始化管理员（随机口令）")
    ap.add_argument("--dialect", choices=sorted(TEMPLATES), default="mysql")
    ap.add_argument("--template", type=Path, help="覆盖默认模板路径")
    ap.add_argument("--username", default=DEFAULT_USERNAME)
    ap.add_argument("--cost", type=int, default=DEFAULT_COST)
    ap.add_argument("--password-bytes", type=int, default=DEFAULT_PASSWORD_BYTES)
    ap.add_argument("--out", type=Path, help="渲染后的 SQL 落盘路径（默认临时文件）")
    ap.add_argument("--apply", action="store_true",
                    help="渲染后立刻执行；目前仅 MySQL 支持直连执行")
    ap.add_argument("--force", action="store_true", help="目标账号已存在时仍然覆盖")
    ap.add_argument("--mysql-client", help="mysql.exe 绝对路径（--apply 时必填）")
    ap.add_argument("--host", default="127.0.0.1")
    ap.add_argument("--port", type=int, default=3306)
    ap.add_argument("--database", default="zhicloud_platform")
    ap.add_argument("--user", default="root")
    ap.add_argument("--password-env", help="从该环境变量取数据库口令，避免命令行泄露")
    args = ap.parse_args(argv)

    if args.cost < 10:
        raise SystemExit("[FAIL] BCrypt cost 必须 ≥ 10（OWASP 口令存储基线）。")
    if args.apply and args.dialect != "mysql":
        raise SystemExit("[FAIL] 只有 mysql 方言支持 --apply；其他方言请渲染后用对应客户端执行。")
    if args.apply and not args.mysql_client:
        raise SystemExit("[FAIL] --apply 需要 --mysql-client。")

    template = args.template or (REPO / TEMPLATES[args.dialect])
    if not template.is_file():
        raise SystemExit(f"[FAIL] 模板不存在：{template}")

    try:
        import bcrypt  # noqa: PLC0415 延迟导入，只在真正需要哈希时才依赖
    except ModuleNotFoundError:
        raise SystemExit("[FAIL] 缺少 bcrypt 依赖：pip install bcrypt（渲染口令哈希必需）。")

    password = secrets.token_urlsafe(args.password_bytes)
    # 登录接口 AuthLoginReqVO 对口令做了 @Length(min = 4, max = 16) 校验：
    # 生成的口令一旦超过 16 位，账号虽然写进库了，但登录时会在参数校验阶段被拒，
    # 表现为"引导成功却登不进去"。这里提前拦掉，别把问题留给交付现场。
    if len(password) > MAX_PASSWORD_LENGTH:
        raise SystemExit(
            f"[FAIL] 生成的管理员口令长度 {len(password)} 超过 {MAX_PASSWORD_LENGTH} 位，"
            f"会被登录接口拒绝；请调小 --password-bytes。"
        )
    password_hash = bcrypt.hashpw(
        password.encode("utf-8"),
        bcrypt.gensalt(args.cost, prefix=b"2a"),
    ).decode("ascii")

    sql = render(template, args.username, password_hash)

    if args.out:
        args.out.parent.mkdir(parents=True, exist_ok=True)
        args.out.write_text(sql, encoding="utf-8", newline="\n")
        rendered = args.out
        cleanup = False
    else:
        tmp = tempfile.NamedTemporaryFile("w", suffix=".sql", delete=False,
                                          encoding="utf-8", newline="\n")
        tmp.write(sql)
        tmp.close()
        rendered = Path(tmp.name)
        # --apply 用完即删；不执行时保留，供人工用对应客户端执行
        cleanup = bool(args.apply)

    result: dict = {
        "dialect": args.dialect,
        "template": str(template.relative_to(REPO)).replace("\\", "/"),
        "rendered_sql": str(rendered),
        "username": args.username,
        "bcrypt_cost": args.cost,
        "applied": False,
    }

    try:
        if args.apply:
            probe = run_sql_text(
                args,
                f"SELECT COUNT(*) FROM system_users "
                f"WHERE username = '{args.username}' AND tenant_id = 1;",
                args.database)
            if probe["exit_code"] != 0:
                result["error"] = probe
                print(json.dumps(result, ensure_ascii=False, indent=2))
                return 2
            exists = probe["stdout"].splitlines()[-1].strip() if probe["stdout"] else "?"
            if exists != "0" and not args.force:
                result["error"] = (f"目标库已存在 username={args.username} 的记录，"
                                   "拒绝重复初始化；确认要覆盖请加 --force。")
                print(json.dumps(result, ensure_ascii=False, indent=2))
                return 2
            exec_res = run_sql_file(args, rendered, args.database)
            result["execute"] = exec_res
            result["applied"] = exec_res["exit_code"] == 0
            print(json.dumps(result, ensure_ascii=False, indent=2))
            if exec_res["exit_code"] != 0:
                return 2
        else:
            print(json.dumps(result, ensure_ascii=False, indent=2))
    finally:
        if cleanup:
            rendered.unlink(missing_ok=True)

    print("\n==== 一次性初始口令（只打印这一次，请立刻存入密钥管理系统）====")
    print(f"username: {args.username}")
    print(f"password: {password}")
    print("============================================================")
    print("提醒：首次登录后必须立即改口令；本脚本不负责口令轮换与 MFA。")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
