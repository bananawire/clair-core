# clair-core

Spring Boot project (Java 25) with Swagger/OpenAPI.

## Features

- Spring Boot 3.x
- REST API with Swagger/OpenAPI
- Maven as dependency manager
- JWT Authentication with Refresh Tokens
- Email Verification Flow
- Redis Caching

## Requirements

- Java 25 (Maven must run on JDK 25)
- Maven 3.6 or higher
- PostgreSQL 15+
- Redis 7+

## Environment Variables

This project uses a `.env` file for configuration. Create a `.env` file in the root directory:

```env
PORT=49220

# Database
DB_URL=jdbc:postgresql://localhost:5432/clair_core
DB_USERNAME=postgres
DB_PASSWORD=admin

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_USERNAME=default
REDIS_PASSWORD=your_redis_password

# SMTP Email (Resend)
SMTP_HOST=smtp.resend.com
SMTP_PORT=465
SMTP_USERNAME=resend
SMTP_PASSWORD=your_resend_api_key
SMTP_FROM=noreply@yourdomain.com
SMTP_AUTH=true
SMTP_SSL=true
SMTP_STARTTLS=false

# JWT
JWT_SECRET=your_super_secret_jwt_key_that_is_at_least_32_characters_long
JWT_EXPIRATION=3600000
JWT_REFRESH_EXPIRATION=604800000

# CORS — tu web app Angular
CORS_ALLOWED_ORIGINS=http://localhost:4200

# Google OAuth 2.0
GOOGLE_OAUTH_CLIENT_ID=your_google_client_id
GOOGLE_OAUTH_ALLOWED_CLIENT_IDS=your_google_client_id
GOOGLE_OAUTH_CLIENT_SECRET=your_google_client_secret
GOOGLE_REDIRECT_URI=http://localhost:49220/api/v1/auth/google/callback

# Frontend redirects
FRONTEND_URL=http://localhost:4200

# Stripe
STRIPE_PRIVATE_KEY=sk_test_your_stripe_private_key
STRIPE_PUBLIC_KEY=pk_test_your_stripe_public_key
STRIPE_WEBHOOK_SECRET=whsec_your_stripe_webhook_secret

# OneSignal
ONESIGNAL_API_URL=https://onesignal.com/api/v1/notifications
ONESIGNAL_REST_API_KEY=your_onesignal_rest_api_key
ONESIGNAL_APP_ID=your_onesignal_app_id
ONESIGNAL_FRONTEND_URL=http://localhost:4200

# Factory inventory (optional). CSV columns: serial_number,hardware_id,api_key,name
DEVICE_PROVISIONING_IMPORT_PATH=
# Where the demo profile writes its generated inventory (contains API keys; keep it out of git)
DEVICE_PROVISIONING_EXPORT_PATH=provisioned-devices.csv

# Flyway
FLYWAY_BASELINE_ON_MIGRATE=false
LEGACY_AUDIT_ZONE=UTC
```

## Device inventory and the demo profile

Devices exist in the core before anyone registers them. There are two ways to get them there:

- **Import** a CSV at startup by setting `DEVICE_PROVISIONING_IMPORT_PATH`. Rows already present
  (same serial number or hardware id) are skipped, so the file can stay configured permanently.
- **Demo profile**: run with `SPRING_PROFILES_ACTIVE=demo` and the core seeds five units
  `CLAIR-0001`..`CLAIR-0005` with fresh API keys, then writes the whole inventory including keys to
  `DEVICE_PROVISIONING_EXPORT_PATH`. Flash a hardware id and its key into the firmware from that file.
  Without the profile nothing is seeded.

Registering a device in the app is a two-step ownership handshake, not discovery: `POST /api/v1/devices/pair`
with the hardware id returns a one-time claim token; `POST /api/v1/devices/claim` with that token and one of
your spaces makes you the owner. Telemetry from a unit that is in inventory but not yet claimed is accepted
and stored against the device; it becomes visible to whoever claims it only from the moment of that claim.

Database schemas are managed by Flyway. Empty databases migrate automatically on startup;
Hibernate validates the resulting schema. For an existing installation, follow
[the migration instructions](docs/audit/migrations.md) before its first Flyway deployment.

## Running on one laptop (local profile)

Start PostgreSQL, Redis and Mailpit with the compose file in the parent directory, then run the
core with the `local` profile. Every external integration has an explicit disabled behaviour:
Google login is refused (placeholder client id), Stripe checkout fails and plans stay FREEMIUM,
push notifications fail and are logged, and sign-up emails land in Mailpit at http://localhost:8025.

```bash
docker compose -f docker-compose.local.yml up -d
SPRING_PROFILES_ACTIVE=local,demo mvn spring-boot:run     # demo seeds CLAIR-0001..0005 and writes provisioned-devices.csv
```

`SMTP_AUTH`, `SMTP_SSL` and `SMTP_STARTTLS` control the mail transport in every profile. The full
laptop runbook, including the edge and the device, is `docs/RUNBOOK.md`.

## Compile the Project

```bash
mvn clean compile
```

Using Nix:
```bash
nix-shell --command "mvn clean compile"
```

## Run the Project

```bash
mvn spring-boot:run
```

Using Nix:
```bash
nix-shell --command "mvn spring-boot:run"
```

## Stripe CLI (Nix)

If you want Stripe CLI available via Nix:

```bash
nix-shell --command "stripe version"
```

Example webhook forward:

```bash
nix-shell --command "stripe listen --forward-to localhost:49220/api/v1/billing/webhook"
```

The server will be available at: `http://localhost:${PORT}` (Default: 49220)

## API Documentation

Access the interactive Swagger documentation at:
```
http://localhost:${PORT}/swagger-ui.html
```

Or view the OpenAPI JSON at:
```
http://localhost:${PORT}/v3/api-docs
```

## Authentication Endpoints

### 1. Sign Up
- **POST** `/api/v1/auth/sign-up`
- Body: `{ "email": "user@example.com", "password": "SecurePass123!" }`
- Response (201):
  ```json
  {
    "sessionId": "550e8400-e29b-41d4-a716-446655440000",
    "message": "Registration initiated. Please check your email for the verification code."
  }
  ```

### 2. Confirm Registration
- **POST** `/api/v1/auth/confirm`
- Body: `{ "sessionId": "550e8400-e29b-41d4-a716-446655440000", "verificationCode": "6G13-789D" }`
- Response (201):
  ```json
  {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "email": "user@example.com"
  }
  ```

### 3. Sign In
- **POST** `/api/v1/auth/sign-in`
- Body: `{ "email": "user@example.com", "password": "SecurePass123!" }`
- Response (200):
  ```json
  {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "email": "user@example.com",
    "token": "eyJhbGciOiJIUzI1NiIs...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIs..."
  }
  ```

### 4. Refresh Token
- **POST** `/api/v1/auth/refresh`
- Body: `{ "refreshToken": "eyJhbGciOiJIUzI1NiIs..." }`
- Response (200):
  ```json
  {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "email": "user@example.com",
    "token": "eyJhbGciOiJIUzI1NiIs...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIs..."
  }
  ```

### 5. Verify Token
- **GET** `/api/v1/auth/verify`
- Header: `Authorization: Bearer <token>`
- Response (200):
  ```json
  {
    "valid": true,
    "email": "user@example.com",
    "expiresAt": "2026-05-05T17:43:27.000Z"
  }
  ```

## Angular Integration

Tu web app en `http://localhost:4200` ya está permitida por CORS.

### Ejemplo de servicio en Angular:

```typescript
import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private apiUrl = 'http://localhost:49220/api/v1/auth';

  constructor(private http: HttpClient) {}

  signUp(email: string, password: string) {
    return this.http.post(`${this.apiUrl}/sign-up`, { email, password });
  }

  confirm(sessionId: string, verificationCode: string) {
    return this.http.post(`${this.apiUrl}/confirm`, { sessionId, verificationCode });
  }

  signIn(email: string, password: string) {
    return this.http.post<{token: string, refreshToken: string}>(`${this.apiUrl}/sign-in`, { email, password });
  }

  refreshToken(refreshToken: string) {
    return this.http.post<{token: string, refreshToken: string}>(`${this.apiUrl}/refresh`, { refreshToken });
  }

  verifyToken(token: string) {
    return this.http.get(`${this.apiUrl}/verify`, {
      headers: { Authorization: `Bearer ${token}` }
    });
  }
}
```

### Guardar tokens después del login:

```typescript
this.authService.signIn(email, password).subscribe(response => {
  localStorage.setItem('token', response.token);
  localStorage.setItem('refreshToken', response.refreshToken);
});
```

### Enviar token en cada request protegido:

```typescript
// Interceptor
const token = localStorage.getItem('token');
if (token) {
  req = req.clone({
    setHeaders: { Authorization: `Bearer ${token}` }
  });
}
```

## Production Build

```bash
mvn clean package
```

The JAR file will be located under `target/`.

## Run the JAR

```bash
java -jar target/clair-core-1.0.0.jar
```

## Security Checklist for Production

- [ ] Follow the Flyway adoption instructions for an existing database
- [ ] Rotate the JWT secret (minimum 32 characters)
- [ ] Rotate the Resend API key
- [ ] Restrict `CORS_ALLOWED_ORIGINS` to your real domain(s)
- [ ] Enable HTTPS (HSTS is already configured)

## Verification

`mvn clean verify` runs the unit tests, JPA tests, complete application-context test, and
architecture rules. H2 tests use H2's dialect, with Flyway disabled.

To include PostgreSQL migration and roster integration tests, point these variables at a
**disposable test database** (the tests create and remove their own schemas):

```sh
export CLAIR_TEST_POSTGRES_URL=jdbc:postgresql://localhost:5432/clair_test
export CLAIR_TEST_POSTGRES_USER=clair_test
export CLAIR_TEST_POSTGRES_PASSWORD=clair_test
mvn clean verify
```

CI supplies PostgreSQL 15 and runs these checks on every build.
