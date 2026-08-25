# Nacos Config Center

## Quick Start

1. Start Nacos via docker-compose:
   `ash
   docker compose up -d nacos
   `
2. Access Nacos console: http://localhost:8848/nacos
   - Default credentials: nacos / nacos
3. Activate Nacos config in zhicloud-server:
   `ash
   java -jar zhicloud-server.jar -Dspring.profiles.active=nacos
   `

## Configuration File Naming

| Config File | Scope | Priority |
|---|---|---|
| zhicloud-common.yaml | Shared across all environments | Lowest |
| zhicloud-server.yaml | Default (no profile) | Medium |
| zhicloud-server-dev.yaml | dev profile | High |
| zhicloud-server-prod.yaml | prod profile | High |

Nacos config overrides local application.yaml (same key, higher priority).

## Shared Config Example (zhicloud-common.yaml)

`yaml
# Shared configuration loaded for all environments
zhicloud:
  info:
    version: 1.0.0
  web:
    cors:
      allowed-origins: "*"
  security:
    xss:
      enable: true
`

## Security: Sensitive Fields

Sensitive fields (database passwords, Redis passwords, etc.) MUST be
injected via environment variables, NOT stored in Nacos:

| Field | Environment Variable |
|---|---|
| Database URL | MASTER_DATASOURCE_URL |
| Database Password | MASTER_DATASOURCE_PASSWORD |
| Redis Host | SPRING_DATA_REDIS_HOST |
| Redis Password | SPRING_DATA_REDIS_PASSWORD |
| Nacos Auth Token | NACOS_AUTH_TOKEN |

## Refresh Whitelist

The following config keys support dynamic refresh (see application-nacos.yaml):
  - zhicloud.security.xss.enable
  - zhicloud.web.cors.allowed-origins
  - spring.datasource.dynamic.datasource.master.url
  - spring.redis.host

## Docker Compose Environment Variables

`ash
# .env file example
NACOS_AUTH_TOKEN=SecretKey012345678901234567890123456789012345678901234567890123456789
NACOS_AUTH_IDENTITY_KEY=nacos
NACOS_AUTH_IDENTITY_VALUE=nacos
`
