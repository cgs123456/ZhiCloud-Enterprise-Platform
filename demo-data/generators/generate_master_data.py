#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""确定性主数据生成器 —— 企业数字化中台演示/开发数据（ERP/WMS/TMS/CRM/HR）。

【边界声明 / Boundary】
1. 本脚本只产出**静态** SQL / JSON 文本，**不连接数据库、不执行任何 DML**。
   Docker daemon 不可用、目标库不可达时依然可运行；产物是否可执行未经实库验证。
2. 产出数据**仅限开发/测试/演示环境**。任何生产/预发环境导入都会污染真实台账，
   清理入口见 demo-data/cleanup/cleanup_demo.sql。
3. fail-closed：所用列必须存在于 schema 快照；快照中的 NOT NULL 列必须被覆盖；
   任一列缺失、主键区间溢出即刻抛错退出，绝不"静默补默认值"掩盖结构漂移。

【确定性 / Determinism】
- 唯一随机源为 random.Random(seed)，seed 默认 20260928；
- 禁止使用 hash()（受 PYTHONHASHSEED 随机化影响）；
- 主键/编码全部由 (tenant, 表序号, 行序) 固定推导，同一组参数多次运行字节一致。

用法：
    python demo-data/generators/generate_master_data.py
    python demo-data/generators/generate_master_data.py --scale large --tenant 9002
"""

from __future__ import annotations

import argparse
import hashlib
import json
import random
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
DEFAULT_SNAPSHOT = REPO_ROOT / "demo-data" / "schema" / "schema-snapshot.json"
DEFAULT_SQL_OUT = REPO_ROOT / "demo-data" / "sql" / "mysql" / "demo_master_data.sql"
DEFAULT_JSON_OUT = REPO_ROOT / "demo-data" / "schema" / "demo_master_data.json"
DEFAULT_SUMMARY = REPO_ROOT / "demo-data" / "schema" / "demo_master_data_summary.json"

# ------------------------------ 演示数据隔离常量 ------------------------------
DEFAULT_TENANT_ID = 9001      # demo 专属租户起点，禁止与真实租户混淆
DEFAULT_SEED = 20260928
CREATOR = "demo-seed"         # 审计列统一取值，便于 cleanup 定位
FIXED_TIME = "2026-01-01 00:00:00"
EMAIL_DOMAIN = "zhicloud.demo.invalid"    # RFC 2606 保留域，永不解析到真实邮箱
PHONE_BASE = 13800138000                  # 广为人知的号码占位区间，非真实号主
PHONE_BLOCK = 10000                       # 每类实体的号码块大小，避免跨实体撞号

# 主键分配：每租户 1,000,000 段，每表 1,000 段（单行表内最多 999 行）
ID_TENANT_STRIDE = 1000000
ID_TABLE_STRIDE = 1000
ID_START = 910000

# ------------------------------ 枚举取值（均引自迁移脚本或代码） ----------------
# ERP 主数据：V10__erp_ddl.sql  "状态（0 启用 1 停用）"
ERP_STATUS_ON = 0
# WMS 主数据：V11__wms_ddl.sql  "状态（0 启用 1 停用）"
WMS_STATUS_ON = 0
# WMS 库区/库位：V72__wms_zone_location_tables.sql
WMS_ZONE_TYPE = {10: "存储区", 20: "拣货区", 30: "退货区"}
WMS_LOCATION_TYPE = {10: "储位", 20: "拣货位", 30: "收货位", 40: "发货位"}
WMS_LOCATION_STATUS_FREE = 10             # 10 空闲 / 20 占用 / 30 锁定 / 40 禁用
# TMS：V58__tms_init.sql —— 注意 TMS 用 10/20/30，不是 0/1
TMS_CARRIER_SERVICE_TYPE = (10, 20, 30)  # 10 快递 / 20 零担 / 30 整车
TMS_STATUS_ON = 10                        # 承运商：10 启用 / 20 停用
TMS_VEHICLE_STATUS_FREE = 10              # 10 可用 / 20 运输中 / 30 维修中
TMS_DRIVER_STATUS_FREE = 10               # 10 可用 / 20 运输中 / 30 休假
# WMS 往来企业：V11__wms_ddl.sql "类型（1 供应商 2 客户 3 承运商）"
WMS_MERCHANT_TYPE = (1, 2, 3)
# HR：V83__hr_base_tables.sql
HR_DEPT_STATUS_ON = 10                    # 10 启用 / 20 禁用
HR_POSITION_LEVEL = (10, 20, 30, 40, 50)  # 10 初级 20 中级 30 高级 40 专家 50 管理
HR_GENDER = (10, 20)                      # 10 男 20 女
HR_EMP_STATUS_ON = 10                     # 10 在职 / 20 离职 / 30 停薪
HR_EMPLOYMENT_TYPE = (10, 20, 30, 40)     # 10 全职 20 兼职 30 实习 40 外包
# CRM：CrmCustomerLevelEnum（zhicloud-module-crm）
CRM_LEVEL = (1, 2, 3)                     # 1 A重点 2 B普通 3 C非优先
# crm_customer.version：V65__crm_optimistic_lock.sql DEFAULT 0，MyBatis-Plus 乐观锁
CRM_VERSION_INIT = 0

# 未核实语义、刻意留空或保守取值的字段（写入 summary，不做猜测填充）
UNVERIFIED_FIELDS = [
    "crm_customer.source（迁移脚本与代码均未定位到枚举定义，留 NULL）",
    "crm_customer.industry_id（同上，留 NULL）",
    "crm_customer.area_id（同上，留 NULL）",
    "crm_customer.owner_user_id（绑定真实用户会与账号体系耦合，留 NULL）",
    "tms_vehicle.driver_user_id（未确认是 system 用户 ID 还是 tms_driver ID，留 NULL）",
    "hr_employee.id_card（不生成可通过校验规则的证件号，避免伪造身份标识，留 NULL）",
    "hr_department.leader_id（需与 hr_employee 绑定，主数据阶段留 NULL）",
]

# 生成顺序即外键依赖顺序（先被引用的先落盘）
TABLE_ORDER = [
    "erp_product_category", "erp_product_unit", "erp_product",
    "erp_customer", "erp_supplier", "erp_warehouse",
    "wms_warehouse", "wms_zone", "wms_location",
    "wms_item_category", "wms_item", "wms_merchant",
    "crm_customer",
    "hr_department", "hr_position", "hr_employee",
    "tms_carrier", "tms_driver", "tms_vehicle",
]

# 规模档位：doc 07 §5.1 建议量级的缩放倍数（仓库/库区/库位按结构推导，不随倍数放大）
SCALE_FACTOR = {"small": 1, "medium": 5, "large": 20}


class FailClosedError(Exception):
    """结构性校验失败：一律中断，绝不降级补默认值。"""


# ------------------------------ 快照加载与校验 ------------------------------
def load_snapshot(path: Path) -> dict:
    if not path.exists():
        raise FailClosedError("schema 快照不存在：%s（请先运行 schema_snapshot.py）" % path)
    data = json.loads(path.read_bytes().decode("utf-8"))
    tables = data.get("tables")
    if not isinstance(tables, dict) or not tables:
        raise FailClosedError("schema 快照格式异常：tables 为空 —— %s" % path)
    return data


def column_index(snapshot: dict, table: str) -> dict:
    entry = snapshot["tables"].get(table)
    if entry is None:
        raise FailClosedError("表 %s 不存在于 schema 快照（禁止为不存在的表造数据）" % table)
    return {c["name"]: c for c in entry["columns"]}


# ------------------------------ 字面量渲染 ------------------------------
def sql_escape(value: str) -> str:
    return value.replace("\\", "\\\\").replace("'", "''")


def render_value(column: dict, value) -> str:
    """按快照中的列类型渲染 MySQL 字面量。BIT(1) 必须用 b'0'/b'1'。"""
    if value is None:
        return "NULL"
    col_type = (column.get("type") or "").upper()
    if isinstance(value, bool):
        value = 1 if value else 0
    if isinstance(value, (int, float)):
        if col_type.startswith("BIT"):
            return "b'1'" if int(value) else "b'0'"
        return str(value)
    if isinstance(value, str):
        return "'" + sql_escape(value) + "'"
    raise FailClosedError("不支持的值类型 %r（列 %s）" % (type(value), column.get("name")))


def with_audit(row: dict, cols: dict, tenant: int) -> dict:
    """补齐审计列与租户列；仅当该列真实存在于快照时才补。"""
    out = dict(row)
    out.setdefault("creator", CREATOR)
    out.setdefault("create_time", FIXED_TIME)
    out.setdefault("updater", CREATOR)
    out.setdefault("update_time", FIXED_TIME)
    if "deleted" in cols:
        out.setdefault("deleted", 0)          # BIT(1) → b'0'；TINYINT(1) → 0
    if "tenant_id" in cols:
        out.setdefault("tenant_id", tenant)
    return out


def validate_rows(table: str, cols: dict, rows: list) -> list:
    """fail-closed：未知列 / 遗漏 NOT NULL 列 / 列顺序统一。"""
    order = list(cols.keys())
    required = [n for n, c in cols.items() if c.get("nullable") is False]
    normalized = []
    for idx, row in enumerate(rows):
        unknown = [k for k in row if k not in cols]
        if unknown:
            raise FailClosedError(
                "表 %s 第 %d 行使用了快照中不存在的列：%s" % (table, idx + 1, ", ".join(sorted(unknown))))
        missing = [n for n in required if n not in row]
        if missing:
            raise FailClosedError(
                "表 %s 第 %d 行未覆盖 NOT NULL 列：%s" % (table, idx + 1, ", ".join(sorted(missing))))
        normalized.append({k: row.get(k) for k in order})
    return normalized


# ------------------------------ 主键分配 ------------------------------
def make_id_allocator(tenant: int):
    """按 (租户段, 表序号, 行序) 分配主键，保证跨表/跨租户不撞号。"""
    tenant_base = ID_START + (tenant - DEFAULT_TENANT_ID) * ID_TENANT_STRIDE
    table_slot = {t: i for i, t in enumerate(TABLE_ORDER)}

    def alloc(table: str, count: int) -> list:
        if table not in table_slot:
            raise FailClosedError("表 %s 未登记于 TABLE_ORDER" % table)
        if count <= 0:
            return []
        if count >= ID_TABLE_STRIDE:
            raise FailClosedError(
                "表 %s 行数 %d 超出单表主键区间 %d，请降低 --scale 或扩大 ID_TABLE_STRIDE"
                % (table, count, ID_TABLE_STRIDE))
        base = tenant_base + table_slot[table] * ID_TABLE_STRIDE
        return [base + i for i in range(count)]

    return alloc


# ------------------------------ 数据集构建 ------------------------------
def build_dataset(snapshot: dict, seed: int, tenant: int, scale: str) -> list:
    """返回 [{'table': 表名, 'rows': [列序化 dict]}...]，顺序按 TABLE_ORDER。"""
    if scale not in SCALE_FACTOR:
        raise FailClosedError("未知规模档位 %r，可选：%s" % (scale, ", ".join(sorted(SCALE_FACTOR))))
    s = SCALE_FACTOR[scale]
    rnd = random.Random(seed)              # 唯一随机源，固定 seed → 可复现
    alloc = make_id_allocator(tenant)
    dataset = []

    def emit(table: str, rows: list):
        cols = column_index(snapshot, table)
        rows = [with_audit(r, cols, tenant) for r in rows]
        dataset.append({"table": table, "rows": validate_rows(table, cols, rows)})

    # --- ERP 产品分类（8，固定）
    cats = [
        ("DEMO_PC01", "DEMO_原材料", None),
        ("DEMO_PC02", "DEMO_电子元器件", 0),
        ("DEMO_PC03", "DEMO_结构件", 0),
        ("DEMO_PC04", "DEMO_半成品", None),
        ("DEMO_PC05", "DEMO_成品", None),
        ("DEMO_PC06", "DEMO_包装材料", None),
        ("DEMO_PC07", "DEMO_耗材", None),
        ("DEMO_PC08", "DEMO_委外加工件", None),
    ]
    cat_ids = alloc("erp_product_category", len(cats))
    emit("erp_product_category", [
        {"id": cat_ids[i], "parent_id": (0 if parent is None else cat_ids[parent]),
         "code": code, "name": name, "sort": (i + 1) * 10, "status": ERP_STATUS_ON}
        for i, (code, name, parent) in enumerate(cats)])

    # --- ERP 计量单位（8，固定）
    unit_names = ["DEMO_个", "DEMO_台", "DEMO_千克", "DEMO_米", "DEMO_平方米", "DEMO_箱", "DEMO_套", "DEMO_升"]
    unit_ids = alloc("erp_product_unit", len(unit_names))
    emit("erp_product_unit", [
        {"id": unit_ids[i], "name": nm, "sort": (i + 1) * 10, "status": ERP_STATUS_ON}
        for i, nm in enumerate(unit_names)])

    # --- ERP 产品（24 × s）
    n_product = 24 * s
    product_ids = alloc("erp_product", n_product)
    product_rows = []
    for i in range(n_product):
        cost = round(rnd.uniform(5.0, 800.0), 2)
        purchase = round(cost * 1.15, 2)
        product_rows.append({
            "id": product_ids[i],
            "category_id": cat_ids[i % len(cat_ids)],
            "unit_id": unit_ids[i % len(unit_ids)],
            "code": "DEMO_P%04d" % (i + 1),
            "name": "DEMO_物料-%04d" % (i + 1),
            "barcode": "DEMOBAR%08d" % (i + 1),
            "spec": "DEMO-SPEC-%02d" % (i % 6 + 1),
            "sale_price": round(purchase * 1.35, 2),
            "purchase_price": purchase,
            "cost_price": cost,
            "status": ERP_STATUS_ON,
        })
    emit("erp_product", product_rows)

    # --- ERP 客户（12 × s）
    n_cust = 12 * s
    cust_ids = alloc("erp_customer", n_cust)
    credit_limits = (500000.0, 300000.0, 200000.0, 100000.0)
    credit_ratio = (0.10, 0.25, 0.40, 0.60)
    emit("erp_customer", [
        {"id": cust_ids[i],
         "code": "DEMO_CUS%05d" % (i + 1),
         "name": "DEMO_客户-%05d" % (i + 1),
         "contact": "DEMO_联系人-%04d" % (i + 1),
         "mobile": str(PHONE_BASE + 0 * PHONE_BLOCK + i),
         "phone": "DEMO-021-0000%04d" % (i + 1),
         "email": "customer%05d@%s" % (i + 1, EMAIL_DOMAIN),
         "address": "DEMO_示例市示例区示例路%05d号" % (i + 1),
         "bank_name": "DEMO_示例银行-分行%02d" % (i % 4 + 1),
         "bank_account": "DEMO-ACCT-%06d" % (i + 1),
         "status": ERP_STATUS_ON,
         "credit_limit": credit_limits[i % len(credit_limits)],
         "used_credit": round(credit_limits[i % len(credit_limits)] * credit_ratio[i % len(credit_ratio)], 4),
         "credit_period": (30, 45, 60)[i % 3]}
        for i in range(n_cust)])

    # --- ERP 供应商（10 × s）
    n_supp = 10 * s
    supp_ids = alloc("erp_supplier", n_supp)
    emit("erp_supplier", [
        {"id": supp_ids[i],
         "code": "DEMO_SUP%05d" % (i + 1),
         "name": "DEMO_供应商-%05d" % (i + 1),
         "contact": "DEMO_供应商联系人-%04d" % (i + 1),
         "mobile": str(PHONE_BASE + 1 * PHONE_BLOCK + i),
         "phone": "DEMO-021-0100%04d" % (i + 1),
         "email": "supplier%05d@%s" % (i + 1, EMAIL_DOMAIN),
         "address": "DEMO_示例市工业区示例大道%05d号" % (i + 1),
         "bank_name": "DEMO_示例银行-分行%02d" % (i % 4 + 1),
         "bank_account": "DEMO-SACCT-%06d" % (i + 1),
         "status": ERP_STATUS_ON}
        for i in range(n_supp)])

    # --- ERP 仓库（4，固定）
    erp_wh_names = ["DEMO_原材料仓", "DEMO_半成品仓", "DEMO_成品仓", "DEMO_不良品仓"]
    erp_wh_ids = alloc("erp_warehouse", len(erp_wh_names))
    emit("erp_warehouse", [
        {"id": erp_wh_ids[i], "code": "DEMO_EWH%02d" % (i + 1), "name": nm,
         "address": "DEMO_示例市物流园%02d 号库" % (i + 1), "sort": (i + 1) * 10,
         "status": ERP_STATUS_ON}
        for i, nm in enumerate(erp_wh_names)])

    # --- WMS 仓库（4，固定）
    wms_wh_names = ["DEMO_华东中心仓", "DEMO_华南中心仓", "DEMO_华北中心仓", "DEMO_西南中转仓"]
    wms_wh_ids = alloc("wms_warehouse", len(wms_wh_names))
    emit("wms_warehouse", [
        {"id": wms_wh_ids[i], "code": "DEMO_WWH%02d" % (i + 1), "name": nm,
         "remark": "DEMO 数据：演示用仓库，勿与真实仓混淆", "sort": (i + 1) * 10}
        for i, nm in enumerate(wms_wh_names)])

    # --- WMS 库区（4 仓 × 3 类型）
    zone_rows, zone_meta = [], []
    n_zone = len(wms_wh_ids) * len(WMS_ZONE_TYPE)
    zone_ids = alloc("wms_zone", n_zone)
    k = 0
    for wi, wh_id in enumerate(wms_wh_ids):
        for zt, zt_name in sorted(WMS_ZONE_TYPE.items()):
            code = "DEMO_Z%02d%02d" % (wi + 1, zt)
            zone_rows.append({"id": zone_ids[k], "warehouse_id": wh_id, "code": code,
                              "name": "DEMO_%s-%s" % (wms_wh_names[wi], zt_name),
                              "type": zt, "remark": "DEMO 库区",
                              "sort": (wi + 1) * 100 + zt})
            zone_meta.append((zone_ids[k], code, wh_id))
            k += 1
    emit("wms_zone", zone_rows)

    # --- WMS 库位（每库区 4 个：储位/拣货位/收货位/发货位）
    loc_rows = []
    n_loc = n_zone * len(WMS_LOCATION_TYPE)
    loc_ids = alloc("wms_location", n_loc)
    k = 0
    for z_id, z_code, wh_id in zone_meta:
        for lt in sorted(WMS_LOCATION_TYPE):
            loc_rows.append({
                "id": loc_ids[k], "zone_id": z_id, "warehouse_id": wh_id,
                "code": "%s-L%02d" % (z_code, lt),
                "name": "DEMO_%s-%s%02d" % (z_code, WMS_LOCATION_TYPE[lt], lt),
                "barcode": "DEMOLC%08d" % (k + 1),
                "type": lt,
                "capacity_weight": round(1000.0 + 100.0 * (k % 10), 4),
                "capacity_volume": round(50.0 + 5.0 * (k % 10), 4),
                "status": WMS_LOCATION_STATUS_FREE,   # 10 空闲（新库未占用）
                "remark": "DEMO 库位", "sort": k + 1,
            })
            k += 1
    emit("wms_location", loc_rows)

    # --- WMS 物料分类（6，固定）
    wms_cats = ["DEMO_IC01", "DEMO_IC02", "DEMO_IC03", "DEMO_IC04", "DEMO_IC05", "DEMO_IC06"]
    wms_cat_ids = alloc("wms_item_category", len(wms_cats))
    emit("wms_item_category", [
        {"id": wms_cat_ids[i], "parent_id": 0, "code": code,
         "name": "DEMO_仓储分类-%02d" % (i + 1), "sort": (i + 1) * 10, "status": WMS_STATUS_ON}
        for i, code in enumerate(wms_cats)])

    # --- WMS 物料（20 × s）
    n_item = 20 * s
    item_ids = alloc("wms_item", n_item)
    emit("wms_item", [
        {"id": item_ids[i],
         "code": "DEMO_WI%05d" % (i + 1),
         "name": "DEMO_物料-%05d" % (i + 1),
         "unit": unit_names[i % len(unit_names)].replace("DEMO_", ""),
         "category_id": wms_cat_ids[i % len(wms_cat_ids)],
         "remark": "DEMO 仓储物料，与 erp_product 同名便于跨模块联调",
         "abc_classification": ("A", "B", "C")[i % 3]}
        for i in range(n_item)])

    # --- WMS 往来企业（8 × s）
    n_merchant = 8 * s
    merchant_ids = alloc("wms_merchant", n_merchant)
    emit("wms_merchant", [
        {"id": merchant_ids[i],
         "code": "DEMO_MER%05d" % (i + 1),
         "name": "DEMO_往来企业-%05d" % (i + 1),
         "type": WMS_MERCHANT_TYPE[i % len(WMS_MERCHANT_TYPE)],
         "level": ("A", "B", "C")[i % 3],
         "bank_name": "DEMO_示例银行-分行%02d" % (i % 4 + 1),
         "bank_account": "DEMO-MACCT-%06d" % (i + 1),
         "address": "DEMO_示例市示例区示例大道%05d号" % (i + 1),
         "mobile": str(PHONE_BASE + 2 * PHONE_BLOCK + i),
         "telephone": "DEMO-021-0200%04d" % (i + 1),
         "contact": "DEMO_往来联系人-%04d" % (i + 1),
         "email": "merchant%05d@%s" % (i + 1, EMAIL_DOMAIN),
         "remark": "DEMO 往来企业"}
        for i in range(n_merchant)])

    # --- CRM 客户（15 × s）
    n_crm = 15 * s
    crm_ids = alloc("crm_customer", n_crm)
    emit("crm_customer", [
        {"id": crm_ids[i],
         "name": "DEMO_CRM客户-%05d" % (i + 1),
         "follow_up_status": 0,                       # BIT(1) → b'0' 未跟进
         "contact_last_time": None,
         "contact_last_content": None,
         "contact_next_time": None,
         "owner_user_id": None,                       # 见 UNVERIFIED_FIELDS
         "owner_time": None,
         "lock_status": 0,
         "deal_status": 0,
         "mobile": str(PHONE_BASE + 3 * PHONE_BLOCK + i),
         "telephone": "DEMO-021-0300%04d" % (i + 1),
         "qq": "DEMO_1000%05d" % (i + 1),
         "wechat": "DEMO_wx_%05d" % (i + 1),
         "email": "crm%05d@%s" % (i + 1, EMAIL_DOMAIN),
         "area_id": None,
         "detail_address": "DEMO_示例市示例区示例路%05d号" % (i + 1),
         "industry_id": None,
         "level": CRM_LEVEL[i % len(CRM_LEVEL)],
         "source": None,
         "remark": "DEMO CRM 客户（枚举未核实字段已留 NULL）",
         "tag_ids": None,
         "version": CRM_VERSION_INIT}
        for i in range(n_crm)])

    # --- HR 部门（8，固定：4 根 + 4 子）
    dept_defs = [
        ("DEMO_D01", "DEMO_研发中心", None),
        ("DEMO_D02", "DEMO_生产制造部", None),
        ("DEMO_D03", "DEMO_供应链部", None),
        ("DEMO_D04", "DEMO_职能支持部", None),
        ("DEMO_D0101", "DEMO_平台研发组", 0),
        ("DEMO_D0102", "DEMO_质量工程组", 0),
        ("DEMO_D0201", "DEMO_装配车间", 1),
        ("DEMO_D0301", "DEMO_仓储物流组", 2),
    ]
    dept_ids = alloc("hr_department", len(dept_defs))
    emit("hr_department", [
        {"id": dept_ids[i], "parent_id": (0 if parent is None else dept_ids[parent]),
         "code": code, "name": name, "leader_id": None,
         "status": HR_DEPT_STATUS_ON, "remark": "DEMO 组织", "sort": (i + 1) * 10}
        for i, (code, name, parent) in enumerate(dept_defs)])

    # --- HR 岗位（10，固定）
    pos_names = ["DEMO_Java工程师", "DEMO_前端工程师", "DEMO_测试工程师", "DEMO_工艺工程师",
                 "DEMO_装配技工", "DEMO_仓储主管", "DEMO_采购专员", "DEMO_质量工程师",
                 "DEMO_人事专员", "DEMO_财务专员"]
    pos_ids = alloc("hr_position", len(pos_names))
    emit("hr_position", [
        {"id": pos_ids[i], "code": "DEMO_POS%02d" % (i + 1), "name": nm,
         "dept_id": dept_ids[i % len(dept_ids)],
         "level": HR_POSITION_LEVEL[i % len(HR_POSITION_LEVEL)],
         "base_salary": round(6000.0 + 1500.0 * (i % 6), 2),
         "remark": "DEMO 岗位", "sort": (i + 1) * 10}
        for i, nm in enumerate(pos_names)])

    # --- HR 员工（40 × s）
    n_emp = 40 * s
    emp_ids = alloc("hr_employee", n_emp)
    emit("hr_employee", [
        {"id": emp_ids[i],
         "emp_no": "DEMO_EMP%05d" % (i + 1),
         "name": "DEMO_员工-%05d" % (i + 1),
         "gender": HR_GENDER[i % len(HR_GENDER)],
         "birth_date": "19%02d-%02d-%02d" % (80 + (i % 20), (i % 12) + 1, (i % 28) + 1),
         "id_card": None,                              # 见 UNVERIFIED_FIELDS
         "phone": str(PHONE_BASE + 4 * PHONE_BLOCK + i),
         "email": "emp%05d@%s" % (i + 1, EMAIL_DOMAIN),
         "dept_id": dept_ids[i % len(dept_ids)],
         "position_id": pos_ids[i % len(pos_ids)],
         "hire_date": "20%02d-%02d-%02d" % (18 + (i % 8), (i % 12) + 1, (i % 28) + 1),
         "leave_date": None,
         "status": HR_EMP_STATUS_ON,
         "employment_type": HR_EMPLOYMENT_TYPE[i % len(HR_EMPLOYMENT_TYPE)],
         "remark": "DEMO 员工（证件号刻意留空）", "sort": (i + 1) * 10}
        for i in range(n_emp)])

    # --- TMS 承运商（6 × s）
    carrier_names = ["DEMO_顺达快递", "DEMO_华通零担", "DEMO_安运整车",
                     "DEMO_捷达快运", "DEMO_中远物流", "DEMO_京广运输"]
    n_carrier = len(carrier_names) * s
    carrier_ids = alloc("tms_carrier", n_carrier)
    emit("tms_carrier", [
        {"id": carrier_ids[i], "name": "%s-%03d" % (carrier_names[i % len(carrier_names)], i + 1),
         "code": "DEMO_TC%05d" % (i + 1),              # uk_tenant_code 唯一
         "service_type": TMS_CARRIER_SERVICE_TYPE[i % len(TMS_CARRIER_SERVICE_TYPE)],
         "contact_person": "DEMO_承运联系人-%04d" % (i + 1),
         "contact_phone": str(PHONE_BASE + 5 * PHONE_BLOCK + i),
         "qualification_no": "DEMO-QUAL-%06d" % (i + 1),
         "rating": (i % 5) + 1,
         "status": TMS_STATUS_ON, "remark": "DEMO 承运商"}
        for i in range(n_carrier)])

    # --- TMS 司机（12 × s）
    n_driver = 12 * s
    driver_ids = alloc("tms_driver", n_driver)
    emit("tms_driver", [
        {"id": driver_ids[i], "name": "DEMO_司机-%05d" % (i + 1),
         "phone": str(PHONE_BASE + 6 * PHONE_BLOCK + i),
         "license_no": "DEMO-DL-%06d" % (i + 1),        # uk_tenant_license_no 唯一
         "license_type": ("A1", "A2", "B1", "B2", "C1")[i % 5],
         "carrier_id": carrier_ids[i % len(carrier_ids)],
         "status": TMS_DRIVER_STATUS_FREE, "remark": "DEMO 司机"}
        for i in range(n_driver)])

    # --- TMS 车辆（12 × s）
    n_vehicle = 12 * s
    vehicle_ids = alloc("tms_vehicle", n_vehicle)
    emit("tms_vehicle", [
        {"id": vehicle_ids[i], "plate_no": "DEMO-TEST-%05d" % (i + 1),  # uk_tenant_plate_no 唯一
         "vehicle_type": ("DEMO_厢式货车", "DEMO_平板车", "DEMO_冷藏车", "DEMO_集装箱车")[i % 4],
         "carrier_id": carrier_ids[i % len(carrier_ids)],
         "load_capacity": round(1000.0 + 500.0 * (i % 8), 4),
         "volume": round(20.0 + 5.0 * (i % 8), 4),
         "driver_user_id": None,                        # 见 UNVERIFIED_FIELDS
         "status": TMS_VEHICLE_STATUS_FREE, "remark": "DEMO 车辆"}
        for i in range(n_vehicle)])

    order = {t: i for i, t in enumerate(TABLE_ORDER)}
    dataset.sort(key=lambda d: order.get(d["table"], 999))
    return dataset


# ------------------------------ 产物渲染 ------------------------------
def render_sql(dataset: list, snapshot: dict, seed: int, tenant: int, scale: str,
               snapshot_path: Path) -> str:
    lines = [
        "-- ============================================================",
        "-- 演示/开发主数据：demo-data/sql/mysql/demo_master_data.sql",
        "-- 由 demo-data/generators/generate_master_data.py 自动生成",
        "-- 参数：seed=%d tenant=%d scale=%s" % (seed, tenant, scale),
        "-- 【禁止导入生产】全部数据位于 tenant_id=%d，名称/编码带 DEMO_ 前缀，" % tenant,
        "--   手机号使用占位区间，邮箱使用 RFC 2606 保留域 .invalid。",
        "--   生产导入会污染真实台账；清理脚本见 demo-data/cleanup/cleanup_demo.sql。",
        "-- 【未实库执行】本文件仅通过静态校验：列取自 schema 快照、NOT NULL 全覆盖；",
        "--   尚未在任何 MySQL 实例上执行，可执行性未经运行时验证。",
        "-- 快照来源：%s" % snapshot_path.name,
        "-- ============================================================",
        "",
        "BEGIN;",
        "",
    ]
    for item in dataset:
        table = item["table"]
        cols = column_index(snapshot, table)
        order = list(cols.keys())
        col_sql = ", ".join("`%s`" % c for c in order)
        lines.append("-- ----------------------------")
        lines.append("-- %s（%d 行）" % (table, len(item["rows"])))
        lines.append("-- ----------------------------")
        for row in item["rows"]:
            values = ", ".join(render_value(cols[c], row.get(c)) for c in order)
            lines.append("INSERT INTO `%s` (%s) VALUES (%s);" % (table, col_sql, values))
        lines.append("")
    lines.append("COMMIT;")
    lines.append("")
    return "\n".join(lines) + "\n"


def render_json(dataset: list, snapshot: dict, seed: int, tenant: int, scale: str,
                snapshot_path: Path) -> str:
    payload = {
        "generated_by": "demo-data/generators/generate_master_data.py",
        "seed": seed,
        "tenant_id": tenant,
        "scale": scale,
        "snapshot": str(snapshot_path.relative_to(REPO_ROOT)).replace("\\", "/"),
        "snapshot_payload_sha256": snapshot.get("payload_sha256"),
        "boundary": "dev/test/demo only —— 禁止导入生产",
        "unverified_fields": UNVERIFIED_FIELDS,
        "tables": [
            {"table": d["table"], "row_count": len(d["rows"]),
             "columns": list(column_index(snapshot, d["table"]).keys()),
             "rows": d["rows"]}
            for d in dataset],
    }
    return json.dumps(payload, ensure_ascii=False, indent=2) + "\n"


def sha256_text(text: str) -> str:
    return hashlib.sha256(text.encode("utf-8")).hexdigest()


def sha256_file(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def write_bytes(path: Path, text: str) -> str:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(text.encode("utf-8"))     # 字节写入，避免 Windows 换行翻译改变摘要
    return sha256_file(path)


def rel(path: Path) -> str:
    try:
        return str(path.relative_to(REPO_ROOT)).replace("\\", "/")
    except ValueError:
        return str(path)


# ------------------------------ 入口 ------------------------------
def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description="企业数字化中台演示主数据生成器（静态产物，不连库）")
    parser.add_argument("--snapshot", default=str(DEFAULT_SNAPSHOT), help="schema 快照 JSON 路径")
    parser.add_argument("--output", default=str(DEFAULT_SQL_OUT), help="MySQL INSERT 脚本输出路径")
    parser.add_argument("--json-out", default=str(DEFAULT_JSON_OUT), help="同内容 JSON 输出路径")
    parser.add_argument("--summary", default=str(DEFAULT_SUMMARY), help="manifest 摘要 JSON 输出路径")
    parser.add_argument("--seed", type=int, default=DEFAULT_SEED, help="随机种子（默认 20260928）")
    parser.add_argument("--tenant", type=int, default=DEFAULT_TENANT_ID, help="demo 租户 ID（默认 9001）")
    parser.add_argument("--scale", default="small", choices=sorted(SCALE_FACTOR),
                        help="数据规模档位：small/medium/large（默认 small）")
    parser.add_argument("--dialect", default="mysql", choices=["mysql"], help="目标方言（当前仅 mysql）")
    args = parser.parse_args(argv)

    snapshot_path = Path(args.snapshot)
    snapshot = load_snapshot(snapshot_path)
    dataset = build_dataset(snapshot, args.seed, args.tenant, args.scale)

    sql_text = render_sql(dataset, snapshot, args.seed, args.tenant, args.scale, snapshot_path)
    json_text = render_json(dataset, snapshot, args.seed, args.tenant, args.scale, snapshot_path)

    sql_path = Path(args.output)
    json_path = Path(args.json_out)
    sql_file_sha = write_bytes(sql_path, sql_text)
    json_file_sha = write_bytes(json_path, json_text)

    manifest = {
        "generated_by": "demo-data/generators/generate_master_data.py",
        "seed": args.seed,
        "tenant_id": args.tenant,
        "scale": args.scale,
        "dialect": args.dialect,
        "snapshot": rel(snapshot_path),
        "snapshot_payload_sha256": snapshot.get("payload_sha256"),
        "table_count": len(dataset),
        "row_total": sum(len(d["rows"]) for d in dataset),
        "tables": [{"table": d["table"], "rows": len(d["rows"])} for d in dataset],
        "unverified_fields": UNVERIFIED_FIELDS,
        "artifacts": {
            "sql": {"path": rel(sql_path), "payload_sha256": sha256_text(sql_text),
                    "file_sha256": sql_file_sha, "bytes": len(sql_text.encode("utf-8"))},
            "json": {"path": rel(json_path), "payload_sha256": sha256_text(json_text),
                     "file_sha256": json_file_sha, "bytes": len(json_text.encode("utf-8"))},
        },
        "status": "STATIC_ONLY —— 未在任何数据库执行过，可执行性未验证",
    }
    summary_path = Path(args.summary)
    summary_file_sha = write_bytes(summary_path, json.dumps(manifest, ensure_ascii=False, indent=2) + "\n")

    print("tenant=%d scale=%s tables=%d rows=%d"
          % (args.tenant, args.scale, manifest["table_count"], manifest["row_total"]))
    for t in manifest["tables"]:
        print("  %-24s %d" % (t["table"], t["rows"]))
    print("sql  payload_sha256=%s" % manifest["artifacts"]["sql"]["payload_sha256"])
    print("sql  file_sha256   =%s" % sql_file_sha)
    print("json file_sha256   =%s" % json_file_sha)
    print("manifest file_sha256=%s" % summary_file_sha)
    return 0


if __name__ == "__main__":
    try:
        sys.exit(main())
    except FailClosedError as exc:
        print("FAIL-CLOSED: %s" % exc, file=sys.stderr)
        sys.exit(2)