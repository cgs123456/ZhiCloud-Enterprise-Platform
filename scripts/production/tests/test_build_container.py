import json
import shutil
import subprocess
import tempfile
import unittest
from pathlib import Path


SCRIPT = Path(__file__).resolve().parents[1] / "build-container.ps1"
SETTINGS = Path(__file__).resolve().parents[1] / "maven-settings.xml"
WORKFLOW = Path(__file__).resolve().parents[3] / ".github" / "workflows" / "maven.yml"
REPO_ROOT = Path(__file__).resolve().parents[3]
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


class BuildContainerScriptTest(unittest.TestCase):
    def test_default_dry_run_uses_pinned_image_and_maven_workspace(self):
        completed = run_script("-DryRun", "-RepoRoot", str(REPO_ROOT))

        self.assertEqual(0, completed.returncode, completed.stderr)
        self.assertIn("maven:3.9.11-eclipse-temurin-21", completed.stdout)
        self.assertIn("/workspace", completed.stdout)
        self.assertIn("mvn -B -V", completed.stdout)
        self.assertIn("clean verify", completed.stdout)
        self.assertNotIn("latest", completed.stdout.lower())

    def test_latest_image_is_rejected(self):
        completed = run_script("-DryRun", "-RepoRoot", str(REPO_ROOT), "-Image", "maven:latest")

        self.assertNotEqual(0, completed.returncode)
        self.assertIn("latest", (completed.stdout + completed.stderr).lower())

    def test_missing_pom_is_rejected(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            completed = run_script("-DryRun", "-RepoRoot", temp_dir)

        self.assertNotEqual(0, completed.returncode)
        self.assertIn("pom.xml", (completed.stdout + completed.stderr).lower())

    def test_build_uses_explicit_central_settings(self):
        completed = run_script("-DryRun", "-RepoRoot", str(REPO_ROOT))
        payload = json.loads(completed.stdout)

        self.assertEqual(0, completed.returncode, completed.stderr)
        self.assertIn("-s", payload["arguments"])
        self.assertIn("/workspace/scripts/production/maven-settings.xml", payload["arguments"])

    def test_central_settings_mirror_all_repositories(self):
        text = SETTINGS.read_text(encoding="utf-8")

        self.assertIn("<mirrorOf>*</mirrorOf>", text)
        self.assertIn("https://repo.maven.apache.org/maven2", text)
        self.assertNotIn("mirrors.huaweicloud.com", text)

    def test_maven_cache_is_outside_project(self):
        completed = run_script("-DryRun", "-RepoRoot", str(REPO_ROOT))
        payload = json.loads(completed.stdout)

        self.assertEqual("zhicloud-maven-cache", payload["cache_volume"])
        self.assertTrue(all("/workspace/.m2" not in arg for arg in payload["arguments"]))
    def test_ci_pins_java_and_maven_versions(self):
        text = WORKFLOW.read_text(encoding="utf-8")

        self.assertIn("java-version: '21.0.9'", text)
        self.assertIn("maven-version: '3.9.11'", text)
        self.assertIn("mvn -B -V clean verify --file pom.xml", text)

    def test_default_image_is_locked_to_digest(self):
        completed = run_script("-DryRun", "-RepoRoot", str(REPO_ROOT))
        payload = json.loads(completed.stdout)

        self.assertEqual(0, completed.returncode, completed.stderr)
        self.assertIn("@sha256:", payload["image"])

    def test_build_command_prints_toolchain_versions(self):
        completed = run_script("-DryRun", "-RepoRoot", str(REPO_ROOT))
        payload = json.loads(completed.stdout)

        self.assertEqual(0, completed.returncode, completed.stderr)
        self.assertIn("-V", payload["arguments"])

    def test_ci_uploads_surefire_reports_on_failure(self):
        text = WORKFLOW.read_text(encoding="utf-8")

        self.assertIn("name: Upload Surefire Reports", text)
        self.assertIn("path: '**/target/surefire-reports/**'", text)
        self.assertIn("if: always()", text)


    def test_default_repo_root_resolves_without_explicit_argument(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            completed = subprocess.run(
                ["pwsh", "-NoProfile", "-File", str(SCRIPT), "-DryRun"],
                cwd=temp_dir,
                capture_output=True,
                text=True,
                encoding="utf-8",
                errors="replace",
                check=False,
                timeout=60,
            )

        self.assertEqual(0, completed.returncode, completed.stderr)
        payload = json.loads(completed.stdout)
        self.assertEqual(REPO_ROOT, Path(payload["repo_root"]))

    @unittest.skipIf(WINDOWS_POWERSHELL is None, "Windows PowerShell is not available")
    def test_default_repo_root_resolves_under_windows_powershell(self):
        with tempfile.TemporaryDirectory() as temp_dir:
            completed = subprocess.run(
                [
                    WINDOWS_POWERSHELL, "-NoProfile", "-ExecutionPolicy", "Bypass",
                    "-File", str(SCRIPT), "-DryRun",
                ],
                cwd=temp_dir,
                capture_output=True,
                text=True,
                encoding="utf-8",
                errors="replace",
                check=False,
                timeout=60,
            )

        self.assertEqual(0, completed.returncode, completed.stdout + completed.stderr)
        payload = json.loads(completed.stdout)
        self.assertEqual(REPO_ROOT, Path(payload["repo_root"]))


if __name__ == "__main__":
    unittest.main()

