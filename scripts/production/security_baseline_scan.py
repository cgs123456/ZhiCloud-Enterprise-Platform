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
  R7 LITERAL_USER_ACCOUNT_SEED SQL 里以明文字面量预置用户账号
  R8 DEMO_MODE_ENABLED         demo: true
  R9 JDBC_TLS_IDENTITY_NOT_VERIFIED
                              JDBC TLS 仅加密或只校验证书链，未校验主机名
                              （verifyServerCertificate=false / sslMode=REQUIRED、
                              PREFERRED、VERIFY_CA）
  R10 SQL_LITERAL_SEED_CREDENTIAL
                              SQL 种子数据中为 OAuth2 / 第三方登录客户端写入
                              明文字面量 secret；生产迁移和安装脚本均属生产路径

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

# 抑制模块与本文件同目录。作为脚本直接运行时脚本目录已在 sys.path 上，
# 被当作模块导入时不一定，因此显式补一次，避免接线静默失败。
sys.path.insert(0, str(Path(__file__).resolve().parent))
from security_baseline_suppressions import (  # noqa: E402
    DEFAULT_MAX_EXPIRY_DAYS,
    apply_suppressions,
    load_suppression_file,
    status_for,
    suppression_section,
)

SCAN_SUFFIXES = {".yaml", ".yml", ".properties", ".sql", ".env", ".tpl", ".sh", ".xml"}
SKIP_DIR_PARTS = {".git", "target", "node_modules", "dist", "build", ".idea", ".vscode",
                  "__pycache__"}
# 渲染证据可能包含生产 values 的完整展开结果；它们是扫描产物而不是待部署源配置。
# 若再次纳入静态扫描，会把同一问题重复计数，也会让 evidence 目录自污染。
SKIP_REL_PREFIXES = ("docs/enterprise-platform/evidence/",)

R1 = re.compile(r"(?:useSSL\s*=\s*false|sslMode\s*=\s*DISABLED\b)", re.IGNORECASE)
R2 = re.compile(r"allowPublicKeyRetrieval\s*=\s*true", re.IGNORECASE)
R3 = re.compile(r"(password|passwd|secret|token|api[-_]?key|access[-_]?key)\s*[:=]\s*"
                r"\$\{[A-Za-z0-9_]+\s*:\s*(?!\?)(?!\$\{)([^}]+)\}", re.IGNORECASE)
R4 = re.compile(r"CHANGE_ME", re.IGNORECASE)
R5 = re.compile(r"^\s*[A-Za-z0-9_]*(PASSWORD|SECRET|KEY)[A-Za-z0-9_]*\s*:\s*\"\"\s*$",
                re.IGNORECASE)
# 这些字段保存的是 Secret/Key 的“引用名”，不是凭据值。其非空约束由 Helm
# required 控制（C13-C18）；把它们误报为 EMPTY_SECRET_VALUE 会掩盖真实凭据问题。
SECRET_REFERENCE_NAME_RE = re.compile(
    r"^\s*[A-Za-z0-9_]*(?:SECRETNAME|SECRETKEY|EXISTINGSECRET|"
    r"EXISTINGSECRETREVISION|EXISTINGTRUSTSTOREREVISION|PASSWORDKEY|FILEKEY)"
    r"\s*:\s*\"\"\s*$",
    re.IGNORECASE,
)
R6 = re.compile(r"(client-secret|secret|password|passwd)\s*[:=]\s*"
                r"[\"']?(xx|xxx|123456|1234|admin|root|test|changeme|password)[\"']?\s*$",
                re.IGNORECASE)
R7 = re.compile(
    r"INSERT\s+(?:IGNORE\s+)?INTO\s+`?system_users`?.*?"
    r"VALUES\s*\(\s*\d+\s*,\s*N?'(?!__ZHICLOUD_)[^']+'",
    re.IGNORECASE)
R10 = re.compile(
    r"INSERT\s+(?:IGNORE\s+)?INTO\s+`?system_oauth2_client`?"
    r".*?VALUES\s*\(\s*\d+\s*,\s*N?'[^']*'\s*,\s*N?'[^']*'"
    r"|INSERT\s+(?:IGNORE\s+)?INTO\s+`?system_social_client`?"
    r".*?VALUES\s*\(\s*\d+\s*,\s*N?'[^']*'\s*,\s*\d+\s*,\s*\d+"
    r"\s*,\s*N?'[^']*'\s*,\s*N?'[^']*'",
    re.IGNORECASE)
R8 = re.compile(r"^\s*demo\s*:\s*true\s*$", re.IGNORECASE)
R9 = re.compile(
    r"(?:verifyServerCertificate\s*=\s*false|"
    r"sslMode\s*=\s*(?:REQUIRED|PREFERRED|VERIFY_CA)\b)",
    re.IGNORECASE)

RULES = {
    "R1": ("JDBC_TLS_DISABLED", R1),
    "R2": ("JDBC_PUBLIC_KEY_RETRIEVE", R2),
    "R3": ("DEFAULT_CREDENTIAL", R3),
    "R4": ("PLACEHOLDER_SECRET", R4),
    "R5": ("EMPTY_SECRET_VALUE", R5),
    "R6": ("LITERAL_WEAK_SECRET", R6),
    "R7": ("LITERAL_USER_ACCOUNT_SEED", R7),
    "R8": ("DEMO_MODE_ENABLED", R8),
    "R9": ("JDBC_TLS_IDENTITY_NOT_VERIFIED", R9),
    "R10": ("SQL_LITERAL_SEED_CREDENTIAL", R10),
}

# 只有这些规则区分"生产路径"与"开发路径"
# zhicloud_platform.sql 是官方安装路径（见 evidence/production/schema-audit.json），
# 其中的预置账号会直接进入生产库，因此按生产路径计。
PROD_PATH_TOKENS = ("prod", "production", "staging", "values.yaml", "k8s", "helm",
                    "zhicloud_platform.sql", "/src/main/resources/db/migration/")
PROD_FLYWAY_ROOT = "src/main/resources/db/migration/"
# 明确排除开发/本地样例，避免 deploy/helm/values-dev.yaml 因路径含 helm 被误判为生产。
NON_PROD_PATH_TOKENS = ("values-dev.yaml", "values-local.yaml", "application-dev.yaml",
                        "application-local.yaml", ".env.example", "docker-compose")


def severity_for(rule_id: str, rel_path: str) -> str:
    lowered = rel_path.lower()
    is_prod = ((any(tok in lowered for tok in PROD_PATH_TOKENS)
                or lowered.startswith(PROD_FLYWAY_ROOT))
               and not any(tok in lowered for tok in NON_PROD_PATH_TOKENS))
    if rule_id in {"R1", "R2", "R3", "R5", "R6", "R7", "R9", "R10"} and is_prod:
        return "P0"
    if rule_id in {"R7", "R10"}:
        # sql/ 下的安装脚本会直接把账号或客户端凭据写进目标库（各 SQL 方言一份）
        return "P0" if lowered.startswith("sql/") else "P1"
    if rule_id in {"R4", "R8"}:
        return "P1" if is_prod else "P2"
    if rule_id in {"R1", "R2", "R3", "R5", "R6", "R9", "R10"}:
        return "P1"
    return "P2"


def is_skipped_relative_path(rel: Path) -> bool:
    rel_str = rel.as_posix()
    return (any(part in SKIP_DIR_PARTS for part in rel.parts)
            or rel_str.startswith(SKIP_REL_PREFIXES))


def iter_files(repo: Path):
    for path in repo.rglob("*"):
        if not path.is_file():
            continue
        if path.suffix.lower() not in SCAN_SUFFIXES:
            continue
        rel = path.relative_to(repo)
        if is_skipped_relative_path(rel):
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
            # R9 是 JDBC 参数规则；纯注释中的示例/说明不是运行配置，
            # 不应被当成生产连接风险。其他规则仍保留全文扫描。
            r9_line = "" if line.lstrip().startswith("#") else line
            for rule_id, (name, pattern) in RULES.items():
                candidate = r9_line if rule_id == "R9" else line
                if rule_id == "R5" and SECRET_REFERENCE_NAME_RE.match(candidate):
                    continue
                m = pattern.search(candidate)
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
    # AI-RAG 使用独立 PostgreSQL 数据源，必须把应用实际读取的密码键
    # ZHICLOUD_AIRAG_DATASOURCE_PASSWORD 注入 Secret；仅创建 POSTGRES_PASSWORD
    # 并不能被 Spring Boot relaxed binding 识别。
    ("C5", "AIRAG_SECRET_WIRED", "deploy/helm/zhicloud/templates/secret.yaml",
     [r"ZHICLOUD_AIRAG_DATASOURCE_PASSWORD",
      r"zhicloud\.requiredSecret"]),
    # 生产启动必须依赖受审计的显式 DDL，而不是 PgVectorStore 自动建表。
    ("C6", "AIRAG_SCHEMA_MANUAL", "sql/postgresql/airag_pgvector.sql",
     [r"CREATE\s+TABLE\s+IF\s+NOT\s+EXISTS\s+airag_vector_store",
      r"CREATE\s+INDEX\s+IF\s+NOT\s+EXISTS\s+airag_vector_store_index"]),
    # 生产 Helm 覆盖与原生 K8s 清单都显式关闭应用启动 DDL。
    ("C7", "AIRAG_SCHEMA_DDL_DISABLED_HELM", "deploy/helm/zhicloud/values-prod.yaml",
     [r"ZHICLOUD_AIRAG_INITIALIZE_SCHEMA:\s*\"false\""]),
    ("C8", "AIRAG_SCHEMA_DDL_DISABLED_K8S", "k8s/zhicloud-server.yaml",
     [r"ZHICLOUD_AIRAG_INITIALIZE_SCHEMA:\s*\"false\""]),
    # PostgreSQL 生产连接必须做主机名与证书链校验，不能只开启加密。
    ("C9", "AIRAG_TLS_VERIFY_HELM", "deploy/helm/zhicloud/values-prod.yaml",
     [r"ZHICLOUD_AIRAG_DATASOURCE_URL:.*sslmode=verify-full"]),
    ("C10", "AIRAG_TLS_VERIFY_K8S", "k8s/zhicloud-server.yaml",
     [r"ZHICLOUD_AIRAG_DATASOURCE_URL:.*sslmode=verify-full"]),
    # MySQL Connector/J 9.x：生产必须校验证书链和主机名。REQUIRED/PREFERRED
    # 只保证加密，VERIFY_CA 只校验证书链，三者都不能替代 VERIFY_IDENTITY。
    ("C11", "MYSQL_TLS_VERIFY_HELM", "deploy/helm/zhicloud/values-prod.yaml",
     [r"MASTER_DATASOURCE_URL:.*sslMode=VERIFY_IDENTITY",
      r"SLAVE_DATASOURCE_URL:.*sslMode=VERIFY_IDENTITY"]),
    ("C12", "MYSQL_TLS_VERIFY_K8S", "k8s/zhicloud-server.yaml",
     [r"MASTER_DATASOURCE_URL:.*sslMode=VERIFY_IDENTITY",
      r"SLAVE_DATASOURCE_URL:.*sslMode=VERIFY_IDENTITY"]),
    # 证书校验依赖容器内可用的 truststore。Helm Deployment 必须从外部 Secret
    # 挂载，且 truststore 密码通过 secretKeyRef 注入，不能写进 ConfigMap。
    ("C13", "MYSQL_TLS_TRUSTSTORE_WIRED",
     "deploy/helm/zhicloud/templates/deployment.yaml",
     [r"MYSQL_TLS_TRUSTSTORE_URL",
      r"MYSQL_TLS_TRUSTSTORE_PASSWORD",
      r"secretKeyRef:",
      r"name:\s*\{\{\s*required .*mysqlTls\.truststore\.existingSecret",
      r"key:\s*\{\{\s*\.Values\.mysqlTls\.truststore\.passwordKey",
      r"mountPath:\s*\{\{\s*\.Values\.mysqlTls\.truststore\.mountPath",
      r"secretName:\s*\{\{\s*required .*mysqlTls\.truststore\.existingSecret",
      r"key:\s*\{\{\s*\.Values\.mysqlTls\.truststore\.fileKey"]),
    # Connector/J 的连接属性由 application-prod.yaml 显式绑定到环境变量；
    # 这样 truststore 密码不会出现在 JDBC URL 或 ConfigMap 中。
    ("C14", "MYSQL_TLS_TRUSTSTORE_CONFIG",
     "zhicloud-server/src/main/resources/application-prod.yaml",
     [r"connection-properties:\s*\n\s*trustCertificateKeyStoreUrl:\s*\$\{MYSQL_TLS_TRUSTSTORE_URL\}",
      r"trustCertificateKeyStorePassword:\s*\$\{MYSQL_TLS_TRUSTSTORE_PASSWORD\}",
      r"trustCertificateKeyStoreType:\s*\$\{MYSQL_TLS_TRUSTSTORE_TYPE:PKCS12\}"]),
    # Flyway PreSync Job 也直连 MySQL；它必须在 Deployment 之前具备同一
    # truststore 卷、挂载和密码注入，否则 VERIFY_IDENTITY 会先阻断发布。
    ("C15", "MYSQL_TLS_TRUSTSTORE_WIRED_JOB",
     "deploy/helm/zhicloud/templates/job-migration.yaml",
     [r"MYSQL_TLS_TRUSTSTORE_URL",
      r"MYSQL_TLS_TRUSTSTORE_PASSWORD",
      r"secretKeyRef:",
      r"name:\s*\{\{\s*required .*mysqlTls\.truststore\.existingSecret",
      r"key:\s*\{\{\s*\.Values\.mysqlTls\.truststore\.passwordKey",
      r"mountPath:\s*\{\{\s*\.Values\.mysqlTls\.truststore\.mountPath",
      r"secretName:\s*\{\{\s*required .*mysqlTls\.truststore\.existingSecret",
      r"key:\s*\{\{\s*\.Values\.mysqlTls\.truststore\.fileKey"]),
    # 原生 K8s 清单没有 Helm values 合并能力，必须独立声明 truststore Secret、
    # secretKeyRef、只读挂载和容器内文件路径。
    ("C16", "MYSQL_TLS_TRUSTSTORE_WIRED_K8S",
     "k8s/zhicloud-server.yaml",
     [r"MYSQL_TLS_TRUSTSTORE_URL",
      r"MYSQL_TLS_TRUSTSTORE_PASSWORD",
      r"secretKeyRef:",
      r"name:\s*zhicloud-mysql-tls-secret",
      r"key:\s*MYSQL_TLS_TRUSTSTORE_PASSWORD",
      r"mountPath:\s*/etc/zhicloud/mysql-tls",
      r"secretName:\s*zhicloud-mysql-tls-secret",
      r"key:\s*truststore\.p12"]),
    # 管理员 PreSync Job 与迁移 Job 一样直连 MySQL，必须完整复用 truststore 契约。
    ("C17", "MYSQL_TLS_TRUSTSTORE_WIRED_BOOTSTRAP_JOB",
     "deploy/helm/zhicloud/templates/job-bootstrap-admin.yaml",
     [r"MYSQL_TLS_TRUSTSTORE_URL",
      r"MYSQL_TLS_TRUSTSTORE_PASSWORD",
      r"secretKeyRef:",
      r"name:\s*\{\{\s*required .*mysqlTls\.truststore\.existingSecret",
      r"key:\s*\{\{\s*\.Values\.mysqlTls\.truststore\.passwordKey",
      r"mountPath:\s*\{\{\s*\.Values\.mysqlTls\.truststore\.mountPath",
      r"secretName:\s*\{\{\s*required .*mysqlTls\.truststore\.existingSecret",
      r"key:\s*\{\{\s*\.Values\.mysqlTls\.truststore\.fileKey"]),
    # 管理员口令必须来自独立 Secret；禁止把它写进 values、ConfigMap 或 Job 明文。
    ("C18", "BOOTSTRAP_ADMIN_PASSWORD_SECRET_WIRED",
     "deploy/helm/zhicloud/templates/job-bootstrap-admin.yaml",
     [r"BOOTSTRAP_ADMIN_PASSWORD",
      r"valueFrom:",
      r"secretKeyRef:",
      r"name:\s*\{\{\s*required .*bootstrapAdmin\.passwordSecretName",
      r"key:\s*\{\{\s*\.Values\.bootstrapAdmin\.passwordSecretKey"]),
]


def run_controls(repo: Path) -> list[dict[str, Any]]:
    out: list[dict[str, Any]] = []
    for cid, name, target, patterns in CONTROLS:
        matches = sorted(repo.rglob(target))
        matches = [p for p in matches
                   if not is_skipped_relative_path(p.relative_to(repo))]
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


def build_report(
    repo: Path,
    fail_on: str = "P0",
    suppression_file: Path | None = None,
    max_expiry_days: int = DEFAULT_MAX_EXPIRY_DAYS,
    today: Any = None,
) -> dict[str, Any]:
    scan = scan_repo(repo)
    all_findings = scan["findings"]
    controls = run_controls(repo)
    entries: list[Any] = []
    if suppression_file is not None:
        entries = load_suppression_file(suppression_file)
    applied = apply_suppressions(all_findings, entries, today=today,
                                 known_rules=sorted(RULES.keys()),
                                 max_expiry_days=max_expiry_days)
    # findings 只保留 remaining：被抑制的项不再阻断，但完整留在
    # suppressed_findings 里（永不隐藏）。
    findings = applied["remaining"]
    by_rule: dict[str, int] = {}
    by_severity: dict[str, int] = {}
    for f in findings:
        by_rule[f["rule"]] = by_rule.get(f["rule"], 0) + 1
        by_severity[f["severity"]] = by_severity.get(f["severity"], 0) + 1
    failed_controls = [c for c in controls if c["status"] != "passed"]
    section = suppression_section(applied, entries,
                                  max_expiry_days=max_expiry_days)
    status = status_for(by_severity, applied, fail_on=fail_on,
                        controls_failed=len(failed_controls))
    return {
        "generated_at": datetime.now().astimezone().isoformat(timespec="seconds"),
        "tool": "security_baseline_scan.py",
        "evidence_class": "static-text scan; no runtime/profile resolution, no dependency CVE scan",
        "repo_root": str(repo),
        "commit": git_head(repo),
        "scanned_files": scan["scanned_files"],
        "rules": {rid: name for rid, (name, _) in RULES.items()},
        "finding_count": len(findings),
        "finding_count_before_suppression": len(all_findings),
        "suppressed_findings": applied["suppressed"],
        "suppression": section,
        "findings_by_rule": by_rule,
        "findings_by_severity": by_severity,
        "controls": controls,
        "controls_failed": len(failed_controls),
        "findings": findings,
        "status": status,
        "fail_on": fail_on,
        "disclaimers": [
            "静态文本扫描：环境变量/Secret 注入可能覆盖默认值，命中项需人工确认。",
            "不覆盖依赖漏洞（CVE）、渗透测试、历史 git 密钥泄露考古。",
            "本扫描通过不等于安全基线已达标；P0-2 仍需逐项人工处置与复测。",
            "MySQL TLS 规则只校验连接参数；真实证书链/主机名握手仍需连接带证书的数据库验证。",
            "若启用 --suppressions：被抑制项仍完整出现在 suppressed_findings 与 suppression 段，"
            "只是不再计入阻断；抑制条目缺字段、超期、规则未知或未命中任何 finding（stale）时，"
            "报告一律判 failed（fail-closed）。",
        ],
    }


def main(argv: list[str] | None = None) -> int:
    ap = argparse.ArgumentParser(description="安全基线静态扫描")
    ap.add_argument("--repo", type=Path, default=Path(__file__).resolve().parents[2])
    ap.add_argument("--output", type=Path)
    ap.add_argument("--summary", action="store_true", help="只打印汇总")
    ap.add_argument("--suppressions", type=Path, default=None,
                    help="可审计抑制文件（JSON 数组）；不传则不启用抑制")
    ap.add_argument("--max-expiry-days", type=int, default=DEFAULT_MAX_EXPIRY_DAYS,
                    help="抑制条目最长有效期，超过即判不合格")
    args = ap.parse_args(argv)

    report = build_report(args.repo.resolve(),
                          suppression_file=args.suppressions,
                          max_expiry_days=args.max_expiry_days)
    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(json.dumps(report, ensure_ascii=False, indent=2,
                                          sort_keys=True) + "\n",
                               encoding="utf-8", newline="\n")
    if not args.summary:
        for f in report["findings"][:200]:
            print(f"[{f['severity']}][{f['rule']}] {f['file']}:{f['line']} :: {f['evidence'][:120]}")
    sup = report["suppression"]
    print(f"扫描文件 {report['scanned_files']}；阻断 {report['finding_count']} "
          f"（{report['findings_by_severity']}）；正向控制失败 {report['controls_failed']}")
    print(f"抑制：条目 {sup['entries']} 有效 {sup['valid']} 应用 {sup['applied']} "
          f"不合格 {sup['invalid']} 未命中 {sup['unmatched']} "
          f"（抑制前命中 {report['finding_count_before_suppression']}）")
    print(f"status={report['status']}")
    return 1 if report["status"] == "failed" else 0


if __name__ == "__main__":
    raise SystemExit(main())
