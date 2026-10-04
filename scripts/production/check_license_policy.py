#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Maven 组件许可证 CI 门禁（T-107）。

默认 fail-closed：除 internal（cn.zhicloud，必须带 MIT 元数据）与
permissive_standard 之外，任何组件必须在 policy allowlist 中存在未过期、
decision 属于审批通过的条目，否则门禁失败（exit=1）。

本脚本只做工程判断，不构成法律意见；审批必须由法务或授权人给出，
未审批就是未审批，不允许为了把门禁变绿而预置 allowlist。
"""
from __future__ import annotations

import argparse
import hashlib
import json
import sys
from datetime import date, datetime
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
DEFAULT_POLICY = Path(__file__).resolve().parent / "license-policy.json"

INTERNAL_GROUP_PREFIX = "cn.zhicloud"
ALLOW = "allow"
DENY_BUCKET = {"deny", "deny_until_approved"}
REVIEW_BUCKET = {"review", "review_and_record"}
MISSING_CATEGORIES = {"", "missing", "unmapped", "unknown", "none"}
DEFAULT_MAX_VALIDITY_DAYS = 365

FLAGS = {
    "production_ready": False,
    "gate3_passed": False,
    "t107_status": "OPEN",
    "independent_review": False,
}
LIMITS = [
    "门禁只判断策略命中，不做法律定性；放行不等于合规。",
    "审批未发生时 allowlist 必须为空，门禁必然 fail-closed。",
    "依赖升级后必须重扫 inventory 并重新审批，不得沿用过期 allowlist。",
]


class InputError(Exception):
    """输入或参数错误，对应退出码 2。"""


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def load_json(path: Path, label: str) -> dict:
    if not path.is_file():
        raise InputError(f"{label} 不存在: {path}")
    try:
        data = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, UnicodeDecodeError, json.JSONDecodeError) as exc:
        raise InputError(f"{label} 不是合法 JSON: {path}: {exc}") from exc
    if not isinstance(data, dict):
        raise InputError(f"{label} 顶层必须是对象: {path}")
    return data


def normalise_purl(purl: object) -> str:
    return str(purl or "").split("?", 1)[0].strip().lower()


def parse_date(value: object, field: str, where: str) -> date | None:
    if value in (None, ""):
        return None
    try:
        return datetime.strptime(str(value).strip(), "%Y-%m-%d").date()
    except ValueError as exc:
        raise InputError(f"allowlist 条目 {where} 的 {field} 必须是 YYYY-MM-DD: {exc}") from exc


def entry_matches(entry: dict, component: dict) -> bool:
    purl = str(entry.get("purl") or "").strip()
    if purl:
        return normalise_purl(purl) == normalise_purl(component.get("purl"))
    group = str(entry.get("group") or "").strip()
    artifact = str(entry.get("artifact") or "").strip()
    if not group or not artifact:
        return False
    if group.lower() != str(component.get("group") or "").lower():
        return False
    if artifact.lower() != str(component.get("artifact") or "").lower():
        return False
    version = str(entry.get("version") or "").strip()
    if version in ("", "*"):
        return True
    return version == str(component.get("version") or "")


def evaluate_allowlist_entry(entry: dict, component: dict, today: date, max_validity_days: int) -> dict:
    """返回 {"status": "approved"|"blocked"|"not_applicable", "reason":..., "rule":...}。"""
    decision = str(entry.get("decision") or "").strip().lower()
    scope = str(entry.get("scope") or "all").strip().lower()
    is_runtime = bool(component.get("server_runtime_scope"))
    where = str(entry.get("purl") or component.get("purl") or "")

    if scope == "server_runtime" and not is_runtime:
        return {"status": "not_applicable", "reason": "allowlist 条目 scope=server_runtime，不适用于非 runtime 组件", "rule": None}

    if decision == "isolated":
        if is_runtime:
            return {
                "status": "blocked",
                "reason": "allowlist decision=isolated，但组件仍在 zhicloud-server runtime scope",
                "rule": "LIC-EXT-007",
            }
    elif decision not in {"approved", "approved_with_exception"}:
        return {
            "status": "blocked",
            "reason": f"allowlist decision={decision or 'EMPTY'} 不是审批通过状态（isolated 仅允许非 runtime）",
            "rule": "LIC-EXT-003",
        }

    expires_on = parse_date(entry.get("expires_on"), "expires_on", where)
    if expires_on is not None and today > expires_on:
        return {"status": "blocked", "reason": f"allowlist 条目已于 {expires_on} 过期", "rule": "LIC-EXT-004"}

    approved_on = parse_date(entry.get("approved_on"), "approved_on", where)
    if approved_on is None:
        return {"status": "blocked", "reason": "allowlist 条目缺少 approved_on", "rule": "LIC-EXT-005"}
    age = (today - approved_on).days
    if max_validity_days > 0 and age > max_validity_days:
        return {
            "status": "blocked",
            "reason": f"allowlist 条目审批于 {approved_on}（{age} 天前），超过 {max_validity_days} 天有效期",
            "rule": "LIC-EXT-005",
        }
    return {"status": "approved", "reason": decision, "rule": None}


def resolve_allowlist(entries: list, component: dict, today: date, max_validity_days: int) -> dict | None:
    first_blocked: dict | None = None
    for entry in entries:
        if not isinstance(entry, dict) or not entry_matches(entry, component):
            continue
        verdict = evaluate_allowlist_entry(entry, component, today, max_validity_days)
        if verdict["status"] == "approved":
            return verdict
        if verdict["status"] == "blocked" and first_blocked is None:
            first_blocked = verdict
    return first_blocked


def _component_record(component: dict, action: str, rule: str, reason: str) -> dict:
    return {
        "purl": component.get("purl"),
        "group": component.get("group"),
        "artifact": component.get("artifact"),
        "version": component.get("version"),
        "license_category": component.get("license_category"),
        "raw_license_label": component.get("raw_license_label"),
        "policy_action": action,
        "server_runtime_scope": bool(component.get("server_runtime_scope")),
        "rule": rule,
        "reason": reason,
    }


def build_report(inventory_path: Path, policy_path: Path, today: date | None = None) -> dict:
    today = today or date.today()
    inventory = load_json(inventory_path, "license inventory")
    policy = load_json(policy_path, "license policy")

    if inventory.get("schema_version") != 1:
        raise InputError(f"inventory schema_version 必须为 1: {inventory_path}")
    if inventory.get("evidence_type") != "maven_license_inventory":
        raise InputError(f"inventory evidence_type 必须为 maven_license_inventory: {inventory_path}")
    components = inventory.get("components")
    if not isinstance(components, list):
        raise InputError(f"inventory components 必须是数组: {inventory_path}")

    if policy.get("schema_version") != 1:
        raise InputError(f"policy schema_version 必须为 1: {policy_path}")
    category_actions = policy.get("category_actions")
    if not isinstance(category_actions, dict) or not category_actions:
        raise InputError(f"policy category_actions 必须是非空对象: {policy_path}")
    allowlist = policy.get("allowlist")
    if not isinstance(allowlist, list):
        raise InputError(f"policy allowlist 必须是数组: {policy_path}")

    internal_cfg = policy.get("internal") if isinstance(policy.get("internal"), dict) else {}
    required_internal = [str(item).upper() for item in (internal_cfg.get("required_licenses") or ["MIT"])]
    group_prefix = str(internal_cfg.get("group_prefix") or INTERNAL_GROUP_PREFIX)
    maintenance = policy.get("maintenance") if isinstance(policy.get("maintenance"), dict) else {}
    try:
        max_validity_days = int(maintenance.get("max_entry_validity_days", DEFAULT_MAX_VALIDITY_DAYS) or 0)
    except (TypeError, ValueError) as exc:
        raise InputError(f"policy max_entry_validity_days 必须是整数: {exc}") from exc
    unmapped_action = str(policy.get("unmapped_category_action") or "deny").strip().lower()

    denied: list[dict] = []
    review_required: list[dict] = []
    approved_hits: list[dict] = []
    counts = {
        "components": 0,
        "internal": 0,
        "external": 0,
        "allowed": 0,
        "denied": 0,
        "review_required": 0,
        "allowlist_hits": 0,
        "runtime_blocked": 0,
        "invalid_entries": 0,
    }

    for component in components:
        counts["components"] += 1
        if not isinstance(component, dict):
            counts["invalid_entries"] += 1
            denied.append(_component_record(
                {}, "deny", "LIC-INV-001", "inventory 条目不是对象",
            ))
            continue

        is_internal = bool(component.get("is_internal")) or str(
            component.get("group") or ""
        ).lower().startswith(group_prefix.lower())
        normalized = [str(item).upper() for item in (component.get("normalized_licenses") or [])]
        runtime = bool(component.get("server_runtime_scope"))

        if is_internal:
            counts["internal"] += 1
            if any(license_name in required_internal for license_name in normalized):
                counts["allowed"] += 1
            else:
                denied.append(_component_record(
                    component, "deny", "LIC-INT-001",
                    f"internal 组件 normalized_licenses={normalized or 'EMPTY'} 未包含 {required_internal}",
                ))
                if runtime:
                    counts["runtime_blocked"] += 1
            continue

        counts["external"] += 1
        category = str(component.get("license_category") or "").strip().lower()
        fallback_rule = None
        if category in MISSING_CATEGORIES:
            # 判定权交回 policy：代码不再写死 deny，否则"内部自研不分发、许可证不作门禁"这类
            # 所有者决定只能改源码，且无法从数据文件里审计。
            # 缺 key 时仍回落到 unmapped_action（默认 deny），保持 fail-closed 语义不变。
            action = str(category_actions.get(category) or unmapped_action).strip().lower()
            fallback_rule = "LIC-EXT-002"
            default_reason = f"许可证缺失或未映射（license_category={category or 'EMPTY'}）"
        else:
            action = str(category_actions.get(category) or "").strip().lower()
            if not action:
                action = unmapped_action
                fallback_rule = "LIC-EXT-006"
            default_reason = f"类别 {category} 默认动作 {action}，尚未取得审批"

        if action == ALLOW:
            counts["allowed"] += 1
            continue

        verdict = resolve_allowlist(allowlist, component, today, max_validity_days)
        if verdict is not None and verdict["status"] == "approved":
            counts["allowed"] += 1
            counts["allowlist_hits"] += 1
            approved_hits.append(_component_record(
                component, action, "LIC-EXT-000", str(verdict["reason"]),
            ))
            continue

        rule = fallback_rule
        reason = default_reason
        if verdict is not None:
            rule = verdict["rule"] or fallback_rule or "LIC-EXT-001"
            reason = str(verdict["reason"])
        record = _component_record(component, action, rule or "LIC-EXT-001", reason)
        if action in DENY_BUCKET:
            denied.append(record)
        elif action in REVIEW_BUCKET:
            review_required.append(record)
        else:
            denied.append(record)
        if runtime:
            counts["runtime_blocked"] += 1

    counts["denied"] = len(denied)
    counts["review_required"] = len(review_required)
    blocked = denied + review_required
    decision = "allow" if not blocked else "fail"

    return {
        "tool": "check_license_policy.py",
        "evidence_type": "maven_license_policy_decision",
        "schema_version": 1,
        "generated_at_local": datetime.now().astimezone().isoformat(timespec="seconds"),
        "input": {
            "inventory": str(inventory_path),
            "inventory_sha256": sha256_file(inventory_path),
            "policy": str(policy_path),
            "policy_sha256": sha256_file(policy_path),
            "evaluated_on": today.isoformat(),
        },
        "decision": decision,
        "counts": counts,
        "denied": denied,
        "review_required": review_required,
        "approved_allowlist_hits": approved_hits,
        "flags": dict(FLAGS),
        "limits": list(LIMITS),
    }


def _print_summary(report: dict) -> None:
    counts = report["counts"]
    for record in report["denied"][:20]:
        print(f"[{record['rule']}] DENY {record['purl']} ({record['license_category']}): {record['reason']}")
    if len(report["denied"]) > 20:
        print(f"... denied 另有 {len(report['denied']) - 20} 条，见 JSON 输出")
    for record in report["review_required"][:20]:
        print(f"[{record['rule']}] REVIEW {record['purl']} ({record['license_category']}): {record['reason']}")
    if len(report["review_required"]) > 20:
        print(f"... review_required 另有 {len(report['review_required']) - 20} 条，见 JSON 输出")
    print(
        f"{report['decision'].upper()}: components={counts['components']} "
        f"allowed={counts['allowed']} denied={counts['denied']} "
        f"review_required={counts['review_required']} "
        f"allowlist_hits={counts['allowlist_hits']} runtime_blocked={counts['runtime_blocked']}"
    )


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Maven 组件许可证 CI 门禁（fail-closed）")
    parser.add_argument("--inventory", type=Path, required=True, help="license inventory JSON 路径")
    parser.add_argument("--policy", type=Path, default=DEFAULT_POLICY, help="license policy JSON 路径")
    parser.add_argument("--output", type=Path, help="决策 JSON 输出路径")
    parser.add_argument("--json", action="store_true", help="在 stdout 打印完整决策 JSON")
    parser.add_argument("--record-only", action="store_true",
                        help="只记录不阻断：判定与 denied/review_required 数组照常产出并写盘，退出码恒 0。"
                             "供策略所有者把许可证降为非阻断项时使用（见 license-policy.json description）")
    args = parser.parse_args(argv)

    try:
        report = build_report(args.inventory, args.policy)
    except InputError as exc:
        print(f"ERROR: {exc}", file=sys.stderr)
        return 2

    # blocking 是**所有者决定的落点**，不是判定的一部分：判定照常算，
    # 只是记不记账由调用方声明。写在 report 里，保证 JSON 证据自身能说明"这次没阻断是声明的，不是漏判"。
    report["blocking"] = not args.record_only

    payload = json.dumps(report, ensure_ascii=False, indent=2, sort_keys=True)
    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(payload + "\n", encoding="utf-8", newline="\n")
        print(f"wrote {args.output}")
    if args.json:
        print(payload)
    else:
        _print_summary(report)
    if args.record_only and report["decision"] != "allow":
        print("RECORD-ONLY：本次判定为 fail 但按 --record-only 不阻断（denied/review_required 数组未清空，见 --output JSON）。")
        return 0
    return 0 if report["decision"] == "allow" else 1


if __name__ == "__main__":
    raise SystemExit(main())