#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""R-G3.5-4 只读盘点：evidence/production 的保留物清点与回收候选（不删除）。

背景（§12.14 / §12.43 / 风险登记册 R-G3.5-4）：沙箱与门禁每跑一次就往
`docs/enterprise-platform/evidence/production/` 落一批工件（日志/JSON/SHA 旁车），
按 §3.4 约定不入库。但"保留多久、谁负责、怎么清"没有落地——缺的**前置机械能力**
是本脚本：一个只读盘点，回答
  1) 现有多少、多大、最老到最新、按扩展名与按"日期批次"怎么分档；
  2) 每个文件是否**仍被台账/README 引用**（复用 check_evidence_citations 的同一套
     文档集发现与路径解析，避免两套口径漂移）；
  3) 哪些是"孤儿"（无引用且超出保留期）——**列为回收候选，不自动删**。

红线：本脚本只读。绝不删除/停止任何文件、容器、卷（§5 坑：容器只 stop 不 rm、
口令不进 argv 等在这里不适用——纯本地文件 stat）。owner 仍留具名责任人拍板，
脚本只是把"拍板需要的数字"摆出来。退出码恒 0（盘点不是门禁），除非参数错。
"""
from __future__ import annotations

import argparse
import json
import sys
from datetime import datetime, timedelta
from pathlib import Path

# 复用同一套引用解析，绝不自己再写一份正则（否则与门禁漂移）
sys.path.insert(0, str(Path(__file__).resolve().parent))
import check_evidence_citations as cite  # noqa: E402


def collect_referenced(repo_root: Path, docs_root: Path) -> set[str]:
    """返回被任一文档引用、且在本机存在解析成功的证据文件（仓库相对 posix 路径集合）。

    docs_root 从传入仓库推导（而不是门禁默认的 cite.DOCS_ROOT），否则临时仓库/CI 检出
    盘点时扫到的是错的文档集——本函数与门禁共享"解析器"，但文档集必须随仓库走。
    """
    doc_files = sorted(p for p in docs_root.rglob("*.md") if p.is_file())
    referenced: set[str] = set()
    for c in cite.scan_docs(doc_files, repo_root):
        if c.get("directory"):
            continue
        for cand in cite.candidate_paths(c["raw"], docs_root, repo_root):
            if cand.is_file():
                try:
                    referenced.add(cand.resolve().relative_to(repo_root).as_posix())
                except ValueError:
                    referenced.add(cand.resolve().as_posix())
                break
    return referenced


def main(argv: list[str] | None = None) -> int:
    ap = argparse.ArgumentParser(description="evidence 保留物只读盘点（R-G3.5-4）")
    ap.add_argument("--retention-days", type=int, default=14,
                    help="保留期（天）；早于此且无引用的文件列为回收候选，默认 14")
    ap.add_argument("--repo", type=Path, default=cite.REPO, help="仓库根（默认取脚本所在仓库）")
    ap.add_argument("--json", action="store_true", help="输出 JSON")
    args = ap.parse_args(argv)

    repo = args.repo.resolve()
    ev_dir = repo / "docs" / "enterprise-platform" / "evidence" / "production"
    if not ev_dir.is_dir():
        sys.stdout.buffer.write(f"[SKIP] 证据目录不存在：{ev_dir}\n".encode("utf-8"))
        return 0

    referenced = collect_referenced(repo, (repo / "docs" / "enterprise-platform"))
    cutoff = datetime.now().timestamp() - timedelta(days=args.retention_days).total_seconds()
    files = sorted(p for p in ev_dir.rglob("*") if p.is_file())
    ext: dict[str, int] = {}
    by_batch: dict[str, dict] = {}
    orphans_within = 0
    candidates = []
    for p in files:
        rel = p.relative_to(repo).as_posix()
        e = p.suffix or "(noext)"
        ext[e] = ext.get(e, 0) + 1
        day = datetime.fromtimestamp(p.stat().st_mtime).strftime("%Y-%m-%d")
        b = by_batch.setdefault(day, {"files": 0, "bytes": 0, "referenced": 0, "unref": 0})
        b["files"] += 1
        b["bytes"] += p.stat().st_size
        ref = rel in referenced or str(p.resolve()) in referenced
        if ref:
            b["referenced"] += 1
        else:
            b["unref"] += 1
            if p.stat().st_mtime < cutoff:
                candidates.append({"path": rel, "bytes": p.stat().st_size,
                                   "mtime": day, "reason": "no-citation + older-than-retention"})
            else:
                orphans_within += 1

    total = len(files)
    total_bytes = sum(p.stat().st_size for p in files)
    report = {
        "tool": "evidence_retention_inventory.py",
        "mode": "READ-ONLY (no deletion, no container/volume touch)",
        "evidence_dir": ev_dir.relative_to(repo).as_posix(),
        "retention_days": args.retention_days,
        "total_files": total,
        "total_bytes": total_bytes,
        "referenced_files": len([f for f in files
                                 if f.relative_to(repo).as_posix() in referenced
                                 or str(f.resolve()) in referenced]),
        "orphan_within_retention_not_cleared": orphans_within,
        "recycle_candidates_count": len(candidates),
        "recycle_candidates_bytes": sum(c["bytes"] for c in candidates),
        "by_ext": dict(sorted(ext.items(), key=lambda x: -x[1])),
        "by_date_batch": dict(sorted(by_batch.items())),
        "oldest": min((datetime.fromtimestamp(p.stat().st_mtime).strftime("%Y-%m-%d") for p in files), default=None),
        "newest": max((datetime.fromtimestamp(p.stat().st_mtime).strftime("%Y-%m-%d") for p in files), default=None),
        "recycle_candidates": candidates,
        "note": "candidates 仅为「超过保留期且未被任何文档引用」；删除仍需具名责任人批准"
                "（owner=R-G3.5-4 未定），本脚本不执行删除。",
    }
    if args.json:
        print(json.dumps(report, ensure_ascii=False, indent=2, default=str))
    else:
        print(f"证据目录 {report['evidence_dir']}：{total} 个文件、{total_bytes/1e6:.1f} MB，"
              f"最老 {report['oldest']} / 最新 {report['newest']}")
        print(f"被引用 {report['referenced_files']}；保留期内未清孤儿 {orphans_within}；"
              f"超 {args.retention_days} 天且无引用→回收候选 {len(candidates)} 个 "
              f"({report['recycle_candidates_bytes']/1e6:.1f} MB)")
        print("按扩展名：", ", ".join(f"{k}={v}" for k, v in report['by_ext'].items()))
        print("（只读盘点，未删除任何文件/容器/卷；owner 留 R-G3.5-4 责任人）")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
