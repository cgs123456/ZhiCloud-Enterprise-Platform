#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
扫描 Controller，找出「写操作」(POST/PUT/PATCH/DELETE) 方法缺少方法级 @PreAuthorize
或被 @PermitAll 匿名放行的缺口。

规则：
- 关注 HTTP 写方法：@PostMapping / @PutMapping / @PatchMapping / @DeleteMapping /
  @RequestMapping(method = ...RequestMethod.POST/PUT/PATCH/DELETE...)。
- 若该方法（或其所在类）已被 @PreAuthorize 标注，则视为已授权。
- 强制检查 @PermitAll：它会让接口完全绕过登录态校验，即使同时写了 @PreAuthorize 也形同虚设，
  因此写接口上出现 @PermitAll 一律判为高危缺口，仅允许 scripts/permitall-allowlist.txt 显式登记。
- 仅报告缺口，不改写代码（人工 review 后补注解，避免错误权限码锁死 RBAC）。

用法：
    python3 scripts/check_missing_preauthorize.py --include-app            # 全仓（CI 用法）
    python3 scripts/check_missing_preauthorize.py --module zhicloud-module-wms --include-app

注意：`--include-app` 必须开启，否则 controller/app 下的 PDA / 移动端接口不会被扫描——
      历史上正是这个盲区让 WmsPda*Controller 的类级 @PermitAll 长期未被发现。

准确性设计（这些都是本脚本曾经踩过的坑，改动前请先想清楚）：
  1. 先剥离注释再匹配。否则 Javadoc 里写一句「已加 @PreAuthorize」就能让整个文件免检。
  2. 类级注解只取 class 声明行之前的部分。此前用 lines[:60] 无条件扫描，
     第一个方法的 @PreAuthorize 会被误判为类级，导致该文件后面所有裸接口全部放行。
  3. 方法注解块按「上界=上一个成员结束，下界=本方法签名行」精确定界，
     而非固定 [i-2, i+8] 窗口——固定窗口在短方法处会串读到下一个方法的注解。
"""
import argparse
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ALLOWLIST_FILE = os.path.join(ROOT, 'scripts', 'permitall-allowlist.txt')

WRITE_METHOD_RE = re.compile(
    r'@(?:Post|Put|Patch|Delete)Mapping\b|'
    r'@RequestMapping\s*\([^)]*method\s*=\s*[^)]*RequestMethod\.(?:POST|PUT|PATCH|DELETE)',
)
PREAUTH_RE = re.compile(r'@PreAuthorize\s*\(')
# @PermitAll 允许匿名访问，出现在写接口上等同于「未鉴权公开写接口」
PERMITALL_RE = re.compile(r'@PermitAll\b')
# 会员端接口（controller/app/**）不得挂后台 RBAC 权限点：
# 会员用户（member_user）在权限模型里结构上拿不到 system_role_menu，写 @ss.hasPermission
# 只会让接口对会员恒 403 —— 这是设计错配而不是"权限没配全"，所以按零容忍处理。
SS_PERMISSION_RE = re.compile(r'@ss\.has(?:Any)?Permissions?\s*\(')
# 会员侧模块（其 controller/app 面向 member_user）：mall 四个子模块、member、pay。
# 判定范围按模块而不是按目录，是因为 WMS 的 PDA 接口也在 controller/app 下，但它是后台用户登录态。
MEMBER_SIDE_MODULE_PREFIXES = (
    'zhicloud-module-member/',
    'zhicloud-module-mall/',
    'zhicloud-module-pay/',
)
MAPPING_PATH_RE = re.compile(r'@(?:Post|Put|Patch|Delete|Request)Mapping\s*\(([^)]*)\)')
# 方法签名：允许泛型、数组、注解修饰的返回类型
METHOD_SIG_RE = re.compile(r'^\s*(?:public|protected|private)\s+[\w<>\[\],.\s?]+\s+(\w+)\s*\(')
CLASS_DECL_RE = re.compile(r'^\s*(?:public\s+|final\s+|abstract\s+)*(?:class|interface|enum)\s+\w+')
CONTROLLER_ANNO_RE = re.compile(r'@(?:Rest)?Controller\b')


def strip_comments(lines):
    """剥离 // 行注释与 /* */ 块注释，保持行数与行号一一对应。

    只做词法级剥离，不处理字符串字面量里的 // ——权限注解不会出现在字符串里，
    误差可接受，且宁可保留也不要误删导致漏报。
    """
    out = []
    in_block = False
    for raw in lines:
        line = raw
        if in_block:
            end = line.find('*/')
            if end == -1:
                out.append('\n')
                continue
            line = ' ' * (end + 2) + line[end + 2:]
            in_block = False
        # 处理本行内可能出现的多个块注释
        while True:
            start = line.find('/*')
            if start == -1:
                break
            end = line.find('*/', start + 2)
            if end == -1:
                line = line[:start]
                in_block = True
                break
            line = line[:start] + ' ' * (end + 2 - start) + line[end + 2:]
        pos = line.find('//')
        if pos != -1:
            line = line[:pos]
        out.append(line if line.endswith('\n') else line + '\n')
    return out


def iter_controller_files(module, include_app):
    if module:
        # 模块可能是聚合目录（如 zhicloud-module-iot 下才是 zhicloud-module-iot-biz/src/...），
        # 所以先试 <module>/src/main/java，不存在就回退到整个 <module> 目录，
        # 避免「路径写错 → 扫 0 个文件 → 永远 [OK]」的空转假绿。
        base = os.path.join(ROOT, module, 'src', 'main', 'java')
        roots = [base] if os.path.isdir(base) else [os.path.join(ROOT, module)]
    else:
        roots = [ROOT]
    seen = set()
    for base in roots:
        for dirpath, dirs, files in os.walk(base):
            dirs[:] = [d for d in dirs if d not in ('target', 'node_modules', '.git')]
            for fn in files:
                if not fn.endswith('.java'):
                    continue
                full = os.path.join(dirpath, fn)
                rel = os.path.relpath(full, ROOT).replace('\\', '/')
                if not include_app and '/app/' in rel:
                    continue
                # 不按「路径里必须有 controller/」筛文件，也不只认 *Controller.java：
                # 以类上是否真的标注了 @RestController/@Controller 为准。
                # 历史盲区：IoTDeviceApiImpl 位于 .../api/device/（无 controller 段）且不以
                # Controller.java 结尾，它通过 /rpc-api 暴露 5 个 @PermitAll 写接口，长期在
                # 本门禁视野之外（handoff §12.168/§12.170）。改成注解判定后这类文件不再逃逸。
                # 命中判定前先剥离注释，避免 Javadoc/import 里出现 @RestController 字样造成误收。
                if not fn.endswith('Controller.java'):
                    try:
                        with open(full, 'r', encoding='utf-8', errors='ignore') as f:
                            head = ''.join(strip_comments(f.readlines())[:80])
                    except OSError:
                        continue
                    if not CONTROLLER_ANNO_RE.search(head):
                        continue
                if rel in seen:
                    continue
                seen.add(rel)
                yield full, rel


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


def find_class_decl(lines):
    """返回 class/interface/enum 声明所在行号；找不到返回 0。"""
    for idx, line in enumerate(lines):
        if CLASS_DECL_RE.match(line):
            return idx
    return 0


def method_block_bounds(lines, i, class_start):
    """给定写映射注解所在行 i，返回该方法注解块的 [start, end] 闭区间与方法名。

    start：向上回溯到上一个成员结束（遇到 } 、; 或方法签名即停），保证不串读上一个方法。
    end  ：向下找到本方法签名行为止，保证不串读下一个方法。
    """
    start = i
    k = i - 1
    while k > class_start:
        s = lines[k].strip()
        if not s:
            k -= 1
            continue
        # 上一个成员的结束标志
        if s.endswith('}') or s.endswith(';') or METHOD_SIG_RE.match(lines[k]):
            break
        start = k
        k -= 1

    end = i
    name = '(?)'
    n = len(lines)
    k = i
    while k < n and k < i + 40:
        m = METHOD_SIG_RE.match(lines[k])
        if m:
            end = k
            name = m.group(1)
            break
        end = k
        k += 1
    return start, end, name


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--module', default=None, help='限定模块目录名，如 zhicloud-module-wms；空=全仓')
    ap.add_argument('--include-app', action='store_true', help='是否纳入 controller/app (PDA/移动端)')
    ap.add_argument('--root', default=ROOT)
    # 棘轮基线：[CRITICAL]（@PermitAll 匿名写）永远零容忍，与本参数无关；
    # [GAP]（缺 @PreAuthorize，但仍需登录）是历史存量，全仓一次性清零不现实，
    # 故用基线卡住「只降不升」：超过基线即失败，低于基线则提示下调基线。
    ap.add_argument('--max-gaps', type=int, default=0,
                    help='允许的 [GAP] 存量上限（默认 0）；[CRITICAL] 不受此参数影响，恒为零容忍')
    args = ap.parse_args()

    allowlist = load_allowlist()
    gaps = []
    permit_all_gaps = []
    app_rbac_gaps = []
    scanned = 0
    for path, rel in iter_controller_files(args.module, args.include_app):
        scanned += 1
        with open(path, 'r', encoding='utf-8', errors='ignore') as f:
            lines = strip_comments(f.readlines())

        # 只判「会员端」：controller/app 下还住着 PDA 这类以**后台用户**登录的接口
        # （zhicloud-module-wms 的 controller/app/pda 就是），它们挂 @ss.hasPermission 是正确写法，
        # 一律按 app 判会把合法用法打成红，也会诱导别人把 PDA 的鉴权一起拆掉。
        if any(rel.startswith(p) for p in MEMBER_SIDE_MODULE_PREFIXES) and '/controller/app/' in rel:
            # 会员端接口挂后台权限点，属结构错配而非"漏配权限"（见文件头注释）
            for idx, line in enumerate(lines, start=1):
                if SS_PERMISSION_RE.search(line):
                    app_rbac_gaps.append((rel, idx))

        class_start = find_class_decl(lines)
        # 类级注解：严格取 class 声明行之前的部分
        header = lines[:class_start] if class_start else []
        class_level_preauth = any(PREAUTH_RE.search(l) for l in header)
        class_level_permitall = any(PERMITALL_RE.search(l) for l in header)

        i = class_start
        n = len(lines)
        while i < n:
            if not WRITE_METHOD_RE.search(lines[i]):
                i += 1
                continue

            start, end, method_name = method_block_bounds(lines, i, class_start)
            block = lines[start:end + 1]
            has_preauth = class_level_preauth or any(PREAUTH_RE.search(l) for l in block)
            has_permitall = class_level_permitall or any(PERMITALL_RE.search(l) for l in block)

            path_txt = ''
            mp = MAPPING_PATH_RE.search(lines[i])
            if mp:
                path_txt = mp.group(1)[:60]

            # @PermitAll 优先判定：它会让接口彻底跳过登录态，比"缺少 @PreAuthorize"更危险
            if has_permitall and f'{rel}#{method_name}' not in allowlist:
                permit_all_gaps.append((rel, i + 1, method_name, path_txt.strip()))
            elif not has_preauth and not has_permitall:
                gaps.append((rel, i + 1, method_name, path_txt.strip()))

            i = max(end, i) + 1

    if not args.include_app:
        print('[WARN] 未开启 --include-app，controller/app（PDA/移动端）未纳入扫描；CI 应始终传该参数。')

    # 空扫 fail-closed：扫描目标写错（模块名不存在、目录结构变化）时必须报错，
    # 否则「0 个文件 + 0 个缺口」会被读成「全仓干净」。
    if scanned == 0:
        target = args.module or '全仓'
        print(f'[ERROR] 扫描 {target} 时匹配到 0 个 Controller 文件——扫描目标或目录结构可能写错，'
              f'按 fail-closed 处理（不视为通过）。')
        return 1

    if not gaps and not permit_all_gaps and not app_rbac_gaps:
        print(f'[OK] 未发现缺失 @PreAuthorize / 违规 @PermitAll 的写接口（扫描 {scanned} 个 Controller 文件）。')
        return 0

    failed = False

    if app_rbac_gaps:
        # 与 @PermitAll 同级：恒零容忍，不受 --max-gaps 棘轮影响
        print(f'[APP-RBAC] 发现 {len(app_rbac_gaps)} 处会员端（controller/app）接口使用后台 RBAC 权限点'
              f'（扫描 {scanned} 个 Controller 文件）：')
        for rel, ln in app_rbac_gaps:
            print(f'  {rel}:{ln}')
        print('  → 会员端应使用 @PreAuthorize("isAuthenticated()")：会员用户结构上拿不到菜单权限，'
              '挂 @ss.hasPermission 只会让接口对会员恒 403。')
        failed = True

    if permit_all_gaps:
        # 匿名可写 = 阻断级，任何情况下都失败，不受 --max-gaps 影响
        print(f'[CRITICAL] 发现 {len(permit_all_gaps)} 个写接口被 @PermitAll 放行为匿名可访问'
              f'（扫描 {scanned} 个 Controller）：')
        for rel, ln, name, p in permit_all_gaps:
            print(f'  {rel}:{ln}  {name}  {p}')
        print('  → 请移除 @PermitAll 并补 @PreAuthorize；确需匿名的接口请登记到 scripts/permitall-allowlist.txt')
        failed = True

    if gaps:
        print(f'[GAP] 发现 {len(gaps)} 个写接口缺少 @PreAuthorize（扫描 {scanned} 个 Controller），'
              f'基线上限 {args.max_gaps}：')
        for rel, ln, name, p in gaps:
            print(f'  {rel}:{ln}  {name}  {p}')
        if len(gaps) > args.max_gaps:
            print(f'  → [GAP] 数量 {len(gaps)} 超过基线 {args.max_gaps}，判定为回归。'
                  f'请为新增写接口补 @PreAuthorize，不要上调基线。')
            failed = True
        else:
            print(f'  → 以上为基线内的历史存量（{len(gaps)}/{args.max_gaps}），本次不判失败。')

    if not failed and gaps and len(gaps) < args.max_gaps:
        # 棘轮：存量被清理后必须立刻收紧，否则基线会变成永久免死金牌
        print(f'[RATCHET] [GAP] 已降至 {len(gaps)}，请把 CI 中的 --max-gaps 下调为 {len(gaps)}。')

    return 1 if failed else 0


if __name__ == '__main__':
    sys.exit(main())
