package com.likelion.backend.auth.controller;

import com.likelion.backend.auth.repository.RefreshTokenRepository;
import com.likelion.backend.user.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @BeforeEach
    void setUp() {
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void signupSuccess() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "user@example.com",
                                  "password": "password123",
                                  "passwordConfirm": "password123",
                                  "name": "홍길동"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("회원가입이 완료되었습니다."))
                .andExpect(jsonPath("$.data.userId").exists())
                .andExpect(jsonPath("$.data.email").value("user@example.com"))
                .andExpect(jsonPath("$.data.name").value("홍길동"))
                .andExpect(jsonPath("$.data.password").doesNotExist());
    }

    @Test
    void signupFailsWithDuplicatedEmail() throws Exception {
        String requestBody = """
                {
                  "email": "user@example.com",
                  "password": "password123",
                  "passwordConfirm": "password123",
                  "name": "홍길동"
                }
                """;

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("DUPLICATED_EMAIL"))
                .andExpect(jsonPath("$.message").value("이미 사용 중인 이메일입니다."));
    }

    @Test
    void signupFailsWithValidationError() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "invalid-email",
                                  "password": "short",
                                  "passwordConfirm": "different",
                                  "name": ""
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[*].field", hasItem("email")))
                .andExpect(jsonPath("$.errors[*].field", hasItem("password")))
                .andExpect(jsonPath("$.errors[*].field", hasItem("name")));
    }

    @Test
    void loginSuccess() throws Exception {
        signup();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "user@example.com",
                                  "password": "password123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("로그인에 성공했습니다."))
                .andExpect(jsonPath("$.data.accessToken", not(blankOrNullString())))
                .andExpect(jsonPath("$.data.user.userId").exists())
                .andExpect(jsonPath("$.data.user.email").value("user@example.com"))
                .andExpect(jsonPath("$.data.user.name").value("홍길동"))
                .andExpect(jsonPath("$.data.user.role").value("USER"));
    }

    @Test
    void loginFailsWithInvalidPassword() throws Exception {
        signup();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "user@example.com",
                                  "password": "wrong-password"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("INVALID_LOGIN"));
    }

    @Test
    void refreshSuccess() throws Exception {
        signup();
        MvcResult loginResult = login();
        Cookie refreshCookie = loginResult.getResponse().getCookie("refreshToken");

        mockMvc.perform(post("/api/auth/refresh").cookie(refreshCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Access Token이 재발급되었습니다."))
                .andExpect(jsonPath("$.data.accessToken", not(blankOrNullString())));
    }

    @Test
    void refreshFailsWithInvalidRefreshToken() throws Exception {
        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie("refreshToken", "invalid-refresh-token")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    void getMeWithoutAccessTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void getMeSuccess() throws Exception {
        signup();
        String accessToken = extractAccessToken(login());

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("내 정보 조회에 성공했습니다."))
                .andExpect(jsonPath("$.data.email").value("user@example.com"))
                .andExpect(jsonPath("$.data.name").value("홍길동"))
                .andExpect(jsonPath("$.data.role").value("USER"));
    }

    @Test
    void logoutSuccessInvalidatesRefreshToken() throws Exception {
        signup();
        MvcResult loginResult = login();
        String accessToken = extractAccessToken(loginResult);
        Cookie refreshCookie = loginResult.getResponse().getCookie("refreshToken");

        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + accessToken)
                        .cookie(refreshCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("로그아웃되었습니다."))
                .andExpect(jsonPath("$.data").doesNotExist());

        mockMvc.perform(post("/api/auth/refresh").cookie(refreshCookie))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    void logoutWithoutAccessTokenReturnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    private void signup() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "user@example.com",
                                  "password": "password123",
                                  "passwordConfirm": "password123",
                                  "name": "홍길동"
                                }
                                """))
                .andExpect(status().isCreated());
    }

    private MvcResult login() throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "user@example.com",
                                  "password": "password123"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();
    }

    private String extractAccessToken(MvcResult result) throws Exception {
        return com.jayway.jsonpath.JsonPath.read(
                result.getResponse().getContentAsString(),
                "$.data.accessToken"
        );
    }
}
