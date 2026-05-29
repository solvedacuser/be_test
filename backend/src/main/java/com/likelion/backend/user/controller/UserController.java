package com.likelion.backend.user.controller;

import com.likelion.backend.auth.dto.UserInfoResponse;
import com.likelion.backend.global.response.ApiResponse;
import com.likelion.backend.security.CustomUserPrincipal;
import com.likelion.backend.user.domain.User;
import com.likelion.backend.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "내 정보 조회", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserInfoResponse>> getMe(
            @AuthenticationPrincipal CustomUserPrincipal principal
    ) {
        User user = userService.findById(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success("내 정보 조회에 성공했습니다.", UserInfoResponse.from(user)));
    }
}
