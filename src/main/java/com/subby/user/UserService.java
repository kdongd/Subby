package com.subby.user;

import com.subby.global.error.ApiException;
import com.subby.global.error.ErrorCode;
import com.subby.user.dto.UserDtos.IdentityResponse;
import com.subby.user.dto.UserDtos.MeResponse;
import com.subby.user.dto.UserDtos.NotificationSettings;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final UserIdentityRepository identityRepository;

    public MeResponse me(Long userId) {
        User user = findUser(userId);
        List<IdentityResponse> identities = identityRepository.findAllByUserIdOrderByCreatedAtAsc(userId).stream()
                .map(identity -> new IdentityResponse(identity.getProvider(), identity.getCreatedAt()))
                .toList();
        return new MeResponse(user.getId(), user.getEmail(), user.getNickname(), user.hasPassword(),
                notificationSettings(user), identities);
    }

    @Transactional
    public MeResponse changeNickname(Long userId, String nickname) {
        findUser(userId).changeNickname(nickname.trim());
        return me(userId);
    }

    public NotificationSettings notificationSettings(Long userId) {
        return notificationSettings(findUser(userId));
    }

    @Transactional
    public NotificationSettings updateNotificationSettings(Long userId, boolean enabled, int daysBefore) {
        if (!User.NOTIFY_DAY_OPTIONS.contains(daysBefore)) {
            throw ApiException.invalidField("daysBefore", "알림 시점은 1, 3, 7일 전 중 하나여야 합니다.");
        }
        User user = findUser(userId);
        user.updateNotification(enabled, daysBefore);
        return notificationSettings(user);
    }

    /** 비밀번호도 없고 남은 소셜 연결이 이것 하나뿐이면 로그인할 방법이 사라지므로 막는다. */
    @Transactional
    public void unlink(Long userId, AuthProvider provider) {
        User user = findUser(userId);
        UserIdentity identity = identityRepository.findByUserIdAndProvider(userId, provider)
                .orElseThrow(() -> new ApiException(ErrorCode.IDENTITY_NOT_FOUND));
        if (!user.hasPassword() && identityRepository.countByUserId(userId) <= 1) {
            throw new ApiException(ErrorCode.LAST_LOGIN_METHOD);
        }
        identityRepository.delete(identity);
    }

    private NotificationSettings notificationSettings(User user) {
        return new NotificationSettings(user.isNotifyEnabled(), user.getNotifyDaysBefore(), user.hasEmail());
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId).orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
    }
}
