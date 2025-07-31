#!/bin/bash

# Development setup script

echo "🚀 Setting up development environment..."

# Start only PostgreSQL
echo "📦 Starting PostgreSQL container..."
docker-compose -f docker-compose.dev.yml up -d

# Wait for database to be ready
echo "⏳ Waiting for database to be ready..."
sleep 5

# Start Spring Boot application
echo "🌱 Starting Spring Boot application..."
cd backend
mvn spring-boot:run
