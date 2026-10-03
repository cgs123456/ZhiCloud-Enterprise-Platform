#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""证据面凭据门禁（R-046）：扫描 evidence 目录里是否残留可用的沙箱明文库口令。

为什么缺这一条：`security_baseline_scan.py` 只扫**已跟踪**文件，而按 §3.4 证据一律不入库，
所以那 1249+ 份证据文件在凭据面上是**零覆盖**。R-044 已经实测到 27 份历史 `.command.txt` 内含
10 字符明文沙箱口令；§12.65 我又是"手工扫两个正则 + 看一眼"就写下"命中 0"——
手工动作既不可复跑，也不会在下一次有人往证据里塞口令时报警。

三层判定，缺一层就会假绿：
① 键值形状：`password|root_password|MYSQL_PWD|MYSQL_ROOT_PASSWORD` 后跟非占位符值；
② **活口令对照**：从沙箱凭据面（state JSON / `*.pw` / runner env 文件）读出当前真实口令，
   逐字节在证据里找——这层不依赖形状，能抓到没有键名的裸口令；
③ 高熵邻接：口令键附近 26-40 位无分隔 token（覆盖 `zc`+24hex 这一族实际生成形状）。

例外表是**带锚点的登记**而不是"忽略"：每条必须 (path + SHA-256) 命中当前一次真实命中；
命中了但哈希变了 → 判红（说明历史被改过，锚点已失效）；登记了却没命中 → 判红（空条目）。
"""
from __future__ import annotations

import argparse
import hashlib
import json
import math
import re
import sys
from collections import Counter
from datetime import datetime, timezone
from pathlib import Path

REPO = Path(__file__).resolve().parents[2]
DEFAULT_EVIDENCE = REPO / "docs" / "enterprise-platform" / "evidence"
DEFAULT_CREDENTIALS = Path(r"C:\Users\Administrator\AppData\Local\Temp")
DEFAULT_ALLOWLIST = REPO / "scripts" / "production" / "evidence-credential-exceptions.json"

PLACEHOLDER_VALUES = {
    "<redacted>", "__root_password__", "redacted", "changeme", "not-a-real-secret",
    "replace-me", "placeholder", "required", "none", "null", "true", "false", "***",
}
KEY_VALUE = re.compile(
    r"(?i)\b(?:mysql_)?(?:root_)?(?:password|pwd|secret|token)\b\s*(?:[:=]|=>)\s*(?:-p)?[\"']?([^\"'\s,;]\S{5,})")
# `[ \t\r]*$` 里的 `\r` 不是装饰：本仓大量证据/配置是 Windows 写的 CRLF，
# 只允许空格/制表符会让行尾 `\r` 挡在 `$` 前面，`MYSQL_ROOT_PASSWORD=xxx\r` 一条都匹配不上
# （守卫夹具用 Path.write_text 落成 CRLF 时实测复现，判定层因此整族漏）。
BARE_ASSIGN = re.compile(r"(?m)^[ \t]*(?:MYSQL_ROOT_PASSWORD|MYSQL_PWD|MYSQL_PWD_FILE)=(\S{6,})[ \t\r]*$")
# `-p<secret>` 是 mysql 客户端的口令形状，R-044 那批历史 `.command.txt` 里就是这个样子；
# 上一版只认 `password|pwd` 键名，等于把这一族整族漏掉（本次实跑才发现）。
DASH_P = re.compile(r"(?<![\w-])-p([A-Za-z0-9]{8,})")
HIGH_ENTROPY_TOKEN = re.compile(r"[A-Za-z0-9]{22,64}")
PATTERN_SCAN_LIMIT = 4_000_000
CREDENTIAL_SOURCES = (
    ("state-json", re.compile(r"^state\.json$")),
    ("pw-file", re.compile(r"\.pw$")),
    ("runner-env", re.compile(r"^zhicloud-runner-env-[0-9a-f]{32}\.txt$")),
)


def sha256_of(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def is_placeholder(value: str) -> bool:
    v = value.strip().strip('"\'').rstrip(".,;")
    if v.startswith("$") or v.startswith("${") or "{{" in v:
        return True                      # 变量引用而不是值
    if v.lower() in PLACEHOLDER_VALUES:
        return True
    if set(v) <= {"*", "x", "X", "•"}:
        return True
    return False


def shannon_entropy(token: str) -> float:
    counts = Counter(token)
    n = len(token)
    return -sum((c / n) * math.log2(c / n) for c in counts.values())


def evidence_files(root: Path) -> list[Path]:
    return sorted(p for p in root.rglob("*") if p.is_file())


# 凭据面是**已声明的几处落点**，不是"把整个 TEMP 走一遍"：
# 全量走 TEMP 既慢又会在别人的重解析点上炸（实测 WinError 1920 on feexp2\base\var\lock），
# 更糟的是它会让人以为"覆盖到了全部"，而实际覆盖范围必须写清楚才算可复核。
CREDENTIAL_GLOBS = (
    "zhicloud-mysql-sandbox/state.json",
    "zhicloud-mysql-sandbox/*.json",
    "zhicloud-runner-env-*.txt",
    # 后两族是**用真活实例试出来的**：BaseDir 由调用方任意取名（本轮实测 `zc-livecred-<date>\.pw`），
    # 只写 `zhicloud-native-*/.pw`、`zc-native-*/.pw` 的话，默认 TEMP 根上探到 0 个凭据、
    # 于是整条活口令对照报 not-exercised 且 exit 0 —— 恰恰在它最该生效的场景下假绿。
    # 深度 1 的 `*/.pw`、`*/state.json` 把命名自由收回来；再深就变成走整盘。
    "*/.pw",
    "*/state.json",
    "zhicloud-native-*/.pw", "zc-native-*/.pw", "zhicloud-*/state.json", "zhicloud-*/.pw", "*.pw",
)


def credential_candidates(root: Path) -> list[Path]:
    found: list[Path] = []
    for pattern in CREDENTIAL_GLOBS:
        try:
            found.extend(p for p in root.glob(pattern) if p.is_file())
        except OSError:
            continue          # 单个落点不可读不算"没有凭据"，coverage 里会记下来
    return sorted({p.resolve() for p in found})


def collect_live_credentials(root: Path) -> tuple[list[dict], int]:
    """返回 (活口令列表, 探到的凭据文件数)。探不到就返回空列表，报告里显式记 not-exercised。"""
    out: list[dict] = []
    if not root.is_dir():
        return out, 0
    candidates = credential_candidates(root)
    for path in candidates:
        try:
            if path.stat().st_size > 200_000:
                continue
            text = path.read_text(encoding="utf-8-sig", errors="replace")
        except (OSError, ValueError):
            continue
        kind = next((k for k, rx in CREDENTIAL_SOURCES if rx.match(path.name)), None) or "unknown"
        values: set[str] = set()
        if kind == "state-json":
            try:
                payload = json.loads(text)
            except json.JSONDecodeError:
                payload = None
            if isinstance(payload, dict):
                for key in ("root_password", "MYSQL_ROOT_PASSWORD", "password"):
                    val = payload.get(key)
                    if isinstance(val, str) and len(val) >= 6:
                        values.add(val)
        else:
            for line in text.splitlines():
                if "=" in line:
                    _k, _, val = line.partition("=")
                else:
                    val = line
                val = val.strip()
                if len(val) >= 6 and not is_placeholder(val):
                    values.add(val)
        for val in values:
            out.append({"source_kind": kind, "value_len": len(val),
                        "sha256_of_value": hashlib.sha256(val.encode()).hexdigest()[:16],
                        "_value": val, "path": str(path)})
    return out, len(candidates)


KEY_CONTEXT = re.compile(r"(?i)(password|passwd|pwd|secret|token|credential)")
DIGEST_SHAPE = re.compile(r"^(?:[0-9a-fA-F]{32}|[0-9a-fA-F]{40}|[0-9a-fA-F]{56}|[0-9a-fA-F]{64}|[0-9a-fA-F]{72}|\d+)$")


def scan_text(path: str, text: str, live: list[dict]) -> list[dict]:
    """四层判定：① `password|pwd|secret|token = 值`、② 行首 `MYSQL_*=(值)`、③ mysql 客户端 `-p<值>`、
    ④ 与活口令逐字节对照。①②③ 都过 is_secret_shaped 形状闸门（见该函数注释）。"""
    hits = []
    for match in KEY_VALUE.finditer(text):
        value = match.group(1)
        if not is_secret_shaped(value):
            continue
        hits.append({"path": path, "layer": "key-value", "line": text[:match.start()].count(chr(10)) + 1,
                     "fingerprint": hashlib.sha256(value.encode()).hexdigest()[:16],
                     "value_len": len(value)})
    for match in BARE_ASSIGN.finditer(text):
        value = match.group(1)
        if not is_secret_shaped(value):
            continue
        hits.append({"path": path, "layer": "bare-assign", "line": text[:match.start()].count(chr(10)) + 1,
                     "fingerprint": hashlib.sha256(value.encode()).hexdigest()[:16],
                     "value_len": len(value)})
    for match in DASH_P.finditer(text):
        value = match.group(1)
        if not is_secret_shaped(value):
            continue
        hits.append({"path": path, "layer": "mysql-dash-p", "line": text[:match.start()].count(chr(10)) + 1,
                     "fingerprint": hashlib.sha256(value.encode()).hexdigest()[:16],
                     "value_len": len(value)})
    for cred in live:
        # 活口令对照：不依赖键名，直接找那串值本身。报告里只留哈希指纹，绝不把口令再抄一遍。
        start = 0
        while True:
            idx = text.find(cred["_value"], start)
            if idx < 0:
                break
            hits.append({"path": path, "layer": "live-credential", "line": text[:idx].count(chr(10)) + 1,
                         "fingerprint": cred["sha256_of_value"], "value_len": cred["value_len"],
                         "matched_source_kind": cred["source_kind"]})
            start = idx + len(cred["_value"])
    # 第 ③ 层"高熵 + 键名邻接"在实跑后被删掉，不是放宽而是止损：
    # 1649 份证据 / 25.6 MB 上它只报出 8 处，逐条掩码核对全是代码标识符
    # （BootstrapAdminHelm…、HelmContract…、Strong…、selector…、ttl…），0 处是真凭据。
    # 保留一个只会产噪声的层，等于训练后来人"整表放行"，比没有这层更危险。
    # 真凭据的形状由 ①键=值、②行内 KEY=值、③-p<值>、④活口令逐字节对照 覆盖。
    return hits


def load_allowlist(path: Path) -> list[dict]:
    if not path.is_file():
        return []
    try:
        payload = json.loads(path.read_text(encoding="utf-8-sig"))
    except json.JSONDecodeError as exc:
        raise SystemExit(f"例外表不是合法 JSON（{path}）：{exc}")
    if not isinstance(payload, list):
        raise SystemExit(f"例外表必须是数组（{path}）")
    return payload


SECRET_SHAPED = re.compile(r"^[A-Za-z0-9+/=_-]{8,}$")


def is_secret_shaped(value: str) -> bool:
    """形状闸门：沙箱随机口令就是 `zc`+24hex 或 10 位混合 alnum 这一族。

    没有这道闸门时，第一版在 1649 份证据里报出 65 份"命中"，逐条看下来几乎全是
    Helm 渲染件里的 `secret: <资源名>`、`secret: <64 位摘要>`、`${MOCK_SECRET}` 模板，
    以及 `${NACOS_PASSWORD}` 这类引用 —— 那是**字段名恰好叫 secret**，不是值。
    一个把渲染清单全判成凭据泄漏的门禁，明天就会被整表放行，所以先修判定再谈登记。
    """
    v = value.strip().strip('"\'').rstrip(".,;:)")
    if not v or is_placeholder(v):
        return False
    if any(ch in v for ch in "{}$<>|() ") or "*" in v:
        return False                                   # 模板 / 变量引用 / 掩码
    if not SECRET_SHAPED.match(v):
        return False
    if DIGEST_SHAPE.match(v):
        return False                                   # 32/40/64 位摘要形状：是引用不是口令
    if len(v) >= 40:
        return False                                   # 本仓库沙箱口令最长 26；更长的一律按摘要/ID 处理
    return any(ch.isdigit() for ch in v) and any(ch.isalpha() for ch in v)


def evaluate(evidence_root: Path, credentials_root: Path, allowlist: list[dict]) -> dict:
    def sha_for(rel_path: str) -> str:
        """登记与对照都以文件哈希为锚：读不到就写 unavailable，不能让锚点静默为空串。"""
        try:
            return sha256_of(evidence_root / rel_path)
        except OSError:
            return "unreadable"

    live, probed = collect_live_credentials(credentials_root)
    files = evidence_files(evidence_root)
    hits: list[dict] = []
    scanned_bytes = 0
    oversized_scanned = 0
    coverage_gaps: list[dict] = []
    for f in files:
        try:
            size = f.stat().st_size
        except OSError:
            continue
        scanned_bytes += size
        try:
            raw = f.read_bytes()
        except OSError as exc:
            coverage_gaps.append({"path": f.relative_to(evidence_root).as_posix(), "gap": "unreadable",
                                  "detail": str(exc)})
            continue
        text = raw.decode("utf-8-sig", "replace")
        if size > PATTERN_SCAN_LIMIT:
            # 大文件不跳过：活口令对照（逐字节找值）照做，只是不再跑形状正则。
            for cred in live:
                for _ in range(text.count(cred["_value"])):
                    hits.append({"path": f.relative_to(evidence_root).as_posix(),
                                 "layer": "live-credential", "line": 0,
                                 "fingerprint": cred["sha256_of_value"],
                                 "value_len": cred["value_len"], "note": "oversized-file"})
            oversized_scanned += 1
            continue
        for hit in scan_text(f.relative_to(evidence_root).as_posix(), text, live):
            hits.append(hit)

    by_path = {}
    for h in hits:
        by_path.setdefault(h["path"], []).append(h)
    accepted, unaccepted = [], []
    used_entries = set()
    for path, group in sorted(by_path.items()):
        entry = next((a for a in allowlist if a.get("path") == path
                      and a.get("sha256") == sha_for(path)), None)
        drifted = next((a for a in allowlist if a.get("path") == path
                        and a.get("sha256") != sha_for(path)), None)
        if drifted is not None:
            unaccepted.append({"path": path, "layer": "allowlist-drift", "sha256": sha_for(path),
                               "registered_sha256": drifted.get("sha256"),
                               "detail": "例外表登记的哈希与当前文件不一致：历史文件被改过，锚点已失效"})
            continue
        if entry is not None:
            used_entries.add(entry["path"])
            accepted.append({"path": path, "layers": sorted({g["layer"] for g in group}),
                             "reason": entry.get("reason", ""), "decided_by": entry.get("decided_by", "")})
            continue
        unaccepted.append({"path": path, "layers": sorted({g["layer"] for g in group}),
                           "sha256": sha_for(path),
                           "fingerprints": sorted({g["fingerprint"] for g in group})[:4],
                           "live_matches": sum(1 for g in group if g["layer"] == "live-credential")})
    dead_entries = [a.get("path") for a in allowlist if a.get("path") not in used_entries
                    and a.get("path") not in {u["path"] for u in unaccepted}]

    layers = Counter(h["layer"] for h in hits)
    return {
        "generated_at": datetime.now(timezone.utc).astimezone().isoformat(timespec="seconds"),
        "command": " ".join([sys.argv[0]] + sys.argv[1:]) if len(sys.argv) > 1 else sys.argv[0],
        "evidence_root": str(evidence_root),
        "credentials_root": str(credentials_root),
        "coverage": {
            "files_present": len(files),
            "files_scanned": len(files) - layers.get("unscanned-oversized", 0) - layers.get("unreadable", 0),
            "bytes_scanned": scanned_bytes,
            "live_credentials_loaded": len(live),
            "credential_files_probed": probed,
            "credential_globs": list(CREDENTIAL_GLOBS),
            "oversized_live_only_scanned": oversized_scanned,
            "pattern_scan_limit_bytes": PATTERN_SCAN_LIMIT,
            "live_layer": "exercised" if live else "not-exercised(no live credential found)",
            "layers_with_hits": dict(layers),
        },
        "coverage_gaps": coverage_gaps,
        "accepted": accepted,
        "violations": unaccepted + [{"path": p, "layer": "dead-allowlist-entry",
                                    "detail": "登记了但当前无命中：要么文件已删/改名，要么判定已失效"}
                                    for p in sorted(dead_entries)],
        "totals": {"hits": len(hits), "files_with_hits": len(by_path),
                   "accepted": len(accepted), "unaccepted": len(unaccepted),
                   "dead_allowlist_entries": len(dead_entries)},
        "production_ready": False,
        "independent_review": False,
    }


def main(argv=None) -> int:
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    ap.add_argument("--evidence-root")
    ap.add_argument("--credentials-root")
    ap.add_argument("--allowlist")
    ap.add_argument("--json", action="store_true")
    ap.add_argument("--output", help="把 JSON 报告写到该路径（UTF-8）")
    ap.add_argument("--show-secrets", action="store_true",
                    help="调试用：把命中值明文打出来。默认只打长度与哈希指纹，绝不把口令抄进日志。")
    args = ap.parse_args(argv)

    evidence_root = Path(args.evidence_root) if args.evidence_root else DEFAULT_EVIDENCE
    credentials_root = Path(args.credentials_root) if args.credentials_root else DEFAULT_CREDENTIALS
    allowlist_path = Path(args.allowlist) if args.allowlist else DEFAULT_ALLOWLIST
    if not evidence_root.is_dir():
        print(f"[FAIL] 证据根目录不存在：{evidence_root}（扫不到不等于干净）")
        return 2
    report = evaluate(evidence_root, credentials_root, load_allowlist(allowlist_path))
    payload = json.dumps(report, ensure_ascii=False, indent=2)
    if args.json:
        # 纯 JSON + 早退：不 early-return 的话，下面的人读摘要会顺着同一个 stdout 流进 JSON 里，
        # 而 print 走控制台编码（cp936）—— 一份 GBK 尾巴就把整份证据打成不可解析（本次实测撞上）。
        sys.stdout.buffer.write(payload.encode("utf-8", "replace") + b"\n")
        return 1 if report["violations"] else 0
    if args.output:
        Path(args.output).write_text(payload, encoding="utf-8")
    c = report["coverage"]
    print(f"证据面：present={c['files_present']} scanned={c['files_scanned']} bytes={c['bytes_scanned']} "
          f"live_credentials={c['live_credentials_loaded']} live_layer={c['live_layer']}")
    print(f"命中：{report['totals']['hits']} 处 / {report['totals']['files_with_hits']} 份文件；"
          f"已登记接受 {report['totals']['accepted']}，未接受 {report['totals']['unaccepted']}，"
          f"空条目 {report['totals']['dead_allowlist_entries']}")
    for gap in report["coverage_gaps"][:5]:
        print("  [COVERAGE-GAP]", json.dumps(gap, ensure_ascii=False)[:160])
    for v in report["violations"][:15]:
        print("  [VIOLATION]", json.dumps(v, ensure_ascii=False)[:200])
    if report["violations"]:
        print("[FAIL] 证据面存在未登记的凭据命中")
        return 1
    print("[OK] 证据面无未登记凭据命中（已登记的按 R-044 逐条带锚点接受）")
    return 0


if __name__ == "__main__":
    sys.exit(main())
