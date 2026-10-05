# OAuth setup guide — Google & Facebook

Follow this before Google/Facebook login can work. It only covers getting the
IDs/secret from each provider's console — the app-side wiring (buttons,
backend endpoints) is a separate step once you have these values.

---

## Part 1 — Google OAuth Client ID

1. Go to the [Google Cloud Console](https://console.cloud.google.com/) and sign in.
2. Create a new project (top bar → project dropdown → **New Project**), or select an existing one.
3. Open **APIs & Services → OAuth consent screen**.
   - User type: **External** (unless you have a Google Workspace org and want it internal-only).
   - Fill in app name, your support email, and developer contact email.
   - Scopes: the defaults (`email`, `profile`, `openid`) are enough — no need to add more.
   - Add yourself as a test user while the app is in "Testing" publish status (you can submit for verification later if you need it public).
4. Open **APIs & Services → Credentials → Create Credentials → OAuth client ID**.
   You'll need to create **more than one client ID** — Google issues a separate one per platform:
   - **Web application** — no redirect URI needed for our use; this client ID exists purely so the backend has something to verify the token's `aud` claim against (see `app.oauth.google.client-ids`). `@react-native-google-signin/google-signin` also requires passing this one as `webClientId` even on native, to get an ID token back at all.
   - **iOS** — enter your app's iOS bundle identifier (e.g. `com.yourcompany.authapp`).
   - **Android** — enter your app's package name plus its SHA-1 signing certificate fingerprint (get this with `keytool -list -v -keystore <your-keystore>`, or via Android Studio's Gradle "signingReport" task for a debug build).
5. Copy each **Client ID**. These are **not secret** — they're safe to embed in the frontend app. There is no client secret to manage for these native/public client types.

---

## Part 2 — Facebook App ID + App Secret

1. Go to [Meta for Developers](https://developers.facebook.com/) and sign in (create a developer account if you don't have one — it's a one-time step tied to your Facebook account).
2. **My Apps → Create App**. Choose the **Consumer** use case (this is a login-only integration), then follow the prompts (app name, contact email).
3. Once created, open the app dashboard and add the **Facebook Login** product (**Add Product → Facebook Login → Set Up**). Its setup wizard will ask for your Android package name + key hash and iOS bundle ID — have those ready (same package name/bundle ID as your Google setup above).
4. No OAuth redirect URI is needed — `react-native-fbsdk-next` uses the native SDK, not a browser redirect.
5. Go to **App Settings → Basic**. You'll see:
   - **App ID** — not secret, goes in the frontend config.
   - **App Secret** — click "Show", copy it. This is a real secret: it must only ever live on the backend, as an environment variable — never in frontend code, and never committed to git.
6. While the app is in development mode, only you (and anyone you add as a Tester under **Roles**) can log in with it. To let the public use Facebook login, submit the `public_profile` and `email` permissions for App Review before launch (these two are usually auto-approved for standard use).

---

## Where each value goes (already wired into the code — just fill these in)

| Value | Secret? | Goes in |
|---|---|---|
| Google Web Client ID | No | `frontend/src/config/appConfig.ts` → `GOOGLE_WEB_CLIENT_ID` |
| Google iOS Client ID | No | `frontend/src/config/appConfig.ts` → `GOOGLE_IOS_CLIENT_ID` |
| Google Android Client ID | No | `frontend/src/config/appConfig.ts` → `GOOGLE_ANDROID_CLIENT_ID` |
| Google Client ID(s) (backend) | No | Backend env var `GOOGLE_CLIENT_IDS` — comma-separated list of the three IDs above, read by `app.oauth.google.client-ids` in `application.properties`. The backend checks a Google token's `aud` claim against this list. |
| Facebook App ID | No | `frontend/src/config/appConfig.ts` → `FACEBOOK_APP_ID`, **and** backend env var `FACEBOOK_APP_ID` (read by `app.oauth.facebook.app-id`) |
| Facebook App Secret | **Yes** | Backend env var `FACEBOOK_APP_SECRET` only (read by `app.oauth.facebook.app-secret`). Never put it in the frontend, never commit it. |

Until these are filled in, `/api/auth/oauth/google` and `/api/auth/oauth/facebook` respond with a clear "not configured yet" error instead of failing silently — see `API.md`.

## Native setup (React Native CLI — no Expo)

The frontend uses native SDKs (`@react-native-google-signin/google-signin`,
`react-native-fbsdk-next`), not a browser-redirect flow, so skip any
"Expo redirect URI" guidance elsewhere referencing `auth.expo.io` — it
doesn't apply here. Instead:

**Google, Android**: download `google-services.json` from your Firebase/
Google Cloud project (linked to the Android Client ID + SHA-1 fingerprint)
and place it at `android/app/google-services.json`.

**Google, iOS**: download `GoogleService-Info.plist` and add it to the Xcode
project; add the `REVERSED_CLIENT_ID` (found inside that plist) as a URL
scheme in `ios/AuthApp/Info.plist` under `CFBundleURLTypes`.

**Facebook, both platforms**: add to `android/app/src/main/res/values/strings.xml`:
```xml
<string name="facebook_app_id">YOUR_APP_ID</string>
<string name="fb_login_protocol_scheme">fbYOUR_APP_ID</string>
```
and to `android/app/src/main/AndroidManifest.xml` inside `<application>`:
```xml
<meta-data android:name="com.facebook.sdk.ApplicationId" android:value="@string/facebook_app_id"/>
```
and to `ios/AuthApp/Info.plist`:
```xml
<key>CFBundleURLTypes</key>
<array>
  <dict>
    <key>CFBundleURLSchemes</key>
    <array><string>fbYOUR_APP_ID</string></array>
  </dict>
</array>
<key>FacebookAppID</key>
<string>YOUR_APP_ID</string>
<key>FacebookClientToken</key>
<string>YOUR_CLIENT_TOKEN</string>
```
(Client token: App Dashboard → Settings → Advanced → Client Token.)

After editing native files, re-run `npx pod-install` (iOS) and rebuild —
these aren't picked up by Metro's hot reload.

## Before this goes to production

- [ ] Move the Facebook App from development to live mode (after App Review, if required for your permissions).
- [ ] Replace Google's "Testing" publish status with a verified consent screen if you'll have external users beyond your test list.
- [ ] Confirm `FACEBOOK_APP_SECRET` is set via your hosting platform's secret/env store, not a checked-in properties file.
- [ ] Confirm the release build's actual signing SHA-1 (not the debug one) is registered in Google Cloud Console, and the release build's key hash is registered with Facebook — native sign-in silently fails otherwise.
