#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
错误码全量重排：将冲突（重复）的错误码数字改为全局唯一值，常量名保持不变。

策略：
- 解析全仓 *ErrorCodeConstants.java，得到每个常量 (file, line, name, code, module, prefix)。
- 对每个冲突码 C（多常量共享同一数字）：
  - 保留其中一个常量原值不动；其余常量重新分配一个新数字。
  - 跨模块冲突（如 MES 与 QMS 共用前缀 1_040）的"被移动"模块，分配一个
    全新的空闲前缀（保留模块标识），其所有冲突常量落到该前缀下。
  - 模块内重复（同模块多常量同码）的额外常量，在本模块原前缀下找空闲数字。
- 仅替换常量定义行的数字字面量（按精确行号 + 原码匹配），不影响任何按
  常量名引用的调用方；测试若按 name 引用也自动跟随。唯一需手工同步的是按
  数字字面量断言的测试（本仓仅 1 处，且其码不在冲突列表，故不受影响）。
- 重排后全仓错误码全局唯一，可供 CI 升级为全量严格检查。

用法：
    python3 scripts/renumber_error_codes.py --dry-run    # 预览映射，不改动文件
    python3 scripts/renumber_error_codes.py              # 执行重排
"""
import argparse
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CODE_RE = re.compile(r'\bnew\s+ErrorCode\(\s*(-?\d[\d_]*)\s*,')
NAME_LINE_RE = re.compile(r'([A-Z][A-Z0-9_]*)\s*=\s*new\s+ErrorCode\(')
NAME_PREV_RE = re.compile(r'([A-Z][A-Z0-9_]*)\s*=\s*$')


def fmt(code: int) -> str:
    s = str(code)
    return f"{s[0]}_{s[1:4]}_{s[4:7]}_{s[7:10]}"


def iter_constants():
    for dirpath, _dirs, files in os.walk(ROOT):
        rel = dirpath.replace(os.sep, '/')
        if '/target/' in rel:
            continue
        for fn in files:
            if fn.endswith('ErrorCodeConstants.java'):
                yield os.path.join(dirpath, fn)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--dry-run', action='store_true', help='仅预览映射，不写文件')
    args = ap.parse_args()

    # 1) 解析全部常量（逐行，记录精确行号，名称从本行（或上一行）提取）
    records = []  # dict(file, rel, line, name, raw, code, module, prefix)
    used_codes = set()
    used_prefixes = set()
    for path in iter_constants():
        rel = os.path.relpath(path, ROOT).replace(os.sep, '/')
        module = rel.split('/')[0]
        with open(path, 'r', encoding='utf-8', newline='') as f:
            content = f.read()
        lines = content.splitlines(keepends=True)
        prev_name = None
        for li, line in enumerate(lines):
            for m in CODE_RE.finditer(line):
                raw = m.group(1)
                code = int(raw)
                nm = NAME_LINE_RE.search(line)
                name = nm.group(1) if nm else (prev_name if NAME_PREV_RE.search(lines[li - 1]) else None)
                prefix = int(str(code)[1:4]) if len(str(code)) >= 10 else None
                records.append({'file': path, 'rel': rel, 'line': li,
                                'name': name, 'raw': raw, 'code': code,
                                'module': module, 'prefix': prefix})
                used_codes.add(code)
                if prefix is not None:
                    used_prefixes.add(prefix)
            # 更新上一行名称（供下一行多行定义回退）
            nm = NAME_LINE_RE.search(line)
            prev_name = nm.group(1) if nm else (prev_name if NAME_PREV_RE.search(line) else None)

    # 2) 冲突分组：code -> [records]
    conflicts = {}
    for r in records:
        conflicts.setdefault(r['code'], []).append(r)
    conflicts = {c: v for c, v in conflicts.items() if len(v) > 1}

    # 3) 为跨模块冲突的"被移动"模块分配全新空闲前缀
    new_prefix_of_module = {}
    for c, recs in conflicts.items():
        mods = []
        for r in recs:
            if r['module'] not in mods:
                mods.append(r['module'])
        if len(mods) > 1:
            for mod in mods[1:]:
                if mod not in new_prefix_of_module:
                    p = 43
                    while p in used_prefixes or p in new_prefix_of_module.values():
                        p += 1
                    new_prefix_of_module[mod] = p
                    used_prefixes.add(p)

    # 4) 生成重排映射（按精确行号定位，避免误改其它常量）
    mapping = []  # (rel, line, old_raw, new_raw)
    for c, recs in conflicts.items():
        for idx, r in enumerate(recs):
            if idx == 0:
                continue
            target_prefix = new_prefix_of_module.get(r['module'], r['prefix'])
            a = b = 0
            while True:
                cand = 1_000_000_000 + target_prefix * 1_000_000 + a * 1000 + b
                if cand not in used_codes:
                    break
                b += 1
                if b >= 1000:
                    b = 0
                    a += 1
            used_codes.add(cand)
            new_raw = fmt(cand)
            mapping.append((r['rel'], r['line'], r['raw'], new_raw))

    if args.dry_run:
        print(f"[DRY-RUN] 冲突码 {len(conflicts)} 个，需重排常量 {len(mapping)} 个；"
              f"跨模块移动模块新前缀: {new_prefix_of_module}")
        for rel, line, old, new in mapping:
            print(f"  {rel}:{line + 1}  {old} -> {new}")
        return 0

    # 5) 写入文件（按 file 聚合，按精确行号替换数字字面量）
    by_file = {}
    for rel, line, old, new in mapping:
        by_file.setdefault(rel, []).append((line, old, new))
    changed_files = 0
    for rel, items in by_file.items():
        path = os.path.join(ROOT, rel)
        with open(path, 'r', encoding='utf-8', newline='') as f:
            lines = f.readlines()
        file_changed = False
        for line, old, new in items:
            if old in lines[line]:
                lines[line] = lines[line].replace(old, new, 1)
                file_changed = True
        if file_changed:
            with open(path, 'w', encoding='utf-8', newline='') as f:
                f.writelines(lines)
            changed_files += 1
    print(f"[DONE] 已重排 {len(mapping)} 个常量，写入 {changed_files} 个文件。"
          f"跨模块移动模块新前缀: {new_prefix_of_module}")
    return 0


if __name__ == '__main__':
    sys.exit(main())
