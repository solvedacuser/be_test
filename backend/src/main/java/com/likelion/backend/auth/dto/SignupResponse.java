package com.likelion.backend.auth.dto;

import com.likelion.backend.user.domain.User;
import lombok.Getter;

@Getter
public class SignupResponse {

    private final Long userId;
    private final String email;
    private final String name;

    private SignupResponse(Long userId, String email, String name) {
        this.userId = userId;
        this.email = email;
        this.name = name;
    }

    public static SignupResponse from(User user) {
        return new SignupResponse(user.getUserId(), user.getEmail(), user.getName());
    }
}
