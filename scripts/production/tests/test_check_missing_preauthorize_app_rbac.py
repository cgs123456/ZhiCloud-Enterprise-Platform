"""会员端授权守卫（[APP-RBAC]）的行为测试。

背景：会员端 controller/app/** 曾被挂上后台 RBAC 权限点（@ss.hasPermission），
而 member_user 在权限模型里结构上拿不到 system_role_menu —— 接口对会员恒 403。
本轮把 17 个文件 35 处改成 isAuthenticated() 后，用这条静态守卫防止回归。

守卫的作用域是"会员侧模块"而不是"任何 controller/app"：WMS 的 PDA 接口也在
controller/app/pda 下，但它的登录态是后台用户，挂 @ss.hasPermission 才是正确的。
下面特意把这条边界钉成用例（test_pda_*），防止守卫泛化成假红或反过来被整块拆掉。
"""
import importlib.util
import io
import os
import sys
import tempfile
import textwrap
import unittest
from contextlib import redirect_stdout
from unittest import mock

# .../zhicloud-audit-src2/scripts/production/tests/<本文件> -> 仓库根要上溯 4 层
ROOT_DIR = os.path.dirname(os.path.dirname(os.path.dirname(
    os.path.dirname(os.path.abspath(__file__)))))
SCRIPT = os.path.join(ROOT_DIR, 'scripts', 'check_missing_preauthorize.py')

BODY_TMPL = """
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
@RequestMapping("%s")
public class ProbeController {

    @PostMapping("/add")
    %s
    public String add() { return "ok"; }
}
"""

ADMIN_ANNO = '@PreAuthorize("@ss.hasPermission(\'trade:cart:create\')")'
BOTH_ANNO = ('@PreAuthorize("@ss.hasAnyPermissions(\'trade:cart:update\', \'trade:cart:create\')")\n'
             '    ' + ADMIN_ANNO)
AUTH_ANNO = '@PreAuthorize("isAuthenticated()")'

MEMBER_APP_REL = 'zhicloud-module-mall/zhicloud-module-trade/src/main/java/cn/zhicloud/module/trade/controller/app/cart'
MEMBER_APP_REL2 = 'zhicloud-module-member/src/main/java/cn/zhicloud/module/member/controller/app/user'
ADMIN_REL = 'zhicloud-module-mall/zhicloud-module-trade/src/main/java/cn/zhicloud/module/trade/controller/admin/cart'
PDA_REL = 'zhicloud-module-wms/src/main/java/cn/zhicloud/module/wms/controller/app/pda'


def load_module():
    spec = importlib.util.spec_from_file_location('cmp_under_test', SCRIPT)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def call_main(module, argv):
    """main() 直接读 sys.argv（不接参数），所以用 argv 注入而不是传参。"""
    with mock.patch.object(sys, 'argv', ['check_missing_preauthorize.py'] + argv):
        return module.main()


class AppRbacGuardTests(unittest.TestCase):

    def run_gate(self, files):
        """files: [(相对包目录, 映射路径, 注解)] —— 每个都带写映射，缺注解会触发 [GAP]，
        所以统一给第二条注解或让它们都带 @PreAuthorize。"""
        module = load_module()
        tmp = tempfile.mkdtemp()
        for rel_dir, mapping, anno in files:
            java_dir = os.path.join(tmp, rel_dir.replace('/', os.sep))
            os.makedirs(java_dir, exist_ok=True)
            body = BODY_TMPL % (mapping, anno)
            with open(os.path.join(java_dir, 'ProbeController.java'), 'w', encoding='utf-8') as f:
                f.write(body)
        # iter_controller_files 读的是模块级 ROOT，测试里把它指向临时仓库
        module.ROOT = tmp
        out = io.StringIO()
        with redirect_stdout(out):
            code = call_main(module, ['--include-app', '--max-gaps', '999'])
        return code, out.getvalue()

    def test_member_app_with_admin_permission_point_is_red(self):
        """会员端挂后台权限点 → 判红，且两条不同写法（hasPermission / hasAnyPermissions）都要抓到。"""
        code, text = self.run_gate([
            (MEMBER_APP_REL, '/trade/cart', ADMIN_ANNO),
            (MEMBER_APP_REL2, '/member/user', BOTH_ANNO),
        ])
        self.assertEqual(1, code, text)
        self.assertIn('[APP-RBAC]', text)
        # 夹具里共 3 处：一个文件 1 处 hasPermission，另一个文件同一方法上 2 处（hasAnyPermissions + hasPermission）
        self.assertIn('3 处', text, text)
        self.assertIn('zhicloud-module-member', text)

    def test_member_app_with_is_authenticated_is_green(self):
        code, text = self.run_gate([(MEMBER_APP_REL, '/trade/cart', AUTH_ANNO)])
        self.assertEqual(0, code, text)
        self.assertNotIn('[APP-RBAC]', text)

    def test_admin_side_is_not_flagged(self):
        """后台 controller/admin 用 @ss.hasPermission 是正确写法，不能被误判。"""
        code, text = self.run_gate([(ADMIN_REL, '/trade/cart', ADMIN_ANNO)])
        self.assertEqual(0, code, text)
        self.assertNotIn('[APP-RBAC]', text)

    def test_pda_app_under_controller_app_is_not_flagged(self):
        """WMS PDA 也在 controller/app 下，但登录态是后台用户：这条边界钉住，
        防止守卫泛化成假红，也防止有人为了过守卫把 PDA 的鉴权拆成 isAuthenticated()。"""
        code, text = self.run_gate([(PDA_REL, '/wms/pda', ADMIN_ANNO)])
        self.assertEqual(0, code, text)
        self.assertNotIn('[APP-RBAC]', text)

    def test_commented_out_permission_point_does_not_count(self):
        """注释里的 @ss.hasPermission 不算违规；若判红说明扫描把注释当代码。"""
        module = load_module()
        tmp = tempfile.mkdtemp()
        d = os.path.join(tmp, MEMBER_APP_REL.replace('/', os.sep))
        os.makedirs(d, exist_ok=True)
        with open(os.path.join(d, 'ProbeController.java'), 'w', encoding='utf-8') as f:
            f.write(BODY_TMPL % ('/trade/cart',
                                 '// @PreAuthorize("@ss.hasPermission(\'trade:cart:create\')")\n    '
                                 + AUTH_ANNO))
        module.ROOT = tmp
        out = io.StringIO()
        with redirect_stdout(out):
            code = call_main(module, ['--include-app', '--max-gaps', '999'])
        self.assertEqual(0, code, out.getvalue())


if __name__ == '__main__':
    unittest.main()
