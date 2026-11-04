package com.subby.auth.oauth;

import com.subby.global.error.ApiException;
import com.subby.global.security.UserPrincipal;
import com.subby.user.AuthProvider;
import com.subby.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * Google, 카카오에서 사용자 정보를 받은 뒤 서비 계정을 찾거나 만든다.
 * 이미 로그인한 사용자가 이 흐름을 시작했으면 새 로그인이 아니라 "로그인 수단 연결"로 처리한다.
 */
@Service
@RequiredArgsConstructor
public class SocialOAuth2UserService extends DefaultOAuth2UserService {

    /** 성공·실패 처리기가 연결 모드였는지 알 수 있도록 요청에 남기는 표시 */
    public static final String LINK_MODE_ATTRIBUTE = "subby.oauth2.linkMode";

    private final SocialAccountService socialAccountService;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);
        AuthProvider provider = AuthProvider.fromRegistrationId(userRequest.getClientRegistration().getRegistrationId());
        SocialProfile profile = SocialProfile.of(provider, oauth2User.getAttributes());

        Long currentUserId = currentUserId();
        if (currentUserId != null) {
            markLinkMode(provider);
        }
        try {
            User user = socialAccountService.resolve(profile, currentUserId);
            return UserPrincipal.from(user);
        } catch (ApiException e) {
            String code = e.getErrorCode().name();
            throw new OAuth2AuthenticationException(new OAuth2Error(code), e.getMessage());
        }
    }

    private Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            return principal.getId();
        }
        return null;
    }

    private void markLinkMode(AuthProvider provider) {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            attributes.setAttribute(LINK_MODE_ATTRIBUTE, provider.name(), RequestAttributes.SCOPE_REQUEST);
        }
    }
}
