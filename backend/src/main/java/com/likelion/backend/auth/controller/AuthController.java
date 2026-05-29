package com.likelion.backend.auth.controller;

import com.likelion.backend.auth.dto.SignupRequest;
import com.likelion.backend.auth.dto.SignupResponse;
import com.likelion.backend.auth.dto.LoginRequest;
import com.likelion.backend.auth.dto.LoginResponse;
import com.likelion.backend.auth.dto.RefreshResponse;
import com.likelion.backend.auth.service.AuthService;
import com.likelion.backend.auth.service.RefreshTokenCookieProvider;
import com.likelion.backend.global.response.ApiResponse;
import com.likelion.backend.user.domain.User;
import com.likelion.backend.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final AuthService authService;
    private final RefreshTokenCookieProvider refreshTokenCookieProvider;

    @Operation(summary = "회원가입")
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignupResponse>> signup(@Valid @RequestBody SignupRequest request) {
        User user = userService.createUser(request.getEmail(), request.getPassword(), request.getName());
        ApiResponse<SignupResponse> response = ApiResponse.success(
                "회원가입이 완료되었습니다.",
                SignupResponse.from(user)
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "로그인")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthService.LoginResult result = authService.login(request.getEmail(), request.getPassword());
        ResponseCookie refreshTokenCookie = refreshTokenCookieProvider.createCookie(result.refreshToken());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                .body(ApiResponse.success("로그인에 성공했습니다.", result.response()));
    }

    @Operation(summary = "Access Token 재발급")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<RefreshResponse>> refresh(
            @CookieValue(name = RefreshTokenCookieProvider.REFRESH_TOKEN_COOKIE_NAME, required = false)
            String refreshToken
    ) {
        RefreshResponse response = authService.refresh(refreshToken);
        return ResponseEntity.ok(ApiResponse.success("Access Token이 재발급되었습니다.", response));
    }

    @Operation(summary = "로그아웃", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @CookieValue(name = RefreshTokenCookieProvider.REFRESH_TOKEN_COOKIE_NAME, required = false)
            String refreshToken
    ) {
        authService.logout(refreshToken);
        ResponseCookie deleteCookie = refreshTokenCookieProvider.deleteCookie();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, deleteCookie.toString())
                .body(ApiResponse.success("로그아웃되었습니다.", null));
    }
}
