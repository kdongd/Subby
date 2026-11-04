package com.subby.notification;

import com.subby.global.error.ApiException;
import com.subby.global.error.ErrorCode;
import com.subby.user.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 메일 하단 링크는 화면(/unsubscribe)으로 연결되고, 사용자가 버튼을 눌러야 이 API가 호출된다.
 * 메일 보안 프로그램이 링크를 미리 열어봐도 알림이 꺼지지 않게 하기 위해서다.
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final UnsubscribeTokenService unsubscribeTokenService;
    private final UserRepository userRepository;

    @PostMapping("/unsubscribe")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void unsubscribe(@Valid @RequestBody UnsubscribeRequest request) {
        Long userId = unsubscribeTokenService.verify(request.token());
        userRepository.findById(userId)
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_UNSUBSCRIBE_TOKEN))
                .disableNotification();
    }

    public record UnsubscribeRequest(@NotBlank(message = "링크가 올바르지 않습니다.") String token) {
    }
}
