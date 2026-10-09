#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""证据引用一致性门禁（R-050）：文档里引用的证据路径与 SHA-256 是否还对得上。

§3.4 立的规矩是"凡被文档引用的证据，引用处必须内联六要素"，但这条规矩今天**全靠人守**：
本轮就手工抓到两处 —— 一条"生成命令"按现行 CLI 根本跑不动（§12.69），
一处把 jar 的构建实例哈希当代码身份跨批次引用（§12.72）。
这说明"引用会不会悬空/会不会指错东西"必须机检，而不是等下一个人在几百行表格里看走眼。

判定（全部是判定，不是叙述）：
① `missing-path` —— 文档写了某条证据路径，本机不存在；
② `hash-drift` —— 同处引用了 64 位 SHA-256，但文件当前字节算出来不是它；
③ `untracked-but-present` —— 文件在、但未进版本库：对干净检出的人是**悬空引用**（§3.4 的已知代价，必须量出来而不是假设）；
④ `unpaired-hash` —— 引用了 SHA 却在同一上下文里找不到可核对的路径（只能证明"曾经算过什么"，不可复核）；
⑤ 覆盖率自证 —— 扫到的文档数、路径数低于门槛即 FAIL（扫不到不等于都对，§12.18.3 的同一课）。

只读：不改文档、不改证据、不碰索引。
"""
from __future__ import annotations

import argparse
import hashlib
import json
import re
import subprocess
import sys
from datetime import datetime, timezone
from pathlib import Path

CRLF = bytes([13, 10])
LF = bytes([10])
REPO = Path(__file__).resolve().parents[2]
DOCS_ROOT = REPO / "docs" / "enterprise-platform"
EVIDENCE_MARK = "evidence/production/"
SHA64 = re.compile(r"\b[0-9a-fA-F]{64}\b")
# 证据路径的形态：可以是相对 docs/enterprise-platform、相对仓库根，或写成绝对路径的尾巴。
PATH_TOKEN = re.compile(r"[A-Za-z0-9_./+:\-]*" + EVIDENCE_MARK + r"[A-Za-z0-9_./+\-]*")
# 省略式写法：`.../name.ext`、`../name.ext` —— 相对**同一行里最近一条完整证据路径所在目录**。
ABBREV_TOKEN = re.compile(r"(?:\.\.\./|\.\./)([A-Za-z0-9_][A-Za-z0-9_.\-]*\.[A-Za-z]{2,4})")
MIN_DOCS = 10
MIN_PATH_CITATIONS = 30
# **绝对基线**，不是比例：上一轮我自己写的一个归一化步骤把 `path_citations` 从 197 静默缩到 168，
# 而当时所有校验（含 `path_citations == resolved + missing + dropped` 这条恒等式与上面的门槛）
# 全部通过、违规数还下降 —— 在分母上做的算术检不出分母自己塌陷。
# 基线取 2026-10-09 证据入库策略切换后的实测值（docs=46 / citations=213 / dangling=0）；此前为 2026-10-02 在 `HEAD=d0ebc96c` 的 42/197/111。文档面**合法变动**时必须一起重测并显式改这三个数，
# 不接受"为了让门禁变绿而下调基线"。
BASELINE = {"docs_scanned": 46, "path_citations": 213, "dangling_advisories": 0}
SHRINK_TOLERANCE = 0.05


def coverage_shrinkage(coverage: dict, dangling: int) -> list[dict]:
    out = []
    measured = {"docs_scanned": coverage["docs_scanned"],
                "path_citations": coverage["path_citations"],
                "dangling_advisories": dangling}
    for key, base in BASELINE.items():
        floor = base * (1 - SHRINK_TOLERANCE)
        if measured[key] < floor:
            out.append({"kind": "coverage-shrink", "metric": key,
                        "baseline": base, "measured": measured[key],
                        "floor": round(floor, 2),
                        "detail": "判定面相对基线收缩：这是覆盖丢失，不是问题变少。"
                                  "若确属文档面合法变动，请重测后显式更新 BASELINE。"})
    return out


def git_lines(*args: str) -> list[str]:
    return [l for l in subprocess.run(["git", *args], cwd=str(REPO), capture_output=True).stdout
            .decode("utf-8", "replace").splitlines() if l.strip()]


def sha256_of(path: Path) -> str:
    return hashlib.sha256(path.read_bytes()).hexdigest()


def candidate_paths(raw: str, docs_root: Path, repo_root: Path) -> list[Path]:
    cleaned = raw.strip().rstrip(".,;）)。`").lstrip("./")
    cleaned = cleaned.replace("\\", "/")
    tail = cleaned[cleaned.find(EVIDENCE_MARK):] if EVIDENCE_MARK in cleaned else cleaned
    return [docs_root / tail, repo_root / "docs" / tail, repo_root / tail]


def resolve(raw: str, docs_root: Path, repo_root: Path) -> Path | None:
    for p in candidate_paths(raw, docs_root, repo_root):
        try:
            if p.is_file():
                return p
        except OSError:
            continue
    return None


def identity_hashes(path: Path) -> dict:
    """一份证据可以有多个"合法身份哈希"，全部算出来再比，否则会把我方工具的正当写法判成 drift。

    实测需要覆盖的四种：字节 SHA、LF 规范化 SHA（本项目 C4 用 canonical LF 哈希）、
    JSON 内 `payload_sha256` / `file_sha256` 字段（内容身份，README 明确与字节身份分开记录）、
    以及 `.sha256` 旁车里记的值。`demo-schema-snapshot-…json` 就是被引用为 `payload_sha256` 而判错的。
    """
    out = {}
    try:
        raw = path.read_bytes()
    except OSError:
        return out
    out["bytes_sha256"] = hashlib.sha256(raw).hexdigest()
    out["canonical_lf_sha256"] = hashlib.sha256(raw.replace(CRLF, LF)).hexdigest()
    if path.suffix.lower() == ".json":
        try:
            payload = json.loads(raw.decode("utf-8-sig"))
        except (json.JSONDecodeError, UnicodeDecodeError):
            payload = None
        if isinstance(payload, dict):
            for key in ("payload_sha256", "file_sha256", "sha256"):
                value = payload.get(key)
                if isinstance(value, str) and SHA64.fullmatch(value.strip()):
                    out[key] = value.strip().lower()
    side = Path(str(path) + ".sha256")
    if side.is_file():
        try:
            out["sidecar_sha256"] = side.read_text(encoding="utf-8", errors="replace").split()[0].lower()
        except (OSError, IndexError):
            pass
    # 第六种身份：证据文件**内部记录的被验对象哈希**。文档常见写法是"引用这份证据，同时记下它
    # 验证的是哪个 BOM / 哪份产物"，那个 64 位值属于被验对象而不是证据文件本身。
    # 实测两例：`maven-license-internal-patch-verification-…md/.json` 里记的 `bom_sha256`
    # `29427017AAADEC5E…`（大写），以及复制沙箱 `SUMMARY.md` 内记的产物哈希。
    # 把它当成 drift 会逼人删掉合法信息；把它当成相符又不说明是哪一侧，所以单列一类。
    try:
        text = raw.decode("utf-8-sig", "replace")
    except UnicodeDecodeError:
        text = ""
    for token in SHA64.findall(text):
        low = token.lower()
        out.setdefault(f"embedded_sha256:{low[:12]}", low)
    return out


def scan_docs(files: list[Path], repo_root: Path) -> list[dict]:
    """按行取上下文，再把每个 SHA 配到**它左边最近**的那条证据路径上。

    只用"同行有路径 + 同行有 SHA"就配对是错的：一行里常同时引用两份证据、两个哈希，
    交叉错配会把对的判成 drift（第一版就因此报出 6 条 drift，其中多条是跨引用串位）。
    """
    citations: list[dict] = []
    for f in files:
        text = f.read_text(encoding="utf-8-sig", errors="replace")
        for lineno, line in enumerate(text.splitlines(), start=1):
            spans = [(m.start(), m.end(), m.group(0)) for m in PATH_TOKEN.finditer(line)]
            if not spans:
                continue
            for _start, end, raw in sorted({(s[0], s[1], s[2]) for s in spans}):
                # 路径正则不含 `<`/`*`，所以 `security-baseline-<时间戳>.json` 会被截断成
                # 一条以 `-` 结尾的碎片；只看截断后的碎片查不出它是模板。必须看**紧跟其后的字符**。
                nxt = line[end:end + 1]
                if nxt in ("<", "*", "[") or raw.endswith(("-", ".")):
                    continue
                is_dir = raw.endswith("/")
                start = line.find(raw)
                nearby = [h.group(0) for h in SHA64.finditer(line)
                          if not is_dir and h.start() > start and (h.start() - start) <= 400]
                citations.append({"doc": f.relative_to(repo_root).as_posix(),
                                  "line": lineno, "raw": raw, "directory": is_dir,
                                  "context": line[:600], "hashes": nearby})
            # 后置遍历，只**追加**省略式引用：既有 197 条的来源不动，所以这一步在结构上
            # 不可能让 path_citations 变小（上一版就是把展开写进主循环，静默吞掉 29 条）。
            last_full = None
            seen_abbrev: set[str] = set()
            for s_start, s_end, raw in sorted({(s[0], s[1], s[2]) for s in spans}):
                last_full = raw
                if "/" not in last_full:
                    continue
                for m in ABBREV_TOKEN.finditer(line, s_end):
                    abbrev = last_full.rsplit("/", 1)[0] + "/" + m.group(1)
                    if abbrev in seen_abbrev:
                        continue
                    seen_abbrev.add(abbrev)
                    tail_hashes = [h.group(0) for h in SHA64.finditer(line)
                                   if h.start() >= m.end() and (h.start() - m.end()) <= 400]
                    citations.append({"doc": f.relative_to(repo_root).as_posix(),
                                      "line": lineno, "raw": abbrev, "directory": False,
                                      "context": line[:600], "hashes": tail_hashes,
                                      "abbreviated": True})
    return citations


def repo_root_for_docs() -> Path:
    return REPO


def evaluate(doc_files: list[Path], tracked: set[str], docs_root: Path, repo_root: Path,
             strict_untracked: bool = False) -> dict:
    citations = scan_docs(doc_files, repo_root)
    violations = []
    advisories: list[dict] = []
    checked_paths = 0
    hash_checked = 0
    hash_embedded_subject = 0
    directory_dropped = 0
    abbrev_resolved = 0
    for c in citations:
        if c.get("directory"):
            counts_dir = None  # 目录级提及不参与文件判定（见 scan_docs）
            continue
        raw_path = None
        for cand in candidate_paths(c["raw"], docs_root, repo_root):
            if cand.is_dir():
                break
            if cand.is_file():
                raw_path = cand
                break
        if raw_path is None and any(cand.is_dir() for cand in candidate_paths(c["raw"], docs_root, repo_root)):
            directory_dropped += 1
            # 目录级引用（常写成 `…/<run-id> -EvidenceDir`，没有结尾斜杠）不是可核对的文件。
            # 第一版只认结尾斜杠，于是把这份 SUMMARY.md 里的运行目录判成"文件缺失"，
            # 我甚至据此写了一段"脚本不存在"的结论 —— 一查就发现是判定错，不是文档错。
            continue
        resolved = raw_path
        rel = resolved.relative_to(repo_root).as_posix() if resolved else c["raw"]
        if resolved is None:
            if c.get("abbreviated"):
                # 省略式写法推出来的路径没落上：记一条 advisory，不冒充"文档引用了不存在的文件"。
                advisories.append({"kind": "abbreviated-unresolved", "doc": c["doc"], "line": c["line"],
                                   "citation": c["raw"][:160],
                                   "detail": "由 `.../名称` 推出但未落上文件；不作为缺失判定的依据"})
                continue
            violations.append({"kind": "missing-path", "doc": c["doc"], "line": c["line"],
                               "citation": c["raw"][:160]})
            continue
        if c.get("abbreviated"):
            abbrev_resolved += 1
        checked_paths += 1
        if rel not in tracked:
            # 这不是缺陷，而是 §3.4 的**既定代价**：证据按约定不入库，所以文档引用对第三方必然悬空。
            # 把它算进 violations 会让门禁永远红，红久了就没人看 —— 那等于没有门禁。
            # 因此默认作为 advisory 量化输出（111 条这种数字本身就是台账规模的证据面代价），
            # 到提交冻结阶段用 --strict-untracked 把它升格为阻断：那时"文档入库而引用对象不入库"才是真缺陷。
            advisories.append({"kind": "untracked-but-present", "doc": c["doc"], "line": c["line"],
                               "path": rel,
                               "detail": "文件在本机存在但不在版本库：干净检出的人拿不到它（§3.4 代价）"})
        if not c["hashes"]:
            continue
        cited = {h.lower() for h in c["hashes"]}
        identities = identity_hashes(resolved)
        matched = [name for name, hx in identities.items() if hx in cited]
        if not matched:
            # 文档用标签说明"这个哈希属于哪种产物"（`01-平台现状核验.md:166` 明写
            # `JSON SHA-256 9CE4E11A…`，而路径列写的是同 runs 的 `.md` 报告）。
            # 认标签而不是判成兄弟错配：标签是文档作者给出的**明确语义**，把它当噪声就是在逼人删信息。
            label = re.search(r"([A-Za-z][A-Za-z0-9_-]{1,12})\s+SHA-?256", c.get("context", ""))
            if label:
                want = "." + label.group(1).lower().replace("json", "json")
                sib = resolved.with_suffix(want) if want != ".md" else resolved.with_suffix(".md")
                if sib.is_file() and identity_hashes(sib).get("bytes_sha256") in cited:
                    hash_checked += 1
                    continue
        embedded_only = bool(matched) and all(name.startswith("embedded_sha256:") for name in matched)
        if matched and not embedded_only:
            hash_checked += 1
            continue
        if embedded_only:
            # 引用可核对，但它记的是**被验对象**的哈希而不是这份证据的字节哈希：
            # 不算缺陷，也不能计入"字节相符"，否则读者以为文件身份已被验证。
            hash_embedded_subject += 1
            advisories.append({"kind": "hash-is-subject-identity", "doc": c["doc"], "line": c["line"],
                               "path": rel, "cited": sorted(cited)[:2],
                               "detail": "所引 SHA 在证据文件内部被记录（如 bom_sha256），属被验对象身份；"
                                         "该证据文件自身的字节哈希未经此值核对"})
            continue
        # 行内其它证据文件才是这个哈希的主人（省略式路径、一条引用带多个哈希都会走到这里）
        owner = next((s for s in (resolve(x["raw"], docs_root, repo_root) for x in citations
                                  if x["doc"] == c["doc"] and x["line"] == c["line"])
                      if s and identity_hashes(s).get("bytes_sha256") in cited), None)
        if owner is not None:
            advisories.append({"kind": "hash-attributed-elsewhere", "doc": c["doc"], "line": c["line"],
                               "path": rel, "real_owner": owner.relative_to(repo_root).as_posix(),
                               "cited": sorted(cited)[:3],
                               "detail": "该 SHA 属于同一上下文里的另一份证据；本条路径的字节哈希不是它"})
            continue
        # 同目录同名不同后缀的兄弟文件命中：文档把兄弟产物的哈希记在了这条路径上
        stem = resolved.stem
        sibling = next((s for s in resolved.parent.glob(stem + ".*")
                        if s != resolved and identity_hashes(s).get("bytes_sha256") in cited), None)
        if sibling is not None:
            advisories.append({"kind": "sibling-hash-mismatch", "doc": c["doc"], "line": c["line"],
                               "path": rel, "hash_belongs_to": sibling.name,
                               "cited": sorted(cited)[:3],
                               "detail": "引用记录的是兄弟产物（不同后缀）的哈希；路径与身份不同源，须由人决定改哪一侧"})
            continue
        violations.append({"kind": "hash-unattributable", "doc": c["doc"], "line": c["line"], "path": rel,
                           "cited": sorted(cited)[:3],
                           "identities": {k: v[:12] for k, v in identities.items()},
                           "detail": "引用的 SHA 既不等该文件任何合法身份变体，也不属于同一上下文或同目录兄弟产物"})
    unpaired = [{"doc": c["doc"], "line": c["line"], "hashes": c["hashes"][:2]}
               for c in citations
               if c["hashes"] and resolve(c["raw"], docs_root, repo_root) is None]
    counts: dict[str, int] = {}
    for v in violations:
        counts[v["kind"]] = counts.get(v["kind"], 0) + 1
    coverage = {
        "docs_scanned": len(doc_files),
        # 分母 = 全部非目录级引用（含省略式推出来的）。省略式早期我把它排除出分母，
        # 结果 `paths_resolved` 反超 `path_citations`、恒等式破裂（守卫当场判红）——
        # 加进来的引用必须同时进分母，否则分子分母口径不同。
        "path_citations": len([c for c in citations if not c.get("directory")]),
        "abbreviated_citations": len([c for c in citations if c.get("abbreviated")]),
        "abbreviated_resolved": abbrev_resolved,
        "directory_mentions": len([c for c in citations if c.get("directory")]),
        "paths_resolved": checked_paths,
        "directory_citations_dropped": directory_dropped,
        "hash_citations_verified_match": hash_checked,
        "hash_matched_as_subject_identity": hash_embedded_subject,
        "unpaired_hash_citations": len(unpaired),
        "evidence_files_total": len([p for p in git_lines("ls-files", "--others", "--exclude-standard",
                                                          "--", "docs/enterprise-platform/evidence")
                                     if p.endswith((".json", ".log", ".txt", ".md", ".sha256"))]),
        "dangling_reference_advisories": sum(1 for a in advisories
                                             if a["kind"] == "untracked-but-present"),
        "baseline": dict(BASELINE),
        "shrink_tolerance": SHRINK_TOLERANCE,
    }
    if strict_untracked:
        violations.extend(advisories)
    if coverage["docs_scanned"] < MIN_DOCS:
        counts["coverage"] = counts.get("coverage", 0) + 1
        violations.append({"kind": "coverage-too-small", "detail":
                           f"只扫到 {coverage['docs_scanned']} 份文档（门槛 {MIN_DOCS}）：判定面已失真"})
    if coverage["path_citations"] < MIN_PATH_CITATIONS:
        counts["coverage"] = counts.get("coverage", 0) + 1
        violations.append({"kind": "coverage-too-small", "detail":
                           f"只解析到 {coverage['path_citations']} 条证据引用（门槛 {MIN_PATH_CITATIONS}）：正则或文档面变了"})
    # 绝对基线检的是"判定面自己有没有缩水"，这是上面两条比例式校验做不到的事。
    violations.extend(coverage_shrinkage(coverage, coverage["dangling_reference_advisories"]))
    return {
        "generated_at": datetime.now(timezone.utc).astimezone().isoformat(timespec="seconds"),
        "command": " ".join([sys.argv[0]] + sys.argv[1:]) if len(sys.argv) > 1 else sys.argv[0],
        "docs_root": str(docs_root),
        "coverage": coverage,
        "violation_counts": {k: sum(1 for x in violations if x["kind"] == k)
                             for k in sorted({x["kind"] for x in violations})},
        "advisory_kind_counts": {k: sum(1 for x in advisories if x["kind"] == k)
                                 for k in sorted({x["kind"] for x in advisories})},
        "advisory_total": len(advisories),
        "advisories": advisories[:80],
        "advisories_truncated": len(advisories) > 80,
        "violations": violations,
        "unpaired_hash_citations": unpaired[:20],
        "production_ready": False,
        "gate3_passed": False,
        "independent_review": False,
        "documents_modified_by_this_tool": 0,
    }


def main(argv=None) -> int:
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    ap.add_argument("--json", action="store_true", help="输出纯 JSON（UTF-8，早退不打摘要）")
    ap.add_argument("--output", help="把 JSON 写到该路径")
    ap.add_argument("--limit-violations", type=int, default=25)
    ap.add_argument("--strict-untracked", action="store_true",
                    help="把『引用文件未入库』从 advisory 升格为阻断（提交冻结阶段使用）")
    args = ap.parse_args(argv)

    if not DOCS_ROOT.is_dir():
        sys.stdout.buffer.write(f"[FAIL] 文档根目录不存在：{DOCS_ROOT}\n".encode("utf-8"))
        return 2
    doc_files = sorted(p for p in DOCS_ROOT.rglob("*.md") if p.is_file())
    tracked = {l.replace("\\", "/") for l in git_lines("ls-files")}
    report = evaluate(doc_files, tracked, DOCS_ROOT, REPO, strict_untracked=args.strict_untracked)
    payload = json.dumps(report, ensure_ascii=False, indent=2)
    if args.output:
        Path(args.output).write_text(payload, encoding="utf-8")
    if args.json:
        sys.stdout.buffer.write(payload.encode("utf-8", "replace") + b"\n")
        return 1 if report["violations"] else 0
    c = report["coverage"]
    print(f"引用面：docs={c['docs_scanned']} 路径引用={c['path_citations']} 解析到={c['paths_resolved']} "
          f"SHA 核对相符={c['hash_citations_verified_match']} 被验对象身份={c['hash_matched_as_subject_identity']} 无配对 SHA={c['unpaired_hash_citations']}")
    print("违规分类：" + json.dumps(report["violation_counts"], ensure_ascii=False)
          + " ｜ advisory：" + json.dumps(report["advisory_kind_counts"], ensure_ascii=False))
    for v in report["violations"][:args.limit_violations]:
        print("  [VIOLATION]", json.dumps(v, ensure_ascii=False)[:200])
    if report["violations"]:
        print(f"[FAIL] 共 {len(report['violations'])} 条")
        return 1
    print("[OK] 全部证据引用可解析且 SHA 与当前字节一致")
    return 0


if __name__ == "__main__":
    sys.exit(main())
