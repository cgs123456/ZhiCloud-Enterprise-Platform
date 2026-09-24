#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""安全基线静态扫描（Phase 1 / P0-2）。

只做**文本静态扫描**，回答不了运行时行为：
  * 不解析 Spring profile 的最终生效值（环境变量可能覆盖默认值）；
  * 不扫依赖 jar，不做 CVE/依赖漏洞扫描；
  * 不做渗透测试、不做密钥泄露的历史 git 考古。
因此本扫描的结果只能作为「待人工确认清单」，不能直接当作"已修复"。

规则（R=风险规则，命中即需要人工确认；C=正向控制，缺失才算问题）：
  R1 JDBC_TLS_DISABLED        JDBC URL 里 useSSL=false
  R2 JDBC_PUBLIC_KEY_RETRIEVE allowPublicKeyRetrieval=true
  R3 DEFAULT_CREDENTIAL       ${ENV:默认值} 形式的默认口令/密钥
                              （${ENV:?required} 必填占位、${A:${B}} 变量回退链
                               都不是硬编码默认口令，不计入）
  R4 PLACEHOLDER_SECRET        CHANGE_ME 占位符
  R5 EMPTY_SECRET_VALUE        密钥配置项为空字符串
  R6 LITERAL_WEAK_SECRET       明文弱口令字面量（xx / 123456 / admin / root / test …）
  R7 TEST_ACCOUNT_SEED         SQL 里预置 admin/test 等测试账号
  R8 DEMO_MODE_ENABLED         demo: true

  C1 PROD_DOCS_DISABLED        生产 profile 关闭 springdoc api-docs / swagger-ui / knife4j
  C2 PROD_SECRET_REQUIRED      生产 profile 的 MYBATIS_ENCRYPTOR_PASSWORD 为必填占位 ${x:?required}
"""
from __future__ import annotations

import argparse
import json
import re
import subprocess
import sys
from datetime import datetime
from pathlib import Path
from typing import Any

SCAN_SUFFIXES = {".yaml", ".yml", ".properties", ".sql", ".env", ".tpl", ".sh", ".xml"}
SKIP_DIR_PARTS = {".git", "target", "node_modules", "dist", "build", ".idea", ".vscode",
                  "__pycache__"}

R1 = re.compile(r"useSSL\s*=\s*false", re.IGNORECASE)
R2 = re.compile(r"allowPublicKeyRetrieval\s*=\s*true", re.IGNORECASE)
R3 = re.compile(r"(password|passwd|secret|token|api[-_]?key|access[-_]?key)\s*[:=]\s*"
                r"\$\{[A-Za-z0-9_]+\s*:\s*(?!\?)(?!\$\{)([^}]+)\}", re.IGNORECASE)
R4 = re.compile(r"CHANGE_ME", re.IGNORECASE)
R5 = re.compile(r"^\s*[A-Za-z0-9_]*(PASSWORD|SECRET|KEY)[A-Za-z0-9_]*\s*:\s*\"\"\s*$",
                re.IGNORECASE)
R6 = re.compile(r"(client-secret|secret|password|passwd)\s*[:=]\s*"
                r"[\"']?(xx|xxx|123456|1234|admin|root|test|changeme|password)[\"']?\s*$",
                re.IGNORECASE)
R7 = re.compile(r"INSERT\s+INTO\s+`?system_users`?.*?"
                r"'(admin|test|zhicloud|yuanma|admin\d+)'", re.IGNORECASE)
R8 = re.compile(r"^\s*demo\s*:\s*true\s*$", re.IGNORECASE)

RULES = {
    "R1": ("JDBC_TLS_DISABLED", R1),
    "R2": ("JDBC_PUBLIC_KEY_RETRIEVE", R2),
    "R3": ("DEFAULT_CREDENTIAL", R3),
    "R4": ("PLACEHOLDER_SECRET", R4),
    "R5": ("EMPTY_SECRET_VALUE", R5),
    "R6": ("LITERAL_WEAK_SECRET", R6),
    "R7": ("TEST_ACCOUNT_SEED", R7),
    "R8": ("DEMO_MODE_ENABLED", R8),
}

# 只有这些规则区分"生产路径"与"开发路径"
# zhicloud_platform.sql 是官方安装路径（见 evidence/production/schema-audit.json），
# 其中的预置账号会直接进入生产库，因此按生产路径计。
PROD_PATH_TOKENS = ("prod", "production", "staging", "values.yaml", "k8s", "helm",
                    "zhicloud_platform.sql")


def severity_for(rule_id: str, rel_path: str) -> str:
    lowered = rel_path.lower()
    is_prod = any(tok in lowered for tok in PROD_PATH_TOKENS)
    if rule_id in {"R1", "R2", "R3", "R5", "R6", "R7"} and is_prod:
        return "P0"
    if rule_id == "R7":
        # sql/ 下的安装脚本会直接把账号写进目标库（8 种方言各一份）
        return "P0" if lowered.startswith("sql/") else "P1"
    if rule_id in {"R4", "R8"}:
        return "P1" if is_prod else "P2"
    if rule_id in {"R1", "R2", "R3", "R5", "R6"}:
        return "P1"
    return "P2"


def iter_files(repo: Path):
    for path in repo.rglob("*"):
        if not path.is_file():
            continue
        if path.suffix.lower() not in SCAN_SUFFIXES:
            continue
        rel = path.relative_to(repo)
        if any(part in SKIP_DIR_PARTS for part in rel.parts):
            continue
        yield path, rel


def scan_repo(repo: Path) -> dict[str, Any]:
    findings: list[dict[str, Any]] = []
    scanned = 0
    for path, rel in iter_files(repo):
        scanned += 1
        try:
            text = path.read_text(encoding="utf-8", errors="replace")
        except OSError:
            continue
        rel_str = rel.as_posix()
        for line_no, line in enumerate(text.splitlines(), start=1):
            for rule_id, (name, pattern) in RULES.items():
                m = pattern.search(line)
                if not m:
                    continue
                findings.append({
                    "rule": rule_id,
                    "code": name,
                    "severity": severity_for(rule_id, rel_str),
                    "file": rel_str,
                    "line": line_no,
                    "evidence": line.strip()[:200],
                })
    return {"scanned_files": scanned, "findings": findings}


CONTROLS = [
    ("C1", "PROD_DOCS_DISABLED", "application-prod.yaml",
     [r"api-docs:\s*\n\s*enabled:\s*false",
      r"swagger-ui:\s*\n\s*enabled:\s*false",
      r"knife4j:\s*\n\s*enable:\s*false"]),
    ("C2", "PROD_SECRET_REQUIRED", "application-prod.yaml",
     [r"password:\s*\$\{MYBATIS_ENCRYPTOR_PASSWORD:\?required\}"]),
    ("C3", "HELM_SECRET_GATE", "_helpers.tpl",
     [r"zhicloud\.requiredSecret"]),
    ("C4", "HELM_SECRET_WIRED", "secret.yaml",
     [r"include \"zhicloud\.requiredSecret\""]),
]


def run_controls(repo: Path) -> list[dict[str, Any]]:
    out: list[dict[str, Any]] = []
    for cid, name, target, patterns in CONTROLS:
        matches = sorted(repo.rglob(target))
        matches = [p for p in matches
                   if not any(part in SKIP_DIR_PARTS
                              for part in p.relative_to(repo).parts)]
        if not matches:
            out.append({"control": cid, "code": name, "status": "file_missing",
                        "file": target, "missing_patterns": patterns})
            continue
        path = matches[0]
        text = path.read_text(encoding="utf-8", errors="replace")
        missing = [p for p in patterns if not re.search(p, text)]
        out.append({
            "control": cid, "code": name,
            "status": "passed" if not missing else "failed",
            "file": path.relative_to(repo).as_posix(),
            "missing_patterns": missing,
        })
    return out


def git_head(repo: Path) -> str:
    try:
        res = subprocess.run(["git", "-C", str(repo), "rev-parse", "HEAD"],
                             capture_output=True, text=True, timeout=30)
        return res.stdout.strip()
    except Exception:
        return ""


def build_report(repo: Path, fail_on: str = "P0") -> dict[str, Any]:
    scan = scan_repo(repo)
    findings = scan["findings"]
    controls = run_controls(repo)
    by_rule: dict[str, int] = {}
    by_severity: dict[str, int] = {}
    for f in findings:
        by_rule[f["rule"]] = by_rule.get(f["rule"], 0) + 1
        by_severity[f["severity"]] = by_severity.get(f["severity"], 0) + 1
    failed_controls = [c for c in controls if c["status"] != "passed"]
    blocking = by_severity.get("P0", 0) + len(failed_controls)
    return {
        "generated_at": datetime.now().astimezone().isoformat(timespec="seconds"),
        "tool": "security_baseline_scan.py",
        "evidence_class": "static-text scan; no runtime/profile resolution, no dependency CVE scan",
        "repo_root": str(repo),
        "commit": git_head(repo),
        "scanned_files": scan["scanned_files"],
        "rules": {rid: name for rid, (name, _) in RULES.items()},
        "finding_count": len(findings),
        "findings_by_rule": by_rule,
        "findings_by_severity": by_severity,
        "controls": controls,
        "controls_failed": len(failed_controls),
        "findings": findings,
        "status": "failed" if blocking else "passed",
        "fail_on": fail_on,
        "disclaimers": [
            "静态文本扫描：环境变量/Secret 注入可能覆盖默认值，命中项需人工确认。",
            "不覆盖依赖漏洞（CVE）、渗透测试、历史 git 密钥泄露考古。",
            "本扫描通过不等于安全基线已达标；P0-2 仍需逐项人工处置与复测。",
        ],
    }


def main(argv: list[str] | None = None) -> int:
    ap = argparse.ArgumentParser(description="安全基线静态扫描")
    ap.add_argument("--repo", type=Path, default=Path(__file__).resolve().parents[2])
    ap.add_argument("--output", type=Path)
    ap.add_argument("--summary", action="store_true", help="只打印汇总")
    args = ap.parse_args(argv)

    report = build_report(args.repo.resolve())
    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(json.dumps(report, ensure_ascii=False, indent=2,
                                          sort_keys=True) + "\n",
                               encoding="utf-8", newline="\n")
    if not args.summary:
        for f in report["findings"][:200]:
            print(f"[{f['severity']}][{f['rule']}] {f['file']}:{f['line']} :: {f['evidence'][:120]}")
    print(f"扫描文件 {report['scanned_files']}；命中 {report['finding_count']} "
          f"（{report['findings_by_severity']}）；正向控制失败 {report['controls_failed']}")
    print(f"status={report['status']}")
    return 1 if report["status"] == "failed" else 0


if __name__ == "__main__":
    raise SystemExit(main())
