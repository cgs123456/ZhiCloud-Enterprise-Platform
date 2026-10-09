# MySQL Physical Replication Sandbox Approximation

- status: passed
- validation_passed: True
- run_id: a4738da2
- image: mysql:8.4@sha256:0744ee5ef89ce6ccfa13de3e579fe6b9e27f93dd70da9c06d2c908b1b193fb8d
- repo_root: C:\Users\Administrator\Desktop\zhicloud\zhicloud-audit-src2
- source_container: zhicloud-mysql-sandbox-33cc76ec
- replica_container: zhicloud-mysql-sandbox-1de00199
- application_database: zhicloud_event_repl_sandbox
- application_schema_ready: True
- promotion: one-way source stop/replica promotion exercise (not HA or failover validation)
- owner_fencing: direct SQL CAS assertions recorded in evidence.json
- checks: 93; failed: 0
- evidence.json SHA-256: 95e598bc095b17f136fc22bddb41943065eec5cf1e3611206095fc24f8955166

## Scope

This evidence proves only that two digest-pinned MySQL containers on one Docker host can be configured for GTID binlog replication, replicate a small synthetic probe table and direct SQL Outbox/Inbox fixtures, enforce replica read-only, resume catch-up after a controlled replica restart, apply a minimal V100->V104->V107->V120 migration chain, perform a one-way source-stop/replica-promotion exercise, and exercise direct SQL owner-fencing CAS assertions.

## Explicitly Not Proven

- production physical master/replica topology, HA, failover, or disaster recovery;
- network partition, production latency, MDL contention, or production-scale DDL/data;
- the full production V1->V120 migration baseline or production application migration correctness;
- application worker concurrency, DLQ service replay, compensation, reconciliation, or tenant datasource topology;
- end-to-end application fencing under process pause, network partition, or concurrent service execution;
- production readiness, gate3, independent review, R-011 closure, or R-029 closure.

## Gate State

- production_ready=false
- gate3_passed=false
- independent_review=false
- R-011=OPEN
- R-029=OPEN
- Go_No_Go=No-Go

- failure: 
