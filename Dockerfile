FROM clojure:openjdk-17-tools-deps-alpine as builder

RUN apk add --update --no-cache npm git
RUN mkdir /src
ADD . /src
WORKDIR /src
RUN clojure -Sdeps '{:mvn/local-repo "/src/.m2"}' -T:build:ui build/all

FROM openjdk:17-alpine

COPY --from=builder /src/target/relatient.jar app.jar
# ADD https://storage.googleapis.com/aidbox-public/jmx\_prometheus\_javaagent-0.15.0.jar prometheus-exporter.jar
# COPY infrastructure/base/prometheus/jmx.yaml jvm-exporter-config.yaml

CMD java \
    # -XX:-OmitStackTraceInFastThrow \
    # -Dcom.sun.management.jmxremote=true \
    # -Dcom.sun.management.jmxremote.ssl=false \
    # -Dcom.sun.management.jmxremote.authenticate=false \
    # -Dcom.sun.management.jmxremote.port=8082 \

    # -javaagent:prometheus-exporter.jar=8081:/jvm-exporter-config.yaml \
    -jar app.jar \
    -m main
