{{/*
============================================================
yudao-server Helm Chart 通用模板函数（_helpers.tpl）
供其他模板通过 {{ include "yudao.xxx" . }} 复用
============================================================
*/}}

{{/*
yudao.fullname：资源名称前缀
- 取 .Values.fullnameOverride（如指定）
- 否则取 "yudao" + release 名（截断 63 字符，结尾非 -/.）
*/}}
{{- define "yudao.fullname" -}}
{{- if .Values.fullnameOverride -}}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- $name := default "yudao" .Values.nameOverride -}}
{{- if contains $name .Release.Name -}}
{{- .Release.Name | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- printf "%s-%s" $name .Release.Name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}
{{- end -}}

{{/*
yudao.chart：返回 Chart 名称 + 版本，用于 container 注解
*/}}
{{- define "yudao.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
yudao.labels：通用标签（含 Chart/appVersion/managed-by）
用于 metadata.labels
*/}}
{{- define "yudao.labels" -}}
helm.sh/chart: {{ include "yudao.chart" . }}
{{ include "yudao.selectorLabels" . }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end -}}

{{/*
yudao.selectorLabels：选择器标签（Deployment/Service 共用）
注意：selectorLabels 必须保持稳定，不能含会被 Release 变更的字段
*/}}
{{- define "yudao.selectorLabels" -}}
app.kubernetes.io/name: yudao
app.kubernetes.io/instance: {{ .Release.Name }}
app: yudao-server
component: backend
{{- end -}}

{{/*
yudao.serviceAccountName：返回 ServiceAccount 名
- 取 .Values.serviceAccount.name（如指定）
- 否则取 fullname
*/}}
{{- define "yudao.serviceAccountName" -}}
{{- if .Values.serviceAccount.create -}}
{{- default (include "yudao.fullname" .) .Values.serviceAccount.name -}}
{{- else -}}
{{- default "default" .Values.serviceAccount.name -}}
{{- end -}}
{{- end -}}

{{/*
yudao.image：返回完整镜像引用
- 若 global.imageRegistry 非空，拼接仓库地址前缀
*/}}
{{- define "yudao.image" -}}
{{- $repository := .Values.image.repository -}}
{{- if .Values.global.imageRegistry -}}
{{- printf "%s/%s:%s" .Values.global.imageRegistry $repository .Values.image.tag -}}
{{- else -}}
{{- printf "%s:%s" $repository .Values.image.tag -}}
{{- end -}}
{{- end -}}

{{/*
yudao.namespace：返回目标命名空间（优先 .Release.Namespace）
*/}}
{{- define "yudao.namespace" -}}
{{- .Release.Namespace -}}
{{- end -}}

{{/*
yudao.resources：渲染容器 resources 字段（片段）
*/}}
{{- define "yudao.resources" -}}
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
yudao.secretName：返回应用使用的 Secret 名称。
若配置了 secrets.existingSecret（由 External Secrets Operator / Sealed Secrets 等外部体系管理），
则直接引用该 Secret，Chart 本身不再渲染任何明文密钥。
*/}}
{{- define "yudao.secretName" -}}
{{- if .Values.secrets.existingSecret -}}
{{- .Values.secrets.existingSecret -}}
{{- else -}}
{{- printf "%s-secret" (include "yudao.fullname" .) -}}
{{- end -}}
{{- end -}}

{{/*
yudao.requiredSecret：渲染一个敏感值，并在其为空或仍为占位符时直接让 helm 渲染失败。

参数：list <root context> <键名> <取值>

设计意图：以往 values.yaml 中 11 个密钥的默认值都是 "CHANGE_ME_IN_PRODUCTION"，
helm install 不会报错，于是「忘了改密码」会静默进入生产——这类问题只有出事才会被发现。
现在改为渲染期硬失败，把它前移成部署时的确定性错误。

如需在本地/CI 冒烟环境临时放行，显式设置 secrets.allowInsecureDefaults=true。
*/}}
{{- define "yudao.requiredSecret" -}}
{{- $root := index . 0 -}}
{{- $key := index . 1 -}}
{{- $value := index . 2 | default "" -}}
{{- if or (eq $value "") (hasPrefix "CHANGE_ME" $value) -}}
{{- if not $root.Values.secrets.allowInsecureDefaults -}}
{{- fail (printf "secrets.%s 未设置或仍为占位符。请通过 External Secrets / Sealed Secrets / SOPS 注入真实值，或设置 secrets.existingSecret 引用外部 Secret；仅限非生产冒烟可用 --set secrets.allowInsecureDefaults=true 放行。" $key) -}}
{{- end -}}
{{- end -}}
{{- $value | quote -}}
{{- end -}}
