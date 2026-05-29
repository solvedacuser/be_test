package com.likelion.backend.auth.dto;

import com.likelion.backend.user.domain.User;
import lombok.Getter;

@Getter
public class LoginResponse {

    private final String accessToken;
    private final UserInfoResponse user;

    private LoginResponse(String accessToken, UserInfoResponse user) {
        this.accessToken = accessToken;
        this.user = user;
    }

    public static LoginResponse of(String accessToken, User user) {
        return new LoginResponse(accessToken, UserInfoResponse.from(user));
    }
}
