#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
扫描指定模块的 Controller，找出「写操作」(POST/PUT/DELETE) 方法缺少
方法级 @PreAuthorize 注解的缺口，输出需补注解的清单。

规则：
- 仅关注 HTTP 写方法：@PostMapping / @PutMapping / @DeleteMapping /
  @RequestMapping(method = ...RequestMethod.POST/PUT/DELETE...) 标注的 public 方法。
- 若该方法（或其所在类）已被 @PreAuthorize 标注，则视为已授权，跳过。
- 仅报告缺口，不直接改写代码（人工 review 后补注解，避免错误权限码锁死 RBAC）。

用法：
    python3 scripts/check_missing_preauthorize.py --module yudao-module-wms
    python3 scripts/check_missing_preauthorize.py            # 全仓
    python3 scripts/check_missing_preauthorize.py --module yudao-module-wms --include-app
"""
import argparse
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

WRITE_METHOD_RE = re.compile(
    r'@(?:Post|Put|Delete)Mapping\s*\(|'
    r'@RequestMapping\s*\(\s*value\s*=|'
    r'@RequestMapping\s*\([^)]*method\s*=\s*[^)]*RequestMethod\.(POST|PUT|DELETE)',
    re.IGNORECASE,
)
PREAUTH_RE = re.compile(r'@PreAuthorize\s*\(')
MAPPING_PATH_RE = re.compile(r'@(?:Post|Put|Delete|Request|Get)Mapping\s*\(([^)]*)\)', re.IGNORECASE)
PUBLIC_METHOD_RE = re.compile(r'public\s+[\w<>\[\],\s]+\s+(\w+)\s*\(')


def iter_controller_files(module, include_app):
    if module:
        base = os.path.join(ROOT, module, 'src', 'main', 'java')
        roots = [base] if os.path.isdir(base) else []
    else:
        roots = [ROOT]
    for base in roots:
        for dirpath, _dirs, files in os.walk(base):
            rel_dir = dirpath.replace('\\', '/')
            if 'controller' not in rel_dir.split('/'):
                continue
            for fn in files:
                if not fn.endswith('Controller.java'):
                    continue
                rel = os.path.relpath(os.path.join(dirpath, fn), ROOT).replace('\\', '/')
                if not include_app and '/app/' in rel:
                    continue
                yield os.path.join(dirpath, fn), rel


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--module', default=None, help='限定模块目录名，如 yudao-module-wms；空=全仓')
    ap.add_argument('--include-app', action='store_true', help='是否纳入 controller/app (PDA/移动端)')
    ap.add_argument('--root', default=ROOT)
    args = ap.parse_args()

    gaps = []
    scanned = 0
    for path, rel in iter_controller_files(args.module, args.include_app):
        scanned += 1
        with open(path, 'r', encoding='utf-8', errors='ignore') as f:
            lines = f.readlines()

        # 类级 @PreAuthorize？
        class_level_preauth = any(PREAUTH_RE.search(l) for l in lines[:60])

        i = 0
        n = len(lines)
        while i < n:
            line = lines[i]
            if WRITE_METHOD_RE.search(line) or (
                '@RequestMapping' in line and re.search(r'RequestMethod\.(POST|PUT|DELETE)', line)
            ):
                # 注解块可能在 @PostMapping 上方或下方（本项目常见顺序：@PostMapping/@Operation/@PreAuthorize/public）
                # 故在 [i-2, i+8] 窗口内查找 @PreAuthorize 与方法名
                lo, hi = max(0, i - 2), min(n, i + 9)
                has_preauth = class_level_preauth
                method_name = '(?)'
                for k in range(lo, hi):
                    if PREAUTH_RE.search(lines[k]):
                        has_preauth = True
                    m = PUBLIC_METHOD_RE.search(lines[k])
                    if m:
                        method_name = m.group(1)
                if has_preauth:
                    i += 1
                    continue
                # 取映射路径
                path_txt = ''
                mp = MAPPING_PATH_RE.search(line)
                if mp:
                    path_txt = mp.group(1)[:60]
                gaps.append((rel, i + 1, method_name, path_txt.strip()))
            i += 1

    if not gaps:
        print(f'[OK] 未发现缺失 @PreAuthorize 的写接口（扫描 {scanned} 个 Controller 文件）。')
        return 0

    print(f'[GAP] 发现 {len(gaps)} 个写接口缺少 @PreAuthorize（扫描 {scanned} 个 Controller）：')
    for rel, ln, name, p in gaps:
        print(f'  {rel}:{ln}  {name}  {p}')
    return 1


if __name__ == '__main__':
    sys.exit(main())
