#!/bin/bash

# Stop development environment

echo "🛑 Stopping development environment..."

# Stop Spring Boot application (if running via script)
echo "🌱 Stopping Spring Boot application..."
pkill -f "spring-boot:run" || echo "Spring Boot was not running via Maven"

# Stop PostgreSQL container
echo "📦 Stopping PostgreSQL container..."
docker-compose -f docker-compose.dev.yml down

echo "✅ Development environment stopped"
