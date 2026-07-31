[Core Crédito] AKS Debezium
Criado por Luiz Gustavo Dos Santos Lima, última atualização em jul 28, 2026  2 minutos de leitura
Objetivo
CDC
Vantagens
Ponte de atenção
Configuração
Azure Database for PostgreSQL Flexible Server
Instalar Debezium Operator
Secrets 
Debezium (Homologação)
Debezium (Produção)


Objetivo
O objetivo desta solução é capturar alterações realizadas no Azure Database for PostgreSQL e publicá-las em tópicos do Confluent Cloud Kafka utilizando o Debezium.

Mais detalhes na documentação: https://debezium.io/documentation/reference/stable/operations/debezium-operator.html

CDC
CDC (Change Data Capture) é uma técnica utilizada para identificar e transmitir alterações realizadas em um banco de dados sem a necessidade de consultas periódicas nas tabelas.

Em vez de executar buscas frequentes para verificar se houve mudanças, o Debezium monitora diretamente o WAL (Write-Ahead Log) do PostgreSQL, capturando eventos de:

INSERT
UPDATE
DELETE
Sempre que uma dessas operações ocorre, o Debezium gera um evento correspondente e o publica em um tópico Kafka.

Fluxo: PostgreSQL → Debezium (CDC) → Kafka → Aplicações consumidoras  

Vantagens
Replicação de dados em tempo real.
Redução da carga sobre o banco de dados.
Integração simplificada com aplicações, Data Lakes e plataformas analíticas.
Processamento orientado a eventos (Event-Driven Architecture).
Ponte de atenção
Necessidade de administrar Debezium.
Consumo adicional de WAL no PostgreSQL.
Possibilidade de eventos duplicados.
Necessidade de monitoramento.
Custos operacionais e de infraestrutura.
Configuração
Azure Database for PostgreSQL Flexible Server

https://debezium.io/documentation/reference/stable/connectors/postgresql.html



# Server Parameters
wal_level = logical
max_replication_slots >= 10
max_wal_senders >= 10

# Criar usuário
CREATE ROLE debezium WITH LOGIN PASSWORD '<KEY VAULT>';

# Conceder permissões
GRANT CONNECT ON DATABASE db_credit_consent TO debezium;
GRANT USAGE ON SCHEMA public TO debezium;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO debezium;
ALTER ROLE debezium WITH REPLICATION;

# Criar publicação
CREATE PUBLICATION dbz_publication
FOR ALL TABLES;
## Exemplo mais específico
FOR TABLE public.tb_debezium_cdc;

Instalar Debezium Operator


kubectl create namespace debezium

helm repo add debezium https://charts.debezium.io

helm upgrade --install debezium-operator \
  debezium/debezium-operator \
  --version 3.2.0-final \
  -n debezium \
  --set resources.requests.cpu=250m \
  --set resources.requests.memory=512Mi \
  --set resources.limits.cpu=500m \
  --set resources.limits.memory=1Gi
Secrets 


apiVersion: v1
kind: Secret
metadata:
  name: confluent-secret
  namespace: debezium
type: Opaque
stringData:
  BOOTSTRAP_SERVERS: <KEY VAULT>
  API_KEY: <KEY VAULT>
  API_SECRET: <KEY VAULT>
--
apiVersion: v1
kind: Secret
metadata:
  name: postgres-secret
  namespace: debezium
type: Opaque
stringData:
  DB_USER: debezium
  DB_PASSWORD: <KEY VAULT>
Debezium (Homologação)
Instalação separada pelo kubectl apply -f values.yaml



apiVersion: debezium.io/v1alpha1
kind: DebeziumServer
metadata:
  name: postgres-cdc
  namespace: debezium
spec:
  version: "3.6"
  runtime:
    environment:
      from:
        - secretRef:
            name: postgres-secret
        - secretRef:
            name: confluent-secret
  sink:
    type: kafka
    config:
      producer.bootstrap.servers: pkc-12pd3.brazilsouth.azure.confluent.cloud:9092

      producer.key.serializer: org.apache.kafka.common.serialization.StringSerializer
      producer.value.serializer: org.apache.kafka.common.serialization.StringSerializer

      producer.security.protocol: SASL_SSL
      producer.sasl.mechanism: PLAIN

      producer.sasl.jaas.config: >
        org.apache.kafka.common.security.plain.PlainLoginModule required
        username="${API_KEY}"
        password="${API_SECRET}";

  source:
    class: io.debezium.connector.postgresql.PostgresConnector
    config:
      database.hostname: sql-credit-consent-test.postgres.database.azure.com
      database.port: "5432"
      database.user: "${DB_USER}"
      database.password: "${DB_PASSWORD}"
      database.dbname: db_credit_consent

      plugin.name: pgoutput
      publication.name: dbz_publication
      slot.name: debezium_slot

      topic.prefix: test.debezium
      publication.autocreate.mode: filtered
      schema.include.list: public

  format:
    key:
      type: json
    value:
      type: json

Automação por pipeline (DEV/HML/PROD)

Para AKS, o fluxo recomendado agora é declarativo e idempotente por pipeline, sem registro manual recorrente por curl.

Arquivos adicionados no repositório:

- `.azuredevops/cd-debezium.yaml` (pipeline complementar)
- `.azuredevops/debezium/reconcile.sh` (render + apply idempotente)
- `.azuredevops/debezium/*.yaml.tmpl` (templates de Namespace/Secrets/DebeziumServer)
- `.azuredevops/config/<env>/debezium.defaults.env` (defaults por ambiente)

Execução local de validação (sem aplicar):

```bash
.azuredevops/debezium/reconcile.sh hml --dry-run
```

Execução com apply:

```bash
.azuredevops/debezium/reconcile.sh hml
```

Regras de fallback de configuração:

1. Valor recebido por variável de ambiente/pipeline vence.
2. Se ausente, usa defaults versionados em `.azuredevops/config/<env>/debezium.defaults.env`.
3. Segredos permanecem fora do Git (pipeline/Key Vault):
   - `DEBEZIUM_DATABASE_PASSWORD`
   - `DEBEZIUM_API_KEY`
   - `DEBEZIUM_API_SECRET`

Padrão local vs AKS:

- Local (`debezium-docker-compose.yaml`): `connect-init` com curl continua válido apenas para ambiente local.
- AKS (DEV/HML/PROD): usar DebeziumServer CRD via pipeline complementar.

Guardrails:

1. Não reutilizar `slot.name`/`publication.name` entre conectores diferentes.
2. Manter `schema.include.list=credit_consent` para evitar captura vazia da outbox.
3. O tópico de negócio continua vindo da coluna outbox `topic_name`; `topic.prefix` é metadado técnico de ambiente.