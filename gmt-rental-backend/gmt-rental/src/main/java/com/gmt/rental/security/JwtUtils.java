package com.gmt.rental.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtUtils {
    private final Key signingKey;
    private final long expirationMs;

    public JwtUtils(
            @Value("${gmt.jwt.secret}") String secret,
            @Value("${gmt.jwt.expiration:86400000}") long expirationMs
    ) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generateToken(String email) {
        Instant now = Instant.now();
        return Jwts.builder().setSubject(email).setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusMillis(expirationMs)))
                .signWith(signingKey, SignatureAlgorithm.HS256).compact();
    }

    public String getEmail(String token) { return claims(token).getSubject(); }
    public boolean isValid(String token) {
        try { claims(token); return true; }
        catch (RuntimeException exception) { return false; }
    }

    private Claims claims(String token) {
        return Jwts.parserBuilder().setSigningKey(signingKey).build().parseClaimsJws(token).getBody();
    }
}
