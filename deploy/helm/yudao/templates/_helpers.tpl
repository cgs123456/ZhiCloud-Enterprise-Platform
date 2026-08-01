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
