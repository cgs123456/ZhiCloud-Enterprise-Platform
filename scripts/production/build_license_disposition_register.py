#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Maven 许可证处置台账生成器（T-107）。

门禁 check_license_policy.py 只回答"是否被策略阻断"，不回答"谁批准、依据什么、有效期多久"。
本脚本把门禁输出的 denied + review_required 转成一份可交法务逐项签署的台账：
每行一个 purl，带类别、是否在服务端运行时闭包内、策略动作、原始许可证标签与工程侧提示；
处置字段（disposition / approved_by / approved_on / expires_on / scope）默认为空，空即"未处置"。

关键约束（勿回退）：
1. 台账本身不解除门禁。只有法务批准后把条目写进 license-policy.json 的 allowlist
   （带 decision / approved_by / approved_on / expires_on / scope），门禁才可能转绿。
   严禁为"变绿"预填台账或放宽 policy —— 那样门禁等于不存在。
2. 依赖升级重扫后必须重新生成本台账；用 --previous-json 按 purl 合并上一版人工填写内容，
   避免把已取得的审批冲掉（依赖版本变化 = 新的 purl = 新的待办，不会静默继承）。
3. 上一版被阻断、这一版不再出现的条目进入 resolved_since_previous，供人工复核，
   不静默丢弃。

本脚本只做工程整理，不构成法律意见，也不宣称任何许可证已获批准。
"""
from __future__ import annotations

import argparse
import hashlib
import json
import sys
from datetime import datetime
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]

PENDING = "PENDING"
DISPOSITION_FIELDS = ("disposition", "approved_by", "approved_on", "expires_on", "scope")
MANUAL_FIELDS = ("disposition", "approved_by", "approved_on", "expires_on", "scope", "notes")

ACTION_ORDER = {"deny": 0, "deny_until_approved": 0, "review": 1, "review_and_record": 1}

FLAGS = {
    "production_ready": False,
    "gate3_passed": False,
    "t107_status": "OPEN",
    "independent_review": False,
}
LIMITS = [
    "台账是工程脚手架，不是法律意见；每行的处置结论必须由法务或授权人填写。",
    "台账转绿不等于合规：放行仅表示该项已按内部策略完成审批与留痕。",
    "依赖升级后必须重扫 inventory 并重新审批，不得沿用过期结论。",
]
PENDING_RULINGS = [
    "弱 Copyleft 的阻断强度：现行 policy 把 weak_copyleft 计为阻断（review_and_record），"
    "这是本台账 51 项与设计文档 14 项的差异来源；需法务裁定是维持阻断还是降级为仅记录。",
    "GPL-2.0-only / GPL-3.0-only 的分类争议：二者当前被归为 weak_copyleft，"
    "通常应按强 Copyleft 处理，需法务裁定并同步修正映射表。",
    "MulanPSL-2.0 未映射：归一结果为 UNMAPPED: 却被计为 permissive_standard，"
    "需补映射并复核该判定是否成立。",
    "netty-tcnative-boringssl-static 静态链接 BoringSSL 原生代码："
    "Maven 元数据声明 Apache-2.0，但内含第三方原生代码的许可证义务需单独确认。",
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
    except json.JSONDecodeError as exc:
        raise InputError(f"{label} 不是合法 JSON: {path} ({exc})") from exc
    if not isinstance(data, dict):
        raise InputError(f"{label} 顶层必须是对象: {path}")
    return data


def auto_notes(item: dict) -> str:
    """工程侧提示：只描述已知争议与可行动方向，不下法律结论。"""
    label = str(item.get("raw_license_label") or "")
    category = str(item.get("license_category") or "")
    artifact = str(item.get("artifact") or "")
    notes: list[str] = []
    if category == "weak_copyleft":
        notes.append(
            "阻断强度待裁定：现行 policy 将 weak_copyleft 计为阻断，"
            "这是 51 项 vs 设计文档 14 项的差异来源；若降级为仅记录则本项不再阻断"
        )
    if "GPL-2.0-only" in label or "GPL-3.0-only" in label:
        notes.append(
            "分类争议：纯 GPL（无 classpath exception）当前归为 weak_copyleft，"
            "通常应按强 Copyleft 处理，需法务裁定"
        )
    if "MulanPSL-2.0" in label:
        notes.append(
            "分类争议：MulanPSL-2.0 在映射表中缺失（归一为 UNMAPPED:）"
            "却被计为 permissive_standard，需补映射并复核"
        )
    if artifact == "netty-tcnative-boringssl-static":
        notes.append(
            "原生代码：静态链接 BoringSSL，元数据声明 Apache-2.0，"
            "内含第三方原生代码的许可证义务需单独确认"
        )
    if not item.get("server_runtime_scope"):
        notes.append(
            "不在服务端运行时闭包内（构建/测试/可选依赖）："
            "可优先考虑直接移除依赖，这是最快解除阻断的路径"
        )
    return "；".join(notes)


def normalise_item(record: dict, bucket: str) -> dict:
    item = {
        "purl": str(record.get("purl") or "").strip(),
        "group": str(record.get("group") or "").strip(),
        "artifact": str(record.get("artifact") or "").strip(),
        "version": str(record.get("version") or "").strip(),
        "license_category": str(record.get("license_category") or "").strip(),
        "policy_action": str(record.get("policy_action") or bucket).strip(),
        "rule": str(record.get("rule") or "").strip(),
        "server_runtime_scope": bool(record.get("server_runtime_scope")),
        "raw_license_label": str(record.get("raw_license_label") or "").strip(),
    }
    for field in DISPOSITION_FIELDS:
        item[field] = "" if field != "disposition" else PENDING
    item["scope"] = ""
    item["notes"] = auto_notes(item)
    return item


def sort_key(item: dict):
    return (
        ACTION_ORDER.get(item["policy_action"], 9),
        item["license_category"],
        item["group"],
        item["artifact"],
        item["version"],
        item["purl"],
    )


def build_items(gate: dict) -> list[dict]:
    denied = gate.get("denied")
    review = gate.get("review_required")
    if not isinstance(denied, list) or not isinstance(review, list):
        raise InputError("门禁 JSON 必须包含 denied 与 review_required 数组")
    items = []
    for bucket, records in (("deny_until_approved", denied), ("review_and_record", review)):
        for record in records:
            if isinstance(record, dict):
                items.append(normalise_item(record, bucket))
    items.sort(key=sort_key)
    return items


def merge_previous(items: list[dict], previous: dict) -> tuple[list[str], int]:
    """按 purl 合并上一版人工填写内容，返回（已解除条目 purl 列表，继承人工内容的行数）。"""
    previous_items = previous.get("items")
    if not isinstance(previous_items, list):
        return [], 0
    by_purl = {str(row.get("purl") or ""): row for row in previous_items if isinstance(row, dict)}
    inherited = 0
    for item in items:
        old = by_purl.get(item["purl"])
        if not old:
            continue
        for field in MANUAL_FIELDS:
            value = str(old.get(field) or "").strip()
            if not value:
                continue
            if field == "disposition" and value == PENDING:
                continue
            item[field] = value
            inherited += 1
    current = {item["purl"] for item in items}
    resolved = [purl for purl in by_purl if purl and purl not in current]
    return sorted(resolved), inherited


def count_dispositions(items: list[dict]) -> dict:
    counter: dict[str, int] = {}
    for item in items:
        key = item.get("disposition") or PENDING
        counter[key] = counter.get(key, 0) + 1
    return dict(sorted(counter.items()))


def render_markdown(payload: dict) -> str:
    counts = payload["counts"]
    items = payload["items"]
    lines: list[str] = []
    lines.append("# Maven 组件许可证处置台账（法务待办）")
    lines.append("")
    lines.append(f"- 生成时间（本地）：{payload['generated_at_local']}")
    lines.append(f"- 门禁输入：`{payload['input']['gate']}`")
    lines.append(f"- 门禁输入 SHA256：`{payload['input']['gate_sha256']}`")
    lines.append(f"- 门禁判定：`{payload['input'].get('gate_decision')}`")
    lines.append(
        f"- 阻断项合计：**{counts['total']}**（deny_until_approved {counts['denied']} "
        f"+ review_and_record {counts['review_required']}）"
    )
    lines.append(
        f"- 处置状态：{json.dumps(counts['dispositions'], ensure_ascii=False)}"
        " —— 全部 PENDING 时台账不解除任何门禁"
    )
    lines.append(
        f"- 运行时拆分：在服务端运行时闭包内 **{counts['in_server_runtime']}** 项"
        f"（随制品分发，优先处置）；不在闭包内 **{counts['not_in_server_runtime']}** 项"
        "（多为测试 / provided / optional 依赖，通常可先评估直接移除）"
    )
    lines.append(
        "> 注意：运行时拆分只用于排定处置优先级，不代表不在闭包内的组件无需处置 —— "
        "源码分发、测试代码与构建产物同样可能触发许可证义务。"
    )
    lines.append("")
    lines.append("> 本台账由 `scripts/production/build_license_disposition_register.py` 生成，")
    lines.append("> 只做工程整理，不构成法律意见，也不宣称任何许可证已获批准。")
    lines.append("")
    lines.append("## 一、待法务裁定（工程无权自行决定）")
    lines.append("")
    for index, ruling in enumerate(payload["pending_legal_rulings"], start=1):
        lines.append(f"{index}. {ruling}")
    lines.append("")

    header = (
        "| # | 组件（group:artifact:version） | 类别 | 运行时 | 策略动作 | 处置 | 批准人 | 批准日期 | 有效期至 | 工程提示 |"
    )
    split = "| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |"

    def table(rows: list[dict], title: str) -> None:
        lines.append(f"## {title}（{len(rows)} 项）")
        lines.append("")
        if not rows:
            lines.append("_无_")
            lines.append("")
            return
        lines.append(header)
        lines.append(split)
        for index, item in enumerate(rows, start=1):
            coordinate = f"{item['group']}:{item['artifact']}:{item['version']}"
            lines.append(
                f"| {index} | `{coordinate}` | {item['license_category']} | "
                f"{'是' if item['server_runtime_scope'] else '否'} | {item['policy_action']} | "
                f"{item['disposition']} | {item['approved_by'] or '—'} | "
                f"{item['approved_on'] or '—'} | {item['expires_on'] or '—'} | "
                f"{item['notes'] or '—'} |"
            )
        lines.append("")
        lines.append("完整 purl 见同名 `.json`（表格按坐标展示，purl 以 JSON 为准）。")
        lines.append("")

    denied_rows = [i for i in items if ACTION_ORDER.get(i["policy_action"], 9) == 0]
    review_rows = [i for i in items if ACTION_ORDER.get(i["policy_action"], 9) != 0]
    table(denied_rows, "二、deny_until_approved（未经批准禁止引入）")
    table(review_rows, "三、review_and_record（弱 Copyleft / 需逐项登记）")

    lines.append("## 四、处置流程")
    lines.append("")
    lines.append("1. 法务（或授权人）逐项填写 `disposition`：`approve` / `reject` / `replace`，")
    lines.append("   并补齐 `approved_by`、`approved_on`、`expires_on`（建议不超过 365 天）、`scope`。")
    lines.append("2. 工程按结论执行：`reject` / `replace` 的组件必须真正从依赖树里移除，")
    lines.append("   不能只改台账文字——移除后重跑 inventory，该 purl 不再出现才算闭环。")
    lines.append("3. `approve` 的条目由工程写入 `scripts/production/license-policy.json` 的 `allowlist`，")
    lines.append("   字段与台账一一对应；allowlist 在审批发生前必须为空。")
    lines.append("4. 重跑 `check_license_policy.py`；依赖升级后必须重扫并重新审批，不得沿用过期结论。")
    lines.append("")
    lines.append("## 五、禁止事项")
    lines.append("")
    for limit in payload["limits"]:
        lines.append(f"- {limit}")
    lines.append("- 不得为让门禁变绿而预填台账、预填 allowlist、放宽 policy，")
    lines.append("  或把 deny 类别改成 permissive —— 那等于取消门禁。")
    lines.append("")
    if payload["resolved_since_previous"]:
        lines.append("## 六、上一版阻断、本版不再出现（需人工复核）")
        lines.append("")
        for purl in payload["resolved_since_previous"]:
            lines.append(f"- `{purl}`")
        lines.append("")
    lines.append(f"- 语义标记：{json.dumps(payload['flags'], ensure_ascii=False)}")
    lines.append("")
    return "\n".join(lines)


def build_payload(gate_path: Path, gate: dict, previous_path: Path | None,
                  previous: dict | None) -> dict:
    items = build_items(gate)
    resolved: list[str] = []
    if previous is not None:
        resolved, _ = merge_previous(items, previous)
    dispositions = count_dispositions(items)
    counts = gate.get("counts") if isinstance(gate.get("counts"), dict) else {}
    return {
        "evidence_type": "maven_license_disposition_register",
        "schema_version": 1,
        "tool": "build_license_disposition_register.py",
        "generated_at_local": datetime.now().astimezone().isoformat(timespec="seconds"),
        "input": {
            "gate": str(gate_path),
            "gate_sha256": sha256_file(gate_path),
            "gate_decision": gate.get("decision"),
            "gate_counts": counts,
            "previous_register": str(previous_path) if previous_path else None,
            "previous_register_sha256": sha256_file(previous_path) if previous_path else None,
        },
        "counts": {
            "total": len(items),
            "denied": sum(1 for i in items if ACTION_ORDER.get(i["policy_action"], 9) == 0),
            "review_required": sum(1 for i in items if ACTION_ORDER.get(i["policy_action"], 9) != 0),
            "pending": dispositions.get(PENDING, 0),
            "dispositions": dispositions,
            "in_server_runtime": sum(1 for i in items if i["server_runtime_scope"]),
            "not_in_server_runtime": sum(1 for i in items if not i["server_runtime_scope"]),
            "resolved_since_previous": len(resolved),
        },
        "items": items,
        "resolved_since_previous": resolved,
        "pending_legal_rulings": list(PENDING_RULINGS),
        "flags": dict(FLAGS),
        "limits": list(LIMITS),
    }


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="生成 Maven 许可证处置台账（法务待办）")
    parser.add_argument("--gate", type=Path, required=True, help="check_license_policy.py 输出的门禁 JSON")
    parser.add_argument("--output-md", type=Path, help="台账 Markdown 输出路径")
    parser.add_argument("--output-json", type=Path, help="台账 JSON 输出路径")
    parser.add_argument("--previous-json", type=Path, help="上一版台账 JSON，用于按 purl 继承人工填写内容")
    args = parser.parse_args(argv)

    try:
        gate = load_json(args.gate, "门禁 JSON")
        previous = None
        previous_path = None
        if args.previous_json:
            previous_path = args.previous_json
            previous = load_json(previous_path, "上一版台账 JSON")
        payload = build_payload(args.gate, gate, previous_path, previous)
    except InputError as exc:
        print(f"ERROR: {exc}", file=sys.stderr)
        return 2

    json_text = json.dumps(payload, ensure_ascii=False, indent=2, sort_keys=True)
    if args.output_json:
        args.output_json.parent.mkdir(parents=True, exist_ok=True)
        args.output_json.write_text(json_text + "\n", encoding="utf-8", newline="\n")
        print(f"wrote {args.output_json}")
    if args.output_md:
        args.output_md.parent.mkdir(parents=True, exist_ok=True)
        args.output_md.write_text(render_markdown(payload) + "\n", encoding="utf-8", newline="\n")
        print(f"wrote {args.output_md}")
    if not args.output_json and not args.output_md:
        print(json_text)
    counts = payload["counts"]
    print(
        f"register: total={counts['total']} denied={counts['denied']} "
        f"review_required={counts['review_required']} pending={counts['pending']} "
        f"resolved_since_previous={counts['resolved_since_previous']}"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
