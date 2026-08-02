#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
扫描指定模块的 Controller，找出「写操作」(POST/PUT/DELETE) 方法缺少
方法级 @PreAuthorize 注解的缺口，输出需补注解的清单。

规则：
- 仅关注 HTTP 写方法：@PostMapping / @PutMapping / @DeleteMapping /
  @RequestMapping(method = ...RequestMethod.POST/PUT/DELETE...) 标注的 public 方法。
- 若该方法（或其所在类）已被 @PreAuthorize 标注，则视为已授权，跳过。
- 另外强制检查 @PermitAll：@PermitAll 会让接口完全绕过登录态校验，
  即使同时写了 @PreAuthorize 也形同虚设，因此写接口上出现 @PermitAll 一律判为高危缺口，
  仅允许 scripts/permitall-allowlist.txt 中显式登记的白名单（如登录接口）。
- 仅报告缺口，不直接改写代码（人工 review 后补注解，避免错误权限码锁死 RBAC）。

用法：
    python3 scripts/check_missing_preauthorize.py --module yudao-module-wms --include-app
    python3 scripts/check_missing_preauthorize.py --include-app   # 全仓（推荐 CI 用法）
    python3 scripts/check_missing_preauthorize.py --module yudao-module-wms

注意：`--include-app` 必须开启，否则 controller/app 下的 PDA / 移动端接口不会被扫描——
      历史上正是这个盲区让 WmsPda*Controller 的类级 @PermitAll 长期未被发现。
"""
import argparse
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ALLOWLIST_FILE = os.path.join(ROOT, 'scripts', 'permitall-allowlist.txt')

WRITE_METHOD_RE = re.compile(
    r'@(?:Post|Put|Delete)Mapping\s*\(|'
    r'@RequestMapping\s*\(\s*value\s*=|'
    r'@RequestMapping\s*\([^)]*method\s*=\s*[^)]*RequestMethod\.(POST|PUT|DELETE)',
    re.IGNORECASE,
)
PREAUTH_RE = re.compile(r'@PreAuthorize\s*\(')
# @PermitAll 允许匿名访问，出现在写接口上等同于「未鉴权公开写接口」
PERMITALL_RE = re.compile(r'^\s*@PermitAll\b')
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


def load_allowlist():
    """读取 @PermitAll 白名单：每行 `相对路径#方法名`，# 开头整行为注释。"""
    allow = set()
    if not os.path.isfile(ALLOWLIST_FILE):
        return allow
    with open(ALLOWLIST_FILE, 'r', encoding='utf-8') as f:
        for raw in f:
            line = raw.strip()
            if not line or line.startswith('#'):
                continue
            allow.add(line)
    return allow


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--module', default=None, help='限定模块目录名，如 yudao-module-wms；空=全仓')
    ap.add_argument('--include-app', action='store_true', help='是否纳入 controller/app (PDA/移动端)')
    ap.add_argument('--root', default=ROOT)
    args = ap.parse_args()

    allowlist = load_allowlist()
    gaps = []
    permit_all_gaps = []
    scanned = 0
    for path, rel in iter_controller_files(args.module, args.include_app):
        scanned += 1
        with open(path, 'r', encoding='utf-8', errors='ignore') as f:
            lines = f.readlines()

        # 类级 @PreAuthorize？
        class_level_preauth = any(PREAUTH_RE.search(l) for l in lines[:60])
        # 类级 @PermitAll？（出现在 class 声明之前即视为类级）
        class_level_permitall = False
        for l in lines[:80]:
            if PERMITALL_RE.search(l):
                class_level_permitall = True
            if re.match(r'\s*public\s+class\s+', l):
                break

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
                has_permitall = class_level_permitall
                method_name = '(?)'
                for k in range(lo, hi):
                    if PREAUTH_RE.search(lines[k]):
                        has_preauth = True
                    if PERMITALL_RE.search(lines[k]):
                        has_permitall = True
                    m = PUBLIC_METHOD_RE.search(lines[k])
                    if m:
                        method_name = m.group(1)
                # 取映射路径
                path_txt = ''
                mp = MAPPING_PATH_RE.search(line)
                if mp:
                    path_txt = mp.group(1)[:60]
                # @PermitAll 优先判定：它会让接口彻底跳过登录态，比"缺少 @PreAuthorize"更危险
                if has_permitall and f'{rel}#{method_name}' not in allowlist:
                    permit_all_gaps.append((rel, i + 1, method_name, path_txt.strip()))
                    i += 1
                    continue
                if has_preauth or has_permitall:
                    i += 1
                    continue
                gaps.append((rel, i + 1, method_name, path_txt.strip()))
            i += 1

    if not args.include_app:
        print('[WARN] 未开启 --include-app，controller/app（PDA/移动端）未纳入扫描；CI 应始终传该参数。')

    if not gaps and not permit_all_gaps:
        print(f'[OK] 未发现缺失 @PreAuthorize / 违规 @PermitAll 的写接口（扫描 {scanned} 个 Controller 文件）。')
        return 0

    if permit_all_gaps:
        print(f'[CRITICAL] 发现 {len(permit_all_gaps)} 个写接口被 @PermitAll 放行为匿名可访问'
              f'（扫描 {scanned} 个 Controller）：')
        for rel, ln, name, p in permit_all_gaps:
            print(f'  {rel}:{ln}  {name}  {p}')
        print('  → 请移除 @PermitAll 并补 @PreAuthorize；确需匿名的接口请登记到 scripts/permitall-allowlist.txt')
    if gaps:
        print(f'[GAP] 发现 {len(gaps)} 个写接口缺少 @PreAuthorize（扫描 {scanned} 个 Controller）：')
        for rel, ln, name, p in gaps:
            print(f'  {rel}:{ln}  {name}  {p}')
    return 1


if __name__ == '__main__':
    sys.exit(main())
