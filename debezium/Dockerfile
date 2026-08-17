FROM confluentinc/cp-kafka-connect:8.2.3

RUN confluent-hub install --no-prompt confluentinc/kafka-connect-avro-converter:8.2.3 && \
    confluent-hub install --no-prompt debezium/debezium-connector-postgresql:3.2.6-2
