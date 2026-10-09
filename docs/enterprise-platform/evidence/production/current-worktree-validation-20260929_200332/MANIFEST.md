# Current Worktree Validation Evidence

- Generated at: 2026-09-29T12:05:53.9845757Z
- Repository: `C:\Users\Administrator\Desktop\zhicloud\zhicloud-audit-src2`
- Branch: `upgrade/production-readiness`
- HEAD: `d0ebc96c9f864450c9c293094e114aff0e4e8588`
- Scope: current dirty worktree, local engineering gates and environment preflight only.

## Results

| Check | Result | Evidence | Boundary |
|---|---|---|---|
| Python full suite | `Ran 474 tests in 122.427s`, `OK`, exit 0 | `python-full.log`, `python-full.exit.txt` | Python production tests only; not full Java regression or production validation |
| PowerShell portability | `status=passed`, 12 scripts, pwsh 7.6.5 and Windows PowerShell 5.1.26100.9444 parse errors empty | `powershell-portability.json` | Static parse/BOM gate only |
| Environment preflight | `status=ready_with_warnings`, exit 0; warning `MYSQL_CLIENT_MISSING` | `preflight.json` | Local toolchain preflight only; no K8s, Secret, TLS or production DB proof |
| Backup/restore script self-test | `passed=true`, exit 0 | `backup-restore-drill-selftest.json` | Pure comparison-function self-test; no backup, restore, PITR, RTO or RPO execution |
| Production-equivalence probe | Missing kube config/current context; cluster connection refused; Helm/kind/minikube/k3d/vault/psql/sqlcmd/sqlplus/redis-cli/mongosh missing | `production-environment-probe.txt` | External environment blocker evidence, not a pass |

## Explicit Non-Claims

- This bundle does not establish production readiness, Gate3, independent review, HA/failover, physical replication, real network partition behavior, production-scale migration, backup/PITR, RTO/RPO or security closure.
- `HEAD` identifies the checked-out commit only. The worktree is dirty, so this evidence must not be represented as a frozen-release validation.
- The protected MySQL sandboxes and their state files were not modified or removed by this validation.

## File Hashes

| File | SHA-256 |
|---|---|
| `backup-restore-drill-selftest.exit.txt` | `A9F58776A09B5DAC438049683F24BF85764E0FF8E7455952456165C68C158627` |
| `backup-restore-drill-selftest.json` | `E1E22198F25C4F3F77A337CA08AE9D065156257467503C6B0119D05A3D794778` |
| `powershell-portability.exit.txt` | `A9F58776A09B5DAC438049683F24BF85764E0FF8E7455952456165C68C158627` |
| `powershell-portability.json` | `245EB306B25C7EA4A7BF83781F86AD1195198BA4D42CB317660BEE5295DBF61B` |
| `powershell-portability.log` | `442F4D21F6767D0146200D455AB07B7599A04FCFA96C2BD77DF8C87C56799EEA` |
| `preflight.exit.txt` | `A9F58776A09B5DAC438049683F24BF85764E0FF8E7455952456165C68C158627` |
| `preflight.json` | `4E5AC1BDB4090460A30422C63DDC76987F101790290B945BE5DAC037A43138F3` |
| `preflight.log` | `4E5AC1BDB4090460A30422C63DDC76987F101790290B945BE5DAC037A43138F3` |
| `production-environment-probe.txt` | `40DE1493C7CCA160F35B062FD7D9DDD9CBE9E939514DBFD1C505C6F02DD3A90D` |
| `python-full.exit.txt` | `A9F58776A09B5DAC438049683F24BF85764E0FF8E7455952456165C68C158627` |
| `python-full.log` | `8057FB15151E9A4F6B87F5511C0FEDB63DED2C3C6E18A299A824FE05A5508587` |
| `worktree-status.txt` | `66BB6D5269EA5AEF8961E6FDD0238DD46762943ED8587849498DB5C24027274F` |
