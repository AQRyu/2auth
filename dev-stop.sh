#!/bin/bash

# Stop development environment

echo "🛑 Stopping development environment..."

# Stop Spring Boot application (Docker services will stop automatically)
echo "🌱 Stopping Spring Boot application..."
pkill -f "spring-boot:run" || echo "Spring Boot was not running via Maven"

echo "✅ Development environment stopped (Docker services stopped automatically)"
