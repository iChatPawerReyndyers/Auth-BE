# Integration playbook — using this auth backend from another app

This walks through wiring a *different* app (web, mobile, or another backend) up to the login/registration API described in `API.md`.

## 1. Point your app at the backend

Set a base URL your new app can read from config/env, e.g.:
```
AUTH_API_BASE_URL=http://<host>:8080/api/auth
```
For a mobile app on a physical device, `localhost` won't reach a backend running on your dev machine — use your machine's LAN IP instead (see the frontend README for the same note).

## 2. Wire up registration

Send the seven required fields as JSON to `POST {AUTH_API_BASE_URL}/register`. Validate on your side first if you want instant feedback (matching rules in `API.md`), but always handle the server's validation errors too — it's the source of truth.

```javascript
async function registerUser(payload) {
  const res = await fetch(`${AUTH_API_BASE_URL}/register`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });
  const data = await res.json();
  if (!res.ok) throw new Error(data.message || "Registration failed");
  return data; // { success, message, username }
}
```

```java
// Java client example (e.g. another Spring service)
RestClient client = RestClient.create();
AuthResponse response = client.post()
    .uri("http://localhost:8080/api/auth/register")
    .contentType(MediaType.APPLICATION_JSON)
    .body(registerRequest)
    .retrieve()
    .body(AuthResponse.class);
```

## 3. Wire up login

Same pattern against `POST {AUTH_API_BASE_URL}/login` with `{ username, password }`.

When the profile is complete, the response includes a short-lived `accessToken`
whose `aud` is the request's `appId`. Do not treat it as a persistent mobile
session token; send it to a trusted app backend to exchange the Auth subject for
that app's own local user ID.

## 3b. Social login (Google / Facebook)

`POST {AUTH_API_BASE_URL}/oauth/google` and `POST {AUTH_API_BASE_URL}/oauth/facebook` are also available — your app obtains a Google ID token / Facebook access token client-side (same as the main frontend does, see `useGoogleAuth.ts` / `useFacebookAuth.ts`) and sends it to these endpoints. The backend verifies it with the provider, then auto-links or auto-creates the account. Full request/response shapes and the account-linking rules are in `API.md`. These won't work until `app.oauth.google.client-ids` / `app.oauth.facebook.app-id` / `app.oauth.facebook.app-secret` are configured — see `OAUTH_SETUP.md`.

## 3c. Forgot / reset password

`POST {AUTH_API_BASE_URL}/password/forgot` with `{ username }` sends a
6-digit code to the account's registered mobile number (logged to the
backend console until a real SMS provider is configured — see
`SMS_OTP_SETUP.md`). `POST {AUTH_API_BASE_URL}/password/reset` with
`{ username, otp, newPassword, confirmNewPassword }` completes the reset.
Full details and error messages in `API.md`.

## 4. Resolve the Auth identity in your app backend

The Auth `sub` is not automatically the consuming app's database ID. Each app
backend must verify the token signature, issuer, audience, and expiration, then
map the Auth subject to its own local user record. Never trust a client-supplied
username/email as proof of identity and never invent a local user ID.

Cartculate implements this exchange at `POST /api/auth/session` on its own
backend. It returns Cartculate's numeric `UserDto.id`. If an Auth subject is
not mapped and a Cartculate username already exists, the endpoint requires the
old Cartculate credentials once to link that record; otherwise the user may
explicitly create a fresh Cartculate profile. Both Render backends must have
the same `AUTH_JWT_SECRET` configured.

This exchange does not itself protect other app API routes. Those routes must
also validate bearer tokens and authorize the token's mapped local user ID
before being considered protected.

## 5. Handle errors consistently

Every error response has a `message` field (and `errors` for field-level validation failures — see `API.md`). Surface `message` directly to users for auth failures; for `errors`, map each key to the matching form field.

## 6. Before you point a second app at a shared backend — checklist

- [ ] Change CORS from `allowedOriginPatterns: "*"` to the exact origins of your apps (`SecurityConfig.java`).
- [ ] Confirm the backend is **not** running with the `dev` Spring profile (that profile accepts any password).
- [ ] Serve the backend over HTTPS once it's reachable outside your machine.
- [ ] Make every protected app API verify the bearer token and authorize its mapped local user; the identity-exchange endpoint alone does not protect data routes.
- [ ] Swap the H2 in-memory database for a persistent one (MySQL/Postgres) — H2 resets on every restart.

## 7. Testing your integration locally

Run the backend with the dev profile so you can log in with any password while you build the new app's UI, without needing real registered users yet:
```
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```
Remember to point your new app at a normal (non-dev) backend before treating anything as production-ready.

Note that the dev profile only skips the **password** check — it does not bypass the subscription gate. A test user still needs `subscriptionExpiresAt` set to today or later to log in (new registrations get a 30-day trial by default, see `API.md`).
