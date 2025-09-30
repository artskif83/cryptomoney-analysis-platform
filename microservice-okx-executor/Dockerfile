# syntax=docker/dockerfile:1.7

ARG MODULE=microservice-okx-executor

############################
# 1) Build stage
############################
FROM eclipse-temurin:21-jdk AS builder
ARG MODULE
WORKDIR /repo

# --- Базовые файлы для кеширования зависимостей
COPY gradlew settings.gradle* build.gradle* ./
COPY gradle ./gradle

# Копируем build-файлы конкретного модуля (для кеша deps)
# Если у модуля есть дополнительный gradle-скрипт(ы) — тоже скопируются маской
COPY ${MODULE}/build.gradle* ${MODULE}/

# Дадим права и прогреем кэш зависимостей
RUN chmod +x ./gradlew \
 && ./gradlew --no-daemon -v \
 && ./gradlew --no-daemon :${MODULE}:dependencies || true

# --- Теперь копируем остальной код репозитория
# (после прогрева deps изменения в исходниках не ломают кэш)
COPY . .

# Собираем только нужный модуль (без тестов)
RUN ./gradlew --no-daemon :${MODULE}:bootJar -x test

############################
# 2) Runtime stage
############################
FROM eclipse-temurin:21-jre-alpine
ARG MODULE
ENV TZ=UTC \
    JAVA_OPTS="-XX:+UseZGC -XX:+ZGenerational -XX:MaxRAMPercentage=75.0 -Dfile.encoding=UTF-8" \
    SPRING_PROFILES_ACTIVE=prod \
    SERVER_PORT=8082

# Непривилегированный пользователь
RUN addgroup -S app && adduser -S app -G app
USER app
WORKDIR /app

# Копируем собранный jar из конкретного модуля
COPY --from=builder /repo/${MODULE}/build/libs/*.jar /app/app.jar

EXPOSE ${SERVER_PORT}

ENTRYPOINT ["sh","-c","java $JAVA_OPTS -Dserver.port=$SERVER_PORT -jar /app/app.jar"]