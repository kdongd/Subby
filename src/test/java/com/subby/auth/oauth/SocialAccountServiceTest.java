package com.subby.auth.oauth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.subby.global.error.ApiException;
import com.subby.global.error.ErrorCode;
import com.subby.support.IntegrationTestSupport;
import com.subby.user.AuthProvider;
import com.subby.user.User;
import com.subby.user.UserIdentityRepository;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class SocialAccountServiceTest extends IntegrationTestSupport {

    @Autowired
    private SocialAccountService socialAccountService;

    @Autowired
    private UserIdentityRepository identityRepository;

    private SocialProfile google(String id, String email) {
        return new SocialProfile(AuthProvider.GOOGLE, id, email, "구글사용자");
    }

    @Test
    @DisplayName("처음 소셜 로그인하면 계정을 만들고, 다음에는 같은 계정으로 로그인한다")
    void createThenReuse() {
        User first = socialAccountService.resolve(google("g-1", "social@example.com"), null);
        User second = socialAccountService.resolve(google("g-1", "social@example.com"), null);

        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(first.hasPassword()).isFalse();
        assertThat(identityRepository.countByUserId(first.getId())).isEqualTo(1);
    }

    @Test
    @DisplayName("같은 이메일의 계정이 있으면 자동으로 합치지 않고 거절한다")
    void doNotMergeByEmail() {
        createUser("taken@example.com");

        assertThatThrownBy(() -> socialAccountService.resolve(google("g-2", "taken@example.com"), null))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.EMAIL_ALREADY_REGISTERED);
    }

    @Test
    @DisplayName("로그인한 상태에서는 기존 계정에 연결한다")
    void linkToCurrentUser() {
        User user = createUser("linker@example.com");

        User linked = socialAccountService.resolve(google("g-3", "linker@example.com"), user.getId());

        assertThat(linked.getId()).isEqualTo(user.getId());
        assertThat(identityRepository.findByProviderAndProviderUserId(AuthProvider.GOOGLE, "g-3"))
                .get()
                .extracting(identity -> identity.getUserId())
                .isEqualTo(user.getId());
    }

    @Test
    @DisplayName("다른 계정에 이미 연결된 소셜 계정은 연결할 수 없다")
    void identityInUse() {
        User owner = socialAccountService.resolve(google("g-4", null), null);
        User other = createUser("other@example.com");

        assertThatThrownBy(() -> socialAccountService.resolve(google("g-4", null), other.getId()))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getErrorCode())
                .isEqualTo(ErrorCode.IDENTITY_IN_USE);
        assertThat(owner.getId()).isNotEqualTo(other.getId());
    }

    @Test
    @DisplayName("카카오는 인증된 이메일만 사용하고, 동의하지 않았으면 이메일 없이 가입한다")
    void kakaoProfile() {
        SocialProfile withoutEmail = SocialProfile.of(AuthProvider.KAKAO, Map.of(
                "id", 12345L,
                "kakao_account", Map.of("profile", Map.of("nickname", "카카오친구"))));
        SocialProfile withEmail = SocialProfile.of(AuthProvider.KAKAO, Map.of(
                "id", 67890L,
                "kakao_account", Map.of(
                        "email", "Kakao@Example.com",
                        "is_email_valid", true,
                        "is_email_verified", true,
                        "profile", Map.of("nickname", "카카오"))));

        assertThat(withoutEmail.email()).isNull();
        assertThat(withoutEmail.providerUserId()).isEqualTo("12345");
        assertThat(withoutEmail.nickname()).isEqualTo("카카오친구");
        assertThat(withEmail.email()).isEqualTo("kakao@example.com");

        User user = socialAccountService.resolve(withoutEmail, null);
        assertThat(user.hasEmail()).isFalse();
    }
}
