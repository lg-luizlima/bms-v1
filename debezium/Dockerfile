FROM confluentinc/cp-kafka-connect-base:7.6.1

RUN confluent-hub install --no-prompt confluentinc/kafka-connect-avro-converter:7.6.1 && \
    confluent-hub install --no-prompt debezium/debezium-connector-postgresql:2.5.4-2
