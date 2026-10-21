# -*- coding: utf-8 -*-
"""Smoke every menu page's primary list endpoint with the local mock admin token."""
import csv
import json
import os
import urllib.request
import urllib.error
from concurrent.futures import ThreadPoolExecutor

BASE = r"C:\Users\Administrator\Desktop\zhicloud"
IN_CSV = os.path.join(BASE, "Temp", "menu_coverage.csv")
OUT_CSV = os.path.join(BASE, "Temp", "api_smoke_results.csv")
OUT_TXT = os.path.join(BASE, "Temp", "api_smoke_summary.txt")
HOST = "http://127.0.0.1:48080/admin-api"
TOKEN = "test-only-for-local900100"
TENANT = "9001"

# Context required by child/detail list endpoints. The menu coverage CSV points
# at the child endpoint for these pages; without the parent id the backend
# correctly rejects the request with a 400 parameter-validation error.
CONTEXT = {
    "/aimultiagent/checkpoint/list": "executionLogId=1",
    "/erp/mps-plan/detail/list": "planId=1",
    "/erp/supplier-evaluation/item/list": "evaluationId=1",
    "/im/manager/group/member/list": "groupId=1",
    "/mes/cal/team-member/page": "teamId=1",
    "/mes/cal/calendar/list": "startDay=2026-10-01%2000:00:00&endDay=2026-10-31%2023:59:59&queryType=TYPE&calendarType=1",
    "/mes/qc/defect-record/page": "qcType=1&qcId=1",
    "/mes/pro/work-order-bom/page": "workOrderId=5960000",
    "/mes/pro/card-process/page": "cardId=1",
    "/mes/qc/template/indicator/page": "templateId=1",
    "/mes/qc/ipqc/line/page": "ipqcId=1",
    "/mes/qc/oqc/line/page": "oqcId=1",
    "/mes/qc/rqc/line/page": "rqcId=1",
    "/mes/wm/arrival-notice-line/page": "noticeId=1",
    "/mes/wm/product-issue-line/page": "issueId=1",
    "/mes/wm/return-vendor-line/page": "returnId=1",
    "/mes/wm/return-issue-line/page": "issueId=1",
    "/mes/wm/misc-issue-line/page": "issueId=1",
    "/mes/wm/outsource-issue-line/page": "issueId=1",
    "/mes/wm/misc-receipt-line/page": "receiptId=1",
    "/mes/wm/package-line/page": "packageId=1",
    "/mes/wm/transfer-line/list": "transferId=1",
    "/mes/wm/stocktaking-plan-param/page": "planId=1",
    "/oa/knowledge-comment/list": "articleId=1",
    "/wms/inventory/list": "warehouseId=916000",
    "/hrm/employee/certificate/list": "employeeId=925000",
    "/hrm/salary/change-record/list": "employeeId=925000",
    "/hrm/salary/month-employee-record/list": "monthRecordId=32",
    "/hrm/portal/salary/slip/list": "startMonth=2026-01&endMonth=2026-12",
    "/mp/user/page": "accountId=1",
    "/mp/message/page": "accountId=1",
    "/mp/free-publish/page": "accountId=1",
    "/mp/draft/page": "accountId=1",
    "/mp/material/page": "accountId=1",
    "/mp/menu/list": "accountId=1",
    "/mp/auto-reply/page": "accountId=1",
    "/infra/codegen/table/list": "dataSourceConfigId=1",
    "/infra/demo03-student-erp/demo03-course/page": "studentId=1",
    "/system/user/list": "ids=1",
    "/fms/config/account-user/list": "accountSetId=1",
    "/fms/config/voucher-word/list": "accountSetId=1",
    "/fms/config/digest/list": "accountSetId=1",
    "/fms/config/auxiliary-item/list": "accountSetId=1",
    "/fms/config/voucher-template/list": "accountSetId=1",
    "/fms/config/finance-indicator/list": "accountSetId=1",
    "/qms/msa/measurement/list": "studyId=1",
    "/qms/ncr/mrb-record/list": "ncrId=6360000",
}
EXTERNAL_CODES = {1006006000, 1006007000}

def call(path):
    base_path = path.split("?", 1)[0]
    ctx = CONTEXT.get(base_path, "")
    sep = "&" if "?" in path else "?"
    url = HOST + path + sep
    if ctx:
        url += ctx + "&"
    url += "pageNo=1&pageSize=1"
    req = urllib.request.Request(url, method="GET")
    req.add_header("Authorization", "Bearer " + TOKEN)
    req.add_header("tenant-id", TENANT)
    try:
        with urllib.request.urlopen(req, timeout=30) as r:
            raw = r.read().decode("utf-8", "replace")
            try:
                body = json.loads(raw)
            except Exception:
                return r.status, -1, raw[:200], 0
            data = body.get("data") or {}
            total = data.get("total") if isinstance(data, dict) else None
            return r.status, body.get("code", -1), body.get("msg", ""), total
    except urllib.error.HTTPError as e:
        raw = e.read().decode("utf-8", "replace")
        try:
            body = json.loads(raw)
            return e.code, body.get("code", -1), body.get("msg", ""), 0
        except Exception:
            return e.code, -1, raw[:200], 0
    except Exception as e:
        return -1, -1, str(e), 0

rows = []
with open(IN_CSV, encoding="utf-8-sig", newline="") as f:
    for r in csv.DictReader(f):
        primary = (r["primary_list"] or "").split(";")[0]
        if not primary or "{" in primary or ":" in primary:
            continue
        rows.append((r, primary))

def work(item):
    r, path = item
    status, code, msg, total = call(path)
    return {
        "domain": r["domain"], "id": r["id"], "name": r["name"], "path": r["path"],
        "endpoint": path, "http": status, "code": code, "msg": (msg or "").replace("\n", " ")[:180],
        "total": "" if total is None else total,
        "result": "PASS" if status == 200 and code == 0 else (
            "EXTERNAL" if code in EXTERNAL_CODES else "FAIL"),
    }

results = []
with ThreadPoolExecutor(max_workers=6) as ex:
    for res in ex.map(work, rows):
        results.append(res)

results.sort(key=lambda x: (x["result"], x["domain"], x["id"]))
with open(OUT_CSV, "w", encoding="utf-8-sig", newline="") as f:
    w = csv.DictWriter(f, fieldnames=list(results[0].keys()))
    w.writeheader()
    w.writerows(results)

passed = sum(1 for x in results if x["result"] == "PASS")
failed = sum(1 for x in results if x["result"] == "FAIL")
external = sum(1 for x in results if x["result"] == "EXTERNAL")
lines = [f"pages_with_primary_list={len(rows)} pass={passed} fail={failed} external={external}", ""]
lines.append("== FAIL ==")
for x in results:
    if x["result"] == "FAIL":
        lines.append(f'{x["domain"]}\t{x["id"]}\t{x["path"]}\t{x["endpoint"]}\thttp={x["http"]}\tcode={x["code"]}\t{x["msg"]}')
lines.append("")
lines.append("== EXTERNAL ==")
for x in results:
    if x["result"] == "EXTERNAL":
        lines.append(f'{x["domain"]}\t{x["id"]}\t{x["path"]}\t{x["endpoint"]}\thttp={x["http"]}\tcode={x["code"]}\t{x["msg"]}')
lines.append("")
lines.append("== PASS by domain ==")
from collections import Counter
c = Counter(x["domain"] for x in results if x["result"] == "PASS")
for k, v in c.most_common():
    lines.append(f"{k}\t{v}")
with open(OUT_TXT, "w", encoding="utf-8") as f:
    f.write("\n".join(lines))
print("\n".join(lines[:60]))
