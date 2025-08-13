# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Initial release of HomeLabAuth
- JWT-based authentication system
- TOTP (Time-based One-Time Password) support
- User management with admin dashboard
- OAuth 2.0 / OpenID Connect protocol support
- PostgreSQL database integration
- Docker containerization
- RESTful API with OpenAPI documentation
- Account lockout and security features
- Role-based access control (USER/ADMIN)
- Session management with sliding window
- Comprehensive integration and UI tests

### Security
- Password complexity requirements
- Account lockout after failed attempts
- JWT token security with configurable expiration
- TOTP encryption for 2FA secrets
- Audit logging for security events

## [1.0.0] - 2025-08-12

### Added
- Initial stable release
- Core authentication functionality
- Admin dashboard
- API documentation
- Docker deployment support
- Comprehensive test suite

[Unreleased]: https://github.com/AQRyu/2auth/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/AQRyu/2auth/releases/tag/v1.0.0
