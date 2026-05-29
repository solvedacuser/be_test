package com.likelion.backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThat;

class CustomAccessDeniedHandlerTest {

    @Test
    void accessDeniedReturnsForbiddenErrorResponse() throws Exception {
        SecurityErrorResponseWriter responseWriter = new SecurityErrorResponseWriter(new ObjectMapper());
        CustomAccessDeniedHandler handler = new CustomAccessDeniedHandler(responseWriter);
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.handle(
                new MockHttpServletRequest("GET", "/api/users/me"),
                response,
                new AccessDeniedException("forbidden")
        );

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).contains("\"success\":false");
        assertThat(response.getContentAsString()).contains("\"code\":\"FORBIDDEN\"");
    }
}
