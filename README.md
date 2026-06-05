# VRP Auth Service

Authentication orchestration microservice for the **Plateforme VRP**
(Vehicle Routing Problem optimization platform).

---

## Architecture overview

```
┌──────────────────────────────────────────────────────────────────────┐
│  CLIENT FLOWS                                                        │
│                                                                      │
│  SPA (React/Vue)  ──── PKCE ────▶  Keycloak :8080                   │
│                   ◀─── JWT ─────                                     │
│                                                                      │
│  SPA  ──── Bearer JWT ────▶  API Gateway  ──▶  Other microservices  │
│                                    │                                 │
│                                    └──▶  auth-service :8081         │
│                                          (register / logout / admin) │
└──────────────────────────────────────────────────────────────────────┘
```

**auth-service is NOT in the login critical path.**  
Login goes SPA → Keycloak directly (Authorization Code + PKCE).  
auth-service handles: registration, logout, profile, role management.

---

## Roles

| Role         | Description                                      |
|--------------|--------------------------------------------------|
| `ADMIN`      | Full platform access – enterprise admin          |
| `DISPATCHER` | Manages routes, vehicles, fleet operations       |
| `DRIVER`     | Delivery driver – itinerary, validate deliveries |
| `USER`       | Grocery store client – orders, parcel tracking   |

---

## Tech stack

| Layer       | Technology                              |
|-------------|------------------------------------------|
| Runtime     | Java 17, Spring Boot 3.2.5               |
| Security    | Spring Security 6, OAuth2 Resource Server|
| IAM         | Keycloak 24 (OIDC, RS256, PKCE)          |
| Database    | PostgreSQL 16 + Flyway migrations        |
| Cache       | Redis 7 (token blacklist)                |
| Mapping     | MapStruct                                |
| Docs        | SpringDoc OpenAPI 3.0                    |
| Container   | Docker (multi-stage, non-root)           |
| Orchestration | Kubernetes (Deployment, HPA, Ingress)  |
| CI/CD       | GitHub Actions → ArgoCD                  |
| SAST        | SonarQube                                |
| Image scan  | Trivy                                    |
| SCA         | OWASP Dependency Check                   |

---

## Prerequisites

- Docker Desktop 4.x+
- Java 17+
- Maven 3.9+

---

## Quick start (Docker Compose)

```bash
# 1 – Clone and navigate
cd auth-service/docker

# 2 – Start all services
docker compose up -d

# 3 – Watch logs
docker compose logs -f auth-service

# 4 – Services
#   Keycloak:     http://localhost:8080  (admin / admin)
#   auth-service: http://localhost:8081
#   Swagger UI:   http://localhost:8081/swagger-ui.html
#   PostgreSQL:   localhost:5432
#   Redis:        localhost:6379
```

> Keycloak takes ~60-90 seconds to start on first boot (DB init + realm import).

---

## Keycloak setup (auto-imported)

The realm `vrp` is automatically imported from
`src/main/resources/keycloak/vrp-realm.json` on first startup.

**Test users created:**

| Username      | Password        | Role         |
|---------------|-----------------|--------------|
| `admin-vrp`   | `Admin@VRP2024!`   | ADMIN        |
| `dispatcher1` | `Dispatcher@2024!` | DISPATCHER   |
| `driver1`     | `Driver@2024!`     | DRIVER       |
| `client1`     | `Client@2024!`     | USER         |

**Manual realm access:**
1. Go to `http://localhost:8080`
2. Log in with `admin / admin`
3. Select realm `vrp`

---

## API reference

### Base URL
```
http://localhost:8081/api/v1
```

### Public endpoints

| Method | Endpoint                  | Description                          |
|--------|---------------------------|--------------------------------------|
| POST   | `/auth/register`          | Register a new USER or DRIVER        |
| POST   | `/auth/logout`            | Blacklist current token              |
| POST   | `/auth/password-reset`    | Trigger Keycloak password-reset email|

### Authenticated endpoints

| Method | Endpoint                  | Role     | Description          |
|--------|---------------------------|----------|----------------------|
| GET    | `/users/me`               | Any      | Get own profile      |
| PUT    | `/users/me`               | Any      | Update own profile   |

### Admin endpoints (ADMIN role required)

| Method | Endpoint                           | Description             |
|--------|------------------------------------|-------------------------|
| GET    | `/admin/users`                     | List all users          |
| GET    | `/admin/users/{id}`                | Get user by ID          |
| PATCH  | `/admin/users/{id}/role`           | Assign role             |
| PUT    | `/admin/users/{id}`                | Update user profile     |
| POST   | `/admin/users/{id}/suspend`        | Suspend account         |
| POST   | `/admin/users/{id}/activate`       | Activate account        |
| DELETE | `/admin/users/{id}`                | Delete user             |

---

## curl examples

### 1 – Register a new DRIVER

```bash
curl -s -X POST http://localhost:8081/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username":    "driver2",
    "email":       "driver2@vrp.com",
    "password":    "Driver@2025!",
    "firstName":   "Paul",
    "lastName":    "Bernard",
    "phoneNumber": "+33612345678",
    "role":        "DRIVER"
  }' | jq .
```

### 2 – Get a token from Keycloak (for testing – PKCE used in real SPA)

```bash
export TOKEN=$(curl -s \
  -d "client_id=vrp-frontend" \
  -d "username=driver1" \
  -d "password=Driver@2024!" \
  -d "grant_type=password" \
  http://localhost:8080/realms/vrp/protocol/openid-connect/token \
  | jq -r .access_token)

echo "Token: $TOKEN"
```

### 3 – Get own profile

```bash
curl -s http://localhost:8081/api/v1/users/me \
  -H "Authorization: Bearer $TOKEN" | jq .
```

### 4 – Secure logout (blacklists token)

```bash
curl -s -X POST http://localhost:8081/api/v1/auth/logout \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{}' | jq .
```

### 5 – Assign DISPATCHER role (ADMIN only)

```bash
export ADMIN_TOKEN=$(curl -s \
  -d "client_id=vrp-frontend" \
  -d "username=admin-vrp" \
  -d "password=Admin@VRP2024!" \
  -d "grant_type=password" \
  http://localhost:8080/realms/vrp/protocol/openid-connect/token \
  | jq -r .access_token)

curl -s -X PATCH "http://localhost:8081/api/v1/admin/users/<keycloak-user-id>/role" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"role":"DISPATCHER"}' | jq .
```

### 6 – Request password reset

```bash
curl -s -X POST http://localhost:8081/api/v1/auth/password-reset \
  -H "Content-Type: application/json" \
  -d '{"email":"driver1@vrp.com"}' | jq .
```

---

## Environment variables

| Variable                | Default (dev)                                 | Description                    |
|-------------------------|-----------------------------------------------|--------------------------------|
| `DB_URL`                | `jdbc:postgresql://localhost:5432/vrp_auth`   | PostgreSQL JDBC URL            |
| `DB_USER`               | `vrp_auth`                                    | Database username              |
| `DB_PASSWORD`           | `vrp_auth_pass`                               | Database password              |
| `REDIS_HOST`            | `localhost`                                   | Redis host                     |
| `REDIS_PORT`            | `6379`                                        | Redis port                     |
| `REDIS_PASSWORD`        | *(empty)*                                     | Redis password                 |
| `KEYCLOAK_URL`          | `http://localhost:8080`                       | Keycloak base URL              |
| `KEYCLOAK_REALM`        | `vrp`                                         | Keycloak realm name            |
| `KEYCLOAK_CLIENT_ID`    | `auth-service`                                | Keycloak client ID             |
| `KEYCLOAK_ISSUER_URI`   | `http://localhost:8080/realms/vrp`            | JWT issuer URI                 |
| `KEYCLOAK_ADMIN_USER`   | `admin`                                       | Keycloak admin username        |
| `KEYCLOAK_ADMIN_PASSWORD`| `admin`                                      | Keycloak admin password        |
| `ALLOWED_ORIGINS`       | `http://localhost:3000,http://localhost:4200`  | CORS allowed origins           |
| `SPRING_PROFILES_ACTIVE`| `default`                                     | Active Spring profile          |

---

## Kubernetes deployment

```bash
# Apply all manifests
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/secret.yaml          # Edit secrets first!
kubectl apply -f k8s/deployment.yaml
kubectl apply -f k8s/service.yaml
kubectl apply -f k8s/ingress.yaml
kubectl apply -f k8s/hpa.yaml

# Check status
kubectl get pods -n vrp
kubectl get svc  -n vrp
kubectl get hpa  -n vrp

# Stream logs
kubectl logs -f deploy/auth-service -n vrp
```

---

## Security notes

- **Passwords**: never stored in auth-service. Keycloak owns all credentials.
- **JWT**: validated via Keycloak's JWKS endpoint (RS256, asymmetric). No shared secrets.
- **Logout**: JWT ID (JTI) stored in Redis with automatic TTL → self-cleaning blacklist.
- **Secrets**: use External Secrets Operator or HashiCorp Vault in production.
- **TLS**: terminated at Ingress (nginx + cert-manager Let's Encrypt).
- **RBAC**: enforced at URL level (SecurityConfig) + method level (@PreAuthorize).

---

## DevSecOps pipeline

```
git push → GitHub Actions
    ①  Build + Unit Tests
    ②  SonarQube SAST (quality gate)
    ③  OWASP SCA (CVE ≥ 7 fails build)
    ④  Docker build + Trivy scan (CRITICAL/HIGH blocks)
    ⑤  Push image to registry (main branch only)
    ⑥  Update k8s/deployment.yaml → ArgoCD auto-sync → Kubernetes
```
