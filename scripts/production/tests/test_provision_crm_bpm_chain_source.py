# -*- coding: utf-8 -*-
"""R-035 收尾：provision_crm_bpm_chain_source.ps1 的守卫面测试。

这个脚本存在的理由是"动态数据源验证器的前置库没有任何仓库内产出路径"，
因此测试盯的是它**不能**假装产出了 schema：空库模式必须期望 0/0，
只有真跑了真链路 IT（由 Flowable 引擎引导建表）才允许期望 32/6，
且 IT 被 SKIP 时绝不能算成功。
"""
from __future__ import annotations

import re
import unittest
from pathlib import Path

SCRIPT = Path(__file__).resolve().parents[3] / 'scripts' / 'production' / 'provision_crm_bpm_chain_source.ps1'


class ProvisionChainSourceTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        if not SCRIPT.is_file():
            raise unittest.SkipTest(f'missing {SCRIPT}')
        cls.raw = SCRIPT.read_bytes()
        cls.text = SCRIPT.read_text(encoding='utf-8-sig')

    def test_encoding_is_bom_plus_lf(self):
        # 5.1 读不到无 BOM 的非 ASCII .ps1；CRLF 又会让既有 .ps1 之间产生幻影差异。
        self.assertTrue(self.raw.startswith(b'\xef\xbb\xbf'), '缺少 UTF-8 BOM')
        self.assertNotIn(b'\r\n', self.raw, '必须是 LF')

    def test_fail_closed_face_present(self):
        self.assertIn('Assert-LocalDockerDaemon', self.text)
        self.assertIn("throw \"检测到 DOCKER_HOST=", self.text)
        self.assertIn("'default', 'desktop-linux', 'colima', 'desktop'", self.text)
        self.assertIn("^zhicloud-mysql-sandbox-[0-9a-f]{8}$", self.text)
        self.assertIn("$SourceDatabase -ne 'zhicloud_crm_bpm_chain_test'", self.text)

    def test_maven_invocation_uses_am_and_the_real_chain_it(self):
        block = re.search(r'\$mavenArgs = @\((.*?)\)', self.text, re.S)
        self.assertIsNotNone(block, '找不到 $mavenArgs 构造块')
        args = block.group(1)
        self.assertIn("'-am'", args, 'R-036：缺 -am 时上游 bpm 模块走旧 jar，产出的 schema 不反映工作树')
        self.assertIn("'-o'", args)
        self.assertIn("'-pl', 'zhicloud-module-crm'", args)
        self.assertIn('-Dtest=$testClass', args)
        self.assertIn("$testClass = 'CrmReceivableBpmTransactionMysqlIntegrationTest'", self.text)

    def test_root_password_never_enters_argv(self):
        self.assertIn("'-e', 'MYSQL_PWD'", self.text)
        self.assertNotIn("'MYSQL_PWD=' +", self.text)
        self.assertNotIn('"-e", ("MYSQL_PWD="', self.text)
        # 走环境继承就必须全程保留：中途清掉会让后面的查询静默变成"无密码连接"，
        # 报出来的是 1045 而不是"忘了传口令"，很难一眼归因（本轮实际踩过）。
        self.assertIn('$env:MYSQL_PWD = $rootPassword', self.text)
        self.assertIn("'MYSQL_PWD'", self.text.split('$envNames = @(')[1].split(')')[0])
        # 断言收窄到我真正要守的那条不变式：沙箱阶段内不得中途清掉 MYSQL_PWD。
        # 原先"任何 SetEnvironmentVariable('MYSQL_PWD' 都不许出现"是过宽的代理断言：
        # native 通道每次调用前自己设、调用后在 finally 里清成 $null，是正确的，却被这条判红。
        # 现在要求全文件只允许出现一次该写法，且必须是 native 分支那次"清空"。
        self.assertEqual(1, self.text.count("SetEnvironmentVariable('MYSQL_PWD'"),
                         '出现第二次 MYSQL_PWD 环境写回，需确认不是中途清掉了沙箱阶段的口令')
        self.assertIn("[Environment]::SetEnvironmentVariable('MYSQL_PWD', $null, 'Process')", self.text)
        self.assertNotIn("SetEnvironmentVariable('MYSQL_PWD', $previousEnv", self.text)

    def test_skip_is_never_treated_as_provisioned_schema(self):
        self.assertIn('$result.test.tests_run -ne 2', self.text)
        self.assertIn('$result.test.skipped -ne 0', self.text)

    def test_expectations_are_locked_to_the_provisioning_mode(self):
        # 空库模式配 32/6、或 IT 模式配 0/0，都会让"前置已满足"变成一句可以自己编的话。
        # 断言的是判定条件本身，不是 throw 里的散文——只查文案会造出一条删掉守卫也照样绿的死测试。
        self.assertIn('if ($RunRealChainIt) {', self.text)
        self.assertIn('if ($ExpectedFlowableTables -ne 32 -or $ExpectedPropertyRows -ne 6) {', self.text)
        self.assertIn('} elseif ($ExpectedFlowableTables -ne 0 -or $ExpectedPropertyRows -ne 0) {', self.text)
        # IT 模式的默认期望值来自引擎产出本身，且只有在调用方没显式传值时才可代入。
        self.assertIn("$PSBoundParameters.ContainsKey('ExpectedFlowableTables')", self.text)
        self.assertIn('$ExpectedFlowableTables = 32', self.text)
        self.assertIn('$ExpectedPropertyRows = 6', self.text)
        self.assertIn('[int]$ExpectedFlowableTables = 0', self.text)
        self.assertIn('[int]$ExpectedPropertyRows = 0', self.text)

    def test_existing_database_is_not_dropped_silently(self):
        self.assertIn('-not $Replace', self.text)
        self.assertIn('不会静默 DROP', self.text)

    def test_evidence_is_unique_named_and_hashed(self):
        self.assertIn("Get-UniqueBaseName -Directory $EvidenceDir -Prefix 'crm-bpm-chain-source-provision'", self.text)
        self.assertIn("$jsonHash + ' *' + [System.IO.Path]::GetFileName($jsonPath)", self.text)
        self.assertIn("$result.artifacts.script_sha256 = Get-Sha256 -Path $scriptPath", self.text)

    def test_declares_it_does_not_hold_the_flowable_ddl(self):
        # 仓库里没有 ACT_/FLW_ 的 DDL；这条 caveat 是"为什么必须跑 IT"的书面根据。
        self.assertIn('仓库内没有 Flowable DDL', self.text)
        self.assertIn('schema_producer', self.text)
        self.assertIn('real_chain_it_flowable_schema_update', self.text)

    def test_jdbc_endpoint_is_selectable_and_recorded(self):
        # 直连发布端口与容器内回环中继是 mysqld 的两种不同视角（172.17.0.1 vs 127.0.0.1，R-033），
        # 选哪一条必须能显式指定并写进证据，否则两种形态的证据又会互相冒充。
        self.assertIn('[int]$DatabasePort = 0', self.text)
        self.assertIn("$endpointSource = 'explicit'", self.text)
        self.assertIn("$endpointSource = 'container_published_port'", self.text)
        self.assertIn('jdbc_endpoint_source = $endpointSource', self.text)
        self.assertIn('container_published_port = $publishedPort', self.text)

    def test_docker_daemon_guard_only_applies_to_the_sandbox_form(self):
        # 同上：native 形态不得被无关的 daemon 守卫挡住；sandbox 形态的守卫必须原样保留。
        branch = "if ($TargetKind -eq 'sandbox') {" + chr(10) + "    Assert-LocalDockerDaemon"
        self.assertIn(branch, self.text)
        self.assertIn('target=native：跳过 Docker daemon 守卫', self.text)
        self.assertIn('    $Docker = $null', self.text)

    def test_verdict_fields_stay_negative(self):
        self.assertIn('production_ready    = $false', self.text)
        self.assertIn('gate3_passed        = $false', self.text)
        self.assertIn("Go_No_Go            = 'No-Go'", self.text)


if __name__ == '__main__':
    unittest.main()
