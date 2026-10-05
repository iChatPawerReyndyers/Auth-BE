# Auth-BE

Java 25 + Spring Boot 4.1.1 backend: registration, login, Google/Facebook
OAuth, forgot/reset password via SMS OTP, a profile-completion gate for
social sign-ups, and multi-app subscription gating (shared across Stickies,
Galleries, Calculator, and more via each call's `appId`).

## Run

Set the Neon credentials in the environment before starting the backend (use
the current username and a newly rotated password; do not commit either value):

```
export NEON_DATABASE_USERNAME='your-neon-username'
export NEON_DATABASE_PASSWORD='your-rotated-neon-password'
export AUTH_JWT_SECRET='same-random-secret-configured-in-trusted-app-backends'
```

```
./mvnw spring-boot:run
```
Runs on `http://localhost:8080`.

Liveness check: `GET /health`. Interactive API docs (Swagger UI):
`http://localhost:8080/swagger-ui.html`.

## Deploy to Render

Create a Web Service from this repository with:

- Runtime: Docker
- Root Directory: leave blank when this repository itself is `Auth-BE`
- Dockerfile Path: `./Dockerfile`
- Health Check Path: `/health` (optional)

Add these in Render's Environment settings. Never commit credential values:

| Variable | Required | Purpose |
|---|---|---|
| `NEON_DATABASE_USERNAME` | Yes | Neon database role |
| `NEON_DATABASE_PASSWORD` | Yes | Rotated Neon database password |
| `AUTH_JWT_SECRET` | Yes | Random secret of at least 32 bytes; configure the same value in Cartculate's backend |
| `APP_FREE_CLIENT_IDS` | Optional | Comma-separated free app IDs; defaults to `stickies,galleries`. Add `cartculate` only if it should bypass subscriptions. |
| `GOOGLE_CLIENT_IDS` | Only for Google sign-in | Comma-separated Google client IDs accepted by the backend |
| `FACEBOOK_APP_ID` | Only for Facebook sign-in | Facebook app ID |
| `FACEBOOK_APP_SECRET` | Only for Facebook sign-in | Facebook app secret |

Render supplies `PORT`; the Docker entrypoint uses it for Spring Boot, so do
not set `SERVER_PORT` yourself. Do not set `SPRING_PROFILES_ACTIVE=dev` on
Render; that profile accepts any password for an existing username.
Auth identity tokens are short-lived (10 minutes), signed with `AUTH_JWT_SECRET`,
and audience-bound to the requesting `appId`.

Dev/testing profile (accepts any password at login — never use outside local dev):
```
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

## Docs

- `docs/API.md` — every endpoint, every field, every error
- `docs/INTEGRATION_PLAYBOOK.md` — step-by-step for another app to integrate
- `docs/OAUTH_SETUP.md` — getting Google/Facebook credentials (React Native CLI native setup included)
- `docs/SMS_OTP_SETUP.md` — wiring in a real SMS provider (Twilio) for password-reset codes

## Config

Base package: `com.ichat.authbe`. Key `application.properties` settings:

| Property | Default | Purpose |
|---|---|---|
| `app.clients.free-ids` | `stickies,galleries` | App IDs exempt from the subscription check at login |
| `app.subscription.default-trial-days` | `30` | Trial length for new registrations/social sign-ups |
| `app.otp.expiry-minutes` | `5` | Password-reset code validity window |
| `app.oauth.google.client-ids` | *(env `GOOGLE_CLIENT_IDS`)* | Comma-separated Google client IDs to verify tokens against |
| `app.oauth.facebook.app-id` / `app-secret` | *(env `FACEBOOK_APP_ID` / `FACEBOOK_APP_SECRET`)* | Facebook app credentials |

Neon PostgreSQL is used by the application with SSL required. The `NEON_DATABASE_USERNAME` and `NEON_DATABASE_PASSWORD` environment variables supply credentials; automated tests use an isolated H2 in-memory database. Hibernate schema updates remain enabled with `spring.jpa.hibernate.ddl-auto=update`.
