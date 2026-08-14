FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Copy dependency definition first for caching
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
RUN chmod +x mvnw

# Resolve and cache dependencies in a separate layer
RUN ./mvnw dependency:go-offline -B

# Copy source code and configuration files, and package the application
COPY checkstyle.xml .
COPY src src
RUN ./mvnw clean package -DskipTests -Dspotless.check.skip=true -Dcheckstyle.skip=true

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create a non-root system user and group for security hardening
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

# Copy JAR and set correct owner permissions
COPY --from=build --chown=appuser:appgroup /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]