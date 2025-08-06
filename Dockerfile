# Dockerfile primary to have single well-known entrypoint
# From https://www.graalvm.org/latest/docs/getting-started/container-images/
FROM quay.io/quarkus/ubi-quarkus-mandrel-builder-image:jdk-21 as builder

WORKDIR /app

COPY --chown=1001 . /app

RUN chmod "g+rwX" /app

# Tests run in separate CI task and require remote service unfortunately
RUN ./gradlew build -Dquarkus.native.enabled=true -Dquarkus.package.jar.enabled=false -x test

## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ## ##

#FROM quay.io/quarkus/quarkus-micro-image:2.0
FROM registry.access.redhat.com/ubi9/ubi-minimal:9.2

WORKDIR /work/
RUN chown 1001 /work \
    && chmod "g+rwX" /work \
    && chown 1001:root /work
COPY --from=builder --chown=1001:root /app/build/*-runner /work/application
EXPOSE 8080
USER 1001

CMD ["./application", "-Dquarkus.http.host=0.0.0.0"]
