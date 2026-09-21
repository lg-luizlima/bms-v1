FROM quay.io/debezium/server:3.6

# io.confluent.connect.avro.AvroConverter isn't on the official image's classpath (/debezium/lib) —
# Debezium's own Avro docs confirm Confluent Schema Registry support was dropped from the
# containers in 2.0. The jars it needs already ship inside the (otherwise unused) Cassandra
# connector directory, so copy them out instead of downloading anything from the network. Same
# pattern as helm-chart-generic-java-debezium/examples/debezium-confluent-image/Dockerfile — the
# recommended (but not yet built/published) alternative to the EXTRA_CONNECTOR=cassandra-5
# runtime workaround HML/prod use today.
USER root
RUN cp /debezium/connectors/debezium-connector-cassandra-5/kafka-connect-avro-converter-*.jar \
       /debezium/connectors/debezium-connector-cassandra-5/kafka-connect-avro-data-*.jar \
       /debezium/connectors/debezium-connector-cassandra-5/avro-*.jar \
       /debezium/connectors/debezium-connector-cassandra-5/kafka-avro-serializer-*.jar \
       /debezium/connectors/debezium-connector-cassandra-5/kafka-schema-registry-client-*.jar \
       /debezium/connectors/debezium-connector-cassandra-5/kafka-schema-serializer-*.jar \
       /debezium/lib/
USER 185
