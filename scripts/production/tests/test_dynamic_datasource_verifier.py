"""R-035 / R-036 在动态数据源验证器上的落地断言。

这两条都不是"数值调低让它过"：
- R-036 要求 mvn 参数真的带 -am/-o（否则测的是 ~/.m2 里的旧 jar）；
- R-035 要求期望表数与"库的来历"绑定：默认只允许流水线可复现的 32/6，
  想要 45/7 必须显式声明 manual —— 少声明一项就失败，而不是悄悄放宽。
"""

import re
import unittest
from pathlib import Path

REPO = Path(__file__).resolve().parents[3]
VERIFIER = REPO / 'scripts' / 'production' / 'verify_crm_bpm_dynamic_datasource_mysql.ps1'


class MutationProbeOwnershipTests(unittest.TestCase):
    """变异探针可以不加 -am，但前提必须是"被改文件属于该 -pl 模块"。

    这条归属关系一旦破了（比如以后有人把变异加到上游模块），无 -am 的探针就会去编译/使用
    ~/.m2 里的旧 jar，"变异被测试捕获"的结论会静默失真。所以它必须是机检的，不能靠注释。
    """

    PROBES = {
        'mutation_v97.py': 'ast-tuples',
        'mutation_v99.py': 'single-module',
        'mutation_v100.py': 'single-module',
    }

    def _read(self, name):
        return (REPO / 'scripts' / 'production' / name).read_text(encoding='utf-8')

    def test_v97_every_mutation_targets_its_own_pl_module(self):
        src = self._read('mutation_v97.py')
        paths = dict(re.findall(r'^(sale|pur|mig|ver)\s*=\s*root / "([^"]+)"', src, re.M))
        self.assertEqual(4, len(paths), 'v97 的路径变量没解析全，守卫可能已失效')
        rows = re.findall(r'\("(M\d+)[^"]*",\s*(\w+),.*?"(zhicloud-[\w/-]+)",', src, re.S)
        self.assertGreaterEqual(len(rows), 12, 'v97 变异条目数量异常，归属检查未覆盖全部条目')
        for label, var, module in rows:
            self.assertTrue(paths[var].startswith(module + '/'),
                            f'{label} 改的是 {paths[var]}，但 -pl 是 {module}：必须加 -am 或改 -pl')

    def test_single_module_probes_mutate_inside_that_module(self):
        for name, marker in (('mutation_v99.py', 'zhicloud-module-erp'),
                             ('mutation_v100.py', 'zhicloud-framework/zhicloud-spring-boot-starter-biz-integration')):
            src = self._read(name)
            self.assertIn(f'"-pl", "{marker}"', src, f'{name} 的 -pl 模块变了，请重新核对归属')
            self.assertTrue(any(line.startswith(marker + '/') for line in re.findall(r'root / "([^"]+)"', src)),
                            f'{name} 的被改文件不在 {marker} 内，无 -am 不再成立')

    def test_probes_do_not_carry_am_in_their_maven_arguments(self):
        # v97/v99/v100 不需要 -am（由上两条证明）；v101 需要，因为它跨模块跑。
        # 只看真正拼进命令行的代码部分：`#` 之后的散文不参与判定 ——
        # 让注释措辞决定守卫红绿，是这条断言前两次踩坑的根因。
        def mvn_code(src):
            return [ln.split('#', 1)[0] for ln in src.splitlines() if 'mvn' in ln]
        for name in ('mutation_v97.py', 'mutation_v99.py', 'mutation_v100.py'):
            code = mvn_code(self._read(name))
            self.assertTrue(code, f'{name} 里找不到 mvn 命令行，守卫已失效')
            for ln in code:
                self.assertNotIn('-am', ln, f'{name} 的 mvn 参数含 -am：{ln.strip()[:80]}')
        self.assertIn('"-am"', self._read('mutation_v101.py'),
                      'v101 跨模块跑，其 COMMON_FLAGS 必须带 -am')


class DynamicDatasourceVerifierTests(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        if not VERIFIER.is_file():
            raise unittest.SkipTest(f'missing {VERIFIER}')
        cls.text = VERIFIER.read_text(encoding='utf-8-sig')

    def test_maven_invocation_builds_upstream_from_source(self):
        block = re.search(r'\$mavenArgs = @\((.*?)\)', self.text, re.S)
        self.assertIsNotNone(block, '找不到 $mavenArgs 构造块')
        args = block.group(1)
        self.assertIn("'-am'", args, 'R-036：缺 -am 时上游模块走已安装 jar，不覆盖工作树源码')
        self.assertIn("'-o'", args)
        self.assertIn("'-pl', 'zhicloud-module-crm'", args)

    def test_expected_counts_are_parameters_not_magic_numbers(self):
        self.assertIn('[int]$ExpectedFlowableTables = 32', self.text)
        self.assertIn('[int]$ExpectedPropertyRows = 6', self.text)
        # 魔数不能再出现在断言里
        self.assertNotIn('-ne 45', self.text)
        self.assertNotIn('-ne 7)', self.text)
        self.assertIn('-ne $ExpectedFlowableTables', self.text)
        self.assertIn('-ne $ExpectedPropertyRows', self.text)

    def test_manual_provisioning_must_be_declared_explicitly(self):
        self.assertIn("[ValidateSet('pipeline', 'manual')]", self.text)
        self.assertIn("$SourceProvisioning -eq 'pipeline'", self.text)
        self.assertIn('-SourceProvisioning manual', self.text)
        # 来历必须进证据，否则下一任无法区分两批证据覆盖的库
        self.assertIn("$result['source_provisioning'] = $SourceProvisioning", self.text)
        # 有序字典只能用索引器加键；属性赋值会静默不生效
        self.assertNotIn('$result.source_provisioning =', self.text)

    def test_root_password_never_enters_the_docker_argv(self):
        # R-041：这个脚本的管理 SQL 全走 docker exec（容器内 root）。
        # 旧写法把口令拼进 argv，任何进程监视器都能读到；现在只经进程环境继承。
        self.assertEqual(0, self.text.count('MYSQL_PWD='), "口令被拼进 -e 参数值")
        self.assertIn("'-e', 'MYSQL_PWD',", self.text)
        self.assertIn(
            "[Environment]::SetEnvironmentVariable('MYSQL_PWD', $script:RootPassword, 'Process')",
            self.text)

    def test_docker_daemon_guard_only_applies_to_the_sandbox_form(self):
        # R-040 方案 B 的收尾：原生形态不碰 docker，所以 daemon 守卫必须按形态条件化。
        # 关键是"条件化"不等于"删掉"：sandbox 分支必须仍然调用它，否则我等于拆了 R-G3.5-1 的面。
        branch = "if ($TargetKind -eq 'sandbox') {" + chr(10) + "    Assert-LocalDockerDaemon"
        self.assertIn(branch, self.text)
        self.assertIn('target=native：跳过 Docker daemon 守卫', self.text)
        self.assertIn("$Docker = Get-Command docker -ErrorAction Stop", self.text)
        self.assertIn('    $Docker = $null', self.text)

    def test_native_channel_has_its_own_fail_closed_controls(self):
        # R-040 方案 B：原生通道是"并列增加一条带约束的通道"，不是把容器名白名单删掉。
        self.assertIn("[ValidateSet('sandbox', 'native')][string]$TargetKind = 'sandbox'", self.text)
        self.assertIn("拒绝 3306", self.text)
        self.assertIn("不在 -NativeDataDirPrefix", self.text)
        self.assertIn("两种形态只能选一种", self.text)
        self.assertIn("^zhicloud-mysql-sandbox-[0-9a-f]{8}$", self.text)
        self.assertIn("native 形态要求 -NativePasswordEnv", self.text)
        # 分派必须在咽喉函数里，调用点才不会漏改
        self.assertIn("if ($TargetKind -eq 'native') { return Invoke-NativeMysqlText -Sql $Sql }", self.text)
        self.assertEqual(0, self.text.count('MYSQL_PWD=$'), '口令不得进命令行')

    def test_am_requires_the_surefire_no_match_escape_hatch(self):
        # R-036 补 -am 的必然后果：上游模块没有该测试类，缺这个开关就会在 zhicloud-common 直接失败
        # （本轮真跑就是这么红的，日志原文 "No tests matching pattern ... were executed"）。
        block = re.search(r'\$mavenArgs = @\((.*?)\)', self.text, re.S).group(1)
        self.assertIn("'-Dsurefire.failIfNoSpecifiedTests=false'", block)
        self.assertIn("'-am'", block)

    def test_script_still_refuses_non_local_targets(self):
        # 这个脚本的 fail-closed 面是：本机 daemon + context 白名单、沙箱容器名格式、
        # 三个预定义库名，以及不出现任何通配授权。
        self.assertIn('Assert-LocalDockerDaemon', self.text)
        self.assertIn("throw \"检测到 DOCKER_HOST=", self.text)
        self.assertIn("'default', 'desktop-linux', 'colima', 'desktop'", self.text)
        self.assertIn("^zhicloud-mysql-sandbox-[0-9a-f]{8}$", self.text)
        self.assertIn('拒绝操作预定义沙箱目标库以外的数据库', self.text)
        # 未放宽授权：没有任何 GRANT/CREATE USER 到通配 host
        self.assertNotIn("@'%'", self.text)


if __name__ == '__main__':
    unittest.main()
