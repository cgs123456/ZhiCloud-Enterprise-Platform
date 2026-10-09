# MySQL Physical Replication Sandbox Approximation

- status: failed
- validation_passed: False
- run_id: 6950d21a
- image: mysql:8.4@sha256:0744ee5ef89ce6ccfa13de3e579fe6b9e27f93dd70da9c06d2c908b1b193fb8d
- repo_root: C:\Users\Administrator\Desktop\zhicloud\zhicloud-audit-src2
- source_container: zhicloud-mysql-sandbox-e5dc7685
- replica_container: zhicloud-mysql-sandbox-6d4e885e
- checks: 10; failed: 0
- evidence.json SHA-256: 61f03a9ef952bbbf5d64eae8626e703288ea56d56e48baf9b2b65ec79d9b2edc

## Scope

This evidence proves only that two digest-pinned MySQL containers on one Docker host can be configured for GTID binlog replication, replicate a small synthetic probe table, enforce replica read-only, and resume catch-up after a controlled replica restart.

## Explicitly Not Proven

- production physical master/replica topology, HA, failover, or disaster recovery;
- network partition, production latency, MDL contention, or production-scale DDL/data;
- application migration correctness, tenant datasource topology, Outbox/Inbox, DLQ, replay, compensation, or reconciliation;
- production readiness, gate3, independent review, R-011 closure, or R-029 closure.

## Gate State

- production_ready=false
- gate3_passed=false
- independent_review=false
- R-011=OPEN
- R-029=OPEN
- Go_No_Go=No-Go

- failure: System.Management.Automation.RuntimeException: docker command failed with exit code 1. Output: ERROR 3056 (HY000) at line 1: The password provided for the replication user exceeds the maximum length of 32 characters
