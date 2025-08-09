# Development Environment Guide

## Quick Start

### Option 1: Using Scripts (Recommended)

```bash
# Start development environment (DB + Spring Boot)
./dev-start.sh

# Stop development environment
./dev-stop.sh
```

### Option 2: Manual Steps

```bash
# 1. Start only PostgreSQL
docker-compose -f docker-compose.dev.yml up -d

# 2. Start Spring Boot application
cd app
mvn spring-boot:run

# 3. Stop when done
docker-compose -f docker-compose.dev.yml down
```

### Option 3: VS Code Tasks

- Press `Ctrl+Shift+P` (or `Cmd+Shift+P` on Mac)
- Type "Tasks: Run Task"
- Choose from:
  - 🌱 Start Spring Boot Only
  - 💾 Start Database Only
  - 🛑 Stop Dev Environment

## Development Benefits

✅ **Instant Changes**: HTML/CSS/JS changes are reflected immediately (no rebuild needed)
✅ **Fast Startup**: ~5 seconds vs ~70 seconds Docker rebuild
✅ **Hot Reload**: Spring Boot DevTools enables automatic restart on Java changes
✅ **Easy Debugging**: Attach debugger directly to Java process
✅ **Better Logging**: See logs directly in terminal

## Frontend Development

- Static files are served from: `app/src/main/resources/static/`
- Changes to `index.html`, `admin-dashboard.js` are **immediately visible**
- Just refresh the browser (F5) - no rebuilds needed!

## Backend Development

- Java changes trigger automatic restart (Spring Boot DevTools)
- Database schema changes are auto-applied (JPA DDL)
- API changes are immediately available

## URLs

- **Application**: <http://localhost:8080>
- **Database**: localhost:5432
  - DB: `auth_db`
  - User: `auth_user`
  - Password: `auth_password`

## Production Deployment

When ready for production, use the original docker-compose:

```bash
docker-compose up -d
```

## Troubleshooting

### Port 8080 already in use

```bash
# Find and kill the process
lsof -ti:8080 | xargs kill -9
```

### Database connection issues

```bash
# Restart database container
docker-compose -f docker-compose.dev.yml restart
```

### Clear Maven cache

```bash
cd app
mvn clean compile
```
