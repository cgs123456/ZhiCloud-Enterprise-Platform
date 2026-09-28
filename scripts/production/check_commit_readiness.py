#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""入仓前置守卫：CI 引用的脚本、策略与门禁证据必须真的在 Git 里。

存在的理由（真实踩坑，勿删）：
`.github/workflows/maven.yml` 里接了十几道门禁，但其中若干脚本与策略文件从未 `git add`。
一旦提交，CI 会以"python3: can't open file"失败 —— 表面看是"门禁按设计变红"，
实际是接线缺失。这两种失败后果完全不同：前者是待法务处置，后者是根本没跑起来。
若不做区分，团队会习惯性忽略红灯，门禁就失效了。

同时 `.gitignore` 里有 `*.log`，而证据目录里的 pytest / Maven 日志正是靠 `*.log` 命名的，
它们被静默排除，文档引用会变成悬空链接。这类文件必须显式 `git add -f` 或加白名单例外。

本守卫只检查"是否被 Git 跟踪 / 是否被忽略"，不执行任何 add / commit。
"""
from __future__ import annotations

import argparse
import json
import subprocess
import sys
from datetime import datetime
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parents[2]

# CI 工作流里被直接调用的脚本与策略文件；新增门禁时必须同步登记到这里。
REQUIRED_TRACKED = [
    ".github/workflows/maven.yml",
    "script/security/security-check.sh",
    "scripts/check_error_codes.py",
    "scripts/check_missing_preauthorize.py",
    "scripts/check_electronic_signature.py",
    "scripts/check_bare_throws.py",
    "scripts/scan_tx.py",
    "scripts/production/check_migration_contract.py",
    "scripts/production/check_duplicate_ddl.py",
    "scripts/production/duplicate_ddl_allowlist.json",
    "scripts/production/migration-contract.json",
    "scripts/production/generate_license_inventory.py",
    "scripts/production/check_license_policy.py",
    "scripts/production/license-policy.json",
    "scripts/production/build_license_disposition_register.py",
    "scripts/production/security_baseline_suppressions.py",
    "scripts/production/security-baseline-suppressions.json",
]

# 需要整棵入仓的文档树：全部审计证据都在里面，整树未跟踪等于所有文档链接悬空。
REQUIRED_TRACKED_TREES = ["docs/enterprise-platform"]
# 仓库根临时脚本残留：历史上出现过，与交付物无关，入仓前需人工裁决删除或移出。
TEMP_RESIDUE_GLOB = "_tmp_*.py"

# 证据目录：文档会按路径引用这些文件，被 .gitignore 静默排除就会变成悬空引用。
EVIDENCE_DIR = Path("docs") / "enterprise-platform" / "evidence" / "production"

FLAGS = {
    "production_ready": False,
    "gate3_passed": False,
    "t107_status": "OPEN",
    "independent_review": False,
}


class GitError(Exception):
    """Git 不可用或不在仓库中，对应退出码 2。"""


def run_git(repo: Path, args: list[str]) -> subprocess.CompletedProcess:
    return subprocess.run(
        ["git", *args], cwd=repo, capture_output=True, text=True,
        encoding="utf-8", errors="replace",
    )


def ensure_repo(repo: Path) -> None:
    proc = run_git(repo, ["rev-parse", "--is-inside-work-tree"])
    if proc.returncode != 0 or proc.stdout.strip() != "true":
        raise GitError(f"不在 Git 工作树中: {repo}")


def is_ignored(repo: Path, rel: str) -> bool:
    proc = run_git(repo, ["check-ignore", "-q", rel])
    return proc.returncode == 0


def is_tracked(repo: Path, rel: str) -> bool:
    proc = run_git(repo, ["ls-files", "--error-unmatch", rel])
    return proc.returncode == 0


def check_required(repo: Path, required: list[str]) -> list[dict]:
    findings: list[dict] = []
    for rel in required:
        path = repo / rel
        if not path.exists():
            findings.append({"path": rel, "status": "missing", "action": "文件不存在，先创建再入仓"})
            continue
        if is_ignored(repo, rel):
            findings.append({
                "path": rel, "status": "ignored",
                "action": "被 .gitignore 排除，需加白名单例外或 git add -f",
            })
            continue
        if not is_tracked(repo, rel):
            findings.append({"path": rel, "status": "untracked", "action": "git add 后才能被 CI 使用"})
            continue
        findings.append({"path": rel, "status": "tracked", "action": ""})
    return findings


def check_evidence(repo: Path) -> list[dict]:
    directory = repo / EVIDENCE_DIR
    findings: list[dict] = []
    if not directory.is_dir():
        return findings
    for path in sorted(directory.rglob("*.log")):
        rel = path.relative_to(repo).as_posix()
        if is_ignored(repo, rel):
            findings.append({
                "path": rel, "status": "ignored_evidence",
                "action": "证据日志被 *.log 规则排除，提交需 git add -f 或加例外",
            })
    return findings


def check_untracked_trees(repo: Path, trees: list[str]) -> list[dict]:
    """整棵目录未入仓 -> 告警。

    不判阻断：入仓需要授权，守卫无权代替人做决定；但必须被看见，
    否则文档里引用的证据路径在提交后全部悬空。
    """
    warnings: list[dict] = []
    for tree in trees:
        directory = repo / tree
        if not directory.is_dir():
            continue
        proc = run_git(repo, ["ls-files", tree])
        if proc.returncode != 0:
            continue
        if not proc.stdout.strip():
            warnings.append({
                "path": tree,
                "status": "untracked_tree",
                "action": "整棵目录未被 Git 跟踪，提交后文档引用的证据路径会全部悬空；需授权 git add",
            })
    return warnings


def check_temp_residue(repo: Path) -> list[dict]:
    """仓库根临时脚本残留 -> 告警（不阻断）。"""
    warnings: list[dict] = []
    for path in sorted(repo.glob(TEMP_RESIDUE_GLOB)):
        if not path.is_file():
            continue
        warnings.append({
            "path": path.relative_to(repo).as_posix(),
            "status": "temp_residue",
            "action": "仓库根临时脚本残留，入仓前需人工确认删除或移出",
        })
    return warnings


def build_report(repo: Path, required: list[str]) -> dict:
    ensure_repo(repo)
    required_findings = check_required(repo, required)
    evidence_findings = check_evidence(repo)
    warnings = check_untracked_trees(repo, REQUIRED_TRACKED_TREES) \
        + check_temp_residue(repo)
    blocking = [f for f in required_findings + evidence_findings if f["status"] != "tracked"]
    counts = {
        "required_total": len(required_findings),
        "required_tracked": sum(1 for f in required_findings if f["status"] == "tracked"),
        "required_untracked": sum(1 for f in required_findings if f["status"] == "untracked"),
        "required_ignored": sum(1 for f in required_findings if f["status"] == "ignored"),
        "required_missing": sum(1 for f in required_findings if f["status"] == "missing"),
        "ignored_evidence_logs": len(evidence_findings),
        "untracked_trees": sum(1 for w in warnings if w["status"] == "untracked_tree"),
        "temp_residue": sum(1 for w in warnings if w["status"] == "temp_residue"),
        "warnings": len(warnings),
        "blocking": len(blocking),
    }
    return {
        "evidence_type": "commit_readiness_check",
        "schema_version": 1,
        "tool": "check_commit_readiness.py",
        "generated_at_local": datetime.now().astimezone().isoformat(timespec="seconds"),
        "repo": str(repo),
        "decision": "pass" if not blocking else "fail",
        "counts": counts,
        "required": required_findings,
        "ignored_evidence": evidence_findings,
        "warnings": warnings,
        "blocking": blocking,
        "flags": dict(FLAGS),
        "limits": [
            "本脚本只检查 Git 跟踪状态，不执行 add / commit，也不判断文件内容是否正确。",
            "被 .gitignore 排除的证据日志不会自动入仓，必须显式 git add -f 或添加例外规则。",
            "warnings 为提示项：不参与 pass/fail 判定，但需要人工裁决（授权 git add 或清理残留）。",
        ],
    }


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="入仓前置守卫：CI 引用的文件必须已被 Git 跟踪")
    parser.add_argument("--repo", type=Path, default=REPO_ROOT, help="仓库根目录")
    parser.add_argument("--output", type=Path, help="结果 JSON 输出路径")
    parser.add_argument("--quiet", action="store_true", help="只打印结论行")
    parser.add_argument("--required", action="append", default=[], metavar="PATH",
                        help="覆盖默认的必查清单（可重复）；用于测试与临时校验")
    args = parser.parse_args(argv)
    repo = args.repo.resolve()
    required = args.required or REQUIRED_TRACKED

    try:
        report = build_report(repo, required)
    except GitError as exc:
        print(f"ERROR: {exc}", file=sys.stderr)
        return 2

    payload = json.dumps(report, ensure_ascii=False, indent=2, sort_keys=True)
    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(payload + "\n", encoding="utf-8", newline="\n")
        print(f"wrote {args.output}")
    if not args.quiet:
        for item in report["blocking"]:
            print(f"BLOCK [{item['status']}] {item['path']} —— {item['action']}")
        for item in report["warnings"]:
            print(f"WARN  [{item['status']}] {item['path']} —— {item['action']}")
    counts = report["counts"]
    print(
        f"{report['decision'].upper()}: required={counts['required_total']} "
        f"tracked={counts['required_tracked']} untracked={counts['required_untracked']} "
        f"ignored={counts['required_ignored']} missing={counts['required_missing']} "
        f"ignored_evidence_logs={counts['ignored_evidence_logs']} "
        f"warnings={counts['warnings']}"
    )
    return 0 if report["decision"] == "pass" else 1


if __name__ == "__main__":
    raise SystemExit(main())
