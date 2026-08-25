{{/*
============================================================
zhicloud-server Helm Chart 通用模板函数（_helpers.tpl）
供其他模板通过 {{ include "zhicloud.xxx" . }} 复用
============================================================
*/}}

{{/*
zhicloud.fullname：资源名称前缀
- 取 .Values.fullnameOverride（如指定）
- 否则取 "zhicloud" + release 名（截断 63 字符，结尾非 -/.）
*/}}
{{- define "zhicloud.fullname" -}}
{{- if .Values.fullnameOverride -}}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- $name := default "zhicloud" .Values.nameOverride -}}
{{- if contains $name .Release.Name -}}
{{- .Release.Name | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- printf "%s-%s" $name .Release.Name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}
{{- end -}}

{{/*
zhicloud.chart：返回 Chart 名称 + 版本，用于 container 注解
*/}}
{{- define "zhicloud.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
zhicloud.labels：通用标签（含 Chart/appVersion/managed-by）
用于 metadata.labels
*/}}
{{- define "zhicloud.labels" -}}
helm.sh/chart: {{ include "zhicloud.chart" . }}
{{ include "zhicloud.selectorLabels" . }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end -}}

{{/*
zhicloud.selectorLabels：选择器标签（Deployment/Service 共用）
注意：selectorLabels 必须保持稳定，不能含会被 Release 变更的字段
*/}}
{{- define "zhicloud.selectorLabels" -}}
app.kubernetes.io/name: zhicloud
app.kubernetes.io/instance: {{ .Release.Name }}
app: zhicloud-server
component: backend
{{- end -}}

{{/*
zhicloud.serviceAccountName：返回 ServiceAccount 名
- 取 .Values.serviceAccount.name（如指定）
- 否则取 fullname
*/}}
{{- define "zhicloud.serviceAccountName" -}}
{{- if .Values.serviceAccount.create -}}
{{- default (include "zhicloud.fullname" .) .Values.serviceAccount.name -}}
{{- else -}}
{{- default "default" .Values.serviceAccount.name -}}
{{- end -}}
{{- end -}}

{{/*
zhicloud.image：返回完整镜像引用
- 若 global.imageRegistry 非空，拼接仓库地址前缀
*/}}
{{- define "zhicloud.image" -}}
{{- $repository := .Values.image.repository -}}
{{- if .Values.global.imageRegistry -}}
{{- printf "%s/%s:%s" .Values.global.imageRegistry $repository .Values.image.tag -}}
{{- else -}}
{{- printf "%s:%s" $repository .Values.image.tag -}}
{{- end -}}
{{- end -}}

{{/*
zhicloud.namespace：返回目标命名空间（优先 .Release.Namespace）
*/}}
{{- define "zhicloud.namespace" -}}
{{- .Release.Namespace -}}
{{- end -}}

{{/*
zhicloud.resources：渲染容器 resources 字段（片段）
*/}}
{{- define "zhicloud.resources" -}}
resources:
  {{- with .Values.resources.requests }}
  requests:
    {{- toYaml . | nindent 4 }}
  {{- end }}
  {{- with .Values.resources.limits }}
  limits:
    {{- toYaml . | nindent 4 }}
  {{- end }}
{{- end -}}

{{/*
zhicloud.secretName：返回应用使用的 Secret 名称。
若配置了 secrets.existingSecret（由 External Secrets Operator / Sealed Secrets 等外部体系管理），
则直接引用该 Secret，Chart 本身不再渲染任何明文密钥。
*/}}
{{- define "zhicloud.secretName" -}}
{{- if .Values.secrets.existingSecret -}}
{{- .Values.secrets.existingSecret -}}
{{- else -}}
{{- printf "%s-secret" (include "zhicloud.fullname" .) -}}
{{- end -}}
{{- end -}}

{{/*
zhicloud.requiredSecret：渲染一个敏感值，并在其为空或仍为占位符时直接让 helm 渲染失败。

参数：list <root context> <键名> <取值>

设计意图：以往 values.yaml 中 11 个密钥的默认值都是 "CHANGE_ME_IN_PRODUCTION"，
helm install 不会报错，于是「忘了改密码」会静默进入生产——这类问题只有出事才会被发现。
现在改为渲染期硬失败，把它前移成部署时的确定性错误。

安全约束：
  - allowInsecureDefaults=true 仅在非 prod profile 下有效；生产部署（spring.profiles.active=prod 或 env.SPRING_PROFILES_ACTIVE=prod）
    无论此开关为何值，都必须使用真实密钥或 existingSecret。
  - 开启时渲染显式占位符 "[INSECURE_DEFAULT]" 而非空字符串，避免「密码为空但镜像可运行」的
    假象；生产环境此路径会被 fail 拦截。
*/}}
{{- define "zhicloud.requiredSecret" -}}
{{- $root := index . 0 -}}
{{- $key := index . 1 -}}
{{- $value := index . 2 | default "" -}}
{{- if or (eq $value "") (hasPrefix "CHANGE_ME" $value) -}}
  {{- if not $root.Values.secrets.allowInsecureDefaults -}}
    {{- fail (printf "secrets.%s 未设置或仍为占位符。请通过 External Secrets / Sealed Secrets / SOPS 注入真实值，或设置 secrets.existingSecret 引用外部 Secret；仅限非生产冒烟可用 --set secrets.allowInsecureDefaults=true 放行。" $key) -}}
  {{- else -}}
    {{/* 非 prod profile 临时放行：检测是否为生产环境 */}}
    {{- $isProd := false -}}
    {{/* 检测 env.SPRING_PROFILES_ACTIVE（部署配置中使用的变量） */}}
    {{- $profilesEnv := splitList "," (index $root.Values "env" | default dict | dig "SPRING_PROFILES_ACTIVE" "") -}}
    {{- range $profilesEnv -}}
      {{- if eq . "prod" -}}
        {{- $isProd = true -}}
      {{- end -}}
    {{- end -}}
    {{/* 兜底：检测 .Values.spring.profiles.active（部分 Helm chart 可能使用此路径） */}}
    {{- if not $isProd -}}
      {{- $profilesActive := splitList "," (index $root.Values "spring" | default dict | dig "profiles" "active" "") -}}
      {{- range $profilesActive -}}
        {{- if eq . "prod" -}}
          {{- $isProd = true -}}
        {{- end -}}
      {{- end -}}
    {{- end -}}
    {{- if $isProd -}}
      {{- fail (printf "secrets.%s：生产环境不允许 allowInsecureDefaults=true，必须注入真实密钥或使用 existingSecret。" $key) -}}
    {{- end -}}
    {{/* 渲染显式占位符，带引号以符合 YAML 规范 */}}
    {{ "[INSECURE_DEFAULT]" | quote }}
  {{- end -}}
{{- else -}}
  {{- $value | quote -}}
{{- end -}}
{{- end -}}
