# Debezium — configs do ambiente local e guia para HML/prod

Esta pasta reúne toda a configuração do Debezium/Kafka Connect usada pelo `ms-vivopay-credit-consent-bms-v1`. Ela tem dois papéis:

1. **Sobe o ambiente local** — `debezium-docker-compose.yaml` traz Postgres/Kafka/Schema Registry/Kafka Connect (+ stack de observabilidade) e registra automaticamente os connectors listados abaixo.
2. **Serve de guia para configurar o Debezium em HML/prod** — o bloco `config` de cada `debezium-*.json` aqui é o que precisa ser traduzido para o `spec.source.config` do CRD `DebeziumServer` (Debezium Operator, Helm), documentado em [`../infra_aks_debezium.md`](../infra_aks_debezium.md). Os campos são os mesmos (`plugin.name`, `slot.name`, `publication.name`, `table.include.list`, os `transforms.outbox.*`), só a forma de aplicar muda: aqui é um `POST` direto contra a REST API do Connect (via `connect-init`); em HML/prod é uma CR do Kubernetes aplicada pelo pipeline complementar descrito naquele documento.

## Arquivos

| Arquivo | Connector | O que captura | Tópico de destino |
|---|---|---|---|
| `debezium.json` | `credit-consent-outbox-connector` | `credit_consent.tb_outbox_events` (outbox do próprio BMS) | vem da coluna `topic_name` de cada linha — hoje sempre `vivopay.credit.consent.events.v1` |
| `debezium-worker-outbox.json` | `credit-consent-worker-outbox-connector` | `credit_consent_worker.tb_outbox_events` (outbox do `ms-vivopay-credit-consent-worker-v1`) | vem da coluna `topic_name` — hoje sempre `vivopay.credit.engine.events.v1` (evento `RefreshOffersEvent`) |
| `debezium-docker-compose.yaml` | — | stack local completa (Postgres, Kafka, Schema Registry, Kafka Connect, observabilidade) | — |

A cópia de `debezium-worker-outbox.json` que existe em `ms-vivopay-credit-consent-worker-v1/scripts/worker-outbox-debezium.json` é só documentação/referência — não é registrada automaticamente em lugar nenhum. **A cópia aqui é a fonte oficial.**

## ⚠️ Regra: todo tópico novo publicado via outbox pattern precisa de connector aqui

Sempre que uma tabela outbox nova — do próprio BMS ou de qualquer outro serviço, como aconteceu com o outbox do worker — precisar publicar em um tópico Kafka via CDC, a config do connector Debezium correspondente **tem que existir nesta pasta**, não só no repositório dono da tabela.

Isso é assim porque o BMS é quem é responsável por provisionar/atualizar o listener do Debezium:
- **Localmente**, é o `connect-init` deste `debezium-docker-compose.yaml` que registra todos os connectors.
- **Em HML/prod**, será o pipeline complementar (`.azuredevops/cd-debezium.yaml` + `.azuredevops/debezium/reconcile.sh`, descritos em [`../infra_aks_debezium.md`](../infra_aks_debezium.md)) quem sobe/atualiza o pod do Debezium com a lista completa de connectors — e esse pipeline vive no BMS.

Um repositório dono de uma tabela outbox nova (ex.: o worker) pode manter uma cópia de referência do JSON do seu próprio connector para documentação local — mas essa cópia nunca é aplicada automaticamente em nenhum ambiente. Adicionar um novo `debezium-<nome>.json` aqui + registrá-lo no `connect-init` (local) é o passo obrigatório para o tópico realmente existir.

## Ambiente local

```bash
# a partir da raiz do repo
docker compose -f debezium/debezium-docker-compose.yaml up -d postgres redis kafka schema-registry debezium
# aguardar Kafka Connect responder em :8083/connectors, então:
docker compose -f debezium/debezium-docker-compose.yaml up connect-init
```

`connect-init` é idempotente (faz `DELETE` seguido de `POST` para cada connector) — pode ser rodado de novo a qualquer momento para re-registrar depois de mudar um dos JSONs.

**Ordem importa**: os dois connectors só registram com sucesso depois que a tabela outbox correspondente já existe no Postgres (criada pelo Flyway da aplicação dona dela). Por isso o BMS e o worker precisam estar de pé (`spring-boot:run`, que roda o Flyway no boot) antes de rodar `connect-init` — registrar antes disso falha com `No table filters found for filtered publication`.

### Acesso rápido às UIs — nenhuma pede login

Um único `docker compose -f debezium/debezium-docker-compose.yaml up -d` sobe tudo — não existe
mais um passo separado de "infra básica" antes deste. Todas as interfaces web abrem direto, sem
tela de login:

| Ferramenta | URL | Observação |
|---|---|---|
| Grafana | http://localhost:3000 | acesso anônimo como Admin (`GF_AUTH_ANONYMOUS_ENABLED`) |
| Kafka UI | http://localhost:8080 | sem autenticação configurada |
| pgAdmin | http://localhost:5051 | modo desktop (`PGADMIN_CONFIG_SERVER_MODE: 'False'`), sem tela de login; a conexão com o Postgres local já vem pré-cadastrada (só pede a senha `postgres` ao expandir a árvore do servidor, isso é a senha do Postgres, não um login do pgAdmin) |
| RedisInsight | http://localhost:8001 | conexão com o Redis local já vem pré-configurada via `RI_REDIS_HOST`/`RI_REDIS_PORT`, EULA aceito via `RI_ACCEPT_TERMS_AND_CONDITIONS` |
| Jaeger | http://localhost:16686 | sem autenticação |
| Debezium Connect (REST) | http://localhost:8083/connectors | sem autenticação |

## Guardrails (mesmos de `../infra_aks_debezium.md`)

1. Nunca reusar `slot.name`/`publication.name` entre connectors diferentes.
2. Manter `table.include.list` (local) / `schema.include.list` (HML/prod) restrito à tabela outbox específica — evita capturar uma publicação vazia.
3. O tópico de negócio sempre vem da coluna outbox `topic_name` (via `transforms.outbox.route.by.field`), nunca de `topic.prefix` — esse último é só metadado técnico do ambiente.

## Imagem do Kafka Connect (`Dockerfile`)

O serviço `debezium` do compose usa `build: context: .` (esta pasta) para gerar a imagem `ms-vivopay-credit-consent-debezium-avro:2.7.0.Final`, a partir do `Dockerfile` desta pasta:

```dockerfile
FROM confluentinc/cp-kafka-connect:8.2.3

RUN confluent-hub install --no-prompt confluentinc/kafka-connect-avro-converter:8.2.3 && \
    confluent-hub install --no-prompt debezium/debezium-connector-postgresql:3.2.6-2
```

Base `confluentinc/cp-kafka-connect` (mesma versão `8.2.3` já usada no `kafka`/`schema-registry` do compose — a Confluent recomenda manter todos os componentes de uma stack na mesma versão de Confluent Platform) em vez da imagem oficial da Debezium, porque a Confluent já traz o CLI `confluent-hub` pronto — os dois componentes que realmente precisamos (o connector Postgres da Debezium e o conversor Avro da Confluent, exigido por `value.converter: io.confluent.connect.avro.AvroConverter` nos dois `debezium-*.json`) são baixados do Confluent Hub em tempo de build, sem depender de nenhum artefato local. Só instalamos o connector Postgres — a imagem oficial da Debezium viria com 9 connectors (MySQL, MongoDB, SQL Server, Oracle, DB2, Spanner, Vitess, Informix, IBM i) que não usamos.

`cp-kafka-connect` (não `cp-kafka-connect-base`) — a variante `-base` foi descontinuada a partir da Confluent Platform 8.3.0 e será removida na 8.4.0; a Confluent recomenda `cp-kafka-connect` como substituição direta (hoje funcionalmente idêntica).

**Por que `8.2.3` e não a versão mais recente da Confluent Platform (`8.3.x`)**: a partir da CP 8.3.0 as imagens base passaram a rodar em Java 25 por padrão, mas a própria Confluent documenta que conectores "não são certificados no Java 25" e recomenda Java 21 mesmo em CP 8.3.x. `8.2.x` é a última linha cujas imagens já vêm com Java 21 (Temurin 21) por padrão — o ponto certo entre "versão recente" e "certificada para connectors". Com Java 21 disponível, o connector Postgres da Debezium pôde voltar para a versão mais recente da linha 3.x publicada no Confluent Hub (`3.2.6-2`) — versões 3.x exigem Java 17+ em runtime (Java 11, usado pela antiga base `cp-kafka-connect-base:7.6.1`, rejeitava esses jars com `UnsupportedClassVersionError`).

Como a base muda de "imagem Debezium" para "imagem Confluent", as variáveis de ambiente do serviço `debezium` no compose usam o prefixo `CONNECT_` (convenção das imagens `cp-kafka-connect*`), não os nomes sem prefixo que a imagem da Debezium aceitaria.
