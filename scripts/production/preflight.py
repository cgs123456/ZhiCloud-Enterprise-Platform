#!/usr/bin/env python3
"""Fail-closed preflight checks for ZhiCloud production upgrades.

This tool intentionally reports blockers instead of silently falling back when a
required production capability is missing. Docker can replace a local JDK or
Maven for CI verification, but the containerized build remains an explicit
verification path, not proof that the target host is production-ready.
"""

from __future__ import annotations

import argparse
import datetime as dt
import json
import os
import re
import shutil
import subprocess
import sys
from pathlib import Path
from typing import Any, Mapping
from urllib.parse import urlparse

MIN_JAVA_MAJOR = 21
MIN_MAVEN_VERSION = (3, 9)
DEFAULT_MIN_FREE_BYTES = 5 * 1024**3
DEFAULT_DATASOURCE = "jdbc:mysql://127.0.0.1:3306/zhicloud_platform"
LOCAL_DATABASE_HOSTS = {"localhost", "127.0.0.1", "::1"}


def parse_java_major(output: str) -> int | None:
    """Parse the major version from modern or legacy ``java -version`` output."""
    match = re.search(r'version\s+"([^"]+)"', output or "")
    if not match:
        match = re.search(r"version\s+([0-9][^\s]*)", output or "")
    if not match:
        return None

    version = match.group(1)
    parts = version.split(".")
    try:
        if parts[0] == "1" and len(parts) > 1:
            return int(parts[1])
        return int(re.match(r"\d+", parts[0]).group(0))
    except (AttributeError, ValueError):
        return None


def parse_maven_version(output: str) -> tuple[int, int] | None:
    """Parse Maven's major and minor version from ``mvn -version`` output."""
    match = re.search(r"Apache Maven\s+(\d+)\.(\d+)", output or "")
    if not match:
        return None
    return int(match.group(1)), int(match.group(2))


def _item(code: str, message: str) -> dict[str, str]:
    return {"code": code, "message": message}


def _tool_available(tools: Mapping[str, Any], name: str) -> bool:
    value = tools.get(name) or {}
    return bool(value.get("available"))


def _tool_version(tools: Mapping[str, Any], name: str) -> str:
    value = tools.get(name) or {}
    return str(value.get("version") or "")


def _datasource_host(datasource_url: str) -> str | None:
    if not datasource_url:
        return None
    value = datasource_url
    if value.startswith("jdbc:"):
        value = value[5:]
    parsed = urlparse(value)
    return parsed.hostname


def assess_environment(
    *,
    tools: Mapping[str, Any],
    docker_daemon: bool,
    datasource_url: str,
    allow_production: bool,
    disk_free_bytes: int,
    min_free_bytes: int = DEFAULT_MIN_FREE_BYTES,
) -> dict[str, Any]:
    """Return a deterministic, JSON-serializable preflight assessment."""
    blockers: list[dict[str, str]] = []
    warnings: list[dict[str, str]] = []

    if not _tool_available(tools, "java"):
        blockers.append(_item("JAVA_MISSING", "Java is required; expected JDK 21 or newer."))
    else:
        java_major = parse_java_major(_tool_version(tools, "java"))
        if java_major is None:
            blockers.append(_item("JAVA_VERSION_UNKNOWN", "Could not parse the Java version."))
        elif java_major < MIN_JAVA_MAJOR:
            blockers.append(
                _item("JAVA_VERSION", f"Java {java_major} is too old; JDK {MIN_JAVA_MAJOR}+ is required.")
            )

    if not _tool_available(tools, "mvn"):
        blockers.append(_item("MAVEN_MISSING", "Maven is required for a reproducible build."))
    else:
        maven_version = parse_maven_version(_tool_version(tools, "mvn"))
        if maven_version is None:
            blockers.append(_item("MAVEN_VERSION_UNKNOWN", "Could not parse the Maven version."))
        elif maven_version < MIN_MAVEN_VERSION:
            blockers.append(
                _item(
                    "MAVEN_VERSION",
                    f"Maven {maven_version[0]}.{maven_version[1]} is too old; Maven 3.9+ is required.",
                )
            )

    if not _tool_available(tools, "docker"):
        blockers.append(_item("DOCKER_MISSING", "Docker CLI is required for the isolated migration sandbox."))
    elif not docker_daemon:
        blockers.append(_item("DOCKER_DAEMON", "Docker CLI exists but the Docker daemon is unreachable."))

    if not _tool_available(tools, "mysql"):
        if docker_daemon:
            warnings.append(
                _item(
                    "MYSQL_CLIENT_MISSING",
                    "No local MySQL client; the sandbox will use the Docker MySQL container instead.",
                )
            )
        else:
            blockers.append(
                _item("MYSQL_CLIENT_MISSING", "No MySQL client and no Docker daemon are available.")
            )

    host = _datasource_host(datasource_url)
    if not host:
        blockers.append(_item("DATASOURCE_INVALID", "MASTER_DATASOURCE_URL has no parseable host."))
    elif host.lower() not in LOCAL_DATABASE_HOSTS and not allow_production:
        blockers.append(
            _item(
                "PRODUCTION_DATASOURCE",
                f"Datasource host '{host}' is not local. Pass --allow-production only after a backup.",
            )
        )

    if disk_free_bytes < min_free_bytes:
        blockers.append(
            _item(
                "DISK_SPACE",
                f"Only {disk_free_bytes} bytes free; at least {min_free_bytes} bytes are required.",
            )
        )

    status = "blocked" if blockers else ("ready_with_warnings" if warnings else "ready")
    return {
        "status": status,
        "blockers": blockers,
        "warnings": warnings,
        "tools": dict(tools),
        "datasource_host": host,
        "allow_production": allow_production,
        "disk_free_bytes": disk_free_bytes,
        "min_free_bytes": min_free_bytes,
        "generated_at": dt.datetime.now(dt.timezone.utc).isoformat(),
    }


def _run_version(command: list[str]) -> str:
    completed = subprocess.run(
        command,
        capture_output=True,
        text=True,
        encoding="utf-8",
        errors="replace",
        timeout=15,
        check=False,
    )
    output = "\n".join(part for part in (completed.stdout, completed.stderr) if part)
    return output.strip()


def _detect_tool(name: str, version_args: list[str]) -> dict[str, Any]:
    executable = shutil.which(name)
    if not executable:
        return {"available": False, "path": None, "version": None}
    try:
        version = _run_version([executable, *version_args])
    except (OSError, subprocess.SubprocessError) as exc:
        version = f"<error: {exc}>"
    return {"available": True, "path": executable, "version": version}


def collect_environment(repo_root: Path, datasource_url: str) -> dict[str, Any]:
    tools = {
        "java": _detect_tool("java", ["-version"]),
        "mvn": _detect_tool("mvn", ["-version"]),
        "mysql": _detect_tool("mysql", ["--version"]),
        "docker": _detect_tool("docker", ["--version"]),
    }
    docker_daemon = False
    docker_path = tools["docker"].get("path")
    if docker_path:
        try:
            output = _run_version([docker_path, "info", "--format", "{{.ServerVersion}}"])
            docker_daemon = bool(output and "<error:" not in output)
        except (OSError, subprocess.SubprocessError):
            docker_daemon = False

    disk_free = shutil.disk_usage(repo_root).free
    return {
        "tools": tools,
        "docker_daemon": docker_daemon,
        "disk_free_bytes": disk_free,
    }


def _render_text(result: Mapping[str, Any]) -> str:
    lines = [f"status: {result['status']}"]
    for blocker in result["blockers"]:
        lines.append(f"BLOCKER {blocker['code']}: {blocker['message']}")
    for warning in result["warnings"]:
        lines.append(f"WARNING {warning['code']}: {warning['message']}")
    return "\n".join(lines)


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="ZhiCloud production upgrade preflight")
    parser.add_argument("--json", action="store_true", help="emit machine-readable JSON")
    parser.add_argument("--output", type=Path, help="write JSON report to this path")
    parser.add_argument("--allow-production", action="store_true", help="allow a non-local datasource host")
    parser.add_argument("--min-free-gb", type=int, default=5, help="minimum free disk in GiB")
    args = parser.parse_args(argv)

    repo_root = Path(__file__).resolve().parents[2]
    datasource_url = os.getenv("MASTER_DATASOURCE_URL", DEFAULT_DATASOURCE)
    environment = collect_environment(repo_root, datasource_url)
    result = assess_environment(
        tools=environment["tools"],
        docker_daemon=environment["docker_daemon"],
        datasource_url=datasource_url,
        allow_production=args.allow_production,
        disk_free_bytes=environment["disk_free_bytes"],
        min_free_bytes=args.min_free_gb * 1024**3,
    )
    result["repo_root"] = str(repo_root)
    result["datasource_url"] = datasource_url

    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")

    if args.json:
        print(json.dumps(result, ensure_ascii=False, indent=2))
    else:
        print(_render_text(result))
    return 2 if result["status"] == "blocked" else 0


if __name__ == "__main__":
    sys.exit(main())
