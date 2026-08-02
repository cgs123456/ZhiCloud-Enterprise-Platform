# CI Build #21 失败根因分析

**时间**: 2026-08-02 21:51 CST
**退出码**: exit 1

## 根因（Root Cause）

**JaCoCo 覆盖率门禁：`yudao-module-bpm` 模块指令覆盖率仅 0.04 (4%)，远低于门禁阈值 0.40 (40%)**

### 验证日志
```
[WARNING] Rule violated for bundle yudao-module-bpm: instructions covered ratio is 0.04, but expected minimum is 0.40
[ERROR] Failed to execute goal org.jacoco:jacoco-maven-plugin:0.8.12:check (check) on project yudao-module-bpm: Coverage checks have not been met.
```

### CI 流程分析

| 步骤 | 状态 |
|------|------|
| ① Error Code Uniqueness Gate (`check_error_codes.py`) | ✅ 本地通过 (2036 定义，0 冲突) |
| ② WMS @PreAuthorize Gate | ✅ 只在 HEAD 版 maven.yml 中出现，WMS 已全部整改 |
| ③ Bare-Throw Exception Gate | ✅ 本地通过 (1 豁免，0 裸抛) |
| ④ Transaction Atomicity Gate | ✅ 本地通过 (0 缺事务方法) |
| ⑤ `mvn -B verify` (含 JaCoCo check) | ❌ **BPM 模块覆盖率 4% < 40%** |
| ⑥ Security Check | ⏭️ 因步骤 ⑤ 失败未执行 |
| ⑦ CycloneDX SBOM 生成 | ⏭️ 因步骤 ⑤ 失败未执行 |
| ⑧ Upload SBOM (`target/bom.*`) | ⏭️ 因步骤 ⑦ 未执行，找不到文件 |

最后一步 "No files were found with target/bom.*" 只是连锁效应，不是根因。

### 配置分析

`pom.xml` 中 JaCoCo 覆盖率规则：

```xml
<includes>
  <include>cn/iocoder/yudao/module/wms/service/**</include>
  <include>cn/iocoder/yudao/module/mes/service/**</include>
  <include>cn/iocoder/yudao/module/bpm/service/**</include>
</includes>
<!-- ... -->
<rules>
  <rule>
    <element>BUNDLE</element>
    <limits>
      <limit>
        <counter>INSTRUCTION</counter>
        <value>COVEREDRATIO</value>
        <minimum>${jacoco.minimum}</minimum>  <!-- 当前 0.40 -->
      </limit>
    </limits>
  </rule>
</rules>
```

三个模块 (wms/mes/bpm) 的 service 包各自独立做 BUNDLE 级覆盖率检查。BPM 有 32 个 service 类但指令覆盖率仅 4%。

### 附近发现（非根因但需关注）

HEAD commit 中的本地未暂存变更：
- `.github/workflows/maven.yml` — 未暂存版本新增了 Full-Repo @PreAuthorize Gate 和 Electronic Signature Gate
- `scripts/check_electronic_signature.py` — **完全新文件**，从未提交到 Git
- `scripts/check_missing_preauthorize.py` — 已暂存新版（含 `--max-gaps` 支持），旧版在 HEAD commit 中不支持此参数

这些不是当前 build #21 的失败原因（当前 CI 跑的是 HEAD commit 的旧 maven.yml，没有这两个新增步骤），但如果推送到远端则会因为 `check_electronic_signature.py` 文件缺失而新增失败。

## 修复方向

### 方向 A：补齐 BPM 模块单测（推荐长期方案）
- BPM 有 20 个测试文件，但覆盖大部分只命中基础 CRUD，未覆盖复杂业务逻辑方法（如 `cleanModel`、流程审批等）
- 需要为 `BpmModelServiceImpl`、`BpmTaskService` 等核心 service 方法补充单元测试

### 方向 B：临时降低 BPM 覆盖率阈值
- 修改 `jacoco.minimum` 从 0.40 降到 0.04（或移除 BPM 的 include）
- 仅在 CI 临时通过时使用，应在后续 commit 中逐步抬回

### 方向 C：从 JaCoCo 检查中临时排除 BPM 模块
- 从 `<includes>` 中移除 `cn/iocoder/yudao/module/bpm/service/**`
- 等 BPM 单测补齐后再加回
