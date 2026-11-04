package com.subby.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.subby.support.IntegrationTestSupport;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;

class PasswordResetIntegrationTest extends IntegrationTestSupport {

    private static final Pattern TOKEN = Pattern.compile("reset-password\\?token=([A-Za-z0-9_-]+)");

    private String requestResetAndCaptureToken(String email) throws Exception {
        mockMvc.perform(post("/api/auth/password-reset/request").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", email))))
                .andExpect(status().isAccepted());
        ArgumentCaptor<String> html = ArgumentCaptor.forClass(String.class);
        verify(mailSender).send(eq(email), anyString(), html.capture());
        Matcher matcher = TOKEN.matcher(html.getValue());
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    @Test
    @DisplayName("재설정 링크로 새 비밀번호를 설정하면 새 비밀번호로 로그인된다")
    void resetPassword() throws Exception {
        createUser("reset@example.com");
        String token = requestResetAndCaptureToken("reset@example.com");

        mockMvc.perform(post("/api/auth/password-reset/confirm").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("token", token, "newPassword", "newpass456"))))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "reset@example.com", "password", "newpass456"))))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "reset@example.com", "password", PASSWORD))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("재설정 링크는 한 번만 쓸 수 있다")
    void tokenIsSingleUse() throws Exception {
        createUser("once@example.com");
        String token = requestResetAndCaptureToken("once@example.com");

        mockMvc.perform(post("/api/auth/password-reset/confirm").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("token", token, "newPassword", "newpass456"))))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/auth/password-reset/confirm").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("token", token, "newPassword", "another789"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_RESET_TOKEN"));
    }

    @Test
    @DisplayName("가입되지 않은 이메일도 같은 응답을 주고, 메일은 보내지 않는다")
    void unknownEmail() throws Exception {
        mockMvc.perform(post("/api/auth/password-reset/request").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("email", "nobody@example.com"))))
                .andExpect(status().isAccepted());
        verify(mailSender, never()).send(anyString(), anyString(), anyString());
    }
}
