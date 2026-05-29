package com.likelion.backend.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenCookieProvider {

    public static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";

    private final long refreshTokenExpirationMs;

    public RefreshTokenCookieProvider(
            @Value("${app.jwt.refresh-token-expiration-ms}") long refreshTokenExpirationMs
    ) {
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    public ResponseCookie createCookie(String refreshToken) {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/")
                .maxAge(refreshTokenExpirationMs / 1000)
                .build();
    }

    public ResponseCookie deleteCookie() {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/")
                .maxAge(0)
                .build();
    }
}
