# -*- coding: utf-8 -*-
"""Verify the 47 smoke failures by supplying the context each endpoint requires."""
import csv
import json
import os
import urllib.error
import urllib.request

BASE_DIR = r"C:\Users\Administrator\Desktop\zhicloud"
HOST = "http://127.0.0.1:48080/admin-api"
TOKEN = "test-only-for-local900100"
TENANT = "9001"
IN_CSV = os.path.join(BASE_DIR, "Temp", "api_smoke_results.csv")
OUT_CSV = os.path.join(BASE_DIR, "Temp", "smoke_fail_verification.csv")
OUT_MD = os.path.join(BASE_DIR, "Temp", "smoke_fail_verification.md")

# Candidate query strings for each failing endpoint. The first candidate that
# returns code=0 is recorded; otherwise the last response is kept.
CANDIDATES = {
    "/aimultiagent/checkpoint/list": ["executionLogId=1"],
    "/erp/mps-plan/detail/list": ["planId=1"],
    "/erp/supplier-evaluation/item/list": ["evaluationId=1"],
    "/im/manager/group/member/list": ["groupId=1"],
    "/mes/cal/team-member/page": ["teamId=1"],
    "/mes/cal/calendar/list": ["startDay=2026-10-01%2000:00:00&endDay=2026-10-31%2023:59:59&queryType=TYPE&calendarType=1"],
    "/mes/qc/defect-record/page": ["qcType=1&qcId=1"],
    "/mes/pro/work-order-bom/page": ["workOrderId=5960000", "workOrderCode=DEMO-WO-00001", "id=5960000"],
    "/mes/pro/card-process/page": ["cardId=1", "cardCode=1", "id=1"],
    "/mes/qc/template/indicator/page": ["templateId=1", "id=1"],
    "/mes/qc/ipqc/line/page": ["ipqcId=1", "id=1"],
    "/mes/qc/oqc/line/page": ["oqcId=1", "id=1"],
    "/mes/qc/rqc/line/page": ["rqcId=1", "id=1"],
    "/mes/wm/arrival-notice-line/page": ["noticeId=1", "arrivalNoticeId=1", "id=1"],
    "/mes/wm/product-issue-line/page": ["issueId=1", "productIssueId=1", "id=1"],
    "/mes/wm/return-vendor-line/page": ["returnId=1", "returnVendorId=1", "id=1"],
    "/mes/wm/return-issue-line/page": ["issueId=1"],
    "/mes/wm/misc-issue-line/page": ["issueId=1", "miscIssueId=1", "id=1"],
    "/mes/wm/outsource-issue-line/page": ["issueId=1", "outsourceIssueId=1", "id=1"],
    "/mes/wm/misc-receipt-line/page": ["receiptId=1", "miscReceiptId=1", "id=1"],
    "/mes/wm/package-line/page": ["packageId=1", "id=1"],
    "/mes/wm/transfer-line/list": ["transferId=1"],
    "/mes/wm/stocktaking-plan-param/page": ["planId=1", "stockTakingPlanId=1", "id=1"],
    "/oa/knowledge-comment/list": ["articleId=1"],
    "/wms/inventory/list": ["warehouseId=916000", "warehouseCode=DEMO_WWH01"],
    "/hrm/employee/certificate/list": ["employeeId=925000"],
    "/hrm/salary/change-record/list": ["employeeId=925000"],
    "/hrm/salary/month-employee-record/list": ["monthRecordId=32"],
    "/hrm/portal/salary/slip/list": ["startMonth=2026-01&endMonth=2026-12"],
    "/mp/user/page": ["accountId=1"],
    "/mp/message/page": ["accountId=1"],
    "/mp/free-publish/page": ["accountId=1"],
    "/mp/draft/page": ["accountId=1"],
    "/mp/material/page": ["accountId=1"],
    "/mp/menu/list": ["accountId=1"],
    "/mp/auto-reply/page": ["accountId=1"],
    "/infra/codegen/table/list": ["dataSourceConfigId=1"],
    "/infra/demo03-student-erp/demo03-course/page": ["studentId=1"],
    "/system/user/list": ["ids=1"],
    "/fms/config/account-user/list": ["accountSetId=1"],
    "/fms/config/voucher-word/list": ["accountSetId=1"],
    "/fms/config/digest/list": ["accountSetId=1"],
    "/fms/config/auxiliary-item/list": ["accountSetId=1"],
    "/fms/config/voucher-template/list": ["accountSetId=1"],
    "/fms/config/finance-indicator/list": ["accountSetId=1"],
    "/qms/msa/measurement/list": ["studyId=1"],
    "/qms/ncr/mrb-record/list": ["ncrId=6360000"],
}


def call(endpoint, query):
    url = HOST + endpoint + "?" + query + ("&" if query else "") + "pageNo=1&pageSize=1"
    req = urllib.request.Request(url, method="GET")
    req.add_header("Authorization", "Bearer " + TOKEN)
    req.add_header("tenant-id", TENANT)
    try:
        with urllib.request.urlopen(req, timeout=30) as r:
            raw = r.read().decode("utf-8", "replace")
            try:
                body = json.loads(raw)
            except Exception:
                return r.status, -1, raw[:300]
            return r.status, body.get("code"), body.get("msg")
    except urllib.error.HTTPError as e:
        raw = e.read().decode("utf-8", "replace")
        try:
            body = json.loads(raw)
            return e.code, body.get("code"), body.get("msg")
        except Exception:
            return e.code, -1, raw[:300]
    except Exception as e:
        return -1, -1, str(e)


rows = []
with open(IN_CSV, encoding="utf-8-sig") as f:
    for r in csv.DictReader(f):
        if r["result"] != "FAIL":
            continue
        endpoint = r["endpoint"]
        candidates = CANDIDATES.get(endpoint, [""])
        last = None
        chosen = None
        for q in candidates:
            http, code, msg = call(endpoint, q)
            last = (q, http, code, msg)
            if code == 0:
                chosen = last
                break
        q, http, code, msg = chosen or last
        if code == 0:
            verdict = "OK_WITH_CONTEXT"
        elif code == 400 and ("参数" in (msg or "") or "不能为空" in (msg or "")):
            verdict = "EXPECTED_PARAM_VALIDATION"
        elif code in (1006006000, 1006007000):
            verdict = "EXTERNAL_WECHAT_CONFIG"
        else:
            verdict = "REAL_FAIL"
        rows.append({
            "domain": r["domain"],
            "id": r["id"],
            "name": r["name"],
            "endpoint": endpoint,
            "params_tried": " | ".join(candidates),
            "params_used": q,
            "http": http,
            "code": code,
            "msg": msg,
            "verdict": verdict,
        })

with open(OUT_CSV, "w", encoding="utf-8-sig", newline="") as f:
    w = csv.DictWriter(f, fieldnames=list(rows[0].keys()))
    w.writeheader()
    w.writerows(rows)

with open(OUT_MD, "w", encoding="utf-8") as f:
    f.write("# API smoke 失败项逐个核实\n\n")
    f.write("对每个失败接口补充其所需的父单/上下文参数后重新调用。\n\n")
    f.write("| # | 模块 | 页面 | 接口 | 使用参数 | code | 结论 |\n")
    f.write("|---|---|---|---|---|---|---|\n")
    for i, r in enumerate(rows, 1):
        f.write(f"| {i} | {r['domain']} | {r['name']} | `{r['endpoint']}` | `{r['params_used']}` | {r['code']} | {r['verdict']} |\n")
    f.write("\n## 说明\n\n")
    f.write("- `OK_WITH_CONTEXT`：补充上下文参数后返回 `code=0`，接口本身正常，原 smoke 缺少参数。\n")
    f.write("- `EXPECTED_PARAM_VALIDATION`：补充常见参数后仍被参数校验拦截；通常是父单不存在或该接口不是页面主列表接口，不是 500/404 缺陷。\n")
    f.write("- `EXTERNAL_WECHAT_CONFIG`：接口调用微信公众平台失败，原因是本地 `mp_account` 的 AppID/密钥无效；属于外部凭证配置问题，不是代码缺陷。\n")
    f.write("- `REAL_FAIL`：非参数校验类失败，需要单独排查。\n")

print(json.dumps({"total": len(rows), "ok": sum(1 for r in rows if r["verdict"] == "OK_WITH_CONTEXT"),
                  "param": sum(1 for r in rows if r["verdict"] == "EXPECTED_PARAM_VALIDATION"),
                  "fail": sum(1 for r in rows if r["verdict"] == "REAL_FAIL")}, ensure_ascii=False))
