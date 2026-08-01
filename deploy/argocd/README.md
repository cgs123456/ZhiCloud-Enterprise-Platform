# yudao-server GitOps 部署说明（ArgoCD）

本文档说明 yudao-server 通过 ArgoCD + Helm 实现的 GitOps 部署流程，包括变更提交、回滚、Sync Hook 机制等。

## 目录结构

```
deploy/
├── helm/
│   └── yudao/                      # Helm Chart
│       ├── Chart.yaml              # Chart 元信息（version=1.0.0, appVersion=2026.06.0）
│       ├── values.yaml             # 默认配置
│       ├── values-dev.yaml         # DEV 环境覆盖（单副本 + 低资源）
│       ├── values-staging.yaml     # STAGING 环境覆盖（2 副本 + 中等资源）
│       ├── values-prod.yaml        # PROD 环境覆盖（3 副本 + 高资源 + 严格安全）
│       └── templates/
│           ├── _helpers.tpl        # 通用模板函数（fullname/labels/selectorLabels）
│           ├── deployment.yaml     # Deployment（含 probes / lifecycle / securityContext）
│           ├── service.yaml        # Service（48080 HTTP + 48090 Actuator）
│           ├── hpa.yaml           # HorizontalPodAutoscaler
│           ├── ingress.yaml       # Ingress（TLS + Nginx 注解）
│           ├── configmap.yaml     # ConfigMap（非敏感配置）
│           ├── secret.yaml        # Secret（敏感配置 stringData 占位）
│           ├── serviceaccount.yaml# ServiceAccount
│           ├── job-migration.yaml  # Flyway 迁移 Job（PreSync Hook）
│           └── pdb.yaml           # PodDisruptionBudget
└── argocd/
    ├── applicationset.yaml         # ApplicationSet（dev/staging/prod 三环境）
    ├── project.yaml                # AppProject（目标 namespace/cluster 白名单）
    └── README.md                   # 本文档
```

## 环境矩阵

| 环境 | namespace | values 文件 | 副本数 | 资源（CPU/内存）| HPA | Ingress Host |
|------|-----------|-------------|--------|-----------------|-----|---------------|
| DEV | yudao-dev | values-dev.yaml | 1 | 100m/256Mi - 500m/1Gi | 关闭 | api-dev.yudao.example.com |
| STAGING | yudao-staging | values-staging.yaml | 2 | 250m/512Mi - 1000m/2Gi | 2~5 | api-staging.yudao.example.com |
| PROD | yudao-prod | values-prod.yaml | 3 | 500m/1Gi - 2000m/4Gi | 3~15 | api.yudao.example.com |

## 部署架构

```
┌──────────────────────────────────────────────────────────────┐
│                        Git 仓库（真相源）                     │
│  deploy/helm/yudao/  +  deploy/argocd/                       │
└───────────────────────────┬──────────────────────────────────┘
                            │ ArgoCD 轮询/推送
                            ▼
┌──────────────────────────────────────────────────────────────┐
│                       ArgoCD 控制面                          │
│  ApplicationSet → 生成 3 个 Application（dev/staging/prod）   │
│  AppProject → 限制目标 namespace 白名单                       │
└───────────────────────────┬──────────────────────────────────┘
                            │ Sync（PreSync → Sync → PostSync）
                            ▼
┌──────────────────────────────────────────────────────────────┐
│                    Kubernetes 集群（in-cluster）             │
│  yudao-dev / yudao-staging / yudao-prod namespace            │
│  每个 namespace 内：Deployment + Service + HPA + Ingress      │
│                   + ConfigMap + Secret + ServiceAccount      │
│                   + PDB + Job（迁移）                          │
└──────────────────────────────────────────────────────────────┘
```

## GitOps 流程

### 1. 提交变更

所有配置变更通过 Git 提交触发，**禁止直接操作集群**（`kubectl edit`）。

```bash
# 修改 values 配置
vim deploy/helm/yudao/values-prod.yaml

# 修改镜像版本
vim deploy/helm/yudao/Chart.yaml   # 修改 appVersion

# 提交并推送
git add deploy/
git commit -m "chore(deploy): bump yudao-server to 2026.06.1"
git push origin main
```

ArgoCD 默认每 3 分钟轮询 Git，检测到变更后自动触发 Sync。如需立即同步，可在 ArgoCD UI 点击 `Refresh` 或执行：

```bash
argocd app sync yudao-server-prod
```

### 2. Sync Hook 机制

ArgoCD Sync 流程分三阶段，yudao 利用 PreSync 执行数据库迁移：

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   PreSync       │ →  │   Sync          │ →  │   PostSync      │
│ (迁移 Job)      │    │ (主资源部署)    │    │ (健康检查)      │
│                 │    │                 │    │                 │
│ job-migration   │    │ Deployment      │    │ ArgoCD 内置     │
│ .yaml 执行      │    │ Service         │    │ 资源健康检查    │
│ Flyway 迁移    │    │ HPA             │    │ （Deployment     │
│                 │    │ Ingress         │    │  Available）    │
│ 失败则阻塞 Sync │    │ ConfigMap       │    │                 │
│                 │    │ Secret          │    │                 │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

- **PreSync Hook**：`job-migration.yaml` 标注 `argocd.argoproj.io/hook: PreSync`，在主资源部署前执行 Flyway 迁移 Job。失败时 Sync 中止，避免新代码跑在旧 schema 上。
- **sync-wave**：`argocd.argoproj.io/sync-wave: "-1"` 保证迁移 Job 在所有主资源之前执行。
- **hook-delete-policy**：`before-hook-creation` 表示每次 Sync 前先删除旧 Job，保证幂等。

### 3. 回滚

GitOps 的回滚即 Git revert：

```bash
# 方法一：Git 回滚（推荐，保留审计轨迹）
git revert HEAD
git push origin main
# ArgoCD 自动 Sync 到回滚版本

# 方法二：ArgoCD 历史回滚（快速，但需手动同步 Git）
# 查看历史
argocd app history yudao-server-prod
# 回滚到指定版本（REVISION 为历史 ID）
argocd app rollback yudao-server-prod <REVISION>
# 注意：回滚后 selfHeal 会尝试同步回 Git HEAD，需同步更新 Git
```

> ⚠️ **重要**：因开启了 `selfHeal`，ArgoCD 历史回滚会被下一次 Sync 覆盖回 Git 最新状态。生产环境回滚建议优先使用 Git revert。

### 4. 密钥管理

所有密码通过 Secret 的 `stringData` 占位（`CHANGE_ME_IN_PRODUCTION`），**禁止硬编码真实密钥到 Git**。生产环境推荐方案：

| 方案 | 适用场景 | 说明 |
|------|----------|------|
| Sealed Secrets | 中小团队 | 加密后可安全存入 Git |
| External Secrets Operator + Vault | 大型团队 | 密钥集中管理，K8s 同步临时 Secret |
| SOPS + age | GitOps 原生 | 文件级加密，helm secrets 插件解密 |

部署前替换占位符示例（以 Sealed Secrets 为例）：

```bash
# 创建加密 SealedSecret
echo -n 'real-password' | kubectl create secret generic yudao-prod-secret \
  --dry-run=client --from-file=MASTER_DATASOURCE_PASSWORD=/dev/stdin -o yaml | \
  kubeseal --controller-namespace=kube-system -o yaml > sealed-secret.yaml

# 提交 SealedSecret 到 Git
```

## 本地验证

### Helm 模板渲染

```bash
cd deploy/helm

# 渲染 prod 配置
helm template yudao-prod ./yudao -f ./yudao/values-prod.yaml -n yudao-prod

# Lint 检查
helm lint ./yudao -f ./yudao/values-prod.yaml
```

### 部署到集群

```bash
# 手动部署（绕过 ArgoCD，仅调试用）
helm install yudao-prod ./deploy/helm/yudao \
  -f ./deploy/helm/yudao/values-prod.yaml \
  -n yudao-prod --create-namespace

# 升级
helm upgrade yudao-prod ./deploy/helm/yudao \
  -f ./deploy/helm/yudao/values-prod.yaml \
  -n yudao-prod

# 卸载
helm uninstall yudao-prod -n yudao-prod
```

### ArgoCD 部署

```bash
# 1. 创建 AppProject
kubectl apply -f deploy/argocd/project.yaml

# 2. 创建 ApplicationSet（自动生成 3 个 Application）
kubectl apply -f deploy/argocd/applicationset.yaml

# 3. 查看 Application 状态
kubectl get applications -n argocd
argocd app list
```

## 与原生 K8s yaml 的关系

`k8s/yudao-server.yaml` 为原生 K8s 清单（保留作为参考），Helm Chart 在 `deploy/helm/yudao/` 提供等价且参数化能力：

| 原生 yaml 资源 | Helm Chart 对应模板 |
|----------------|---------------------|
| ConfigMap | templates/configmap.yaml |
| Secret | templates/secret.yaml |
| Deployment | templates/deployment.yaml |
| Service | templates/service.yaml |
| HPA | templates/hpa.yaml |
| Ingress | templates/ingress.yaml |
| PDB | templates/pdb.yaml |
| NetworkPolicy | （未包含，可按需添加） |

Helm Chart 相对原生 yaml 的增强：
- 多环境 values 覆盖（dev/staging/prod）
- 配置项参数化（镜像/资源/副本数/域名）
- Flyway 迁移 Job（PreSync Hook）
- ServiceAccount 管理
- 拓扑分布约束
- Pod 注入 checksum 实现自动滚动更新

## 注意事项

1. **镜像 tag 策略**：`image.tag` 与 `Chart.appVersion` 对齐。CI/CD 构建镜像后，更新 `Chart.yaml` 的 `appVersion` 即可触发全环境更新。
2. **HPA 与副本数**：HPA 启用后 `Deployment.replicas` 字段被忽略，实际副本数由 HPA 控制。`ignoreDifferences` 配置避免 ArgoCD 误报差异。
3. **优雅停机**：`preStop` 调用 `actuator/shutdown` 需在 `application.yaml` 开启 `management.endpoint.shutdown.enabled=true`。
4. **数据库迁移**：迁移 Job 当前为占位实现，实际迁移由 Spring Boot 启动时 Flyway 自动执行。生产高可用场景建议拆出独立迁移流程。
5. **Sync 窗口**：`project.yaml` 配置了 prod 环境的 sync window（工作日 9:00-21:00），紧急变更可临时 `manualSync: true` 放开。
