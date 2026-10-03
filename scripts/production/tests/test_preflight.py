import importlib.util
import json
import unittest
from pathlib import Path


MODULE_PATH = Path(__file__).resolve().parents[1] / "preflight.py"
SPEC = importlib.util.spec_from_file_location("zhicloud_preflight", MODULE_PATH)
preflight = importlib.util.module_from_spec(SPEC) if SPEC else None
if SPEC and preflight is not None:
    SPEC.loader.exec_module(preflight)


def tool(version, available=True, path="C:/tools/tool.exe"):
    return {"available": available, "path": path, "version": version}


class PreflightAssessmentTest(unittest.TestCase):
    def assess(self, **overrides):
        values = {
            "tools": {
                "java": tool("openjdk version \"21.0.11\" 2025-10-21"),
                "mvn": tool("Apache Maven 3.9.11"),
                "mysql": tool("mysql  Ver 8.4.0"),
                "docker": tool("Docker version 29.8.0"),
            },
            "docker_daemon": True,
            "datasource_url": "jdbc:mysql://localhost:3306/zhicloud",
            "allow_production": False,
            "disk_free_bytes": 20 * 1024**3,
            "min_free_bytes": 5 * 1024**3,
        }
        values.update(overrides)
        return preflight.assess_environment(**values)

    def test_java_below_21_is_blocker(self):
        result = self.assess(tools={
            "java": tool('java version "17.0.12"'),
            "mvn": tool("Apache Maven 3.9.11"),
            "mysql": tool("mysql  Ver 8.4.0"),
            "docker": tool("Docker version 29.8.0"),
        })

        self.assertEqual("blocked", result["status"])
        self.assertIn("JAVA_VERSION", [item["code"] for item in result["blockers"]])

    def test_maven_below_39_is_blocker(self):
        result = self.assess(tools={
            "java": tool('openjdk version "21.0.11"'),
            "mvn": tool("Apache Maven 3.8.8"),
            "mysql": tool("mysql  Ver 8.4.0"),
            "docker": tool("Docker version 29.8.0"),
        })

        self.assertEqual("blocked", result["status"])
        self.assertIn("MAVEN_VERSION", [item["code"] for item in result["blockers"]])

    def test_unavailable_docker_daemon_is_blocker(self):
        result = self.assess(docker_daemon=False)

        self.assertEqual("blocked", result["status"])
        self.assertIn("DOCKER_DAEMON", [item["code"] for item in result["blockers"]])

    def test_remote_datasource_requires_explicit_production_opt_in(self):
        result = self.assess(datasource_url="jdbc:mysql://db.prod.example.com:3306/zhicloud")

        self.assertEqual("blocked", result["status"])
        self.assertIn("PRODUCTION_DATASOURCE", [item["code"] for item in result["blockers"]])

    def test_remote_datasource_is_allowed_with_explicit_opt_in(self):
        result = self.assess(
            datasource_url="jdbc:mysql://db.prod.example.com:3306/zhicloud",
            allow_production=True,
        )

        self.assertNotIn("PRODUCTION_DATASOURCE", [item["code"] for item in result["blockers"]])

    def test_low_disk_is_blocker(self):
        result = self.assess(disk_free_bytes=2 * 1024**3)

        self.assertEqual("blocked", result["status"])
        self.assertIn("DISK_SPACE", [item["code"] for item in result["blockers"]])

    def test_result_has_required_contract(self):
        result = self.assess()

        for key in ("status", "blockers", "warnings", "tools", "generated_at"):
            self.assertIn(key, result)
        self.assertEqual("ready", result["status"])
        json.dumps(result, ensure_ascii=False)


class VersionParsingTest(unittest.TestCase):
    def test_parses_legacy_java_major(self):
        self.assertEqual(8, preflight.parse_java_major('java version "1.8.0_401"'))

    def test_parses_modern_java_major(self):
        self.assertEqual(21, preflight.parse_java_major('openjdk version "21.0.11" 2025-10-21'))

    def test_parses_maven_major_minor(self):
        self.assertEqual((3, 9), preflight.parse_maven_version("Apache Maven 3.9.11"))


if __name__ == "__main__":
    unittest.main()
