# -*- coding: utf-8 -*-
"""R-041：物理复制沙箱验证器的 root 口令不得出现在 docker 命令行里。

这个脚本没有专属测试文件，所以守卫单独成文：它改的是 `Invoke-MySqlQuery` / `Invoke-MySqlScript`
两个原语（都走 `docker exec`，容器内 `--host=127.0.0.1 --port=3306`），
漏一处就等于把明文口令放回进程命令行。
"""
from __future__ import annotations

import unittest
from pathlib import Path

SCRIPT = Path(__file__).resolve().parents[3] / 'scripts' / 'production' / 'verify_mysql_physical_replication_sandbox.ps1'
SETTER = "[Environment]::SetEnvironmentVariable('MYSQL_PWD', $script:RootPassword, 'Process')"
SAFE_FLAG = "'-e', 'MYSQL_PWD',"
LEAK_FORM = "('-e', ('MYSQL_PWD=' + $script:RootPassword),"


class ReplicationPasswordTransportTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        if not SCRIPT.is_file():
            raise unittest.SkipTest(f'missing {SCRIPT}')
        cls.text = SCRIPT.read_text(encoding='utf-8-sig')

    def test_leak_form_is_gone_everywhere(self):
        self.assertEqual(0, self.text.count(LEAK_FORM), "口令被拼进 docker exec 的 -e 参数值")
        self.assertEqual(0, self.text.count('MYSQL_PWD=$'))

    def test_both_call_sites_inherit_through_the_process_environment(self):
        # 两个原语各自声明；只在其中一个设置会让另一条路径继续泄漏。
        self.assertEqual(2, self.text.count(SAFE_FLAG))
        self.assertEqual(2, self.text.count(SETTER))

    def test_environment_is_declared_before_the_argument_array(self):
        # 顺序是语义的一部分：`-e MYSQL_PWD`（不带值）从 CLI 进程环境取值，
        # 声明如果落在数组之后，第一条查询就会以"无密码"发出并报 1045。
        for marker in ('-i',):
            pass
        pairs = [(self.text.index(SETTER, i), self.text.index(SAFE_FLAG, i)) for i in (0, 1)]
        for setter_pos, flag_pos in pairs:
            self.assertLess(setter_pos, flag_pos, '必须先声明环境，再构造 -e 参数')

    def test_container_side_endpoint_unchanged(self):
        # 这条改动不该动连接面：仍是容器内真实回环，不引入宿主→发布端口。
        self.assertIn("'--host=127.0.0.1'", self.text)
        self.assertIn("'--port=3306'", self.text)
        self.assertNotIn('-p 3306', self.text)


if __name__ == '__main__':
    unittest.main()
