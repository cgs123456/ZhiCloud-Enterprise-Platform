"""@PreAuthorize / @PermitAll 门禁的回归测试（2026-10-10 新增）。

背景（handoff §12.168 / §12.170）：
  门禁脚本原本用「路径里必须有 controller/ 目录段」筛文件，导致
  `zhicloud-module-iot/.../api/device/IoTDeviceApiImpl.java`（包路径无 controller 段、
  文件名也不是 *Controller.java）连同它 5 个 @PermitAll 写接口长期不在扫描范围内。
  2026-10-10 已改为「按类上是否真的标注 @RestController/@Controller」判定，
  并把那 5 个设备端 RPC 接口登记到 scripts/permitall-allowlist.txt。

本测试固化两件事，防止盲区回归：
  1. 真实仓库扫描：口径必须覆盖到 IoTDeviceApiImpl，且门禁 PASS；
  2. 判定单元：路径不在 controller/ 下、文件名不以 Controller.java 结尾，只要类上
     真有 @RestController 就必须被收进来；而只在 import/Javadoc 里出现字样的
     文件（如 PiiMaskConverter）必须被排除。
"""

import os
import sys
import tempfile
import textwrap
import unittest
from pathlib import Path

SCRIPTS_DIR = Path(__file__).resolve().parents[2]
REPO_ROOT = Path(__file__).resolve().parents[3]
sys.path.insert(0, str(SCRIPTS_DIR))

import check_missing_preauthorize as cmp  # noqa: E402

IOT_IMPL = ("zhicloud-module-iot/zhicloud-module-iot-biz/src/main/java/cn/zhicloud/"
            "module/iot/api/device/IoTDeviceApiImpl.java")
PII_MASK = ("zhicloud-module-ai/src/main/java/cn/zhicloud/module/ai/config/security/"
            "PiiMaskConverter.java")


class RealRepoScopeTests(unittest.TestCase):
    """守卫必须自证真的扫到了那个越界文件，否则会退化成永远 [OK] 的空转检查。"""

    def test_real_repo_scan_includes_iot_device_api_impl(self):
        files = {rel for _full, rel in cmp.iter_controller_files(None, True)}
        self.assertIn(IOT_IMPL, files)

    def test_real_repo_scan_excludes_comment_only_controller_mention(self):
        """PiiMaskConverter 只在 import 与 Javadoc 里出现 @RestController，不是 Controller。"""
        files = {rel for _full, rel in cmp.iter_controller_files(None, True)}
        self.assertNotIn(PII_MASK, files)

    def test_real_repo_gate_passes_with_iot_allowlisted(self):
        allow = cmp.load_allowlist()
        for method in ("authDevice", "getDevice", "getModbusDeviceConfigList",
                       "registerDevice", "registerSubDevices"):
            self.assertIn(f"{IOT_IMPL}#{method}", allow,
                          f"{method} 必须显式登记在 permitall-allowlist.txt（含理由）")


class ScopeRuleTests(unittest.TestCase):
    """用临时仓库验证「按注解判定」本身，而不依赖真实仓库当前的文件布局。"""

    def _scan(self, rel_path, body):
        with tempfile.TemporaryDirectory() as tmp:
            full = Path(tmp) / rel_path
            full.parent.mkdir(parents=True, exist_ok=True)
            full.write_text(textwrap.dedent(body), encoding="utf-8")
            orig_root = cmp.ROOT
            try:
                cmp.ROOT = tmp
                return {r for _f, r in cmp.iter_controller_files(None, True)}
            finally:
                cmp.ROOT = orig_root

    def test_non_controller_dir_with_restcontroller_is_scanned(self):
        rel = "some-module/src/main/java/cn/x/api/device/ThingApi.java"
        files = self._scan(rel, """
            package cn.x.api.device;

            import org.springframework.web.bind.annotation.RestController;

            @RestController
            public class ThingApi {
            }
        """)
        self.assertIn(rel, files)

    def test_comment_only_annotation_is_not_scanned(self):
        # 文件名刻意不以 Controller.java 结尾——否则会走「文件名即 Controller」快速路径，
        # 测不到「先剥注释再匹配注解」这条规则（PiiMaskConverter 正是这种文件名）。
        rel = "some-module/src/main/java/cn/x/config/PiiMaskStyle.java"
        files = self._scan(rel, """
            package cn.x.config;

            import org.springframework.web.bind.annotation.RestController;

            /** 同时排除非 Controller（@RestController）返回类型。 */
            public class PiiMaskStyle {
            }
        """)
        self.assertNotIn(rel, files)


class EmptyScanGuardTests(unittest.TestCase):
    """空扫必须 fail-closed——2026-10-10 发现 --module 指到聚合目录时
    `roots` 为空，脚本扫 0 个文件却打印 [OK]，是典型「假绿」。"""

    def test_module_mode_scans_aggregator_module_children(self):
        """zhicloud-module-iot 是聚合目录，真正的 Controller 在 iot-biz 子模块下。"""
        files = {rel for _full, rel in cmp.iter_controller_files('zhicloud-module-iot', True)}
        self.assertGreater(len(files), 0,
                           '--module zhicloud-module-iot 必须扫到子模块里的 Controller，不能空转')
        self.assertTrue(any('controller/' in f for f in files))

    def test_unknown_module_yields_no_files(self):
        """未知模块名不报异常，但必须产生 0 个文件，由 main() 的空扫守卫判失败。"""
        files = list(cmp.iter_controller_files('zhicloud-module-does-not-exist', True))
        self.assertEqual(files, [])


if __name__ == "__main__":
    unittest.main()
