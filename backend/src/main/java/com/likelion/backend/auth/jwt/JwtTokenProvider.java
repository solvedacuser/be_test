package com.likelion.backend.auth.jwt;

import com.likelion.backend.user.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long accessTokenExpirationMs;
    private final long refreshTokenExpirationMs;

    public JwtTokenProvider(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-token-expiration-ms}") long accessTokenExpirationMs,
            @Value("${app.jwt.refresh-token-expiration-ms}") long refreshTokenExpirationMs
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationMs = accessTokenExpirationMs;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    public String generateAccessToken(User user) {
        return generateToken(user, accessTokenExpirationMs, "access");
    }

    public String generateRefreshToken(User user) {
        return generateToken(user, refreshTokenExpirationMs, "refresh");
    }

    public boolean validateToken(String token) {
        parseClaims(token);
        return true;
    }

    public boolean validateAccessToken(String token) {
        validateTokenType(token, "access");
        return true;
    }

    public boolean validateRefreshToken(String token) {
        validateTokenType(token, "refresh");
        return true;
    }

    public Long getUserId(String token) {
        return Long.valueOf(parseClaims(token).getSubject());
    }

    public String getRole(String token) {
        return parseClaims(token).get("role", String.class);
    }

    private void validateTokenType(String token, String expectedType) {
        String tokenType = parseClaims(token).get("type", String.class);
        if (!expectedType.equals(tokenType)) {
            throw new JwtException("Invalid token type");
        }
    }

    private String generateToken(User user, long expirationMs, String tokenType) {
        Date now = new Date();
        Date expiresAt = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(String.valueOf(user.getUserId()))
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .claim("type", tokenType)
                .issuedAt(now)
                .expiration(expiresAt)
                .signWith(secretKey)
                .compact();
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
