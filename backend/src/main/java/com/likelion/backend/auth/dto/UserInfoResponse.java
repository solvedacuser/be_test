package com.likelion.backend.auth.dto;

import com.likelion.backend.user.domain.User;
import lombok.Getter;

@Getter
public class UserInfoResponse {

    private final Long userId;
    private final String email;
    private final String name;
    private final String role;

    private UserInfoResponse(Long userId, String email, String name, String role) {
        this.userId = userId;
        this.email = email;
        this.name = name;
        this.role = role;
    }

    public static UserInfoResponse from(User user) {
        return new UserInfoResponse(
                user.getUserId(),
                user.getEmail(),
                user.getName(),
                user.getRole().name()
        );
    }
}
