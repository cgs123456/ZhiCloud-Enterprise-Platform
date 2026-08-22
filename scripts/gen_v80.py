#!/usr/bin/env python3
"""
Final V80 generator:
1. Parse DDL from all migrations + sql/mysql (handles both `) COMMENT=` and `) ENGINE=` endings).
2. Scan DOs for fields whose snake_case column is missing AND no @TableField annotation.
3. Curated RENAME_MAP: DO-expected column <- existing DDL column (semantic rename + data migration).
4. Emit V80 with idempotent stored procedure + CALLs + UPDATE data migrations.
"""
import re, os, glob

BASE = r"D:/Desktop/yudao"
OUT = f"{BASE}/yudao-server/src/main/resources/db/migration/V82__fix_do_column_mappings.sql"

def c2u(n):
    s1 = re.sub(r'(.)([A-Z][a-z]+)', r'\1_\2', n)
    return re.sub(r'([a-z0-9])([A-Z])', r'\1_\2', s1).lower()

# ── 1. Parse DDL (robust: capture until line starting with ')') ─────────────
def parse_ddl():
    tables = {}
    for pat in [f"{BASE}/yudao-server/src/main/resources/db/migration/V*.sql", f"{BASE}/sql/mysql/*.sql"]:
        for fp in sorted(glob.glob(pat)):
            txt = open(fp, encoding="utf-8", errors="ignore").read()
            # CREATE TABLE form
            for m in re.finditer(r'CREATE TABLE IF NOT EXISTS `?(\w+)`?\s*\((.*?)\n\)', txt, re.DOTALL):
                tbl = m.group(1)
                cols = set(re.findall(r'^\s*`?(\w+)`?\s+(?:BIGINT|INT|TINYINT|SMALLINT|VARCHAR|DECIMAL|DATETIME|DATE|BIT|TEXT|JSON|TIME|FLOAT|DOUBLE)', m.group(2), re.MULTILINE))
                tables.setdefault(tbl, set()).update(cols)
            # ALTER TABLE ADD COLUMN form (direct statements)
            for m in re.finditer(r'ALTER\s+TABLE\s+`?(\w+)`?\s+ADD\s+(?:COLUMN\s+)?`?(\w+)`?\s+(?:BIGINT|INT|TINYINT|SMALLINT|VARCHAR|DECIMAL|DATETIME|DATE|BIT|TEXT|JSON|TIME|FLOAT|DOUBLE)', txt, re.IGNORECASE):
                tables.setdefault(m.group(1), set()).add(m.group(2))
            # ALTER TABLE inside dynamic SQL (CONCAT('ALTER TABLE `', tbl, '` ADD COLUMN `', col, '` ...'))
            # handled by CALL-site analysis below
    return tables

def parse_call_sites():
    """Parse CALL p_xxx_add_column('tbl','col',...) sites from migrations."""
    calls = set()
    for pat in [f"{BASE}/yudao-server/src/main/resources/db/migration/V*.sql", f"{BASE}/sql/mysql/*.sql"]:
        for fp in sorted(glob.glob(pat)):
            txt = open(fp, encoding="utf-8", errors="ignore").read()
            for m in re.finditer(r"CALL\s+\w*\w*add_column\w*\s*\(\s*'(\w+)'\s*,\s*'(\w+)'", txt, re.IGNORECASE):
                calls.add((m.group(1), m.group(2)))
            # also CONCAT-based dynamic SQL with literal table/column names
            for m in re.finditer(r"ALTER\s+TABLE\s+`(\w+)`\s+ADD\s+(?:COLUMN\s+)?`(\w+)`", txt, re.IGNORECASE):
                calls.add((m.group(1), m.group(2)))
    return calls

ddl = parse_ddl()
call_cols = parse_call_sites()
for tbl, col in call_cols:
    ddl.setdefault(tbl, set()).add(col)
print(f"DDL tables parsed: {len(ddl)}")

# ── 2. Semantic rename map: (table, new_col) -> old_col ─────────────────────
RENAME = {
    # *_items 明细表：master_id -> 语义化父单 ID
    ('erp_purchase_order_items', 'order_id'): 'master_id',
    ('erp_purchase_order_items', 'count'): 'purchase_count',
    ('erp_purchase_order_items', 'product_price'): 'purchase_price',
    ('erp_purchase_in_items', 'in_id'): 'master_id',
    ('erp_purchase_in_items', 'count'): 'purchase_count',
    ('erp_purchase_in_items', 'product_price'): 'purchase_price',
    ('erp_purchase_return_items', 'return_id'): 'master_id',
    ('erp_purchase_return_items', 'count'): 'purchase_count',
    ('erp_purchase_return_items', 'product_price'): 'purchase_price',
    ('erp_sale_order_items', 'order_id'): 'master_id',
    ('erp_sale_order_items', 'count'): 'sale_count',
    ('erp_sale_order_items', 'product_price'): 'sale_price',
    ('erp_sale_out_items', 'out_id'): 'master_id',
    ('erp_sale_out_items', 'count'): 'sale_count',
    ('erp_sale_out_items', 'product_price'): 'sale_price',
    ('erp_sale_return_items', 'return_id'): 'master_id',
    ('erp_sale_return_items', 'count'): 'sale_count',
    ('erp_sale_return_items', 'product_price'): 'sale_price',
    ('erp_stock_in_item', 'in_id'): 'master_id',
    ('erp_stock_out_item', 'out_id'): 'master_id',
    ('erp_stock_move_item', 'move_id'): 'master_id',
    ('erp_stock_check_item', 'check_id'): 'master_id',
    ('erp_stock_check_item', 'count'): 'diff_count',
    # 财务明细：master_id/bill_type/bill_id -> 语义化
    ('erp_finance_payment_item', 'payment_id'): 'master_id',
    ('erp_finance_payment_item', 'biz_type'): 'bill_type',
    ('erp_finance_payment_item', 'biz_id'): 'bill_id',
    ('erp_finance_receipt_item', 'receipt_id'): 'master_id',
    ('erp_finance_receipt_item', 'biz_type'): 'bill_type',
    ('erp_finance_receipt_item', 'biz_id'): 'bill_id',
    # 财务主表：finance_time -> 语义化
    ('erp_finance_payment', 'payment_time'): 'finance_time',
    ('erp_finance_receipt', 'receipt_time'): 'finance_time',
    # 产品：barcode/spec -> bar_code/standard（与上游 yudao DO 对齐）
    ('erp_product', 'bar_code'): 'barcode',
    ('erp_product', 'standard'): 'spec',
    # 账户：code/default_flag -> no/default_status
    ('erp_account', 'no'): 'code',
    ('erp_account', 'default_status'): 'default_flag',
    # 库存流水：bill_id/bill_item_id -> biz_id/biz_item_id
    ('erp_stock_record', 'biz_id'): 'bill_id',
    ('erp_stock_record', 'biz_item_id'): 'bill_item_id',
    # 供应商/客户：phone -> telephone
    ('erp_supplier', 'telephone'): 'phone',
    ('erp_customer', 'telephone'): 'phone',
}

# ── 3. Scan DOs ──────────────────────────────────────────────────────────────
TYPE_MAP = {'Long':'BIGINT','Integer':'INT','Short':'SMALLINT','Byte':'TINYINT',
            'String':'VARCHAR(255)','BigDecimal':'DECIMAL(20,4)','Boolean':'TINYINT(1)',
            'LocalDateTime':'DATETIME','LocalDate':'DATE','LocalTime':'TIME',
            'Float':'FLOAT','Double':'DOUBLE'}

missing = {}  # tbl -> list of (field, col, sql_type, java_file)

for mod in ['yudao-module-erp', 'yudao-module-wms', 'yudao-module-crm', 'yudao-module-qms']:
    src = f"{BASE}/{mod}/src/main/java"
    for root, dirs, files in os.walk(src):
        for f in files:
            if not f.endswith('DO.java'):
                continue
            fp = os.path.join(root, f)
            txt = open(fp, encoding="utf-8").read()
            tm = re.search(r'@TableName\("(\w+)"\)', txt)
            if not tm:
                continue
            tbl = tm.group(1)
            if tbl not in ddl:
                continue
            # fields with annotation check
            lines = txt.split('\n')
            for i, line in enumerate(lines):
                m = re.match(r'\s*private\s+(\w+(?:<[^>]+>)?)\s+(\w+)\s*;', line)
                if not m:
                    continue
                ftype, fname = m.group(1), m.group(2)
                # skip if @TableField already present within 6 lines above
                skip = False
                for j in range(i-1, max(i-7, -1), -1):
                    if lines[j].strip().startswith('@TableField'):
                        skip = True
                        break
                if skip:
                    continue
                col = c2u(fname)
                if col in ddl[tbl] or fname in ddl[tbl]:
                    continue
                base = re.sub(r'<.*>', '', ftype).strip()
                sql_t = TYPE_MAP.get(base, 'VARCHAR(255)')
                missing.setdefault(tbl, []).append((fname, col, sql_t, f))

# ── 4. Emit V80 ──────────────────────────────────────────────────────────────
adds, renames = [], []
for tbl in sorted(missing):
    for fname, col, sql_t, srcfile in missing[tbl]:
        key = (tbl, col)
        if key in RENAME and RENAME[key] in ddl.get(tbl, set()):
            renames.append((tbl, col, sql_t, RENAME[key]))
        else:
            adds.append((tbl, col, sql_t))

print(f"Semantic renames (add + copy data): {len(renames)}")
for t, c, s, o in renames:
    print(f"  {t}.{c} <- {o}")
print(f"Plain new columns: {len(adds)}")

with open(OUT, 'w', encoding='utf-8') as f:
    f.write("""-- ============================================================
-- V80: 修复 DO 实体字段与 DDL 列名不匹配（运行时 Unknown column 根因）
--
-- 背景：Java DO（业务契约，字段如 orderId/count/productPrice）与 V10 等
-- 早期迁移脚本的列名（master_id/purchase_count/purchase_price）系统性漂移，
-- MyBatis-Plus 按 camelCase→snake_case 推导列名，导致所有 CRUD 抛
-- SQLException: Unknown column。
--
-- 策略：以 Java DO 为真相源，对齐数据库列：
--   1) 语义重命名：新增语义化列并从旧列迁移数据（旧列保留不删，兼容存量读法）
--   2) 真缺失列：幂等新增
-- 全部通过存储过程 + information_schema 校验实现幂等，可安全重复执行。
-- 生成：scripts/gen_v80.py（2026-08-23）
-- ============================================================

DROP PROCEDURE IF EXISTS p_v80_add_column;
DELIMITER $$
CREATE PROCEDURE p_v80_add_column(IN p_table VARCHAR(64), IN p_column VARCHAR(64), IN p_definition VARCHAR(500))
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables
               WHERE table_schema = DATABASE() AND table_name = p_table)
       AND NOT EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema = DATABASE() AND table_name = p_table AND column_name = p_column) THEN
        SET @v80_sql = CONCAT('ALTER TABLE `', p_table, '` ADD COLUMN `', p_column, '` ', p_definition);
        PREPARE v80_stmt FROM @v80_sql;
        EXECUTE v80_stmt;
        DEALLOCATE PREPARE v80_stmt;
    END IF;
END$$
DELIMITER ;

-- ============================================================
-- 1. 语义重命名列（新增列 + 从旧列复制数据）
-- ============================================================
""")
    cur = None
    for tbl, col, sql_t, old in renames:
        if tbl != cur:
            f.write(f"\n-- {tbl}\n")
            cur = tbl
        f.write(f"CALL p_v80_add_column('{tbl}', '{col}', '{sql_t} DEFAULT NULL COMMENT ''{col}（原列 {old}）''');\n")
    f.write("\n-- 数据迁移：旧列值复制到新列（仅新列为空时）\n")
    for tbl, col, sql_t, old in renames:
        f.write(f"UPDATE `{tbl}` SET `{col}` = `{old}` WHERE `{col}` IS NULL AND `{old}` IS NOT NULL;\n")

    f.write("""
-- ============================================================
-- 2. 真缺失列（幂等新增）
-- ============================================================
""")
    cur = None
    for tbl, col, sql_t in adds:
        if tbl != cur:
            f.write(f"\n-- {tbl}\n")
            cur = tbl
        f.write(f"CALL p_v80_add_column('{tbl}', '{col}', '{sql_t} DEFAULT NULL COMMENT ''{col}''');\n")

    f.write("""
DROP PROCEDURE IF EXISTS p_v80_add_column;
""")

print(f"\nV80 written: {OUT}")
print(f"  renames: {len(renames)} (with data migration)")
print(f"  adds: {len(adds)}")
