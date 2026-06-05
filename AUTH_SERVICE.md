# Auth Service — Documentation Technique

> **Stack :** Java 17 · Spring Boot 3.2.5 · Keycloak 24 · PostgreSQL · Redis  
> **Approche :** Authorization Code Flow + PKCE (OpenID Connect)  
> **Port :** 8081

---

## 1. Rôles de l'auth-service

L'auth-service n'est **pas** un Authorization Server. Il joue trois rôles distincts :

### Rôle 1 — Orchestrateur Keycloak (Admin API)
Il est le seul service autorisé à appeler l'Admin API Keycloak pour gérer les utilisateurs.
Aucun autre microservice ne touche à Keycloak directement.

| Action | Endpoint auth-service | Ce qu'il fait dans Keycloak |
|---|---|---|
| Inscription | `POST /api/v1/auth/register` | Crée l'utilisateur + assigne le rôle realm |
| Réinitialisation mot de passe | `POST /api/v1/auth/password-reset` | Déclenche UPDATE_PASSWORD action |
| Suspension | `POST /api/v1/admin/users/{id}/suspend` | Désactive le compte (`enabled=false`) |
| Activation | `POST /api/v1/admin/users/{id}/activate` | Réactive le compte (`enabled=true`) |
| Changement de rôle | `PATCH /api/v1/admin/users/{id}/role` | Retire l'ancien rôle, assigne le nouveau |
| Suppression | `DELETE /api/v1/admin/users/{id}` | Supprime dans Keycloak + base locale |

### Rôle 2 — Resource Server (pour ses propres endpoints)
L'auth-service valide les JWT Keycloak sur ses routes protégées via Spring Security OAuth2.
Il ne valide **pas** les tokens des autres microservices — c'est le rôle de l'API Gateway.

| Endpoint | Protection |
|---|---|
| `POST /api/v1/auth/logout` | JWT requis |
| `GET /api/v1/users/me` | JWT requis |
| `PUT /api/v1/users/me` | JWT requis |
| `GET /api/v1/admin/users/**` | JWT requis + rôle ADMIN |

### Rôle 3 — Fournisseur de données internes
Expose un endpoint sans JWT pour les appels inter-services internes (Docker DNS) :

```
GET /api/v1/auth/chauffeurs/disponibles
← appelé par vrp-service pour connaître les chauffeurs actifs
← retourne les UserProfile avec role=DRIVER et status=ACTIVE
```

---

## 2. Communications du service

### L'auth-service appelle :

```
auth-service
  │
  ├──► Keycloak :8090 (Admin API)
  │     └─ Gestion des utilisateurs (register, suspend, delete...)
  │
  ├──► PostgreSQL :5432 (auth_db)
  │     └─ UserProfile, AuditLog (lecture/écriture)
  │
  └──► Redis :6379
        └─ Blacklist des tokens révoqués (clé JTI → TTL)
```

### L'auth-service reçoit des appels de :

```
API Gateway :8080
  └──► auth-service :8081
        Routes : /api/v1/auth/**, /api/v1/users/**, /api/v1/admin/**

vrp-service :8084
  └──► auth-service :8081
        Route : GET /api/v1/auth/chauffeurs/disponibles
        (appel direct Docker DNS, pas via Gateway)
```

### L'auth-service ne communique PAS avec :
- commande-service
- livraison-service
- tracking-service
- notification-service
- L'API Gateway (c'est la Gateway qui appelle l'auth-service, jamais l'inverse)

---

## 3. OpenID Connect — est-il utilisé ?

**Oui.** OpenID Connect (OIDC) est la couche d'identité construite au-dessus d'OAuth2.

### Distinction OAuth2 / OIDC

| | OAuth2 | OpenID Connect |
|---|---|---|
| But | Autorisation (accès aux ressources) | Authentification (identité de l'utilisateur) |
| Ce qu'il produit | Access Token | Access Token + ID Token |
| Standard | RFC 6749 | Construit sur OAuth2 |

### Dans notre projet

```
SPA                    Keycloak (OIDC Provider)
 │                          │
 │  GET /auth               │
 │  ?scope=openid profile   │  ← scope "openid" active OIDC
 │─────────────────────────►│
 │                          │
 │◄─────────────────────────│
 │  access_token (JWT)      │
 │  id_token (JWT OIDC)     │  ← contient sub, email, name
 │  refresh_token           │
```

### Les éléments OIDC présents

| Élément OIDC | Où | Rôle |
|---|---|---|
| **Authorization Code Flow + PKCE** | SPA → Keycloak | Flow d'authentification |
| **JWKS endpoint** | `http://keycloak:8090/realms/vrp/protocol/openid-connect/certs` | Distribution de la clé publique |
| **Claims OIDC standard** | Dans le JWT | `sub`, `email`, `preferred_username`, `iat`, `exp` |
| **Realm roles** | Dans le JWT | `realm_access.roles` → `["DRIVER"]` |
| **OIDC Discovery** | `http://keycloak:8090/realms/vrp/.well-known/openid-configuration` | Autodécouverte des endpoints |

### Keycloak est l'OIDC Provider (OP)
### Le SPA React est le Relying Party (RP / Client OIDC)
### L'auth-service est un Resource Server

---

## 4. Flux complet — Approche PKCE

### Étape 1 — Login (SPA → Keycloak directement)

```
SPA génère :
  code_verifier  = random(64 bytes)
  code_challenge = Base64URL(SHA256(code_verifier))

SPA redirige le navigateur :
  GET http://keycloak:8090/realms/vrp/protocol/openid-connect/auth
      ?client_id=vrp-frontend
      &response_type=code
      &redirect_uri=http://localhost:3000/callback
      &code_challenge={code_challenge}
      &code_challenge_method=S256
      &scope=openid profile email

Keycloak affiche la page de login → utilisateur saisit ses credentials
Keycloak valide → génère un code d'autorisation (opaque, usage unique, 60s)
Keycloak redirige → http://localhost:3000/callback?code=AUTH_CODE
```

### Étape 2 — Échange du code contre un JWT

```
SPA appelle Keycloak :
  POST /realms/vrp/protocol/openid-connect/token
  {
    grant_type:    "authorization_code",
    client_id:     "vrp-frontend",
    code:          "AUTH_CODE",
    redirect_uri:  "http://localhost:3000/callback",
    code_verifier: "abc123..."   ← prouve l'identité du SPA
  }

Keycloak vérifie SHA256(code_verifier) == code_challenge → ✅

Keycloak retourne :
  {
    "access_token":  "eyJhbGciOiJSUzI1NiJ9...",  ← JWT RS256 (5 min)
    "refresh_token": "eyJhbGciOiJSUzI1NiJ9...",  ← (7 jours)
    "expires_in":    300,
    "token_type":    "Bearer"
  }

SPA stocke :
  access_token  → mémoire RAM (jamais localStorage)
  refresh_token → httpOnly cookie (inaccessible au JavaScript)
```

### Étape 3 — Appel API protégé

```
SPA
  GET http://localhost:8080/api/commandes
  Authorization: Bearer {access_token}

API Gateway :8080
  ├─ Fetch JWKS Keycloak (clé publique, mise en cache)
  ├─ Vérifie signature RS256
  ├─ Vérifie exp (now < exp ?)
  ├─ Extrait claims : sub, realm_access.roles, email
  ├─ Injecte headers :
  │    X-User-Id:   "keycloak-uuid"
  │    X-User-Role: "USER"
  │    X-Email:     "epicier1@vrp.com"
  └─ Route → commande-service:8082

commande-service :8082
  ├─ Lit String role = request.getHeader("X-User-Role")
  ├─ role == "USER" → retourne les commandes du client ✅
  └─ role == autre  → 403 Forbidden ✗
```

### Étape 4 — Renouvellement silencieux du token

```
access_token expire après 5 minutes

SPA détecte l'expiration (en lisant le claim "exp")
  │
  ▼
SPA appelle Keycloak silencieusement :
  POST /realms/vrp/protocol/openid-connect/token
  {
    grant_type:    "refresh_token",
    client_id:     "vrp-frontend",
    refresh_token: "{depuis le httpOnly cookie}"
  }
  │
  ▼
Keycloak retourne un nouvel access_token (5 min)
SPA reprend ses appels sans interruption
```

### Étape 5 — Déconnexion

```
SPA appelle l'auth-service :
  POST http://localhost:8080/api/v1/auth/logout
  Authorization: Bearer {access_token}
  { "refreshToken": "..." }

auth-service :8081
  ├─ Extrait le JTI du token
  ├─ Redis SET auth:blacklist:jti:{jti} "REVOKED" EX {ttl_restant}
  ├─ Blacklist aussi le refresh_token
  └─ Audit log (USER_LOGGED_OUT)

Toute requête suivante avec ce token :
  API Gateway → auth-service → TokenBlacklistFilter
  → Redis GET auth:blacklist:jti:{jti} → "REVOKED"
  → 401 Unauthorized ✗
```

---

## 5. Structure du JWT émis par Keycloak

```json
Header :
{
  "alg": "RS256",
  "typ": "JWT",
  "kid": "keycloak-key-id"
}

Payload :
{
  "sub":                "keycloak-user-uuid",
  "preferred_username": "driver1",
  "email":              "driver1@vrp.com",
  "realm_access": {
    "roles": ["DRIVER"]
  },
  "iss":  "http://keycloak:8090/realms/vrp",
  "iat":  1748000000,
  "exp":  1748000300,
  "jti":  "unique-token-uuid"
}

Signature : RSA_SIGN(header.payload, clé_privée_keycloak)
```

---

## 6. Endpoints exposés

### Publics (sans JWT)

| Méthode | URL | Description |
|---|---|---|
| `POST` | `/api/v1/auth/register` | Créer un compte (CLIENT ou DRIVER) |
| `POST` | `/api/v1/auth/password-reset` | Déclencher reset email Keycloak |
| `GET` | `/api/v1/auth/chauffeurs/disponibles` | Interne — pour vrp-service |

### Protégés (JWT requis)

| Méthode | URL | Rôle requis | Description |
|---|---|---|---|
| `POST` | `/api/v1/auth/logout` | Tout utilisateur authentifié | Blackliste le token |
| `GET` | `/api/v1/users/me` | Tout utilisateur authentifié | Profil connecté |
| `PUT` | `/api/v1/users/me` | Tout utilisateur authentifié | Modifier son profil |
| `GET` | `/api/v1/admin/users` | ADMIN | Lister tous les utilisateurs |
| `GET` | `/api/v1/admin/users/{id}` | ADMIN | Détail d'un utilisateur |
| `PATCH` | `/api/v1/admin/users/{id}/role` | ADMIN | Changer le rôle |
| `POST` | `/api/v1/admin/users/{id}/suspend` | ADMIN | Suspendre le compte |
| `POST` | `/api/v1/admin/users/{id}/activate` | ADMIN | Activer le compte |
| `DELETE` | `/api/v1/admin/users/{id}` | ADMIN | Supprimer le compte |

---

## 7. Couches de sécurité

```
Requête entrante
  │
  ├─ 1. TokenBlacklistFilter
  │       └─ Redis : JTI révoqué ? → 401
  │
  ├─ 2. OAuth2 Resource Server (Spring Security)
  │       └─ JWKS Keycloak : signature RS256 valide ? → 401
  │       └─ exp dépassé ? → 401
  │
  ├─ 3. URL Authorization (SecurityConfig)
  │       └─ /api/v1/admin/** sans ADMIN → 403
  │
  └─ 4. Method Security (@PreAuthorize)
          └─ Double protection sur les méthodes admin
```

---

## 8. Variables d'environnement

| Variable | Défaut (local) | Description |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/vrp_auth` | URL PostgreSQL |
| `DB_USER` | `vrp_auth` | Utilisateur DB |
| `DB_PASSWORD` | `vrp_auth_pass` | Mot de passe DB |
| `REDIS_HOST` | `localhost` | Hôte Redis |
| `REDIS_PORT` | `6379` | Port Redis |
| `KEYCLOAK_URL` | `http://localhost:8090` | URL Keycloak |
| `KEYCLOAK_ISSUER_URI` | `http://localhost:8090/realms/vrp` | Issuer JWT |
| `KEYCLOAK_CLIENT_SECRET` | `auth-service-secret-change-in-prod` | Secret Admin API |
| `KEYCLOAK_ADMIN_USER` | `admin` | Admin Keycloak |
| `KEYCLOAK_ADMIN_PASSWORD` | `admin` | Mot de passe admin |
| `ALLOWED_ORIGINS` | `http://localhost:3000` | CORS origins |

---

## 9. Démarrage rapide

```bash
# Lancer tout le stack
cd auth-service/docker
docker compose up -d

# Vérifier que Keycloak est prêt
curl http://localhost:8090/realms/vrp/.well-known/openid-configuration

# Vérifier que l'auth-service est prêt
curl http://localhost:8081/actuator/health

# Swagger UI
open http://localhost:8081/swagger-ui.html
```

---

## 10. Utilisateurs de test (Keycloak)

| Username | Email | Mot de passe | Rôle |
|---|---|---|---|
| `admin-vrp` | `admin@vrp.com` | `Admin@VRP2024!` | ADMIN |
| `driver1` | `driver1@vrp.com` | `Driver@2024!` | DRIVER |
| `client1` | `epicier1@vrp.com` | `Client@2024!` | USER |
