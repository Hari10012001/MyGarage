# Stage 1: Build
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder
WORKDIR /app

# Cache dependencies
COPY backend/pom.xml backend/pom.xml
RUN mvn -f backend/pom.xml dependency:go-offline -B

# Copy source code and build production artifact
COPY backend/src backend/src
RUN mvn -f backend/pom.xml clean package -DskipTests -B

# Stage 2: Production Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Copy generated executable Spring Boot JAR
COPY --from=builder /app/backend/target/mygarage-*.jar app.jar

# Render-ready default port exposure
EXPOSE 8080

# Memory tuning for Render Free Tier (512 MB memory boundary)
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70.0 -Xss512k -XX:MaxMetaspaceSize=128m -XX:+UseG1GC"

# Launch Spring Boot application
ENTRYPOINT ["java", "-jar", "app.jar"]
