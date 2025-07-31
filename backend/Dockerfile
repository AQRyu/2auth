# Use OpenJDK 21 as base image
FROM openjdk:21-jdk-slim

# Set working directory
WORKDIR /app

# Install curl for health checks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Copy Maven wrapper and pom.xml
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Make Maven wrapper executable and download dependencies
RUN chmod +x ./mvnw && \
    ./mvnw dependency:go-offline -B

# Copy source code
COPY src src

# Build the application and create non-root user
RUN ./mvnw clean package -DskipTests && \
    addgroup --system spring && \
    adduser --system spring --ingroup spring

# Switch to non-root user
USER spring:spring

# Expose port
EXPOSE 8080

# Run the application
ENTRYPOINT ["java", "-jar", "target/auth-0.0.1-SNAPSHOT.jar"]
