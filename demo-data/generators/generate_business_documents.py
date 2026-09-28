#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""确定性业务单据生成器 —— 企业数字化中台演示/开发数据。

覆盖 8 个业务域的**业务单据**（而非主数据）：
ERP 销售/采购/库存/财务、MES 生产/报工、WMS 出入库/拣货/波次、
QMS 检验/不合格品/CAPA/投诉、CRM 线索/商机/合同/回款、
HR 考勤/请假/薪资/绩效、OA 公告/报销/会议/日程、TMS 运单/轨迹/运费。

【边界声明 / Boundary】
1. 本脚本只产出**静态** SQL / JSON 文本，**不连接数据库、不执行任何 DML**。
   产物是否可执行未经实库验证（Docker daemon 不可用、目标库不可达）。
2. 产出数据**仅限开发/测试/演示环境**。生产导入会污染真实台账；
   清理入口见 demo-data/cleanup/cleanup_demo.sql（需随本脚本同步扩展）。
3. 依赖主数据：必须先运行 generate_master_data.py，本脚本读取其 JSON 产物取真实 ID，
   保证单据外键指向库内真实存在的行；主数据缺失即 fail-closed 退出。
4. fail-closed：列必须存在于 schema 快照；NOT NULL 列必须被覆盖；
   主键区间溢出、主数据缺失即刻抛错，绝不"静默补默认值"掩盖结构漂移。

【确定性 / Determinism】
- 唯一随机源为 random.Random(seed)，seed 默认 20260928；禁止使用 hash()；
- 主键由 (租户段, 表序号, 行序) 固定推导，同一组参数多次运行字节一致。

用法：
    python demo-data/generators/generate_business_documents.py
    python demo-data/generators/generate_business_documents.py --scale large --tenant 9002
"""

from __future__ import annotations

import argparse
import hashlib
from datetime import date as _date, timedelta as _timedelta
import json
import random
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
DEFAULT_SNAPSHOT = REPO_ROOT / "demo-data" / "schema" / "schema-snapshot.json"
DEFAULT_MASTER_JSON = REPO_ROOT / "demo-data" / "schema" / "demo_master_data.json"
DEFAULT_SQL_OUT = REPO_ROOT / "demo-data" / "sql" / "mysql" / "demo_business_documents.sql"
DEFAULT_JSON_OUT = REPO_ROOT / "demo-data" / "schema" / "demo_business_documents.json"
DEFAULT_SUMMARY = REPO_ROOT / "demo-data" / "schema" / "demo_business_documents_summary.json"

# ------------------------------ 演示数据隔离常量（与 generate_master_data.py 一致） -------------
DEFAULT_TENANT_ID = 9001
DEFAULT_SEED = 20260928
CREATOR = "demo-seed"
FIXED_TIME = "2026-01-01 00:00:00"
EMAIL_DOMAIN = "zhicloud.demo.invalid"      # RFC 2606 保留域

# 主键分配：单据使用独立于主数据的 ID 段，避免与 generate_master_data.py 的
# [910000 + tenant*1e6, +19*1000) 区间撞号。
ID_BASE_DOC = 5000000
ID_TENANT_STRIDE_DOC = 10000000
ID_TABLE_STRIDE_DOC = 20000                 # 单表最多 19999 行

# 业务时间轴：全部落在 2026-01-01 ~ 2026-06-30，与 FIXED_TIME 审计列区分
BIZ_DATE_START = "2026-01-05"
BIZ_DAYS = 170

# ------------------------------ 枚举取值（全部引自 DDL 注释或 Java 枚举） ----------------------
# ERP 单据：V10__erp_ddl.sql
ERP_SALE_ORDER_STATUS = (10, 20, 30, 40)          # 10 草稿 20 已审批 30 已出库 40 已关闭
ERP_OUT_STATUS = (10, 20)                          # 10 草稿 20 已出库
ERP_RECEIPT_STATUS = (10, 20, 30)                  # 10 未收款 20 部分收款 30 已收款
ERP_PAY_STATUS = (10, 20, 30)                      # 10 未付款 20 部分付款 30 已付款
ERP_REFUND_STATUS = (10, 20, 30)                   # 10 未退款 20 部分退款 30 已退款
ERP_PURCHASE_ORDER_STATUS = (10, 20, 30, 40)
ERP_STOCK_STATUS = (10, 20)                        # 10 草稿 20 已完成
ERP_STOCK_RECORD_BIZ_TYPE = (10, 20, 30, 40, 50, 60, 70, 80)
ERP_FINANCE_STATUS = (10, 20)                      # 10 草稿 20 已完成
ERP_FINANCE_BILL_TYPE_SALE = 10                    # 10 销售出库 20 销售退货
ERP_FINANCE_BILL_TYPE_SALE_RETURN = 20
ERP_FINANCE_BILL_TYPE_PURCHASE = 10                # erp_finance_payment_item 复用同一字典
ERP_ACCOUNT_TYPE = (10, 20, 30)                    # 10 现金 20 银行 30 其他

# WMS：V11__wms_ddl.sql
WMS_RECEIPT_TYPE = (101, 102, 103)                 # 101 采购入库 102 退货入库 103 归还入库
WMS_RECEIPT_STATUS = (0, 1, 2, 3)                  # 0 草稿 1 待审核 2 已审核 3 已完成
WMS_SHIPMENT_TYPE = (200, 201, 202)                # 200 退货出库 201 销售出库 202 生产出库
WMS_SHIPMENT_STATUS = (0, 10, 20, 30, 40, 50, 99)
WMS_PICK_STATUS = (10, 20, 30)                     # 10 待拣 20 已拣 30 已确认
WMS_WAVE_STATUS = (0, 4)                           # 0 草稿 4 已完成
WMS_MOVE_STATUS = (0, 1, 2, 3)
WMS_ASN_STATUS = (10, 20, 30, 40)                  # 10 待到货 20 已到货 30 已收货 40 已上架

# QMS：V13__qms_ddl.sql
QMS_INSP_TYPE = (10, 20, 30, 35)                   # 10 IQC 20 IPQC 30 OQC 35 FQC
QMS_INSP_ORDER_STATUS = (10, 20, 30, 40)           # 10 待检验 20 检验中 30 通过 40 不通过
QMS_INSP_RESULT = (10, 20, 30)                     # 10 合格 20 不合格 30 不适用
QMS_SEVERITY = (10, 20, 30)                        # 10 致命 20 严重 30 轻微
QMS_NCR_STATUS = (10, 20, 30, 40)
QMS_CAPA_STATUS = (10, 20, 30)
QMS_VERIFY_RESULT = (10, 20, 30)
QMS_COMPLAINT_STATUS = (10, 20, 30, 40)
QMS_COMPLAINT_HANDLE_TYPE = (10, 20, 30, 40)
QMS_INSTRUMENT_STATUS = (10, 20, 30, 40)           # 10 在用 20 停用 30 报废 40 封存
QMS_CALIBRATION_RESULT = (10, 20)

# MES：MesProWorkOrderStatusEnum / MesProTaskStatusEnum / MesOrderStatusConstants
MES_WO_STATUS = (0, 1, 2, 3, 4, 5, 6)              # 0 草稿 1 已确认 2 已完成 3 已取消 4 已派工 5 报工中 6 已关闭
MES_TASK_STATUS = (0, 4, 5)                        # 0 草稿 4 已完成 5 已取消
MES_FEEDBACK_TYPE = (1, 2)                         # 1 自行报工 2 统一报工
MES_WO_TYPE = (1, 2, 3)                            # 1 自行生产 2 代工 3 采购
MES_WO_SOURCE_TYPE = (1, 2)                        # 1 客户订单 2 库存备货
MES_LINK_TYPE = (0, 1, 2, 3)
MES_BOM_TYPE = ("MANUFACTURING", "ENGINEERING", "PHANTOM")
MES_ITEM_OR_PRODUCT = ("ITEM", "PRODUCT")
MES_COMMON_STATUS_ON = 0                           # CommonStatusEnum：0 启用 / 1 停用

# HR：V83__hr_base_tables.sql
HR_ATTENDANCE_STATUS = (10, 20, 30, 40, 50)        # 10 正常 20 迟到 30 早退 40 缺勤 50 加班
HR_LEAVE_STATUS = (0, 1, 2, 3)                     # 0 待审批 1 已批准 2 已驳回 3 已撤销
HR_CONTRACT_TYPE = (1, 2, 3, 4)
HR_CONTRACT_STATUS = (0, 1, 2, 3, 4)
HR_SALARY_STATUS = (10, 20, 30)
HR_PERF_GRADE = (10, 20, 30, 40)                   # 10 A 20 B 30 C 40 D
# OA：oa 模块 DDL
OA_ANNOUNCEMENT_STATUS = (10, 20, 30, 40)
OA_REIMBURSE_TYPE = (10, 20, 30, 40, 50)
OA_REIMBURSE_PAY_STATUS = (10, 20, 30)
OA_REIMBURSE_STATUS = (10, 20, 30, 40, 50)
OA_SCHEDULE_TYPE = (10, 20, 30, 40)
OA_SCHEDULE_REPEAT = (0, 10, 20, 30, 40)
OA_SCHEDULE_STATUS = (0, 1, 2)
OA_MEETING_STATUS = (10, 20, 30, 40)
# TMS：V58__tms_init.sql
TMS_SHIPMENT_TYPE = (10, 20, 30, 40)
TMS_SHIPMENT_STATUS = (10, 20, 30, 40, 50)
TMS_TRACKING_EVENT_TYPE = (10, 20, 30, 40)
TMS_FREIGHT_STATUS = (10, 20, 30, 40)
TMS_RECON_STATUS = (0, 10, 20, 30, 40)
# CRM：CrmAuditStatus / 通用
CRM_AUDIT_STATUS = (10, 20, 30)                    # 10 草稿/待审批 20 审批中 30 已通过
CRM_RETURN_TYPE = (10, 20, 30)                     # 10 回款 20 退款 30 其它
CRM_PRODUCT_STATUS = (0, 1)                        # 0 上架 1 下架

# 刻意留 NULL 或语义未核实的字段（写入 manifest，不做猜测填充）
UNVERIFIED_FIELDS = [
    "crm_business.status_type_id / status_id（已生成 crm_business_status_type/status 并绑定，"
    "但 end_status 语义仅知为 int，未定位枚举定义，按 0/1 填充并标注）",
    "crm_contract.process_instance_id / esign_task_id（工作流与电子签实例 ID，留 NULL）",
    "crm_receivable.process_instance_id（同上，留 NULL）",
    "crm_receivable.idempotency_key / payload_hash（幂等与防篡改字段，留 NULL）",
    "crm_clue.owner_user_id / crm_business.owner_user_id / crm_contract.owner_user_id"
    "（绑定真实 system 用户会与账号体系耦合，留 NULL）",
    "crm_contact.owner_user_id（同上，留 NULL）",
    "crm_follow_up_record.business_ids / contact_ids（逗号分隔 ID 串，格式未核实，留 NULL）",
    "oa_reimburse.process_instance_id（工作流实例 ID，留 NULL）",
    "oa_meeting_reservation.attendee_user_ids（ID 串格式未核实，留 NULL）",
    "mes_pro_task_issue.source_doc_id / source_line_id（来源单据行标识，无上游来源单据，留 NULL）",
    "qms_customer_complaint.eight_d_id（8D 报告 ID，未生成 qms_eight_d_report，留 NULL）",
    "wms_pick_task.picker_user_id / wms_wave_order.picker（system 用户 ID，留 NULL）",
    "erp_stock_check.status（盘点差异未回填业务语义，按 ERP 通用 10/20 填充）",
]

# 生成顺序即外键依赖顺序（先被引用的先落盘）
TABLE_ORDER = [
    # --- 支撑主数据：单据引用前必须先落盘
    "erp_account", "wms_item_sku",
    "mes_md_unit_measure", "mes_md_item_type", "mes_md_item",
    "mes_md_client", "mes_md_vendor",
    "mes_pro_process", "mes_md_workshop", "mes_md_workstation",
    "mes_pro_route", "mes_pro_route_process",
    "mes_md_bom", "mes_md_bom_detail",
    "crm_product_category", "crm_product",
    "crm_business_status_type", "crm_business_status",
    "qms_inspection_item", "qms_instrument",
    "hr_leave_type", "oa_meeting_room",
    # --- ERP 单据
    "erp_sale_order", "erp_sale_order_items",
    "erp_sale_out", "erp_sale_out_items",
    "erp_sale_return", "erp_sale_return_items",
    "erp_purchase_order", "erp_purchase_order_items",
    "erp_purchase_in", "erp_purchase_in_items",
    "erp_purchase_return", "erp_purchase_return_items",
    "erp_stock_in", "erp_stock_in_item",
    "erp_stock_out", "erp_stock_out_item",
    "erp_stock_move", "erp_stock_move_item",
    "erp_stock", "erp_stock_record",
    "erp_stock_check", "erp_stock_check_item",
    "erp_finance_receipt", "erp_finance_receipt_item",
    "erp_finance_payment", "erp_finance_payment_item",
    # --- MES 单据
    "mes_pro_work_order", "mes_pro_work_order_bom",
    "mes_pro_task", "mes_pro_task_issue",
    "mes_pro_feedback", "mes_wm_material_stock",
    # --- WMS 单据
    "wms_asn_order", "wms_asn_order_detail",
    "wms_receipt_order", "wms_receipt_order_detail", "wms_inventory",
    "wms_shipment_order", "wms_shipment_order_detail",
    "wms_wave_order", "wms_wave_order_detail", "wms_pick_task",
    "wms_movement_order", "wms_movement_order_detail",
    # --- QMS 单据
    "qms_inspection_order", "qms_inspection_record",
    "qms_ncr_document", "qms_capa_document",
    "qms_customer_complaint", "qms_instrument_calibration",
    # --- CRM 单据
    "crm_clue", "crm_business", "crm_business_product",
    "crm_contract", "crm_contract_product",
    "crm_receivable_plan", "crm_receivable",
    "crm_contact", "crm_follow_up_record",
    "crm_invoice", "crm_invoice_line",
    # --- HR 单据
    "hr_leave_balance", "hr_leave_request", "hr_attendance",
    "hr_contract", "hr_salary", "hr_performance",
    # --- OA 单据
    "oa_announcement", "oa_reimburse", "oa_reimburse_item",
    "oa_meeting_reservation", "oa_schedule",
    # --- TMS 单据
    "tms_shipment", "tms_shipment_stop", "tms_tracking_event",
    "tms_freight", "tms_freight_reconciliation",
]

SCALE_FACTOR = {"small": 1, "medium": 4, "large": 10}


class FailClosedError(Exception):
    """结构性校验失败：一律中断，绝不降级补默认值。"""


# ------------------------------ 快照 / 主数据加载 ------------------------------
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


class MasterData:
    """读取 generate_master_data.py 的 JSON 产物，作为单据外键的真实 ID 池。"""

    REQUIRED = [
        "erp_product", "erp_customer", "erp_supplier", "erp_warehouse",
        "wms_item", "wms_warehouse", "wms_location", "wms_merchant",
        "crm_customer", "hr_employee", "hr_department", "hr_position",
        "tms_carrier", "tms_driver", "tms_vehicle",
    ]

    def __init__(self, path: Path):
        if not path.exists():
            raise FailClosedError(
                "主数据 JSON 不存在：%s（请先运行 generate_master_data.py）" % path)
        payload = json.loads(path.read_bytes().decode("utf-8"))
        self.index = {}
        for item in payload.get("tables", []):
            self.index[item["table"]] = item["rows"]
        missing = [t for t in self.REQUIRED if not self.index.get(t)]
        if missing:
            raise FailClosedError("主数据缺失或为空表：%s" % ", ".join(missing))
        self.tenant = payload.get("tenant_id")
        self.seed = payload.get("seed")

    def rows(self, table: str) -> list:
        got = self.index.get(table)
        if not got:
            raise FailClosedError("主数据无表 %s（单据外键不能指向空池）" % table)
        return got

    def ids(self, table: str) -> list:
        return [r["id"] for r in self.rows(table)]

    def pick(self, table: str, rnd, i: int):
        """确定性取值：用 (i) 取模，避免 rnd 状态被无关调用污染。"""
        pool = self.rows(table)
        return pool[i % len(pool)]

    def pick_id(self, table: str, rnd, i: int):
        return self.pick(table, rnd, i)["id"]

    def field(self, table: str, i: int, name: str):
        return self.pick(table, None, i).get(name)


# ------------------------------ 字面量渲染 ------------------------------
def sql_escape(value: str) -> str:
    return value.replace("\\", "\\\\").replace("'", "''")


def render_value(column: dict, value) -> str:
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
    out = dict(row)
    out.setdefault("creator", CREATOR)
    out.setdefault("create_time", FIXED_TIME)
    out.setdefault("updater", CREATOR)
    out.setdefault("update_time", FIXED_TIME)
    if "deleted" in cols:
        out.setdefault("deleted", 0)
    if "tenant_id" in cols:
        out.setdefault("tenant_id", tenant)
    if "version" in cols:
        out.setdefault("version", 0)
    return out


def validate_scalar(table: str, row_no: int, name: str, column: dict, value):
    """校验单个字段是否满足快照声明的标量类型和长度。

    只校验 DDL 中可可靠推断的约束；未知类型保持宽松，避免伪造约束。
    """
    if value is None:
        return
    col_type = (column.get("type") or "").upper().strip()
    if col_type.startswith("VARCHAR(") or col_type.startswith("CHAR("):
        if not isinstance(value, str):
            raise FailClosedError(
                "表 %s 第 %d 行字段 %s 需要字符串，实际为 %s"
                % (table, row_no, name, type(value).__name__))
        open_at = col_type.find("(")
        close_at = col_type.find(")", open_at + 1)
        if close_at < 0:
            raise FailClosedError("表 %s 字段 %s 的 VARCHAR/CHAR 类型缺少长度：%s" % (table, name, col_type))
        max_len = int(col_type[open_at + 1:close_at].strip())
        if len(value) > max_len:
            raise FailClosedError(
                "表 %s 第 %d 行字段 %s 长度 %d 超过 DDL 上限 %d"
                % (table, row_no, name, len(value), max_len))
        return
    if col_type in ("TEXT", "MEDIUMTEXT", "LONGTEXT"):
        if not isinstance(value, str):
            raise FailClosedError(
                "表 %s 第 %d 行字段 %s 需要文本，实际为 %s"
                % (table, row_no, name, type(value).__name__))
        return
    if col_type.startswith(("BIGINT", "INT", "SMALLINT", "TINYINT")):
        if isinstance(value, bool) or not isinstance(value, int):
            raise FailClosedError(
                "表 %s 第 %d 行字段 %s 需要整数，实际为 %s"
                % (table, row_no, name, type(value).__name__))
        return
    if col_type.startswith("DECIMAL(") or col_type in ("DOUBLE", "FLOAT", "REAL"):
        if isinstance(value, bool) or not isinstance(value, (int, float)):
            raise FailClosedError(
                "表 %s 第 %d 行字段 %s 需要数值，实际为 %s"
                % (table, row_no, name, type(value).__name__))
        return
    if col_type in ("DATE", "DATETIME", "TIME"):
        if not isinstance(value, str):
            raise FailClosedError(
                "表 %s 第 %d 行字段 %s 需要日期/时间字符串，实际为 %s"
                % (table, row_no, name, type(value).__name__))
        return
    if col_type.startswith("BIT"):
        if isinstance(value, bool):
            return
        if not isinstance(value, int) or value not in (0, 1):
            raise FailClosedError(
                "表 %s 第 %d 行字段 %s 需要 BIT(0/1)，实际为 %r"
                % (table, row_no, name, value))
        return
    if col_type in ("BLOB", "MEDIUMBLOB", "LONGBLOB"):
        if not isinstance(value, (bytes, bytearray)):
            raise FailClosedError(
                "表 %s 第 %d 行字段 %s 需要二进制值，实际为 %s"
                % (table, row_no, name, type(value).__name__))


def validate_rows(table: str, cols: dict, rows: list) -> list:
    order = list(cols.keys())
    required = [n for n, c in cols.items() if c.get("nullable") is False]
    normalized = []
    for idx, row in enumerate(rows):
        unknown = [k for k in row if k not in cols]
        if unknown:
            raise FailClosedError(
                "表 %s 第 %d 行使用了快照中不存在的列：%s" % (table, idx + 1, ", ".join(sorted(unknown))))
        missing = [n for n in required if n not in row or row.get(n) is None]
        if missing:
            raise FailClosedError(
                "表 %s 第 %d 行未覆盖或置空 NOT NULL 列：%s" % (table, idx + 1, ", ".join(sorted(missing))))
        for name in order:
            validate_scalar(table, idx + 1, name, cols[name], row.get(name))
        normalized.append({k: row.get(k) for k in order})
    return normalized


# ------------------------------ 主键分配 ------------------------------
def make_id_allocator(tenant: int):
    tenant_base = ID_BASE_DOC + (tenant - DEFAULT_TENANT_ID) * ID_TENANT_STRIDE_DOC
    table_slot = {t: i for i, t in enumerate(TABLE_ORDER)}

    def alloc(table: str, count: int) -> list:
        if table not in table_slot:
            raise FailClosedError("表 %s 未登记于 TABLE_ORDER" % table)
        if count <= 0:
            return []
        if count >= ID_TABLE_STRIDE_DOC:
            raise FailClosedError(
                "表 %s 行数 %d 超出单表主键区间 %d，请降低 --scale 或扩大 ID_TABLE_STRIDE_DOC"
                % (table, count, ID_TABLE_STRIDE_DOC))
        base = tenant_base + table_slot[table] * ID_TABLE_STRIDE_DOC
        return [base + i for i in range(count)]

    return alloc


# ------------------------------ 日期工具（确定性） ------------------------------
_MONTH_LEN = (31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)


def biz_datetime(offset_days: int, hour: int = 9, minute: int = 0) -> str:
    """从 BIZ_DATE_START 起确定性偏移，产出 'YYYY-MM-DD HH:MM:SS'。"""
    base = _date(2026, 1, 5)
    d = base + _timedelta(days=int(offset_days))
    return "%04d-%02d-%02d %02d:%02d:00" % (d.year, d.month, d.day, hour, minute)


def biz_date(offset_days: int) -> str:
    return biz_datetime(offset_days)[:10]


def biz_month(offset_months: int) -> str:
    """2026-01 起按月偏移，产出 'YYYY-MM'。"""
    total = 2026 * 12 + (1 - 1) + int(offset_months)
    y, m = divmod(total, 12)
    return "%04d-%02d" % (y, m + 1)


def money(x) -> float:
    return round(float(x), 2)


def qty(x) -> float:
    return round(float(x), 3)

# ------------------------------ 构建上下文 ------------------------------
class Builder:
    """持有快照、主键分配器、主数据池，并按 TABLE_ORDER 顺序落盘。"""

    def __init__(self, snapshot: dict, seed: int, tenant: int, scale: str, master: MasterData):
        self.snapshot = snapshot
        self.seed = seed
        self.tenant = tenant
        self.scale = scale
        self.master = master
        self.s = SCALE_FACTOR[scale]
        self.rnd = random.Random(seed)
        self.alloc = make_id_allocator(tenant)
        self.dataset = []
        self.pools = {}

    # --- 落盘与查询
    def emit(self, table: str, rows: list):
        cols = column_index(self.snapshot, table)
        rows = [with_audit(r, cols, self.tenant) for r in rows]
        norm = validate_rows(table, cols, rows)
        self.dataset.append({"table": table, "rows": norm})
        self.pools[table] = norm
        return norm

    def own(self, table: str) -> list:
        """本脚本已生成的表行（用于单据间外键）。"""
        got = self.pools.get(table)
        if not got:
            raise FailClosedError("表 %s 尚未生成，无法作为外键来源" % table)
        return got

    def own_id(self, table: str, i: int):
        pool = self.own(table)
        return pool[i % len(pool)]["id"]

    # --- 主数据便捷访问
    def m_id(self, table: str, i: int):
        return self.master.pick_id(table, self.rnd, i)

    def m_row(self, table: str, i: int):
        return self.master.pick(table, self.rnd, i)

    def m_name(self, table: str, i: int, key: str = "name"):
        return self.m_row(table, i).get(key)

    # --- 文档编号
    def biz_no(self, prefix: str, i: int) -> str:
        return "DEMO-%s-%05d" % (prefix, i + 1)


# ------------------------------ 支撑主数据 ------------------------------
def build_supporting(b: Builder):
    # --- erp_account：ERP 收付款单的 account_id 依赖
    accounts = [
        ("DEMO_CASH", "DEMO_现金账户", 10, 1),
        ("DEMO_BANK_CNY", "DEMO_银行基本户", 20, 0),
        ("DEMO_BANK_USD", "DEMO_银行外币户", 20, 0),
        ("DEMO_OTHER", "DEMO_其他账户", 30, 0),
    ]
    ids = b.alloc("erp_account", len(accounts))
    b.emit("erp_account", [
        {"id": ids[i], "code": c, "name": n, "type": t, "status": 0, "default_flag": f}
        for i, (c, n, t, f) in enumerate(accounts)])

    # --- wms_item_sku：WMS 出入库明细的 sku_id 依赖（每个 wms_item 生成 2 个 SKU）
    items = b.master.rows("wms_item")
    sku_rows, sku_seq = [], 0
    for i, it in enumerate(items):
        for k in range(2):
            cost = money(b.rnd.uniform(8.0, 900.0))
            sku_rows.append({
                "id": None, "item_id": it["id"],
                "name": "DEMO_SKU-%03d" % (sku_seq + 1),
                "code": "DEMO-SKU-%03d" % (sku_seq + 1),
                "bar_code": "DEMOBC%08d" % (sku_seq + 1),
                "cost_price": cost, "selling_price": money(cost * b.rnd.uniform(1.12, 1.45)),
                "length": qty(b.rnd.uniform(5, 60)), "width": qty(b.rnd.uniform(5, 40)),
                "height": qty(b.rnd.uniform(2, 30)),
                "gross_weight": qty(b.rnd.uniform(0.2, 25)), "net_weight": qty(b.rnd.uniform(0.2, 24)),
            })
            sku_seq += 1
    sku_ids = b.alloc("wms_item_sku", len(sku_rows))
    for row, rid in zip(sku_rows, sku_ids):
        row["id"] = rid
    b.emit("wms_item_sku", sku_rows)

    # --- mes_md_unit_measure
    um = [("DEMO_PC", "DEMO_个"), ("DEMO_KG", "DEMO_千克"), ("DEMO_M", "DEMO_米"),
          ("DEMO_SET", "DEMO_套"), ("DEMO_BOX", "DEMO_箱"), ("DEMO_L", "DEMO_升")]
    um_ids = b.alloc("mes_md_unit_measure", len(um))
    b.emit("mes_md_unit_measure", [
        {"id": um_ids[i], "code": c, "name": n, "primary_flag": 1 if i == 0 else 0,
         "primary_id": um_ids[0], "change_rate": qty(1.0 if i == 0 else b.rnd.choice([10, 100, 1000])),
         "status": MES_COMMON_STATUS_ON, "remark": "DEMO_演示计量单位"}
        for i, (c, n) in enumerate(um)])

    # --- mes_md_item_type
    itype = [("DEMO_RAW", "DEMO_原材料", "ITEM"), ("DEMO_SEMI", "DEMO_半成品", "ITEM"),
             ("DEMO_FG", "DEMO_成品", "PRODUCT"), ("DEMO_AUX", "DEMO_辅料", "ITEM")]
    it_ids = b.alloc("mes_md_item_type", len(itype))
    b.emit("mes_md_item_type", [
        {"id": it_ids[i], "code": c, "name": n, "parent_id": 0, "item_or_product": p,
         "sort": (i + 1) * 10, "status": MES_COMMON_STATUS_ON, "remark": "DEMO_演示物料分类"}
        for i, (c, n, p) in enumerate(itype)])

    # --- mes_md_item
    n_item = 12 * b.s
    item_ids = b.alloc("mes_md_item", n_item)
    b.emit("mes_md_item", [{
        "id": item_ids[i],
        "code": "DEMO-MI-%04d" % (i + 1),
        "name": "DEMO_物料%04d" % (i + 1),
        "specification": "DEMO_SPEC-%d" % (i + 1),
        "unit_measure_id": um_ids[i % len(um_ids)],
        "item_type_id": it_ids[i % len(it_ids)],
        "status": MES_COMMON_STATUS_ON,
        "safe_stock_flag": 1 if i % 3 == 0 else 0,
        "min_stock": qty(b.rnd.uniform(50, 200)), "max_stock": qty(b.rnd.uniform(500, 2000)),
        "high_value": 1 if i % 5 == 0 else 0,
        "batch_flag": 1 if i % 2 == 0 else 0,
        "remark": "DEMO_演示物料",
    } for i in range(n_item)])

    # --- mes_md_client / mes_md_vendor
    n_client = 6 * b.s
    cl_ids = b.alloc("mes_md_client", n_client)
    b.emit("mes_md_client", [{
        "id": cl_ids[i], "code": "DEMO-MC-%03d" % (i + 1), "name": "DEMO_客户%03d" % (i + 1),
        "nickname": "DEMO_客户简称%03d" % (i + 1), "type": 10, "address": "DEMO_演示地址%03d" % (i + 1),
        "email": "client%03d@%s" % (i + 1, EMAIL_DOMAIN),
        "telephone": "1380013%04d" % (5000 + i),
        "contact1_name": "DEMO_联系人%03d" % (i + 1), "contact1_telephone": "1380013%04d" % (6000 + i),
        "contact1_email": "c1_%03d@%s" % (i + 1, EMAIL_DOMAIN),
        "status": MES_COMMON_STATUS_ON, "remark": "DEMO_演示客户",
    } for i in range(n_client)])

    n_vendor = 5 * b.s
    vd_ids = b.alloc("mes_md_vendor", n_vendor)
    b.emit("mes_md_vendor", [{
        "id": vd_ids[i], "code": "DEMO-MV-%03d" % (i + 1), "name": "DEMO_供应商%03d" % (i + 1),
        "nickname": "DEMO_供应商简称%03d" % (i + 1), "level": ("A", "B", "C")[i % 3],
        "score": int(b.rnd.uniform(60, 98)), "address": "DEMO_演示地址V%03d" % (i + 1),
        "email": "vendor%03d@%s" % (i + 1, EMAIL_DOMAIN),
        "telephone": "1380013%04d" % (7000 + i),
        "contact1_name": "DEMO_供方联系人%03d" % (i + 1), "contact1_telephone": "1380013%04d" % (8000 + i),
        "contact1_email": "v1_%03d@%s" % (i + 1, EMAIL_DOMAIN),
        "status": MES_COMMON_STATUS_ON, "remark": "DEMO_演示供应商",
    } for i in range(n_vendor)])

    # --- mes_pro_process
    proc = [("DEMO_OP10", "DEMO_下料"), ("DEMO_OP20", "DEMO_机加工"), ("DEMO_OP30", "DEMO_焊接"),
            ("DEMO_OP40", "DEMO_装配"), ("DEMO_OP50", "DEMO_检验"), ("DEMO_OP60", "DEMO_包装")]
    pr_ids = b.alloc("mes_pro_process", len(proc))
    b.emit("mes_pro_process", [
        {"id": pr_ids[i], "code": c, "name": n, "attention": "DEMO_演示作业指导要点%d" % (i + 1),
         "status": MES_COMMON_STATUS_ON, "remark": "DEMO_演示工序"}
        for i, (c, n) in enumerate(proc)])

    # --- mes_md_workshop
    ws = [("DEMO_WS01", "DEMO_一号车间", 1200.0), ("DEMO_WS02", "DEMO_二号车间", 860.0)]
    ws_ids = b.alloc("mes_md_workshop", len(ws))
    b.emit("mes_md_workshop", [
        {"id": ws_ids[i], "code": c, "name": n, "area": qty(a),
         "status": MES_COMMON_STATUS_ON, "remark": "DEMO_演示车间"}
        for i, (c, n, a) in enumerate(ws)])

    # --- mes_md_workstation
    n_st = 8 * b.s
    st_ids = b.alloc("mes_md_workstation", n_st)
    b.emit("mes_md_workstation", [{
        "id": st_ids[i],
        "code": "DEMO-ST-%03d" % (i + 1), "name": "DEMO_工位%03d" % (i + 1),
        "address": "DEMO_%s-%03d" % ("一号" if i % 2 == 0 else "二号", i + 1),
        "workshop_id": ws_ids[i % len(ws_ids)], "process_id": pr_ids[i % len(pr_ids)],
        "warehouse_id": b.m_id("wms_warehouse", i), "location_id": b.m_id("wms_location", i),
        "status": MES_COMMON_STATUS_ON, "remark": "DEMO_演示工位",
    } for i in range(n_st)])

    # --- mes_pro_route
    rt = [("DEMO_RT01", "DEMO_标准装配工艺路线"), ("DEMO_RT02", "DEMO_简易装配工艺路线")]
    rt_ids = b.alloc("mes_pro_route", len(rt))
    b.emit("mes_pro_route", [
        {"id": rt_ids[i], "code": c, "name": n, "description": "DEMO_演示工艺路线%d" % (i + 1),
         "status": MES_COMMON_STATUS_ON, "remark": "DEMO_演示工艺路线"}
        for i, (c, n) in enumerate(rt)])

    # --- mes_pro_route_process：每条路线串 6 道工序
    rp_rows = []
    for r in range(len(rt_ids)):
        for k in range(len(pr_ids)):
            rp_rows.append({
                "id": None, "route_id": rt_ids[r], "process_id": pr_ids[k],
                "sort": (k + 1) * 10,
                "next_process_id": pr_ids[k + 1] if k + 1 < len(pr_ids) else 0,
                "link_type": b.rnd.choice(MES_LINK_TYPE),
                "prepare_time": b.rnd.randint(5, 30), "wait_time": b.rnd.randint(0, 20),
                "color_code": "DEMO_C%d" % (k + 1),
                "key_flag": 1 if k == len(pr_ids) - 1 else 0,
                "check_flag": 1 if k == len(pr_ids) - 2 else 0,
                "remark": "DEMO_演示路线工序",
            })
    rp_ids = b.alloc("mes_pro_route_process", len(rp_rows))
    for row, rid in zip(rp_rows, rp_ids):
        row["id"] = rid
    b.emit("mes_pro_route_process", rp_rows)

    # --- mes_md_bom / mes_md_bom_detail
    n_bom = 6 * b.s
    bom_ids = b.alloc("mes_md_bom", n_bom)
    b.emit("mes_md_bom", [{
        "id": bom_ids[i], "bom_no": "DEMO-BOM-%04d" % (i + 1),
        "product_id": b.m_id("erp_product", i),
        "bom_type": MES_BOM_TYPE[i % len(MES_BOM_TYPE)],
        "version": "V1.0", "status": MES_COMMON_STATUS_ON, "remark": "DEMO_演示 BOM",
    } for i in range(n_bom)])

    det_rows = []
    for i in range(n_bom):
        for k in range(3):
            det_rows.append({
                "id": None, "bom_id": bom_ids[i],
                "product_id": b.m_id("erp_product", i * 3 + k + 1),
                "quantity": qty(b.rnd.uniform(1, 12)), "unit": "DEMO_个",
                "scrap_rate": qty(b.rnd.uniform(0.0, 0.05)),
                "unit_cost": money(b.rnd.uniform(3.0, 260.0)),
                "remark": "DEMO_演示 BOM 明细",
            })
    det_ids = b.alloc("mes_md_bom_detail", len(det_rows))
    for row, rid in zip(det_rows, det_ids):
        row["id"] = rid
    b.emit("mes_md_bom_detail", det_rows)

    # --- crm_product_category / crm_product
    cpc = ["DEMO_硬件", "DEMO_软件", "DEMO_服务"]
    cpc_ids = b.alloc("crm_product_category", len(cpc))
    b.emit("crm_product_category", [
        {"id": cpc_ids[i], "name": n, "parent_id": 0} for i, n in enumerate(cpc)])

    n_cp = 10 * b.s
    cp_ids = b.alloc("crm_product", n_cp)
    b.emit("crm_product", [{
        "id": cp_ids[i], "name": "DEMO_产品%03d" % (i + 1), "no": "DEMO-P-%04d" % (i + 1),
        "unit": 1 + i % 5, "price": money(b.rnd.uniform(120, 9800)),
        "status": CRM_PRODUCT_STATUS[i % 2], "category_id": cpc_ids[i % len(cpc_ids)],
        "description": "DEMO_演示 CRM 产品",
    } for i in range(n_cp)])

    # --- crm_business_status_type / crm_business_status
    bst = ["DEMO_新签商机", "DEMO_续约商机"]
    bst_ids = b.alloc("crm_business_status_type", len(bst))
    b.emit("crm_business_status_type", [
        {"id": bst_ids[i], "name": n} for i, n in enumerate(bst)])

    bs = [("DEMO_初步接洽", 10), ("DEMO_需求确认", 30), ("DEMO_方案报价", 60), ("DEMO_赢单", 100)]
    bs_rows = []
    for t in bst_ids:
        for k, (n, p) in enumerate(bs):
            bs_rows.append({"id": None, "type_id": t, "name": n, "percent": p, "sort": (k + 1) * 10})
    bs_ids = b.alloc("crm_business_status", len(bs_rows))
    for row, rid in zip(bs_rows, bs_ids):
        row["id"] = rid
    b.emit("crm_business_status", bs_rows)

    # --- qms_inspection_item
    insp = [("DEMO_外观", 10), ("DEMO_尺寸", 20), ("DEMO_硬度", 30),
            ("DEMO_电气性能", 30), ("DEMO_包装完整性", 10)]
    ii_ids = b.alloc("qms_inspection_item", len(insp))
    b.emit("qms_inspection_item", [{
        "id": ii_ids[i], "code": "DEMO-QI-%03d" % (i + 1), "name": n,
        "type": QMS_INSP_TYPE[i % len(QMS_INSP_TYPE)], "method": m,
        "standard": "DEMO_检验标准%d" % (i + 1), "target": "DEMO_目标值%d" % (i + 1),
        "upper_limit": qty(b.rnd.uniform(100, 500)), "lower_limit": qty(b.rnd.uniform(0, 50)),
        "unit": "DEMO_mm", "status": 0, "remark": "DEMO_演示检验项",
    } for i, (n, m) in enumerate(insp)])

    # --- qms_instrument
    inst = [("DEMO_EQ001", "DEMO_数显卡尺", 10), ("DEMO_EQ002", "DEMO_硬度计", 30),
            ("DEMO_EQ003", "DEMO_耐压测试仪", 40)]
    in_ids = b.alloc("qms_instrument", len(inst))
    b.emit("qms_instrument", [{
        "id": in_ids[i], "code": c, "name": n, "category": cat,
        "model": "DEMO-MODEL-%03d" % (i + 1), "manufacturer": "DEMO_演示厂商",
        "serial_no": "DEMO-SN-%05d" % (i + 1), "accuracy": "DEMO_ACC_%.3f" % b.rnd.uniform(0.001, 0.05),
        "measure_range": "DEMO_0-500", "unit": "DEMO_mm", "location": "DEMO_计量室",
        "responsible_person": "DEMO_计量员", "calibration_cycle_days": 365,
        "last_calibration_date": biz_date(0), "next_calibration_date": biz_date(365),
        "status": QMS_INSTRUMENT_STATUS[0], "sort": (i + 1) * 10, "remark": "DEMO_演示器具",
    } for i, (c, n, cat) in enumerate(inst)])

    # --- hr_leave_type
    lt = [("DEMO_年假", "DEMO_ANNUAL"), ("DEMO_事假", "DEMO_AFFAIR"),
          ("DEMO_病假", "DEMO_SICK"), ("DEMO_调休", "DEMO_OFF")]
    lt_ids = b.alloc("hr_leave_type", len(lt))
    b.emit("hr_leave_type", [
        {"id": lt_ids[i], "name": n, "code": c, "is_paid": 1 if i in (0, 2, 3) else 0,
         "deduct_salary": 1 if i == 1 else 0, "remark": "DEMO_演示假期类型"}
        for i, (n, c) in enumerate(lt)])

    # --- oa_meeting_room
    rooms = [("DEMO_第一会议室", "DEMO_A 座 3F", 20), ("DEMO_第二会议室", "DEMO_A 座 5F", 12),
             ("DEMO_视频会议室", "DEMO_B 座 2F", 8)]
    rm_ids = b.alloc("oa_meeting_room", len(rooms))
    b.emit("oa_meeting_room", [
        {"id": rm_ids[i], "name": n, "location": loc, "capacity": cap,
         "equipment": "DEMO_投影仪,白板", "status": 10, "remark": "DEMO_演示会议室"}
        for i, (n, loc, cap) in enumerate(rooms)])


# ------------------------------ ERP 单据 ------------------------------
def build_erp(b: Builder):
    s = b.s
    acct_ids = [r["id"] for r in b.own("erp_account")]

    # --- 销售订单 + 明细
    n_so = 10 * s
    so_ids = b.alloc("erp_sale_order", n_so)
    so_items, so_heads = [], []
    for i in range(n_so):
        cust_id = b.m_id("erp_customer", i)
        lines, total = [], 0.0
        for k in range(3):
            pid = b.m_id("erp_product", i * 3 + k)
            cnt = qty(b.rnd.uniform(5, 60))
            price = money(b.rnd.uniform(50, 1800))
            amt = money(cnt * price)
            total += amt
            lines.append({"product_id": pid, "sale_count": cnt, "sale_price": price, "total_price": amt})
        so_heads.append({
            "id": so_ids[i], "no": b.biz_no("SO", i), "customer_id": cust_id,
            "out_time": biz_datetime(i * 7 + 3, 10, 30),
            "status": ERP_SALE_ORDER_STATUS[i % len(ERP_SALE_ORDER_STATUS)],
            "total_count": qty(sum(l["sale_count"] for l in lines)),
            "total_amount": money(total), "remark": "DEMO_演示销售订单",
        })
        so_items.append(lines)
    b.emit("erp_sale_order", so_heads)

    it_rows = []
    for i, lines in enumerate(so_items):
        for l in lines:
            it_rows.append({"id": None, "master_id": so_ids[i], **l, "remark": "DEMO_销售订单明细"})
    it_ids = b.alloc("erp_sale_order_items", len(it_rows))
    for row, rid in zip(it_rows, it_ids):
        row["id"] = rid
    b.emit("erp_sale_order_items", it_rows)

    # --- 销售出库 + 明细
    n_out = 8 * s
    out_ids = b.alloc("erp_sale_out", n_out)
    out_items, out_heads = [], []
    for i in range(n_out):
        lines, total = [], 0.0
        for k in range(2):
            cnt = qty(b.rnd.uniform(3, 40))
            price = money(b.rnd.uniform(50, 1800))
            amt = money(cnt * price)
            total += amt
            lines.append({"product_id": b.m_id("erp_product", i * 2 + k),
                          "sale_count": cnt, "sale_price": price, "total_price": amt})
        out_heads.append({
            "id": out_ids[i], "no": b.biz_no("SOUT", i), "customer_id": b.m_id("erp_customer", i),
            "out_time": biz_datetime(i * 8 + 5, 14, 0),
            "status": ERP_OUT_STATUS[i % len(ERP_OUT_STATUS)],
            "receipt_status": ERP_RECEIPT_STATUS[i % len(ERP_RECEIPT_STATUS)],
            "total_count": qty(sum(l["sale_count"] for l in lines)),
            "total_amount": money(total), "remark": "DEMO_演示销售出库",
        })
        out_items.append(lines)
    b.emit("erp_sale_out", out_heads)

    oit_rows = []
    for i, lines in enumerate(out_items):
        for l in lines:
            oit_rows.append({"id": None, "master_id": out_ids[i], **l, "remark": "DEMO_销售出库明细"})
    oit_ids = b.alloc("erp_sale_out_items", len(oit_rows))
    for row, rid in zip(oit_rows, oit_ids):
        row["id"] = rid
    b.emit("erp_sale_out_items", oit_rows)

    # --- 销售退货 + 明细
    n_sr = 3 * s
    sr_ids = b.alloc("erp_sale_return", n_sr)
    sr_items, sr_heads = [], []
    for i in range(n_sr):
        lines, total = [], 0.0
        for k in range(2):
            cnt = qty(b.rnd.uniform(1, 8))
            price = money(b.rnd.uniform(50, 1800))
            amt = money(cnt * price)
            total += amt
            lines.append({"product_id": b.m_id("erp_product", i * 2 + k + 1),
                          "sale_count": cnt, "sale_price": price, "total_price": amt})
        sr_heads.append({
            "id": sr_ids[i], "no": b.biz_no("SRT", i), "customer_id": b.m_id("erp_customer", i),
            "out_time": biz_datetime(i * 11 + 9, 11, 0),
            "status": ERP_OUT_STATUS[i % len(ERP_OUT_STATUS)],
            "refund_status": ERP_REFUND_STATUS[i % len(ERP_REFUND_STATUS)],
            "total_count": qty(sum(l["sale_count"] for l in lines)),
            "total_amount": money(total), "remark": "DEMO_演示销售退货",
        })
        sr_items.append(lines)
    b.emit("erp_sale_return", sr_heads)

    srt_rows = []
    for i, lines in enumerate(sr_items):
        for l in lines:
            srt_rows.append({"id": None, "master_id": sr_ids[i], **l, "remark": "DEMO_销售退货明细"})
    srt_ids = b.alloc("erp_sale_return_items", len(srt_rows))
    for row, rid in zip(srt_rows, srt_ids):
        row["id"] = rid
    b.emit("erp_sale_return_items", srt_rows)

    # --- 采购订单 + 明细
    n_po = 8 * s
    po_ids = b.alloc("erp_purchase_order", n_po)
    po_items, po_heads = [], []
    for i in range(n_po):
        lines, total = [], 0.0
        for k in range(3):
            cnt = qty(b.rnd.uniform(20, 200))
            price = money(b.rnd.uniform(10, 900))
            amt = money(cnt * price)
            total += amt
            lines.append({"product_id": b.m_id("erp_product", i * 3 + k + 2),
                          "purchase_count": cnt, "purchase_price": price, "total_price": amt})
        po_heads.append({
            "id": po_ids[i], "no": b.biz_no("PO", i), "supplier_id": b.m_id("erp_supplier", i),
            "in_time": biz_datetime(i * 6 + 4, 9, 30),
            "status": ERP_PURCHASE_ORDER_STATUS[i % len(ERP_PURCHASE_ORDER_STATUS)],
            "total_count": qty(sum(l["purchase_count"] for l in lines)),
            "total_amount": money(total), "remark": "DEMO_演示采购订单",
        })
        po_items.append(lines)
    b.emit("erp_purchase_order", po_heads)

    poi_rows = []
    for i, lines in enumerate(po_items):
        for l in lines:
            poi_rows.append({"id": None, "master_id": po_ids[i], **l, "remark": "DEMO_采购订单明细"})
    poi_ids = b.alloc("erp_purchase_order_items", len(poi_rows))
    for row, rid in zip(poi_rows, poi_ids):
        row["id"] = rid
    b.emit("erp_purchase_order_items", poi_rows)

    # --- 采购入库 + 明细
    n_pi = 6 * s
    pi_ids = b.alloc("erp_purchase_in", n_pi)
    pi_items, pi_heads = [], []
    for i in range(n_pi):
        lines, total = [], 0.0
        for k in range(2):
            cnt = qty(b.rnd.uniform(10, 120))
            price = money(b.rnd.uniform(10, 900))
            amt = money(cnt * price)
            total += amt
            lines.append({"product_id": b.m_id("erp_product", i * 2 + k),
                          "purchase_count": cnt, "purchase_price": price, "total_price": amt})
        pi_heads.append({
            "id": pi_ids[i], "no": b.biz_no("PIN", i), "supplier_id": b.m_id("erp_supplier", i),
            "in_time": biz_datetime(i * 9 + 6, 15, 30),
            "status": ERP_STOCK_STATUS[i % len(ERP_STOCK_STATUS)],
            "payment_status": ERP_PAY_STATUS[i % len(ERP_PAY_STATUS)],
            "total_count": qty(sum(l["purchase_count"] for l in lines)),
            "total_amount": money(total), "remark": "DEMO_演示采购入库",
        })
        pi_items.append(lines)
    b.emit("erp_purchase_in", pi_heads)

    pii_rows = []
    for i, lines in enumerate(pi_items):
        for l in lines:
            pii_rows.append({"id": None, "master_id": pi_ids[i], **l, "remark": "DEMO_采购入库明细"})
    pii_ids = b.alloc("erp_purchase_in_items", len(pii_rows))
    for row, rid in zip(pii_rows, pii_ids):
        row["id"] = rid
    b.emit("erp_purchase_in_items", pii_rows)

    # --- 采购退货 + 明细
    n_pr = 2 * s
    pr_ids = b.alloc("erp_purchase_return", n_pr)
    pr_items, pr_heads = [], []
    for i in range(n_pr):
        lines, total = [], 0.0
        for k in range(2):
            cnt = qty(b.rnd.uniform(2, 15))
            price = money(b.rnd.uniform(10, 900))
            amt = money(cnt * price)
            total += amt
            lines.append({"product_id": b.m_id("erp_product", i * 2 + k + 3),
                          "purchase_count": cnt, "purchase_price": price, "total_price": amt})
        pr_heads.append({
            "id": pr_ids[i], "no": b.biz_no("PRT", i), "supplier_id": b.m_id("erp_supplier", i + 1),
            "in_time": biz_datetime(i * 13 + 8, 10, 0),
            "status": ERP_STOCK_STATUS[i % len(ERP_STOCK_STATUS)],
            "refund_status": ERP_REFUND_STATUS[i % len(ERP_REFUND_STATUS)],
            "total_count": qty(sum(l["purchase_count"] for l in lines)),
            "total_amount": money(total), "remark": "DEMO_演示采购退货",
        })
        pr_items.append(lines)
    b.emit("erp_purchase_return", pr_heads)

    pri_rows = []
    for i, lines in enumerate(pr_items):
        for l in lines:
            pri_rows.append({"id": None, "master_id": pr_ids[i], **l, "remark": "DEMO_采购退货明细"})
    pri_ids = b.alloc("erp_purchase_return_items", len(pri_rows))
    for row, rid in zip(pri_rows, pri_ids):
        row["id"] = rid
    b.emit("erp_purchase_return_items", pri_rows)
    # --- 其它入库 + 明细
    n_si = 5 * s
    si_ids = b.alloc("erp_stock_in", n_si)
    si_rows, si_heads = [], []
    for i in range(n_si):
        lines, total = [], 0.0
        for k in range(2):
            cnt = qty(b.rnd.uniform(10, 80))
            total += cnt
            lines.append({"product_id": b.m_id("erp_product", i * 2 + k + 4),
                          "warehouse_id": b.m_id("erp_warehouse", i), "count": cnt})
        si_heads.append({
            "id": si_ids[i], "no": b.biz_no("SIN", i), "supplier_id": b.m_id("erp_supplier", i + 2),
            "in_time": biz_datetime(i * 10 + 2, 9, 0),
            "status": ERP_STOCK_STATUS[i % len(ERP_STOCK_STATUS)],
            "total_count": qty(total), "remark": "DEMO_演示其它入库"})
        si_rows.append(lines)
    b.emit("erp_stock_in", si_heads)

    sii = []
    for i, lines in enumerate(si_rows):
        for l in lines:
            sii.append({"id": None, "master_id": si_ids[i], **l, "remark": "DEMO_其它入库明细"})
    sii_ids = b.alloc("erp_stock_in_item", len(sii))
    for row, rid in zip(sii, sii_ids):
        row["id"] = rid
    b.emit("erp_stock_in_item", sii)

    # --- 其它出库 + 明细
    n_so2 = 5 * s
    so2_ids = b.alloc("erp_stock_out", n_so2)
    so2_rows, so2_heads = [], []
    for i in range(n_so2):
        lines, total = [], 0.0
        for k in range(2):
            cnt = qty(b.rnd.uniform(5, 50))
            total += cnt
            lines.append({"product_id": b.m_id("erp_product", i * 2 + k + 5),
                          "warehouse_id": b.m_id("erp_warehouse", i + 1), "count": cnt})
        so2_heads.append({
            "id": so2_ids[i], "no": b.biz_no("SOUT2", i), "customer_id": b.m_id("erp_customer", i + 3),
            "out_time": biz_datetime(i * 10 + 7, 16, 0),
            "status": ERP_STOCK_STATUS[i % len(ERP_STOCK_STATUS)],
            "total_count": qty(total), "remark": "DEMO_演示其它出库"})
        so2_rows.append(lines)
    b.emit("erp_stock_out", so2_heads)

    so2i = []
    for i, lines in enumerate(so2_rows):
        for l in lines:
            so2i.append({"id": None, "master_id": so2_ids[i], **l, "remark": "DEMO_其它出库明细"})
    so2i_ids = b.alloc("erp_stock_out_item", len(so2i))
    for row, rid in zip(so2i, so2i_ids):
        row["id"] = rid
    b.emit("erp_stock_out_item", so2i)

    # --- 调拨 + 明细
    n_mv = 4 * s
    mv_ids = b.alloc("erp_stock_move", n_mv)
    mv_rows, mv_heads = [], []
    for i in range(n_mv):
        lines, total = [], 0.0
        for k in range(2):
            cnt = qty(b.rnd.uniform(5, 40))
            total += cnt
            lines.append({"product_id": b.m_id("erp_product", i * 2 + k + 6), "count": cnt})
        mv_heads.append({
            "id": mv_ids[i], "no": b.biz_no("MV", i),
            "from_warehouse_id": b.m_id("erp_warehouse", i),
            "to_warehouse_id": b.m_id("erp_warehouse", (i + 1) % len(b.master.rows("erp_warehouse"))),
            "move_time": biz_datetime(i * 12 + 4, 10, 45),
            "status": ERP_STOCK_STATUS[i % len(ERP_STOCK_STATUS)],
            "total_count": qty(total), "remark": "DEMO_演示库存调拨"})
        mv_rows.append(lines)
    b.emit("erp_stock_move", mv_heads)

    mvi = []
    for i, lines in enumerate(mv_rows):
        for l in lines:
            mvi.append({"id": None, "master_id": mv_ids[i], **l, "remark": "DEMO_调拨明细"})
    mvi_ids = b.alloc("erp_stock_move_item", len(mvi))
    for row, rid in zip(mvi, mvi_ids):
        row["id"] = rid
    b.emit("erp_stock_move_item", mvi)

    # --- 库存余额（每个 产品×仓库 组合一行）
    products = b.master.rows("erp_product")
    whs = b.master.rows("erp_warehouse")
    st_rows = []
    for i, p in enumerate(products):
        for k, w in enumerate(whs):
            st_rows.append({"id": None, "product_id": p["id"], "warehouse_id": w["id"],
                            "count": qty(b.rnd.uniform(0, 500))})
    st_ids = b.alloc("erp_stock", len(st_rows))
    for row, rid in zip(st_rows, st_ids):
        row["id"] = rid
    b.emit("erp_stock", st_rows)

    # --- 库存流水：为每个入库/出库/调拨明细生成一条流水
    rec = []
    for i, lines in enumerate(si_rows):
        for l in lines:
            rec.append({"product_id": l["product_id"], "warehouse_id": l["warehouse_id"],
                        "biz_type": 10, "count": l["count"], "bill_id": si_ids[i]})
    for i, lines in enumerate(so2_rows):
        for l in lines:
            rec.append({"product_id": l["product_id"], "warehouse_id": l["warehouse_id"],
                        "biz_type": 20, "count": l["count"], "bill_id": so2_ids[i]})
    for i, lines in enumerate(mv_rows):
        for l in lines:
            rec.append({"product_id": l["product_id"], "warehouse_id": mv_heads[i]["from_warehouse_id"],
                        "biz_type": 40, "count": l["count"], "bill_id": mv_ids[i]})
            rec.append({"product_id": l["product_id"], "warehouse_id": mv_heads[i]["to_warehouse_id"],
                        "biz_type": 30, "count": l["count"], "bill_id": mv_ids[i]})
    rec_ids = b.alloc("erp_stock_record", len(rec))
    for row, rid in zip(rec, rec_ids):
        row["id"] = rid
        row["total_count"] = qty(row["count"] * b.rnd.uniform(1.0, 3.0))
        row["bill_item_id"] = None
    b.emit("erp_stock_record", rec)

    # --- 盘点单 + 明细
    n_sc = 3 * s
    sc_ids = b.alloc("erp_stock_check", n_sc)
    sc_rows, sc_heads = [], []
    for i in range(n_sc):
        lines, total = [], 0.0
        for k in range(3):
            stock = qty(b.rnd.uniform(50, 300))
            actual = qty(stock + b.rnd.uniform(-8, 8))
            total += actual
            lines.append({"product_id": b.m_id("erp_product", i * 3 + k),
                          "stock_count": stock, "actual_count": actual,
                          "diff_count": qty(actual - stock)})
        sc_heads.append({
            "id": sc_ids[i], "no": b.biz_no("SC", i), "warehouse_id": b.m_id("erp_warehouse", i),
            "check_time": biz_datetime(i * 20 + 15, 17, 0),
            "status": ERP_STOCK_STATUS[i % len(ERP_STOCK_STATUS)],
            "total_count": qty(total), "remark": "DEMO_演示库存盘点"})
        sc_rows.append(lines)
    b.emit("erp_stock_check", sc_heads)

    sci = []
    for i, lines in enumerate(sc_rows):
        for l in lines:
            sci.append({"id": None, "master_id": sc_ids[i], **l, "remark": "DEMO_盘点明细"})
    sci_ids = b.alloc("erp_stock_check_item", len(sci))
    for row, rid in zip(sci, sci_ids):
        row["id"] = rid
    b.emit("erp_stock_check_item", sci)

    # --- 收款单 + 明细（核销销售出库）
    outs = b.own("erp_sale_out")
    n_fr = min(6 * s, len(outs))
    fr_ids = b.alloc("erp_finance_receipt", n_fr)
    fr_heads, fr_items = [], []
    for i in range(n_fr):
        o = outs[i]
        total = money(float(o["total_amount"]))
        paid = money(total * (1.0 if i % 3 == 0 else b.rnd.uniform(0.3, 0.8)))
        fr_heads.append({
            "id": fr_ids[i], "no": b.biz_no("REC", i), "customer_id": o["customer_id"],
            "account_id": acct_ids[i % len(acct_ids)],
            "finance_time": biz_datetime(i * 9 + 12, 15, 0),
            "status": ERP_FINANCE_STATUS[i % len(ERP_FINANCE_STATUS)],
            "total_price": total, "payment_price": paid, "remark": "DEMO_演示收款单"})
        fr_items.append({"master_id": fr_ids[i], "bill_id": o["id"],
                         "bill_type": ERP_FINANCE_BILL_TYPE_SALE, "payment_price": paid})
    b.emit("erp_finance_receipt", fr_heads)

    fri_ids = b.alloc("erp_finance_receipt_item", len(fr_items))
    for row, rid in zip(fr_items, fri_ids):
        row["id"] = rid
        row["remark"] = "DEMO_收款核销明细"
    b.emit("erp_finance_receipt_item", fr_items)

    # --- 付款单 + 明细（核销采购入库）
    pins = b.own("erp_purchase_in")
    n_fp = min(5 * s, len(pins))
    fp_ids = b.alloc("erp_finance_payment", n_fp)
    fp_heads, fp_items = [], []
    for i in range(n_fp):
        pi_ = pins[i]
        total = money(float(pi_["total_amount"]))
        paid = money(total * (1.0 if i % 2 == 0 else b.rnd.uniform(0.4, 0.9)))
        fp_heads.append({
            "id": fp_ids[i], "no": b.biz_no("PAY", i), "supplier_id": pi_["supplier_id"],
            "account_id": acct_ids[i % len(acct_ids)],
            "finance_time": biz_datetime(i * 11 + 14, 15, 30),
            "status": ERP_FINANCE_STATUS[i % len(ERP_FINANCE_STATUS)],
            "total_price": total, "payment_price": paid, "remark": "DEMO_演示付款单"})
        fp_items.append({"master_id": fp_ids[i], "bill_id": pi_["id"],
                         "bill_type": ERP_FINANCE_BILL_TYPE_PURCHASE, "payment_price": paid})
    b.emit("erp_finance_payment", fp_heads)

    fpi_ids = b.alloc("erp_finance_payment_item", len(fp_items))
    for row, rid in zip(fp_items, fpi_ids):
        row["id"] = rid
        row["remark"] = "DEMO_付款核销明细"
    b.emit("erp_finance_payment_item", fp_items)


# ------------------------------ MES 单据 ------------------------------
def build_mes(b: Builder):
    s = b.s
    item_ids = [r["id"] for r in b.own("mes_md_item")]
    st_ids = [r["id"] for r in b.own("mes_md_workstation")]
    rt_ids = [r["id"] for r in b.own("mes_pro_route")]
    pr_ids = [r["id"] for r in b.own("mes_pro_process")]
    cl_ids = [r["id"] for r in b.own("mes_md_client")]
    vd_ids = [r["id"] for r in b.own("mes_md_vendor")]
    um_ids = [r["id"] for r in b.own("mes_md_unit_measure")]
    it_ids = [r["id"] for r in b.own("mes_md_item_type")]

    # --- 生产工单 + BOM
    n_wo = 8 * s
    wo_ids = b.alloc("mes_pro_work_order", n_wo)
    wo_heads = []
    for i in range(n_wo):
        q = qty(b.rnd.uniform(50, 800))
        produced = qty(q * b.rnd.uniform(0.2, 1.0))
        wo_heads.append({
            "id": wo_ids[i], "code": "DEMO-WO-%05d" % (i + 1),
            "name": "DEMO_工单%05d" % (i + 1),
            "type": MES_WO_TYPE[i % len(MES_WO_TYPE)],
            "order_source_type": MES_WO_SOURCE_TYPE[i % len(MES_WO_SOURCE_TYPE)],
            "order_source_code": "DEMO-SRC-%05d" % (i + 1),
            "product_id": b.m_id("erp_product", i),
            "quantity": q, "quantity_produced": produced, "quantity_changed": 0,
            "quantity_scheduled": q, "client_id": cl_ids[i % len(cl_ids)],
            "vendor_id": vd_ids[i % len(vd_ids)], "batch_code": "DEMO-BATCH-%04d" % (i + 1),
            "request_date": biz_date(i * 5 + 6), "parent_id": 0,
            "finish_date": biz_date(i * 5 + 20) if i % 3 == 0 else None,
            "status": MES_WO_STATUS[i % len(MES_WO_STATUS)], "remark": "DEMO_演示生产工单",
        })
    b.emit("mes_pro_work_order", wo_heads)

    wob = []
    for i in range(n_wo):
        for k in range(3):
            wob.append({"id": None, "work_order_id": wo_ids[i],
                        "item_id": item_ids[(i * 3 + k) % len(item_ids)],
                        "quantity": qty(b.rnd.uniform(2, 40)), "remark": "DEMO_工单物料"})
    wob_ids = b.alloc("mes_pro_work_order_bom", len(wob))
    for row, rid in zip(wob, wob_ids):
        row["id"] = rid
    b.emit("mes_pro_work_order_bom", wob)

    # --- 生产任务（每工单 3 个任务）
    tk = []
    for i in range(n_wo):
        for k in range(3):
            q = qty(b.rnd.uniform(20, 300))
            prod = qty(q * b.rnd.uniform(0.4, 1.0))
            qual = qty(prod * b.rnd.uniform(0.9, 1.0))
            tk.append({
                "id": None, "code": "DEMO-TASK-%05d-%d" % (i + 1, k + 1),
                "name": "DEMO_任务%05d-%d" % (i + 1, k + 1),
                "work_order_id": wo_ids[i], "workstation_id": st_ids[(i + k) % len(st_ids)],
                "route_id": rt_ids[i % len(rt_ids)], "process_id": pr_ids[(k * 2) % len(pr_ids)],
                "item_id": item_ids[(i + k) % len(item_ids)],
                "quantity": q, "produced_quantity": prod, "qualify_quantity": qual,
                "unqualify_quantity": qty(prod - qual), "changed_quantity": 0,
                "client_id": cl_ids[(i + k) % len(cl_ids)],
                "start_time": biz_datetime(i * 5 + k * 2 + 7, 8, 0),
                "duration": b.rnd.randint(2, 16),
                "end_time": biz_datetime(i * 5 + k * 2 + 8, 18, 0),
                "color_code": "DEMO_C%d" % (k + 1),
                "status": MES_TASK_STATUS[i % len(MES_TASK_STATUS)], "remark": "DEMO_演示生产任务",
            })
    tk_ids = b.alloc("mes_pro_task", len(tk))
    for row, rid in zip(tk, tk_ids):
        row["id"] = rid
    b.emit("mes_pro_task", tk)

    # --- 任务发料：每个任务 2 行
    ti = []
    for i, t in enumerate(tk):
        for k in range(2):
            issued = qty(b.rnd.uniform(10, 120))
            ti.append({
                "id": None, "task_id": t["id"], "work_order_id": t["work_order_id"],
                "workstation_id": t["workstation_id"], "source_doc_type": "WORK_ORDER",
                "source_doc_code": "DEMO-WO-%05d" % (i // 3 + 1),
                "batch_code": "DEMO-BATCH-%04d" % (i // 3 + 1),
                "item_id": item_ids[(i * 2 + k) % len(item_ids)],
                "unit_measure_id": um_ids[(i + k) % len(um_ids)],
                "issued_quantity": issued, "available_quantity": qty(issued * b.rnd.uniform(1.0, 1.5)),
                "used_quantity": qty(issued * b.rnd.uniform(0.6, 1.0)),
                "remark": "DEMO_演示任务发料",
            })
    ti_ids = b.alloc("mes_pro_task_issue", len(ti))
    for row, rid in zip(ti, ti_ids):
        row["id"] = rid
    b.emit("mes_pro_task_issue", ti)

    # --- 报工：每个任务 1 条
    fb = []
    for i, t in enumerate(tk):
        sched = float(t["quantity"])
        feed = qty(sched * b.rnd.uniform(0.5, 1.0))
        qual = qty(feed * b.rnd.uniform(0.9, 1.0))
        unqual = qty(feed - qual)
        fb.append({
            "id": None, "code": "DEMO-FB-%05d" % (i + 1),
            "type": MES_FEEDBACK_TYPE[i % len(MES_FEEDBACK_TYPE)], "channel": ("DEMO_WEB", "DEMO_PAD", "DEMO_TERMINAL")[i % 3],
            "feedback_time": biz_datetime(i * 3 + 9, 17, 30),
            "workstation_id": t["workstation_id"], "route_id": t["route_id"],
            "process_id": t["process_id"], "work_order_id": t["work_order_id"], "task_id": t["id"],
            "item_id": t["item_id"], "expire_date": biz_date(i * 3 + 180),
            "lot_number": "DEMO-LOT-%05d" % (i + 1),
            "scheduled_quantity": qty(sched), "feedback_quantity": feed,
            "qualified_quantity": qual, "unqualified_quantity": unqual,
            "uncheck_quantity": 0, "labor_scrap_quantity": qty(unqual * 0.2),
            "material_scrap_quantity": qty(unqual * 0.3),
            "other_scrap_quantity": qty(unqual * 0.1),
            "status": MES_TASK_STATUS[i % len(MES_TASK_STATUS)], "remark": "DEMO_演示报工",
        })
    fb_ids = b.alloc("mes_pro_feedback", len(fb))
    for row, rid in zip(fb, fb_ids):
        row["id"] = rid
    b.emit("mes_pro_feedback", fb)

    # --- MES 物料库存：每个物料×仓库 一行
    mwh = b.master.rows("wms_warehouse")
    mloc = b.master.rows("wms_location")
    ms = []
    for i, it in enumerate(item_ids):
        for k, w in enumerate(mwh):
            ms.append({
                "id": None, "item_type_id": it_ids[i % len(it_ids)], "item_id": it,
                "batch_code": "DEMO-BATCH-%04d" % ((i + k) % 100 + 1),
                "warehouse_id": w["id"], "location_id": mloc[(i + k) % len(mloc)]["id"],
                "vendor_id": vd_ids[(i + k) % len(vd_ids)],
                "quantity": qty(b.rnd.uniform(0, 1200)),
                "receipt_time": biz_datetime((i + k) % 120, 9, 0), "frozen": 0,
            })
    ms_ids = b.alloc("mes_wm_material_stock", len(ms))
    for row, rid in zip(ms, ms_ids):
        row["id"] = rid
    b.emit("mes_wm_material_stock", ms)

# ------------------------------ WMS 单据 ------------------------------
def build_wms(b: Builder):
    s = b.s
    skus = b.own("wms_item_sku")
    sku_ids = [r["id"] for r in skus]
    whs = b.master.rows("wms_warehouse")
    wh_ids = [r["id"] for r in whs]
    loc_ids = [r["id"] for r in b.master.rows("wms_location")]
    merch_ids = [r["id"] for r in b.master.rows("wms_merchant")]

    def sku_name(i):
        return skus[i % len(skus)].get("name")

    # --- ASN 到货通知 + 明细
    n_asn = 5 * s
    asn_ids = b.alloc("wms_asn_order", n_asn)
    asn_heads, asn_lines = [], []
    for i in range(n_asn):
        lines, total = [], 0.0
        for k in range(2):
            exp = qty(b.rnd.uniform(50, 400))
            recv = qty(exp * b.rnd.uniform(0.6, 1.0))
            total += recv
            lines.append({"sku_id": sku_ids[(i * 2 + k) % len(sku_ids)],
                          "expected_quantity": exp, "received_quantity": recv,
                          "product_name": sku_name(i * 2 + k), "unit": "DEMO_个",
                          "lot_number": "DEMO-LOT-%04d" % (i * 2 + k + 1),
                          "production_date": biz_date(i * 4), "expiry_date": biz_date(i * 4 + 365)})
        asn_heads.append({
            "id": asn_ids[i], "no": b.biz_no("ASN", i),
            "supplier_id": b.m_id("erp_supplier", i), "warehouse_id": wh_ids[i % len(wh_ids)],
            "expected_arrival_time": biz_datetime(i * 6 + 2, 8, 30),
            "actual_arrival_time": biz_datetime(i * 6 + 3, 10, 15),
            "total_quantity": qty(total), "total_amount": money(total * b.rnd.uniform(20, 300)),
            "transport_mode": 10, "carrier_name": "DEMO_演示承运商",
            "vehicle_no": "DEMO-TEST-%05d" % (i + 1),
            "status": WMS_ASN_STATUS[i % len(WMS_ASN_STATUS)], "remark": "DEMO_演示到货通知"})
        asn_lines.append(lines)
    b.emit("wms_asn_order", asn_heads)

    asnd = []
    for i, lines in enumerate(asn_lines):
        for l in lines:
            asnd.append({"id": None, "asn_order_id": asn_ids[i], **l, "remark": "DEMO_ASN 明细"})
    asnd_ids = b.alloc("wms_asn_order_detail", len(asnd))
    for row, rid in zip(asnd, asnd_ids):
        row["id"] = rid
    b.emit("wms_asn_order_detail", asnd)

    # --- 入库单 + 明细
    n_rc = 8 * s
    rc_ids = b.alloc("wms_receipt_order", n_rc)
    rc_heads, rc_lines = [], []
    for i in range(n_rc):
        lines, total = [], 0.0
        for k in range(2):
            q = qty(b.rnd.uniform(30, 300))
            price = money(b.rnd.uniform(15, 800))
            total += q
            lines.append({"sku_id": sku_ids[(i * 2 + k) % len(sku_ids)],
                          "warehouse_id": wh_ids[i % len(wh_ids)], "quantity": q,
                          "price": price, "total_price": money(q * price)})
        rc_heads.append({
            "id": rc_ids[i], "no": b.biz_no("RCP", i),
            "type": WMS_RECEIPT_TYPE[i % len(WMS_RECEIPT_TYPE)],
            "order_time": biz_datetime(i * 5 + 3, 9, 15),
            "status": WMS_RECEIPT_STATUS[i % len(WMS_RECEIPT_STATUS)],
            "biz_order_no": "DEMO-PO-%05d" % (i + 1),
            "merchant_id": merch_ids[i % len(merch_ids)],
            "warehouse_id": wh_ids[i % len(wh_ids)], "total_quantity": qty(total),
            "total_price": money(sum(float(l["total_price"]) for l in lines)),
            "qc_biz_type": "PURCHASE_IN", "remark": "DEMO_演示入库单"})
        rc_lines.append(lines)
    b.emit("wms_receipt_order", rc_heads)

    rcd = []
    for i, lines in enumerate(rc_lines):
        for l in lines:
            rcd.append({"id": None, "order_id": rc_ids[i], **l})
    rcd_ids = b.alloc("wms_receipt_order_detail", len(rcd))
    for row, rid in zip(rcd, rcd_ids):
        row["id"] = rid
    b.emit("wms_receipt_order_detail", rcd)

    # --- 库存余额（每个 SKU×仓库）
    inv = []
    for i, sk in enumerate(sku_ids):
        for k, w in enumerate(wh_ids):
            q = qty(b.rnd.uniform(0, 800))
            locked = qty(q * b.rnd.uniform(0, 0.1))
            frozen = qty(q * b.rnd.uniform(0, 0.05))
            inv.append({"id": None, "sku_id": sk, "warehouse_id": w, "quantity": q,
                        "available_quantity": qty(q - locked - frozen),
                        "locked_quantity": locked, "frozen_quantity": frozen,
                        "owner_id": merch_ids[(i + k) % len(merch_ids)]})
    inv_ids = b.alloc("wms_inventory", len(inv))
    for row, rid in zip(inv, inv_ids):
        row["id"] = rid
    b.emit("wms_inventory", inv)

    # --- 出库单 + 明细
    n_sh = 8 * s
    sh_ids = b.alloc("wms_shipment_order", n_sh)
    sh_heads, sh_lines = [], []
    for i in range(n_sh):
        lines, total = [], 0.0
        for k in range(2):
            q = qty(b.rnd.uniform(10, 150))
            price = money(b.rnd.uniform(15, 800))
            total += q
            lines.append({"sku_id": sku_ids[(i * 2 + k + 1) % len(sku_ids)],
                          "warehouse_id": wh_ids[i % len(wh_ids)], "quantity": q,
                          "price": price, "total_price": money(q * price)})
        sh_heads.append({
            "id": sh_ids[i], "no": b.biz_no("SHP", i),
            "type": WMS_SHIPMENT_TYPE[i % len(WMS_SHIPMENT_TYPE)],
            "order_time": biz_datetime(i * 5 + 4, 13, 45),
            "status": WMS_SHIPMENT_STATUS[i % len(WMS_SHIPMENT_STATUS)],
            "biz_order_no": "DEMO-SO-%05d" % (i + 1),
            "merchant_id": merch_ids[(i + 1) % len(merch_ids)],
            "warehouse_id": wh_ids[i % len(wh_ids)], "total_quantity": qty(total),
            "total_price": money(sum(float(l["total_price"]) for l in lines)),
            "remark": "DEMO_演示出库单"})
        sh_lines.append(lines)
    b.emit("wms_shipment_order", sh_heads)

    shd = []
    for i, lines in enumerate(sh_lines):
        for l in lines:
            shd.append({"id": None, "order_id": sh_ids[i], **l})
    shd_ids = b.alloc("wms_shipment_order_detail", len(shd))
    for row, rid in zip(shd, shd_ids):
        row["id"] = rid
    b.emit("wms_shipment_order_detail", shd)

    # --- 波次 + 明细（每波次含 2 个出库单）
    n_wv = 4 * s
    wv_ids = b.alloc("wms_wave_order", n_wv)
    wv_heads, wv_lines = [], []
    for i in range(n_wv):
        picked = []
        for k in range(2):
            o = sh_ids[(i * 2 + k) % len(sh_ids)]
            det = [d for d in shd if d["order_id"] == o]
            for d in det:
                pq = qty(float(d["quantity"]) * b.rnd.uniform(0.5, 1.0))
                picked.append({"wave_order_id": wv_ids[i], "shipment_order_id": o,
                               "sku_id": d["sku_id"], "pick_quantity": float(d["quantity"]),
                               "picked_quantity": pq})
        wv_heads.append({
            "id": wv_ids[i], "no": b.biz_no("WAVE", i), "warehouse_id": wh_ids[i % len(wh_ids)],
            "strategy": 10, "order_time": biz_datetime(i * 8 + 5, 7, 30),
            "status": WMS_WAVE_STATUS[i % len(WMS_WAVE_STATUS)],
            "shipment_count": 2, "sku_count": len(picked),
            "total_quantity": qty(sum(p["pick_quantity"] for p in picked)),
            "total_price": money(sum(p["pick_quantity"] for p in picked) * b.rnd.uniform(20, 300)),
            "remark": "DEMO_演示波次"})
        wv_lines.append(picked)
    b.emit("wms_wave_order", wv_heads)

    wvd = []
    for lines in wv_lines:
        for l in lines:
            wvd.append({"id": None, **l, "remark": "DEMO_波次明细"})
    wvd_ids = b.alloc("wms_wave_order_detail", len(wvd))
    for row, rid in zip(wvd, wvd_ids):
        row["id"] = rid
    b.emit("wms_wave_order_detail", wvd)

    # --- 拣货任务（每行波次明细 1 个任务）
    pt = []
    for i, row in enumerate(wvd):
        pt.append({
            "id": None, "task_no": "DEMO-PICK-%05d" % (i + 1), "sku_id": row["sku_id"],
            "quantity": float(row["pick_quantity"]), "picked_quantity": float(row["picked_quantity"]),
            "status": WMS_PICK_STATUS[i % len(WMS_PICK_STATUS)],
            "shipment_order_id": row["shipment_order_id"], "wave_order_id": row["wave_order_id"],
            "product_name": sku_name(i), "location_id": loc_ids[i % len(loc_ids)],
            "pick_sequence": (i % 20) + 1,
            "pick_time": biz_datetime(i * 2 + 6, 10, 30), "remark": "DEMO_演示拣货任务"})
    pt_ids = b.alloc("wms_pick_task", len(pt))
    for row, rid in zip(pt, pt_ids):
        row["id"] = rid
    b.emit("wms_pick_task", pt)

    # --- 库内移位 + 明细
    n_mo = 3 * s
    mo_ids = b.alloc("wms_movement_order", n_mo)
    mo_heads, mo_lines = [], []
    for i in range(n_mo):
        src = wh_ids[i % len(wh_ids)]
        tgt = wh_ids[(i + 1) % len(wh_ids)]
        lines = []
        for k in range(2):
            q = qty(b.rnd.uniform(5, 60))
            price = money(b.rnd.uniform(15, 800))
            lines.append({"sku_id": sku_ids[(i * 2 + k + 3) % len(sku_ids)],
                          "source_warehouse_id": src, "target_warehouse_id": tgt,
                          "quantity": q, "price": price, "total_price": money(q * price)})
        mo_heads.append({
            "id": mo_ids[i], "no": b.biz_no("MOV", i), "order_time": biz_datetime(i * 14 + 9, 11, 30),
            "status": WMS_MOVE_STATUS[i % len(WMS_MOVE_STATUS)],
            "source_warehouse_id": src, "target_warehouse_id": tgt,
            "total_quantity": qty(sum(float(l["quantity"]) for l in lines)),
            "total_price": money(sum(float(l["total_price"]) for l in lines)),
            "remark": "DEMO_演示库内移位"})
        mo_lines.append(lines)
    b.emit("wms_movement_order", mo_heads)

    mod = []
    for i, lines in enumerate(mo_lines):
        for l in lines:
            mod.append({"id": None, "order_id": mo_ids[i], **l})
    mod_ids = b.alloc("wms_movement_order_detail", len(mod))
    for row, rid in zip(mod, mod_ids):
        row["id"] = rid
    b.emit("wms_movement_order_detail", mod)


# ------------------------------ QMS 单据 ------------------------------
def build_qms(b: Builder):
    s = b.s
    ii_ids = [r["id"] for r in b.own("qms_inspection_item")]
    inst_ids = [r["id"] for r in b.own("qms_instrument")]
    wo_ids = [r["id"] for r in b.own("mes_pro_work_order")]

    # --- 检验单 + 记录
    n_io = 8 * s
    io_ids = b.alloc("qms_inspection_order", n_io)
    io_heads, io_recs = [], []
    for i in range(n_io):
        io_heads.append({
            "id": io_ids[i], "order_no": b.biz_no("QC", i),
            "type": QMS_INSP_TYPE[i % len(QMS_INSP_TYPE)],
            "supplier_id": b.m_id("erp_supplier", i), "batch_no": "DEMO-BATCH-%04d" % (i + 1),
            "work_order_id": wo_ids[i % len(wo_ids)] if wo_ids and i % 2 == 0 else None,
            "product_id": b.m_id("erp_product", i),
            "inspector": "DEMO_质检员%02d" % (i % 5 + 1),
            "inspect_time": biz_datetime(i * 6 + 4, 14, 0),
            "status": QMS_INSP_ORDER_STATUS[i % len(QMS_INSP_ORDER_STATUS)],
            "acceptance_quantity": b.rnd.randint(80, 500),
            "reject_quantity": b.rnd.randint(0, 20),
            "biz_type": "PURCHASE_IN", "remark": "DEMO_演示检验单"})
        for k in range(3):
            item = ii_ids[(i * 3 + k) % len(ii_ids)]
            io_recs.append({"order_id": io_ids[i], "item_id": item,
                            "result": QMS_INSP_RESULT[(i + k) % len(QMS_INSP_RESULT)],
                            "severity": QMS_SEVERITY[(i + k) % len(QMS_SEVERITY)],
                            "measured_value": "DEMO_%.3f" % b.rnd.uniform(0.01, 480.0),
                            "inspector": "DEMO_质检员%02d" % (i % 5 + 1),
                            "inspect_time": biz_datetime(i * 6 + 4, 14, 30)})
    b.emit("qms_inspection_order", io_heads)

    ir_ids = b.alloc("qms_inspection_record", len(io_recs))
    for row, rid in zip(io_recs, ir_ids):
        row["id"] = rid
        row["remark"] = "DEMO_检验记录"
    b.emit("qms_inspection_record", io_recs)

    # --- 不合格品处置单（NCR）
    n_ncr = 4 * s
    ncr_ids = b.alloc("qms_ncr_document", n_ncr)
    b.emit("qms_ncr_document", [{
        "id": ncr_ids[i], "ncr_no": b.biz_no("NCR", i),
        "source": 10,
        "defect_description": "DEMO_演示不合格描述%d：外观划伤超出允收范围" % (i + 1),
        "inspection_order_id": io_ids[i % len(io_ids)],
        "product_id": b.m_id("erp_product", i), "supplier_id": b.m_id("erp_supplier", i),
        "work_order_id": wo_ids[i % len(wo_ids)] if wo_ids else None,
        "defect_level": QMS_SEVERITY[i % len(QMS_SEVERITY)],
        "quantity": qty(b.rnd.uniform(1, 60)),
        "disposition": 60,
        "status": QMS_NCR_STATUS[i % len(QMS_NCR_STATUS)], "remark": "DEMO_演示 NCR",
    } for i in range(n_ncr)])

    # --- CAPA
    n_capa = 3 * s
    capa_ids = b.alloc("qms_capa_document", n_capa)
    b.emit("qms_capa_document", [{
        "id": capa_ids[i], "capa_no": b.biz_no("CAPA", i), "source": 10,
        "problem": "DEMO_演示问题%d：来料划伤反复发生" % (i + 1),
        "cause": "DEMO_演示直接原因：包装防护不足",
        "root_cause_analysis": "DEMO_演示根因：供应商包装规范未落地",
        "corrective_action": "DEMO_演示纠正措施：全批返工并要求供应商整改",
        "preventive_action": "DEMO_演示预防措施：修订入厂包装规范并纳入年度审核",
        "responsible_person": "DEMO_质量工程师%02d" % (i + 1),
        "due_date": biz_date(i * 10 + 30),
        "close_date": biz_date(i * 10 + 60) if i % 2 == 0 else None,
        "priority": 10 + (i % 3) * 10, "stage": 10 + (i % 4) * 10,
        "status": QMS_CAPA_STATUS[i % len(QMS_CAPA_STATUS)],
        "verification_result": QMS_VERIFY_RESULT[i % len(QMS_VERIFY_RESULT)],
        "verification_comment": "DEMO_演示验证意见",
        "verified_by": "DEMO_验证人%02d" % (i + 1),
        "verified_time": biz_datetime(i * 10 + 62, 16, 0) if i % 2 == 0 else None,
        "remark": "DEMO_演示 CAPA",
    } for i in range(n_capa)])

    # --- 客户投诉
    n_cc = 4 * s
    cc_ids = b.alloc("qms_customer_complaint", n_cc)
    b.emit("qms_customer_complaint", [{
        "id": cc_ids[i], "complaint_no": b.biz_no("CC", i),
        "customer_id": b.m_id("crm_customer", i),
        "customer_name": b.m_name("crm_customer", i, "name"),
        "product_id": b.m_id("erp_product", i),
        "product_name": b.m_name("erp_product", i, "name"),
        "complaint_content": "DEMO_演示投诉内容%d：交付批次存在外观缺陷" % (i + 1),
        "complaint_date": biz_date(i * 12 + 5),
        "root_cause": "DEMO_演示根因：过程检验漏检",
        "impact_scope": "DEMO_影响范围：单批次",
        "handle_type": QMS_COMPLAINT_HANDLE_TYPE[i % len(QMS_COMPLAINT_HANDLE_TYPE)],
        "handle_action": "DEMO_演示处理措施：换货并加强出货检验",
        "close_time": biz_datetime(i * 12 + 25, 17, 0) if i % 2 == 0 else None,
        "status": QMS_COMPLAINT_STATUS[i % len(QMS_COMPLAINT_STATUS)], "remark": "DEMO_演示投诉",
    } for i in range(n_cc)])

    # --- 器具校准记录（每个器具 2 条）
    cal = []
    for i, ins in enumerate(inst_ids):
        for k in range(2):
            cal.append({
                "id": None, "instrument_id": ins, "calibration_no": b.biz_no("CAL", i * 2 + k),
                "calibration_date": biz_date(i * 30 + k * 180),
                "calibration_result": QMS_CALIBRATION_RESULT[(i + k) % len(QMS_CALIBRATION_RESULT)],
                "calibration_organization": "DEMO_演示计量院",
                "deviation": qty(b.rnd.uniform(-0.02, 0.02)),
                "uncertainty": "DEMO_UNC_%.4f" % b.rnd.uniform(0.001, 0.01),
                "next_calibration_date": biz_date(i * 30 + k * 180 + 365),
                "sort": (k + 1) * 10, "remark": "DEMO_演示校准记录"})
    cal_ids = b.alloc("qms_instrument_calibration", len(cal))
    for row, rid in zip(cal, cal_ids):
        row["id"] = rid
    b.emit("qms_instrument_calibration", cal)


# ------------------------------ CRM 单据 ------------------------------
def build_crm(b: Builder):
    s = b.s
    # --- 线索
    n_clue = 12 * s
    clue_ids = b.alloc("crm_clue", n_clue)
    b.emit("crm_clue", [{
        "id": clue_ids[i],
        "name": "DEMO_线索%03d" % (i + 1),
        "follow_up_status": 1 if i % 3 else 0,
        "contact_last_time": biz_datetime(i * 3, 10, 30),
        "contact_last_content": "DEMO_演示沟通记录%d：初步需求确认" % (i + 1),
        "contact_next_time": biz_datetime(i * 3 + 7, 14, 0),
        "owner_user_id": None,
        "receive_count": 1 + i % 3,
        "transform_status": 1 if i % 2 == 0 else 0,
        "customer_id": b.m_id("crm_customer", i),
        "mobile": "1380020%04d" % (i + 1),
        "telephone": "DEMO-021-010%05d" % (i + 1),
        "qq": "DEMO_QQ_%06d" % (100000 + i),
        "wechat": "DEMO_WX_CLUE%03d" % (i + 1),
        "email": "clue%05d@%s" % (i + 1, EMAIL_DOMAIN),
        "area_id": None,
        "detail_address": "DEMO_演示地址-线索%03d" % (i + 1),
        "industry_id": None,
        "level": 1 + i % 5,
        "source": 1 + i % 6,
        "remark": "DEMO_演示 CRM 线索",
    } for i in range(n_clue)])

    # --- 商机 + 商机产品（先算明细，再回填商机金额，保证金额自洽）
    n_biz = 10 * s
    biz_ids = b.alloc("crm_business", n_biz)
    bp_rows = []
    for i in range(n_biz):
        for k in range(2):
            unit = money(b.rnd.uniform(500, 12000))
            cnt = money(b.rnd.uniform(1, 30))
            price = money(unit * 0.92)
            bp_rows.append({"id": None, "business_id": biz_ids[i],
                            "product_id": b.own_id("crm_product", i * 2 + k),
                            "product_price": unit, "business_price": price,
                            "count": cnt, "total_price": money(price * cnt)})
    bp_ids = b.alloc("crm_business_product", len(bp_rows))
    for row, rid in zip(bp_rows, bp_ids):
        row["id"] = rid

    biz_rows = []
    for i in range(n_biz):
        mine = [r for r in bp_rows if r["business_id"] == biz_ids[i]]
        total_product = money(sum(r["product_price"] * r["count"] for r in mine))
        total = money(sum(r["total_price"] for r in mine))
        biz_rows.append({
            "id": biz_ids[i], "name": "DEMO_商机%03d" % (i + 1),
            "customer_id": b.m_id("crm_customer", i + 1),
            "follow_up_status": 1 if i % 2 == 0 else 0,
            "contact_last_time": biz_datetime(i * 5 + 2, 11, 0),
            "contact_next_time": biz_datetime(i * 5 + 12, 15, 0),
            "owner_user_id": None,
            "status_type_id": b.own_id("crm_business_status_type", i),
            "status_id": b.own_id("crm_business_status", i * 4 + (i % 4)),
            "end_status": 1 if i % 3 == 0 else 0,
            "end_remark": "DEMO_演示结束备注",
            "deal_time": biz_datetime(i * 5 + 20, 16, 30) if i % 3 == 0 else None,
            "total_product_price": total_product,
            "discount_percent": money(8.0),
            "total_price": total,
            "remark": "DEMO_演示 CRM 商机",
        })
    b.emit("crm_business", biz_rows)
    b.emit("crm_business_product", bp_rows)

    # --- 联系人（合同签署人引用）
    n_cust = len(b.master.rows("crm_customer"))
    n_contact = n_cust * s
    ct_ids = b.alloc("crm_contact", n_contact)
    b.emit("crm_contact", [{
        "id": ct_ids[i],
        "name": "DEMO_联系人%03d" % (i + 1),
        "customer_id": b.m_id("crm_customer", i),
        "contact_last_time": biz_datetime(i * 4, 9, 30),
        "contact_last_content": "DEMO_演示沟通：确认交付计划",
        "contact_next_time": biz_datetime(i * 4 + 10, 9, 30),
        "owner_user_id": None,
        "mobile": "1380030%04d" % (i + 1),
        "telephone": "DEMO-021-020%05d" % (i + 1),
        "email": "contact%05d@%s" % (i + 1, EMAIL_DOMAIN),
        "qq": 200000 + i,
        "wechat": "DEMO_WX_CT%03d" % (i + 1),
        "area_id": None,
        "detail_address": "DEMO_演示地址-联系人%03d" % (i + 1),
        "sex": 1 + i % 2,
        "master": 1 if i % 3 == 0 else 0,
        "post": "DEMO_采购经理" if i % 2 == 0 else "DEMO_技术负责人",
        "parent_id": None,
        "remark": "DEMO_演示 CRM 联系人",
    } for i in range(n_contact)])

    # --- 合同 + 合同产品
    n_ctr = 6 * s
    ctr_ids = b.alloc("crm_contract", n_ctr)
    cp_rows = []
    for i in range(n_ctr):
        for k in range(2):
            unit = money(b.rnd.uniform(800, 15000))
            cnt = money(b.rnd.uniform(2, 40))
            price = money(unit * 0.95)
            cp_rows.append({"id": None, "contract_id": ctr_ids[i],
                            "product_id": b.own_id("crm_product", i * 2 + k + 1),
                            "product_price": unit, "contract_price": price,
                            "count": cnt, "total_price": money(price * cnt)})
    cp_ids = b.alloc("crm_contract_product", len(cp_rows))
    for row, rid in zip(cp_rows, cp_ids):
        row["id"] = rid

    ctr_rows = []
    for i in range(n_ctr):
        mine = [r for r in cp_rows if r["contract_id"] == ctr_ids[i]]
        total_product = money(sum(r["product_price"] * r["count"] for r in mine))
        total = money(sum(r["total_price"] for r in mine))
        ctr_rows.append({
            "id": ctr_ids[i], "name": "DEMO_合同%03d" % (i + 1),
            "no": "DEMO-HT-%05d" % (i + 1),
            "customer_id": b.m_id("crm_customer", i + 2),
            "business_id": b.own_id("crm_business", i),
            "contact_last_time": biz_datetime(i * 8 + 3, 10, 0),
            "owner_user_id": None,
            "process_instance_id": None,
            "audit_status": CRM_AUDIT_STATUS[i % len(CRM_AUDIT_STATUS)],
            "order_date": biz_datetime(i * 8 + 4, 10, 0),
            "start_time": biz_datetime(i * 8 + 6, 0, 0),
            "end_time": biz_datetime(i * 8 + 150, 0, 0),
            "total_product_price": total_product,
            "discount_percent": money(5.0),
            "total_price": total,
            "sign_contact_id": b.own_id("crm_contact", i + 1),
            "sign_user_id": None,
            "file_urls": None,
            "esign_task_id": None,
            "remark": "DEMO_演示 CRM 合同",
        })
    b.emit("crm_contract", ctr_rows)
    b.emit("crm_contract_product", cp_rows)

    # --- 回款计划 + 回款（先分配回款 ID，互相回填）
    n_recv = n_ctr * 2
    recv_ids = b.alloc("crm_receivable", n_recv)
    plan_rows = []
    for i in range(n_ctr):
        ctr = ctr_rows[i]
        for k in range(2):
            recv_idx = i * 2 + k
            price = money(ctr["total_price"] / 2.0)
            plan_rows.append({
                "id": None, "period": k + 1,
                "customer_id": ctr["customer_id"], "contract_id": ctr["id"],
                "owner_user_id": None,
                "return_time": biz_datetime(i * 8 + 30 + k * 45, 10, 0),
                "return_type": CRM_RETURN_TYPE[0],
                "price": price,
                "receivable_id": recv_ids[recv_idx],
                "remind_days": 7, "remind_time": biz_datetime(i * 8 + 23 + k * 45, 9, 0),
                "remark": "DEMO_演示回款计划",
            })
    plan_ids = b.alloc("crm_receivable_plan", len(plan_rows))
    for row, rid in zip(plan_rows, plan_ids):
        row["id"] = rid
    b.emit("crm_receivable_plan", plan_rows)

    recv_rows = []
    for idx, plan in enumerate(plan_rows):
        recv_rows.append({
            "id": recv_ids[idx], "no": "DEMO-RCV-%05d" % (idx + 1),
            "plan_id": plan["id"],
            "customer_id": plan["customer_id"], "contract_id": plan["contract_id"],
            "owner_user_id": None,
            "return_time": plan["return_time"],
            "return_type": plan["return_type"],
            "price": plan["price"],
            "process_instance_id": None,
            "audit_status": CRM_AUDIT_STATUS[idx % len(CRM_AUDIT_STATUS)],
            "idempotency_key": None, "payload_hash": None,
            "remark": "DEMO_演示回款",
        })
    b.emit("crm_receivable", recv_rows)

    # --- 跟进记录
    n_fu = 20 * s
    fu_ids = b.alloc("crm_follow_up_record", n_fu)
    b.emit("crm_follow_up_record", [{
        "id": fu_ids[i],
        "biz_type": CRM_FOLLOW_UP_BIZ_TYPE[i % len(CRM_FOLLOW_UP_BIZ_TYPE)],
        "biz_id": b.own_id("crm_business", i),
        "type": CRM_FOLLOW_UP_TYPE[i % len(CRM_FOLLOW_UP_TYPE)],
        "content": "DEMO_演示跟进记录%d：沟通方案与报价" % (i + 1),
        "next_time": biz_datetime(i * 3 + 5, 14, 0),
        "pic_urls": None, "file_urls": None,
        "business_ids": None, "contact_ids": None,
    } for i in range(n_fu)])

    # --- 发票 + 发票行
    n_inv = 5 * s
    inv_ids = b.alloc("crm_invoice", n_inv)
    inv_rows = []
    for i in range(n_inv):
        awt = money(b.rnd.uniform(3000, 90000))
        tax = money(awt * 0.13)
        inv_rows.append({
            "id": inv_ids[i], "no": "DEMO-INV-%05d" % (i + 1),
            "contract_id": b.own_id("crm_contract", i),
            "customer_id": b.m_id("crm_customer", i + 3),
            "contact_id": b.own_id("crm_contact", i + 2),
            "invoice_type": CRM_INVOICE_TYPE[i % len(CRM_INVOICE_TYPE)],
            "invoice_no": "DEMO-INVNO-%08d" % (i + 1),
            "buyer_name": b.m_name("crm_customer", i + 3, "name"),
            "buyer_tax_no": "DEMO_TAXNO_%012d" % (i + 1),
            "amount_without_tax": awt, "tax_amount": tax,
            "amount_with_tax": money(awt + tax),
            "invoice_date": biz_date(i * 12 + 10),
            "audit_status": CRM_AUDIT_STATUS[i % len(CRM_AUDIT_STATUS)],
            "process_instance_id": None, "owner_user_id": None,
            "file_urls": None, "remark": "DEMO_演示发票",
        })
    b.emit("crm_invoice", inv_rows)

    il_rows = []
    for i in range(n_inv):
        for k in range(2):
            unit = money(b.rnd.uniform(300, 9000))
            cnt = money(b.rnd.uniform(1, 20))
            awt = money(unit * cnt)
            tax = money(awt * 0.13)
            il_rows.append({
                "id": None, "invoice_id": inv_ids[i],
                "product_name": "DEMO_开票产品%03d" % (i * 2 + k + 1),
                "quantity": cnt, "unit_price": unit,
                "amount_without_tax": awt, "tax_rate": money(13.0),
                "tax_amount": tax, "amount_with_tax": money(awt + tax),
                "remark": "DEMO_演示发票行",
            })
    il_ids = b.alloc("crm_invoice_line", len(il_rows))
    for row, rid in zip(il_rows, il_ids):
        row["id"] = rid
    b.emit("crm_invoice_line", il_rows)

# ------------------------------ HR 单据 ------------------------------
def build_hr(b: Builder):
    s = b.s
    emp = b.master.rows("hr_employee")
    emp_ids = [r["id"] for r in emp]
    leave_types = b.own("hr_leave_type")
    lt_ids = [r["id"] for r in leave_types]
    LT_TOTAL = {0: 10.0, 1: 5.0, 2: 10.0, 3: 3.0}   # 年假/事假/病假/调休 年度额度

    # --- 假期余额（员工 × 假期类型）
    bal_rows = []
    for ei, eid in enumerate(emp_ids):
        for li, lid in enumerate(lt_ids):
            total = LT_TOTAL.get(li % len(LT_TOTAL), 5.0)
            used = money(min(total, round((ei * 0.5 + li) % (total + 1), 1)))
            bal_rows.append({"id": None, "employee_id": eid, "leave_type_id": lid,
                             "year": 2026, "total_days": total, "used_days": used,
                             "remaining_days": money(total - used),
                             "remark": "DEMO_演示假期余额"})
    bal_ids = b.alloc("hr_leave_balance", len(bal_rows))
    for row, rid in zip(bal_rows, bal_ids):
        row["id"] = rid
    b.emit("hr_leave_balance", bal_rows)

    # --- 请假申请
    n_lr = 20 * s
    lr_ids = b.alloc("hr_leave_request", n_lr)
    b.emit("hr_leave_request", [{
        "id": lr_ids[i],
        "employee_id": emp_ids[i % len(emp_ids)],
        "leave_type_id": lt_ids[i % len(lt_ids)],
        "start_date": biz_date(i * 6 + 3),
        "end_date": biz_date(i * 6 + 5),
        "days": money(3.0),
        "reason": "DEMO_演示请假事由%d" % (i + 1),
        "status": HR_LEAVE_STATUS[i % len(HR_LEAVE_STATUS)],
        "approver_id": emp_ids[(i + 7) % len(emp_ids)],
        "approve_time": biz_datetime(i * 6 + 2, 17, 0) if i % 4 != 0 else None,
        "approve_remark": "DEMO_演示审批意见",
    } for i in range(n_lr)])

    # --- 考勤（每员工 20 个工作日）
    att_rows = []
    for ei, eid in enumerate(emp_ids):
        for d in range(20):
            st = HR_ATTENDANCE_STATUS[(ei + d) % len(HR_ATTENDANCE_STATUS)]
            day = biz_date(ei % 5 + d * 7)
            att_rows.append({
                "id": None, "employee_id": eid,
                "attendance_date": day,
                "check_in_time": "%s 08:%02d:00" % (day, 55 if st == 20 else 30),
                "check_out_time": "%s 18:%02d:00" % (day, 5 if st == 30 else 30),
                "status": st,
                "overtime_hours": money(2.0) if st == 50 else money(0.0),
                "remark": "DEMO_演示考勤",
            })
    att_ids = b.alloc("hr_attendance", len(att_rows))
    for row, rid in zip(att_rows, att_ids):
        row["id"] = rid
    b.emit("hr_attendance", att_rows)

    # --- 劳动合同（每员工 1 份）
    n_hc = len(emp_ids)
    hc_ids = b.alloc("hr_contract", n_hc)
    b.emit("hr_contract", [{
        "id": hc_ids[i],
        "employee_id": emp_ids[i],
        "contract_no": "DEMO-LABOR-%05d" % (i + 1),
        "contract_type": HR_CONTRACT_TYPE[i % len(HR_CONTRACT_TYPE)],
        "start_date": biz_date(i),
        "end_date": biz_date(i + 1095),
        "sign_date": biz_date(max(i - 5, 0)),
        "probation_end_date": biz_date(i + 90),
        "position_id": b.m_id("hr_position", i),
        "department_id": b.m_id("hr_department", i),
        "salary": money(b.rnd.uniform(6000, 26000)),
        "status": HR_CONTRACT_STATUS[i % len(HR_CONTRACT_STATUS)],
        "file_url": None, "remark": "DEMO_演示劳动合同",
    } for i in range(n_hc)])

    # --- 薪资（每员工 3 个月）
    sal_rows = []
    for ei, eid in enumerate(emp_ids):
        for m in range(3):
            base = money(b.rnd.uniform(6000, 26000))
            ot = money(b.rnd.uniform(0, 3000))
            bonus = money(b.rnd.uniform(0, 6000))
            ded = money(b.rnd.uniform(0, 800))
            si = money(base * 0.105)
            hf = money(base * 0.07)
            taxable = max(base + ot + bonus - ded - si - hf, 0)
            tax = money(taxable * 0.10)
            net = money(base + ot + bonus - ded - si - hf - tax)
            sal_rows.append({
                "id": None, "employee_id": eid, "salary_month": biz_month(m),
                "base_salary": base, "overtime_pay": ot, "bonus": bonus,
                "deduction": ded, "social_insurance": si, "housing_fund": hf,
                "tax": tax, "net_salary": net,
                "status": HR_SALARY_STATUS[(ei + m) % len(HR_SALARY_STATUS)],
                "remark": "DEMO_演示薪资",
            })
    sal_ids = b.alloc("hr_salary", len(sal_rows))
    for row, rid in zip(sal_rows, sal_ids):
        row["id"] = rid
    b.emit("hr_salary", sal_rows)

    # --- 绩效（每员工 2 个周期）
    perf_rows = []
    for ei, eid in enumerate(emp_ids):
        for p in range(2):
            score = money(b.rnd.uniform(60, 99))
            perf_rows.append({
                "id": None, "employee_id": eid,
                "period": "2026-H%d" % (p + 1),
                "score": score,
                "grade": HR_PERF_GRADE[min(int((100 - score) // 10), len(HR_PERF_GRADE) - 1)],
                "evaluator_id": emp_ids[(ei + 11) % len(emp_ids)],
                "evaluation_date": biz_date(90 + p * 90),
                "comment": "DEMO_演示绩效评语",
                "remark": "DEMO_演示绩效",
            })
    perf_ids = b.alloc("hr_performance", len(perf_rows))
    for row, rid in zip(perf_rows, perf_ids):
        row["id"] = rid
    b.emit("hr_performance", perf_rows)


# ------------------------------ OA 单据 ------------------------------
def build_oa(b: Builder):
    s = b.s
    rooms = b.own("oa_meeting_room")
    room_ids = [r["id"] for r in rooms]

    # --- 公告
    n_an = 6 * s
    an_ids = b.alloc("oa_announcement", n_an)
    b.emit("oa_announcement", [{
        "id": an_ids[i],
        "title": "DEMO_演示公告%03d" % (i + 1),
        "content": "DEMO_演示公告正文%d：制度更新与执行说明" % (i + 1),
        "category": "DEMO_公司制度" if i % 2 == 0 else "DEMO_通知",
        "priority": 1 + i % 3,
        "publisher_user_id": OA_PLACEHOLDER_USER_ID,
        "publisher_name": "DEMO_行政发布人",
        "target_scope": "DEMO_全员",
        "publish_time": biz_datetime(i * 9, 9, 0),
        "expire_time": biz_datetime(i * 9 + 60, 23, 59),
        "top_flag": 1 if i % 4 == 0 else 0,
        "status": OA_ANNOUNCEMENT_STATUS[i % len(OA_ANNOUNCEMENT_STATUS)],
        "view_count": 10 + i * 7,
        "remark": "DEMO_演示公告",
    } for i in range(n_an)])

    # --- 报销单 + 明细
    n_rb = 8 * s
    rb_ids = b.alloc("oa_reimburse", n_rb)
    rb_item_rows = []
    for i in range(n_rb):
        for k in range(2):
            rb_item_rows.append({
                "id": None, "reimburse_id": rb_ids[i],
                "subject": "DEMO_差旅费" if k == 0 else "DEMO_办公用品",
                "occurrence_date": biz_date(i * 7 + k * 3),
                "amount": money(b.rnd.uniform(120, 4800)),
                "invoice_count": 1 + k,
                "description": "DEMO_演示报销明细",
            })
    rb_item_ids = b.alloc("oa_reimburse_item", len(rb_item_rows))
    for row, rid in zip(rb_item_rows, rb_item_ids):
        row["id"] = rid

    rb_rows = []
    for i in range(n_rb):
        mine = [r for r in rb_item_rows if r["reimburse_id"] == rb_ids[i]]
        rb_rows.append({
            "id": rb_ids[i], "no": "DEMO-RB-%05d" % (i + 1),
            "reimburse_name": "DEMO_演示报销单%03d" % (i + 1),
            "applicant_user_id": OA_PLACEHOLDER_USER_ID,
            "dept_id": b.m_id("hr_department", i),
            "reimburse_type": OA_REIMBURSE_TYPE[i % len(OA_REIMBURSE_TYPE)],
            "reimburse_date": biz_date(i * 7 + 5),
            "total_amount": money(sum(r["amount"] for r in mine)),
            "payment_status": OA_REIMBURSE_PAY_STATUS[i % len(OA_REIMBURSE_PAY_STATUS)],
            "process_instance_id": None,
            "status": OA_REIMBURSE_STATUS[i % len(OA_REIMBURSE_STATUS)],
            "remark": "DEMO_演示报销单",
        })
    b.emit("oa_reimburse", rb_rows)
    b.emit("oa_reimburse_item", rb_item_rows)

    # --- 会议预订
    n_mt = 6 * s
    mt_ids = b.alloc("oa_meeting_reservation", n_mt)
    b.emit("oa_meeting_reservation", [{
        "id": mt_ids[i],
        "title": "DEMO_演示会议%03d" % (i + 1),
        "room_id": room_ids[i % len(room_ids)],
        "organizer_user_id": OA_PLACEHOLDER_USER_ID,
        "attendee_user_ids": None,
        "start_time": biz_datetime(i * 5 + 1, 14, 0),
        "end_time": biz_datetime(i * 5 + 1, 15, 30),
        "status": OA_MEETING_STATUS[i % len(OA_MEETING_STATUS)],
        "reminder_enabled": 1 if i % 2 == 0 else 0,
        "reminder_minutes": 15,
        "remark": "DEMO_演示会议预订",
    } for i in range(n_mt)])

    # --- 日程
    n_sc = 10 * s
    sc_ids = b.alloc("oa_schedule", n_sc)
    b.emit("oa_schedule", [{
        "id": sc_ids[i],
        "user_id": OA_PLACEHOLDER_USER_ID,
        "title": "DEMO_演示日程%03d" % (i + 1),
        "description": "DEMO_演示日程说明",
        "type": OA_SCHEDULE_TYPE[i % len(OA_SCHEDULE_TYPE)],
        "start_time": biz_datetime(i * 4, 10, 0),
        "end_time": biz_datetime(i * 4, 11, 0),
        "all_day": 1 if i % 5 == 0 else 0,
        "location": "DEMO_A 座 3F 会议室",
        "remind_minutes": 10,
        "reminded": 0,
        "repeat_type": OA_SCHEDULE_REPEAT[i % len(OA_SCHEDULE_REPEAT)],
        "status": OA_SCHEDULE_STATUS[i % len(OA_SCHEDULE_STATUS)],
        "remark": "DEMO_演示日程",
    } for i in range(n_sc)])


# ------------------------------ TMS 单据 ------------------------------
def build_tms(b: Builder):
    s = b.s
    carrier_ids = [r["id"] for r in b.master.rows("tms_carrier")]
    vehicle_ids = [r["id"] for r in b.master.rows("tms_vehicle")]
    driver_ids = [r["id"] for r in b.master.rows("tms_driver")]

    # --- 运单
    n_sh = 8 * s
    sh_ids = b.alloc("tms_shipment", n_sh)
    sh_rows = []
    for i in range(n_sh):
        qtyv = qty(b.rnd.uniform(50, 900))
        sh_rows.append({
            "id": sh_ids[i], "no": "DEMO-TMS-%05d" % (i + 1),
            "carrier_id": carrier_ids[i % len(carrier_ids)],
            "vehicle_id": vehicle_ids[i % len(vehicle_ids)],
            "driver_id": driver_ids[i % len(driver_ids)],
            "origin_address": "DEMO_演示发货地-华东仓",
            "destination_address": "DEMO_演示收货地-%03d" % (i + 1),
            "shipment_type": TMS_SHIPMENT_TYPE[i % len(TMS_SHIPMENT_TYPE)],
            "source_order_no": b.biz_no("SO", i),
            "total_quantity": qtyv,
            "total_weight": qty(b.rnd.uniform(200, 8000)),
            "total_volume": qty(b.rnd.uniform(2, 60)),
            "freight_amount": money(b.rnd.uniform(800, 26000)),
            "departure_time": biz_datetime(i * 9 + 1, 8, 0),
            "estimated_arrival_time": biz_datetime(i * 9 + 3, 18, 0),
            "actual_arrival_time": biz_datetime(i * 9 + 3, 19, 0) if i % 3 else None,
            "status": TMS_SHIPMENT_STATUS[i % len(TMS_SHIPMENT_STATUS)],
            "remark": "DEMO_演示运单",
        })
    b.emit("tms_shipment", sh_rows)

    # --- 途经站点（每运单 2 个）
    stop_rows = []
    for i, sh in enumerate(sh_rows):
        for k in range(2):
            stop_rows.append({
                "id": None, "shipment_id": sh["id"], "sequence_no": k + 1,
                "address": "DEMO_演示中转站%02d" % (i * 2 + k + 1),
                "arrival_time": biz_datetime(i * 9 + 1 + k, 12, 0),
                "departure_time": biz_datetime(i * 9 + 1 + k, 13, 0),
                "remark": "DEMO_演示站点",
            })
    stop_ids = b.alloc("tms_shipment_stop", len(stop_rows))
    for row, rid in zip(stop_rows, stop_ids):
        row["id"] = rid
    b.emit("tms_shipment_stop", stop_rows)

    # --- 轨迹事件（每运单 3 条）
    ev_rows = []
    for i, sh in enumerate(sh_rows):
        for k in range(3):
            ev_rows.append({
                "id": None, "shipment_id": sh["id"],
                "event_type": TMS_TRACKING_EVENT_TYPE[k % len(TMS_TRACKING_EVENT_TYPE)],
                "event_time": biz_datetime(i * 9 + 1, 9 + k * 3, 30),
                "location": "DEMO_演示轨迹点%02d" % (i * 3 + k + 1),
                "longitude": round(121.47 + i * 0.01 + k * 0.002, 6),
                "latitude": round(31.23 + i * 0.01 + k * 0.002, 6),
                "description": "DEMO_演示轨迹事件",
            })
    ev_ids = b.alloc("tms_tracking_event", len(ev_rows))
    for row, rid in zip(ev_rows, ev_ids):
        row["id"] = rid
    b.emit("tms_tracking_event", ev_rows)

    # --- 运费单（每运单 1 张）
    fr_ids = b.alloc("tms_freight", n_sh)
    fr_rows = []
    for i, sh in enumerate(sh_rows):
        bq = sh["total_quantity"]
        up = money(b.rnd.uniform(3, 28))
        sur = money(b.rnd.uniform(0, 600))
        disc = money(b.rnd.uniform(0, 300))
        total = money(bq * up + sur - disc)
        fr_rows.append({
            "id": fr_ids[i], "no": "DEMO-FRT-%05d" % (i + 1),
            "shipment_id": sh["id"],
            "carrier_id": sh["carrier_id"],
            "billing_method": TMS_BILLING_METHOD[i % len(TMS_BILLING_METHOD)],
            "billing_quantity": bq, "unit_price": up,
            "surcharge": sur, "discount_amount": disc, "total_amount": total,
            "status": TMS_FREIGHT_STATUS[i % len(TMS_FREIGHT_STATUS)],
            "auditor": "DEMO_演示审核人",
            "audit_time": biz_datetime(i * 9 + 4, 10, 0) if i % 2 == 0 else None,
            "settle_time": biz_datetime(i * 9 + 20, 10, 0) if i % 4 == 0 else None,
            "reject_reason": None, "remark": "DEMO_演示运费单",
        })
    b.emit("tms_freight", fr_rows)

    # --- 运费对账单
    n_rc = 3 * s
    rc_ids = b.alloc("tms_freight_reconciliation", n_rc)
    rc_rows = []
    for i in range(n_rc):
        sysamt = money(b.rnd.uniform(20000, 180000))
        caramt = money(sysamt + b.rnd.uniform(-800, 800))
        rc_rows.append({
            "id": rc_ids[i], "no": "DEMO-RECON-%05d" % (i + 1),
            "carrier_id": carrier_ids[i % len(carrier_ids)],
            "period_start": biz_date(i * 60),
            "period_end": biz_date(i * 60 + 59),
            "system_amount": sysamt, "carrier_amount": caramt,
            "diff_amount": money(caramt - sysamt),
            "status": TMS_RECON_STATUS[i % len(TMS_RECON_STATUS)],
            "reconciler_id": None,
            "reconcile_time": biz_datetime(i * 60 + 62, 10, 0),
            "confirmer_id": None,
            "confirm_time": biz_datetime(i * 60 + 64, 10, 0) if i % 2 == 0 else None,
            "remark": "DEMO_演示运费对账单",
        })
    b.emit("tms_freight_reconciliation", rc_rows)

# ------------------------------ 补充常量（CRM/TMS/OA） ------------------------------
# CRM 跟进记录：biz_type / type 未定位到 Java 枚举定义，按 yudao CRM 常用取值域填充并标注未核实
CRM_FOLLOW_UP_BIZ_TYPE = (1, 2, 3, 4)
CRM_FOLLOW_UP_TYPE = (10, 20, 30)
CRM_INVOICE_TYPE = (10, 20)                        # 10 增值税专用发票 20 增值税普通发票
TMS_BILLING_METHOD = (10, 20, 30)                  # 10 按重量 20 按体积 30 按件

# OA 部分 NOT NULL 的 user_id 列指向 system 用户；demo 数据不含真实用户 ID，
# 这里统一填占位值并在 UNVERIFIED_FIELDS_EXTRA 中标注，不留 NULL（NOT NULL 约束）。
OA_PLACEHOLDER_USER_ID = 1

UNVERIFIED_FIELDS_EXTRA = [
    "crm_follow_up_record.biz_type / type（未定位到 Java 枚举定义，按 (1,2,3,4)/(10,20,30) 取值域填充）",
    "crm_invoice.invoice_type / crm_invoice_line.tax_rate（按 10/20 与 13% 常见取值填充，未与税务配置核对）",
    "crm_invoice.buyer_tax_no（DEMO_TAXNO_ 前缀占位，非真实纳税人识别号）",
    "tms_freight.billing_method（按 (10,20,30) 取值域填充，未定位枚举定义）",
    "oa_schedule.user_id / oa_reimburse.applicant_user_id / oa_meeting_reservation.organizer_user_id / "
    "oa_announcement.publisher_user_id（NOT NULL 但指向 system 用户，demo 数据无真实用户，"
    "统一填占位 ID=1，未校验 system_users 中是否存在该行）",
    "hr_attendance.check_in_time / check_out_time（按状态推导的演示时刻，未与考勤规则引擎核对）",
    "hr_performance.period（使用 2026-H1/H2 半年度格式，未与绩效周期配置核对）",
]


# ------------------------------ 组装 ------------------------------
def build_dataset(snapshot: dict, seed: int, tenant: int, scale: str, master: MasterData) -> list:
    if scale not in SCALE_FACTOR:
        raise FailClosedError("不支持的数据规模：%s（可选：%s）" % (scale, ", ".join(sorted(SCALE_FACTOR))))
    if master.tenant is not None and master.tenant != tenant:
        raise FailClosedError(
            "主数据租户 %s 与本次 tenant %d 不一致，单据外键会指向错误租户"
            % (master.tenant, tenant))
    b = Builder(snapshot, seed, tenant, scale, master)
    build_supporting(b)
    build_erp(b)
    build_mes(b)
    build_wms(b)
    build_qms(b)
    build_crm(b)
    build_hr(b)
    build_oa(b)
    build_tms(b)
    order = {t: i for i, t in enumerate(TABLE_ORDER)}
    b.dataset.sort(key=lambda d: order.get(d["table"], 999))
    return b.dataset


# ------------------------------ 渲染 ------------------------------
def render_sql(dataset: list, snapshot: dict, seed: int, tenant: int, scale: str,
               snapshot_path: Path, master_path: Path) -> str:
    lines = [
        "-- ============================================================",
        "-- 演示/开发业务单据：demo-data/sql/mysql/demo_business_documents.sql",
        "-- 由 demo-data/generators/generate_business_documents.py 自动生成",
        "-- 参数：seed=%d tenant=%d scale=%s" % (seed, tenant, scale),
        "-- 依赖：必须先导入 demo_master_data.sql（单据外键指向主数据行）",
        "-- 【禁止导入生产】全部数据位于 tenant_id=%d，编号带 DEMO_ 前缀，" % tenant,
        "--   手机号使用占位区间，邮箱使用 RFC 2606 保留域 .invalid。",
        "--   生产导入会污染真实台账；清理脚本见 demo-data/cleanup/cleanup_demo.sql。",
        "-- 【未实库执行】本文件仅通过静态校验：列取自 schema 快照、NOT NULL 全覆盖；",
        "--   尚未在任何 MySQL 实例上执行，可执行性未经运行时验证。",
        "-- 快照来源：%s" % snapshot_path.name,
        "-- 主数据来源：%s" % master_path.name,
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
                snapshot_path: Path, master_path: Path) -> str:
    payload = {
        "generated_by": "demo-data/generators/generate_business_documents.py",
        "seed": seed,
        "tenant_id": tenant,
        "scale": scale,
        "snapshot": str(snapshot_path.relative_to(REPO_ROOT)).replace("\\", "/"),
        "snapshot_payload_sha256": snapshot.get("payload_sha256"),
        "depends_on_master_data": str(master_path.relative_to(REPO_ROOT)).replace("\\", "/"),
        "boundary": "dev/test/demo only —— 禁止导入生产",
        "unverified_fields": UNVERIFIED_FIELDS + UNVERIFIED_FIELDS_EXTRA,
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
    parser = argparse.ArgumentParser(
        description="企业数字化中台演示业务单据生成器（静态产物，不连库）")
    parser.add_argument("--snapshot", default=str(DEFAULT_SNAPSHOT), help="schema 快照 JSON 路径")
    parser.add_argument("--master-json", default=str(DEFAULT_MASTER_JSON),
                        help="generate_master_data.py 产出的 JSON（单据外键来源）")
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
    master_path = Path(args.master_json)
    snapshot = load_snapshot(snapshot_path)
    master = MasterData(master_path)
    if master.tenant is not None and master.tenant != args.tenant:
        raise FailClosedError(
            "主数据租户 %s 与本次 --tenant %d 不一致，单据外键会指向错误租户"
            % (master.tenant, args.tenant))

    dataset = build_dataset(snapshot, args.seed, args.tenant, args.scale, master)

    sql_text = render_sql(dataset, snapshot, args.seed, args.tenant, args.scale,
                          snapshot_path, master_path)
    json_text = render_json(dataset, snapshot, args.seed, args.tenant, args.scale,
                            snapshot_path, master_path)

    sql_path = Path(args.output)
    json_path = Path(args.json_out)
    sql_file_sha = write_bytes(sql_path, sql_text)
    json_file_sha = write_bytes(json_path, json_text)

    manifest = {
        "generated_by": "demo-data/generators/generate_business_documents.py",
        "seed": args.seed,
        "tenant_id": args.tenant,
        "scale": args.scale,
        "dialect": args.dialect,
        "snapshot": rel(snapshot_path),
        "snapshot_payload_sha256": snapshot.get("payload_sha256"),
        "depends_on": {
            "master_data_json": rel(master_path),
            "master_data_file_sha256": sha256_file(master_path),
            "master_data_tenant_id": master.tenant,
            "master_data_seed": master.seed,
        },
        "table_count": len(dataset),
        "row_total": sum(len(d["rows"]) for d in dataset),
        "tables": [{"table": d["table"], "rows": len(d["rows"])} for d in dataset],
        "unverified_fields": UNVERIFIED_FIELDS + UNVERIFIED_FIELDS_EXTRA,
        "artifacts": {
            "sql": {"path": rel(sql_path), "payload_sha256": sha256_text(sql_text),
                    "file_sha256": sql_file_sha, "bytes": len(sql_text.encode("utf-8"))},
            "json": {"path": rel(json_path), "payload_sha256": sha256_text(json_text),
                     "file_sha256": json_file_sha, "bytes": len(json_text.encode("utf-8"))},
        },
        "status": "STATIC_ONLY —— 未在任何数据库执行过，可执行性未验证",
    }
    summary_path = Path(args.summary)
    summary_file_sha = write_bytes(summary_path,
                                   json.dumps(manifest, ensure_ascii=False, indent=2) + "\n")

    print("tenant=%d scale=%s tables=%d rows=%d"
          % (args.tenant, args.scale, manifest["table_count"], manifest["row_total"]))
    for t in manifest["tables"]:
        print("  %-32s %d" % (t["table"], t["rows"]))
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