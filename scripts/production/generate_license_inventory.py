#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""从 CycloneDX BOM + server runtime dependency tree 生成许可证治理清单（T-107）。

存在理由：此前 `maven-license-inventory-*.json` 由一次性脚本生成，脚本未入库，
导致 CI 流程第 4 步"重新生成 inventory"无法在干净 checkout 上复现。本脚本把该步骤固化，
并让分类规则显式、可评审、可回归。

映射表来源：`maven-license-inventory-20260928_0028.json`（862 组件/48 个外部 raw label）
这一份已审核基线。表中未出现的标签一律归一为 `UNMAPPED:<raw>` 并归类 `unmapped`，
由 `check_license_policy.py` 以 deny 处理（fail-closed）。

已知历史不一致（保留以复现基线，不静默"修正"）：
- `MulanPSL-2.0` 在基线中被记为 `UNMAPPED:MulanPSL-2.0` 却归类 permissive_standard。
- `GPL-2.0-only` / `GPL-3.0-only` 在基线中被归类 weak_copyleft；按通行理解它们属于强 copyleft，
  该归类偏宽松，已作为争议项记录，需法务/负责人裁决后调整策略，不在本脚本内静默变更。
- 基线 `server_runtime_scope_components = 707`，本脚本复现为 **719**。差值为 12 个 netty
  带 classifier 的构件（`netty-transport-native-epoll` / `netty-transport-native-kqueue` /
  `netty-resolver-dns-native-macos` / `netty-tcnative-boringssl-static`）：它们在
  `dependency:tree` 中以 6 字段形态出现（`group:artifact:type:classifier:version:scope`），
  基线解析器只按 5 字段匹配因而漏统计。这 12 个构件声明 Apache-2.0、归类 permissive_standard，
  不改变门禁结论（denied=13 / review_required=38 与基线逐 purl 一致，见回归测试）。
  注意：`netty-tcnative-boringssl-static` 以静态链接方式内嵌 BoringSSL 原生代码，
  其声明许可证虽为 Apache-2.0，静态链接引入的第三方原生代码许可需法务另行确认。

本脚本不做法律定性，也不表示任何许可证已获审批。
"""
from __future__ import annotations

import argparse
import hashlib
import json
import re
import sys
from collections import Counter
from datetime import datetime
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]
DEFAULT_INTERNAL_PREFIX = "cn.zhicloud"
DEFAULT_RUNTIME_COMMAND = "mvn -pl zhicloud-server dependency:tree -Dscope=runtime"

# raw license 标签（单个 license 条目）-> 归一标签
RAW_TO_TOKEN = {
    "0BSD": "0BSD",
    "AL 2.0": "Apache-2.0",
    "Apache License v2": "Apache-2.0",
    "Apache-2.0": "Apache-2.0",
    "BSD 3-clause License w/nuclear disclaimer": "BSD-3-Clause-with-nuclear-disclaimer-custom",
    "BSD licence": "BSD-version-unspecified",
    "BSD-2-Clause": "BSD-2-Clause",
    "BSD-3-Clause": "BSD-3-Clause",
    "BSD-4-Clause": "BSD-4-Clause",
    "Bouncy Castle Licence": "Bouncy-Castle-MIT-like",
    "CC0-1.0": "CC0-1.0",
    "CDDL, v1.0": "CDDL-1.0",
    "EPL 1.0": "EPL-1.0",
    "EPL-1.0": "EPL-1.0",
    "EPL-2.0": "EPL-2.0",
    "Eclipse Public License (EPL) 2.0": "EPL-2.0",
    "GNU General Public License, version 2 with the GNU Classpath Exception": "GPL-2.0-with-classpath-exception",
    "GNU LESSER GENERAL PUBLIC LICENSE, version 3 (LGPL-3.0)": "LGPL-3.0-only",
    "GNU Lesser Public License": "LGPL-version-unspecified",
    "GPL v2": "GPL-2.0-only",
    "GPL-2.0-with-classpath-exception": "GPL-2.0-with-classpath-exception",
    "GENERAL PUBLIC LICENSE, version 3 (GPL-3.0)": "GPL-3.0-only",
    "Indiana University Extreme! Lab Software License": "Indiana-Extreme-Lab-custom",
    "ISC": "ISC",
    "LGPL-2.1-only": "LGPL-2.1-only",
    "LGPL-2.1-or-later": "LGPL-2.1-or-later",
    "LGPL-3.0-only": "LGPL-3.0-only",
    "MIT": "MIT",
    "MIT-0": "MIT-0",
    "MPL-1.1": "MPL-1.1",
    "MPL-2.0": "MPL-2.0",
    "Mozilla Public License Version 1.1": "MPL-1.1",
    "MulanPSL-2.0": "UNMAPPED:MulanPSL-2.0",
    "Oracle Free Use Terms and Conditions (FUTC)": "Oracle-FUTC-custom",
    "Podam runs under the MIT license": "MIT",
    "Public Domain": "Public-Domain",
    "Similar to Apache License but with the acknowledgment clause removed": "Apache-like-custom",
    "The GNU General Public License, v2 with Universal FOSS Exception, v1.0": "GPL-2.0-with-universal-foss-exception",
    "UPL-1.0": "UPL-1.0",
    "Unicode-3.0": "Unicode-3.0",
    "UnRar License": "UnRar-custom-restrictive",
    "WTFPL": "WTFPL",
}

# 归一标签 -> 基类别
TOKEN_CATEGORY = {
    "0BSD": "permissive_standard",
    "Apache-2.0": "permissive_standard",
    "Apache-like-custom": "strong_or_restrictive",
    "BSD-2-Clause": "permissive_standard",
    "BSD-3-Clause": "permissive_standard",
    "BSD-3-Clause-with-nuclear-disclaimer-custom": "strong_or_restrictive",
    "BSD-4-Clause": "permissive_standard",
    "BSD-version-unspecified": "permissive_standard",
    "Bouncy-Castle-MIT-like": "custom_or_non_spdx",
    "CC0-1.0": "permissive_standard",
    "CDDL-1.0": "weak_copyleft",
    "EPL-1.0": "weak_copyleft",
    "EPL-2.0": "weak_copyleft",
    "GPL-2.0-only": "weak_copyleft",
    "GPL-2.0-with-classpath-exception": "weak_copyleft",
    "GPL-2.0-with-universal-foss-exception": "strong_or_restrictive",
    "GPL-3.0-only": "weak_copyleft",
    "ISC": "permissive_standard",
    "Indiana-Extreme-Lab-custom": "strong_or_restrictive",
    "LGPL-2.1-only": "weak_copyleft",
    "LGPL-2.1-or-later": "weak_copyleft",
    "LGPL-3.0-only": "weak_copyleft",
    "LGPL-version-unspecified": "weak_copyleft",
    "MIT": "permissive_standard",
    "MIT-0": "permissive_standard",
    "MPL-1.1": "weak_copyleft",
    "MPL-2.0": "weak_copyleft",
    "Oracle-FUTC-custom": "strong_or_restrictive",
    "Public-Domain": "permissive_standard",
    "UNMAPPED:MulanPSL-2.0": "permissive_standard",
    "UPL-1.0": "permissive_standard",
    "UnRar-custom-restrictive": "strong_or_restrictive",
    "Unicode-3.0": "permissive_standard",
    "WTFPL": "permissive_standard",
    "expression:CDDL-1.0 OR GPL-2.0-with-classpath-exception": "expression_or_dual",
}

# 多标签组件的类别裁决优先级（数值越小越优先）
CATEGORY_PRECEDENCE = {
    "unmapped": 0,
    "missing": 0,
    "expression_or_dual": 1,
    "strong_or_restrictive": 2,
    "custom_or_non_spdx": 3,
    "weak_copyleft": 4,
    "permissive_standard": 5,
}
REVIEW_CATEGORIES = {"strong_or_restrictive", "custom_or_non_spdx", "expression_or_dual"}

# mvn dependency:tree 输出两种形态:
#   group:artifact:type:version:scope
#   group:artifact:type:classifier:version:scope   (带 classifier, 例如 netty native transport)
# classifier 可选; version 恒为倒数第二段, scope 恒为最后一段。
COORD_RE = re.compile(
    r"^(?P<group>[^:\s]+):(?P<artifact>[^:\s]+):(?P<type>[^:\s]+):(?:(?P<classifier>[^:\s]+):)?(?P<version>[^:\s]+):(?P<scope>[^:\s]+)$"
)
TREE_PREFIX_RE = re.compile(r"^[\s|+\\-]*")


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


def parse_runtime_coordinates(text: str) -> set[tuple[str, str, str]]:
    """解析 `mvn dependency:tree` 输出中的 group:artifact:version 坐标。"""
    coords: set[tuple[str, str, str]] = set()
    for line in text.split("\n"):
        stripped = line.strip()
        if not stripped.startswith("[INFO]"):
            continue
        candidate = TREE_PREFIX_RE.sub("", stripped[len("[INFO]"):].strip())
        match = COORD_RE.match(candidate)
        if match:
            coords.add((match.group("group"), match.group("artifact"), match.group("version")))
    return coords


def purl_type(purl: str) -> str:
    match = re.search(r"[?&]type=([^&]+)", purl or "")
    return match.group(1) if match else "jar"


def extract_raw_labels(component: dict) -> list[str]:
    """返回该组件每个 license 条目的原始标签（保持顺序）。"""
    labels: list[str] = []
    for entry in component.get("licenses") or []:
        if not isinstance(entry, dict):
            continue
        if "expression" in entry and str(entry.get("expression") or "").strip():
            labels.append(str(entry["expression"]).strip())
            continue
        license_obj = entry.get("license") if isinstance(entry.get("license"), dict) else {}
        for key in ("id", "name"):
            value = str(license_obj.get(key) or "").strip()
            if value:
                labels.append(value)
                break
    return labels


def normalise_label(raw: str) -> str:
    if raw.startswith("(") and raw.endswith(")") and " OR " in raw:
        return "expression:" + raw.strip("()").strip()
    return RAW_TO_TOKEN.get(raw, f"UNMAPPED:{raw}")


def resolve_category(tokens: list[str]) -> str:
    categories = []
    for token in tokens:
        categories.append(TOKEN_CATEGORY.get(token, "unmapped"))
    return min(categories, key=lambda item: CATEGORY_PRECEDENCE.get(item, 0))


def build_inventory(
    bom: dict,
    runtime_coords: set[tuple[str, str, str]],
    internal_prefix: str = DEFAULT_INTERNAL_PREFIX,
) -> tuple[list[dict], list[str], list[str], Counter]:
    components: list[dict] = []
    internal_missing: list[str] = []
    external_missing: list[str] = []
    external_raw_counts: Counter = Counter()

    for entry in bom.get("components") or []:
        if not isinstance(entry, dict):
            continue
        purl = str(entry.get("purl") or "").strip()
        group = str(entry.get("group") or "").strip()
        artifact = str(entry.get("name") or "").strip()
        version = str(entry.get("version") or "").strip()
        is_internal = group.lower().startswith(internal_prefix.lower())
        raw_labels = extract_raw_labels(entry)
        tokens: list[str] = []
        for raw in raw_labels:
            token = normalise_label(raw)
            if token not in tokens:
                tokens.append(token)
        if not tokens:
            tokens = ["MISSING"]
        category = resolve_category(tokens)
        if "MISSING" in tokens:
            category = "missing"
            (internal_missing if is_internal else external_missing).append(purl)
        if not is_internal:
            external_raw_counts["|".join(raw_labels)] += 1

        components.append({
            "purl": purl,
            "group": group,
            "artifact": artifact,
            "version": version,
            "type": purl_type(purl),
            "is_internal": is_internal,
            "server_runtime_scope": (group, artifact, version) in runtime_coords,
            "raw_license_label": "|".join(raw_labels),
            "normalized_licenses": tokens,
            "license_category": category,
        })

    return components, internal_missing, external_missing, external_raw_counts


def build_report(
    bom_path: Path,
    runtime_tree_path: Path,
    internal_prefix: str = DEFAULT_INTERNAL_PREFIX,
    bom_label: str | None = None,
    tree_label: str | None = None,
    runtime_command: str = DEFAULT_RUNTIME_COMMAND,
    change_note: str = "",
    generated_at: str | None = None,
) -> dict:
    bom = load_json(bom_path, "CycloneDX BOM")
    if bom.get("bomFormat") != "CycloneDX":
        raise InputError(f"BOM 必须是 CycloneDX 格式: {bom_path}")
    if not runtime_tree_path.is_file():
        raise InputError(f"runtime dependency tree 不存在: {runtime_tree_path}")
    runtime_tree_text = runtime_tree_path.read_text(encoding="utf-8", errors="replace")
    runtime_coords = parse_runtime_coordinates(runtime_tree_text)

    components, internal_missing, external_missing, external_raw_counts = build_inventory(
        bom, runtime_coords, internal_prefix,
    )

    internal_count = sum(1 for item in components if item["is_internal"])
    external_count = len(components) - internal_count
    runtime_items = [item for item in components if item["server_runtime_scope"]]

    def counts(items: list[dict]) -> dict:
        return dict(sorted(Counter(item["license_category"] for item in items).items()))

    review_required = sum(
        1 for item in components
        if not item["is_internal"] and item["license_category"] in REVIEW_CATEGORIES
    )

    return {
        "schema_version": 1,
        "evidence_type": "maven_license_inventory",
        "generated_at_local": generated_at or datetime.now().astimezone().isoformat(timespec="seconds"),
        "source": {
            "bom_path": bom_label or str(bom_path),
            "bom_sha256": sha256_file(bom_path),
            "runtime_tree_path": tree_label or str(runtime_tree_path),
            "runtime_tree_sha256": sha256_file(runtime_tree_path),
            "runtime_command": runtime_command,
        },
        "method": {
            "license_extraction": "CycloneDX license.id / license.name / expression fields",
            "normalization": (
                "raw labels mapped through an explicit reviewed table "
                f"({len(RAW_TO_TOKEN)} raw labels / {len(TOKEN_CATEGORY)} tokens); "
                "unknown labels become UNMAPPED and are denied by the policy gate"
            ),
            "scope_detection": "coordinates parsed from zhicloud-server runtime dependency tree",
            "disclaimer": "This is engineering evidence for governance, not a legal opinion.",
        },
        "summary": {
            "total_components": len(components),
            "internal_components": internal_count,
            "external_components": external_count,
            "external_missing_license": len(external_missing),
            "internal_missing_license": len(internal_missing),
            "external_distinct_raw_labels": len(external_raw_counts),
            "server_runtime_scope_components": len(runtime_items),
            "category_counts_all": counts(components),
            "category_counts_external": counts([item for item in components if not item["is_internal"]]),
            "category_counts_server_runtime_scope": counts(runtime_items),
            "review_required_external_components": review_required,
            "change_note": change_note,
        },
        "external_missing_license": external_missing,
        "internal_missing_license": internal_missing,
        "raw_label_counts_external": dict(sorted(external_raw_counts.items())),
        "components": components,
    }


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="从 CycloneDX BOM 生成 Maven 许可证治理清单")
    parser.add_argument("--bom", type=Path, required=True, help="CycloneDX aggregate BOM JSON")
    parser.add_argument("--runtime-tree", type=Path, required=True, help="mvn dependency:tree 输出日志")
    parser.add_argument("--output", type=Path, required=True, help="inventory JSON 输出路径")
    parser.add_argument("--internal-prefix", default=DEFAULT_INTERNAL_PREFIX)
    parser.add_argument("--bom-label", help="写入 source.bom_path 的标签（默认使用传入路径）")
    parser.add_argument("--tree-label", help="写入 source.runtime_tree_path 的标签")
    parser.add_argument("--runtime-command", default=DEFAULT_RUNTIME_COMMAND)
    parser.add_argument("--change-note", default="")
    args = parser.parse_args(argv)

    try:
        report = build_report(
            args.bom,
            args.runtime_tree,
            internal_prefix=args.internal_prefix,
            bom_label=args.bom_label,
            tree_label=args.tree_label,
            runtime_command=args.runtime_command,
            change_note=args.change_note,
        )
    except InputError as exc:
        print(f"ERROR: {exc}", file=sys.stderr)
        return 2

    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(
        json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8", newline="\n",
    )
    summary = report["summary"]
    print(
        f"wrote {args.output}: components={summary['total_components']} "
        f"internal={summary['internal_components']} external={summary['external_components']} "
        f"runtime={summary['server_runtime_scope_components']} "
        f"missing={summary['external_missing_license'] + summary['internal_missing_license']} "
        f"review_required={summary['review_required_external_components']}"
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())