#!/bin/bash

# Development setup script using Spring Boot Docker Compose support

echo "🚀 Setting up development environment..."

# Spring Boot will automatically start PostgreSQL via Docker Compose
echo "🌱 Starting Spring Boot application (will auto-start PostgreSQL)..."
cd app
mvn spring-boot:run
