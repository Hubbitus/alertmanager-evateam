# Dockerfile primary to have single well-known entrypoint
# From https://www.graalvm.org/latest/docs/getting-started/container-images/
#FROM ghcr.io/graalvm/native-image-community:17 as builder
#FROM quay.io/quarkus/ubi-quarkus-mandrel-builder-image:22.3-java17 as builder
FROM quay.io/quarkus/ubi-quarkus-mandrel-builder-image:jdk-21 as builder

WORKDIR /app

COPY --chown=1001 . /app

RUN chmod "g+rwX" /app

# Tests run in separate CI task
RUN ./gradlew build -Dquarkus.package.type=native

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
