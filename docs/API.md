# Auth API documentation

Base URL (local dev): `http://localhost:8080`

All endpoints are under `/api/auth`, accept and return `application/json`, and require no authentication header themselves (they're the entry point to authentication).

---

## GET /health

Liveness check for load balancers/monitoring — not under `/api/auth`, no
auth required. Doesn't check the database or any downstream dependency.

```json
{ "status": "UP", "timestamp": "2026-10-02T21:56:00Z" }
```

## Interactive API docs (Swagger UI)

Once the backend is running, browse to `http://localhost:8080/swagger-ui.html`
for interactive, try-it-out documentation of every endpoint below (raw
OpenAPI JSON at `/v3/api-docs`). Both are public in the current config —
restrict them (or remove the `permitAll` for them in `SecurityConfig.java`)
before exposing this backend publicly, same as the H2 console.

---

## POST /api/auth/register

Creates a new user.

### Request body

| Field             | Type    | Rules                                                        |
|--------------------|---------|---------------------------------------------------------------|
| `username`         | string  | required, 3–30 characters, must be unique                     |
| `firstName`        | string  | required                                                       |
| `lastName`         | string  | required                                                       |
| `password`         | string  | required, minimum 8 characters                                 |
| `confirmPassword`  | string  | required, must exactly match `password`                       |
| `birthYear`        | integer | required, between 1900 and 2026                                |
| `phoneNumber`      | string  | required, 7–15 digits, optional leading `+`                    |
| `subscriptionExpiresAt` | string (ISO date, `yyyy-MM-dd`) | optional — if omitted, the user is granted a 30-day trial from today (configurable via `app.subscription.default-trial-days`) |

```json
{
  "username": "jane_doe",
  "firstName": "Jane",
  "lastName": "Doe",
  "password": "correct-horse-battery",
  "confirmPassword": "correct-horse-battery",
  "birthYear": 1995,
  "phoneNumber": "+15551234567",
  "subscriptionExpiresAt": "2026-12-31"
}
```
`subscriptionExpiresAt` can be omitted entirely — see the table above for the default.

### Success response — `200 OK`

```json
{
  "success": true,
  "message": "Registration successful",
  "username": "jane_doe"
}
```

### Error responses

Field validation failure — `400 Bad Request`:
```json
{
  "success": false,
  "message": "Validation failed",
  "errors": {
    "password": "Password must be at least 8 characters",
    "phoneNumber": "Phone number must be 7-15 digits, optionally starting with +"
  }
}
```

Business rule failure (mismatched passwords, duplicate username) — `400 Bad Request`:
```json
{
  "success": false,
  "message": "Username is already taken"
}
```
Other possible `message` values: `"Password and confirm password do not match"`.

---

## POST /api/auth/login

Authenticates an existing user.

### Request body

| Field      | Type   | Rules            |
|------------|--------|------------------|
| `username` | string | required         |
| `password` | string | required         |
| `appId`    | string | required — identifies the calling app (e.g. `"stickies"`, `"galleries"`, `"calculator"`). Decides whether the subscription check below applies — see "Multi-app subscription gating" at the end of this doc. |

```json
{
  "username": "jane_doe",
  "password": "correct-horse-battery",
  "appId": "stickies"
}
```

### Success response — `200 OK`

```json
{
  "success": true,
  "message": "Login successful",
  "username": "jane_doe",
  "profileComplete": true,
  "accessToken": "<short-lived app-audience JWT>"
}
```
When the profile is complete, `accessToken` is a 10-minute JWT signed with
`AUTH_JWT_SECRET`; `sub` is the Auth user ID and `aud` is the request's
`appId`. A trusted app backend can verify it to resolve its own local user
identity. Do not use it as a long-lived mobile session token. When
`profileComplete` is false, no token is issued until profile completion.
`profileComplete` is `false` when `birthYear` or `phoneNumber` is still
missing — only possible for a Google/Facebook sign-up, since password
registration requires both. **The frontend must treat `profileComplete: false`
as blocking**: show the "complete your profile" modal (see
`POST /api/auth/profile/complete` below) instead of letting the user into
the app. This is re-checked live on every successful login/OAuth call, not
stored as a one-time flag — so closing the app or canceling the modal
doesn't bypass it; it reappears on the next login with the same account.

### Error response — `400 Bad Request`

Wrong credentials:
```json
{
  "success": false,
  "message": "Invalid username or password"
}
```
The same message is returned whether the username doesn't exist or the password is wrong, so callers can't enumerate valid usernames.

Subscription expired or never started — only returned when `appId` requires a subscription (checked only after the password is verified):
```json
{
  "success": false,
  "message": "Unable to log in: your subscription is not active. Please renew your subscription to continue."
}
```

---

---

## POST /api/auth/oauth/google

Logs in (or auto-registers/auto-links) a user via a Google ID token obtained client-side.

### Request body

| Field     | Type   | Rules    |
|-----------|--------|----------|
| `idToken` | string | required — the ID token from Google Sign-In on the client |
| `appId`   | string | required — same meaning as `/login`'s `appId` |

### Success response — `200 OK`
Same shape as `/login`: `{ "success": true, "message": "Login successful", "username": "...", "profileComplete": true }`

### Error responses — `400 Bad Request`
- `"Google sign-in is not configured yet (app.oauth.google.client-ids is empty)"` — no client ID set on the backend yet.
- `"Google sign-in failed: token was not issued for this app"` — the token's `aud` doesn't match any configured client ID.
- `"Google sign-in failed: invalid or expired token"`
- Subscription error (same message as `/login`) — only if `appId` requires a subscription and the resolved account's isn't active.

---

## POST /api/auth/oauth/facebook

Logs in (or auto-registers/auto-links) a user via a Facebook access token obtained client-side.

### Request body

| Field         | Type   | Rules    |
|---------------|--------|----------|
| `accessToken` | string | required — the access token from Facebook Login on the client |
| `appId`       | string | required — same meaning as `/login`'s `appId` |

### Success response — `200 OK`
Same shape as `/login`.

### Error responses — `400 Bad Request`
- `"Facebook sign-in is not configured yet (app.oauth.facebook.app-id / app-secret are empty)"`
- `"Facebook sign-in failed: token is invalid or was issued for a different app"`
- `"Facebook sign-in failed: invalid or expired token"` / `"...could not fetch profile"`
- Subscription error (same message as `/login`) — only if `appId` requires a subscription and the resolved account's isn't active.

### Account linking behavior (Google and Facebook both)

1. If this provider+providerId has signed in before, it logs into the same account every time.
2. Otherwise, if the provider returns an email that matches an existing account (password-based or another provider), this identity is **linked to that account** — the same user can have a password, a Google login, and a Facebook login all at once.
3. Otherwise, a **new account is created** automatically (username derived from the email, or from the provider's name as a fallback), with the same default trial subscription as a normal registration. `birthYear` and `phoneNumber` are left empty since social providers don't supply them — the response comes back with `profileComplete: false`, and the account stays that way (fully created, just incomplete) until `POST /api/auth/profile/complete` is called.

---

---

## Multi-app subscription gating

This backend is shared across multiple separate apps (Stickies, Galleries,
Calculator, and more). One account can use any of them, and has a single
`subscriptionExpiresAt` — but whether that's actually *checked* at login
depends on which app is asking:

- Every `/login`, `/oauth/google`, and `/oauth/facebook` call includes
  `appId`, identifying the calling app.
- The backend's `app.clients.free-ids` property lists app IDs exempt from
  the subscription check. **Current value: `stickies,galleries`.**
- Any `appId` not on that list — Calculator included, and any future app
  until it's added — requires an active subscription to log in.
- An unrecognized/misspelled `appId` is treated as **requiring** a
  subscription (fails closed), not as an error and not as free. If a free
  app's login starts unexpectedly demanding a subscription, check for a
  typo in either its `appId` or the `app.clients.free-ids` config before
  assuming something else is wrong.

To add a new free app later: add its ID to `app.clients.free-ids` in
`application.properties` (comma-separated) and restart the backend — no
code change needed. Every other app (anything not on that list) requires a
subscription automatically, with no config needed on its part.

---

## POST /api/auth/profile/complete

Fills in `birthYear`/`phoneNumber` on an account that doesn't have them yet
— exclusively needed for Google/Facebook sign-ups, since password
registration already requires both.

### Request body

| Field | Type | Rules |
|---|---|---|
| `username` | string | required |
| `birthYear` | integer | required, between 1900 and 2026 |
| `phoneNumber` | string | required, 7–15 digits, optional leading `+` |
| `appId` | string | optional for older clients; when supplied, issues a token for this app after completion |

### Success — `200 OK`
```json
{ "success": true, "message": "Login successful", "username": "jane_doe", "profileComplete": true, "accessToken": "<short-lived app-audience JWT>" }
```

### Errors — `400 Bad Request`
- Field validation (same shape as `/register`'s validation errors)
- `"Account not found"` — unknown username

---

## POST /api/auth/password/forgot

Starts a password reset by sending a 6-digit code to the account's
registered mobile number.

### Request body

| Field      | Type   | Rules    |
|------------|--------|----------|
| `username` | string | required |

### Response — always `200 OK`

```json
{
  "success": true,
  "message": "If an account with that username exists, a verification code has been sent to its registered mobile number.",
  "username": null
}
```
This message is returned **whether or not the username exists**, and
whether or not it has a phone number on file — so this endpoint can't be
used to check which usernames are registered. No SMS provider is wired in
by default; the code is logged to the backend console instead (see
`SMS_OTP_SETUP.md`) until a real provider is configured.

---

## POST /api/auth/password/reset

Completes a password reset using the code sent above.

### Request body

| Field                 | Type   | Rules                                   |
|-----------------------|--------|------------------------------------------|
| `username`            | string | required                                  |
| `otp`                 | string | required, exactly 6 digits                |
| `newPassword`         | string | required, minimum 8 characters            |
| `confirmNewPassword`  | string | required, must exactly match `newPassword` |

### Success — `200 OK`
```json
{
  "success": true,
  "message": "Password reset successful. You can now log in with your new password.",
  "username": "jane_doe"
}
```

### Errors — `400 Bad Request`
- `"New password and confirm password do not match"`
- `"Invalid or expired code"` — wrong OTP, expired OTP (default 5 minutes, see `app.otp.expiry-minutes`), already-used OTP, or unknown username. Deliberately the same message in every case so the endpoint doesn't leak which part was wrong.

---

## curl examples

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"jane_doe","firstName":"Jane","lastName":"Doe","password":"correct-horse-battery","confirmPassword":"correct-horse-battery","birthYear":1995,"phoneNumber":"+15551234567"}'

curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"jane_doe","password":"correct-horse-battery","appId":"stickies"}'

curl -X POST http://localhost:8080/api/auth/oauth/google \
  -H "Content-Type: application/json" \
  -d '{"idToken":"<google-id-token>","appId":"stickies"}'

curl -X POST http://localhost:8080/api/auth/oauth/facebook \
  -H "Content-Type: application/json" \
  -d '{"accessToken":"<facebook-access-token>","appId":"stickies"}'

curl -X POST http://localhost:8080/api/auth/profile/complete \
  -H "Content-Type: application/json" \
  -d '{"username":"jane_doe","birthYear":1995,"phoneNumber":"+15551234567"}'

curl -X POST http://localhost:8080/api/auth/password/forgot \
  -H "Content-Type: application/json" \
  -d '{"username":"jane_doe"}'

curl -X POST http://localhost:8080/api/auth/password/reset \
  -H "Content-Type: application/json" \
  -d '{"username":"jane_doe","otp":"482197","newPassword":"new-correct-horse","confirmNewPassword":"new-correct-horse"}'
```

---

## Notes for integrators

- **Identity token:** successful login/OAuth and profile completion (when `appId` is supplied) return a 10-minute `accessToken` for the requesting app. A trusted app backend can verify it with the shared `AUTH_JWT_SECRET`; `sub` is the Auth user ID and `aud` is `appId`. The token is intended for backend identity exchange, not long-lived storage in a mobile app. This Auth service does not automatically protect a consuming app's APIs; each backend must verify the token and map it to its own local user ID.
- **Subscription gate**: login only succeeds if the user's `subscriptionExpiresAt` is today or later. A new registration gets a 30-day trial by default (see the register table above) — after that, `login` will return the subscription error above until the date is extended. There's currently no endpoint to renew/extend a subscription from the API; that value is only set at registration or directly in the database for now.
- **Passwords** are hashed with BCrypt server-side; plaintext passwords are never stored.
- **Google/Facebook login** requires `app.oauth.google.client-ids`, `app.oauth.facebook.app-id`, and `app.oauth.facebook.app-secret` to be set (via env vars) — see `OAUTH_SETUP.md`. Until then, both endpoints return a clear "not configured yet" error rather than failing silently.
- **Password reset OTPs** are logged to the backend console, not texted, until a real SMS provider is wired in — see `SMS_OTP_SETUP.md`.
- **CORS** currently allows all origins (`*`) for local development — see the Playbook's production checklist before shipping.
- **Dev/testing mode**: when the backend runs with the `dev` Spring profile, `/api/auth/login` accepts any password for an existing username. Never point a production or shared environment at a backend running with that profile active.
