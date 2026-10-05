package com.ichat.authbe.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.ichat.authbe.model.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuthTokenServiceTest {

    private static final String SECRET = "test-only-shared-auth-token-secret-at-least-32-bytes";

    @Test
    void issuesShortLivedTokenForTheRequestedAppAndAuthSubject() {
        User user = new User();
        user.setId(42L);
        user.setUsername("jane_doe");
        user.setFirstName("Jane");
        user.setLastName("Doe");
        user.setEmail("jane@example.com");

        String token = new AuthTokenService(SECRET, "ichat-auth", 600).issue(user, "cartculate");
        var claims = JWT.require(Algorithm.HMAC256(SECRET))
                .withIssuer("ichat-auth")
                .withAudience("cartculate")
                .build()
                .verify(token);

        assertEquals("42", claims.getSubject());
        assertEquals("jane_doe", claims.getClaim("username").asString());
        assertEquals("Jane Doe", claims.getClaim("name").asString());
        assertEquals("jane@example.com", claims.getClaim("email").asString());
        assertEquals(600L, (claims.getExpiresAt().getTime() - claims.getIssuedAt().getTime()) / 1000);
    }
}
