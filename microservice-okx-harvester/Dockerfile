# syntax=docker/dockerfile:1.6

# Можно переопределить модуль при сборке: --build-arg MODULE=...
ARG MODULE=microservice-okx-harvester

# --- 1) Builder: собираем native runner
FROM quay.io/quarkus/ubi9-quarkus-mandrel-builder-image:23.1-java21 AS build
ARG MODULE
WORKDIR /workspace

# Gradle cache в рабочем каталоге
ENV GRADLE_USER_HOME=/workspace/.gradle

# Готовим права на весь workspace
USER root
RUN mkdir -p /workspace && chown -R 1001:0 /workspace

# Копируем проект СРАЗУ с правильным владельцем (иначе будет root:root и нельзя писать)
COPY --chown=1001:0 . .

# Дальше работаем непривилегированным юзером
USER 1001

# Чиним wrapper и убираем случайный .gradle из репо
RUN sed -i 's/\r$//' ./gradlew || true && chmod +x ./gradlew
RUN rm -rf .gradle || true

# Сборка native + кэш Gradle (важно: uid/gid совпадают с пользователем процесса)
RUN --mount=type=cache,target=/workspace/.gradle,uid=1001,gid=0 \
    ./gradlew --gradle-user-home /workspace/.gradle -p ${MODULE} \
      -Dquarkus.native.enabled=true \
      -Dquarkus.package.jar.enabled=false \
      -Dquarkus.native.native-image-xmx=4g \
      -x test \
      clean quarkusBuild

# Проверяем, что раннер собран
RUN ls -l /workspace/${MODULE}/build/ && test -f /workspace/${MODULE}/build/*-runner

# --- 2) Runtime: минимальный образ без JVM
FROM quay.io/quarkus/ubi9-quarkus-micro-image:2.0
ARG MODULE
WORKDIR /work/

# Копируем бинарь из builder-стадии
COPY --from=build --chown=1001:root --chmod=0755 \
     /workspace/${MODULE}/build/*-runner /work/application

# Чуть поправим права на /work
RUN chown 1001 /work && chmod g+rwX /work && chown 1001:root /work

EXPOSE 8080
USER 1001
ENTRYPOINT ["./application","-Dquarkus.http.host=0.0.0.0"]