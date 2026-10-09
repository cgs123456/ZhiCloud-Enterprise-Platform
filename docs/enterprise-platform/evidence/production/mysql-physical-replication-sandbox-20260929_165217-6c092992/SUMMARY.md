# MySQL Physical Replication Sandbox Approximation

- status: passed
- validation_passed: True
- run_id: 6c092992
- image: mysql:8.4@sha256:0744ee5ef89ce6ccfa13de3e579fe6b9e27f93dd70da9c06d2c908b1b193fb8d
- repo_root: C:\Users\Administrator\Desktop\zhicloud\zhicloud-audit-src2
- source_container: zhicloud-mysql-sandbox-3aab78f3
- replica_container: zhicloud-mysql-sandbox-110e95ff
- checks: 23; failed: 0
- evidence.json SHA-256: c773dddf1fd080050622ca604695f6862f4046b5dcd891c745fb98a7c6b9e37e

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

- failure: 
