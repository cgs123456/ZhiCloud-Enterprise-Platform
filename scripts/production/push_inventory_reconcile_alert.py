#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""只读库存对账结果的外部告警推送器。

设计边界：
  * 不在 Java 应用内新增 Quartz/消息队列；按 owner 口径，告警是运维设施。
  * 本脚本由外部调度器调用（cron / Windows Task Scheduler / K8s CronJob 均可）。
  * 只调用对账只读端点；对账服务本身不写任何业务表。
  * 仅当报告 consistent=false 时推送，避免定时噪音。
  * Webhook URL 与凭据从环境变量读取，避免进入 shell history/argv。

用法：
  ZC_RECONCILE_TOKEN=xxx ZC_RECONCILE_WEBHOOK_URL=https://... \\
      python scripts/production/push_inventory_reconcile_alert.py \\
      --base-url http://127.0.0.1:48080/admin-api --tenant-id 1
"""

from __future__ import annotations

import argparse
import json
import os
import sys
import urllib.error
import urllib.request
from pathlib import Path
from typing import Any


def http_json(url: str, *, method: str = "GET", headers: dict[str, str] | None = None,
              payload: dict[str, Any] | None = None, timeout: float = 10.0) -> tuple[int, dict[str, Any]]:
    data = None
    request_headers = dict(headers or {})
    if payload is not None:
        data = json.dumps(payload, ensure_ascii=False).encode("utf-8")
        request_headers.setdefault("Content-Type", "application/json; charset=utf-8")
    request = urllib.request.Request(url, data=data, headers=request_headers, method=method)
    try:
        with urllib.request.urlopen(request, timeout=timeout) as response:
            body = response.read().decode("utf-8")
            return response.status, json.loads(body)
    except urllib.error.HTTPError as exc:
        body = exc.read().decode("utf-8", errors="replace")
        raise RuntimeError(f"HTTP {exc.code}: {body[:500]}") from exc
    except urllib.error.URLError as exc:
        raise RuntimeError(f"request failed: {exc.reason}") from exc


def summarize_mismatches(report: dict[str, Any]) -> str:
    mismatches = report.get("mismatches") or []
    lines = [
        f"库存对账不一致：source={report.get('sourceCount')} projection={report.get('projectionCount')} "
        f"mismatches={len(mismatches)}"
    ]
    for item in mismatches[:10]:
        lines.append(
            f"- source={item.get('source')} item={item.get('itemId')} wh={item.get('warehouseId')} "
            f"truth={item.get('truthQuantity')} projection={item.get('projectionQuantity')}"
        )
    if len(mismatches) > 10:
        lines.append(f"- ...另有 {len(mismatches) - 10} 条")
    return "\n".join(lines)


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description="Push inventory reconcile mismatches to a webhook")
    parser.add_argument("--base-url", default="http://127.0.0.1:48080/admin-api")
    parser.add_argument("--tenant-id", type=int, default=1)
    parser.add_argument("--token-env", default="ZC_RECONCILE_TOKEN")
    parser.add_argument("--webhook-env", default="ZC_RECONCILE_WEBHOOK_URL")
    parser.add_argument("--timeout", type=float, default=10.0)
    parser.add_argument("--dry-run", action="store_true", help="fetch report, but do not POST webhook")
    args = parser.parse_args(argv)

    token = os.environ.get(args.token_env)
    if not token:
        raise SystemExit(f"missing token: set {args.token_env}")
    webhook = os.environ.get(args.webhook_env)
    if not webhook and not args.dry_run:
        raise SystemExit(f"missing webhook: set {args.webhook_env} or use --dry-run")

    endpoint = args.base_url.rstrip("/") + "/erp/inventory-reconcile/run"
    status, body = http_json(endpoint, headers={
        "Authorization": f"Bearer {token}",
        "tenant-id": str(args.tenant_id),
    }, timeout=args.timeout)
    if status != 200 or body.get("code") != 0:
        raise RuntimeError(f"reconcile endpoint failed: HTTP={status} body={body}")

    report = body.get("data") or {}
    consistent = bool(report.get("consistent", True))
    message = summarize_mismatches(report)

    if consistent:
        print("inventory reconcile consistent; no alert")
        return 0

    print(message)
    if args.dry_run:
        print("[dry-run] webhook not called")
        return 0

    payload = {
        "msg_type": "text",
        "content": {"text": message},
        "source": "inventory-reconcile",
        "consistent": False,
        "report": report,
    }
    post_status, post_body = http_json(webhook, method="POST", payload=payload, timeout=args.timeout)
    if post_status < 200 or post_status >= 300:
        raise RuntimeError(f"webhook failed: HTTP={post_status} body={post_body}")
    print(f"alert pushed; webhook HTTP={post_status}")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except Exception as exc:
        print(f"FAIL: {exc}", file=sys.stderr)
        raise SystemExit(2)