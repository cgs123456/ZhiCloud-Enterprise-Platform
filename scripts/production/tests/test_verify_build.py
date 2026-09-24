import json
import os
import shutil
import subprocess
import tempfile
import unittest
from pathlib import Path


SCRIPT = Path(__file__).resolve().parents[1] / "verify-build.ps1"
WINDOWS_POWERSHELL = shutil.which("powershell.exe")


def run_script(*args):
    return subprocess.run(
        ["pwsh", "-NoProfile", "-File", str(SCRIPT), *args],
        capture_output=True,
        text=True,
        encoding="utf-8",
        errors="replace",
        check=False,
        timeout=30,
    )


def write_fixture(root: Path, *, build_exit_code: int = 0, build_success: bool = True):
    repo = root / "repo"
    repo.mkdir()
    (repo / "pom.xml").write_text("<project/>", encoding="utf-8")
    (repo / ".git").mkdir()

    log = root / "build.log"
    result = "BUILD SUCCESS" if build_success else "BUILD FAILURE"
    log.write_text(
        "\n".join(
            [
                "Apache Maven 3.9.11 (test)",
                "Java version: 21.0.8, vendor: Eclipse Adoptium",
                result,
            ]
        ),
        encoding="utf-8",
    )

    report_dir = repo / "zhicloud-module-test" / "target" / "surefire-reports"
    report_dir.mkdir(parents=True)
    (report_dir / "TEST-sample.xml").write_text(
        '<?xml version="1.0" encoding="UTF-8"?>'
        '<testsuite name="sample" tests="3" failures="0" errors="0" skipped="1"/>',
        encoding="utf-8",
    )

    output = root / "verify-build.json"
    return repo, log, output



def make_real_git_repo(repo: Path) -> str:
    """Turn the fixture directory into a real git repo and return its HEAD sha."""
    git = shutil.which("git")
    if git is None:
        raise unittest.SkipTest("git is not available")
    (repo / ".git").rmdir()
    subprocess.run([git, "init", "-q", str(repo)], check=True)
    subprocess.run(
        [
            git, "-C", str(repo),
            "-c", "user.email=test@example.com",
            "-c", "user.name=test",
            "commit", "-q", "--allow-empty", "-m", "fixture",
        ],
        check=True,
    )
    rev = subprocess.run(
        [git, "-C", str(repo), "rev-parse", "HEAD"],
        capture_output=True, text=True, check=True,
    )
    return rev.stdout.strip()


class VerifyBuildScriptTest(unittest.TestCase):
    def test_successful_build_writes_reproducible_evidence(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            root = Path(temp_dir)
            repo, log, output = write_fixture(root)

            completed = run_script(
                "-RepoRoot", str(repo),
                "-BuildLog", str(log),
                "-BuildExitCode", "0",
                "-Output", str(output),
            )

            self.assertEqual(0, completed.returncode, completed.stderr)
            payload = json.loads(output.read_text(encoding="utf-8"))
            self.assertEqual("passed", payload["status"])
            self.assertEqual("3.9.11", payload["toolchain"]["maven"])
            self.assertEqual("21.0.8", payload["toolchain"]["java"])
            self.assertEqual(3, payload["tests"]["tests"])
            self.assertEqual(1, payload["tests"]["skipped"])
            self.assertEqual(0, payload["tests"]["failures"])
            self.assertEqual(1, payload["surefire"]["report_count"])

    def test_nonzero_build_exit_is_rejected(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            root = Path(temp_dir)
            repo, log, output = write_fixture(root, build_exit_code=1, build_success=False)

            completed = run_script(
                "-RepoRoot", str(repo),
                "-BuildLog", str(log),
                "-BuildExitCode", "1",
                "-Output", str(output),
            )

            self.assertNotEqual(0, completed.returncode)
            payload = json.loads(output.read_text(encoding="utf-8"))
            self.assertEqual("failed", payload["status"])
            self.assertIn("build_exit_code", payload["failure_reasons"])

    def test_missing_build_log_is_rejected_without_fabricated_evidence(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            root = Path(temp_dir)
            repo, _, output = write_fixture(root)

            completed = run_script(
                "-RepoRoot", str(repo),
                "-BuildLog", str(root / "missing.log"),
                "-BuildExitCode", "0",
                "-Output", str(output),
            )

            self.assertNotEqual(0, completed.returncode)
            self.assertFalse(output.exists())
            self.assertIn("build log", (completed.stdout + completed.stderr).lower())

    def test_failing_surefire_tests_are_rejected(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            root = Path(temp_dir)
            repo, log, output = write_fixture(root)
            report = next(repo.rglob("TEST-*.xml"))
            report.write_text(
                '<?xml version="1.0" encoding="UTF-8"?>'
                '<testsuite name="sample" tests="3" failures="1" errors="0" skipped="0"/>',
                encoding="utf-8",
            )

            completed = run_script(
                "-RepoRoot", str(repo),
                "-BuildLog", str(log),
                "-BuildExitCode", "0",
                "-Output", str(output),
            )

            self.assertNotEqual(0, completed.returncode)
            payload = json.loads(output.read_text(encoding="utf-8"))
            self.assertEqual("failed", payload["status"])
            self.assertIn("surefire_failures", payload["failure_reasons"])

    def test_missing_toolchain_evidence_is_rejected(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            root = Path(temp_dir)
            repo, log, output = write_fixture(root)
            log.write_text("BUILD SUCCESS\n", encoding="utf-8")

            completed = run_script(
                "-RepoRoot", str(repo),
                "-BuildLog", str(log),
                "-BuildExitCode", "0",
                "-Output", str(output),
            )

            self.assertNotEqual(0, completed.returncode)
            payload = json.loads(output.read_text(encoding="utf-8"))
            self.assertEqual("failed", payload["status"])
            self.assertIn("toolchain_evidence", payload["failure_reasons"])


    def test_commit_sha_is_recorded_for_real_git_repository(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            root = Path(temp_dir)
            repo, log, output = write_fixture(root)
            expected_sha = make_real_git_repo(repo)

            completed = run_script(
                "-RepoRoot", str(repo),
                "-BuildLog", str(log),
                "-BuildExitCode", "0",
                "-Output", str(output),
            )

            self.assertEqual(0, completed.returncode, completed.stderr)
            payload = json.loads(output.read_text(encoding="utf-8"))
            self.assertEqual("passed", payload["status"])
            self.assertEqual(expected_sha, payload["commit_sha"])
            self.assertEqual(40, len(payload["commit_sha"]))

    def test_failed_evidence_uses_documented_exit_code(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            root = Path(temp_dir)
            repo, log, output = write_fixture(root, build_exit_code=1, build_success=False)

            completed = run_script(
                "-RepoRoot", str(repo),
                "-BuildLog", str(log),
                "-BuildExitCode", "1",
                "-Output", str(output),
            )

            self.assertEqual(2, completed.returncode, completed.stdout + completed.stderr)
            payload = json.loads(output.read_text(encoding="utf-8"))
            self.assertEqual("failed", payload["status"])


    def test_repo_root_is_recorded_in_evidence(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            root = Path(temp_dir)
            repo, log, output = write_fixture(root)

            completed = run_script(
                "-RepoRoot", str(repo),
                "-BuildLog", str(log),
                "-BuildExitCode", "0",
                "-Output", str(output),
            )

            self.assertEqual(0, completed.returncode, completed.stderr)
            payload = json.loads(output.read_text(encoding="utf-8"))
            # 脚本会用 GetFullPath 归一化路径（含把 8.3 短名展开为长名），因此按规范化路径比较。
            self.assertEqual(
                Path(os.path.realpath(repo)),
                Path(os.path.realpath(payload["repo_root"])),
            )

    @unittest.skipIf(WINDOWS_POWERSHELL is None, "Windows PowerShell is not available")
    def test_default_repo_root_resolves_under_windows_powershell(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            missing_log = Path(temp_dir) / "missing.log"
            completed = subprocess.run(
                [
                    WINDOWS_POWERSHELL, "-NoProfile", "-ExecutionPolicy", "Bypass",
                    "-File", str(SCRIPT),
                    "-BuildLog", str(missing_log),
                    "-BuildExitCode", "0",
                ],
                capture_output=True,
                text=True,
                encoding="utf-8",
                errors="replace",
                check=False,
                timeout=60,
            )

        message = (completed.stdout + completed.stderr).lower()
        self.assertNotEqual(0, completed.returncode)
        self.assertIn("build log", message)
        self.assertNotIn("empty string", message)


    @unittest.skipIf(WINDOWS_POWERSHELL is None, "Windows PowerShell is not available")
    def test_evidence_is_host_independent_and_bom_free(self):
        """证据产物必须与宿主 PowerShell 版本无关。

        实测（2026-09-24）：同一份脚本在 pwsh 7.6.5 下写出 16 字节无 BOM 的 JSON，
        在 powershell.exe 5.1.26100 下写出 22 字节并带 UTF-8 BOM（EF BB BF），
        而 BOM 会让严格 JSON 解析器（如 Python json.loads）直接失败。
        因此这里同时断言：无 BOM、可被严格解析、两个宿主在同一份 fixture 上产出同一份证据。
        """
        with tempfile.TemporaryDirectory() as temp_dir:
            root = Path(temp_dir)
            repo, log, _ = write_fixture(root)

            payloads = {}
            for host in ("pwsh", WINDOWS_POWERSHELL):
                output = root / f"evidence-{Path(host).stem}.json"
                completed = subprocess.run(
                    [
                        host, "-NoProfile", "-ExecutionPolicy", "Bypass",
                        "-File", str(SCRIPT),
                        "-RepoRoot", str(repo),
                        "-BuildLog", str(log),
                        "-BuildExitCode", "0",
                        "-Output", str(output),
                    ],
                    capture_output=True,
                    text=True,
                    encoding="utf-8",
                    errors="replace",
                    check=False,
                    timeout=60,
                )

                self.assertEqual(0, completed.returncode, completed.stdout + completed.stderr)
                raw = output.read_bytes()
                self.assertNotEqual(
                    b"\xef\xbb\xbf",
                    raw[:3],
                    f"{host} wrote a UTF-8 BOM; evidence must be BOM-free UTF-8",
                )
                payloads[host] = json.loads(raw.decode("utf-8"))

        pwsh_payload = dict(payloads["pwsh"])
        windows_payload = dict(payloads[WINDOWS_POWERSHELL])
        pwsh_payload.pop("generated_at", None)
        windows_payload.pop("generated_at", None)
        self.assertEqual(
            pwsh_payload,
            windows_payload,
            "pwsh and powershell.exe must produce identical evidence",
        )


if __name__ == "__main__":
    unittest.main()
