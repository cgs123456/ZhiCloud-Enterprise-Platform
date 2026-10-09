#!/usr/bin/env bash
# V77 repair script integration gate for MySQL 8.x.
#
# This intentionally exercises the recovery SQL through the real mysql client,
# because DELIMITER and stored-procedure parsing are client-side behavior that a
# Java/JDBC test cannot faithfully reproduce.
set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
REPAIR_SQL="${REPAIR_SQL:-${REPO_ROOT}/scripts/production/repair_v77_aimultiagent_resilience.sql}"
MYSQL_HOST="${MYSQL_HOST:-127.0.0.1}"
MYSQL_PORT="${MYSQL_PORT:-3306}"
MYSQL_USER="${MYSQL_USER:-root}"
MYSQL_PASSWORD="${MYSQL_PASSWORD:?MYSQL_PASSWORD must be set}"
MYSQL_DATABASE="${MYSQL_DATABASE:-zhicloud_v77_repair_ci}"

if [[ ! -f "${REPAIR_SQL}" ]]; then
  echo "repair SQL not found: ${REPAIR_SQL}" >&2
  exit 1
fi

MYSQL=(mysql --protocol=TCP -h"${MYSQL_HOST}" -P"${MYSQL_PORT}" -u"${MYSQL_USER}" --batch --raw --skip-column-names)
export MYSQL_PWD="${MYSQL_PASSWORD}"

mysql_exec() {
  "${MYSQL[@]}" "${MYSQL_DATABASE}" "$@"
}

reset_database() {
  "${MYSQL[@]}" -e "DROP DATABASE IF EXISTS \`${MYSQL_DATABASE}\`; CREATE DATABASE \`${MYSQL_DATABASE}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
}

create_history_table() {
  mysql_exec -e "
    CREATE TABLE flyway_schema_history (
      installed_rank INT NOT NULL AUTO_INCREMENT,
      version VARCHAR(50) NULL,
      description VARCHAR(200) NOT NULL,
      type VARCHAR(20) NOT NULL,
      script VARCHAR(1000) NOT NULL,
      checksum INT NULL,
      installed_by VARCHAR(100) NOT NULL,
      installed_on TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
      execution_time INT NOT NULL,
      success TINYINT(1) NOT NULL,
      PRIMARY KEY (installed_rank)
    ) ENGINE=InnoDB;
  "
}

create_target_tables() {
  mysql_exec -e "
    CREATE TABLE aimultiagent_topology (
      id BIGINT NOT NULL PRIMARY KEY,
      status VARCHAR(20) NULL
    ) ENGINE=InnoDB;
    CREATE TABLE aimultiagent_execution_log (
      id BIGINT NOT NULL PRIMARY KEY,
      duration_ms BIGINT NULL
    ) ENGINE=InnoDB;
  "
}

run_repair() {
  mysql_exec < "${REPAIR_SQL}" 2>&1
}

assert_contains() {
  local haystack="$1"
  local needle="$2"
  if [[ "${haystack}" != *"${needle}"* ]]; then
    echo "expected output to contain '${needle}', got:" >&2
    printf '%s\n' "${haystack}" >&2
    exit 1
  fi
}

assert_fails() {
  local output
  if output="$(run_repair)"; then
    echo "repair unexpectedly succeeded; output:" >&2
    printf '%s\n' "${output}" >&2
    exit 1
  fi
  printf '%s\n' "${output}"
}

assert_column_exists() {
  local table="$1"
  local column="$2"
  local count
  count="$(mysql_exec -e "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = '${table}' AND column_name = '${column}';")"
  if [[ "${count}" != "1" ]]; then
    echo "expected column ${table}.${column} to exist exactly once, got ${count}" >&2
    exit 1
  fi
}

echo "[v77-ci] scenario 1: no Flyway history table is a no-op"
reset_database
out="$(run_repair)"
assert_contains "${out}" "V77_REPAIR_NOOP_NO_FLYWAY_HISTORY"

echo "[v77-ci] scenario 2: no V77 row and no new columns is a no-op"
reset_database
create_target_tables
create_history_table
out="$(run_repair)"
assert_contains "${out}" "V77_REPAIR_NOOP_NO_V77_HISTORY"

echo "[v77-ci] scenario 3: columns without V77 history fail closed"
reset_database
create_target_tables
create_history_table
mysql_exec -e "ALTER TABLE aimultiagent_topology ADD COLUMN version VARCHAR(32) NULL;"
out="$(assert_fails)"
assert_contains "${out}" "columns exist without V77 history"

echo "[v77-ci] scenario 4: half-applied V77 is repaired and marked successful"
reset_database
create_target_tables
create_history_table
mysql_exec -e "
  ALTER TABLE aimultiagent_topology ADD COLUMN version VARCHAR(32) NULL;
  INSERT INTO flyway_schema_history
    (version, description, type, script, checksum, installed_by, execution_time, success)
  VALUES ('77', 'aimultiagent resilience', 'SQL', 'V77__aimultiagent_resilience.sql', 123, 'ci', 1, 0);
"
out="$(run_repair)"
assert_contains "${out}" "V77_REPAIR_APPLIED"
assert_column_exists aimultiagent_topology version
assert_column_exists aimultiagent_execution_log trace_id
history_state="$(mysql_exec -e "SELECT success, checksum FROM flyway_schema_history WHERE version='77' AND script='V77__aimultiagent_resilience.sql';")"
if [[ "${history_state}" != "1	-282762132" ]]; then
  echo "unexpected V77 history state: ${history_state}" >&2
  exit 1
fi

echo "[v77-ci] scenario 5: repeated repair is a consistent no-op"
out="$(run_repair)"
assert_contains "${out}" "V77_REPAIR_ALREADY_CONSISTENT"

echo "[v77-ci] scenario 6: duplicate V77 history rows fail closed"
reset_database
create_target_tables
create_history_table
mysql_exec -e "
  ALTER TABLE aimultiagent_topology ADD COLUMN version VARCHAR(32) NULL;
  ALTER TABLE aimultiagent_execution_log ADD COLUMN trace_id VARCHAR(64) NULL;
  INSERT INTO flyway_schema_history
    (version, description, type, script, checksum, installed_by, execution_time, success)
  VALUES
    ('77', 'aimultiagent resilience', 'SQL', 'V77__aimultiagent_resilience.sql', -282762132, 'ci', 1, 1),
    ('77', 'aimultiagent resilience', 'SQL', 'V77__aimultiagent_resilience.sql', -282762132, 'ci', 1, 1);
"
out="$(assert_fails)"
assert_contains "${out}" "duplicate V77 history rows"

echo "[v77-ci] all scenarios passed"