package com.likelion.backend.auth.service;

import com.likelion.backend.auth.domain.RefreshToken;
import com.likelion.backend.auth.dto.LoginResponse;
import com.likelion.backend.auth.dto.RefreshResponse;
import com.likelion.backend.auth.jwt.JwtTokenProvider;
import com.likelion.backend.global.error.BusinessException;
import com.likelion.backend.global.error.ErrorCode;
import com.likelion.backend.user.domain.User;
import com.likelion.backend.user.service.UserService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;

    @Transactional
    public LoginResult login(String email, String password) {
        User user = userService.findByEmail(email);
        if (!passwordEncoder.matches(password, user.getPassword())) {
            log.debug("Login failed for email: {}", email);
            throw new BusinessException(ErrorCode.INVALID_LOGIN);
        }

        String accessToken = jwtTokenProvider.generateAccessToken(user);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user);
        refreshTokenService.save(user, refreshToken);
        log.debug("Login succeeded for userId: {}", user.getUserId());

        return new LoginResult(LoginResponse.of(accessToken, user), refreshToken);
    }

    @Transactional
    public RefreshResponse refresh(String refreshTokenValue) {
        if (refreshTokenValue == null || refreshTokenValue.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        try {
            jwtTokenProvider.validateRefreshToken(refreshTokenValue);
        } catch (ExpiredJwtException exception) {
            log.debug("Access Token refresh failed: expired Refresh Token");
            throw new BusinessException(ErrorCode.EXPIRED_REFRESH_TOKEN);
        } catch (JwtException | IllegalArgumentException exception) {
            log.debug("Access Token refresh failed: invalid Refresh Token");
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        RefreshToken refreshToken = refreshTokenService.getValidToken(refreshTokenValue);
        String accessToken = jwtTokenProvider.generateAccessToken(refreshToken.getUser());
        log.debug("Access Token refreshed for userId: {}", refreshToken.getUser().getUserId());
        return new RefreshResponse(accessToken);
    }

    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            refreshTokenService.revoke(refreshToken);
        }
    }

    public record LoginResult(LoginResponse response, String refreshToken) {
    }
}
