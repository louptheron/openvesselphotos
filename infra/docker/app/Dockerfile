#####################
# Multi stage build #
#####################

###########################
# Build frontend with npm #
###########################
FROM node:26-alpine AS build-front

WORKDIR /tmp/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

###########################
# Build backend with gradle #
###########################
FROM gradle:9.8.1-jdk21 AS build-back

WORKDIR /home/gradle/app
COPY settings.gradle.kts build.gradle.kts ./
COPY backend/ backend/
# Served by Spring Boot as static files
COPY --from=build-front /tmp/frontend/build frontend/build
RUN gradle :backend:bootJar --no-daemon

######################
# Create final image #
######################
FROM azul/zulu-openjdk-alpine:21-jre

RUN adduser -D openvesselphotos
USER openvesselphotos
WORKDIR /home/openvesselphotos

COPY --from=build-back /home/gradle/app/backend/build/libs/openvesselphotos.jar ./

ENV OPENVESSELPHOTOS_SERVER_PORT=8880
EXPOSE 8880

# JVM options sized for 256 MB, as on Scalingo (see the Procfile). JAVA_OPTS comes last and overrides them.
ENV DEFAULT_JAVA_OPTS="-XX:+UseSerialGC -Xmx96m -XX:MaxMetaspaceSize=110m -XX:TieredStopAtLevel=1 -XX:ReservedCodeCacheSize=32m -XX:MaxDirectMemorySize=16m -Xss512k -XX:+ExitOnOutOfMemoryError"

CMD ["sh", "-c", "exec java $DEFAULT_JAVA_OPTS $JAVA_OPTS -jar openvesselphotos.jar"]
