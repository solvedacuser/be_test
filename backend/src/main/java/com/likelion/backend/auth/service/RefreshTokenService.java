package com.likelion.backend.auth.service;

import com.likelion.backend.auth.domain.RefreshToken;
import com.likelion.backend.auth.repository.RefreshTokenRepository;
import com.likelion.backend.global.error.BusinessException;
import com.likelion.backend.global.error.ErrorCode;
import com.likelion.backend.user.domain.User;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenHashService tokenHashService;
    private final long refreshTokenExpirationMs;

    public RefreshTokenService(
            RefreshTokenRepository refreshTokenRepository,
            TokenHashService tokenHashService,
            @Value("${app.jwt.refresh-token-expiration-ms}") long refreshTokenExpirationMs
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.tokenHashService = tokenHashService;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    @Transactional
    public void save(User user, String refreshToken) {
        RefreshToken token = RefreshToken.builder()
                .user(user)
                .tokenHash(tokenHashService.hash(refreshToken))
                .expiresAt(LocalDateTime.now().plusNanos(refreshTokenExpirationMs * 1_000_000))
                .build();
        refreshTokenRepository.save(token);
    }

    @Transactional(readOnly = true)
    public RefreshToken getValidToken(String refreshToken) {
        RefreshToken token = refreshTokenRepository.findByTokenHashAndRevokedFalse(tokenHashService.hash(refreshToken))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));

        if (token.isExpired(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.EXPIRED_REFRESH_TOKEN);
        }

        return token;
    }

    @Transactional
    public void revoke(String refreshToken) {
        refreshTokenRepository.findByTokenHashAndRevokedFalse(tokenHashService.hash(refreshToken))
                .ifPresent(RefreshToken::revoke);
    }
}
