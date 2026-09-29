#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Generate the production clean baseline from the reviewed V1.1 schema script.

V1.1 is the production schema source of truth and must not contain public
accounts or client credentials.  The migration runner permits a one-time,
checksum-whitelisted alignment for previously deployed databases; fresh
installations instead use this separately reviewed artifact, which keeps all
schema DDL and non-seed reference data while removing demo rows.

This generator deliberately removes whole ``INSERT`` lines by table name.  It
does not parse, rewrite, or invent any password/secret value.  If a target
table ever gains a multi-line INSERT, generation fails closed and a human must
review the source change.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import re
import sys
from collections import Counter
from pathlib import Path

REPO = Path(__file__).resolve().parents[2]
SOURCE_DEFAULT = (REPO / "zhicloud-server" / "src" / "main" / "resources"
                  / "db" / "migration" / "V1.1__system_base_schema.sql")
OUTPUT_DEFAULT = (REPO / "zhicloud-server" / "src" / "main" / "resources"
                  / "db" / "clean-baseline" / "V1.1__system_base_schema.clean.sql")
MANIFEST_DEFAULT = OUTPUT_DEFAULT.with_name("manifest.json")

GENERATOR_VERSION = "1.0.0"
REMOVED_TABLES = {
    "system_users",
    "system_oauth2_client",
    "system_social_client",
    "system_user_role",
    "system_user_post",
}
DEMO_TABLE_PREFIX = "zhicloud_demo"
INSERT_RE = re.compile(
    r"^\s*INSERT\s+(?:IGNORE\s+)?INTO\s+`?([A-Za-z0-9_]+)`?\b",
    re.IGNORECASE,
)


def sha256_bytes(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def sha256_text(text: str) -> str:
    return sha256_bytes(text.encode("utf-8"))


def relative_or_absolute(path: Path, base: Path = REPO) -> str:
    try:
        return path.resolve().relative_to(base.resolve()).as_posix()
    except ValueError:
        return path.resolve().as_posix()


def render_clean_baseline(source_text: str) -> tuple[str, Counter[str], int]:
    """Return the filtered SQL, removed statement counts, and removed line count."""
    removed: Counter[str] = Counter()
    kept: list[str] = []
    removed_lines = 0

    for line_number, line in enumerate(source_text.splitlines(keepends=True), start=1):
        match = INSERT_RE.match(line)
        if not match:
            kept.append(line)
            continue

        table = match.group(1)
        if table not in REMOVED_TABLES and not table.startswith(DEMO_TABLE_PREFIX):
            kept.append(line)
            continue

        if not line.rstrip().endswith(";"):
            raise ValueError(
                f"目标表的 INSERT 不是单行语句（line {line_number}, table {table}）；"
                "为避免静默漏删，拒绝生成 clean baseline。"
            )
        removed[table] += 1
        removed_lines += 1

    output = "".join(kept)
    if not output.endswith("\n"):
        output += "\n"
    return output, removed, removed_lines


def build_manifest(source: Path, output: Path, source_sha256: str,
                   output_text: str, removed: Counter[str],
                   removed_lines: int) -> dict:
    return {
        "schema_version": 1,
        "generator": "scripts/production/generate_clean_baseline.py",
        "generator_version": GENERATOR_VERSION,
        "source_file": relative_or_absolute(source),
        "source_sha256": source_sha256,
        "clean_baseline_file": relative_or_absolute(output),
        "clean_baseline_sha256": sha256_text(output_text),
        "removed_insert_statements": dict(sorted(removed.items())),
        "removed_insert_lines": removed_lines,
        "removed_table_prefixes": sorted(REMOVED_TABLES | {DEMO_TABLE_PREFIX}),
        "preserved_demo_table_ddl": True,
        "purpose": (
            "Fresh-install bootstrap artifact: schema and non-seed reference data only; "
            "no public user, OAuth2, social-client, or demo rows."
        ),
    }


def write_text(path: Path, text: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding="utf-8", newline="\n")


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="生成生产 clean baseline（不修改 V1.1）")
    parser.add_argument("--source", type=Path, default=SOURCE_DEFAULT)
    parser.add_argument("--output", type=Path, default=OUTPUT_DEFAULT)
    parser.add_argument("--manifest", type=Path, default=MANIFEST_DEFAULT)
    parser.add_argument("--check", action="store_true",
                        help="只校验输出和 manifest 是否与当前源文件一致，不写入")
    parser.add_argument("--json", action="store_true", help="以 JSON 输出结果")
    args = parser.parse_args(argv)

    if not args.source.is_file():
        print(f"[FAIL] 源文件不存在: {args.source}", file=sys.stderr)
        return 2

    source_bytes = args.source.read_bytes()
    source_sha256 = sha256_bytes(source_bytes)
    # Normalise only the generated artifact; the source hash above remains tied
    # to the exact bytes that Flyway checksum validation sees.
    source_text = source_bytes.decode("utf-8").replace("\r\n", "\n").replace("\r", "\n")
    try:
        output_text, removed, removed_lines = render_clean_baseline(source_text)
    except ValueError as exc:
        print(f"[FAIL] {exc}", file=sys.stderr)
        return 1

    manifest = build_manifest(args.source, args.output, source_sha256,
                              output_text, removed, removed_lines)
    manifest_text = json.dumps(manifest, ensure_ascii=False, indent=2, sort_keys=True) + "\n"
    result = {
        "status": "passed",
        "source": relative_or_absolute(args.source),
        "source_sha256": manifest["source_sha256"],
        "output": relative_or_absolute(args.output),
        "output_sha256": manifest["clean_baseline_sha256"],
        "removed_insert_statements": manifest["removed_insert_statements"],
        "removed_insert_lines": removed_lines,
    }

    if args.check:
        mismatches: list[str] = []
        if not args.output.is_file():
            mismatches.append(f"缺少 clean baseline: {args.output}")
        elif args.output.read_bytes().decode("utf-8") != output_text:
            mismatches.append(f"clean baseline 内容漂移: {args.output}")
        if not args.manifest.is_file():
            mismatches.append(f"缺少 manifest: {args.manifest}")
        elif args.manifest.read_text(encoding="utf-8") != manifest_text:
            mismatches.append(f"manifest 内容漂移: {args.manifest}")
        if mismatches:
            result["status"] = "failed"
            result["mismatches"] = mismatches
            if args.json:
                print(json.dumps(result, ensure_ascii=False, indent=2, sort_keys=True))
            else:
                for item in mismatches:
                    print(f"[FAIL] {item}", file=sys.stderr)
            return 1
    else:
        write_text(args.output, output_text)
        write_text(args.manifest, manifest_text)

    if args.json:
        print(json.dumps(result, ensure_ascii=False, indent=2, sort_keys=True))
    else:
        print(f"{'PASS' if result['status'] == 'passed' else 'FAIL'}: "
              f"removed_insert_lines={removed_lines} "
              f"statements={manifest['removed_insert_statements']}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
