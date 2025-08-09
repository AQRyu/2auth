# Context Path Removal - /auth to Root

## Summary

Successfully removed the `/auth` context path from the Spring Boot application while maintaining authentication semantics in specific API endpoints. The application now runs on the root path `/` instead of `/auth`.

## Changes Made

### 1. Application Configuration

**Files:**

- `/app/src/main/resources/application.properties`
- `/app/src/main/resources/application-docker.properties`

**Changes:**

- Removed `server.servlet.context-path=/auth`
- Added comment explaining the change

### 2. API Endpoints Structure

The API endpoints are now organized as follows:

**Authentication endpoints:** `/api/auth/*`

- POST `/api/auth/login`
- POST `/api/auth/refresh`
- POST `/api/auth/logout`

**Admin endpoints:** `/api/admin/*`

- GET `/api/admin/users`
- POST `/api/admin/users`
- PUT `/api/admin/users/{id}`
- DELETE `/api/admin/users/{id}`
- etc.

**User endpoints:** `/api/user/*`

- GET `/api/user/profile`
- POST `/api/user/totp/enable`
- POST `/api/user/totp/confirm`
- POST `/api/user/totp/disable`
- etc.

**OAuth2 endpoints:** `/oauth2/*`

- GET `/oauth2/userinfo`
- etc.

**Health endpoint:** `/api/health`

### 3. Frontend JavaScript

**File:** `/app/src/main/resources/static/admin-dashboard.js`

**Changes:**

- Updated `API_BASE` from `/auth/api` to `/api`
- All endpoint calls now work correctly with the new structure

### 4. OpenAPI Configuration

**File:** `/app/src/main/java/com/aqryuz/auth/config/OpenApiConfig.java`

**Changes:**

- Removed dependency on `server.servlet.context-path`
- Updated server URLs to use root path:
  - Development: `http://localhost:8080`
  - Production: `https://auth.homelab.local`
- Removed unused `@Value` import

### 5. Docker Configuration

**File:** `/docker-compose.yml`

**Changes:**

- Updated health check from `/auth/api/health` to `/api/health`

### 6. Documentation

**File:** `/README.md`

**Changes:**

- Updated API Base URL from `http://localhost:8080/auth` to `http://localhost:8080`
- Added specific authentication API reference: `http://localhost:8080/api/auth/*`
- Updated health check URL to `/api/health`
- Updated all API endpoint examples to remove `/auth` prefix:
  - `/auth/api/auth/login` → `/api/auth/login`
  - `/auth/api/admin/users` → `/api/admin/users`
  - `/auth/api/user/profile` → `/api/user/profile`
  - etc.

## Benefits

1. **Cleaner URLs**: Direct access without context path prefix
2. **Better semantics**: Authentication endpoints clearly identified by `/api/auth/` path
3. **Simplified configuration**: No context path management needed
4. **Consistent structure**: All API endpoints follow `/api/` pattern

## URL Migration

### Before

- Base URL: `http://localhost:8080/auth`
- Login: `http://localhost:8080/auth/api/auth/login`
- Admin: `http://localhost:8080/auth/api/admin/users`
- Health: `http://localhost:8080/auth/api/health`

### After

- Base URL: `http://localhost:8080`
- Login: `http://localhost:8080/api/auth/login`
- Admin: `http://localhost:8080/api/admin/users`
- Health: `http://localhost:8080/api/health`

## Testing

- ✅ Application compiles successfully
- ✅ Integration tests pass
- ✅ Context path removal verified in application logs
- ✅ API endpoints respond correctly
- ✅ Frontend JavaScript works with new endpoints

## Backward Compatibility

**Breaking Change**: Clients using the old `/auth` context path will need to update their endpoint URLs. The authentication logic itself remains unchanged, only the URL structure has been simplified.
