package com.subby.notification;

import com.subby.global.error.ApiException;
import com.subby.global.error.ErrorCode;
import com.subby.notification.NotificationDtos.NotificationListResponse;
import com.subby.notification.NotificationDtos.NotificationResponse;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 사이트 안 알림 목록과 읽음 처리 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationInboxService {

    /** 알림은 90일 뒤 지워지므로 최근 것만 보여 줘도 충분하다. */
    static final int LIST_LIMIT = 50;

    private final NotificationRepository repository;
    private final Clock clock;

    public NotificationListResponse list(Long userId) {
        var items = repository.findAllByUserIdOrderByCreatedAtDescIdDesc(userId, PageRequest.of(0, LIST_LIMIT))
                .stream()
                .map(NotificationResponse::from)
                .toList();
        return new NotificationListResponse(items, repository.countByUserIdAndReadAtIsNull(userId));
    }

    public long unreadCount(Long userId) {
        return repository.countByUserIdAndReadAtIsNull(userId);
    }

    @Transactional
    public void markRead(Long userId, Long notificationId) {
        repository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new ApiException(ErrorCode.NOTIFICATION_NOT_FOUND))
                .markRead(clock.instant());
    }

    @Transactional
    public void markAllRead(Long userId) {
        repository.markAllRead(userId, clock.instant());
    }
}
