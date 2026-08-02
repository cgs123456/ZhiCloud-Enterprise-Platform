#!/usr/bin/env python3
# 为某个 yudao 业务模块生成【生产环境 MySQL】建表 DDL，并同时输出两份：
#   1) sql/mysql/<short>.sql                       —— 参考/手工基线脚本
#   2) yudao-server/.../db/migration/V<ver>__<short>_ddl.sql —— Flyway 增量迁移（自动纳管）
# 风格对齐本仓 Flyway 迁移（V38/V58）：CREATE TABLE IF NOT EXISTS、tenant_id 恒存在、
#   deleted TINYINT(1)、creator/updater DEFAULT ''、tenant 维度唯一索引、ENGINE=InnoDB utf8mb4。
# 注：列长/索引为基于字段类型的启发式默认值，上线前需在真实 MySQL 上做建表冒烟验证。
import os, re, glob, sys, argparse

REPO = r"D:/Desktop/yudao"
MIG_DIR = os.path.join(REPO, "yudao-server", "src", "main", "resources", "db", "migration")
TYPE_MAP = {
    "Long": "BIGINT", "long": "BIGINT",
    "Integer": "INT", "int": "INT",
    "Short": "SMALLINT", "short": "SMALLINT",
    "Byte": "TINYINT", "byte": "TINYINT",
    "Boolean": "BIT(1)", "boolean": "BIT(1)",
    "BigDecimal": "DECIMAL(20,2)",
    "Double": "DOUBLE", "double": "DOUBLE",
    "Float": "FLOAT", "float": "FLOAT",
    "LocalDateTime": "DATETIME",
    "LocalDate": "DATE",
    "byte[]": "BLOB",
}
LONG_HINT = ("content", "prompt", "config", "message", "sql", "json", "data",
             "text", "description", "remark", "template", "document", "segment",
             "history", "conversation", "definition", "expression", "listener",
             "flow", "nodes", "edges", "role", "model", "image", "music",
             "write", "workflow", "knowledge", "tool", "chat", "api_key")
RESERVED = {"sql", "order", "group", "key", "desc", "primary", "table",
            "index", "values", "select", "from", "where", "limit", "status"}

def snake(name):
    return re.sub(r'(?<!^)(?=[A-Z])', '_', name).lower()

def q(col):
    return f"`{col}`" if col in RESERVED else col

def mysql_type(t, fname):
    t = t.split("<")[0].strip()
    if "<" in t or "[" in t:
        return "TEXT"
    if t == "String":
        return "TEXT" if any(h in fname.lower() for h in LONG_HINT) else "VARCHAR(255)"
    return TYPE_MAP.get(t, "VARCHAR(255)")

def parse_file(path):
    with open(path, encoding="utf-8") as f:
        src = f.read()
    m = re.search(r'@TableName\s*\(\s*(?:value\s*=\s*)?"([^"]+)"', src)
    if not m:
        return None
    table = m.group(1)
    fields = []
    lines = src.splitlines()
    pending = []
    for line in lines:
        st = line.strip()
        if st.startswith("@"):
            pending.append(st)
            continue
        fm = re.match(r'(?:private|protected|public)?\s*([\w\.<>\[\],\s]+?)\s+(\w+)\s*;', st)
        if not fm:
            pending = []
            continue
        if "(" in st:
            pending = []
            continue
        ann = " ".join(pending)
        pending = []
        ftype = fm.group(1).strip()
        fname = fm.group(2)
        if "exist = false" in ann:
            continue
        if "static" in st or "final" in st:
            continue
        col = fname
        vm = re.search(r'value\s*=\s*"([^"]+)"', ann)
        col = vm.group(1) if vm else snake(fname)
        is_pk = "@TableId" in ann
        fields.append((col, mysql_type(ftype, fname), is_pk))
    return table, fields

def emit(table, fields):
    cols = []
    pk = None
    for col, mt, is_pk in fields:
        if is_pk:
            pk = col
            cols.append(f"    {q(col)} {mt} NOT NULL AUTO_INCREMENT COMMENT '编号'")
        else:
            cols.append(f"    {q(col)} {mt} DEFAULT NULL COMMENT '{col}'")
    cols += [
        "    creator VARCHAR(64) DEFAULT '' COMMENT '创建者'",
        "    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'",
        "    updater VARCHAR(64) DEFAULT '' COMMENT '更新者'",
        "    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'",
        "    deleted TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否删除'",
        "    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号'",
    ]
    pkcol = pk or "id"
    cols.append(f"    PRIMARY KEY ({q(pkcol)})")
    cols_lower = [c for c, _, _ in fields]
    if "code" in cols_lower:
        cols.append("    UNIQUE KEY uk_tenant_code (tenant_id, code, deleted)")
    cols.append("    KEY idx_tenant (tenant_id)")
    cols.append("    KEY idx_create_time (create_time)")
    body = ",\n".join(cols)
    return (f"CREATE TABLE IF NOT EXISTS {q(table)} (\n{body}\n) "
            f"ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='{table}';\n")

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("module", help="模块目录名，如 yudao-module-ai")
    ap.add_argument("--ver", required=True, help="Flyway 版本号，如 73")
    args = ap.parse_args()
    module = args.module
    short = module.replace("yudao-module-", "")
    base_dir = os.path.join(REPO, module, "src", "main", "java")
    ref_path = os.path.join(REPO, "sql", "mysql", f"{short}.sql")
    fly_path = os.path.join(MIG_DIR, f"V{args.ver}__{short}_ddl.sql")
    if not os.path.isdir(base_dir):
        print(f"ERROR: 找不到 {base_dir}")
        sys.exit(1)
    results = []
    seen = set()
    for path in glob.glob(os.path.join(base_dir, "**", "*.java"), recursive=True):
        s = open(path, encoding="utf-8").read()
        m = re.search(r'@TableName\s*\(\s*(?:value\s*=\s*)?"([^"]+)"', s)
        if not m:
            continue
        table = m.group(1)
        if os.path.exists(os.path.join(REPO, "sql", "mysql", f"{table}.sql")):
            continue  # 已有独立建表文件（如 ai_prompt_template.sql）
        if table in seen:
            continue
        seen.add(table)
        # 仅取本文件内的字段（避免跨文件误合并）
        r = parse_file(path)
        if r:
            results.append(r)
    results.sort(key=lambda x: x[0])
    ddls = "\n\n".join(emit(t, f) for t, f in results)
    ref = (f"-- ======================== {module} 生产建表脚本（自动生成，上线前需在真实 MySQL 冒烟验证） ========================\n\n"
           + ddls + "\n")
    os.makedirs(os.path.dirname(ref_path), exist_ok=True)
    with open(ref_path, "w", encoding="utf-8") as f:
        f.write(ref)
    fly = (f"-- ============================================================\n"
           f"-- V{args.ver}: {module} 模块建表（补齐 Flyway 未纳管的表）\n"
           f"-- 兼容性：完全新增，不影响历史数据\n"
           f"-- 幂等性：使用 CREATE TABLE IF NOT EXISTS\n"
           f"-- ============================================================\n\n" + ddls + "\n")
    os.makedirs(MIG_DIR, exist_ok=True)
    with open(fly_path, "w", encoding="utf-8") as f:
        f.write(fly)
    print(f"[{module}] tables={len(results)} -> ref={ref_path}\n            -> flyway={fly_path}")
    for t, flds in results:
        print(f"  {t} cols={len(flds)}")

if __name__ == "__main__":
    main()
