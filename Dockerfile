# Stage 1: Build stage
FROM maven:3.9-eclipse-temurin-21 AS build

# Set working directory
WORKDIR /app

# Build argument for GitHub token
ARG GITHUB_TOKEN
ARG GITHUB_USERNAME=Vinskao

# Convert ARG to ENV to ensure availability in RUN commands
ENV GITHUB_TOKEN=${GITHUB_TOKEN}
ENV GITHUB_USERNAME=${GITHUB_USERNAME}

# Create Maven settings.xml for GitHub Packages authentication
RUN mkdir -p /root/.m2 && \
    cat > /root/.m2/settings.xml <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
                              http://maven.apache.org/xsd/settings-1.0.0.xsd">
  <servers>
    <server>
      <id>github</id>
      <username>${GITHUB_USERNAME}</username>
      <password>${GITHUB_TOKEN}</password>
    </server>
  </servers>
</settings>
EOF

# Copy pom.xml and download dependencies (with retry tolerance for network issues)
COPY pom.xml .
RUN mvn dependency:resolve dependency:resolve-plugins -B || true

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests -Pplatform

# Stage 2: Runtime stage
FROM eclipse-temurin:21-jre-alpine

# Set working directory
WORKDIR /app

# Copy the built jar from build stage
COPY --from=build /app/target/ty-multiverse-gateway.jar app.jar

# Add non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Expose port
EXPOSE 8082

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8082/actuator/health || exit 1

# Set environment variables
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

