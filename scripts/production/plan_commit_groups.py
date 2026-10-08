#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""分组提交计划的可机检生成器（R-045）。

为什么需要它：本工作树有 ~1300 项未提交/未跟踪，其中证据类按 §3.4 **不入库**，
代码与 handoff 文档要**分组提交**。此前分组只写在 handoff 文档里（人读的一张表）。
计划 T2 已经证明过这类清单会自败：`migration-contract.json` 登记了 V119/V120/V121 的哈希，
而三者未跟踪 —— 单独 checkout 那笔提交就 C5 失败。当时只修了这一处；
但"某一组引用了另一组里尚未提交的工件"是一整类缺陷，靠人记是记不住的。

本脚本做三件事，全部是判定而不是叙述：
1. 枚举工作树每一项，按**路径规则**分入有序分组（规则表在 GROUP_RULES，不抄文件名）；
2. 断言四条不变量：证据不入库、覆盖且仅覆盖一次、跨组引用必须指向同组或更早的组、规则本身不空转；
3. 输出分组计划与违规清单（有违规就非零退出）。

它不碰索引、不 add、不 commit —— 只读，可反复跑。
"""
from __future__ import annotations

import argparse
import json
import re
import subprocess
import sys
from datetime import datetime, timezone
from pathlib import Path

REPO = Path(__file__).resolve().parents[2]
EVIDENCE_PREFIX = "docs/enterprise-platform/evidence/"

# 有序分组：(组号, 组名, 路径匹配规则, 为什么必须是这一组)。
# 规则是"路径前缀/正则"，不是手抄的文件清单 —— 抄来的清单会在下一个文件加入时静默失真（§12.18.3）。
# 组号就是建议的提交顺序；但**顺序不由这里声明说了算**：下面的引用图会实测，
# 出现"前指"就判红，出现互相引用（环）就判"必须并成一次提交"。
GROUP_RULES = [
    (1, "docs-handoff",
     lambda p: p.startswith("docs/enterprise-platform/handoff/"),
     "4 份 handoff（含分组计划本身）是当前最大的丢失风险：整目录 100% 未跟踪。"),
    (2, "docs-platform",
     lambda p: (p.startswith("docs/enterprise-platform/") and p.endswith(".md")
                # 这条排除位是**实测逼出来的**：证据目录里也有 MANIFEST.md / SUMMARY.md，
                # 上一版规则只判 `docs/enterprise-platform/**.md`，10 份证据文件会被扫进文档提交，
                # 直接违反 §3.4（证据不入库）。判定面把规则逐条对证据路径再跑一次，就是为了让这种
                # "顺手宽了一点"的前缀当场响。
                and not p.startswith(EVIDENCE_PREFIX)),
     "台账与证据口径文档；只含被引用的六要素文本，不含证据文件本体（含证据目录里的 .md 旁车）。"
     "边界：本门禁只看代码/配置里的**工件引用**，散文式引用（.md 里提到某个文件名）不在判定面内。"),
    (3, "migration-contract",
     lambda p: (p.startswith("zhicloud-server/src/main/resources/db/migration/")
                or p.startswith("zhicloud-server/src/main/resources/db/verification/")
                or p.startswith("scripts/production/migration-contract")),
     "T2 的自败点：契约 JSON 与 V1xx 迁移、验证 SQL 必须**同组**，否则单独 checkout 必 C5。"),
    (4, "business-code",
     lambda p: p.startswith(("zhicloud-framework/", "zhicloud-module-", "zhicloud-server/", "zhicloud-ui/")),
     "Java 生产/测试代码与测试资源 SQL，以及前端 zhicloud-ui（此前从未 dirty，"
     "商业域移除删除其商城目录后才首次被认领，实测踩到 unclassified）。"),
    (5, "demo-data",
     lambda p: p.startswith("demo-data/"),
     "演示数据生成器与种子 SQL：被沙箱脚本按路径读取，因此必须早于 tooling。"),
    (6, "tooling",
     lambda p: (p.startswith("scripts/") or p.startswith(".github/workflows/") or p == "pom.xml"),
     "门禁脚本 + 沙箱 .ps1 + 守卫测试 + CI 与根构建配置。原本按这三类分开列组，实测是**互相引用的一个强连通分量**"
     "（.ps1 ↔ 门禁 .py ↔ tests .py），分开提交会让其中任意一笔单独 checkout 时缺工件，故并为一组。"
     "CI workflow 与根 pom.xml 也归这组：它们是**调用**门禁脚本的那一侧，且此前没有任何规则认领它们"
     "（真树上一 dirty 就报 unclassified，是实测踩到的）。"),
]

# 一条引用文本里可能出现的"工件名"。只用于把引用落到具体仓库文件上，不做语义猜测。
REF_TOKEN = re.compile(r"[A-Za-z0-9_./\\-]+\.(?:ps1|py|sql|json|java|xml|yaml|yml)\b")
MIGRATION_TOKEN = re.compile(r"\bV(\d{2,3})__")

# 每个分组允许被哪些"关键词引用"落到别处的未跟踪文件上时视为同组约束（大小写不敏感）。
GROUP_OF_INTEREST_SUFFIXES = (".ps1", ".py", ".sql", ".json")


def git_lines(*args: str) -> list[str]:
    proc = subprocess.run(["git", *args], cwd=str(REPO), capture_output=True)
    if proc.returncode != 0:
        raise SystemExit(f"git {' '.join(args)} 失败：{proc.stderr.decode('utf-8', 'replace').strip()}")
    return [l for l in proc.stdout.decode("utf-8", "replace").splitlines() if l.strip()]


def worktree_items() -> tuple[list[str], list[str]]:
    """返回 (未跟踪文件, 已跟踪且被修改文件)，全部为仓库相对 posix 路径。"""
    untracked, modified = [], []
    for line in git_lines("status", "--porcelain=1", "-uall"):
        status, path = line[:2], line[3:].split(" -> ")[0].strip().strip('"')
        path = path.replace("\\", "/")
        if status == "??":
            untracked.append(path)
        elif status.strip():
            modified.append(path)
    return untracked, modified


def classify(paths: list[str]) -> dict[int, list[str]]:
    groups: dict[int, list[str]] = {no: [] for no, _n, _f, _w in GROUP_RULES}
    for path in paths:
        if path.startswith(EVIDENCE_PREFIX):
            continue
        for no, _name, rule, _why in GROUP_RULES:
            if rule(path):
                groups[no].append(path)
                break
    return groups


def tracked_files() -> set[str]:
    return {l.replace("\\", "/") for l in git_lines("ls-files")}


def reference_targets(text: str, existing: set[str]) -> set[str]:
    """把一段文本里的工件引用落到"确实存在于仓库或工作树"的路径上。"""
    hits: set[str] = set()
    for token in REF_TOKEN.findall(text):
        norm = token.replace("\\", "/").lstrip("./")
        cands = [p for p in existing if p == norm or p.endswith("/" + norm)]
        hits.update(cands)
    return hits


def expand_migration(paths_in_set: set[str], text: str, existing: set[str]) -> set[str]:
    """V1xx 形式的引用：迁移脚本按 `V119__xxx.sql` 命名，文本里常只出现版本号。"""
    hits: set[str] = set()
    for ver in set(MIGRATION_TOKEN.findall(text)):
        padded = ver.zfill(3)
        hits.update(p for p in existing
                    if re.search(rf"/V{padded}__[^/]*\.sql$", p) or p.startswith(f"V{padded}__"))
    return hits


def strongly_connected(nodes: list[int], edges: list[tuple[int, int]]) -> list[list[int]]:
    """Tarjan 简化版（分组数 ≤ 8，够用且无外部依赖）。返回若干强连通分量。"""
    adj: dict[int, list[int]] = {n: [] for n in nodes}
    for a, b in edges:
        adj.setdefault(a, []).append(b)
    index: dict[int, int] = {}
    low: dict[int, int] = {}
    on_stack: set[int] = set()
    stack: list[int] = []
    out: list[list[int]] = []
    counter = [0]

    def strongconnect(v):
        index[v] = low[v] = counter[0]
        counter[0] += 1
        stack.append(v)
        on_stack.add(v)
        for w in adj.get(v, []):
            if w not in index:
                strongconnect(w)
                low[v] = min(low[v], low[w])
            elif w in on_stack:
                low[v] = min(low[v], index[w])
        if low[v] == index[v]:
            comp = []
            while True:
                w = stack.pop()
                on_stack.discard(w)
                comp.append(w)
                if w == v:
                    break
            out.append(comp)

    for n in nodes:
        if n not in index:
            strongconnect(n)
    return out


def default_reader(path: str) -> str:
    f = REPO / path
    if not f.is_file() or f.suffix not in GROUP_OF_INTEREST_SUFFIXES or f.stat().st_size > 400_000:
        return ""
    try:
        return f.read_text(encoding="utf-8-sig", errors="replace")
    except OSError:
        return ""


def analyze(items: list[str], existing: set[str], read_text, worktree_stats: dict) -> dict:
    """把分组判定做成纯函数：合成夹具与真工作树走同一条代码，测试才不会只测"真树恰好是绿的"。"""
    all_items = sorted(set(items))
    non_evidence = [p for p in all_items if not p.startswith(EVIDENCE_PREFIX)]
    evidence = [p for p in all_items if p.startswith(EVIDENCE_PREFIX)]
    groups = classify(all_items)
    name_of = {no: name for no, name, _r, _w in GROUP_RULES}
    order_of = {path: no for no, paths in groups.items() for path in paths}

    # 引用面：只看"这组里会落进未提交集合的引用"，否则每个对已跟踪源码的引用都是噪声。
    refs: dict[int, set[str]] = {no: set() for no in groups}
    for no, paths in groups.items():
        for path in paths:
            text = read_text(path)
            if not text:
                continue
            found = reference_targets(text, existing) | expand_migration(existing, text, existing)
            refs[no].update(p for p in found if p in order_of and p != path)

    violations = []
    # 1) 证据不得进入任何分组。classify 会跳过证据路径，所以"分组里没有证据"是**构造出来的真**，
    #    那样这条判定永远不响（等于空转）。真正要问的是：**任何一条规则是否可能捞进证据** ——
    #    有人把 `docs/enterprise-platform/` 写成前缀就会命中，所以逐条规则对证据路径再判一次。
    for path in evidence:
        for no, name, rule, _why in GROUP_RULES:
            if rule(path):
                violations.append({"rule": "evidence-must-not-be-committed", "group": name,
                                   "paths": [path],
                                   "detail": f"组{name} 的规则会捞到证据文件 {path}：证据按 §3.4 不入库"})
                break
    # 2) 覆盖且仅覆盖一次
    covered = [p for paths in groups.values() for p in paths]
    duplicates = sorted({p for p in covered if covered.count(p) > 1})
    orphans = sorted(set(non_evidence) - set(covered))
    if duplicates:
        violations.append({"rule": "exactly-one-group", "kind": "duplicate", "paths": duplicates})
    if orphans:
        violations.append({"rule": "exactly-one-group", "kind": "unclassified", "paths": orphans})
    # 3) 引用图：A 引用 B 里的未提交工件 ⇒ B 必须在 A **同一次或更早**的提交里落地。
    #    单独 checkout 某组时红不红，取决于这条，而不是取决于我自觉。
    edges: dict[tuple[int, int], set[str]] = {}
    for no, targets in refs.items():
        for t in targets:
            edges.setdefault((no, order_of[t]), set()).add(t)
    sccs = strongly_connected([n for n, *_rest in GROUP_RULES], sorted({(a, b) for a, b in edges}))
    merged_with = {n: c for c in sccs for n in c}
    for cycle in sccs:
        if len(cycle) > 1:
            pairs = sorted({(name_of[a], name_of[b], sorted(edges[(a, b)])[:2][0])
                            for a in cycle for b in cycle if (a, b) in edges})
            violations.append({"rule": "groups-must-merge",
                               "groups": [name_of[n] for n in sorted(cycle)],
                               "detail": "互相引用：不存在能让每一笔单独 checkout 都绿的先后顺序，必须并成一次提交",
                               "examples": pairs[:6]})
    for (a, b), targets in sorted(edges.items()):
        if a == b or merged_with[a] == merged_with[b]:
            continue
        if b > a:
            violations.append({"rule": "reference-order", "group": name_of[a],
                               "referenced_group": name_of[b],
                               "paths": sorted(targets)[:5],
                               "detail": f"{name_of[a]}(组{a}) 引用了 {name_of[b]}(组{b})，"
                                         f"按当前顺序是**前指**：单独 checkout 组{a} 会缺工件"})
    # 4) 规则不得空转：命中面要落在整个仓库（未跟踪 + 已跟踪），而不是"今天恰好有脏文件"。
    #    上一批工件提交之后规则今天命中 0 个脏文件是正常状态；把它判成死规则会在每次成功提交后
    #    把工具自己变红，并诱导"删掉规则"这种错误处置（规则是按路径前缀写的，下一批同类文件仍要归类）。
    #    宇宙用调用方传进来的 existing（真树=已跟踪∪全部条目，夹具=合成集合），
    #    绝不能在这里现场调 git，否则合成用例与真树走的就不是同一条判定。
    universe = set(non_evidence) | set(existing)
    for no, name, rule, _why in GROUP_RULES:
        if not groups[no] and not any(rule(p) for p in universe):
            violations.append({"rule": "dead-group-rule", "group": name,
                               "detail": "该分组规则在全仓库都命中不到文件：分组表已失真或工件被删却没删规则"})

    return {
        "generated_at": datetime.now(timezone.utc).astimezone().isoformat(timespec="seconds"),
        "command": "python scripts/production/plan_commit_groups.py",
        "cwd": str(REPO),
        "worktree": dict(worktree_stats,
                         committable_items=len(non_evidence),
                         items_seen=len(all_items)),
        "groups": [
            {"order": no, "name": name_of[no], "why": next(w for n, _x, _r, w in GROUP_RULES if n == no),
             "count": len(groups[no]), "paths": sorted(groups[no])}
            for no in sorted(groups)
        ],
        "references": {name_of[no]: sorted(refs[no]) for no in sorted(refs) if refs[no]},
        "group_edges": [{"from": name_of[a], "to": name_of[b], "paths": sorted(t)[:6], "count": len(t)}
                        for (a, b), t in sorted(edges.items()) if a != b],
        "violations": violations,
        "violation_counts": {k: sum(1 for v in violations if v["rule"] == k) for k in
                             ("evidence-must-not-be-committed", "exactly-one-group",
                              "reference-order", "groups-must-merge", "dead-group-rule")},
        "production_ready": False,
        "gate3_passed": False,
        "independent_review": False,
        "commits_made_by_this_tool": 0,
    }


def build_plan() -> dict:
    untracked, modified = worktree_items()
    all_items = sorted(set(untracked) | set(modified))
    stats = {
        "untracked_total": len(untracked),
        "evidence_untracked": sum(1 for p in untracked if p.startswith(EVIDENCE_PREFIX)),
        "modified_tracked": len(modified),
        "git_status_uall_lines": len(all_items),
        "git_status_folded_lines": len(git_lines("status", "--porcelain=1")),
    }
    return analyze(all_items, tracked_files() | set(all_items), default_reader, stats)


def emit(text: str) -> None:
    """一律按 UTF-8 写字节到 stdout。

    默认走控制台编码（这台机器是 cp936），中文路径与中文说明会被 GBK 编码后再被我按 UTF-8 读回，
    得到 U+FFFD —— 一份"给人看还行、给机器读会坏"的证据面。工具的门禁输出必须是纯 UTF-8。
    """
    sys.stdout.buffer.write((text + "\n").encode("utf-8", "replace"))


def main(argv=None) -> int:
    ap = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    ap.add_argument("--json", action="store_true", help="输出完整 JSON（含每组文件清单）")
    ap.add_argument("--output", help="把 JSON 写到该路径（UTF-8，无 BOM）")
    ap.add_argument("--quiet", action="store_true", help="只打摘要与违规")
    args = ap.parse_args(argv)

    plan = build_plan()
    payload = json.dumps(plan, ensure_ascii=False, indent=2)
    if args.output:
        Path(args.output).write_text(payload, encoding="utf-8")
    if args.json:
        # --json 必须是**纯** JSON：混进人读摘要的 stdout 没法被机器消费（本次就撞上了）。
        emit(payload)
        return 1 if plan["violations"] else 0
    if not args.quiet:
        w = plan["worktree"]
        emit(f"工作树：uall {w['git_status_uall_lines']} 行（未跟踪 {w['untracked_total']}，"
             f"其中证据 {w['evidence_untracked']}；已跟踪且修改 {w['modified_tracked']}）"
             f"；折叠 {w['git_status_folded_lines']} 行；可提交面 {w['committable_items']} 项")
        for g in plan["groups"]:
            emit(f"  组{g['order']} {g['name']:<20} {g['count']:>4} 项  —— {g['why'][:38]}")
    if plan["violations"]:
        emit(f"[FAIL] 违规 {len(plan['violations'])} 条")
        for v in plan["violations"][:20]:
            emit("  - " + json.dumps(v, ensure_ascii=False)[:170])
        return 1
    emit("[OK] 分组计划自洽：证据不入库、覆盖一次、引用不回指、规则不空转")
    return 0


if __name__ == "__main__":
    sys.exit(main())
