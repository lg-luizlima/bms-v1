[Core Crédito] AKS OpenTelemetry Collector

Objetivo
Levar para o ambiente hml (AKS `fintech-test`, RG `rg-fintech-test`, subscription `Fintech-Test`, brazilsouth) o mesmo pipeline de observabilidade que já funciona localmente (`debezium-docker-compose.yaml` + `otel-collector-config.yaml`), trocando o backend local (Tempo/Jaeger/Loki/Grafana) por Azure Monitor (Application Insights).

O código das aplicações (bms-v1 e worker-v1) já fala **apenas OTLP** — nunca conhece Tempo/Loki/Azure Monitor diretamente (ver `OpenTelemetryLoggingConfig`, `application-{local,hml,prod}.yml`). Trocar de backend é só trocar a config do Collector; zero mudança de código.

Guardrails de acesso ao Azure/AKS para este fluxo: ver `AKS_HML_ACCESS_GUARDRAILS.md`.

Arquitetura — decisões
1. **Um Collector único, compartilhado entre bms-v1 e worker-v1 (e futuros serviços)**, não um por microserviço. Padrão *Gateway*: `Deployment` (não DaemonSet) numa namespace própria `observability`, exposto só via Service `ClusterIP`. Um Collector por serviço multiplicaria configuração/conexões com o Azure Monitor sem ganho real — isolamento por serviço só compensa em multi-tenancy rígida ou coleta de métricas de host por nó (padrão Agent/DaemonSet), que não é o caso aqui.
2. **Sem Ingress.** bms-v1 e worker-v1 já rodam dentro do mesmo cluster AKS — o tráfego OTLP fica inteiramente interno via Service `ClusterIP` + DNS interno do Kubernetes. Ingress só seria necessário se algo fora do AKS precisasse enviar telemetria. Expor o receptor OTLP publicamente seria um problema de segurança/custo: não tem autenticação nativa, então um endpoint público aceitaria telemetria de qualquer um (ingestão indevida faturada no Azure Monitor).
3. **Azure Monitor em vez de Loki/Tempo/Grafana.** Troca-se só a seção `exporters` da config do Collector: em vez de `otlp/tempo`, `otlp/jaeger`, `otlphttp/loki`, usa-se o exporter `azuremonitor` (da distro **contrib** do Collector — não está na distro "core"/"k8s"), apontando para a connection string de um Application Insights via Kubernetes Secret. Um único exporter cobre traces, logs e métricas.

Pré-requisitos
- VPN da empresa conectada (o cluster AKS `fintech-test` é **privado** — API server não tem IP público; confirmado via `az aks show --query apiServerAccessProfile.enablePrivateCluster` → `true`).
- `helm` e `kubectl` instalados localmente; contexto obtido via:
  ```bash
  az aks get-credentials -g rg-fintech-test -n fintech-test --overwrite-existing
  ```
- Permissão `Microsoft.Insights/components/write` em `rg-fintech-test` para criar o Application Insights (ver "Status atual" abaixo — bloqueado nesta rodada por falta de permissão).
- Confirmar a namespace real onde bms-v1/worker-v1 rodam em hml (vem do chart Helm compartilhado `deploy-helm/vvpp.yaml@CodePlay`, não está explícita nos repos — checar com `kubectl get ns` após VPN conectada).

Passo a passo

1) Application Insights (backend Azure Monitor)

Workspace-based, vinculado ao Log Analytics já existente `log-fintech-test` (eastus, RG `rg-pixparcelado-test`):

```bash
az monitor app-insights component create \
  --app otel-fintech-test \
  --location brazilsouth \
  --resource-group rg-fintech-test \
  --workspace /subscriptions/0471a664-6d45-4061-87e9-086e919733e8/resourceGroups/rg-pixparcelado-test/providers/Microsoft.OperationalInsights/workspaces/log-fintech-test \
  --application-type web
```

Recuperar a connection string:

```bash
az monitor app-insights component show \
  --app otel-fintech-test -g rg-fintech-test \
  --query connectionString -o tsv
```

2) Namespace + Secret com a connection string

```bash
kubectl create namespace observability

kubectl create secret generic otel-appinsights-secret \
  --namespace observability \
  --from-literal=APPLICATIONINSIGHTS_CONNECTION_STRING='<connection string do passo 1>'
```

3) Helm chart do OpenTelemetry Collector

```bash
helm repo add open-telemetry https://open-telemetry.github.io/opentelemetry-helm-charts
helm repo update
```

`otel-collector-values-hml.yaml` (não versionado no repo por decisão do time — guardar localmente/artefato de deploy; conteúdo abaixo é a fonte da verdade):

```yaml
# Values do chart open-telemetry/opentelemetry-collector para o ambiente hml
# do cluster AKS "fintech-test" (rg-fintech-test, brazilsouth).
#
# Backend: Azure Monitor (Application Insights), via exporter `azuremonitor`
# (imagem "contrib", pois o exporter azuremonitor não está na distro "k8s"/core).
#
# A connection string do Application Insights NÃO vai aqui — é injetada via
# secret `otel-appinsights-secret` (ver passo 2).

mode: deployment
replicaCount: 1

image:
  repository: otel/opentelemetry-collector-contrib
  tag: "0.113.0"

extraEnvsFrom:
  - secretRef:
      name: otel-appinsights-secret

config:
  exporters:
    azuremonitor:
      connection_string: ${env:APPLICATIONINSIGHTS_CONNECTION_STRING}
    debug:
      verbosity: basic

  service:
    pipelines:
      traces:
        receivers: [otlp]
        processors: [memory_limiter, batch]
        exporters: [debug, azuremonitor]
      logs:
        receivers: [otlp]
        processors: [memory_limiter, batch]
        exporters: [debug, azuremonitor]
      metrics:
        receivers: [otlp]
        processors: [memory_limiter, batch]
        exporters: [debug, azuremonitor]

# Portas não usadas (só falamos OTLP com os apps) — desliga jaeger/zipkin.
ports:
  jaeger-compact:
    enabled: false
  jaeger-thrift:
    enabled: false
  jaeger-grpc:
    enabled: false
  zipkin:
    enabled: false

resources:
  requests:
    cpu: 100m
    memory: 128Mi
  limits:
    cpu: 500m
    memory: 512Mi

service:
  type: ClusterIP
```

Validado localmente (sem cluster, só renderização do chart) com:
```bash
helm template otel-collector open-telemetry/opentelemetry-collector \
  -n observability -f otel-collector-values-hml.yaml --version 0.165.0
```
Resultado confirmado: Service `otel-collector-opentelemetry-collector` expõe só as portas `4317` (grpc) e `4318` (http); Deployment usa a imagem `otel/opentelemetry-collector-contrib:0.113.0`, injeta a connection string via `envFrom` do secret, `replicas: 1`.

Instalar de fato (dentro da VPN, com `kubectl`/`helm` já apontando pro cluster):
```bash
helm upgrade --install otel-collector open-telemetry/opentelemetry-collector \
  --namespace observability \
  -f otel-collector-values-hml.yaml \
  --version 0.165.0
```

4) Apontar bms-v1 e worker-v1 (hml) para o Collector

Endpoint interno resultante do passo 3:
`otel-collector-opentelemetry-collector.observability.svc.cluster.local`

Adicionar em `.azuredevops/config/hml/environments_variables.yml` de **cada um dos dois repos** (não são segredos — endpoint interno do cluster):

```yaml
- name: OTEL_EXPORTER_OTLP_ENABLED
  value: "true"
- name: OTEL_EXPORTER_OTLP_ENDPOINT
  value: http://otel-collector-opentelemetry-collector.observability.svc.cluster.local:4318/v1/traces
- name: OTEL_EXPORTER_OTLP_LOGS_ENDPOINT
  value: http://otel-collector-opentelemetry-collector.observability.svc.cluster.local:4318/v1/logs
- name: OTEL_EXPORTER_OTLP_METRICS_ENDPOINT
  value: http://otel-collector-opentelemetry-collector.observability.svc.cluster.local:4318/v1/metrics
- name: OTEL_EXPORTER_OTLP_METRICS_ENABLED
  value: "true"
```

Redeploy dos dois serviços em hml pelo pipeline existente (`cd.yaml` / template `deploy-helm/vvpp.yaml@CodePlay`) para pegar as novas variáveis.

5) Validação end-to-end

```bash
kubectl get pods,svc -n observability
kubectl logs -n observability deploy/otel-collector-opentelemetry-collector
```
- Sem erro de export para o Azure Monitor nos logs do Collector.
- Chamar um endpoint do bms-v1 em hml e conferir no Azure Portal → Application Insights `otel-fintech-test` → *Transaction Search* que o trace aparece.
- Conferir logs correlacionados por `trace_id` no blade *Logs*.
- Conferir se o worker-v1 aparece como span filho do mesmo trace — valida em ambiente real, pela primeira vez, a propagação `traceparent` → coluna `trace_context` do outbox → header Kafka (Debezium) → consumo no worker.
- Conferir métricas em *Metrics* / *Application Map*.

Status atual (2026-08-03)
- Passo 1 **bloqueado**: a conta usada (`joao.reis@telefonicafinancial.onmicrosoft.com`) não tem a permissão `Microsoft.Insights/components/write` em `rg-fintech-test` (`AuthorizationFailed` ao tentar criar `otel-fintech-test`). Responsável pelo ambiente precisa criar o recurso (mesmos parâmetros do passo 1) ou conceder a role (Contributor, ou no mínimo Application Insights Component Contributor) no resource group.
- Passos 2-5 preparados e validados localmente (renderização do chart), mas não executados contra o cluster — dependem da connection string do passo 1 e de acesso via VPN ao API server privado do AKS.
