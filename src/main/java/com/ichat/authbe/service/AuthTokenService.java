package com.ichat.authbe.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.ichat.authbe.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Service
public class AuthTokenService {

    private final Algorithm algorithm;
    private final String issuer;
    private final long expirationSeconds;

    public AuthTokenService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.issuer:ichat-auth}") String issuer,
            @Value("${app.jwt.expiration-seconds:600}") long expirationSeconds) {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("AUTH_JWT_SECRET must contain at least 32 bytes");
        }
        this.algorithm = Algorithm.HMAC256(secret);
        this.issuer = issuer;
        this.expirationSeconds = expirationSeconds;
    }

    public String issue(User user, String appId) {
        if (appId == null || appId.isBlank()) {
            throw new IllegalArgumentException("appId is required to issue an identity token");
        }

        Instant issuedAt = Instant.now();
        String name = String.join(" ", user.getFirstName(), user.getLastName()).trim();
        var token = JWT.create()
                .withIssuer(issuer)
                .withAudience(appId)
                .withSubject(user.getId().toString())
                .withClaim("username", user.getUsername())
                .withClaim("name", name)
                .withIssuedAt(Date.from(issuedAt))
                .withExpiresAt(Date.from(issuedAt.plusSeconds(expirationSeconds)));

        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            token.withClaim("email", user.getEmail());
        }

        return token.sign(algorithm);
    }
}