# syntax=docker/dockerfile:1

# ---------- Build stage ----------
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /workspace

# Leverage layer caching
COPY classroom/pom.xml ./
RUN mvn -B -q -DskipTests dependency:go-offline

COPY classroom ./
RUN mvn -B -DskipTests clean package

# ---------- Runtime stage ----------
FROM eclipse-temurin:17-jre
WORKDIR /app

# Copy the built jar (Spring Boot fat jar)
COPY --from=build /workspace/target/*.jar /app/app.jar

EXPOSE 8080

# Respect JVM options and Spring settings if provided
ENV JAVA_OPTS=""

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]

