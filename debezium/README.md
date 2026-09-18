# Debezium — configs do ambiente local e guia para HML/prod

Esta pasta reúne toda a configuração do Debezium usada pelo `ms-vivopay-credit-consent-bms-v1`.
O ambiente local roda o **mesmo runtime que HML/prod**: **Debezium Server** (app Quarkus
standalone), não Kafka Connect. Em HML/prod ele é provisionado pelo Debezium Operator via CRD
`DebeziumServer` (chart `helm-chart-generic-java-debezium`, `templates/debezium.yaml`, bloco
`debezium:` do `values.yml` de cada ambiente — ver [`../infra_aks_debezium.md`](../infra_aks_debezium.md)
e [`../debezium-configuration-confluence.md`](../debezium-configuration-confluence.md) para o guia
completo); localmente, `debezium-docker-compose.yaml` sobe o mesmo binário direto num container,
configurado por variáveis de ambiente `DEBEZIUM_SINK_*`/`DEBEZIUM_SOURCE_*`/`DEBEZIUM_FORMAT_*`
que traduzem, linha a linha, o `spec.sink`/`spec.source`/`spec.format` daquele CRD. Não existe
REST API para registrar connectors — a captura começa sozinha no boot do container, lendo sua
própria config.

## Arquivos

| Arquivo | O que é | Status |
|---|---|---|
| `debezium-docker-compose.yaml` | Stack local completa (Postgres, Kafka, Schema Registry, Debezium Server, observabilidade) | **Fonte de verdade** — é o que realmente roda localmente |
| `Dockerfile` | Imagem `quay.io/debezium/server:3.6` + Avro converter (ver seção "Imagem" abaixo) | **Fonte de verdade** |
| `debezium.json` | Config do connector Postgres (`public.tb_outbox_events`, outbox do próprio BMS) no formato Kafka-Connect-JSON | **Referência/documentação** — mapeia campo a campo para as `DEBEZIUM_SOURCE_*` do compose, mas não é mais aplicado automaticamente em lugar nenhum |

O tópico de destino de cada linha capturada vem da coluna outbox `topic_name`, nunca deste
arquivo — hoje sempre `vivopay.credit.consent.events.v1` para o BMS.

### ⚠️ A outbox do worker não é capturada hoje — nem localmente, nem em HML/prod

Debezium Server só roda **uma fonte por instância** (diferente do Kafka Connect, que este ambiente
usava antes e rodava os dois connectors — BMS e worker — num único worker local). O
`tableIncludeList` configurado em `.azuredevops/config/{hml,prod}/values.yml` do BMS hoje só lista
`public.tb_outbox_events` — a outbox do worker (`credit_consent_worker.tb_outbox_events`, que
publicaria em `vivopay.credit.engine.events.v1`, evento `RefreshOffersEvent`) não está configurada
em nenhum ambiente gerenciado. O ambiente local espelha essa mesma lacuna deliberadamente, em vez
de mascará-la rodando uma segunda instância que HML/prod não têm.

Havia um arquivo `debezium-worker-outbox.json` aqui documentando campo a campo o connector que
essa captura precisaria — ele foi **removido** por não descrever nada que estivesse de fato
rodando em lugar nenhum (nem localmente, nem em HML/prod), o que criava a impressão enganosa de
ser config ativa. Se essa captura for implementada no futuro, siga o mesmo padrão de campos de
`debezium.json` (adaptando `database.dbname`/`table.include.list`/`slot.name`/
`publication.name` para a tabela do worker) e suba uma segunda instância do serviço `debezium`
no compose local + o bloco `debezium:` equivalente em HML/prod.

## ⚠️ Regra: todo tópico novo publicado via outbox pattern precisa de configuração aqui

Sempre que uma tabela outbox nova — do próprio BMS ou de qualquer outro serviço, como aconteceu
com o outbox do worker — precisar publicar em um tópico Kafka via CDC, a config do Debezium
correspondente **tem que existir nesta pasta**, não só no repositório dono da tabela — o BMS é
quem provisiona/atualiza o listener do Debezium em todo ambiente (local via este
`debezium-docker-compose.yaml`, HML/prod via o bloco `debezium:` do `values.yml`, aplicado pelo
`helm upgrade` do pipeline de CD normal do BMS).

Como o Debezium Server só cobre uma tabela por instância, adicionar uma tabela outbox nova hoje
significa: estender `table.include.list`/`schema.include.list` (se a tabela nova estiver na mesma
conexão Postgres já usada) **ou** subir uma segunda instância do serviço `debezium` no compose
(e o bloco `debezium:` equivalente em HML/prod) — não é mais "adicionar um JSON e listar no
`connect-init`", que não existe mais.

Um repositório dono de uma tabela outbox nova (ex.: o worker) pode manter uma cópia de referência
da config do seu próprio connector para documentação local — mas essa cópia nunca é aplicada
automaticamente em nenhum ambiente; a config que efetivamente roda em cada ambiente sempre vive
aqui no BMS (`debezium-docker-compose.yaml` local, bloco `debezium:` do `values.yml` em
Kubernetes). Evite manter aqui um arquivo de referência para uma tabela que não tem nenhuma
config ativa em lugar nenhum (foi o caso de `debezium-worker-outbox.json`, removido — ver nota
acima) — isso cria a falsa impressão de que a captura já existe.

## Ambiente local

```bash
# a partir da raiz do repo
docker compose -f debezium/debezium-docker-compose.yaml up -d
```

Não há passo de registro separado — o container `debezium` já sobe capturando. Ele só consegue
começar depois que a tabela `tb_outbox_events` existir no Postgres (criada pelo Flyway do BMS no
boot); até lá ele fica em `restart: unless-stopped`, tentando de novo. Suba o BMS
(`./mvnw spring-boot:run`, que roda o Flyway) antes ou depois — o container do Debezium se
recupera sozinho assim que a tabela aparecer. Depois de mudar qualquer `DEBEZIUM_*` no compose,
`docker compose -f debezium/debezium-docker-compose.yaml up -d --build debezium` reconstrói e
reinicia só esse serviço.

Se o container falhar no boot com `NullPointerException` (geralmente envolvendo
`route.topic.replacement`) ou `IOException: Is a directory` (offset store), veja a tabela de
troubleshooting em [`../debezium-configuration-confluence.md`](../debezium-configuration-confluence.md#9-troubleshooting-comum)
— são duas pegadinhas específicas de env var do Debezium Server, já resolvidas na config atual do
compose, mas fáceis de reintroduzir ao editar `DEBEZIUM_SOURCE_*` sem saber da causa raiz.

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
| Debezium Server (health/metrics) | http://localhost:8083/q/health | sem autenticação — não é mais uma API de gerenciamento de connectors |

## Guardrails (mesmos de `../infra_aks_debezium.md`)

1. Nunca reusar `slot.name`/`publication.name` entre connectors diferentes.
2. Manter `table.include.list`/`schema.include.list` restrito à tabela outbox específica — evita capturar uma publicação vazia.
3. O tópico de negócio sempre vem da coluna outbox `topic_name` (via `transforms.outbox.route.by.field`), nunca de `topic.prefix` — esse último é só metadado técnico do ambiente.

## Imagem do Debezium Server (`Dockerfile`)

O serviço `debezium` do compose usa `build: context: .` (esta pasta) para gerar a imagem
`ms-vivopay-credit-consent-debezium-server-avro:3.6`, a partir do `Dockerfile` desta pasta:

```dockerfile
FROM quay.io/debezium/server:3.6

USER root
RUN cp /debezium/connectors/debezium-connector-cassandra-5/kafka-connect-avro-converter-*.jar \
       /debezium/connectors/debezium-connector-cassandra-5/kafka-connect-avro-data-*.jar \
       /debezium/connectors/debezium-connector-cassandra-5/avro-*.jar \
       /debezium/connectors/debezium-connector-cassandra-5/kafka-avro-serializer-*.jar \
       /debezium/connectors/debezium-connector-cassandra-5/kafka-schema-registry-client-*.jar \
       /debezium/connectors/debezium-connector-cassandra-5/kafka-schema-serializer-*.jar \
       /debezium/lib/
USER 185
```

Base `quay.io/debezium/server:3.6` — a mesma imagem oficial e a mesma versão que
`helm-chart-generic-java-debezium/values.yaml` usa por padrão em HML/prod (`debezium.version:
"3.6"`, sem `debezium.image` customizada configurada em nenhum `values.yml` de ambiente hoje). O
connector Postgres já vem embutido nessa imagem — diferente da antiga base Kafka Connect, não é
preciso instalar nada via `confluent-hub`.

O único componente que falta é o `io.confluent.connect.avro.AvroConverter`: a partir do Debezium
2.0, os containers oficiais pararam de incluir o suporte ao Confluent Schema Registry (confirmado
na [documentação oficial de Avro do Debezium 3.6](https://debezium.io/documentation/reference/3.6/configuration/avro.html)).
Os jars necessários já vêm dentro da imagem, só no lugar errado: empacotados junto do connector
Cassandra (não usado por este pipeline), em `/debezium/connectors/debezium-connector-cassandra-5/`.
O `Dockerfile` só copia esses 6 jars para `/debezium/lib/`, sem baixar nada da rede — build-time em
vez de runtime.

**Isso é a Opção A** descrita em [`../debezium-configuration-confluence.md`](../debezium-configuration-confluence.md)
(seção 7.1) — a alternativa recomendada pelo próprio Debezium, validada localmente (boot ~10s), ao
mecanismo que HML/prod usam hoje (`extraEnvVars: EXTRA_CONNECTOR=cassandra-5`, um workaround não
documentado que resolve o mesmo `ClassNotFoundException` emprestando **toda** a pasta do connector
Cassandra em runtime — boot de ~5-6min, e explicitamente marcado como não recomendado para
produção). É a única peça deste setup local que não é um espelho byte-a-byte de HML/prod — todo o
resto (runtime Debezium Server, shape da config via `sink`/`source`/`format`, `transforms.outbox.*`)
é o mesmo. Um efeito colateral bom dessa escolha: como o `EXTRA_CONNECTOR` também troca o binding
SLF4J ativo para Logback (motivo do `logbackConfig`/`JAVA_OPTS` em HML/prod, ver seção 5.4 do guia),
e a Opção A não traz esse efeito colateral, o logging aqui usa `QUARKUS_LOG_*` nativo — não precisa
do hack de `logbackConfig`.

A imagem `examples/debezium-confluent-image/Dockerfile` do repo `helm-chart-generic-java-debezium`
segue o mesmo padrão — ainda sem pipeline de build/publish para uso em HML/prod (ver o guia para o
comparativo completo entre as opções).
