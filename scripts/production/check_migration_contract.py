#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Fail closed when reviewed migration bytes drift.

This gate does not prove that a migration can run on MySQL. It locks the exact
bytes of every migration at or above ``contract_start_version`` to a reviewed
SHA-256 manifest, and rejects untracked/duplicate versions. Runtime evidence
still requires a real MySQL shadow/pre-production execution.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import re
import sys
from collections import Counter
from pathlib import Path

import sql_migration_lint as migration_lint

REPO_ROOT = Path(__file__).resolve().parents[2]
DEFAULT_MIGRATIONS = REPO_ROOT / "zhicloud-server" / "src" / "main" / "resources" / "db" / "migration"
DEFAULT_VERIFICATION = REPO_ROOT / "zhicloud-server" / "src" / "main" / "resources" / "db" / "verification"
DEFAULT_MANIFEST = Path(__file__).resolve().parent / "migration-contract.json"
MIGRATION_RE = re.compile(r"^V(?P<version>\d+(?:\.\d+)*)__(?P<description>[A-Za-z0-9_]+)\.sql$")


def sha256_file(path: Path, *, normalize_lf: bool = False) -> str:
    """Hash a file, optionally normalizing CRLF to LF for canonical text bytes."""
    data = path.read_bytes()
    if normalize_lf:
        data = data.replace(b"\r\n", b"\n")
    return hashlib.sha256(data).hexdigest()


def _version_key(version: str) -> tuple[int, ...]:
    return tuple(int(part) for part in version.split("."))


def _finding(rule: str, path: Path | str, message: str) -> dict[str, str]:
    return {"rule": rule, "file": str(path), "message": message}


def _load_manifest(path: Path) -> dict:
    data = json.loads(path.read_text(encoding="utf-8"))
    if not isinstance(data, dict) or data.get("schema_version") != 1:
        raise ValueError("manifest schema_version must be 1")
    if not isinstance(data.get("contract_start_version"), int):
        raise ValueError("manifest contract_start_version must be an integer")
    if not isinstance(data.get("migrations"), list):
        raise ValueError("manifest migrations must be a list")
    return data


def build_report(migrations_dir: Path, verification_dir: Path, manifest_path: Path) -> dict:
    findings: list[dict[str, str]] = []
    try:
        manifest = _load_manifest(manifest_path)
    except (OSError, ValueError, json.JSONDecodeError) as exc:
        return {
            "tool": "check_migration_contract.py",
            "status": "failed",
            "scanned_migrations": 0,
            "tracked_migrations": 0,
            "findings": [_finding("C6", manifest_path, f"invalid manifest: {exc}")],
        }

    start_version = manifest["contract_start_version"]
    tracked = manifest["migrations"]
    tracked_names: set[str] = set()
    tracked_versions: list[int] = []

    migration_files: list[tuple[Path, str]] = []
    if migrations_dir.is_dir():
        for path in sorted(migrations_dir.glob("V*.sql")):
            match = MIGRATION_RE.match(path.name)
            if match:
                migration_files.append((path, match.group("version")))

    version_counts = Counter(version for _, version in migration_files)
    for path, version in migration_files:
        if version_counts[version] > 1:
            findings.append(_finding(
                "C3", path, f"duplicate migration version V{version}",
            ))

    for item in tracked:
        if not isinstance(item, dict):
            findings.append(_finding("C6", manifest_path, "tracked entry must be an object"))
            continue
        try:
            version = int(item["version"])
            filename = str(item["file"])
            expected_hash = str(item["sha256"]).lower()
            verification_file = str(item["verification_file"])
            verification_hash = str(item["verification_sha256"]).lower()
        except (KeyError, TypeError, ValueError) as exc:
            findings.append(_finding("C6", manifest_path, f"invalid tracked entry: {exc}"))
            continue

        filename_match = MIGRATION_RE.match(filename)
        if filename_match is None:
            findings.append(_finding("C6", manifest_path, f"invalid tracked filename: {filename}"))
        elif filename_match.group("version") != str(version):
            findings.append(_finding(
                "C6", manifest_path,
                f"tracked version V{version} does not match filename {filename}",
            ))
        if version < start_version:
            findings.append(_finding(
                "C6", manifest_path, f"tracked V{version} is below contract_start_version",
            ))
        if filename in tracked_names or version in tracked_versions:
            findings.append(_finding(
                "C6", manifest_path, f"duplicate tracked entry V{version}/{filename}",
            ))
        tracked_names.add(filename)
        tracked_versions.append(version)

        migration_path = migrations_dir / filename
        if not migration_path.is_file():
            findings.append(_finding("C5", migration_path, "tracked migration file is missing"))
        elif sha256_file(migration_path) != expected_hash:
            findings.append(_finding("C1", migration_path, "migration SHA-256 differs from manifest"))
        if migration_path.is_file():
            for violation in migration_lint.lint_text(
                migration_path.read_text(encoding="utf-8"), migration_path.name,
            ):
                findings.append(_finding(
                    "L1", migration_path,
                    f"[{violation['rule']}] line {violation['line']}: {violation['message']}",
                ))

        verification_path = verification_dir / verification_file
        if not verification_path.is_file():
            findings.append(_finding("C4", verification_path, "verification SQL file is missing"))
        elif sha256_file(verification_path, normalize_lf=True) != verification_hash:
            findings.append(_finding("C4", verification_path, "verification SQL SHA-256 differs from manifest"))

    for path, version in migration_files:
        if _version_key(version) >= _version_key(str(start_version)) and path.name not in tracked_names:
            findings.append(_finding("C2", path, "migration at or above contract start is not tracked"))

    return {
        "tool": "check_migration_contract.py",
        "status": "passed" if not findings else "failed",
        "contract_start_version": start_version,
        "scanned_migrations": len(migration_files),
        "tracked_migrations": len(tracked),
        "findings": findings,
        "limits": [
            "SHA-256 locks reviewed bytes; verification SQL is hashed after LF normalization to match .gitattributes eol=lf, while migration SQL remains byte-exact.",
            "This gate does not prove MySQL execution.",
            "Semantic backfill correctness still requires pre-production data validation.",
        ],
    }


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="迁移内容哈希与版本契约门禁")
    parser.add_argument("--migrations-dir", type=Path, default=DEFAULT_MIGRATIONS)
    parser.add_argument("--verification-dir", type=Path, default=DEFAULT_VERIFICATION)
    parser.add_argument("--manifest", type=Path, default=DEFAULT_MANIFEST)
    parser.add_argument("--json", action="store_true", help="输出 JSON")
    args = parser.parse_args(argv)

    report = build_report(args.migrations_dir, args.verification_dir, args.manifest)
    if args.json:
        print(json.dumps(report, ensure_ascii=False, indent=2, sort_keys=True))
    else:
        for finding in report["findings"]:
            print(f"[{finding['rule']}] {finding['file']}: {finding['message']}")
        print(f"{report['status'].upper()}: tracked={report['tracked_migrations']} "
              f"scanned={report['scanned_migrations']} findings={len(report['findings'])}")
    return 0 if report["status"] == "passed" else 1


if __name__ == "__main__":
    raise SystemExit(main())