package com.subby.user;

import com.subby.global.error.ApiException;
import com.subby.global.error.ErrorCode;
import com.subby.global.security.UserPrincipal;
import com.subby.user.dto.UserDtos.IdentityResponse;
import com.subby.user.dto.UserDtos.MeResponse;
import com.subby.user.dto.UserDtos.NicknameRequest;
import com.subby.user.dto.UserDtos.NotificationSettings;
import com.subby.user.dto.UserDtos.NotificationSettingsRequest;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
@RequiredArgsConstructor
public class MeController {

    private final UserService userService;

    @GetMapping
    public MeResponse me(@AuthenticationPrincipal UserPrincipal principal) {
        return userService.me(principal.getId());
    }

    @PatchMapping
    public MeResponse changeNickname(@AuthenticationPrincipal UserPrincipal principal,
                                     @Valid @RequestBody NicknameRequest request) {
        return userService.changeNickname(principal.getId(), request.nickname());
    }

    @GetMapping("/identities")
    public List<IdentityResponse> identities(@AuthenticationPrincipal UserPrincipal principal) {
        return userService.me(principal.getId()).identities();
    }

    @DeleteMapping("/identities/{provider}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unlink(@AuthenticationPrincipal UserPrincipal principal, @PathVariable String provider) {
        AuthProvider authProvider;
        try {
            authProvider = AuthProvider.fromRegistrationId(provider);
        } catch (IllegalArgumentException e) {
            throw new ApiException(ErrorCode.IDENTITY_NOT_FOUND);
        }
        userService.unlink(principal.getId(), authProvider);
    }

    @GetMapping("/notification-settings")
    public NotificationSettings notificationSettings(@AuthenticationPrincipal UserPrincipal principal) {
        return userService.notificationSettings(principal.getId());
    }

    @PutMapping("/notification-settings")
    public NotificationSettings updateNotificationSettings(@AuthenticationPrincipal UserPrincipal principal,
                                                           @Valid @RequestBody NotificationSettingsRequest request) {
        return userService.updateNotificationSettings(principal.getId(), request.enabled(), request.daysBefore());
    }
}
