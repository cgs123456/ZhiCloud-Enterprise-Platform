#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""安全基线扫描结果的可审计抑制（suppression）能力。

要解决的问题：扫描器把"生产路径"与"演示/历史路径"的命中混在一份统计里，
demo-data 下的 160 条 R7（P1）与 V1.1 种子里的 32 条 P0 无法区分，门禁长期红，
久而久之红色就不再是信号。抑制机制让"生产路径 P0 = 0"成为可判定的结论，
同时不让任何一条命中从证据里消失。

三条不可破坏的约束：

  1. fail-closed：字段缺失、格式错误、规则未知、日期超期或超过上限、绝对路径、
     以及"没有命中任何 finding"的 stale 条目，一律让报告判 failed。
     写错抑制文件只会更红，不会更绿。
  2. 永不隐藏：被抑制的 finding 仍完整出现在 suppressed_findings 与
     suppression.by_* 统计里，只是不再计入阻断（findings_by_severity）。
  3. 零预置：模板是空数组。每条抑制都必须填写 reason / owner / approved_by /
     expires_on / evidence，不提供"先加上、以后再补"的口子。

命令行：
    python security_baseline_suppressions.py --check 抑制文件 [--report 报告.json]
校验格式；给了 --report 时额外核对 stale（未命中任何 finding 的条目）。
存在 invalid 或 unmatched 时 exit 1。
"""
from __future__ import annotations

import argparse
import hashlib
import json
import re
import sys
from datetime import date, datetime, timedelta
from pathlib import Path
from typing import Any

# 每条抑制必须填写的字段。缺任何一个即判 invalid（fail-closed）。
REQUIRED_FIELDS = (
    "file",
    "rule",
    "disposition",
    "reason",
    "owner",
    "approved_by",
    "expires_on",
    "evidence",
)

# disposition 只有两种：已缓解（有对应修复）或已接受风险（有审批记录）。
ALLOWED_DISPOSITIONS = ("mitigated", "accepted_risk")
# exact = 精确到 文件+规则+行号+证据指纹；file_rule = 文件+规则（整文件豁免）。
ALLOWED_SCOPES = ("exact", "file_rule")
# 不写 scope 时取最严格的 exact，避免"少写一个字段就变成整文件豁免"。
DEFAULT_SCOPE = "exact"
# 抑制最长有效期，超过即 invalid，强制定期复核而不是一次批准永久有效。
DEFAULT_MAX_EXPIRY_DAYS = 180

HEX64 = re.compile(r"^[0-9a-f]{64}$")
ISO_DATE = re.compile(r"^[0-9]{4}-[0-9]{2}-[0-9]{2}$")

POLICY = {
    "fail_closed": True,
    "never_hidden": True,
    "predefined_entries": 0,
    "max_expiry_days": DEFAULT_MAX_EXPIRY_DAYS,
    "required_fields": list(REQUIRED_FIELDS),
    "allowed_dispositions": list(ALLOWED_DISPOSITIONS),
    "allowed_scopes": list(ALLOWED_SCOPES),
    "default_scope": DEFAULT_SCOPE,
    "note": "模板为空数组；任何抑制都必须人工填写 reason/owner/approved_by/expires_on/evidence。",
}

def sha256_text(text: str) -> str:
    """UTF-8 文本的 SHA-256 十六进制摘要。"""
    return hashlib.sha256(str(text).encode("utf-8")).hexdigest()


def normalize_rel_path(raw: Any) -> str:
    """统一成 posix 相对路径。不解析符号链接，不做大小写折叠。"""
    text = str(raw).replace("\\", "/").strip()
    while text.startswith("./"):
        text = text[2:]
    return text


def finding_evidence_hash(evidence: Any) -> str:
    """finding.evidence 的指纹，用于与条目的 evidence_sha256 精确比对。"""
    return sha256_text(str(evidence).strip())


def path_reason(rel: str) -> str:
    """返回空串表示路径安全；否则返回机器可读的原因码。

    绝对路径（含盘符与 ~）与 .. 穿越一律拒绝：抑制条目必须锚定仓库内
    的相对路径，否则换台机器就会指向别的仓库，等于凭空签发豁免。
    """
    if not rel:
        return "empty_path"
    if rel.startswith("/") or rel.startswith("\\"):
        return "absolute_path"
    if re.match(r"^[A-Za-z]:", rel):
        return "absolute_path"
    if rel.startswith("~"):
        return "absolute_path"
    if ".." in rel.split("/"):
        return "path_traversal"
    return ""


def coerce_today(value: Any) -> date:
    """把 today 参数统一成 date；None 表示取系统当天。"""
    if value is None:
        return date.today()
    if isinstance(value, datetime):
        return value.date()
    if isinstance(value, date):
        return value
    text = str(value).strip()
    if ISO_DATE.match(text):
        return date.fromisoformat(text)
    return datetime.fromisoformat(text).date()


def load_suppression_file(path: str | Path) -> list[Any]:
    """读取抑制文件。顶层必须是 JSON 数组，否则抛错。

    不接受 {"suppressions": [...]} 这类包装：多一层结构就多一处可以被
    悄悄塞东西的地方，也让 diff review 变难。
    """
    data = json.loads(Path(path).read_text(encoding="utf-8"))
    if not isinstance(data, list):
        raise ValueError(
            "抑制文件顶层必须是 JSON 数组，实际为 " + type(data).__name__
        )
    return data


def empty_template_text() -> str:
    """空模板。刻意不带任何预置条目。"""
    return "[]\n"

def validate_entries(
    entries: list[Any],
    today: Any = None,
    max_expiry_days: int = DEFAULT_MAX_EXPIRY_DAYS,
    known_rules: Any = None,
) -> dict[str, Any]:
    """逐条校验抑制条目，返回 {"valid": [...], "invalid": [...]}。

    valid 里的条目带下划线前缀的内部字段（_file / _rule / _scope / _line /
    _evidence_sha256 / _expires_on / _index），供匹配使用；序列化到报告前
    会用 suppression_entry_public() 去掉它们，避免内部字段外泄到证据里。
    """
    day = coerce_today(today)
    limit = day + timedelta(days=int(max_expiry_days))
    valid: list[dict[str, Any]] = []
    invalid: list[dict[str, Any]] = []
    for index, entry in enumerate(entries or []):
        if not isinstance(entry, dict):
            invalid.append({"index": index, "entry": entry,
                            "reasons": ["not_an_object"]})
            continue
        reasons: list[str] = []
        for field in REQUIRED_FIELDS:
            value = entry.get(field)
            if value is None:
                reasons.append("missing_field:" + field)
            elif isinstance(value, str) and not value.strip():
                reasons.append("empty_field:" + field)

        rel = normalize_rel_path(entry.get("file", ""))
        unsafe = path_reason(rel)
        if unsafe:
            reasons.append(unsafe)

        rule = str(entry.get("rule", "")).strip().upper()
        if rule and known_rules is not None and rule not in known_rules:
            reasons.append("unknown_rule:" + rule)

        disposition = str(entry.get("disposition", "")).strip().lower()
        if disposition and disposition not in ALLOWED_DISPOSITIONS:
            reasons.append("invalid_disposition")

        scope = str(entry.get("scope", DEFAULT_SCOPE) or DEFAULT_SCOPE).strip().lower()
        if scope not in ALLOWED_SCOPES:
            reasons.append("invalid_scope")

        expires_on: date | None = None
        expires_raw = str(entry.get("expires_on", "")).strip()
        if expires_raw:
            if not ISO_DATE.match(expires_raw):
                reasons.append("invalid_expires_on_format")
            else:
                try:
                    expires_on = date.fromisoformat(expires_raw)
                except ValueError:
                    reasons.append("invalid_expires_on_format")
                    expires_on = None
            if expires_on is not None:
                if expires_on < day:
                    reasons.append("expired")
                elif expires_on > limit:
                    reasons.append("expiry_too_far")

        line_no: int | None = None
        digest: str | None = None
        if scope == "exact":
            raw_line = entry.get("line")
            if isinstance(raw_line, bool) or raw_line is None:
                reasons.append("missing_line")
            elif not isinstance(raw_line, int):
                reasons.append("invalid_line")
            elif int(raw_line) <= 0:
                reasons.append("invalid_line")
            else:
                line_no = int(raw_line)
            raw_digest = entry.get("evidence_sha256")
            if raw_digest is None or not str(raw_digest).strip():
                reasons.append("missing_evidence_sha256")
            else:
                digest_raw = str(raw_digest).strip().lower()
                if not HEX64.match(digest_raw):
                    reasons.append("invalid_evidence_sha256")
                else:
                    digest = digest_raw

        if reasons:
            invalid.append({"index": index, "file": rel, "rule": rule,
                            "scope": scope, "reasons": reasons})
            continue

        record = dict(entry)
        record.update({
            "_index": index,
            "_file": rel,
            "_rule": rule,
            "_scope": scope,
            "_line": line_no,
            "_evidence_sha256": digest,
            "_expires_on": expires_on.isoformat() if expires_on else None,
            "_disposition": disposition,
        })
        valid.append(record)
    return {"valid": valid, "invalid": invalid}

def suppression_entry_public(entry: dict[str, Any]) -> dict[str, Any]:
    """去掉内部字段后的条目副本，用于写进报告证据。"""
    return {k: v for k, v in entry.items() if not k.startswith("_")}


def entry_matches(entry: dict[str, Any], finding: dict[str, Any]) -> bool:
    """判断一条有效抑制是否命中一条 finding。

    exact：文件 + 规则 + 行号 + 证据指纹四项全等。证据指纹让"代码改了但抑制
    还挂着"变成 unmatched（stale）而不是继续豁免，这是 fail-closed 的关键。
    file_rule：文件 + 规则全等，不校验行号与指纹（用于整文件豁免，需要审批）。
    """
    if entry["_file"] != normalize_rel_path(finding.get("file", "")):
        return False
    if entry["_rule"] != str(finding.get("rule", "")).strip().upper():
        return False
    if entry["_scope"] == "exact":
        if entry["_line"] != int(finding.get("line", 0) or 0):
            return False
        if entry["_evidence_sha256"] != finding_evidence_hash(finding.get("evidence", "")):
            return False
    return True


def apply_suppressions(
    findings: list[dict[str, Any]],
    entries: list[Any],
    today: Any = None,
    known_rules: Any = None,
    max_expiry_days: int = DEFAULT_MAX_EXPIRY_DAYS,
) -> dict[str, Any]:
    """把抑制应用到 findings 上。

    返回 remaining / suppressed / invalid / unmatched / valid：
      * remaining  未被抑制，仍然阻断；
      * suppressed 被抑制，但完整保留 finding 原文 + 抑制出处；
      * invalid    条目本身不合格 -> 报告必须 failed；
      * unmatched  条目合格但一条 finding 都没命中（stale）-> 报告必须 failed。
    """
    checked = validate_entries(entries or [], today=today,
                               max_expiry_days=max_expiry_days,
                               known_rules=known_rules)
    valid = checked["valid"]
    remaining: list[dict[str, Any]] = []
    suppressed: list[dict[str, Any]] = []
    used: set[int] = set()
    for finding in findings or []:
        hit: dict[str, Any] | None = None
        for entry in valid:
            if entry_matches(entry, finding):
                hit = entry
                break
        if hit is None:
            remaining.append(finding)
            continue
        used.add(hit["_index"])
        item = dict(finding)
        item["suppression"] = {
            "source_index": hit["_index"],
            "file": hit["_file"],
            "rule": hit["_rule"],
            "scope": hit["_scope"],
            "line": hit["_line"],
            "evidence_sha256": hit["_evidence_sha256"],
            "disposition": hit["_disposition"],
            "reason": str(hit.get("reason", "")).strip(),
            "owner": str(hit.get("owner", "")).strip(),
            "approved_by": str(hit.get("approved_by", "")).strip(),
            "expires_on": hit["_expires_on"],
            "approval_evidence": str(hit.get("evidence", "")).strip(),
        }
        suppressed.append(item)
    unmatched: list[dict[str, Any]] = []
    for entry in valid:
        if entry["_index"] in used:
            continue
        unmatched.append({
            "index": entry["_index"],
            "file": entry["_file"],
            "rule": entry["_rule"],
            "scope": entry["_scope"],
            "line": entry["_line"],
            "disposition": entry["_disposition"],
            "reason": str(entry.get("reason", "")).strip(),
            "expires_on": entry["_expires_on"],
        })
    return {
        "checked_at": coerce_today(today).isoformat(),
        "remaining": remaining,
        "suppressed": suppressed,
        "invalid": checked["invalid"],
        "unmatched": unmatched,
        "valid": valid,
    }


def suppression_section(
    result: dict[str, Any],
    entries: list[Any],
    max_expiry_days: int = DEFAULT_MAX_EXPIRY_DAYS,
) -> dict[str, Any]:
    """报告里的 suppression 段：统计 + 完整的不合格/未命中清单。"""
    by_severity: dict[str, int] = {}
    by_rule: dict[str, int] = {}
    by_disposition: dict[str, int] = {}
    for item in result["suppressed"]:
        sev = str(item.get("severity", "")).strip() or "UNKNOWN"
        by_severity[sev] = by_severity.get(sev, 0) + 1
        rule = str(item.get("rule", "")).strip() or "UNKNOWN"
        by_rule[rule] = by_rule.get(rule, 0) + 1
        disp = str(item.get("suppression", {}).get("disposition", "")).strip() or "UNKNOWN"
        by_disposition[disp] = by_disposition.get(disp, 0) + 1
    policy = dict(POLICY)
    policy["max_expiry_days"] = int(max_expiry_days)
    return {
        "enabled": bool(entries),
        "policy": policy,
        "entries": len(entries or []),
        "valid": len(result["valid"]),
        "applied": len(result["suppressed"]),
        "invalid": len(result["invalid"]),
        "unmatched": len(result["unmatched"]),
        "by_severity": by_severity,
        "by_rule": by_rule,
        "by_disposition": by_disposition,
        "invalid_entries": result["invalid"],
        "unmatched_entries": result["unmatched"],
        "fail_closed_triggered": bool(result["invalid"] or result["unmatched"]),
    }


def status_for(
    remaining_by_severity: dict[str, int],
    result: dict[str, Any],
    fail_on: str = "P0",
    controls_failed: int = 0,
) -> str:
    """抑制之后的报告状态。三种取值，语义互斥：
      failed                    仍有 P0 阻断，或抑制文件本身不合格/stale；
      passed_with_suppressions  应用了抑制且剩余阻断为 0；
      passed                    没有任何抑制，也没命中。
    """
    if result["invalid"] or result["unmatched"]:
        return "failed"
    blocking = int(remaining_by_severity.get(fail_on, 0)) + int(controls_failed)
    if blocking:
        return "failed"
    if result["suppressed"]:
        return "passed_with_suppressions"
    return "passed"

def main(argv: list[str] | None = None) -> int:
    ap = argparse.ArgumentParser(description="安全基线抑制文件校验（fail-closed）")
    ap.add_argument("--check", required=True, type=Path,
                    help="抑制 JSON 文件路径（顶层必须是数组）")
    ap.add_argument("--report", type=Path, default=None,
                    help="可选：security_baseline_scan 报告，用于 stale 核对")
    ap.add_argument("--max-expiry-days", type=int, default=DEFAULT_MAX_EXPIRY_DAYS)
    ap.add_argument("--known-rules", default="",
                    help="逗号分隔的合法规则 ID；留空表示不校验规则名")
    ap.add_argument("--output", type=Path, default=None)
    args = ap.parse_args(argv)

    try:
        entries = load_suppression_file(args.check)
    except (OSError, ValueError) as exc:
        print("抑制文件不可读或格式错误：" + str(exc), file=sys.stderr)
        return 2

    known: list[str] | None = None
    if args.known_rules.strip():
        known = sorted({r.strip().upper() for r in args.known_rules.split(",")
                        if r.strip()})

    findings: list[dict[str, Any]] = []
    stale_check = "skipped"
    if args.report is not None:
        report = json.loads(args.report.read_text(encoding="utf-8"))
        findings = report.get("findings", [])
        stale_check = "enabled"

    result = apply_suppressions(findings, entries,
                                max_expiry_days=args.max_expiry_days,
                                known_rules=known)
    if stale_check == "skipped":
        # 没有报告就无法证明条目不是 stale；这里只做格式校验，不假装通过了
        # stale 核对，因此 unmatched 不参与 fail-closed 判定。
        result["unmatched"] = []

    section = suppression_section(result, entries,
                                  max_expiry_days=args.max_expiry_days)
    section["stale_check"] = stale_check

    payload = {
        "tool": "security_baseline_suppressions.py",
        "suppression_file": str(args.check),
        "report": str(args.report) if args.report else None,
        "suppression": section,
    }
    text = json.dumps(payload, ensure_ascii=False, indent=2, sort_keys=True) + "\n"
    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(text, encoding="utf-8", newline="\n")
    print(json.dumps({
        "entries": section["entries"],
        "valid": section["valid"],
        "applied": section["applied"],
        "invalid": section["invalid"],
        "unmatched": section["unmatched"],
        "stale_check": stale_check,
    }, ensure_ascii=False))
    for bad in section["invalid_entries"]:
        print("invalid #" + str(bad.get("index")) + " " + ",".join(bad["reasons"]),
              file=sys.stderr)
    for bad in section["unmatched_entries"]:
        print("stale #" + str(bad["index"]) + " " + str(bad["file"]) + " "
              + str(bad["rule"]), file=sys.stderr)
    return 1 if section["fail_closed_triggered"] else 0


if __name__ == "__main__":
    raise SystemExit(main())