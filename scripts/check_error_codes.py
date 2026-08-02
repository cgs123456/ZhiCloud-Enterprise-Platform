#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
扫描全仓 *ErrorCodeConstants.java，检测数字错误码是否重复（跨模块/模块内）。

规则：
- 仅收集形如 `new ErrorCode(<数字>, ...)` 的定义。
- 同一数字出现在不同常量名或不同文件，即视为冲突。
- 通过 --baseline 传入已知冲突清单（每行一个数字）后可「冻结」这些历史冲突，
  仅对新增冲突（不在 baseline 中的重复码）报错——用于在不破坏已有行为的前提下，
  先建立防回归门禁，待错误码去重（依赖 U-6 前端是否硬编码）完成后再收紧为全量严格。

退出码：0 = 无（新增）冲突；1 = 存在冲突（供 CI 作为门禁）。

用法：
    python3 scripts/check_error_codes.py                         # 全量检测，任何重复即失败
    python3 scripts/check_error_codes.py --emit-baseline scripts/error_code_conflicts_baseline.txt
                                                               # 导出当前全部冲突码为 baseline
    python3 scripts/check_error_codes.py --baseline scripts/error_code_conflicts_baseline.txt
                                                               # 仅对 baseline 之外的新增重复失败
"""
import argparse
import os
import re
import sys
from collections import defaultdict

# 匹配 new ErrorCode(123, "...") 或 ErrorCode(123, "...")，支持数字字面量下划线
CODE_RE = re.compile(r'\bnew\s+ErrorCode\(\s*(-?\d[\d_]*)\s*,')
# 常量名（用于报告）：紧邻的 `NAME = new ErrorCode(...)` 或 `NAME(new ErrorCode(...)`
NAME_RE = re.compile(r'([A-Z][A-Z0-9_]*)\s*=\s*(?:new\s+)?ErrorCode\(')

# 排除：全局错误码（0~999 由 GlobalErrorCodeConstants 统一定义，各模块引用而非重定义）
GLOBAL_RANGE_MAX = 999


def iter_constants_files(root):
    for dirpath, _dirs, files in os.walk(root):
        for fn in files:
            if fn.endswith('ErrorCodeConstants.java'):
                yield os.path.join(dirpath, fn)


def load_baseline(path):
    if not path:
        return set()
    s = set()
    if os.path.exists(path):
        with open(path, 'r', encoding='utf-8') as f:
            for line in f:
                line = line.strip()
                if line and not line.startswith('#'):
                    s.add(int(line.replace('_', '')))
    return s


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--root', default=os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
    ap.add_argument('--include-global', action='store_true',
                    help='是否把 0~999 全局码也纳入冲突检测（默认排除，因其为共享定义）')
    ap.add_argument('--baseline', help='已知冲突码基线文件（每行一个数字），其中的重复将被忽略')
    ap.add_argument('--emit-baseline', help='将当前全部冲突码写出到该文件后退出（用于初始化基线）')
    args = ap.parse_args()

    baseline = load_baseline(args.baseline)

    # code -> list of (file, name)
    occurrences = defaultdict(list)
    total = 0
    for path in iter_constants_files(args.root):
        rel = os.path.relpath(path, args.root)
        with open(path, 'r', encoding='utf-8', errors='ignore') as f:
            content = f.read()
        for m in CODE_RE.finditer(content):
            code = int(m.group(1))
            if code <= GLOBAL_RANGE_MAX and not args.include_global:
                continue
            nm = NAME_RE.search(content, max(0, m.start() - 80), m.end())
            name = nm.group(1) if nm else '?'
            occurrences[code].append((rel, name))
            total += 1

    conflicts = {c: v for c, v in occurrences.items() if len(v) > 1}

    if args.emit_baseline:
        with open(args.emit_baseline, 'w', encoding='utf-8') as f:
            for c in sorted(conflicts):
                f.write(f"{c}\n")
        print(f"[BASELINE] 已将 {len(conflicts)} 个冲突码写入 {args.emit_baseline}")
        return 0

    # 仅报告 baseline 之外的新增冲突
    new_conflicts = {c: v for c, v in conflicts.items() if c not in baseline}

    if not new_conflicts:
        frozen = len(conflicts) - len(new_conflicts)
        msg = f"[OK] 无新增重复错误码（共 {total} 个定义"
        if frozen:
            msg += f"；已冻结历史冲突 {frozen} 个，待去重后收紧门禁"
        msg += "）。"
        print(msg)
        return 0

    print(f"[FAIL] 检测到 {len(new_conflicts)} 个新增重复错误码（共 {total} 个定义）：")
    for code in sorted(new_conflicts):
        locs = new_conflicts[code]
        print(f"  码 {code} 出现 {len(locs)} 次：")
        for rel, name in locs:
            print(f"    - {name}  @  {rel}")
    return 1


if __name__ == '__main__':
    sys.exit(main())
