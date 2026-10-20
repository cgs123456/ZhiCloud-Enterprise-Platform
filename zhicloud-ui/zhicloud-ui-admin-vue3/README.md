# 智云管理后台（Vue 3）

> 智云企业数字化平台的管理端：Vue 3 + Vite + Element Plus，承载全部业务域菜单、按钮级权限与多租户切换。

## 快速开始

需要 Node ≥ 20.19.0、pnpm ≥ 8.6.0（强制 pnpm），并先启动后端（见[根 README](../../README.md#快速开始)）。

```bash
pnpm install
pnpm dev      # 默认 http://localhost
```

开发服务器把 `/admin-api` 与 `/infra/ws` 代理到 `127.0.0.1:48080`（见 `vite.config.ts`），端口由 `.env.dev.local` 的 `VITE_PORT` 控制。

## 环境变量

按 mode 加载 `.env.dev` / `.env.test` / `.env.stage` / `.env.prod`，同名 `.env.*.local` 覆盖。

| 变量 | 说明 |
|---|---|
| `VITE_BASE_URL` | 后端地址前缀。留空 = 同源：开发由 Vite 代理，生产由 nginx 反代；前后端分开部署时才填完整域名 |
| `VITE_API_URL` | 接口前缀，默认 `/admin-api` |
| `VITE_APP_TENANT_ENABLE` | 是否发送 `tenant-id` 请求头；后端开启多租户时必须为 `true` |
| `VITE_APP_DEFAULT_LOGIN_TENANT` / `VITE_APP_DEFAULT_LOGIN_USERNAME` | 登录页预填的租户名与账号 |
| `VITE_GOVIEW_URL` | 大屏设计器地址，需另行部署 GoView 服务 |
| `VITE_PORT` | 开发端口，仅写在 `.env.dev.local` |

## 构建与部署

```bash
pnpm build:prod            # 输出到 dist-prod/
pnpm serve:prod            # 本地预览产线产物
```

生产按同源部署：nginx 托管 `dist-prod/`，并把 `/admin-api`、`/infra/ws` 反代到后端；WebSocket 必须带 `Upgrade` / `Connection` 头，否则 IM 与站内推送不可用。完整配置见[交付手册](../../docs/enterprise-platform/handoff/pilot-deployment-20261020.md#5-反向代理nginx必须满足的点)。

## 质量检查

```bash
pnpm ts:check       # vue-tsc 类型检查，CI 门禁
```

## 许可证

MIT，与根仓库一致。
