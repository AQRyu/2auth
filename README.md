# HomeLabAuth - Centralized Authentication Service

A simplified, centralized authentication service for home lab applications, similar to Keycloak but much lighter and easier to deploy.

## Features

- **Authentication Methods**: Username/password + TOTP (Time-based One-Time Password)
- **User Management**: Admin dashboard with full CRUD operations
- **Protocol Support**: OAuth 2.0 / OpenID Connect, JWT tokens
- **Security Features**: Account lockout, password complexity, audit logging
- **Database**: PostgreSQL
- **Deployment**: Docker container ready

## Quick Start

### Prerequisites

- Docker and Docker Compose
- Mini PC or server for deployment

### 1. Clone and Deploy

```bash
git clone <your-repo>
cd 2auth
docker-compose up -d
```

### 2. Access the Service

- **API Base URL**: `http://localhost:8080/auth`
- **Health Check**: `http://localhost:8080/auth/api/health`
- **Default Admin**: username `admin`, password `admin123`

⚠️ **Important**: Change the default admin password immediately after first login!

### 3. Environment Variables (Production)

Create a `.env` file in the root directory:

```env
# Database
POSTGRES_DB=auth_db
POSTGRES_USER=auth_user
POSTGRES_PASSWORD=your_secure_password

# JWT
JWT_SECRET=your_very_long_and_secure_jwt_secret_key_here
JWT_EXPIRATION=86400000
JWT_REFRESH_EXPIRATION=604800000

# TOTP
TOTP_ISSUER=YourHomeLab
TOTP_PERIOD=30
TOTP_DIGITS=6

# OAuth2
OAUTH2_CLIENT_ID=your_client_id
OAUTH2_CLIENT_SECRET=your_client_secret
OAUTH2_REDIRECT_URI=http://your-domain.com/callback

# Admin
ADMIN_USERNAME=admin
ADMIN_PASSWORD=your_secure_admin_password
ADMIN_EMAIL=admin@yourdomain.com
```

## API Documentation

### Authentication Endpoints

#### Login

```bash
POST /auth/api/auth/login
Content-Type: application/json

{
  "usernameOrEmail": "admin",
  "password": "admin123",
  "totpCode": "123456"  // Optional, required if TOTP is enabled
}
```

Response:

```json
{
  "accessToken": "jwt_token_here",
  "refreshToken": "refresh_token_here",
  "tokenType": "Bearer",
  "expiresIn": 86400,
  "user": {
    "id": 1,
    "username": "admin",
    "email": "admin@homelab.local",
    "roles": ["ADMIN", "USER"]
  },
  "requireTotp": false
}
```

#### Refresh Token

```bash
POST /auth/api/auth/refresh
Content-Type: application/json

{
  "refreshToken": "refresh_token_here"
}
```

### User Management (Admin Only)

#### Get All Users

```bash
GET /auth/api/admin/users?page=0&size=10&sortBy=createdAt&sortDir=desc
Authorization: Bearer jwt_token_here
```

#### Create User

```bash
POST /auth/api/admin/users
Authorization: Bearer jwt_token_here
Content-Type: application/json

{
  "username": "newuser",
  "password": "password123",
  "email": "user@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "roles": ["USER"]
}
```

#### Update User

```bash
PUT /auth/api/admin/users/1
Authorization: Bearer jwt_token_here
Content-Type: application/json

{
  "email": "newemail@example.com",
  "accountEnabled": true,
  "roles": ["USER", "ADMIN"]
}
```

#### Delete User

```bash
DELETE /auth/api/admin/users/1
Authorization: Bearer jwt_token_here
```

### User Self-Management

#### Get Profile

```bash
GET /auth/api/user/profile
Authorization: Bearer jwt_token_here
```

#### Enable TOTP

```bash
POST /auth/api/user/totp/enable
Authorization: Bearer jwt_token_here
```

Response includes QR code for authenticator app setup.

#### Confirm TOTP

```bash
POST /auth/api/user/totp/confirm
Authorization: Bearer jwt_token_here
Content-Type: application/json

{
  "totpCode": "123456"
}
```

#### Disable TOTP

```bash
POST /auth/api/user/totp/disable
Authorization: Bearer jwt_token_here
```

## Integration Examples

### Web Application (JavaScript)

```javascript
// Login
const login = async (username, password, totpCode) => {
  const response = await fetch('http://localhost:8080/auth/api/auth/login', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      usernameOrEmail: username,
      password: password,
      totpCode: totpCode
    })
  });
  
  const data = await response.json();
  
  if (data.accessToken) {
    localStorage.setItem('accessToken', data.accessToken);
    localStorage.setItem('refreshToken', data.refreshToken);
  }
  
  return data;
};

// Make authenticated requests
const makeAuthenticatedRequest = async (url, options = {}) => {
  const token = localStorage.getItem('accessToken');
  
  return fetch(url, {
    ...options,
    headers: {
      ...options.headers,
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/json'
    }
  });
};
```

### Spring Boot Application

```java
@Component
public class AuthClient {
    
    @Value("${auth.service.url}")
    private String authServiceUrl;
    
    public boolean validateToken(String token) {
        // Implement JWT validation or call auth service
        return true;
    }
    
    public UserInfo getUserInfo(String token) {
        // Extract user info from JWT or call auth service
        return new UserInfo();
    }
}
```

## Security Considerations

1. **Change Default Credentials**: Always change the default admin password
2. **Use HTTPS**: Deploy behind a reverse proxy with SSL/TLS
3. **Secure JWT Secret**: Use a long, random JWT secret key
4. **Database Security**: Use strong database credentials
5. **Network Security**: Restrict database access to the application only
6. **Regular Updates**: Keep the container images updated

## Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Web App 1     │    │   Web App 2     │    │   API Service   │
│                 │    │                 │    │                 │
└─────────┬───────┘    └─────────┬───────┘    └─────────┬───────┘
          │                      │                      │
          │                      │                      │
          └──────────────────────┼──────────────────────┘
                                 │
                   ┌─────────────▼──────────────┐
                   │    HomeLabAuth Service     │
                   │  ┌─────────────────────┐   │
                   │  │  Spring Boot App   │   │
                   │  │  - JWT Auth        │   │
                   │  │  - TOTP Support    │   │
                   │  │  - User Management │   │
                   │  │  - OAuth2/OIDC     │   │
                   │  └─────────────────────┘   │
                   └─────────────┬──────────────┘
                                 │
                   ┌─────────────▼──────────────┐
                   │     PostgreSQL DB          │
                   │  - Users & Roles          │
                   │  - TOTP Secrets           │
                   │  - Audit Logs             │
                   └───────────────────────────┘
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Submit a pull request

## License

This project is licensed under the MIT License - see the LICENSE file for details.
