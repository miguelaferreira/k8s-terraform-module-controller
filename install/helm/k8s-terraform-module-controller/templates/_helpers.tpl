{{/* vim: set filetype=mustache: */}}
{{/* # @formatter:off */}}
{{/*
Expand the name of the chart.
*/}}
{{- define "k8s-terraform-module-controller.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "k8s-terraform-module-controller.fullname" -}}
{{- if .Values.fullnameOverride -}}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- $name := default .Chart.Name .Values.nameOverride -}}
{{- if contains $name .Release.Name -}}
{{- .Release.Name | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}
{{- end -}}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "k8s-terraform-module-controller.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Common labels
*/}}
{{- define "k8s-terraform-module-controller.labels" -}}
{{ include "k8s-terraform-module-controller.selectorLabels" . }}
app.kubernetes.io/component: "k8s-terraform-module-controller"
app.kubernetes.io/managed-by: {{ .Release.Service | quote }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
helm.sh/chart: {{ include "k8s-terraform-module-controller.chart" . | quote }}
{{- end -}}


{{/*
Selector labels
*/}}
{{- define "k8s-terraform-module-controller.selectorLabels" -}}
app.kubernetes.io/name: {{ include "k8s-terraform-module-controller.name" . | quote }}
app.kubernetes.io/instance: {{ .Release.Name | quote }}
{{- end -}}

{{/*
Create the name of the service account to use
*/}}
{{- define "k8s-terraform-module-controller.serviceAccountName" -}}
{{- if .Values.rbac.serviceAccount.create -}}
{{ include "k8s-terraform-module-controller.fullname" . }}
{{- else -}}
"default"
{{- end -}}
{{- end -}}

{{/*
Create the name of the application configuration configmap
*/}}
{{- define "k8s-terraform-module-controller.appliction.configmapName" -}}
"controller-application-config"
{{- end -}}

{{/* # @formatter:on */}}
