package com.subby.user.dto;

import com.subby.user.AuthProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

public final class UserDtos {

    private UserDtos() {
    }

    public record MeResponse(
            Long id,
            String email,
            String nickname,
            boolean hasPassword,
            NotificationSettings notification,
            List<IdentityResponse> identities
    ) {
    }

    public record IdentityResponse(AuthProvider provider, Instant connectedAt) {
    }

    /** 결제 알림 설정. 사이트 안 알림과 기기 푸시에 함께 적용된다. */
    public record NotificationSettings(boolean enabled, int daysBefore) {
    }

    public record NicknameRequest(
            @NotBlank(message = "닉네임을 입력해 주세요.")
            @Size(max = 30, message = "닉네임은 30자 이하로 입력해 주세요.")
            String nickname
    ) {
    }

    public record NotificationSettingsRequest(
            @NotNull(message = "알림 사용 여부를 선택해 주세요.") Boolean enabled,
            @NotNull(message = "알림 시점을 선택해 주세요.") Integer daysBefore
    ) {
    }
}
