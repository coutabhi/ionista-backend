# Ionista Backend

Production-style e-commerce backend for **Ionista**, a fashion brand storefront. Built with Spring Boot 3.3.2 and Java 21.

Includes: JWT auth + Google OAuth2 login, product catalog with admin management, cart & wishlist, coupons & flash-sale offers, Razorpay checkout & payments, order lifecycle management, product reviews, a loyalty/referral points system, admin sales analytics, and rate limiting.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language / Runtime | Java 21 |
| Framework | Spring Boot 3.3.2 |
| Security | Spring Security 6 (JWT, OAuth2 Client) |
| Persistence | Spring Data JPA / Hibernate 6.5 |
| Database | MySQL 8 |
| Payments | Razorpay |
| Image storage | Cloudinary |
| Rate limiting | Bucket4j (in-memory) |
| API docs | springdoc-openapi (Swagger UI) |
| Build tool | Maven |

---

## Prerequisites

- **JDK 21** (`java -version` should show 21)
- **Maven 3.9+** (or use the bundled `./mvnw` wrapper if present)
- **MySQL 8** running locally or reachable via network
- A **Google OAuth2 Client ID/Secret** (from [Google Cloud Console](https://console.cloud.google.com/apis/credentials)) — only required if you want to test Google login
- A **Razorpay** test account (Key ID / Key Secret / Webhook Secret) — only required to test checkout/payments
- A **Cloudinary** account (Cloud Name / API Key / API Secret) — only required to test product image uploads

None of the third-party integrations block local boot — the app starts fine without real Google/Razorpay/Cloudinary credentials as long as the environment variables are *set to something* (empty/dummy values are fine until you actually exercise those features).

---

## 1. Clone & configure the database

Create the database (Hibernate will auto-create/alter tables on boot via `ddl-auto: update`):

```sql
CREATE DATABASE ionista;
```

---

## 2. Environment variables

All configuration is externalized via environment variables (see `src/main/resources/application.yml`). Nothing secret is hardcoded.

| Variable | Required | Default | Purpose |
|---|---|---|---|
| `DB_URL` | No | `jdbc:mysql://127.0.0.1:3306/ionista?...` | MySQL JDBC URL |
| `DB_USERNAME` | No | `ionista_user` | MySQL username |
| `DB_PASSWORD` | **Yes** | — | MySQL password |
| `JWT_SECRET` | **Yes** | — | HS256 signing key for access/refresh JWTs (use a long random string, 256-bit+) |
| `JWT_EXPIRATION` | No | `86400000` (24h, ms) | Access token TTL |
| `JWT_REFRESH_EXPIRATION` | No | `2592000000` (30d, ms) | Refresh token TTL |
| `CORS_ALLOWED_ORIGINS` | No | `http://localhost:3000` | Comma-separated allowed frontend origins |
| `GOOGLE_CLIENT_ID` | **Yes**\* | — | Google OAuth2 client ID |
| `GOOGLE_CLIENT_SECRET` | **Yes**\* | — | Google OAuth2 client secret |
| `OAUTH2_FRONTEND_REDIRECT_URI` | No | `http://localhost:3000/oauth2/callback` | Where the frontend receives the one-time exchange code after Google login |
| `OAUTH2_FRONTEND_FAILURE_URI` | No | `http://localhost:3000/login?error=oauth2` | Where the frontend is redirected on OAuth2 failure |
| `CLOUDINARY_CLOUD_NAME` | **Yes**\* | — | Cloudinary cloud name |
| `CLOUDINARY_API_KEY` | **Yes**\* | — | Cloudinary API key |
| `CLOUDINARY_API_SECRET` | **Yes**\* | — | Cloudinary API secret |
| `RAZORPAY_KEY_ID` | **Yes**\* | — | Razorpay key ID |
| `RAZORPAY_KEY_SECRET` | **Yes**\* | — | Razorpay key secret |
| `RAZORPAY_WEBHOOK_SECRET` | **Yes**\* | — | Secret configured on the Razorpay webhook, used to verify webhook signatures |
| `SHIPPING_FLAT_FEE` | No | `0` | Flat shipping fee added at checkout |
| `LOYALTY_EARN_RATE` | No | `100` | Currency units spent per 1 loyalty point earned |
| `LOYALTY_REDEEM_VALUE` | No | `1` | Currency value of 1 redeemed loyalty point |
| `LOYALTY_REFERRAL_BONUS` | No | `50` | Points awarded to both referrer and referee |
| `RATE_LIMIT_AUTH_CAPACITY` | No | `10` | Max requests per bucket refill window on `/api/v1/auth/**` |
| `RATE_LIMIT_AUTH_REFILL` | No | `10` | Tokens added per refill |
| `RATE_LIMIT_AUTH_REFILL_MINUTES` | No | `1` | Refill interval (minutes) |

\* The app boots without these set to real values, but the corresponding feature (Google login / image upload / payments) will fail at request time until they're supplied. Spring Boot still requires the property to resolve to *some* string, so set a placeholder if you're not using that feature yet.

### Setting environment variables

**PowerShell (current session only):**

```powershell
$env:DB_PASSWORD = "your_mysql_password"
$env:JWT_SECRET = "replace-with-a-long-random-secret-string"
$env:GOOGLE_CLIENT_ID = "placeholder"
$env:GOOGLE_CLIENT_SECRET = "placeholder"
$env:CLOUDINARY_CLOUD_NAME = "placeholder"
$env:CLOUDINARY_API_KEY = "placeholder"
$env:CLOUDINARY_API_SECRET = "placeholder"
$env:RAZORPAY_KEY_ID = "placeholder"
$env:RAZORPAY_KEY_SECRET = "placeholder"
$env:RAZORPAY_WEBHOOK_SECRET = "placeholder"
```

**Bash / Git Bash (current session only):**

```bash
export DB_PASSWORD="your_mysql_password"
export JWT_SECRET="replace-with-a-long-random-secret-string"
export GOOGLE_CLIENT_ID="placeholder"
export GOOGLE_CLIENT_SECRET="placeholder"
export CLOUDINARY_CLOUD_NAME="placeholder"
export CLOUDINARY_API_KEY="placeholder"
export CLOUDINARY_API_SECRET="placeholder"
export RAZORPAY_KEY_ID="placeholder"
export RAZORPAY_KEY_SECRET="placeholder"
export RAZORPAY_WEBHOOK_SECRET="placeholder"
```

Alternatively, create a `.env` file and use an IDE run-configuration or a tool like `direnv` — just make sure `.env` is in `.gitignore` and never committed.

---

## 3. Build

```bash
mvn clean compile      # quick compile check
mvn clean package       # full build, produces target/ionista-backend-0.0.1-SNAPSHOT.jar
mvn clean package -DskipTests   # skip tests if you just want the jar fast
```

---

## 4. Run

**Option A — Maven plugin (recommended for development, supports devtools hot-reload):**

```bash
mvn spring-boot:run
```

**Option B — Run the packaged jar:**

```bash
java -jar target/ionista-backend-0.0.1-SNAPSHOT.jar
```

The app starts on **http://localhost:8080** by default (`server.port`, overridable via `$env:SERVER_PORT`).

On first boot, Hibernate (`ddl-auto: update`) will create all tables automatically in the `ionista` schema — no manual migration step needed for local dev.

---

## 5. Verify it's running

```bash
curl http://localhost:8080/api/v1/categories
```

Should return `[]` (empty list) on a fresh database — not an error.

**Swagger / OpenAPI UI:**

```
http://localhost:8080/swagger-ui/index.html
```

---

## 6. Quick smoke test (curl)

```bash
# Register
curl -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Test User","email":"test@example.com","password":"Passw0rd!"}'

# Login (copy the accessToken from the response)
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"Passw0rd!"}'

# Call a protected endpoint
curl http://localhost:8080/api/v1/cart \
  -H "Authorization: Bearer <accessToken>"
```

To test Google login: visit `http://localhost:8080/oauth2/authorization/google` in a browser (requires real `GOOGLE_CLIENT_ID`/`SECRET` and an authorized redirect URI `http://localhost:8080/login/oauth2/code/google` configured in Google Cloud Console).

---

## 7. Making yourself an admin

New users register with role `USER` by default. To test admin-only endpoints (`/api/v1/admin/**`), promote a user directly in MySQL:

```sql
UPDATE users SET role = 'ADMIN' WHERE email = 'test@example.com';
```

Then log in again to get a fresh JWT with the `ROLE_ADMIN` claim.

---

## 8. Razorpay webhook (local testing)

Razorpay needs a publicly reachable URL to call your webhook endpoint (`POST /api/v1/webhooks/razorpay`). For local testing, expose your machine with a tunnel (e.g. `ngrok http 8080`) and register the generated URL + your `RAZORPAY_WEBHOOK_SECRET` in the Razorpay Dashboard under Webhooks.

---

## 9. Deploying to Railway

The repo includes a multistage `Dockerfile`, which Railway builds automatically — no Nixpacks config needed.

### 9.1 Push to GitHub

Railway deploys from a GitHub repo. Push this repo there if it isn't already.

### 9.2 Create the project

1. [railway.app](https://railway.app) → **New Project** → **Deploy from GitHub repo** → select this repo.
2. Railway detects the `Dockerfile` and builds from it automatically.

### 9.3 Add a MySQL database

In the same project: **New** → **Database** → **Add MySQL**. Railway provisions an instance and exposes `MYSQLHOST`, `MYSQLPORT`, `MYSQLUSER`, `MYSQLPASSWORD`, `MYSQLDATABASE` as variables on that MySQL service.

### 9.4 Configure environment variables on the app service

Open the **app service** (not the MySQL service) → **Variables**, and set:

```
DB_URL=jdbc:mysql://${{MySQL.MYSQLHOST}}:${{MySQL.MYSQLPORT}}/${{MySQL.MYSQLDATABASE}}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
DB_USERNAME=${{MySQL.MYSQLUSER}}
DB_PASSWORD=${{MySQL.MYSQLPASSWORD}}
```

`${{MySQL.VAR}}` is Railway's variable-reference syntax — it resolves to the MySQL service's values automatically, including if they ever rotate. Then set the rest of the vars from the table in step 2 above (`JWT_SECRET`, `GOOGLE_CLIENT_ID/SECRET`, `CLOUDINARY_*`, `RAZORPAY_*`, `CORS_ALLOWED_ORIGINS`, `OAUTH2_FRONTEND_REDIRECT_URI`, `OAUTH2_FRONTEND_FAILURE_URI`) with real/production values. Point `CORS_ALLOWED_ORIGINS` and the OAuth2 redirect/failure URIs at your future frontend's domain once you know it.

Do **not** set `PORT` or `SERVER_PORT` yourself — Railway injects `PORT` at runtime, and `server.port` already reads it (`${PORT:8080}`).

### 9.5 Set the health check path

**Settings** → **Healthcheck Path** → `/actuator/health`. This endpoint is permitted without auth in `SecurityConfig`, so Railway can poll it to confirm the container is up before routing traffic to it.

### 9.6 Generate a public domain

**Settings** → **Networking** → **Generate Domain**. You'll get something like `https://ionista-backend-production.up.railway.app`.

### 9.7 Point third-party callbacks at the live URL

- **Google Cloud Console** → your OAuth client → Authorized redirect URIs → add `https://<your-domain>/login/oauth2/code/google`.
- **Razorpay Dashboard** → Webhooks → add `https://<your-domain>/api/v1/webhooks/razorpay`, using the same secret you set as `RAZORPAY_WEBHOOK_SECRET`.
- Cloudinary needs no callback — server-to-Cloudinary uploads work as soon as `CLOUDINARY_*` vars are correct.

### 9.8 Verify the deployment

```bash
curl https://<your-domain>/actuator/health
curl https://<your-domain>/api/v1/categories   # should return []
```

Then open `https://<your-domain>/swagger-ui/index.html` and repeat the smoke test from step 6 against the live URL. Test an admin product-image upload to confirm Cloudinary works end-to-end from the deployed server.

---

## Project structure

```
src/main/java/com/ionista/
├── common/          # SecurityUtils, SlugUtils, PriceUtils
├── config/          # SecurityConfig, CloudinaryConfig
├── controller/      # REST controllers
├── dto/
│   ├── request/
│   └── response/
├── entity/          # JPA entities
├── enums/
├── exception/       # Custom exceptions + GlobalExceptionHandler
├── mapper/          # Entity <-> DTO mappers
├── repository/       # Spring Data JPA repositories + specifications
├── security/        # JWT filter/service, OAuth2 success handler, rate limiter
└── service/
    └── impl/
```

---

## Notes for production hardening (not done in this build)

- Replace `jpa.hibernate.ddl-auto: update` with a proper migration tool (Flyway/Liquibase).
- Move rate limiting from in-memory (Bucket4j `ConcurrentHashMap`) to a distributed store (Redis) if running multiple instances.
- Put real secrets in a secret manager (AWS Secrets Manager / Vault) rather than plain environment variables.
- Add HTTPS termination (reverse proxy / load balancer) — the app itself serves plain HTTP. Railway terminates TLS at its edge automatically for generated domains, so this is only a concern for other deploy targets.
