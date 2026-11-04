package com.subby.auth.oauth;

import com.subby.global.error.ApiException;
import com.subby.global.error.ErrorCode;
import com.subby.user.User;
import com.subby.user.UserIdentity;
import com.subby.user.UserIdentityRepository;
import com.subby.user.UserRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 소셜 로그인 결과를 서비 계정과 연결한다.
 *
 * <p>같은 이메일의 계정이 이미 있어도 자동으로 합치지 않는다. 이 서비스는 가입 시 이메일 인증을 하지 않으므로,
 * 공격자가 남의 이메일로 먼저 가입해두면 자동 합치기로 진짜 주인의 계정에 공격자의 비밀번호가 붙게 된다.
 * 연결은 로그인한 상태에서 설정 화면을 통해서만 한다.
 */
@Service
@RequiredArgsConstructor
public class SocialAccountService {

    private final UserRepository userRepository;
    private final UserIdentityRepository identityRepository;

    /**
     * @param currentUserId 이미 로그인한 상태에서 연결을 요청했으면 그 사용자 ID, 로그인 화면에서 왔으면 null
     */
    @Transactional
    public User resolve(SocialProfile profile, Long currentUserId) {
        Optional<UserIdentity> existing =
                identityRepository.findByProviderAndProviderUserId(profile.provider(), profile.providerUserId());
        return currentUserId == null
                ? login(profile, existing)
                : link(profile, existing, currentUserId);
    }

    private User login(SocialProfile profile, Optional<UserIdentity> existing) {
        if (existing.isPresent()) {
            return findUser(existing.get().getUserId());
        }
        String email = profile.email();
        if (email != null && userRepository.existsByEmail(email)) {
            throw new ApiException(ErrorCode.EMAIL_ALREADY_REGISTERED);
        }
        User user = userRepository.save(User.createSocial(email, profile.nickname()));
        identityRepository.save(new UserIdentity(user.getId(), profile.provider(), profile.providerUserId()));
        return user;
    }

    private User link(SocialProfile profile, Optional<UserIdentity> existing, Long currentUserId) {
        if (existing.isPresent()) {
            if (existing.get().getUserId().equals(currentUserId)) {
                return findUser(currentUserId);
            }
            throw new ApiException(ErrorCode.IDENTITY_IN_USE);
        }
        if (identityRepository.existsByUserIdAndProvider(currentUserId, profile.provider())) {
            throw new ApiException(ErrorCode.PROVIDER_ALREADY_LINKED);
        }
        User user = findUser(currentUserId);
        identityRepository.save(new UserIdentity(currentUserId, profile.provider(), profile.providerUserId()));
        return user;
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
    }
}
