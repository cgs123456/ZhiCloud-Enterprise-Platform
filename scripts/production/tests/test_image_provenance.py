"""R-G3.5-3 镜像溯源守卫：对纯函数 ImageProvenance.ps1 做行为级测试。

helper 刻意不起子进程，所以这些断言不需要 Docker daemon 就能跑；
另有一条把钉定摘要与本机真实 RepoDigests 对表，防止 pin 悄悄过期。
"""

import json
import shutil
import subprocess
import unittest
from pathlib import Path

REPO = Path(__file__).resolve().parents[3]
HELPER = REPO / 'scripts' / 'production' / 'ImageProvenance.ps1'
SANDBOX = REPO / 'scripts' / 'production' / 'mysql-sandbox.ps1'

PWSH = shutil.which('pwsh') or shutil.which('powershell')

GOOD = 'sha256:' + 'a' * 64
OTHER = 'sha256:' + 'b' * 64


def run_ps(body: str):
    script = '. "' + str(HELPER).replace('\\', '\\\\') + '"\n' + body
    done = subprocess.run([PWSH, '-NoProfile', '-ExecutionPolicy', 'Bypass', '-Command', script],
                          capture_output=True, text=True, encoding='utf-8', errors='replace',
                          timeout=120, check=False)
    return done.returncode, (done.stdout or '') + (done.stderr or '')


class ProvenanceLogicTests(unittest.TestCase):

    @classmethod
    def setUpClass(cls):
        if not PWSH:
            raise unittest.SkipTest('pwsh/powershell 均不可用：溯源守卫未经任何验证')
        if not HELPER.is_file():
            raise unittest.SkipTest(f'missing {HELPER}')

    def test_matching_digest_returns_provenance_object(self):
        body = (
            "$r = Assert-ImageDigestPinned -ImageId 'img-1' "
            "-RepoDigests @('mysql@" + OTHER + "') "
            "-PinnedImage 'mysql:8.4@" + OTHER + "' -Stage 'unit'; "
            "\"id=$($r.image_id) digest=$($r.pinned_digest) matched=$($r.matched_entry) count=$($r.repo_digests.Count)\""
        )
        rc, text = run_ps(body)
        self.assertEqual(rc, 0, text[-300:])
        self.assertIn('id=img-1', text)
        self.assertIn('matched=mysql@' + OTHER, text)
        self.assertIn('count=1', text)

    def test_digest_mismatch_is_refused(self):
        rc, text = run_ps(
            "try { $null = Assert-ImageDigestPinned -ImageId 'img-1' "
            "-RepoDigests @('mysql@%s') -PinnedImage 'mysql:8.4@%s' -Stage 'unit' ; 'GUARD_MISS' } "
            "catch { 'GUARD_HIT ' + $_.Exception.Message }" % (OTHER, GOOD)
        )
        self.assertEqual(rc, 0, text[-300:])
        self.assertIn('GUARD_HIT', text)
        self.assertIn('does not contain the pinned digest', text)

    def test_image_without_repo_digests_is_refused_not_passed(self):
        # 这条是关键：空列表若被判为通过，就正好是"扫描到 0 个文件却说 [OK]"那类假绿。
        rc, text = run_ps(
            "try { $null = Assert-ImageDigestPinned -ImageId 'img-1' -RepoDigests @() "
            "-PinnedImage 'mysql:8.4@%s' -Stage 'unit' ; 'GUARD_MISS' } "
            "catch { 'GUARD_HIT ' + $_.Exception.Message }" % GOOD
        )
        self.assertEqual(rc, 0, text[-300:])
        self.assertIn('GUARD_HIT', text)
        self.assertIn('no RepoDigests', text)

    def test_unpinned_reference_is_refused(self):
        rc, text = run_ps(
            "try { $null = Assert-ImageDigestPinned -ImageId 'img-1' "
            "-RepoDigests @('mysql@%s') -PinnedImage 'mysql:latest' -Stage 'unit' ; 'GUARD_MISS' } "
            "catch { 'GUARD_HIT ' + $_.Exception.Message }" % OTHER
        )
        self.assertEqual(rc, 0, text[-300:])
        self.assertIn('GUARD_HIT', text)
        self.assertIn('carries no @sha256', text)

    def test_json_parser_is_not_vacuous(self):
        body = (
            '"empty=" + @(ConvertFrom-RepoDigestsJson -Json ' + "'[]'" + ').Count;'
            '"blank=" + @(ConvertFrom-RepoDigestsJson -Json ' + "''" + ').Count;'
            '"one=" + @(ConvertFrom-RepoDigestsJson -Json ' + "'[\"mysql@sha256:aa\"]'" + ').Count;'
            '"two=" + @(ConvertFrom-RepoDigestsJson -Json ' + "'[\"mysql@sha256:aa\",\"busybox@sha256:bb\"]'" + ').Count;'
            '"first=" + @(ConvertFrom-RepoDigestsJson -Json ' + "'[\"mysql@sha256:aa\",\"busybox@sha256:bb\"]'" + ')[0]'
        )
        rc, text = run_ps(body)
        self.assertEqual(rc, 0, text[-300:])
        self.assertIn('empty=0', text)
        self.assertIn('blank=0', text)
        self.assertIn('one=1', text)
        self.assertIn('two=2', text)
        self.assertIn('first=mysql@sha256:aa', text)


class WiringTests(unittest.TestCase):

    def test_sandbox_script_resolves_provenance_for_both_images(self):
        text = SANDBOX.read_text(encoding='utf-8-sig')
        self.assertIn('ImageProvenance.ps1', text)
        self.assertIn("Stage 'sandbox-image-provenance'", text)
        self.assertIn("Stage 'relay-image-provenance'", text)
        self.assertIn('image_matched_digest = $provenance.pinned_digest', text)
        self.assertIn('relay_image_repo_digests', text)
        self.assertIn('image_provenance', text)
        # 溯源必须走执行器的 StdOut，而不是把 stderr 并进来解析 JSON
        self.assertIn('-Arguments @(\'inspect\', \'--format\', \'{{.Image}}\', $ContainerName)', text)

    def test_pinned_default_digest_matches_this_machine(self):
        docker = shutil.which('docker')
        if not docker:
            self.skipTest('本机无 docker：钉定摘要与真实 RepoDigests 的对表未执行')
        source = SANDBOX.read_text(encoding='utf-8-sig')
        needle = "[string]$Image = '"
        line = next(l for l in source.splitlines() if needle in l)
        pinned = line.split("'")[1]
        digest = pinned.split('@sha256:')[-1]
        done = subprocess.run([docker, 'image', 'inspect', '--format', '{{json .RepoDigests}}', 'mysql:8.4'],
                              capture_output=True, text=True, encoding='utf-8', errors='replace',
                              timeout=120, check=False)
        if done.returncode != 0:
            self.skipTest('本机无 mysql:8.4 镜像，无法对表钉定摘要：' + (done.stderr or '')[:120])
        digests = json.loads(done.stdout.strip() or '[]')
        self.assertTrue(any(digest in d for d in digests),
                        f'钉定摘要已过期：pinned={digest[:18]}… repo={digests}')


if __name__ == '__main__':
    unittest.main()
