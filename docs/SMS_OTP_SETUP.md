# SMS/OTP setup guide

Password reset generates a 6-digit code and sends it through `SmsSender`
(`backend/src/main/java/com/authapp/backend/service/sms/`). Out of the box,
the only implementation is `ConsoleSmsSender`, which **logs the code to the
backend console instead of texting it** — fine for local development, not
usable for real users. This doc covers wiring in a real provider.

## How the abstraction works

```java
public interface SmsSender {
    void send(String phoneNumber, String message);
}
```
`PasswordResetService` only depends on this interface. To go live, add a
second implementation and make Spring prefer it over the console one.

## Option A — Twilio (most common choice)

1. Create a [Twilio](https://www.twilio.com/) account and a phone number capable of sending SMS.
2. From the Twilio Console, copy:
   - **Account SID**
   - **Auth Token**
   - Your **Twilio phone number** (the "from" number)
3. Add the Twilio Java SDK to `backend/pom.xml`:
   ```xml
   <dependency>
       <groupId>com.twilio.sdk</groupId>
       <artifactId>twilio</artifactId>
       <version>10.4.1</version>
   </dependency>
   ```
4. Add a new implementation, preferred over the console one:
   ```java
   @Component
   @Primary
   public class TwilioSmsSender implements SmsSender {

       @Value("${app.sms.twilio.account-sid}") private String accountSid;
       @Value("${app.sms.twilio.auth-token}") private String authToken;
       @Value("${app.sms.twilio.from-number}") private String fromNumber;

       @PostConstruct
       void init() {
           Twilio.init(accountSid, authToken);
       }

       @Override
       public void send(String phoneNumber, String message) {
           Message.creator(
               new PhoneNumber(phoneNumber),
               new PhoneNumber(fromNumber),
               message
           ).create();
       }
   }
   ```
5. Add the config (as env vars, never committed):
   ```properties
   app.sms.twilio.account-sid=${TWILIO_ACCOUNT_SID:}
   app.sms.twilio.auth-token=${TWILIO_AUTH_TOKEN:}
   app.sms.twilio.from-number=${TWILIO_FROM_NUMBER:}
   ```
6. `@Primary` makes Spring inject `TwilioSmsSender` wherever `SmsSender` is
   needed instead of `ConsoleSmsSender`, with no change needed to
   `PasswordResetService`.

## Option B — another provider (Vonage, AWS SNS, MessageBird, etc.)

Same pattern: implement `SmsSender`, mark it `@Primary`, add that
provider's credentials as env-var-backed properties. The rest of the
password-reset flow doesn't change.

## Where each value goes

| Value | Secret? | Goes in |
|---|---|---|
| Twilio Account SID | Treat as sensitive | Backend env var `TWILIO_ACCOUNT_SID` |
| Twilio Auth Token | **Yes** | Backend env var `TWILIO_AUTH_TOKEN` — never the frontend, never committed |
| Twilio phone number | No | Backend env var `TWILIO_FROM_NUMBER` |

## Before this goes to production

- [ ] Remove/disable `ConsoleSmsSender` (or ensure `@Primary` on the real sender always wins) so OTPs never get logged in a shared environment.
- [ ] Confirm your SMS provider account has sufficient sending limits/credit for expected volume.
- [ ] Consider rate-limiting `/api/auth/password/forgot` (not implemented yet) so the endpoint can't be used to spam a phone number with codes.
