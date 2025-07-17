FROM openjdk:21-jdk AS builder

WORKDIR /app

# layer 1 - cache all dependencies, update only if pom.xml changes
COPY .mvn/wrapper/maven-wrapper.properties .mvn/wrapper/
COPY pom.xml mvnw ./
RUN --mount=type=cache,target=/root/.m2 ./mvnw dependency:go-offline --batch-mode

# layer 2 - build if any file changed
COPY src ./src

# we need to copy the .git directory to get the commit hash
WORKDIR /app
COPY .git/ .git/

WORKDIR /app
RUN --mount=type=cache,target=/root/.m2 ./mvnw package --batch-mode

FROM openjdk:21-jdk-slim

RUN apt-get update && apt-get install -y curl && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app

ARG APP_USER=appuser
ARG APP_UID=10001
ARG APP_GID=10001

RUN groupadd  -g "${APP_GID}" "${APP_USER}" \
     && useradd   -m -u "${APP_UID}" -g "${APP_GID}" "${APP_USER}"

COPY --chown=${APP_USER}:${APP_USER} --from=builder /app/target/diceyvicy.jar diceyvicy.jar

EXPOSE 8080

USER ${APP_USER}

ENTRYPOINT ["java", "-jar", "diceyvicy.jar"]