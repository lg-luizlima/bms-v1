FROM acrsharedservices01.azurecr.io/docker.io/library/eclipse-temurin:21-jre-alpine

RUN echo "@edge https://dl-cdn.alpinelinux.org/alpine/edge/main" >> /etc/apk/repositories \
    && apk update \
    && apk upgrade --no-cache --available \
    && apk add --no-cache --upgrade musl@edge musl-utils@edge \
    && rm -rf /var/cache/apk/* \

RUN addgroup -g 1000 -S app && \
    adduser -u 1000 -S app -G app

ARG APPLICATION_NAME
ENV APPLICATION_NAME=$APPLICATION_NAME
ENV JAR_NAME=${APPLICATION_NAME}.jar

ARG APPLICATION_PATH
ENV APPLICATION_PATH=$APPLICATION_PATH

ADD ${APPLICATION_PATH}/${JAR_NAME} ${JAR_NAME}

VOLUME /config

ENV JAVA_MEMORY_RAM_MIN_PERCENTAGE=40
ENV JAVA_MEMORY_RAM_MAX_PERCENTAGE=70
ENV JAVA_OPTS_METASPACE_MIN=128m
ENV JAVA_OPTS_METASPACE_MAX=384m

ENV JAVA_OPTS="\
-XX:+UseG1GC \
-XX:MinRAMPercentage=${JAVA_MEMORY_RAM_MIN_PERCENTAGE} \
-XX:MaxRAMPercentage=${JAVA_MEMORY_RAM_MAX_PERCENTAGE} \
-XX:MetaspaceSize=${JAVA_OPTS_METASPACE_MIN} \
-XX:MaxMetaspaceSize=${JAVA_OPTS_METASPACE_MAX} \
-XX:InitialRAMPercentage=50 \
-XX:MaxGCPauseMillis=200 \
-XX:+UseStringDeduplication \
-XX:+ExitOnOutOfMemoryError \
-XX:+UseContainerSupport \
-Djava.security.egd=file:/dev/./urandom"


USER app

EXPOSE 8082

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar $JAR_NAME"]
