package com.subby.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.subby.support.IntegrationTestSupport;
import jakarta.servlet.http.Cookie;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

class AuthIntegrationTest extends IntegrationTestSupport {

    @Test
    @DisplayName("회원가입하면 바로 로그인되고, 세션 쿠키로 내 정보를 조회할 수 있다")
    void signupLogsIn() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/signup").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "New@Example.com", "password", PASSWORD, "nickname", "꾸꾸"))))
                .andExpect(status().isCreated())
                .andReturn();

        Cookie session = result.getResponse().getCookie("SESSION");
        assertThat(session).isNotNull();

        mockMvc.perform(get("/api/me").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("new@example.com"))
                .andExpect(jsonPath("$.nickname").value("꾸꾸"))
                .andExpect(jsonPath("$.hasPassword").value(true));
    }

    @Test
    @DisplayName("같은 이메일로는 가입할 수 없다 (대소문자 무시)")
    void duplicateEmail() throws Exception {
        createUser("dup@example.com");

        mockMvc.perform(post("/api/auth/signup").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "DUP@example.com", "password", PASSWORD, "nickname", "다른사람"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("email"));
    }

    @Test
    @DisplayName("비밀번호 규칙(8자 이상, 영문+숫자)을 지키지 않으면 가입할 수 없다")
    void weakPassword() throws Exception {
        mockMvc.perform(post("/api/auth/signup").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "weak@example.com", "password", "abcdefgh", "nickname", "약함"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.errors[0].field").value("password"));
    }

    @Test
    @DisplayName("로그인 성공 시 세션이 생기고 BCrypt로 저장된 비밀번호와 비교한다")
    void login() throws Exception {
        createUser("login@example.com");

        MvcResult result = mockMvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "LOGIN@example.com", "password", PASSWORD))))
                .andExpect(status().isNoContent())
                .andReturn();

        Cookie session = result.getResponse().getCookie("SESSION");
        mockMvc.perform(get("/api/me").cookie(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("login@example.com"));
        assertThat(userRepository.findByEmail("login@example.com").orElseThrow().getPasswordHash())
                .startsWith("$2");
    }

    @Test
    @DisplayName("없는 이메일과 틀린 비밀번호는 같은 메시지로 응답한다")
    void sameMessageForUnknownEmailAndWrongPassword() throws Exception {
        createUser("known@example.com");

        String wrongPassword = mockMvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "known@example.com", "password", "wrong1234"))))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();
        String unknownEmail = mockMvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "unknown@example.com", "password", "wrong1234"))))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        assertThat(objectMapper.readTree(wrongPassword).get("detail"))
                .isEqualTo(objectMapper.readTree(unknownEmail).get("detail"));
    }

    @Test
    @DisplayName("5번 연속 실패하면 올바른 비밀번호로도 15분 동안 로그인할 수 없다")
    void lockAfterFiveFailures() throws Exception {
        createUser("lock@example.com");
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/login").with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("email", "lock@example.com", "password", "wrong1234"))))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "lock@example.com", "password", PASSWORD))))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("LOGIN_LOCKED"));
    }

    @Test
    @DisplayName("CSRF 토큰 없이 보낸 쓰기 요청은 거부한다")
    void csrfRequired() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "a@example.com", "password", PASSWORD))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("로그인하지 않으면 API는 401")
    void unauthorized() throws Exception {
        mockMvc.perform(get("/api/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("로그아웃하면 세션이 끊긴다")
    void logout() throws Exception {
        createUser("bye@example.com");
        Cookie session = mockMvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "bye@example.com", "password", PASSWORD))))
                .andReturn().getResponse().getCookie("SESSION");

        mockMvc.perform(post("/api/auth/logout").with(csrf()).cookie(session))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/me").cookie(session))
                .andExpect(status().isUnauthorized());
    }
}
